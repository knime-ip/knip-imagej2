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
package org.knime.knip.imagej1.plugin.reg;

/*====================================================================
 | Version: July 7, 2011
 \===================================================================*/

/*====================================================================
 | EPFL/STI/IMT/LIB/BM.4.137
 | Philippe Thevenaz
 | Station 17
 | CH-1015 Lausanne VD
 | Switzerland
 |
 | phone (CET): +41(21)693.51.61
 | fax: +41(21)693.37.01
 | RFC-822: philippe.thevenaz@epfl.ch
 | X-400: /C=ch/A=400net/P=switch/O=epfl/S=thevenaz/G=philippe/
 | URL: http://bigwww.epfl.ch/
 \===================================================================*/

/*====================================================================
 | This work is based on the following paper:
 |
 | P. Thevenaz, U.E. Ruttimann, M. Unser
 | A Pyramid Approach to Subpixel Registration Based on Intensity
 | IEEE Transactions on Image Processing
 | vol. 7, no. 1, pp. 27-41, January 1998.
 |
 | This paper is available on-line at
 | http://bigwww.epfl.ch/publications/thevenaz9801.html
 |
 | Other relevant on-line publications are available at
 | http://bigwww.epfl.ch/publications/
 \===================================================================*/

/*====================================================================
 | Additional help available at
 | http://bigwww.epfl.ch/thevenaz/stackreg/
 | Ancillary TurboReg_ plugin available at
 | http://bigwww.epfl.ch/thevenaz/turboreg/
 |
 | You'll be free to use this software for research purposes, but you
 | should not redistribute it without our consent. In addition, we expect
 | you to include a citation or acknowledgment whenever you present or
 | publish results that are based on it.
 \===================================================================*/

// ImageJ
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ShortProcessor;

import org.knime.knip.imagej1.plugin.reg.TurboReg_.TransformationType;

/*====================================================================
 |	StackReg_
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class StackReg_ implements PlugIn

{ /* class StackReg_ */
    private TurboReg_ m_turboReg;

    private final TransformationType m_transformation;

    double[][] initSourcePoints = new double[turboRegPointHandler.NUM_POINTS][2];

    double[][] initTargetPoints = new double[turboRegPointHandler.NUM_POINTS][2];

    protected StackReg_(final TransformationType type) {
        m_transformation = type;
    }

    /*
     * ....................................................................
     * PlugIn methods
     * ....................................................................
     */
    /*------------------------------------------------------------------*/
    @Override
    public void run(final String arg) {
        final ImagePlus imp = WindowManager.getCurrentImage();
        m_turboReg = new TurboReg_();
        // if (imp == null) {
        // IJ.error("No image available");
        // return;
        // }
        // if (imp.getStack().isRGB() || imp.getStack().isHSB()) {
        // IJ.error("Unable to process either RGB or HSB stacks");
        // return;
        // }
        // GenericDialog gd = new GenericDialog("StackReg");

        // gd.addChoice("Transformation:", transformationItem,
        // "Rigid Body");
        // gd.addCheckbox("Credits", false);
        // gd.showDialog();
        // if (gd.wasCanceled()) {
        // return;
        // }

        final int width = imp.getWidth();
        final int height = imp.getHeight();
        final int targetSlice = 0;
        final double[][] globalTransform = {{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}};
        double[][] anchorPoints = null;

        switch (m_transformation) {
            case TRANSLATION: {

                initSourcePoints[0][1] = height / 2;
                initSourcePoints[0][0] = width / 2;
                initTargetPoints[0][1] = height / 2;
                initTargetPoints[0][0] = width / 2;

                anchorPoints = new double[1][3];
                anchorPoints[0][0] = (width / 2);
                anchorPoints[0][1] = (height / 2);
                anchorPoints[0][2] = 1.0;
                break;
            }
            case RIGID_BODY: {

                initSourcePoints[0][1] = height / 2;
                initSourcePoints[0][0] = width / 2;
                initTargetPoints[0][1] = height / 2;
                initTargetPoints[0][0] = width / 2;

                initSourcePoints[1][1] = height / 4;
                initSourcePoints[1][0] = width / 2;
                initTargetPoints[1][1] = height / 4;
                initTargetPoints[1][0] = width / 2;

                initSourcePoints[2][1] = ((3 * height) / 4);
                initSourcePoints[2][0] = width / 2;
                initTargetPoints[2][1] = ((3 * height) / 4);
                initTargetPoints[2][0] = width / 2;

                anchorPoints = new double[3][3];
                anchorPoints[0][0] = (width / 2);
                anchorPoints[0][1] = (height / 2);
                anchorPoints[0][2] = 1.0;
                anchorPoints[1][0] = (width / 2);
                anchorPoints[1][1] = (height / 4);
                anchorPoints[1][2] = 1.0;
                anchorPoints[2][0] = (width / 2);
                anchorPoints[2][1] = ((3 * height) / 4);
                anchorPoints[2][2] = 1.0;
                break;
            }
            case SCALED_ROTATION: {

                initSourcePoints[0][1] = height / 2;
                initSourcePoints[0][0] = width / 4;
                initTargetPoints[0][1] = height / 2;
                initTargetPoints[0][0] = width / 4;

                initSourcePoints[1][1] = height / 2;
                initSourcePoints[1][0] = ((3 * width) / 4);
                initTargetPoints[1][1] = height / 2;
                initTargetPoints[1][0] = ((3 * width) / 4);

                anchorPoints = new double[2][3];
                anchorPoints[0][0] = (width / 4);
                anchorPoints[0][1] = (height / 2);
                anchorPoints[0][2] = 1.0;
                anchorPoints[1][0] = ((3 * width) / 4);
                anchorPoints[1][1] = (height / 2);
                anchorPoints[1][2] = 1.0;
                break;
            }
            case AFFINE: {

                initSourcePoints[0][1] = height / 4;
                initSourcePoints[0][0] = width / 2;
                initTargetPoints[0][1] = height / 4;
                initTargetPoints[0][0] = width / 2;

                initSourcePoints[1][1] = ((3 * height) / 4);
                initSourcePoints[1][0] = width / 4;
                initTargetPoints[1][1] = ((3 * height) / 4);
                initTargetPoints[1][0] = width / 4;

                initSourcePoints[1][1] = ((3 * height) / 4);
                initSourcePoints[1][0] = ((3 * width) / 4);
                initTargetPoints[1][1] = ((3 * height) / 4);
                initTargetPoints[1][0] = ((3 * width) / 4);

                anchorPoints = new double[3][3];
                anchorPoints[0][0] = (width / 2);
                anchorPoints[0][1] = (height / 4);
                anchorPoints[0][2] = 1.0;
                anchorPoints[1][0] = (width / 4);
                anchorPoints[1][1] = ((3 * height) / 4);
                anchorPoints[1][2] = 1.0;
                anchorPoints[2][0] = ((3 * width) / 4);
                anchorPoints[2][1] = ((3 * height) / 4);
                anchorPoints[2][2] = 1.0;
                break;
            }
            default: {
                IJ.error("Unexpected transformation");
                return;
            }
        }
        ImagePlus source = null;
        ImagePlus target = null;
        final double[] colorWeights = null;
        switch (imp.getType()) {
            case ImagePlus.GRAY8: {
                target =
                        new ImagePlus("StackRegTarget", new ByteProcessor(width, height, new byte[width * height], imp
                                .getProcessor().getColorModel()));
                target.getProcessor().copyBits(imp.getProcessor(), 0, 0, Blitter.COPY);
                break;
            }
            case ImagePlus.GRAY16: {
                target =
                        new ImagePlus("StackRegTarget", new ShortProcessor(width, height, new short[width * height],
                                imp.getProcessor().getColorModel()));
                target.getProcessor().copyBits(imp.getProcessor(), 0, 0, Blitter.COPY);
                break;
            }
            case ImagePlus.GRAY32: {
                target =
                        new ImagePlus("StackRegTarget", new FloatProcessor(width, height, new float[width * height],
                                imp.getProcessor().getColorModel()));
                target.getProcessor().copyBits(imp.getProcessor(), 0, 0, Blitter.COPY);
                break;
            }
            default: {
                IJ.error("Unexpected image type");
                return;
            }
        }
        for (int s = targetSlice - 1; (0 < s); s--) {
            source =
                    registerSlice(source, target, imp, width, height, m_transformation, globalTransform, anchorPoints,
                                  colorWeights, s);
            if (source == null) {
                imp.setSlice(targetSlice);
                return;
            }
        }
        for (int s = targetSlice + 1; (s <= imp.getStackSize()); s++) {
            source =
                    registerSlice(source, target, imp, width, height, m_transformation, globalTransform, anchorPoints,
                                  colorWeights, s);
            if (source == null) {
                imp.setSlice(targetSlice);
                return;
            }
        }
        imp.setSlice(targetSlice);
    } /* end run */

    /*------------------------------------------------------------------*/
    private double[][] getTransformationMatrix(final double[][] fromCoord, final double[][] toCoord,
                                               final TransformationType transformation) {
        final double[][] matrix = new double[3][3];
        switch (transformation) {
            case TRANSLATION: {
                matrix[0][0] = 1.0;
                matrix[0][1] = 0.0;
                matrix[0][2] = toCoord[0][0] - fromCoord[0][0];
                matrix[1][0] = 0.0;
                matrix[1][1] = 1.0;
                matrix[1][2] = toCoord[0][1] - fromCoord[0][1];
                break;
            }
            case RIGID_BODY: {
                final double angle =
                        Math.atan2(fromCoord[2][0] - fromCoord[1][0], fromCoord[2][1] - fromCoord[1][1])
                                - Math.atan2(toCoord[2][0] - toCoord[1][0], toCoord[2][1] - toCoord[1][1]);
                final double c = Math.cos(angle);
                final double s = Math.sin(angle);
                matrix[0][0] = c;
                matrix[0][1] = -s;
                matrix[0][2] = (toCoord[0][0] - (c * fromCoord[0][0])) + (s * fromCoord[0][1]);
                matrix[1][0] = s;
                matrix[1][1] = c;
                matrix[1][2] = toCoord[0][1] - (s * fromCoord[0][0]) - (c * fromCoord[0][1]);
                break;
            }
            case SCALED_ROTATION: {
                final double[][] a = new double[3][3];
                final double[] v = new double[3];
                a[0][0] = fromCoord[0][0];
                a[0][1] = fromCoord[0][1];
                a[0][2] = 1.0;
                a[1][0] = fromCoord[1][0];
                a[1][1] = fromCoord[1][1];
                a[1][2] = 1.0;
                a[2][0] = (fromCoord[0][1] - fromCoord[1][1]) + fromCoord[1][0];
                a[2][1] = (fromCoord[1][0] + fromCoord[1][1]) - fromCoord[0][0];
                a[2][2] = 1.0;
                invertGauss(a);
                v[0] = toCoord[0][0];
                v[1] = toCoord[1][0];
                v[2] = (toCoord[0][1] - toCoord[1][1]) + toCoord[1][0];
                for (int i = 0; (i < 3); i++) {
                    matrix[0][i] = 0.0;
                    for (int j = 0; (j < 3); j++) {
                        matrix[0][i] += a[i][j] * v[j];
                    }
                }
                v[0] = toCoord[0][1];
                v[1] = toCoord[1][1];
                v[2] = (toCoord[1][0] + toCoord[1][1]) - toCoord[0][0];
                for (int i = 0; (i < 3); i++) {
                    matrix[1][i] = 0.0;
                    for (int j = 0; (j < 3); j++) {
                        matrix[1][i] += a[i][j] * v[j];
                    }
                }
                break;
            }
            case AFFINE: {
                final double[][] a = new double[3][3];
                final double[] v = new double[3];
                a[0][0] = fromCoord[0][0];
                a[0][1] = fromCoord[0][1];
                a[0][2] = 1.0;
                a[1][0] = fromCoord[1][0];
                a[1][1] = fromCoord[1][1];
                a[1][2] = 1.0;
                a[2][0] = fromCoord[2][0];
                a[2][1] = fromCoord[2][1];
                a[2][2] = 1.0;
                invertGauss(a);
                v[0] = toCoord[0][0];
                v[1] = toCoord[1][0];
                v[2] = toCoord[2][0];
                for (int i = 0; (i < 3); i++) {
                    matrix[0][i] = 0.0;
                    for (int j = 0; (j < 3); j++) {
                        matrix[0][i] += a[i][j] * v[j];
                    }
                }
                v[0] = toCoord[0][1];
                v[1] = toCoord[1][1];
                v[2] = toCoord[2][1];
                for (int i = 0; (i < 3); i++) {
                    matrix[1][i] = 0.0;
                    for (int j = 0; (j < 3); j++) {
                        matrix[1][i] += a[i][j] * v[j];
                    }
                }
                break;
            }
            default: {
                IJ.error("Unexpected transformation");
            }
        }
        matrix[2][0] = 0.0;
        matrix[2][1] = 0.0;
        matrix[2][2] = 1.0;
        return (matrix);
    } /* end getTransformationMatrix */

    /*------------------------------------------------------------------*/
    private void invertGauss(final double[][] matrix) {
        final int n = matrix.length;
        final double[][] inverse = new double[n][n];
        for (int i = 0; (i < n); i++) {
            double max = matrix[i][0];
            double absMax = Math.abs(max);
            for (int j = 0; (j < n); j++) {
                inverse[i][j] = 0.0;
                if (absMax < Math.abs(matrix[i][j])) {
                    max = matrix[i][j];
                    absMax = Math.abs(max);
                }
            }
            inverse[i][i] = 1.0 / max;
            for (int j = 0; (j < n); j++) {
                matrix[i][j] /= max;
            }
        }
        for (int j = 0; (j < n); j++) {
            double max = matrix[j][j];
            double absMax = Math.abs(max);
            int k = j;
            for (int i = j + 1; (i < n); i++) {
                if (absMax < Math.abs(matrix[i][j])) {
                    max = matrix[i][j];
                    absMax = Math.abs(max);
                    k = i;
                }
            }
            if (k != j) {
                final double[] partialLine = new double[n - j];
                final double[] fullLine = new double[n];
                System.arraycopy(matrix[j], j, partialLine, 0, n - j);
                System.arraycopy(matrix[k], j, matrix[j], j, n - j);
                System.arraycopy(partialLine, 0, matrix[k], j, n - j);
                System.arraycopy(inverse[j], 0, fullLine, 0, n);
                System.arraycopy(inverse[k], 0, inverse[j], 0, n);
                System.arraycopy(fullLine, 0, inverse[k], 0, n);
            }
            for (k = 0; (k <= j); k++) {
                inverse[j][k] /= max;
            }
            for (k = j + 1; (k < n); k++) {
                matrix[j][k] /= max;
                inverse[j][k] /= max;
            }
            for (int i = j + 1; (i < n); i++) {
                for (k = 0; (k <= j); k++) {
                    inverse[i][k] -= matrix[i][j] * inverse[j][k];
                }
                for (k = j + 1; (k < n); k++) {
                    matrix[i][k] -= matrix[i][j] * matrix[j][k];
                    inverse[i][k] -= matrix[i][j] * inverse[j][k];
                }
            }
        }
        for (int j = n - 1; (1 <= j); j--) {
            for (int i = j - 1; (0 <= i); i--) {
                for (int k = 0; (k <= j); k++) {
                    inverse[i][k] -= matrix[i][j] * inverse[j][k];
                }
                for (int k = j + 1; (k < n); k++) {
                    matrix[i][k] -= matrix[i][j] * matrix[j][k];
                    inverse[i][k] -= matrix[i][j] * inverse[j][k];
                }
            }
        }
        for (int i = 0; (i < n); i++) {
            System.arraycopy(inverse[i], 0, matrix[i], 0, n);
        }
    } /* end invertGauss */

    /*------------------------------------------------------------------*/

    /*------------------------------------------------------------------*/
    /**
     * @param colorWeights
     */
    private ImagePlus registerSlice(ImagePlus source, final ImagePlus target, final ImagePlus imp, final int width,
                                    final int height, final TransformationType transformation,
                                    final double[][] globalTransform, final double[][] anchorPoints,
                                    final double[] colorWeights, final int s) {
        imp.setSlice(s);

        double[][] sourcePoints = null;
        double[][] targetPoints = null;
        double[][] localTransform = null;
        switch (imp.getType()) {
            case ImagePlus.GRAY8: {
                source =
                        new ImagePlus("StackRegSource", new ByteProcessor(width, height, (byte[])imp.getProcessor()
                                .getPixels(), imp.getProcessor().getColorModel()));
                break;
            }
            case ImagePlus.GRAY16: {
                source =
                        new ImagePlus("StackRegSource", new ShortProcessor(width, height, (short[])imp.getProcessor()
                                .getPixels(), imp.getProcessor().getColorModel()));
                break;
            }
            case ImagePlus.GRAY32: {
                source =
                        new ImagePlus("StackRegSource", new FloatProcessor(width, height, (float[])imp.getProcessor()
                                .getPixels(), imp.getProcessor().getColorModel()));
                break;
            }
            default: {
                IJ.error("Unexpected image type");
                return (null);
            }
        }
        m_turboReg.run(source, target, transformation, initSourcePoints, initTargetPoints);
        target.setProcessor(null, source.getProcessor());
        sourcePoints = m_turboReg.getSourcePoints();
        targetPoints = m_turboReg.getTargetPoints();
        localTransform = getTransformationMatrix(targetPoints, sourcePoints, transformation);
        final double[][] rescued =
                {{globalTransform[0][0], globalTransform[0][1], globalTransform[0][2]},
                        {globalTransform[1][0], globalTransform[1][1], globalTransform[1][2]},
                        {globalTransform[2][0], globalTransform[2][1], globalTransform[2][2]}};
        for (int i = 0; (i < 3); i++) {
            for (int j = 0; (j < 3); j++) {
                globalTransform[i][j] = 0.0;
                for (int k = 0; (k < 3); k++) {
                    globalTransform[i][j] += localTransform[i][k] * rescued[k][j];
                }
            }
        }
        switch (imp.getType()) {
            case ImagePlus.GRAY8:
            case ImagePlus.GRAY16:
            case ImagePlus.GRAY32: {
                switch (transformation) {
                    case TRANSLATION: {
                        sourcePoints = new double[1][3];
                        for (int i = 0; (i < 3); i++) {
                            sourcePoints[0][i] = 0.0;
                            for (int j = 0; (j < 3); j++) {
                                sourcePoints[0][i] += globalTransform[i][j] * anchorPoints[0][j];
                            }
                        }

                        initSourcePoints[0][1] = sourcePoints[0][1];
                        initSourcePoints[0][0] = sourcePoints[0][0];

                        m_turboReg.transform(source, target, transformation, initSourcePoints, initTargetPoints);
                        break;
                    }
                    case RIGID_BODY: {

                        sourcePoints = new double[3][3];
                        for (int i = 0; (i < 3); i++) {
                            sourcePoints[0][i] = 0.0;
                            sourcePoints[1][i] = 0.0;
                            sourcePoints[2][i] = 0.0;
                            for (int j = 0; (j < 3); j++) {
                                sourcePoints[0][i] += globalTransform[i][j] * anchorPoints[0][j];
                                sourcePoints[1][i] += globalTransform[i][j] * anchorPoints[1][j];
                                sourcePoints[2][i] += globalTransform[i][j] * anchorPoints[2][j];
                            }
                        }

                        initSourcePoints[0][1] = sourcePoints[0][1];
                        initSourcePoints[0][0] = sourcePoints[0][0];

                        initSourcePoints[1][1] = sourcePoints[1][1];
                        initSourcePoints[1][0] = sourcePoints[1][0];

                        initSourcePoints[2][1] = sourcePoints[2][1];
                        initSourcePoints[2][0] = sourcePoints[2][0];

                        m_turboReg.transform(source, target, transformation, initSourcePoints, initTargetPoints);
                        break;
                    }
                    case SCALED_ROTATION: {
                        sourcePoints = new double[2][3];
                        for (int i = 0; (i < 3); i++) {
                            sourcePoints[0][i] = 0.0;
                            sourcePoints[1][i] = 0.0;
                            for (int j = 0; (j < 3); j++) {
                                sourcePoints[0][i] += globalTransform[i][j] * anchorPoints[0][j];
                                sourcePoints[1][i] += globalTransform[i][j] * anchorPoints[1][j];
                            }
                        }
                        initSourcePoints[0][1] = sourcePoints[0][1];
                        initSourcePoints[0][0] = sourcePoints[0][0];

                        initSourcePoints[1][1] = sourcePoints[1][1];
                        initSourcePoints[1][0] = sourcePoints[1][0];

                        m_turboReg.transform(source, target, transformation, initSourcePoints, initTargetPoints);
                        break;
                    }
                    case AFFINE: {
                        sourcePoints = new double[3][3];
                        for (int i = 0; (i < 3); i++) {
                            sourcePoints[0][i] = 0.0;
                            sourcePoints[1][i] = 0.0;
                            sourcePoints[2][i] = 0.0;
                            for (int j = 0; (j < 3); j++) {
                                sourcePoints[0][i] += globalTransform[i][j] * anchorPoints[0][j];
                                sourcePoints[1][i] += globalTransform[i][j] * anchorPoints[1][j];
                                sourcePoints[2][i] += globalTransform[i][j] * anchorPoints[2][j];
                            }
                        }

                        initSourcePoints[0][1] = sourcePoints[0][1];
                        initSourcePoints[0][0] = sourcePoints[0][0];

                        initSourcePoints[1][1] = sourcePoints[1][1];
                        initSourcePoints[1][0] = sourcePoints[1][0];

                        initSourcePoints[2][1] = sourcePoints[2][1];
                        initSourcePoints[2][0] = sourcePoints[2][0];

                        m_turboReg.transform(source, target, transformation, sourcePoints, initTargetPoints);
                        break;
                    }
                    default: {
                        IJ.error("Unexpected transformation");
                        return (null);
                    }
                }
                final ImagePlus transformedSource = m_turboReg.getTransformedImage();
                transformedSource.getStack().deleteLastSlice();
                switch (imp.getType()) {
                    case ImagePlus.GRAY8: {
                        transformedSource.getProcessor().setMinAndMax(0.0, 255.0);
                        final ImageConverter converter = new ImageConverter(transformedSource);
                        converter.convertToGray8();
                        break;
                    }
                    case ImagePlus.GRAY16: {
                        transformedSource.getProcessor().setMinAndMax(0.0, 65535.0);
                        final ImageConverter converter = new ImageConverter(transformedSource);
                        converter.convertToGray16();
                        break;
                    }
                    case ImagePlus.GRAY32: {
                        break;
                    }
                    default: {
                        IJ.error("Unexpected image type");
                        return (null);
                    }
                }
                imp.setProcessor(null, transformedSource.getProcessor());
                break;
            }
            default: {
                IJ.error("Unexpected image type");
                return (null);
            }
        }
        return (source);
    } /* end registerSlice */
} /* end class StackReg_ */
