/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.imagej2.core.util;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.DefaultTypedAxis;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableRealInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.img.Img;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.knime.core.node.KNIMEConstants;
import org.knime.knip.base.KNIPConstants;
import org.knime.knip.core.ThreadPoolExecutorService;
import org.knime.knip.core.ops.metadata.DimSwapper;

/**
 * {@link UnaryOperation} to create an {@link ImagePlus} from an {@link ImgPlus}
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public final class ImgToIJ {

    /**
     * Default ImageJ1 Axis Order
     */
    public static final AxisType[] DEFAULT_ORDER = new AxisType[]{Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME};

    /**
     * Default IJ1 Mapping
     */
    public static final Map<AxisType, Integer> DEFAULT_IJ1_MAPPING = createDefaultMapping();

    /**
     * Creates a mapping based on the DEFAULT_ORDER
     *
     * @return mapping based on the default axis order
     */
    public static final Map<AxisType, Integer> createDefaultMapping() {
        Map<AxisType, Integer> mapping = new HashMap<>();

        for (int i = 0; i < DEFAULT_ORDER.length; i++) {
            mapping.put(DEFAULT_ORDER[i], i);
        }

        return mapping;
    }

    /**
     * Wraps an {@link Img} using default IJ1Converter.
     *
     * @param img to be wrapped
     * @return wrapped {@link ImagePlus}
     */
    public static final <T extends RealType<T>> ImagePlus wrap(final ImgPlus<T> img) {
        return wrap(img, new DefaultProcessorFactory(), new DefaultImgToIJ1Converter<T>(img.firstElement()));
    }

    /**
     * @param img
     * @param processorFactory
     * @param converter
     *
     * @return wrapped {@link ImagePlus}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final <T extends RealType<T>> ImagePlus wrap(final ImgPlus<T> img,
                                                               final ImageProcessorFactory processorFactory,
                                                               final Converter<T, FloatType> converter) {
        // we always want to have 5 dimensions
        final RandomAccessibleInterval permuted = extendAndPermute(img);

        final int width = (int)permuted.dimension(0);
        final int height = (int)permuted.dimension(1);

        final ImagePlus r = new ImagePlus();
        final ImageStack is = new ImageStack(width, height);

        final RandomAccessibleInterval<T> access =
                img.iterationOrder().equals(((IterableRealInterval<?>)permuted).iterationOrder()) ? img : permuted;

        final IntervalIterator ii = createIntervalIterator(access);

        final long[] min = new long[access.numDimensions()];
        final long[] max = new long[access.numDimensions()];

        max[0] = permuted.max(0);
        max[1] = permuted.max(1);

        // number of planes = num tasks
        int numSlices = 1;
        for (int d = 2; d < access.numDimensions(); d++) {
            numSlices *= access.dimension(d);
        }

        // parallelization
        final ImageProcessor[] slices = new ImageProcessor[numSlices];
        final ExecutorService service =
                new ThreadPoolExecutorService(
                        KNIMEConstants.GLOBAL_THREAD_POOL.createSubPool(KNIPConstants.THREADS_PER_NODE));

        final ArrayList<Future<Void>> futures = new ArrayList<Future<Void>>();
        final T inType = img.firstElement();

        int i = 0;
        while (ii.hasNext()) {
            ii.fwd();

            for (int d = 2; d < ii.numDimensions(); d++) {
                min[d] = ii.getIntPosition(d);
                max[d] = min[d];
            }

            final int proxy = i++;

            futures.add(service.submit(new Callable<Void>() {

                final FinalInterval tmp = new FinalInterval(min, max);

                @Override
                public Void call() throws Exception {

                    final Cursor<T> cursor = Views.iterable(Views.interval(access, tmp)).cursor();

                    final ImageProcessor ip = processorFactory.createProcessor(width, height, inType);

                    final FloatType outProxy = new FloatType();
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            converter.convert(cursor.next(), outProxy);
                            ip.setf(x, y, outProxy.get());
                        }
                    }
                    slices[proxy] = ip;

                    return null;
                }
            }));
        }

        for (final Future<Void> f : futures) {
            try {
                f.get();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            } catch (final ExecutionException e) {
                e.printStackTrace();
            }
        }

        // add slices to stack
        for (ImageProcessor slice : slices) {
            is.addSlice("", slice);
        }

        // set calibration
        final double[] newCalibration = getNewCalibration(img);
        Calibration cal = new Calibration();
        cal.pixelWidth = newCalibration[0];
        cal.pixelHeight = newCalibration[1];
        cal.pixelDepth = newCalibration[3];
        cal.frameInterval = newCalibration[4];
        r.setCalibration(cal);

        r.setStack(is, (int)permuted.dimension(2), (int)permuted.dimension(3), (int)permuted.dimension(4));
        r.setTitle(img.getName());

        return r;
    }

    /**
     * @param permuted
     * @return
     */
    private static IntervalIterator createIntervalIterator(final RandomAccessibleInterval<?> permuted) {
        final long[] dims = new long[permuted.numDimensions()];
        permuted.dimensions(dims);

        dims[0] = 1;
        dims[1] = 1;

        final IntervalIterator ii = new IntervalIterator(dims);
        return ii;
    }

    /**
     * @param img
     * @return calibration
     */
    public static double[] getNewCalibration(final ImgPlus<?> img) {

        int[] mapping = getInferredMapping(img);

        // also map/swap calibration
        double[] newCalib = new double[mapping.length];
        for (int i = 0; i < img.numDimensions(); i++) {
            newCalib[mapping[i]] = img.averageScale(i);
        }

        return newCalib;
    }

    /**
     * @param img
     * @return the inferred mapping
     */
    private static int[] getInferredMapping(final ImgPlus<?> img) {

        int[] inferredMapping = new int[DEFAULT_IJ1_MAPPING.size()];
        Arrays.fill(inferredMapping, -1);

        for (int d = 0; d < img.numDimensions(); d++) {
            inferredMapping[d] = DEFAULT_IJ1_MAPPING.get(img.axis(d).type());
        }

        int offset = 0;
        for (AxisType type : DEFAULT_IJ1_MAPPING.keySet()) {
            boolean contains = false;
            for (int d = 0; d < img.numDimensions(); d++) {
                if (img.axis(d).type().equals(type)) {
                    contains = true;
                    break;
                }
            }

            if (!contains) {
                inferredMapping[img.numDimensions() + offset] = DEFAULT_IJ1_MAPPING.get(type);
                offset++;
            }
        }

        return inferredMapping;
    }

    /**
     * Check if ImgPlus contains axis which can not be mapped to IJ ImagePlus. Valid axes in ImagePlus are X, Y,
     * Channel, Z, Time.
     *
     * @param img
     *
     * @return true if mapping is valid
     */
    public static boolean validateMapping(final ImgPlus<?> img) {
        for (int d = 0; d < img.numDimensions(); d++) {
            if (DEFAULT_IJ1_MAPPING.get(((DefaultTypedAxis)img.axis(d)).type()) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extends the given {@link ImgPlus} to 5-dimensions and makes sure, that the dimension order suits IJ.
     *
     * @param img to be extended and permuted
     * @return extended and permuted {@link Img}
     */
    public static <T> RandomAccessibleInterval<T> extendAndPermute(final ImgPlus<T> img) {

        // Create Mapping [at position one -> new index, at position 2 -> new index etc.] given: ImgPlus and m_mapping
        // we always want to have 5 dimensions
        RandomAccessibleInterval<T> extended = img;
        for (int d = img.numDimensions(); d < 5; d++) {
            extended = Views.addDimension(extended, 0, 0);
        }

        return DimSwapper.swap(extended, getInferredMapping(img));
    }

}
