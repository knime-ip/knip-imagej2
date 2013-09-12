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
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.meta.DefaultCalibratedAxis;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.ops.operation.subset.views.ImgView;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

// TODO has to be replaced if imglib2 has this as fast routines
/**
 * TODO Auto-generated
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public final class ImgToIJ implements UnaryOutputOperation<ImgPlus<? extends RealType<?>>, ImagePlus> {

    private Map<AxisType, Integer> m_mapping;

    public ImgToIJ() {
        m_mapping = new HashMap<AxisType, Integer>();

        // standard mapping from ImgPlus to ImagePlus
        m_mapping.put(Axes.X, 0);
        m_mapping.put(Axes.Y, 1);
        m_mapping.put(Axes.CHANNEL, 2);
        m_mapping.put(Axes.Z, 3);
        m_mapping.put(Axes.TIME, 4);
    }

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

        // swap metadata
        final double[] calibration = new double[img.numDimensions()];
        final double[] tmpCalibration = new double[img.numDimensions()];
        img.calibration(tmpCalibration);
        final AxisType[] axes = new AxisType[img.numDimensions()];
        for (int i = 0; i < axes.length; i++) {
            calibration[i] = tmpCalibration[mapping[i]];
            axes[i] = Axes.get(img.axis(mapping[i]).type().getLabel());
        }

        // Permute Operation to make sure a certain ordering
        RandomAccessibleInterval permuted = img;

        while (!(inOrder(mapping))) {
            Integer type;
            for (int d = 0; d < img.numDimensions(); d++) {
                if ((type = m_mapping.get(img.axis(d).type())) != null) {
                    if (mapping[d] == d) {
                        continue;
                    }
                    permuted = Views.permute(permuted, d, type);
                    int temp  = mapping[mapping[d]];
                    mapping[mapping[d]] = mapping[d];
                    mapping[d] = temp;
                    break;
                }
            }
        }
        //        for (int d = 0; d < img.numDimensions(); d++) {
        //            Integer type;
        //            if ((type = m_mapping.get(img.axis(d).type())) != null) {
        //                // Hier: aufpassen dass er nicht rückwärtsmapped etc  TODO:
        //                permuted = Views.permute(permuted, d, type);
        //            }
        //        }

        final ImgPlus correctedImg = new ImgPlus(new ImgView(permuted, img.factory()));
        for (int i = 0; i < axes.length; i++) {
            correctedImg.setAxis(new DefaultCalibratedAxis(axes[i]), i);
            correctedImg.setCalibration(calibration[i], i);
        }

        final long[] dim = new long[correctedImg.numDimensions()];
        correctedImg.dimensions(dim);
        final long width = dim[0];
        final long height = dim[1];
        int x, y;
        dim[0] = 1;
        dim[1] = 1;
        final IntervalIterator ii = new IntervalIterator(dim);
        final RandomAccess<? extends RealType> ra = correctedImg.randomAccess();
        final ImageStack is = new ImageStack((int)permuted.dimension(0), (int)permuted.dimension(1));

        while (ii.hasNext()) {
            ii.fwd();
            //TODO: Use cursor. can be made faster with subset interval
            ra.setPosition(ii);

            final ImageProcessor ip = createImageProcessor(correctedImg);
            for (y = 0; y < height; y++) {
                ra.setPosition(y, 1);
                for (x = 0; x < width; x++) {
                    ra.setPosition(x, 0);
                    ip.setf(x, y, (ra.get().getRealFloat() + offset) * scale);
                }
            }
            is.addSlice("", ip);
        }

        // This is still possible to be done one op (even if permuted) as we just want to know to dimensionalities. The ordering is given in permuted.
        r.setStack(is, (int)(correctedImg.numDimensions() > 2 ? correctedImg.dimension(2) : 0),
                   (int)(correctedImg.numDimensions() > 3 ? correctedImg.dimension(3) : 0),
                   (int)(correctedImg.numDimensions() > 4 ? correctedImg.dimension(4) : 0));
        //TODO: r.setCalibration(new Calibration().setC)
        return r;
    }

    /**
     * checks if mapping is ordered
     * @param mapping
     * @return true if ordered
     */
    private boolean inOrder(final int[] mapping) {
        for (int i = 0; i < mapping.length; i++) {
            if (mapping[i] != i) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param img
     * @return
     */
    private int[] getNewMapping(final ImgPlus<? extends RealType<?>> img) {
        int[] mapping = new int[img.numDimensions()];
        for (int i = 0; i < img.numDimensions(); i++) {
            mapping[i] = m_mapping.get(img.axis(i).type());
        }
        // IJ Mapping ist: X Y Channel Z T. Eingabe Bild Mapping ist: Y X Z Channel T
        // Resultat mapping[1, 0, 3, 2, 4]
        //TODO
        return mapping;
    }

    /**
     * Check wether ImgPlus contains axis which can not be mapped to IJ ImagePlus. Valid axes in ImagePlus are Channel
     * (index 0), Z (index 1), Time (index 2). Use setMapping if you want to change.
     *
     * @param img
     * @return
     */
    public boolean validateMapping(final ImgPlus img) {
        for (int d = 0; d < img.numDimensions(); d++) {
            Integer i;
            if ((i = m_mapping.get(img.axis(d).type())) == null) {
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

    private int[] getMapping(final ImgPlus<? extends RealType> img, final long[] dimensions) {
        int[] mappedSizes = new int[5];
        Arrays.fill(mappedSizes, 1);

        for (int d = 0; d < img.numDimensions(); d++) {
            Integer i;
            if ((i = m_mapping.get(img.axis(d).type())) == null) {
                throw new RuntimeException("Dimension " + img.axis(d).type() + " could not be mapped to IJ ImagePlus.");
            }

            mappedSizes[i] = (int)img.dimension(d);
        }

        return mappedSizes;
    }

    private static ImageProcessor createImageProcessor(final Img<? extends RealType<?>> op) {
        if ((op.dimension(0) > Integer.MAX_VALUE) || (op.dimension(1) > Integer.MAX_VALUE)) {
            throw new RuntimeException("Dimension exceeds ImageJ capabilities");
        }
        if ((op.firstElement() instanceof ByteType) || (op.firstElement() instanceof UnsignedByteType)
                || (op.firstElement() instanceof BitType)) {
            return new ByteProcessor((int)op.dimension(0), (int)op.dimension(1));
        }
        if ((op.firstElement() instanceof ShortType) || (op.firstElement() instanceof UnsignedShortType)) {
            return new ShortProcessor((int)op.dimension(0), (int)op.dimension(1));
        }
        if (op.firstElement() instanceof FloatType) {
            return new FloatProcessor((int)op.dimension(0), (int)op.dimension(1));
        }
        throw new RuntimeException("Can't transform type to ImageJ primitives");
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
