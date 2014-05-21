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
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.meta.DefaultTypedAxis;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.knime.knip.core.ops.metadata.DimSwapper;

/**
 * {@link UnaryOperation} to create an {@link ImagePlus} from an {@link ImgPlus}
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public final class ImgToIJ implements UnaryOutputOperation<ImgPlus<? extends RealType<?>>, ImagePlus> {

    private Map<AxisType, Integer> m_mapping;

    /**
     * Standard constructor, assumes input image has 5 dimensions (X, Y, Channel, Z, Time)
     */
    public ImgToIJ() {
        m_mapping = new HashMap<AxisType, Integer>();

        // standard mapping from ImgPlus to ImagePlus
        m_mapping.put(Axes.X, 0);
        m_mapping.put(Axes.Y, 1);
        m_mapping.put(Axes.CHANNEL, 2);
        m_mapping.put(Axes.Z, 3);
        m_mapping.put(Axes.TIME, 4);
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public final ImagePlus compute(final ImgPlus<? extends RealType<?>> img, final ImagePlus r) {
        float offset = 0;
        float scale = 1;
        if (img.firstElement() instanceof BitType) {
            scale = 255;
        }
        if (img.firstElement() instanceof ByteType) {
            offset = -Byte.MIN_VALUE;
        }
        if (img.firstElement() instanceof ShortType) {
            offset = -Short.MIN_VALUE;
        }

        // Create Mapping [at position one -> new index, at position 2 -> new index etc.] given: ImgPlus and m_mapping
        int[] mapping = getNewMapping(img);

        // we always want to have 5 dimensions
        RandomAccessibleInterval extended = img;
        for (int d = img.numDimensions(); d < 5; d++) {
            extended = Views.addDimension(extended, 0, 0);
        }

        // also map/swap calibration
        double[] newCalib = new double[mapping.length];
        for(int i = 0; i < img.numDimensions(); i++){
            newCalib[mapping[i]] = img.averageScale(i);
        }

        RandomAccessibleInterval permuted = DimSwapper.swap(extended, mapping);

        //Building the IJ ImagePlus
        final long[] dim = new long[permuted.numDimensions()];
        permuted.dimensions(dim);
        final long width = dim[0];
        final long height = dim[1];
        int x, y;
        dim[0] = 1;
        dim[1] = 1;
        final IntervalIterator ii = new IntervalIterator(dim);
        final RandomAccess<? extends RealType> ra = permuted.randomAccess();
        final ImageStack is = new ImageStack((int)permuted.dimension(0), (int)permuted.dimension(1));

        while (ii.hasNext()) {
            ii.fwd();
            //TODO: Use cursor. can be made faster with subset interval
            ra.setPosition(ii);

            final ImageProcessor ip = createImageProcessor(new ImgView(permuted, null));
            for (y = 0; y < height; y++) {
                ra.setPosition(y, 1);
                for (x = 0; x < width; x++) {
                    ra.setPosition(x, 0);
                    ip.setf(x, y, (ra.get().getRealFloat() + offset) * scale);
                }
            }
            is.addSlice("", ip);
        }

        //calculates the missing arguments for the image stack constructor
        int channels = 1;
        int slices = 1;
        int frames = 1;

        switch (permuted.numDimensions()) {
            case 2:
                break;
            case 3:
                slices = (int)permuted.dimension(2);
                break;
            case 4:
                slices = (int)permuted.dimension(2);
                frames = (int)permuted.dimension(3);
                break;
            case 5:
                channels = (int)permuted.dimension(2);
                slices = (int)permuted.dimension(3);
                frames = (int)permuted.dimension(4);
                break;
            default:
                break;
        }

//        set spatial calibration
        Calibration cal = new Calibration();
        cal.pixelWidth = newCalib[0];
        cal.pixelHeight = newCalib[1];
        cal.pixelDepth = newCalib[3];
        r.setCalibration(cal );

        r.setStack(is, channels, slices, frames);
        r.setTitle(img.getName());
        return r;
    }

    /**
     * @param img
     * @return
     */
    private int[] getNewMapping(final ImgPlus<? extends RealType<?>> img) {

        int[] mapping = new int[m_mapping.size()];
        Arrays.fill(mapping, -1);

        for (int d = 0; d < img.numDimensions(); d++) {
            mapping[d] = m_mapping.get(img.axis(d).type());
        }

        int offset = 0;
        for (AxisType type : m_mapping.keySet()) {
            boolean contains = false;
            for (int d = 0; d < img.numDimensions(); d++) {
                if (img.axis(d).type().equals(type)) {
                    contains = true;
                    break;
                }
            }

            if (!contains) {
                mapping[img.numDimensions() + offset] = m_mapping.get(type);
                offset++;
            }

        }

        return mapping;
    }

    /**
     * Check if ImgPlus contains axis which can not be mapped to IJ ImagePlus. Valid axes in ImagePlus are Channel
     * (index 0), Z (index 1), Time (index 2). Use setMapping if you want to change this.
     *
     * @param img
     *
     * @return true if mapping is valid
     */
    public <T> boolean validateMapping(final ImgPlus<T> img) {
        for (int d = 0; d < img.numDimensions(); d++) {
            if (m_mapping.get(((DefaultTypedAxis)img.axis(d)).type()) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Set the dimension mapping from ImagePlus Axis to IJ ImagePlus Index
     *
     * @param mapping
     */
    public void setMapping(final Map<AxisType, Integer> mapping) {
        m_mapping = mapping;
    }

    /**
     * Infers a mapping for the argument picture
     *
     * @param <T> {@link ImgPlus} the mapping shall be based on
     * @param img the {@link ImgPlus} to infer the mapping from
     *
     * @return True when successful, equal to {@link #ImgToIJ.validateMapping}
     */
    public <T> boolean inferMapping(final ImgPlus<T> img) {
        HashMap<AxisType, Integer> newMapping = new HashMap<AxisType, Integer>();

        for (int d = 0; d < img.numDimensions(); d++) {
            newMapping.put(img.axis(d).type(), d);
        }

        m_mapping = newMapping;
        return validateMapping(img);

    }

    private static ImageProcessor createImageProcessor(final Img<? extends RealType<?>> op) {
        if ((op.dimension(0) > Integer.MAX_VALUE) || (op.dimension(1) > Integer.MAX_VALUE)) {
            throw new RuntimeException("Dimension exceeds ImageJ capabilities");
        }

        if ((op.firstElement() instanceof BitType)) {
            return new BinaryProcessor(new ByteProcessor((int)op.dimension(0), (int)op.dimension(1)));
        }
        if ((op.firstElement() instanceof ByteType) || (op.firstElement() instanceof UnsignedByteType)) {
            return new ByteProcessor((int)op.dimension(0), (int)op.dimension(1));
        }
        if ((op.firstElement() instanceof ShortType) || (op.firstElement() instanceof UnsignedShortType)) {
            return new ShortProcessor((int)op.dimension(0), (int)op.dimension(1));
        }
        if (op.firstElement() instanceof FloatType) {
            return new FloatProcessor((int)op.dimension(0), (int)op.dimension(1));
        }

        throw new UntransformableIJTypeException(op.firstElement());
    }

    @Override
    public UnaryOutputOperation<ImgPlus<? extends RealType<?>>, ImagePlus> copy() {
        return new ImgToIJ();
    }

    @Override
    public UnaryObjectFactory<ImgPlus<? extends RealType<?>>, ImagePlus> bufferFactory() {
        return new UnaryObjectFactory<ImgPlus<? extends RealType<?>>, ImagePlus>() {

            @Override
            public ImagePlus instantiate(final ImgPlus<? extends RealType<?>> a) {
                return new ImagePlus();
            }
        };
    }
}
