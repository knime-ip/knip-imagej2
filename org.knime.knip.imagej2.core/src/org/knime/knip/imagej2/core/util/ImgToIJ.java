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

import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.meta.Axes;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

// TODO has to be replaced if imglib2 has this as fast routines
/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public final class ImgToIJ implements UnaryOutputOperation<ImgPlus<? extends RealType<?>>, ImagePlus> {

    @Override
    public final ImagePlus compute(final ImgPlus<? extends RealType<?>> op, final ImagePlus r) {
        float offset = 0;
        float scale = 1;
        if (op.firstElement() instanceof BitType) {
            scale = 255;
        }
        if (op.firstElement() instanceof ByteType) {
            offset = -Byte.MIN_VALUE;
        }
        if (op.firstElement() instanceof ShortType) {
            offset = -Short.MIN_VALUE;
        }
        final long[] dim = new long[op.numDimensions()];
        op.dimensions(dim);
        final long width = dim[0];
        final long height = dim[1];
        int x, y;
        dim[0] = 1;
        dim[1] = 1;
        final IntervalIterator ii = new IntervalIterator(dim);
        final RandomAccess<? extends RealType> ra = op.randomAccess();
        final ImageStack is = new ImageStack((int)op.dimension(0), (int)op.dimension(1));
        while (ii.hasNext()) {
            ii.fwd();
            //TODO: USe cursor. can be made faster with subset interval

            ra.setPosition(ii);

            final ImageProcessor ip = createImageProcessor(op);
            for (y = 0; y < height; y++) {
                ra.setPosition(y, 1);
                for (x = 0; x < width; x++) {
                    ra.setPosition(x, 0);
                    ip.setf(x, y, (ra.get().getRealFloat() + offset) * scale);
                }
            }
            is.addSlice("", ip);
        }

        int[] mapping = getMapping(op, dim);
        r.setStack(is, mapping[0], mapping[1], mapping[2]);
        return r;
    }

    private int[] getMapping(final ImgPlus<? extends RealType> img, final long[] dimensions) {
        int[] mappedSizes = new int[3];
        Arrays.fill(mappedSizes, 1);

        for (int d = 2; d < img.numDimensions(); d++) {
            if (img.axis(d).type().getLabel().equalsIgnoreCase(Axes.CHANNEL.getLabel())) {
                mappedSizes[0] = (int)img.dimension(d);
            } else if (img.axis(d).type().getLabel().equalsIgnoreCase(Axes.Z.getLabel())) {
                mappedSizes[1] = (int)img.dimension(d);
            } else if (img.axis(d).type().getLabel().equalsIgnoreCase(Axes.TIME.getLabel())) {
                mappedSizes[2] = (int)img.dimension(d);
            } else {
                throw new RuntimeException("Unknown dimension name. Only Z, Channel and Time are supported.");
            }
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
