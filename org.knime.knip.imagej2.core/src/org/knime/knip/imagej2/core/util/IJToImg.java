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
import ij.measure.Measurements;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * This class provides functionality to create an {@link Img} from an {@link ImagePlus}
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @param <T>
 */
public final class IJToImg<T extends RealType<T> & NativeType<T>> implements
        UnaryOutputOperation<ImagePlus, ImgPlus<T>> {

    /**
     * Creates Bit-, UnsignedByte-, UnsignedShort- or FloatType depending on the ImagePlus bit depth.
     *
     * @param op query {@link ImagePlus} for which the matching type will be determined
     * @return the matching type
     */
    public static final RealType<?> createMatchingType(final ImagePlus op) {
        switch (op.getBitDepth()) {
            case 8:
                final ImageStatistics is = op.getStatistics(Measurements.AREA);
                if ((is.histogram[0] + is.histogram[is.histogram.length - 1]) == is.area) {
                    return new BitType();
                }
                return new UnsignedByteType();
            case 16:
                return new UnsignedShortType();
            case 32:
                return new FloatType();
            default:
                throw new RuntimeException("Unknown ImageJ bit depth.");
        }
    }

    private final T m_type;

    private final boolean m_scale;

    private final int m_numDimensions;

    /**
     * @param type type of the input
     */
    public IJToImg(final T type) {
        this(type, false, -1);
    }

    /**
     * @param type the type of the IJ
     * @param scale scaling
     * @param numDimensions number of dimensions
     */
    public IJToImg(final T type, final boolean scale, final int numDimensions) {
        m_type = type;
        m_scale = scale;
        m_numDimensions = numDimensions;
    }

    @Override
    public final ImgPlus<T> compute(final ImagePlus op, final ImgPlus<T> r) {
        final IterableInterval<T> permuted = Views.iterable(ImgToIJ.extendAndPermute(r));
        final Cursor<T> cur;
        if (permuted.iterationOrder().equals(r.iterationOrder())) {
            cur = r.cursor();
        } else {
            cur = permuted.cursor();
        }

        final ImageStatistics is = op.getStatistics(Measurements.MIN_MAX);
        final long[] dim = new long[permuted.numDimensions()];
        permuted.dimensions(dim);
        final long width = dim[0];
        final long height = dim[1];
        int x, y;
        dim[0] = 1;
        dim[1] = 1;
        final IntervalIterator ii = new IntervalIterator(dim);
        final double raMin = r.firstElement().getMinValue();
        final double raMax = r.firstElement().getMaxValue();
        final double scale = (is.max - is.min) / (raMax - raMin);
        float v;

        while (ii.hasNext()) {
            ii.fwd();
            op.setPosition(ii.getIntPosition(2) + 1, ii.getIntPosition(3) + 1, ii.getIntPosition(4) + 1);

            final ImageProcessor ip = op.getProcessor();

            for (y = 0; y < height; y++) {
                for (x = 0; x < width; x++) {
                    cur.fwd();
                    v = ip.getf(x, y);
                    if (m_scale) {
                        cur.get().setReal(((v - is.min) / scale) + raMin);
                    } else if (v < raMin) {
                        cur.get().setReal(raMin);
                    } else if (v > raMax) {
                        cur.get().setReal(raMax);
                    } else {
                        cur.get().setReal(v);
                    }
                }
            }
        }
        return r;
    }

    @Override
    public UnaryOutputOperation<ImagePlus, ImgPlus<T>> copy() {
        return new IJToImg<T>(m_type);
    }

    @Override
    public UnaryObjectFactory<ImagePlus, ImgPlus<T>> bufferFactory() {
        return new UnaryObjectFactory<ImagePlus, ImgPlus<T>>() {

            @Override
            public ImgPlus<T> instantiate(final ImagePlus op) {
                int nDim = m_numDimensions;
                if (m_numDimensions < 0) {
                    nDim = 5;
                }
                final long[] dim = new long[nDim];
                int i;
                for (i = 0; i < dim.length; i++) {
                    dim[i] = op.getDimensions()[i];
                }
                for (; i < op.getNDimensions(); i++) {
                    if (op.getDimensions()[i] > 1) {
                        throw new IllegalArgumentException("Too less dimensions");
                    }
                }
                return new ImgPlus<T>(new ArrayImgFactory<T>().create(dim, m_type), op.getTitle(),
                        ImgToIJ.DEFAULT_ORDER);
            }
        };
    }
}
