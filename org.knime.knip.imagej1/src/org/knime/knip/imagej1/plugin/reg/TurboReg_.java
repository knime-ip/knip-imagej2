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
 | Philippe Thevenaz
 | EPFL/STI/IMT/LIB/BM.4.137
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
 | Additional help available at http://bigwww.epfl.ch/thevenaz/turboreg/
 |
 | You'll be free to use this software for research purposes, but you
 | should not redistribute it without our consent. In addition, we expect
 | you to include a citation or acknowledgment whenever you present or
 | publish results that are based on it.
 \===================================================================*/

// ImageJ
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.PolygonRoi;
import ij.gui.Toolbar;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.process.FloatProcessor;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Stack;

import org.knime.knip.imagej1.plugin.reg.TurboReg_.TransformationType;

/*====================================================================
 |	TurboReg_
 \===================================================================*/

/*********************************************************************
 * This class is the only one that is accessed directly by imageJ; it launches a modeless dialog and dies. Note that it
 * implements <code>PlugIn</code> rather than <code>PlugInFilter</code>.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class TurboReg_

{

    public enum TransformationType {
        GENERIC_TRANSFORMATION(-1), TRANSLATION(2), RIGID_BODY(3), SCALED_ROTATION(4), AFFINE(6);

        private int m_numParam;

        private TransformationType(final int numParam) {
            setNumParam(numParam);
        }

        public int getNumParam() {
            return m_numParam;
        }

        public void setNumParam(final int numParam) {
            m_numParam = numParam;
        }
    };

    /*
     * ....................................................................
     * private variables
     * ....................................................................
     */
    private double[][] sourcePoints = new double[turboRegPointHandler.NUM_POINTS][2];

    private double[][] targetPoints = new double[turboRegPointHandler.NUM_POINTS][2];

    private ImagePlus transformedImage = null;

    public void run(final ImagePlus source, final ImagePlus target, final TransformationType type,
                    final double[][] sourcePoints, final double[][] targetPoints) {
        final TransformationType transformation = type;

        this.sourcePoints = sourcePoints;
        this.targetPoints = targetPoints;

        transformedImage = alignImages(source, target, transformation);
    }

    /**
     * @param target
     */
    public void transform(final ImagePlus source, final ImagePlus target, final TransformationType type,
                          final double[][] sourcePoints, final double[][] targetPoints) {

        this.sourcePoints = sourcePoints;
        this.targetPoints = targetPoints;

        transformedImage = transformImage(source, source.getWidth(), source.getHeight(), type);
    }

    /*
     * ....................................................................
     * public methods
     * ....................................................................
     */
    /*********************************************************************
     * Accessor method for the <code>(double[][])sourcePoints</code> variable. This variable is valid only after a call
     * to <code>run</code> with the option <code>-align</code> has been issued. What is returned is a two-dimensional
     * array of type <code>double[][]</code> that contains coordinates from the image <code>sourceFilename</code>. These
     * coordinates are given relative to the original image, before that the cropping described by the
     * <code>sourceCropLeft</code>, <code>sourceCropTop</code>, <code>sourceCropRight</code>, and
     * <code>sourceCropBottom</code> has been applied. These coordinates match those available from
     * <code>targetPoints</code>. The total number of coordinates, equal to <code>sourcePoints[*].length</code>, is
     * given by the constant <code>turboRegPointHandler.NUM_POINTS</code> which corresponds to four coordinates in the
     * present version. The second index gives the horizontal component for <code>[0]</code> and the vertical component
     * for <code>[1]</code>. The number of <i>useful</i> coordinates depends on the specific transformation for which
     * the alignment has been performed: translation (1), scaled rotation (2), rotation (3), affine transformation (3),
     * and bilinear transformation (4).
     *
     * @see TurboReg_#run
     * @see TurboReg_#getTargetPoints
     ********************************************************************/
    public double[][] getSourcePoints() {
        return (sourcePoints);
    } /* end getSourcePoints */

    /*********************************************************************
     * Accessor method for the <code>(double[][])targetPoints</code> variable. This variable is valid only after a call
     * to <code>run</code> with the option <code>-align</code> has been issued. What is returned is a two-dimensional
     * array of type <code>double[][]</code> that contains coordinates from the image <code>targetFilename</code>. These
     * coordinates are given relative to the original image, before that the cropping described by the
     * <code>targetCropLeft</code>, <code>targetCropTop</code>, <code>targetCropRight</code>, and
     * <code>targetCropBottom</code> has been applied. These coordinates match those available from
     * <code>sourcePoints</code>. The total number of coordinates, equal to <code>targetPoints[*].length</code>, is
     * given by the constant <code>turboRegPointHandler.NUM_POINTS</code> which corresponds to four coordinates in the
     * present version. The second index gives the horizontal component for <code>[0]</code> and the vertical component
     * for <code>[1]</code>. The number of <i>useful</i> coordinates depends on the specific transformation for which
     * the alignment has been performed: translation (1), scaled rotation (2), rotation (3), affine transformation (3),
     * and bilinear transformation (4).
     *
     * @see TurboReg_#run
     * @see TurboReg_#getSourcePoints
     ********************************************************************/
    public double[][] getTargetPoints() {
        return (targetPoints);
    } /* end getTargetPoints */

    /*********************************************************************
     * Accessor method for the <code>(ImagePlus)transformedImage</code> variable. This variable is valid only after a
     * call to <code>run</code> with the option <code>-transform</code> has been issued. What is returned is an
     * <code>ImagePlus</code> object of the size described by the <code>outputWidth</code> and <code>outputHeight</code>
     * parameters of the call to the <code>run</code> method of <code>TurboReg_</code>.
     *
     * @see TurboReg_#run
     ********************************************************************/
    public ImagePlus getTransformedImage() {
        return (transformedImage);
    } /* end getTransformedImage */

    /*
     * ....................................................................
     * private methods
     * ....................................................................
     */
    /*------------------------------------------------------------------*/
    private ImagePlus alignImages(final ImagePlus source, final ImagePlus target,
                                  final TransformationType transformation) {
        if ((source.getType() != ImagePlus.GRAY16) && (source.getType() != ImagePlus.GRAY32)
                && ((source.getType() != ImagePlus.GRAY8) || source.getStack().isRGB() || source.getStack().isHSB())) {
            IJ.error(source.getTitle() + " should be grayscale (8, 16, or 32 bit)");
            return (null);
        }
        if ((target.getType() != ImagePlus.GRAY16) && (target.getType() != ImagePlus.GRAY32)
                && ((target.getType() != ImagePlus.GRAY8) || target.getStack().isRGB() || target.getStack().isHSB())) {
            IJ.error(target.getTitle() + " should be grayscale (8, 16, or 32 bit)");
            return (null);
        }

        source.setRoi(0, 0, source.getWidth(), source.getHeight());
        source.setRoi(0, 0, target.getWidth(), target.getHeight());

        source.setSlice(1);
        target.setSlice(1);
        final ImagePlus sourceImp = new ImagePlus("source", source.getProcessor().crop());
        final ImagePlus targetImp = new ImagePlus("target", target.getProcessor().crop());
        final turboRegImage sourceImg = new turboRegImage(sourceImp, transformation, false);
        final turboRegImage targetImg = new turboRegImage(targetImp, transformation, true);
        final int pyramidDepth =
                getPyramidDepth(sourceImp.getWidth(), sourceImp.getHeight(), targetImp.getWidth(),
                                targetImp.getHeight());
        sourceImg.setPyramidDepth(pyramidDepth);
        targetImg.setPyramidDepth(pyramidDepth);
        sourceImg.getThread().start();
        targetImg.getThread().start();
        if (2 <= source.getStackSize()) {
            source.setSlice(2);
        }
        if (2 <= target.getStackSize()) {
            target.setSlice(2);
        }
        final ImagePlus sourceMskImp = new ImagePlus("source mask", source.getProcessor().crop());
        final ImagePlus targetMskImp = new ImagePlus("target mask", target.getProcessor().crop());
        final turboRegMask sourceMsk = new turboRegMask(sourceMskImp);
        final turboRegMask targetMsk = new turboRegMask(targetMskImp);
        source.setSlice(1);
        target.setSlice(1);
        if (source.getStackSize() < 2) {
            sourceMsk.clearMask();
        }
        if (target.getStackSize() < 2) {
            targetMsk.clearMask();
        }
        sourceMsk.setPyramidDepth(pyramidDepth);
        targetMsk.setPyramidDepth(pyramidDepth);
        sourceMsk.getThread().start();
        targetMsk.getThread().start();

        final turboRegPointHandler sourcePh =
                (null == sourceImp.getWindow()) ? (new turboRegPointHandler(transformation, sourceImp))
                        : (new turboRegPointHandler(sourceImp, transformation));
        final turboRegPointHandler targetPh =
                (null == sourceImp.getWindow()) ? (new turboRegPointHandler(transformation, targetImp))
                        : (new turboRegPointHandler(targetImp, transformation));
        sourcePh.setPoints(sourcePoints);
        targetPh.setPoints(targetPoints);
        try {
            sourceMsk.getThread().join();
            targetMsk.getThread().join();
            sourceImg.getThread().join();
            targetImg.getThread().join();
        } catch (final InterruptedException e) {
            IJ.log("Unexpected interruption exception " + e.getMessage());
        }
        final turboRegFinalAction finalAction =
                new turboRegFinalAction(sourceImg, sourceMsk, sourcePh, targetImg, targetMsk, targetPh, transformation);
        finalAction.getThread().start();
        try {
            finalAction.getThread().join();
        } catch (final InterruptedException e) {
            IJ.log("Unexpected interruption exception " + e.getMessage());
        }
        sourcePoints = sourcePh.getPoints();
        targetPoints = targetPh.getPoints();
        final ResultsTable table = Analyzer.getResultsTable();
        table.reset();
        switch (transformation) {
            case TRANSLATION: {
                table.incrementCounter();
                table.addValue("sourceX", sourcePoints[0][0]);
                table.addValue("sourceY", sourcePoints[0][1]);
                table.addValue("targetX", targetPoints[0][0]);
                table.addValue("targetY", targetPoints[0][1]);
                break;
            }
            case SCALED_ROTATION: {
                for (int k = 0; (k < 2); k++) {
                    table.incrementCounter();
                    table.addValue("sourceX", sourcePoints[k][0]);
                    table.addValue("sourceY", sourcePoints[k][1]);
                    table.addValue("targetX", targetPoints[k][0]);
                    table.addValue("targetY", targetPoints[k][1]);
                }
                break;
            }
            case RIGID_BODY:
            case AFFINE: {
                for (int k = 0; (k < 3); k++) {
                    table.incrementCounter();
                    table.addValue("sourceX", sourcePoints[k][0]);
                    table.addValue("sourceY", sourcePoints[k][1]);
                    table.addValue("targetX", targetPoints[k][0]);
                    table.addValue("targetY", targetPoints[k][1]);
                }
                break;
            }
        }
        source.killRoi();
        target.killRoi();
        return (transformImage(source, target.getWidth(), target.getHeight(), transformation));
    } /* end alignImages */

    /*------------------------------------------------------------------*/
    private int getPyramidDepth(int sw, int sh, int tw, int th) {
        int pyramidDepth = 1;
        while (((2 * 12) <= sw) && ((2 * 12) <= sh) && ((2 * 12) <= tw) && ((2 * 12) <= th)) {
            sw /= 2;
            sh /= 2;
            tw /= 2;
            th /= 2;
            pyramidDepth++;
        }
        return (pyramidDepth);
    } /* end getPyramidDepth */

    // /*------------------------------------------------------------------*/
    // private String[] getTokens(String options) {
    // final String fileSeparator = System.getProperty("file.separator");
    // if (fileSeparator.equals("\\")) {
    // options = options.replaceAll("\\\\", "/");
    // } else {
    // options = options.replaceAll(fileSeparator, "/");
    // }
    // String[] token = new String[0];
    // final StringReader sr = new StringReader(options);
    // final StreamTokenizer st = new StreamTokenizer(sr);
    // st.resetSyntax();
    // st.whitespaceChars(0, ' ');
    // st.wordChars('!', 255);
    // st.quoteChar('\"');
    // final Vector<String> v = new Vector<String>();
    // try {
    // while (st.nextToken() != StreamTokenizer.TT_EOF) {
    // v.add(new String(st.sval));
    // }
    // } catch (IOException e) {
    // IJ.log("IOException exception " + e.getMessage());
    // return (token);
    // }
    // token = v.toArray(token);
    // return (token);
    // } /* end getTokens */
    //
    // /*------------------------------------------------------------------*/
    // private int getTransformation(final String token) {
    // if (token.equals("-translation")) {
    // return (TRANSLATION);
    // } else if (token.equals("-rigidBody")) {
    // return (RIGID_BODY);
    // } else if (token.equals("-scaledRotation")) {
    // return (SCALED_AFFINE);
    // } else if (token.equals("-affine")) {
    // return (AFFINE);
    // } else if (token.equals("-bilinear")) {
    // return (BILINEAR);
    // } else {
    // return (GENERIC_TRANSFORMATION);
    // }
    // } /* end getTransformation */

    /*------------------------------------------------------------------*/
    private ImagePlus transformImage(final ImagePlus source, final int width, final int height,
                                     final TransformationType transformation) {
        if ((source.getType() != ImagePlus.GRAY16) && (source.getType() != ImagePlus.GRAY32)
                && ((source.getType() != ImagePlus.GRAY8) || source.getStack().isRGB() || source.getStack().isHSB())) {
            IJ.error(source.getTitle() + " should be grayscale (8, 16, or 32 bit)");
            return (null);
        }
        source.setSlice(1);
        final turboRegImage sourceImg = new turboRegImage(source, TransformationType.GENERIC_TRANSFORMATION, false);
        sourceImg.getThread().start();
        if (2 <= source.getStackSize()) {
            source.setSlice(2);
        }
        final turboRegMask sourceMsk = new turboRegMask(source);
        source.setSlice(1);
        if (source.getStackSize() < 2) {
            sourceMsk.clearMask();
        }
        final turboRegPointHandler sourcePh = new turboRegPointHandler(sourcePoints, transformation);
        final turboRegPointHandler targetPh = new turboRegPointHandler(targetPoints, transformation);
        try {
            sourceImg.getThread().join();
        } catch (final InterruptedException e) {
            IJ.log("Unexpected interruption exception " + e.getMessage());
        }
        final turboRegTransform regTransform =
                new turboRegTransform(sourceImg, sourceMsk, sourcePh, null, null, targetPh, transformation, false,
                        false);
        final ImagePlus transformedImage = regTransform.doFinalTransform(width, height);
        return (transformedImage);
    } /* end transformImage */

} /* end class TurboReg_ */

/*
 * ==================================================================== |
 * turboRegCredits
 * \===================================================================
 */

/*********************************************************************
 * This class creates the credits dialog.
 ********************************************************************/
class turboRegCredits extends Dialog

{ /* class turboRegCredits */

    /*
     * ....................................................................
     * private variables
     * ....................................................................
     */
    private static final long serialVersionUID = 1L;

    /*
     * ....................................................................
     * Container methods
     * ....................................................................
     */
    /*********************************************************************
     * Return some additional margin to the dialog, for aesthetic purposes. Necessary for the current MacOS X Java
     * version, lest the first item disappears from the frame.
     ********************************************************************/
    @Override
    public Insets getInsets() {
        return (new Insets(0, 20, 20, 20));
    } /* end getInsets */

    /*
     * ....................................................................
     * constructors
     * ....................................................................
     */
    /*********************************************************************
     * This constructor prepares the dialog box.
     *
     * @param parentWindow Parent window.
     ********************************************************************/
    public turboRegCredits(final Frame parentWindow) {
        super(parentWindow, "TurboReg", true);
        setLayout(new BorderLayout(0, 20));
        final Label separation = new Label("");
        final Panel buttonPanel = new Panel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        final Button doneButton = new Button("Done");
        doneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ae) {
                if (ae.getActionCommand().equals("Done")) {
                    dispose();
                }
            }
        });
        buttonPanel.add(doneButton);
        final TextArea text = new TextArea(26, 72);
        text.setEditable(false);
        text.append(" \n");
        text.append(" This TurboReg version is dated July 7, 2011\n");
        text.append(" \n");
        text.append(" ###\n");
        text.append(" \n");
        text.append(" This work is based on the following paper:\n");
        text.append("\n");
        text.append(" P. Th\u00E9venaz, U.E. Ruttimann, M. Unser\n");
        text.append(" A Pyramid Approach to Subpixel Registration Based on Intensity\n");
        text.append(" IEEE Transactions on Image Processing\n");
        text.append(" vol. 7, no. 1, pp. 27-41, January 1998.\n");
        text.append("\n");
        text.append(" This paper is available on-line at\n");
        text.append(" http://bigwww.epfl.ch/publications/thevenaz9801.html\n");
        text.append("\n");
        text.append(" Other relevant on-line publications are available at\n");
        text.append(" http://bigwww.epfl.ch/publications/\n");
        text.append("\n");
        text.append(" Additional help available at\n");
        text.append(" http://bigwww.epfl.ch/thevenaz/turboreg/\n");
        text.append("\n");
        text.append(" You'll be free to use this software for research purposes, but\n");
        text.append(" you should not redistribute it without our consent. In addition,\n");
        text.append(" we expect you to include a citation or acknowledgment whenever\n");
        text.append(" you present or publish results that are based on it.\n");
        add("North", separation);
        add("Center", text);
        add("South", buttonPanel);
        pack();
    } /* end turboRegCredits */

} /* end class turboRegCredits */

/*
 * ==================================================================== |
 * turboRegFinalAction
 * \===================================================================
 */

/*********************************************************************
 * The purpose of this class is to allow access to the progress bar, since it is denied to the
 * <code>turboRegDialog</code> class. It proceeds by wrapping <code>turboRegDialog</code> inside a thread that is under
 * the main event loop control.
 ********************************************************************/
class turboRegFinalAction implements Runnable

{ /* class turboRegFinalAction */

    /*
     * ....................................................................
     * public variables
     * ....................................................................
     */
    /*********************************************************************
     * Automatic registration: the initial source landmarks are refined to minimize the mean-square error.
     ********************************************************************/
    public static final int AUTOMATIC = 1;

    /*********************************************************************
     * Manual registration: the initial source landmarks are used <i>as is</i> to produce the output image.
     ********************************************************************/
    public static final int MANUAL = 2;

    /*********************************************************************
     * Batch registration: each slice of the source image is registered to the target image.
     ********************************************************************/
    public static final int BATCH = 3;

    /*
     * ....................................................................
     * private variables
     * ....................................................................
     */
    private final Thread t;

    private volatile ImagePlus sourceImp;

    private volatile ImagePlus targetImp;

    private volatile turboRegImage sourceImg;

    private volatile turboRegImage targetImg;

    private volatile turboRegMask sourceMsk;

    private volatile turboRegMask targetMsk;

    private volatile turboRegPointHandler sourcePh;

    private volatile turboRegPointHandler targetPh;

    private volatile int operation;

    private volatile int pyramidDepth;

    private volatile TransformationType transformation;

    private volatile boolean accelerated;

    /*
     * ....................................................................
     * Runnable methods
     * ....................................................................
     */
    /*********************************************************************
     * Start under the control of the main event loop, pause as long as the dialog event loop is active, and resume
     * processing when <code>turboRegDialog</code> finally dies.
     ********************************************************************/
    @Override
    public void run() {
        double[][] sourcePoints = null;
        double[][] targetPoints = null;
        turboRegTransform tt = null;
        ImageStack outputStack = null;
        ImagePlus outputImp = null;
        switch (operation) {
            case AUTOMATIC:
            case MANUAL: {
                tt =
                        new turboRegTransform(sourceImg, sourceMsk, sourcePh, targetImg, targetMsk, targetPh,
                                transformation, accelerated, false);
                if (operation == AUTOMATIC) {
                    tt.doRegistration();
                }

                outputImp = tt.doFinalTransform(targetImg.getWidth(), targetImg.getHeight());
                break;
            }
            case BATCH: {
                outputStack = new ImageStack(targetImg.getWidth(), targetImg.getHeight());
                for (int i = 0; (i < sourceImp.getStackSize()); i++) {
                    outputStack.addSlice("", new FloatProcessor(targetImg.getWidth(), targetImg.getHeight()));
                }
                outputImp = new ImagePlus("Registered", outputStack);
                if (transformation == TransformationType.RIGID_BODY) {
                    targetPoints = new double[transformation.getNumParam()][2];
                    sourcePoints = new double[transformation.getNumParam()][2];
                    for (int k = 0; (k < transformation.getNumParam()); k++) {
                        sourcePoints[k][0] = sourcePh.getPoints()[k][0];
                        sourcePoints[k][1] = sourcePh.getPoints()[k][1];
                        targetPoints[k][0] = targetPh.getPoints()[k][0];
                        targetPoints[k][1] = targetPh.getPoints()[k][1];
                    }
                } else {
                    targetPoints = new double[transformation.getNumParam() / 2][2];
                    sourcePoints = new double[transformation.getNumParam() / 2][2];
                    for (int k = 0; (k < (transformation.getNumParam() / 2)); k++) {
                        sourcePoints[k][0] = sourcePh.getPoints()[k][0];
                        sourcePoints[k][1] = sourcePh.getPoints()[k][1];
                        targetPoints[k][0] = targetPh.getPoints()[k][0];
                        targetPoints[k][1] = targetPh.getPoints()[k][1];
                    }
                }
                tt =
                        new turboRegTransform(sourceImg, null, sourcePh, targetImg, targetMsk, targetPh,
                                transformation, accelerated, false);
                if (2 <= sourceImp.getStackSize()) {
                    sourceImp.setSlice(2);
                    sourceImg = new turboRegImage(sourceImp, transformation, false);
                    sourceImg.setPyramidDepth(pyramidDepth);
                    sourceImg.getThread().start();
                }
                tt.doRegistration();
                tt.doBatchFinalTransform((float[])outputStack.getProcessor(1).getPixels());
                outputImp.setSlice(1);
                outputImp.getProcessor().resetMinAndMax();
                Runtime.getRuntime().gc();
                for (int i = 2; (i <= sourceImp.getStackSize()); i++) {
                    targetPh.setPoints(targetPoints);
                    sourcePh = new turboRegPointHandler(sourceImp, transformation);
                    sourcePh.setPoints(sourcePoints);
                    try {
                        sourceImg.getThread().join();
                    } catch (final InterruptedException e) {
                        IJ.log("Unexpected interruption exception " + e.getMessage());
                    }
                    tt =
                            new turboRegTransform(sourceImg, null, sourcePh, targetImg, targetMsk, targetPh,
                                    transformation, accelerated, false);
                    if (i < sourceImp.getStackSize()) {
                        sourceImp.setSlice(i + 1);
                        sourceImg = new turboRegImage(sourceImp, transformation, false);
                        sourceImg.setPyramidDepth(pyramidDepth);
                        sourceImg.getThread().start();
                    }
                    tt.doRegistration();
                    tt.doBatchFinalTransform((float[])outputStack.getProcessor(i).getPixels());
                    outputImp.setSlice(i);
                    outputImp.getProcessor().resetMinAndMax();
                    Runtime.getRuntime().gc();
                }
                sourceImp.killRoi();
                targetImp.killRoi();
                outputImp.setSlice(1);
                outputImp.getProcessor().resetMinAndMax();
                break;
            }
        }
    } /* end run */

    /*
     * ....................................................................
     * constructors
     * ....................................................................
     */
    /*********************************************************************
     * Start a thread under the control of the main event loop. This thread has access to the progress bar, while
     * methods called directly from within <code>turboRegDialog</code> do not because they are under the control of its
     * own event loop.
     *
     * @param dialog Gives access to some utility methods within <code>turboRegDialog</code>.
     ********************************************************************/
    public turboRegFinalAction() {
        t = new Thread(this);
    } /* end turboRegFinalAction */

    /*********************************************************************
     * Start a thread under the control of the main event loop.
     ********************************************************************/
    public turboRegFinalAction(final turboRegImage sourceImg, final turboRegMask sourceMsk,
                               final turboRegPointHandler sourcePh, final turboRegImage targetImg,
                               final turboRegMask targetMsk, final turboRegPointHandler targetPh,
                               final TransformationType transformation) {
        this.sourceImg = sourceImg;
        this.sourceMsk = sourceMsk;
        this.sourcePh = sourcePh;
        this.targetImg = targetImg;
        this.targetMsk = targetMsk;
        this.targetPh = targetPh;
        this.transformation = transformation;
        accelerated = false;
        operation = AUTOMATIC;
        t = new Thread(this);
    } /* end turboRegFinalAction */

    /*
     * ....................................................................
     * public methods
     * ....................................................................
     */
    /*********************************************************************
     * Return the thread associated with this <code>turboRegFinalAction</code> object.
     ********************************************************************/
    public Thread getThread() {
        return (t);
    } /* end getThread */

    /*********************************************************************
     * Pass parameter from <code>turboRegDialog</code> to <code>turboRegFinalAction</code>.
     *
     * @param saveOnExit
     ********************************************************************/
    public void setup(final turboRegImage sourceImg, final turboRegMask sourceMsk, final turboRegPointHandler sourcePh,
                      final turboRegImage targetImg, final turboRegMask targetMsk, final turboRegPointHandler targetPh,
                      final TransformationType transformation, final boolean accelerated, final boolean saveOnExit,
                      final int operation) {
        this.sourceImg = sourceImg;
        this.sourceMsk = sourceMsk;
        this.sourcePh = sourcePh;
        this.targetImg = targetImg;
        this.targetMsk = targetMsk;
        this.targetPh = targetPh;
        this.transformation = transformation;
        this.accelerated = accelerated;
        this.operation = operation;
    } /* end setup */

    /*********************************************************************
     * Pass parameter from <code>turboRegDialog</code> to <code>turboRegFinalAction</code>.
     *
     * @param sourceColorPlane
     * @param saveOnExit
     ********************************************************************/
    public void setup(final ImagePlus sourceImp, final turboRegImage sourceImg, final turboRegMask sourceMsk,
                      final turboRegPointHandler sourcePh, final int sourceColorPlane, final turboRegImage targetImg,
                      final turboRegMask targetMsk, final turboRegPointHandler targetPh,
                      final TransformationType transformation, final boolean accelerated, final boolean saveOnExit,
                      final int operation) {
        this.sourceImp = sourceImp;
        this.sourceImg = sourceImg;
        this.sourceMsk = sourceMsk;
        this.sourcePh = sourcePh;
        this.targetImg = targetImg;
        this.targetMsk = targetMsk;
        this.targetPh = targetPh;
        this.transformation = transformation;
        this.accelerated = accelerated;
        this.operation = operation;
    } /* end setup */

    /*********************************************************************
     * Pass parameter from <code>turboRegDialog</code> to <code>turboRegFinalAction</code>.
     *
     * @param saveOnExit
     ********************************************************************/
    public void setup(final ImagePlus sourceImp, final turboRegImage sourceImg, final turboRegPointHandler sourcePh,
                      final ImagePlus targetImp, final turboRegImage targetImg, final turboRegMask targetMsk,
                      final turboRegPointHandler targetPh, final TransformationType transformation,
                      final boolean accelerated, final boolean saveOnExit, final int pyramidDepth) {
        this.sourceImp = sourceImp;
        this.sourceImg = sourceImg;
        this.sourcePh = sourcePh;
        this.targetImp = targetImp;
        this.targetImg = targetImg;
        this.targetMsk = targetMsk;
        this.targetPh = targetPh;
        this.transformation = transformation;
        this.accelerated = accelerated;
        this.pyramidDepth = pyramidDepth;
        operation = BATCH;
    } /* end setup */

} /* end class turboRegFinalAction */

/*
 * ==================================================================== |
 * turboRegImage
 * \===================================================================
 */

/*********************************************************************
 * This class is responsible for the image preprocessing that takes place concurrently with user-interface events. It
 * contains methods to compute B-spline coefficients and their pyramids, image pyramids, gradients, and gradient
 * pyramids.
 ********************************************************************/
class turboRegImage implements Runnable

{ /* class turboRegImage */

    /*
     * ....................................................................
     * private variables
     * ....................................................................
     */
    private final Stack<Object> pyramid = new Stack<Object>();

    private final Thread t;

    private float[] image;

    private float[] coefficient;

    private float[] xGradient;

    private float[] yGradient;

    private final int width;

    private final int height;

    private int pyramidDepth;

    private TransformationType transformation;

    private final boolean isTarget;

    /*
     * ....................................................................
     * Runnable methods
     * ....................................................................
     */
    /*********************************************************************
     * Start the image precomputations. The computation of the B-spline coefficients of the full-size image is not
     * interruptible; all other methods are.
     ********************************************************************/
    @Override
    public void run() {
        coefficient = getBasicFromCardinal2D();
        switch (transformation) {
            case TRANSLATION:
            case RIGID_BODY:
            case SCALED_ROTATION:
            case AFFINE: {
                if (isTarget) {
                    buildCoefficientPyramid();
                } else {
                    imageToXYGradient2D();
                    buildImageAndGradientPyramid();
                }
                break;
            }
            // case BILINEAR: {
            // if (isTarget) {
            // buildImagePyramid();
            // } else {
            // buildCoefficientPyramid();
            // }
            // break;
            // }
        }
    } /* end run */

    /*
     * ....................................................................
     * constructors
     * ....................................................................
     */
    /*********************************************************************
     * Converts the pixel array of the incoming <code>ImagePlus</code> object into a local <code>float</code> array.
     *
     * @param imp <code>ImagePlus</code> object to preprocess.
     * @param transformation Transformation code.
     * @param isTarget Tags the current object as a target or source image.
     ********************************************************************/
    public turboRegImage(final ImagePlus imp, final TransformationType transformation, final boolean isTarget) {
        t = new Thread(this);
        t.setDaemon(true);
        this.transformation = transformation;
        this.isTarget = isTarget;
        width = imp.getWidth();
        height = imp.getHeight();
        int k = 0;
        turboRegProgressBar.addWorkload(height);
        if (imp.getType() == ImagePlus.GRAY8) {
            image = new float[width * height];
            final byte[] pixels = (byte[])imp.getProcessor().getPixels();
            for (int y = 0; (y < height); y++) {
                for (int x = 0; (x < width); x++, k++) {
                    image[k] = (pixels[k] & 0xFF);
                }
                turboRegProgressBar.stepProgressBar();
            }
        } else if (imp.getType() == ImagePlus.GRAY16) {
            image = new float[width * height];
            final short[] pixels = (short[])imp.getProcessor().getPixels();
            for (int y = 0; (y < height); y++) {
                for (int x = 0; (x < width); x++, k++) {
                    if (pixels[k] < (short)0) {
                        image[k] = pixels[k] + 65536.0F;
                    } else {
                        image[k] = pixels[k];
                    }
                }
                turboRegProgressBar.stepProgressBar();
            }
        } else if (imp.getType() == ImagePlus.GRAY32) {
            image = (float[])imp.getProcessor().getPixels();
        }
        turboRegProgressBar.workloadDone(height);
    } /* end turboRegImage */

    /*
     * ....................................................................
     * public methods
     * ....................................................................
     */
    /*********************************************************************
     * Return the B-spline coefficients of the full-size image.
     ********************************************************************/
    public float[] getCoefficient() {
        return (coefficient);
    } /* end getCoefficient */

    /*********************************************************************
     * Return the full-size image height.
     ********************************************************************/
    public int getHeight() {
        return (height);
    } /* end getHeight */

    /*********************************************************************
     * Return the full-size image array.
     ********************************************************************/
    public float[] getImage() {
        return (image);
    } /* end getImage */

    /*********************************************************************
     * Return the image pyramid as a <code>Stack</code> object. The organization of the stack depends on whether the
     * <code>turboRegImage</code> object corresponds to the target or the source image, and on the transformation (ML* =
     * { <code>TRANSLATION</code>,<code>RIGID_BODY</code>, <code>SCALED_AFFINE</code>, <code>AFFINE</code> vs. ML = {
     * <code>BILINEAR<code>}). A single pyramid level consists of
 <p>
 <table border="1">
 <tr><th><code>isTarget</code></th> <th>ML*</th> <th>ML</th></tr>
     * <tr>
     * <td>true</td>
     * <td>width<br>
     * height<br>
     * B-spline coefficients</td>
     * <td>width<br>
     * height<br>
     * samples</td>
     * </tr>
     * <tr>
     * <td>false</td>
     * <td>width<br>
     * height<br>
     * samples<br>
     * horizontal gradients<br>
     * vertical gradients</td>
     * <td>width<br>
     * height<br>
     * B-spline coefficients</td>
     * </tr>
     * </table>
     ********************************************************************/
    public Stack<Object> getPyramid() {
        return (pyramid);
    } /* end getPyramid */

    /*********************************************************************
     * Return the depth of the image pyramid. A depth <code>1</code> means that one coarse resolution level is present
     * in the stack. The full-size level is not placed on the stack.
     ********************************************************************/
    public int getPyramidDepth() {
        return (pyramidDepth);
    } /* end getPyramidDepth */

    /*********************************************************************
     * Return the thread associated with this <code>turboRegImage</code> object.
     ********************************************************************/
    public Thread getThread() {
        return (t);
    } /* end getThread */

    /*********************************************************************
     * Return the full-size image width.
     ********************************************************************/
    public int getWidth() {
        return (width);
    } /* end getWidth */

    /*********************************************************************
     * Return the full-size horizontal gradient of the image, if available.
     *
     * @see turboRegImage#getPyramid()
     ********************************************************************/
    public float[] getXGradient() {
        return (xGradient);
    } /* end getXGradient */

    /*********************************************************************
     * Return the full-size vertical gradient of the image, if available.
     *
     * @see turboRegImage#getImage()
     ********************************************************************/
    public float[] getYGradient() {
        return (yGradient);
    } /* end getYGradient */

    /*********************************************************************
     * Sets the depth up to which the pyramids should be computed.
     *
     * @see turboRegImage#getImage()
     ********************************************************************/
    public void setPyramidDepth(final int pyramidDepth) {
        this.pyramidDepth = pyramidDepth;
    } /* end setPyramidDepth */

    /*********************************************************************
     * Set or modify the transformation.
     ********************************************************************/
    public void setTransformation(final TransformationType transformation) {
        this.transformation = transformation;
    } /* end setTransformation */

    /*
     * ....................................................................
     * private methods
     * ....................................................................
     */
    /*------------------------------------------------------------------*/
    private void antiSymmetricFirMirrorOffBounds1D(final double[] h, final double[] c, final double[] s) {
        if (2 <= c.length) {
            s[0] = h[1] * (c[1] - c[0]);
            for (int i = 1; (i < (s.length - 1)); i++) {
                s[i] = h[1] * (c[i + 1] - c[i - 1]);
            }
            s[s.length - 1] = h[1] * (c[c.length - 1] - c[c.length - 2]);
        } else {
            s[0] = 0.0;
        }
    } /* end antiSymmetricFirMirrorOffBounds1D */

    /*------------------------------------------------------------------*/
    private void basicToCardinal2D(final float[] basic, final float[] cardinal, final int width, final int height,
                                   final int degree) {
        final double[] hLine = new double[width];
        final double[] vLine = new double[height];
        final double[] hData = new double[width];
        final double[] vData = new double[height];
        double[] h = null;
        switch (degree) {
            case 3: {
                h = new double[2];
                h[0] = 2.0 / 3.0;
                h[1] = 1.0 / 6.0;
                break;
            }
            case 7: {
                h = new double[4];
                h[0] = 151.0 / 315.0;
                h[1] = 397.0 / 1680.0;
                h[2] = 1.0 / 42.0;
                h[3] = 1.0 / 5040.0;
                break;
            }
            default: {
                h = new double[1];
                h[0] = 1.0;
            }
        }
        int workload = width + height;
        turboRegProgressBar.addWorkload(workload);
        for (int y = 0; ((y < height) && (!t.isInterrupted())); y++) {
            extractRow(basic, y, hLine);
            symmetricFirMirrorOffBounds1D(h, hLine, hData);
            putRow(cardinal, y, hData);
            turboRegProgressBar.stepProgressBar();
            workload--;
        }
        for (int x = 0; ((x < width) && (!t.isInterrupted())); x++) {
            extractColumn(cardinal, width, x, vLine);
            symmetricFirMirrorOffBounds1D(h, vLine, vData);
            putColumn(cardinal, width, x, vData);
            turboRegProgressBar.stepProgressBar();
            workload--;
        }
        turboRegProgressBar.skipProgressBar(workload);
        turboRegProgressBar.workloadDone(width + height);
    } /* end basicToCardinal2D */

    /*------------------------------------------------------------------*/
    private void buildCoefficientPyramid() {
        int fullWidth;
        int fullHeight;
        float[] fullDual = new float[width * height];
        int halfWidth = width;
        int halfHeight = height;
        if (1 < pyramidDepth) {
            basicToCardinal2D(coefficient, fullDual, width, height, 7);
        }
        for (int depth = 1; ((depth < pyramidDepth) && (!t.isInterrupted())); depth++) {
            fullWidth = halfWidth;
            fullHeight = halfHeight;
            halfWidth /= 2;
            halfHeight /= 2;
            final float[] halfDual = getHalfDual2D(fullDual, fullWidth, fullHeight);
            final float[] halfCoefficient = getBasicFromCardinal2D(halfDual, halfWidth, halfHeight, 7);
            pyramid.push(halfCoefficient);
            pyramid.push(new Integer(halfHeight));
            pyramid.push(new Integer(halfWidth));
            fullDual = halfDual;
        }
    } /* end buildCoefficientPyramid */

    /*------------------------------------------------------------------*/
    private void buildImageAndGradientPyramid() {
        int fullWidth;
        int fullHeight;
        float[] fullDual = new float[width * height];
        int halfWidth = width;
        int halfHeight = height;
        if (1 < pyramidDepth) {
            cardinalToDual2D(image, fullDual, width, height, 3);
        }
        for (int depth = 1; ((depth < pyramidDepth) && (!t.isInterrupted())); depth++) {
            fullWidth = halfWidth;
            fullHeight = halfHeight;
            halfWidth /= 2;
            halfHeight /= 2;
            final float[] halfDual = getHalfDual2D(fullDual, fullWidth, fullHeight);
            final float[] halfImage = getBasicFromCardinal2D(halfDual, halfWidth, halfHeight, 7);
            final float[] halfXGradient = new float[halfWidth * halfHeight];
            final float[] halfYGradient = new float[halfWidth * halfHeight];
            coefficientToXYGradient2D(halfImage, halfXGradient, halfYGradient, halfWidth, halfHeight);
            basicToCardinal2D(halfImage, halfImage, halfWidth, halfHeight, 3);
            pyramid.push(halfYGradient);
            pyramid.push(halfXGradient);
            pyramid.push(halfImage);
            pyramid.push(new Integer(halfHeight));
            pyramid.push(new Integer(halfWidth));
            fullDual = halfDual;
        }
    } /* end buildImageAndGradientPyramid */

    /*------------------------------------------------------------------*/
    // private void buildImagePyramid() {
    // int fullWidth;
    // int fullHeight;
    // float[] fullDual = new float[width * height];
    // int halfWidth = width;
    // int halfHeight = height;
    // if (1 < pyramidDepth) {
    // cardinalToDual2D(image, fullDual, width, height, 3);
    // }
    // for (int depth = 1; ((depth < pyramidDepth) && (!t.isInterrupted()));
    // depth++) {
    // fullWidth = halfWidth;
    // fullHeight = halfHeight;
    // halfWidth /= 2;
    // halfHeight /= 2;
    // final float[] halfDual = getHalfDual2D(fullDual, fullWidth,
    // fullHeight);
    // final float[] halfImage = new float[halfWidth * halfHeight];
    // dualToCardinal2D(halfDual, halfImage, halfWidth, halfHeight, 3);
    // pyramid.push(halfImage);
    // pyramid.push(new Integer(halfHeight));
    // pyramid.push(new Integer(halfWidth));
    // fullDual = halfDual;
    // }
    // } /* end buildImagePyramid */

    /*------------------------------------------------------------------*/
    private void cardinalToDual2D(final float[] cardinal, final float[] dual, final int width, final int height,
                                  final int degree) {
        basicToCardinal2D(getBasicFromCardinal2D(cardinal, width, height, degree), dual, width, height,
                          (2 * degree) + 1);
    } /* end cardinalToDual2D */

    /*------------------------------------------------------------------*/
    private void coefficientToGradient1D(final double[] c) {
        final double[] h = {0.0, 1.0 / 2.0};
        final double[] s = new double[c.length];
        antiSymmetricFirMirrorOffBounds1D(h, c, s);
        System.arraycopy(s, 0, c, 0, s.length);
    } /* end coefficientToGradient1D */

    /*------------------------------------------------------------------*/
    private void coefficientToSamples1D(final double[] c) {
        final double[] h = {2.0 / 3.0, 1.0 / 6.0};
        final double[] s = new double[c.length];
        symmetricFirMirrorOffBounds1D(h, c, s);
        System.arraycopy(s, 0, c, 0, s.length);
    } /* end coefficientToSamples1D */

    /*------------------------------------------------------------------*/
    private void coefficientToXYGradient2D(final float[] basic, final float[] xGradient, final float[] yGradient,
                                           final int width, final int height) {
        final double[] hLine = new double[width];
        final double[] hData = new double[width];
        final double[] vLine = new double[height];
        int workload = 2 * (width + height);
        turboRegProgressBar.addWorkload(workload);
        for (int y = 0; ((y < height) && (!t.isInterrupted())); y++) {
            extractRow(basic, y, hLine);
            System.arraycopy(hLine, 0, hData, 0, width);
            coefficientToGradient1D(hLine);
            turboRegProgressBar.stepProgressBar();
            workload--;
            coefficientToSamples1D(hData);
            putRow(xGradient, y, hLine);
            putRow(yGradient, y, hData);
            turboRegProgressBar.stepProgressBar();
            workload--;
        }
        for (int x = 0; ((x < width) && (!t.isInterrupted())); x++) {
            extractColumn(xGradient, width, x, vLine);
            coefficientToSamples1D(vLine);
            putColumn(xGradient, width, x, vLine);
            turboRegProgressBar.stepProgressBar();
            workload--;
            extractColumn(yGradient, width, x, vLine);
            coefficientToGradient1D(vLine);
            putColumn(yGradient, width, x, vLine);
            turboRegProgressBar.stepProgressBar();
            workload--;
        }
        turboRegProgressBar.skipProgressBar(workload);
        turboRegProgressBar.workloadDone(2 * (width + height));
    } /* end coefficientToXYGradient2D */

    /*------------------------------------------------------------------*/
    // private void dualToCardinal2D(final float[] dual, final float[]
    // cardinal,
    // final int width, final int height, final int degree) {
    // basicToCardinal2D(
    // getBasicFromCardinal2D(dual, width, height, 2 * degree + 1),
    // cardinal, width, height, degree);
    // } /* end dualToCardinal2D */

    /*------------------------------------------------------------------*/
    private void extractColumn(final float[] array, final int width, int x, final double[] column) {
        for (int i = 0; (i < column.length); i++) {
            column[i] = array[x];
            x += width;
        }
    } /* end extractColumn */

    /*------------------------------------------------------------------*/
    private void extractRow(final float[] array, int y, final double[] row) {
        y *= row.length;
        for (int i = 0; (i < row.length); i++) {
            row[i] = array[y++];
        }
    } /* end extractRow */

    /*------------------------------------------------------------------*/
    private float[] getBasicFromCardinal2D() {
        final float[] basic = new float[width * height];
        final double[] hLine = new double[width];
        final double[] vLine = new double[height];
        turboRegProgressBar.addWorkload(width + height);
        for (int y = 0; (y < height); y++) {
            extractRow(image, y, hLine);
            samplesToInterpolationCoefficient1D(hLine, 3, 0.0);
            putRow(basic, y, hLine);
            turboRegProgressBar.stepProgressBar();
        }
        for (int x = 0; (x < width); x++) {
            extractColumn(basic, width, x, vLine);
            samplesToInterpolationCoefficient1D(vLine, 3, 0.0);
            putColumn(basic, width, x, vLine);
            turboRegProgressBar.stepProgressBar();
        }
        turboRegProgressBar.workloadDone(width + height);
        return (basic);
    } /* end getBasicFromCardinal2D */

    /*------------------------------------------------------------------*/
    private float[] getBasicFromCardinal2D(final float[] cardinal, final int width, final int height, final int degree) {
        final float[] basic = new float[width * height];
        final double[] hLine = new double[width];
        final double[] vLine = new double[height];
        int workload = width + height;
        turboRegProgressBar.addWorkload(workload);
        for (int y = 0; ((y < height) && (!t.isInterrupted())); y++) {
            extractRow(cardinal, y, hLine);
            samplesToInterpolationCoefficient1D(hLine, degree, 0.0);
            putRow(basic, y, hLine);
            turboRegProgressBar.stepProgressBar();
            workload--;
        }
        for (int x = 0; ((x < width) && (!t.isInterrupted())); x++) {
            extractColumn(basic, width, x, vLine);
            samplesToInterpolationCoefficient1D(vLine, degree, 0.0);
            putColumn(basic, width, x, vLine);
            turboRegProgressBar.stepProgressBar();
            workload--;
        }
        turboRegProgressBar.skipProgressBar(workload);
        turboRegProgressBar.workloadDone(width + height);
        return (basic);
    } /* end getBasicFromCardinal2D */

    /*------------------------------------------------------------------*/
    private float[] getHalfDual2D(final float[] fullDual, final int fullWidth, final int fullHeight) {
        final int halfWidth = fullWidth / 2;
        final int halfHeight = fullHeight / 2;
        final double[] hLine = new double[fullWidth];
        final double[] hData = new double[halfWidth];
        final double[] vLine = new double[fullHeight];
        final double[] vData = new double[halfHeight];
        final float[] demiDual = new float[halfWidth * fullHeight];
        final float[] halfDual = new float[halfWidth * halfHeight];
        int workload = halfWidth + fullHeight;
        turboRegProgressBar.addWorkload(workload);
        for (int y = 0; ((y < fullHeight) && (!t.isInterrupted())); y++) {
            extractRow(fullDual, y, hLine);
            reduceDual1D(hLine, hData);
            putRow(demiDual, y, hData);
            turboRegProgressBar.stepProgressBar();
            workload--;
        }
        for (int x = 0; ((x < halfWidth) && (!t.isInterrupted())); x++) {
            extractColumn(demiDual, halfWidth, x, vLine);
            reduceDual1D(vLine, vData);
            putColumn(halfDual, halfWidth, x, vData);
            turboRegProgressBar.stepProgressBar();
            workload--;
        }
        turboRegProgressBar.skipProgressBar(workload);
        turboRegProgressBar.workloadDone(halfWidth + fullHeight);
        return (halfDual);
    } /* end getHalfDual2D */

    /*------------------------------------------------------------------*/
    /**
     * @param tolerance
     */
    @SuppressWarnings("static-method")
    private double getInitialAntiCausalCoefficientMirrorOffBounds(final double[] c, final double z,
                                                                  final double tolerance) {
        return ((z * c[c.length - 1]) / (z - 1.0));
    } /* end getInitialAntiCausalCoefficientMirrorOffBounds */

    /*------------------------------------------------------------------*/
    private double getInitialCausalCoefficientMirrorOffBounds(final double[] c, final double z, final double tolerance) {
        double z1 = z;
        double zn = Math.pow(z, c.length);
        double sum = (1.0 + z) * (c[0] + (zn * c[c.length - 1]));
        int horizon = c.length;
        if (0.0 < tolerance) {
            horizon = 2 + (int)(Math.log(tolerance) / Math.log(Math.abs(z)));
            horizon = (horizon < c.length) ? (horizon) : (c.length);
        }
        zn = zn * zn;
        for (int n = 1; (n < (horizon - 1)); n++) {
            z1 = z1 * z;
            zn = zn / z;
            sum = sum + ((z1 + zn) * c[n]);
        }
        return (sum / (1.0 - Math.pow(z, 2 * c.length)));
    } /* end getInitialCausalCoefficientMirrorOffBounds */

    /*------------------------------------------------------------------*/
    private void imageToXYGradient2D() {
        final double[] hLine = new double[width];
        final double[] vLine = new double[height];
        xGradient = new float[width * height];
        yGradient = new float[width * height];
        int workload = width + height;
        turboRegProgressBar.addWorkload(workload);
        for (int y = 0; ((y < height) && (!t.isInterrupted())); y++) {
            extractRow(image, y, hLine);
            samplesToInterpolationCoefficient1D(hLine, 3, 0.0);
            coefficientToGradient1D(hLine);
            putRow(xGradient, y, hLine);
            turboRegProgressBar.stepProgressBar();
            workload--;
        }
        for (int x = 0; ((x < width) && (!t.isInterrupted())); x++) {
            extractColumn(image, width, x, vLine);
            samplesToInterpolationCoefficient1D(vLine, 3, 0.0);
            coefficientToGradient1D(vLine);
            putColumn(yGradient, width, x, vLine);
            turboRegProgressBar.stepProgressBar();
            workload--;
        }
        turboRegProgressBar.skipProgressBar(workload);
        turboRegProgressBar.workloadDone(width + height);
    } /* end imageToXYGradient2D */

    /*------------------------------------------------------------------*/
    private void putColumn(final float[] array, final int width, int x, final double[] column) {
        for (int i = 0; (i < column.length); i++) {
            array[x] = (float)column[i];
            x += width;
        }
    } /* end putColumn */

    /*------------------------------------------------------------------*/
    private void putRow(final float[] array, int y, final double[] row) {
        y *= row.length;
        for (int i = 0; (i < row.length); i++) {
            array[y++] = (float)row[i];
        }
    } /* end putRow */

    /*------------------------------------------------------------------*/
    private void reduceDual1D(final double[] c, final double[] s) {
        final double h[] = {6.0 / 16.0, 4.0 / 16.0, 1.0 / 16.0};
        if (2 <= s.length) {
            s[0] = (h[0] * c[0]) + (h[1] * (c[0] + c[1])) + (h[2] * (c[1] + c[2]));
            for (int i = 2, j = 1; (j < (s.length - 1)); i += 2, j++) {
                s[j] = (h[0] * c[i]) + (h[1] * (c[i - 1] + c[i + 1])) + (h[2] * (c[i - 2] + c[i + 2]));
            }
            if (c.length == (2 * s.length)) {
                s[s.length - 1] =
                        (h[0] * c[c.length - 2]) + (h[1] * (c[c.length - 3] + c[c.length - 1]))
                                + (h[2] * (c[c.length - 4] + c[c.length - 1]));
            } else {
                s[s.length - 1] =
                        (h[0] * c[c.length - 3]) + (h[1] * (c[c.length - 4] + c[c.length - 2]))
                                + (h[2] * (c[c.length - 5] + c[c.length - 1]));
            }
        } else {
            switch (c.length) {
                case 3: {
                    s[0] = (h[0] * c[0]) + (h[1] * (c[0] + c[1])) + (h[2] * (c[1] + c[2]));
                    break;
                }
                case 2: {
                    s[0] = (h[0] * c[0]) + (h[1] * (c[0] + c[1])) + (2.0 * h[2] * c[1]);
                    break;
                }
            }
        }
    } /* end reduceDual1D */

    /*------------------------------------------------------------------*/
    private void samplesToInterpolationCoefficient1D(final double[] c, final int degree, final double tolerance) {
        double[] z = new double[0];
        double lambda = 1.0;
        switch (degree) {
            case 3: {
                z = new double[1];
                z[0] = Math.sqrt(3.0) - 2.0;
                break;
            }
            case 7: {
                z = new double[3];
                z[0] = -0.5352804307964381655424037816816460718339231523426924148812;
                z[1] = -0.122554615192326690515272264359357343605486549427295558490763;
                z[2] = -0.0091486948096082769285930216516478534156925639545994482648003;
                break;
            }
        }
        if (c.length == 1) {
            return;
        }
        for (int k = 0; (k < z.length); k++) {
            lambda *= (1.0 - z[k]) * (1.0 - (1.0 / z[k]));
        }
        for (int n = 0; (n < c.length); n++) {
            c[n] = c[n] * lambda;
        }
        for (int k = 0; (k < z.length); k++) {
            c[0] = getInitialCausalCoefficientMirrorOffBounds(c, z[k], tolerance);
            for (int n = 1; (n < c.length); n++) {
                c[n] = c[n] + (z[k] * c[n - 1]);
            }
            c[c.length - 1] = getInitialAntiCausalCoefficientMirrorOffBounds(c, z[k], tolerance);
            for (int n = c.length - 2; (0 <= n); n--) {
                c[n] = z[k] * (c[n + 1] - c[n]);
            }
        }
    } /* end samplesToInterpolationCoefficient1D */

    /*------------------------------------------------------------------*/
    private void symmetricFirMirrorOffBounds1D(final double[] h, final double[] c, final double[] s) {
        switch (h.length) {
            case 2: {
                if (2 <= c.length) {
                    s[0] = (h[0] * c[0]) + (h[1] * (c[0] + c[1]));
                    for (int i = 1; (i < (s.length - 1)); i++) {
                        s[i] = (h[0] * c[i]) + (h[1] * (c[i - 1] + c[i + 1]));
                    }
                    s[s.length - 1] = (h[0] * c[c.length - 1]) + (h[1] * (c[c.length - 2] + c[c.length - 1]));
                } else {
                    s[0] = (h[0] + (2.0 * h[1])) * c[0];
                }
                break;
            }
            case 4: {
                if (6 <= c.length) {
                    s[0] = (h[0] * c[0]) + (h[1] * (c[0] + c[1])) + (h[2] * (c[1] + c[2])) + (h[3] * (c[2] + c[3]));
                    s[1] = (h[0] * c[1]) + (h[1] * (c[0] + c[2])) + (h[2] * (c[0] + c[3])) + (h[3] * (c[1] + c[4]));
                    s[2] = (h[0] * c[2]) + (h[1] * (c[1] + c[3])) + (h[2] * (c[0] + c[4])) + (h[3] * (c[0] + c[5]));
                    for (int i = 3; (i < (s.length - 3)); i++) {
                        s[i] =
                                (h[0] * c[i]) + (h[1] * (c[i - 1] + c[i + 1])) + (h[2] * (c[i - 2] + c[i + 2]))
                                        + (h[3] * (c[i - 3] + c[i + 3]));
                    }
                    s[s.length - 3] =
                            (h[0] * c[c.length - 3]) + (h[1] * (c[c.length - 4] + c[c.length - 2]))
                                    + (h[2] * (c[c.length - 5] + c[c.length - 1]))
                                    + (h[3] * (c[c.length - 6] + c[c.length - 1]));
                    s[s.length - 2] =
                            (h[0] * c[c.length - 2]) + (h[1] * (c[c.length - 3] + c[c.length - 1]))
                                    + (h[2] * (c[c.length - 4] + c[c.length - 1]))
                                    + (h[3] * (c[c.length - 5] + c[c.length - 2]));
                    s[s.length - 1] =
                            (h[0] * c[c.length - 1]) + (h[1] * (c[c.length - 2] + c[c.length - 1]))
                                    + (h[2] * (c[c.length - 3] + c[c.length - 2]))
                                    + (h[3] * (c[c.length - 4] + c[c.length - 3]));
                } else {
                    switch (c.length) {
                        case 5: {
                            s[0] =
                                    (h[0] * c[0]) + (h[1] * (c[0] + c[1])) + (h[2] * (c[1] + c[2]))
                                            + (h[3] * (c[2] + c[3]));
                            s[1] =
                                    (h[0] * c[1]) + (h[1] * (c[0] + c[2])) + (h[2] * (c[0] + c[3]))
                                            + (h[3] * (c[1] + c[4]));
                            s[2] = (h[0] * c[2]) + (h[1] * (c[1] + c[3])) + ((h[2] + h[3]) * (c[0] + c[4]));
                            s[3] =
                                    (h[0] * c[3]) + (h[1] * (c[2] + c[4])) + (h[2] * (c[1] + c[4]))
                                            + (h[3] * (c[0] + c[3]));
                            s[4] =
                                    (h[0] * c[4]) + (h[1] * (c[3] + c[4])) + (h[2] * (c[2] + c[3]))
                                            + (h[3] * (c[1] + c[2]));
                            break;
                        }
                        case 4: {
                            s[0] =
                                    (h[0] * c[0]) + (h[1] * (c[0] + c[1])) + (h[2] * (c[1] + c[2]))
                                            + (h[3] * (c[2] + c[3]));
                            s[1] =
                                    (h[0] * c[1]) + (h[1] * (c[0] + c[2])) + (h[2] * (c[0] + c[3]))
                                            + (h[3] * (c[1] + c[3]));
                            s[2] =
                                    (h[0] * c[2]) + (h[1] * (c[1] + c[3])) + (h[2] * (c[0] + c[3]))
                                            + (h[3] * (c[0] + c[2]));
                            s[3] =
                                    (h[0] * c[3]) + (h[1] * (c[2] + c[3])) + (h[2] * (c[1] + c[2]))
                                            + (h[3] * (c[0] + c[1]));
                            break;
                        }
                        case 3: {
                            s[0] =
                                    (h[0] * c[0]) + (h[1] * (c[0] + c[1])) + (h[2] * (c[1] + c[2]))
                                            + (2.0 * h[3] * c[2]);
                            s[1] = (h[0] * c[1]) + ((h[1] + h[2]) * (c[0] + c[2])) + (2.0 * h[3] * c[1]);
                            s[2] =
                                    (h[0] * c[2]) + (h[1] * (c[1] + c[2])) + (h[2] * (c[0] + c[1]))
                                            + (2.0 * h[3] * c[0]);
                            break;
                        }
                        case 2: {
                            s[0] = ((h[0] + h[1] + h[3]) * c[0]) + ((h[1] + (2.0 * h[2]) + h[3]) * c[1]);
                            s[1] = ((h[0] + h[1] + h[3]) * c[1]) + ((h[1] + (2.0 * h[2]) + h[3]) * c[0]);
                            break;
                        }
                        case 1: {
                            s[0] = (h[0] + (2.0 * (h[1] + h[2] + h[3]))) * c[0];
                            break;
                        }
                    }
                }
                break;
            }
        }
    } /* end symmetricFirMirrorOffBounds1D */

} /* end class turboRegImage */

/*
 * ==================================================================== |
 * turboRegMask
 * \===================================================================
 */

/*********************************************************************
 * This class is responsible for the mask preprocessing that takes place concurrently with user-interface events. It
 * contains methods to compute the mask pyramids.
 ********************************************************************/
class turboRegMask implements Runnable

{ /* class turboRegMask */

    /*
     * ....................................................................
     * private variables
     * ....................................................................
     */
    private final Stack<float[]> pyramid = new Stack<float[]>();

    private final Thread t;

    private final float[] mask;

    private final int width;

    private final int height;

    private int pyramidDepth;

    /*
     * ....................................................................
     * Runnable methods
     * ....................................................................
     */
    /*********************************************************************
     * Start the mask precomputations, which are interruptible.
     ********************************************************************/
    @Override
    public void run() {
        buildPyramid();
    } /* end run */

    /*
     * ....................................................................
     * constructors
     * ....................................................................
     */
    /*********************************************************************
     * Converts the pixel array of the incoming <code>ImagePlus</code> object into a local <code>boolean</code> array.
     *
     * @param imp <code>ImagePlus</code> object to preprocess.
     ********************************************************************/
    public turboRegMask(final ImagePlus imp) {
        t = new Thread(this);
        t.setDaemon(true);
        width = imp.getWidth();
        height = imp.getHeight();
        int k = 0;
        turboRegProgressBar.addWorkload(height);
        mask = new float[width * height];
        if (imp.getType() == ImagePlus.GRAY8) {
            final byte[] pixels = (byte[])imp.getProcessor().getPixels();
            for (int y = 0; (y < height); y++) {
                for (int x = 0; (x < width); x++, k++) {
                    mask[k] = pixels[k];
                }
                turboRegProgressBar.stepProgressBar();
            }
        } else if (imp.getType() == ImagePlus.GRAY16) {
            final short[] pixels = (short[])imp.getProcessor().getPixels();
            for (int y = 0; (y < height); y++) {
                for (int x = 0; (x < width); x++, k++) {
                    mask[k] = pixels[k];
                }
                turboRegProgressBar.stepProgressBar();
            }
        } else if (imp.getType() == ImagePlus.GRAY32) {
            final float[] pixels = (float[])imp.getProcessor().getPixels();
            for (int y = 0; (y < height); y++) {
                for (int x = 0; (x < width); x++, k++) {
                    mask[k] = pixels[k];
                }
                turboRegProgressBar.stepProgressBar();
            }
        }
        turboRegProgressBar.workloadDone(height);
    } /* end turboRegMask */

    /*
     * ....................................................................
     * public methods
     * ....................................................................
     */
    /*********************************************************************
     * Set to <code>true</code> every pixel of the full-size mask.
     ********************************************************************/
    public void clearMask() {
        int k = 0;
        turboRegProgressBar.addWorkload(height);
        for (int y = 0; (y < height); y++) {
            for (int x = 0; (x < width); x++) {
                mask[k++] = 1.0F;
            }
            turboRegProgressBar.stepProgressBar();
        }
        turboRegProgressBar.workloadDone(height);
    } /* end clearMask */

    /*********************************************************************
     * Return the full-size mask array.
     ********************************************************************/
    public float[] getMask() {
        return (mask);
    } /* end getMask */

    /*********************************************************************
     * Return the pyramid as a <code>Stack</code> object. A single pyramid level consists of
     * <p>
     * <table border="1">
     * <tr>
     * <th><code>isTarget</code></th>
     * <th>ML*</th>
     * <th>ML</th>
     * </tr>
     * <tr>
     * <td>true</td>
     * <td>mask samples</td>
     * <td>mask samples</td>
     * </tr>
     * <tr>
     * <td>false</td>
     * <td>mask samples</td>
     * <td>mask samples</td>
     * </tr>
     * </table>
     *
     * @see turboRegImage#getPyramid()
     ********************************************************************/
    public Stack<float[]> getPyramid() {
        return (pyramid);
    } /* end getPyramid */

    /*********************************************************************
     * Return the thread associated with this <code>turboRegMask</code> object.
     ********************************************************************/
    public Thread getThread() {
        return (t);
    } /* end getThread */

    /*********************************************************************
     * Set the depth up to which the pyramids should be computed.
     *
     * @see turboRegMask#getPyramid()
     ********************************************************************/
    public void setPyramidDepth(final int pyramidDepth) {
        this.pyramidDepth = pyramidDepth;
    } /* end setPyramidDepth */

    /*
     * ....................................................................
     * private methods
     * ....................................................................
     */
    /*------------------------------------------------------------------*/
    private void buildPyramid() {
        int fullWidth;
        int fullHeight;
        float[] fullMask = mask;
        int halfWidth = width;
        int halfHeight = height;
        for (int depth = 1; ((depth < pyramidDepth) && (!t.isInterrupted())); depth++) {
            fullWidth = halfWidth;
            fullHeight = halfHeight;
            halfWidth /= 2;
            halfHeight /= 2;
            final float[] halfMask = getHalfMask2D(fullMask, fullWidth, fullHeight);
            pyramid.push(halfMask);
            fullMask = halfMask;
        }
    } /* end buildPyramid */

    /*------------------------------------------------------------------*/
    private float[] getHalfMask2D(final float[] fullMask, final int fullWidth, final int fullHeight) {
        final int halfWidth = fullWidth / 2;
        final int halfHeight = fullHeight / 2;
        final boolean oddWidth = ((2 * halfWidth) != fullWidth);
        int workload = 2 * halfHeight;
        final float[] halfMask = new float[halfWidth * halfHeight];
        int k = 0;
        for (int y = 0; ((y < halfHeight) && (!t.isInterrupted())); y++) {
            for (int x = 0; (x < halfWidth); x++) {
                halfMask[k++] = 0.0F;
            }
            turboRegProgressBar.stepProgressBar();
            workload--;
        }
        k = 0;
        int n = 0;
        for (int y = 0; ((y < (halfHeight - 1)) && (!t.isInterrupted())); y++) {
            for (int x = 0; (x < (halfWidth - 1)); x++) {
                halfMask[k] += Math.abs(fullMask[n++]);
                halfMask[k] += Math.abs(fullMask[n]);
                halfMask[++k] += Math.abs(fullMask[n++]);
            }
            halfMask[k] += Math.abs(fullMask[n++]);
            halfMask[k++] += Math.abs(fullMask[n++]);
            if (oddWidth) {
                n++;
            }
            for (int x = 0; (x < (halfWidth - 1)); x++) {
                halfMask[k - halfWidth] += Math.abs(fullMask[n]);
                halfMask[k] += Math.abs(fullMask[n++]);
                halfMask[k - halfWidth] += Math.abs(fullMask[n]);
                halfMask[(k - halfWidth) + 1] += Math.abs(fullMask[n]);
                halfMask[k] += Math.abs(fullMask[n]);
                halfMask[++k] += Math.abs(fullMask[n++]);
            }
            halfMask[k - halfWidth] += Math.abs(fullMask[n]);
            halfMask[k] += Math.abs(fullMask[n++]);
            halfMask[k - halfWidth] += Math.abs(fullMask[n]);
            halfMask[k++] += Math.abs(fullMask[n++]);
            if (oddWidth) {
                n++;
            }
            k -= halfWidth;
            turboRegProgressBar.stepProgressBar();
            workload--;
        }
        for (int x = 0; (x < (halfWidth - 1)); x++) {
            halfMask[k] += Math.abs(fullMask[n++]);
            halfMask[k] += Math.abs(fullMask[n]);
            halfMask[++k] += Math.abs(fullMask[n++]);
        }
        halfMask[k] += Math.abs(fullMask[n++]);
        halfMask[k++] += Math.abs(fullMask[n++]);
        if (oddWidth) {
            n++;
        }
        k -= halfWidth;
        for (int x = 0; (x < (halfWidth - 1)); x++) {
            halfMask[k] += Math.abs(fullMask[n++]);
            halfMask[k] += Math.abs(fullMask[n]);
            halfMask[++k] += Math.abs(fullMask[n++]);
        }
        halfMask[k] += Math.abs(fullMask[n++]);
        halfMask[k] += Math.abs(fullMask[n]);
        turboRegProgressBar.stepProgressBar();
        workload--;
        turboRegProgressBar.skipProgressBar(workload);
        turboRegProgressBar.workloadDone(2 * halfHeight);
        return (halfMask);
    } /* end getHalfMask2D */

} /* end class turboRegMask */

/*
 * ==================================================================== |
 * turboRegPointAction
 * \===================================================================
 */

/*********************************************************************
 * This class implements the various listeners that are in charge of user interactions when dealing with landmarks. It
 * overrides the listeners of ImageJ, if any. Those are restored upon restitution of this <code>ImageCanvas</code>
 * object to ImageJ.
 ********************************************************************/
class turboRegPointAction extends ImageCanvas implements AdjustmentListener, FocusListener, KeyListener, MouseListener,
        MouseMotionListener

{ /* class turboRegPointAction */

    /*
     * ....................................................................
     * private variables
     * ....................................................................
     */
    private final ImagePlus mainImp;

    private ImagePlus secondaryImp;

    private final turboRegPointHandler mainPh;

    private turboRegPointHandler secondaryPh;

    private final turboRegPointToolbar tb;

    private static final long serialVersionUID = 1L;

    /*
     * ....................................................................
     * AdjustmentListener methods
     * ....................................................................
     */
    /*********************************************************************
     * Listen to <code>AdjustmentEvent</code> events.
     *
     * @param e Ignored.
     ********************************************************************/
    @Override
    public synchronized void adjustmentValueChanged(final AdjustmentEvent e) {
        updateAndDraw();
    } /* adjustmentValueChanged */

    /*
     * ....................................................................
     * FocusListener methods
     * ....................................................................
     */
    /*********************************************************************
     * Listen to <code>focusGained</code> events.
     *
     * @param e Ignored.
     ********************************************************************/
    @Override
    public void focusGained(final FocusEvent e) {
        updateAndDraw();
    } /* end focusGained */

    /*********************************************************************
     * Listen to <code>focusGained</code> events.
     *
     * @param e Ignored.
     ********************************************************************/
    @Override
    public void focusLost(final FocusEvent e) {
        updateAndDraw();
    } /* end focusLost */

    /*
     * ....................................................................
     * KeyListener methods
     * ....................................................................
     */
    /*********************************************************************
     * Listen to <code>keyPressed</code> events.
     *
     * @param e The expected key codes are as follows:
     *            <ul>
     *            <li><code>KeyEvent.VK_COMMA</code>: display the previous slice, if any;</li>
     *            <li><code>KeyEvent.VK_DOWN</code>: move down the current landmark;</li>
     *            <li><code>KeyEvent.VK_LEFT</code>: move the current landmark to the left;</li>
     *            <li><code>KeyEvent.VK_PERIOD</code>: display the next slice, if any;</li>
     *            <li><code>KeyEvent.VK_SPACE</code>: select the current landmark;</li>
     *            <li><code>KeyEvent.VK_RIGHT</code>: move the current landmark to the right;</li>
     *            <li><code>KeyEvent.VK_UP</code>: move up the current landmark.</li>
     *            </ul>
     ********************************************************************/
    @Override
    public void keyPressed(final KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_COMMA: {
                if (1 < mainImp.getCurrentSlice()) {
                    mainImp.setSlice(mainImp.getCurrentSlice() - 1);
                    updateStatus();
                }
                return;
            }
            case KeyEvent.VK_PERIOD: {
                if (mainImp.getCurrentSlice() < mainImp.getStackSize()) {
                    mainImp.setSlice(mainImp.getCurrentSlice() + 1);
                    updateStatus();
                }
                return;
            }
        }
        final int x = mainPh.getPoint().x;
        final int y = mainPh.getPoint().y;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_DOWN: {
                mainPh.movePoint(mainImp.getWindow().getCanvas().screenX(x),
                                 mainImp.getWindow()
                                         .getCanvas()
                                         .screenY(y
                                                          + (int)Math.ceil(1.0 / mainImp.getWindow().getCanvas()
                                                                  .getMagnification())));
                mainImp.setRoi(mainPh);
                break;
            }
            case KeyEvent.VK_LEFT: {
                mainPh.movePoint(mainImp.getWindow()
                                         .getCanvas()
                                         .screenX(x
                                                          - (int)Math.ceil(1.0 / mainImp.getWindow().getCanvas()
                                                                  .getMagnification())), mainImp.getWindow()
                                         .getCanvas().screenY(y));
                mainImp.setRoi(mainPh);
                break;
            }
            case KeyEvent.VK_RIGHT: {
                mainPh.movePoint(mainImp.getWindow()
                                         .getCanvas()
                                         .screenX(x
                                                          + (int)Math.ceil(1.0 / mainImp.getWindow().getCanvas()
                                                                  .getMagnification())), mainImp.getWindow()
                                         .getCanvas().screenY(y));
                mainImp.setRoi(mainPh);
                break;
            }
            case KeyEvent.VK_SPACE: {
                break;
            }
            case KeyEvent.VK_UP: {
                mainPh.movePoint(mainImp.getWindow().getCanvas().screenX(x),
                                 mainImp.getWindow()
                                         .getCanvas()
                                         .screenY(y
                                                          - (int)Math.ceil(1.0 / mainImp.getWindow().getCanvas()
                                                                  .getMagnification())));
                mainImp.setRoi(mainPh);
                break;
            }
        }
        updateStatus();
    } /* end keyPressed */

    /*********************************************************************
     * Listen to <code>keyReleased</code> events.
     *
     * @param e Ignored.
     ********************************************************************/
    @Override
    public void keyReleased(final KeyEvent e) {
    } /* end keyReleased */

    /*********************************************************************
     * Listen to <code>keyTyped</code> events.
     *
     * @param e Ignored.
     ********************************************************************/
    @Override
    public void keyTyped(final KeyEvent e) {
    } /* end keyTyped */

    /*
     * ....................................................................
     * MouseListener methods
     * ....................................................................
     */
    /*********************************************************************
     * Listen to <code>mouseClicked</code> events.
     *
     * @param e Ignored.
     ********************************************************************/
    @Override
    public void mouseClicked(final MouseEvent e) {
    } /* end mouseClicked */

    /*********************************************************************
     * Listen to <code>mouseEntered</code> events. Change the cursor to a crosshair.
     *
     * @param e Event.
     ********************************************************************/
    @Override
    public void mouseEntered(final MouseEvent e) {
        WindowManager.setCurrentWindow(mainImp.getWindow());
        mainImp.getWindow().toFront();
        mainImp.getWindow().getCanvas().setCursor(crosshairCursor);
        updateAndDraw();
    } /* end mouseEntered */

    /*********************************************************************
     * Listen to <code>mouseExited</code> events. Change the cursor to the default cursor. Update the ImageJ status.
     *
     * @param e Event.
     ********************************************************************/
    @Override
    public void mouseExited(final MouseEvent e) {
        mainImp.getWindow().getCanvas().setCursor(defaultCursor);
        IJ.showStatus("");
    } /* end mouseExited */

    /*********************************************************************
     * Listen to <code>mousePressed</code> events. Update the current point or call the ImageJ's zoom methods.
     *
     * @param e Event.
     ********************************************************************/
    @Override
    public void mousePressed(final MouseEvent e) {
        final int x = e.getX();
        final int y = e.getY();
        switch (tb.getCurrentTool()) {
            case turboRegPointHandler.MAGNIFIER: {
                final int flags = e.getModifiers();
                if ((flags & (Event.ALT_MASK | Event.META_MASK | Event.CTRL_MASK)) != 0) {
                    mainImp.getWindow().getCanvas().zoomOut(x, y);
                } else {
                    mainImp.getWindow().getCanvas().zoomIn(x, y);
                }
                break;
            }
            case turboRegPointHandler.MOVE_CROSS: {
                final int currentPoint = mainPh.findClosest(x, y);
                secondaryPh.setCurrentPoint(currentPoint);
                updateAndDraw();
                break;
            }
        }
    } /* end mousePressed */

    /*********************************************************************
     * Listen to <code>mouseReleased</code> events.
     *
     * @param e Ignored.
     ********************************************************************/
    @Override
    public void mouseReleased(final MouseEvent e) {
    } /* end mouseReleased */

    /*
     * ....................................................................
     * MouseMotionListener methods
     * ....................................................................
     */
    /*********************************************************************
     * Listen to <code>mouseDragged</code> events. Move the position of the current point. Update the window's ROI.
     * Update ImageJ's window.
     *
     * @param e Event.
     * @see turboRegPointHandler#movePoint(int, int)
     * @see turboRegPointAction#mouseMoved(java.awt.event.MouseEvent)
     ********************************************************************/
    @Override
    public void mouseDragged(final MouseEvent e) {
        final int x = e.getX();
        final int y = e.getY();
        if (tb.getCurrentTool() == turboRegPointHandler.MOVE_CROSS) {
            mainPh.movePoint(x, y);
            updateAndDraw();
        }
        mouseMoved(e);
    } /* end mouseDragged */

    /*********************************************************************
     * Listen to <code>mouseMoved</code> events. Update the ImageJ status by displaying the value of the pixel under the
     * cursor hot spot.
     *
     * @param e Event.
     ********************************************************************/
    @Override
    public void mouseMoved(final MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        x = mainImp.getWindow().getCanvas().offScreenX(x);
        y = mainImp.getWindow().getCanvas().offScreenY(y);
        IJ.showStatus(mainImp.getLocationAsString(x, y) + getValueAsString(x, y));
    } /* end mouseMoved */

    /*
     * ....................................................................
     * constructors
     * ....................................................................
     */
    /*********************************************************************
     * Keep a local copy of the <code>turboRegPointHandler</code> and <code>turboRegPointToolbar</code> objects.
     *
     * @param imp <code>ImagePlus</code> object.
     * @param ph <code>turboRegPointHandler</code> object.
     * @param tb <code>turboRegPointToolbar</code> object.
     ********************************************************************/
    public turboRegPointAction(final ImagePlus imp, final turboRegPointHandler ph, final turboRegPointToolbar tb) {
        super(imp);
        this.mainImp = imp;
        this.mainPh = ph;
        this.tb = tb;
    } /* end turboRegPointAction */

    /*
     * ....................................................................
     * public methods
     * ....................................................................
     */
    /*********************************************************************
     * Set a reference to the <code>ImagePlus</code> and <code>turboRegPointHandler</code> objects of the other image.
     *
     * @param secondaryImp <code>ImagePlus</code> object.
     * @param secondaryPh <code>turboRegPointHandler</code> object.
     ********************************************************************/
    public void setSecondaryPointHandler(final ImagePlus secondaryImp, final turboRegPointHandler secondaryPh) {
        this.secondaryImp = secondaryImp;
        this.secondaryPh = secondaryPh;
    } /* end setSecondaryPointHandler */

    /*
     * ....................................................................
     * private methods
     * ....................................................................
     */
    /*------------------------------------------------------------------*/
    private String getValueAsString(final int x, final int y) {
        final Calibration cal = imp.getCalibration();
        final int[] v = imp.getPixel(x, y);
        switch (imp.getType()) {
            case ImagePlus.GRAY8:
            case ImagePlus.GRAY16: {
                final double cValue = cal.getCValue(v[0]);
                if (cValue == v[0]) {
                    return (", value=" + v[0]);
                } else {
                    return (", value=" + IJ.d2s(cValue) + " (" + v[0] + ")");
                }
            }
            case ImagePlus.GRAY32: {
                return (", value=" + Float.intBitsToFloat(v[0]));
            }
            case ImagePlus.COLOR_256: {
                return (", index=" + v[3] + ", value=" + v[0] + "," + v[1] + "," + v[2]);
            }
            case ImagePlus.COLOR_RGB: {
                return (", value=" + v[0] + "," + v[1] + "," + v[2]);
            }
            default: {
                return ("");
            }
        }
    } /* end getValueAsString */

    /*------------------------------------------------------------------*/
    private void updateAndDraw() {
        mainImp.setRoi(mainPh);
        secondaryImp.setRoi(secondaryPh);
    } /* end updateAndDraw */

    /*------------------------------------------------------------------*/
    private void updateStatus() {
        final Point p = mainPh.getPoint();
        if (p == null) {
            IJ.showStatus("");
            return;
        }
        final int x = p.x;
        final int y = p.y;
        IJ.showStatus(imp.getLocationAsString(x, y) + getValueAsString(x, y));
    } /* end updateStatus */

} /* end class turboRegPointAction */

/*
 * ==================================================================== |
 * turboRegPointHandler
 * \===================================================================
 */

/*********************************************************************
 * This class implements the graphic interactions when dealing with landmarks.
 ********************************************************************/
class turboRegPointHandler extends PolygonRoi

{ /* class turboRegPointHandler */

    /*
     * ....................................................................
     * public variables
     * ....................................................................
     */
    /*********************************************************************
     * The magnifying tool is set in eleventh position to be coherent with ImageJ.
     ********************************************************************/
    public static final int MAGNIFIER = 11;

    /*********************************************************************
     * The moving tool is set in second position to be coherent with the <code>PointPicker_</code> plugin.
     ********************************************************************/
    public static final int MOVE_CROSS = 1;

    /*********************************************************************
     * The number of points we are willing to deal with is at most <code>4</code>.
     *
     * @see turboRegDialog#transformation
     ********************************************************************/
    public static final int NUM_POINTS = 4;

    /*
     * ....................................................................
     * private variables
     * ....................................................................
     */
    /*********************************************************************
     * Serialization version number.
     ********************************************************************/
    private static final long serialVersionUID = 1L;

    /*********************************************************************
     * The drawn landmarks fit in a 11x11 matrix.
     ********************************************************************/
    private static final int CROSS_HALFSIZE = 5;

    /*********************************************************************
     * The golden ratio mathematical constant determines where to put the initial landmarks.
     ********************************************************************/
    private static final double GOLDEN_RATIO = 0.5 * (Math.sqrt(5.0) - 1.0);

    private boolean interactive = true;

    // private boolean started = false;
    private double[][] precisionPoint = new double[NUM_POINTS][2];

    private final Point[] point = new Point[NUM_POINTS];

    private final Color[] spectrum = new Color[NUM_POINTS];

    private TransformationType transformation;

    private int currentPoint = 0;

    /*
     * ....................................................................
     * constructors
     * ....................................................................
     */
    /*********************************************************************
     * Keep a local copy of the points and of the transformation.
     ********************************************************************/
    public turboRegPointHandler(final double[][] precisionPoint, final TransformationType transformation) {
        super(new Polygon(), POLYGON);
        this.transformation = transformation;
        this.precisionPoint = precisionPoint;
        interactive = false;
    } /* end turboRegPointHandler */

    /*********************************************************************
     * Keep a local copy of the <code>ImagePlus</code> object. Set the landmarks to their initial position for the given
     * transformation.
     *
     * @param imp <code>ImagePlus</code> object.
     * @param transformation Transformation code.
     * @see turboRegDialog#restoreAll()
     ********************************************************************/
    public turboRegPointHandler(final ImagePlus imp, final TransformationType transformation) {
        super(0, 0, imp);
        this.imp = imp;
        this.transformation = transformation;
        setTransformation(transformation);
        imp.setRoi(this);
        // started = true;
    } /* end turboRegPointHandler */

    /*********************************************************************
     * Set the landmarks to their initial position for the given transformation.
     *
     * @param transformation Transformation code.
     * @see turboRegDialog#restoreAll()
     ********************************************************************/
    public turboRegPointHandler(final TransformationType transformation, final ImagePlus imp) {
        super(new Polygon(), POLYGON);
        this.imp = imp;
        this.transformation = transformation;
        setTransformation(transformation);
        imp.setRoi(this);
        // started = true;
    } /* end turboRegPointHandler */

    /*
     * ....................................................................
     * public methods
     * ....................................................................
     */
    /*********************************************************************
     * Set the current point as that which is closest to (x, y).
     *
     * @param x Horizontal coordinate in canvas units.
     * @param y Vertical coordinate in canvas units.
     ********************************************************************/
    public int findClosest(int x, int y) {
        x = ic.offScreenX(x);
        y = ic.offScreenY(y);
        int closest = 0;
        Point p = point[closest];
        double distance = ((double)(x - p.x) * (double)(x - p.x)) + ((double)(y - p.y) * (double)(y - p.y));
        double candidate;
        if (transformation == TransformationType.RIGID_BODY) {
            for (int k = 1; (k < transformation.getNumParam()); k++) {
                p = point[k];
                candidate = ((double)(x - p.x) * (double)(x - p.x)) + ((double)(y - p.y) * (double)(y - p.y));
                if (candidate < distance) {
                    distance = candidate;
                    closest = k;
                }
            }
        } else {
            for (int k = 1; (k < (transformation.getNumParam() / 2)); k++) {
                p = point[k];
                candidate = ((double)(x - p.x) * (double)(x - p.x)) + ((double)(y - p.y) * (double)(y - p.y));
                if (candidate < distance) {
                    distance = candidate;
                    closest = k;
                }
            }
        }
        currentPoint = closest;
        return (currentPoint);
    } /* end findClosest */

    /*********************************************************************
     * Return the current point as a <code>Point</code> object.
     ********************************************************************/
    public Point getPoint() {
        return (point[currentPoint]);
    } /* end getPoint */

    /*********************************************************************
     * Return all landmarks as an array <code>double[transformation / 2][2]</code>, except for a rigid-body
     * transformation for which the array has size <code>double[3][2]</code> .
     ********************************************************************/
    public double[][] getPoints() {
        if (interactive) {
            if (transformation == TransformationType.RIGID_BODY) {
                final double[][] points = new double[transformation.getNumParam()][2];
                for (int k = 0; (k < transformation.getNumParam()); k++) {
                    points[k][0] = point[k].x;
                    points[k][1] = point[k].y;
                }
                return (points);
            } else {
                final double[][] points = new double[transformation.getNumParam() / 2][2];
                for (int k = 0; (k < (transformation.getNumParam() / 2)); k++) {
                    points[k][0] = point[k].x;
                    points[k][1] = point[k].y;
                }
                return (points);
            }
        } else {
            return (precisionPoint);
        }
    } /* end getPoints */

    /*********************************************************************
     * Modify the location of the current point. Clip the admissible range to the image size.
     *
     * @param x Desired new horizontal coordinate in canvas units.
     * @param y Desired new vertical coordinate in canvas units.
     ********************************************************************/
    public void movePoint(int x, int y) {
        interactive = true;
        x = ic.offScreenX(x);
        y = ic.offScreenY(y);
        x = (x < 0) ? (0) : (x);
        x = (imp.getWidth() <= x) ? (imp.getWidth() - 1) : (x);
        y = (y < 0) ? (0) : (y);
        y = (imp.getHeight() <= y) ? (imp.getHeight() - 1) : (y);
        if ((transformation == TransformationType.RIGID_BODY) && (currentPoint != 0)) {
            final Point p = new Point(x, y);
            final Point q = point[3 - currentPoint];
            final double radius =
                    0.5 * Math.sqrt(((ic.screenX(p.x) - ic.screenX(q.x)) * (ic.screenX(p.x) - ic.screenX(q.x)))
                            + ((ic.screenY(p.y) - ic.screenY(q.y)) * (ic.screenY(p.y) - ic.screenY(q.y))));
            if (CROSS_HALFSIZE < radius) {
                point[currentPoint].x = x;
                point[currentPoint].y = y;
            }
        } else {
            point[currentPoint].x = x;
            point[currentPoint].y = y;
        }
    } /* end movePoint */

    /*********************************************************************
     * Set a new current point.
     *
     * @param currentPoint New current point index.
     ********************************************************************/
    public void setCurrentPoint(final int currentPoint) {
        this.currentPoint = currentPoint;
    } /* end setCurrentPoint */

    /*********************************************************************
     * Set new position for all landmarks, without clipping.
     *
     * @param precisionPoint New coordinates in canvas units.
     ********************************************************************/
    public void setPoints(final double[][] precisionPoint) {
        interactive = false;
        if (transformation == TransformationType.RIGID_BODY) {
            for (int k = 0; (k < transformation.getNumParam()); k++) {
                point[k].x = (int)Math.round(precisionPoint[k][0]);
                point[k].y = (int)Math.round(precisionPoint[k][1]);
                this.precisionPoint[k][0] = precisionPoint[k][0];
                this.precisionPoint[k][1] = precisionPoint[k][1];
            }
        } else {
            for (int k = 0; (k < (transformation.getNumParam() / 2)); k++) {
                point[k].x = (int)Math.round(precisionPoint[k][0]);
                point[k].y = (int)Math.round(precisionPoint[k][1]);
                this.precisionPoint[k][0] = precisionPoint[k][0];
                this.precisionPoint[k][1] = precisionPoint[k][1];
            }
        }
    } /* end setPoints */

    /*********************************************************************
     * Reset the landmarks to their initial position for the given transformation.
     *
     * @param transformation Transformation code.
     ********************************************************************/
    public void setTransformation(final TransformationType transformation) {
        interactive = true;
        this.transformation = transformation;
        final int width = imp.getWidth();
        final int height = imp.getHeight();
        currentPoint = 0;
        switch (transformation) {
            case TRANSLATION: {
                point[0] =
                        new Point(Math.round((float)(Math.floor(0.5 * width))), Math.round((float)(Math
                                .floor(0.5 * height))));
                break;
            }
            case RIGID_BODY: {
                point[0] =
                        new Point(Math.round((float)(Math.floor(0.5 * width))), Math.round((float)(Math
                                .floor(0.5 * height))));
                point[1] =
                        new Point(Math.round((float)(Math.floor(0.5 * width))), Math.round((float)(Math.ceil(0.25
                                * GOLDEN_RATIO * height))));
                point[2] =
                        new Point(Math.round((float)(Math.floor(0.5 * width))), height
                                - Math.round((float)(Math.ceil(0.25 * GOLDEN_RATIO * height))));
                break;
            }
            case SCALED_ROTATION: {
                point[0] =
                        new Point(Math.round((float)(Math.floor(0.25 * GOLDEN_RATIO * width))), Math.round((float)(Math
                                .floor(0.5 * height))));
                point[1] =
                        new Point(width - Math.round((float)(Math.ceil(0.25 * GOLDEN_RATIO * width))),
                                Math.round((float)(Math.floor(0.5 * height))));
                break;
            }
            case AFFINE: {
                point[0] =
                        new Point(Math.round((float)(Math.floor(0.5 * width))), Math.round((float)(Math.floor(0.25
                                * GOLDEN_RATIO * height))));
                point[1] =
                        new Point(Math.round((float)(Math.floor(0.25 * GOLDEN_RATIO * width))), height
                                - Math.round((float)(Math.ceil(0.25 * GOLDEN_RATIO * height))));
                point[2] =
                        new Point(width - Math.round((float)(Math.ceil(0.25 * GOLDEN_RATIO * width))), height
                                - Math.round((float)(Math.ceil(0.25 * GOLDEN_RATIO * height))));
                break;
            }
        }
        setSpectrum();
        imp.updateAndDraw();
    } /* end setTransformation */

    /*------------------------------------------------------------------*/
    private void setSpectrum() {
        if (transformation == TransformationType.RIGID_BODY) {
            spectrum[0] = Color.green;
            spectrum[1] = new Color(16, 119, 169);
            spectrum[2] = new Color(119, 85, 51);
        } else {
            spectrum[0] = Color.green;
            spectrum[1] = Color.yellow;
            spectrum[2] = Color.magenta;
            spectrum[3] = Color.cyan;
        }
    } /* end setSpectrum */

} /* end class turboRegPointHandler */

/*
 * ==================================================================== |
 * turboRegPointToolbar
 * \===================================================================
 */

/*********************************************************************
 * This class implements the user interactions when dealing with the toolbar in the ImageJ's window.
 ********************************************************************/
class turboRegPointToolbar extends Canvas implements MouseListener

{ /* class turboRegPointToolbar */

    /*
     * ....................................................................
     * private variables
     * ....................................................................
     */
    /*********************************************************************
     * Serialization version number.
     ********************************************************************/
    private static final long serialVersionUID = 1L;

    /*********************************************************************
     * Same number of tools than in ImageJ version 1.22
     ********************************************************************/
    private static final int NUM_TOOLS = 19;

    /*********************************************************************
     * Same tool offset than in ImageJ version 1.22
     ********************************************************************/
    private static final int OFFSET = 3;

    /*********************************************************************
     * Same tool size than in ImageJ version 1.22
     ********************************************************************/
    private static final int SIZE = 22;

    private final Color gray = Color.lightGray;

    private final Color brighter = gray.brighter();

    private final Color darker = gray.darker();

    private final Color evenDarker = darker.darker();

    private final boolean[] down = new boolean[NUM_TOOLS];

    private final turboRegPointToolbar instance;

    private final Toolbar previousInstance;

    private Graphics g;

    private int currentTool = turboRegPointHandler.MOVE_CROSS;

    private int x;

    private int y;

    private int xOffset;

    private int yOffset;

    /*
     * ....................................................................
     * Canvas methods
     * ....................................................................
     */
    /*********************************************************************
     * Draw the toolbar tools.
     *
     * @param g Graphics environment.
     ********************************************************************/
    @Override
    public void paint(final Graphics g) {
        drawButtons(g);
    } /* paint */

    /*
     * ....................................................................
     * MouseListener methods
     * ....................................................................
     */
    /*********************************************************************
     * Listen to <code>mouseClicked</code> events.
     *
     * @param e Ignored.
     ********************************************************************/
    @Override
    public void mouseClicked(final MouseEvent e) {
    } /* end mouseClicked */

    /*********************************************************************
     * Listen to <code>mouseEntered</code> events.
     *
     * @param e Ignored.
     ********************************************************************/
    @Override
    public void mouseEntered(final MouseEvent e) {
    } /* end mouseEntered */

    /*********************************************************************
     * Listen to <code>mouseExited</code> events.
     *
     * @param e Ignored.
     ********************************************************************/
    @Override
    public void mouseExited(final MouseEvent e) {
    } /* end mouseExited */

    /*********************************************************************
     * Listen to <code>mousePressed</code> events. Set the current tool index.
     *
     * @param e Event.
     ********************************************************************/
    @Override
    public void mousePressed(final MouseEvent e) {
        final int x = e.getX();
        int newTool = 0;
        for (int i = 0; (i < NUM_TOOLS); i++) {
            if (((i * SIZE) < x) && (x < ((i * SIZE) + SIZE))) {
                newTool = i;
            }
        }
        setTool(newTool);
    } /* mousePressed */

    /*********************************************************************
     * Listen to <code>mouseReleased</code> events.
     *
     * @param e Ignored.
     ********************************************************************/
    @Override
    public void mouseReleased(final MouseEvent e) {
    } /* end mouseReleased */

    /*
     * ....................................................................
     * constructors
     * ....................................................................
     */
    /*********************************************************************
     * Override the ImageJ toolbar by this <code>turboRegToolbar</code> object. Store a local copy of the ImageJ's
     * toolbar for later restore.
     *
     * @see turboRegPointToolbar#restorePreviousToolbar()
     ********************************************************************/
    public turboRegPointToolbar(final Toolbar previousToolbar) {
        previousInstance = previousToolbar;
        instance = this;
        final Container container = previousToolbar.getParent();
        final Component[] component = container.getComponents();
        for (int i = 0; (i < component.length); i++) {
            if (component[i] == previousToolbar) {
                container.remove(previousToolbar);
                container.add(this, i);
                break;
            }
        }
        resetButtons();
        down[currentTool] = true;
        setTool(currentTool);
        setForeground(Color.black);
        setBackground(gray);
        addMouseListener(this);
        container.validate();
    } /* end turboRegPointToolbar */

    /*
     * ....................................................................
     * public methods
     * ....................................................................
     */
    /*********************************************************************
     * Return the current tool index.
     ********************************************************************/
    public int getCurrentTool() {
        return (currentTool);
    } /* getCurrentTool */

    /*********************************************************************
     * Restore the ImageJ toolbar.
     ********************************************************************/
    public void restorePreviousToolbar() {
        final Container container = instance.getParent();
        final Component[] component = container.getComponents();
        for (int i = 0; (i < component.length); i++) {
            if (component[i] == instance) {
                container.remove(instance);
                container.add(previousInstance, i);
                container.validate();
                break;
            }
        }
    } /* end restorePreviousToolbar */

    /*********************************************************************
     * Set the current tool and update its appearance on the toolbar.
     *
     * @param tool Tool index.
     ********************************************************************/
    public void setTool(final int tool) {
        if (tool == currentTool) {
            return;
        }
        down[tool] = true;
        down[currentTool] = false;
        final Graphics g = this.getGraphics();
        drawButton(g, currentTool);
        drawButton(g, tool);
        g.dispose();
        showMessage(tool);
        currentTool = tool;
    } /* end setTool */

    /*
     * ....................................................................
     * private methods
     * ....................................................................
     */
    /*------------------------------------------------------------------*/
    private void d(int x, int y) {
        x += xOffset;
        y += yOffset;
        g.drawLine(this.x, this.y, x, y);
        this.x = x;
        this.y = y;
    } /* end d */

    /*------------------------------------------------------------------*/
    private void drawButton(final Graphics g, final int tool) {
        fill3DRect(g, (tool * SIZE) + 1, 1, SIZE, SIZE - 1, !down[tool]);
        g.setColor(Color.black);
        int x = (tool * SIZE) + OFFSET;
        int y = OFFSET;
        if (down[tool]) {
            x++;
            y++;
        }
        this.g = g;
        switch (tool) {
            case turboRegPointHandler.MOVE_CROSS: {
                xOffset = x;
                yOffset = y;
                m(1, 1);
                d(1, 10);
                m(2, 2);
                d(2, 9);
                m(3, 3);
                d(3, 8);
                m(4, 4);
                d(4, 7);
                m(5, 5);
                d(5, 7);
                m(6, 6);
                d(6, 7);
                m(7, 7);
                d(7, 7);
                m(11, 5);
                d(11, 6);
                m(10, 7);
                d(10, 8);
                m(12, 7);
                d(12, 8);
                m(9, 9);
                d(9, 11);
                m(13, 9);
                d(13, 11);
                m(10, 12);
                d(10, 15);
                m(12, 12);
                d(12, 15);
                m(11, 9);
                d(11, 10);
                m(11, 13);
                d(11, 15);
                m(9, 13);
                d(13, 13);
                break;
            }
            case turboRegPointHandler.MAGNIFIER: {
                xOffset = x + 2;
                yOffset = y + 2;
                m(3, 0);
                d(3, 0);
                d(5, 0);
                d(8, 3);
                d(8, 5);
                d(7, 6);
                d(7, 7);
                d(6, 7);
                d(5, 8);
                d(3, 8);
                d(0, 5);
                d(0, 3);
                d(3, 0);
                m(8, 8);
                d(9, 8);
                d(13, 12);
                d(13, 13);
                d(12, 13);
                d(8, 9);
                d(8, 8);
                break;
            }
        }
    } /* end drawButton */

    /*------------------------------------------------------------------*/
    private void drawButtons(final Graphics g) {
        for (int i = 0; (i < NUM_TOOLS); i++) {
            drawButton(g, i);
        }
    } /* end drawButtons */

    /*------------------------------------------------------------------*/
    private void fill3DRect(final Graphics g, final int x, final int y, final int width, final int height,
                            final boolean raised) {
        if (raised) {
            g.setColor(gray);
        } else {
            g.setColor(darker);
        }
        g.fillRect(x + 1, y + 1, width - 2, height - 2);
        g.setColor((raised) ? (brighter) : (evenDarker));
        g.drawLine(x, y, x, (y + height) - 1);
        g.drawLine(x + 1, y, (x + width) - 2, y);
        g.setColor((raised) ? (evenDarker) : (brighter));
        g.drawLine(x + 1, (y + height) - 1, (x + width) - 1, (y + height) - 1);
        g.drawLine((x + width) - 1, y, (x + width) - 1, (y + height) - 2);
    } /* end fill3DRect */

    /*------------------------------------------------------------------*/
    private void m(final int x, final int y) {
        this.x = xOffset + x;
        this.y = yOffset + y;
    } /* end m */

    /*------------------------------------------------------------------*/
    private void resetButtons() {
        for (int i = 0; (i < NUM_TOOLS); i++) {
            down[i] = false;
        }
    } /* end resetButtons */

    /*------------------------------------------------------------------*/
    private void showMessage(final int tool) {
        switch (tool) {
            case turboRegPointHandler.MOVE_CROSS: {
                IJ.showStatus("Move crosses");
                break;
            }
            case turboRegPointHandler.MAGNIFIER: {
                IJ.showStatus("Magnifying glass");
                break;
            }
            default: {
                IJ.showStatus("Undefined operation");
                break;
            }
        }
    } /* end showMessage */

} /* end class turboRegPointToolbar */

/*
 * ==================================================================== |
 * turboRegProgressBar
 * \===================================================================
 */

/*********************************************************************
 * This class implements the interactions when dealing with ImageJ's progress bar.
 ********************************************************************/
class turboRegProgressBar

{ /* class turboRegProgressBar */

    /*
     * ....................................................................
     * private variables
     * ....................................................................
     */
    /*********************************************************************
     * Same time constant than in ImageJ version 1.22
     ********************************************************************/
    private static final long TIME_QUANTUM = 50L;

    private static volatile long lastTime = System.currentTimeMillis();

    private static volatile int completed = 0;

    private static volatile int workload = 0;

    /*
     * ....................................................................
     * public methods
     * ....................................................................
     */
    /*********************************************************************
     * Extend the amount of work to perform by <code>batch</code>.
     *
     * @param batch Additional amount of work that need be performed.
     ********************************************************************/
    public static synchronized void addWorkload(final int batch) {
        workload += batch;
    } /* end addWorkload */

    /*********************************************************************
     * Erase the progress bar and cancel pending operations.
     ********************************************************************/
    public static synchronized void resetProgressBar() {
        final long timeStamp = System.currentTimeMillis();
        if ((timeStamp - lastTime) < TIME_QUANTUM) {
            try {
                Thread.sleep((TIME_QUANTUM - timeStamp) + lastTime);
            } catch (final InterruptedException e) {
                IJ.log("Unexpected interruption exception " + e.getMessage());
            }
        }
        lastTime = timeStamp;
        completed = 0;
        workload = 0;
        IJ.showProgress(1.0);
    } /* end resetProgressBar */

    /*********************************************************************
     * Perform <code>stride</code> operations at once.
     *
     * @param stride Amount of work that is skipped.
     ********************************************************************/
    public static synchronized void skipProgressBar(final int stride) {
        completed += stride - 1;
        stepProgressBar();
    } /* end skipProgressBar */

    /*********************************************************************
     * Perform <code>1</code> operation unit.
     ********************************************************************/
    public static synchronized void stepProgressBar() {
        final long timeStamp = System.currentTimeMillis();
        completed = completed + 1;
        if ((TIME_QUANTUM <= (timeStamp - lastTime)) | (completed == workload)) {
            lastTime = timeStamp;
            IJ.showProgress((double)completed / (double)workload);
        }
    } /* end stepProgressBar */

    /*********************************************************************
     * Acknowledge that <code>batch</code> work has been performed.
     *
     * @param batch Completed amount of work.
     ********************************************************************/
    public static synchronized void workloadDone(final int batch) {
        workload -= batch;
        completed -= batch;
    } /* end workloadDone */

} /* end class turboRegProgressBar */

/*
 * ==================================================================== |
 * turboRegTransform
 * \===================================================================
 */

/*********************************************************************
 * This class implements the algorithmic methods of the plugin. It refines the landmarks and computes the final images.
 ********************************************************************/
class turboRegTransform

{ /* class turboRegTransform */

    /*
     * ....................................................................
     * private variables
     * ....................................................................
     */
    /*********************************************************************
     * Maximal number of registration iterations per level, when speed is requested at the expense of accuracy. This
     * number must be corrected so that there are more iterations at the coarse levels of the pyramid than at the fine
     * levels.
     *
     * @see turboRegTransform#ITERATION_PROGRESSION
     ********************************************************************/
    private static final int FEW_ITERATIONS = 5;

    /*********************************************************************
     * Initial value of the Marquardt-Levenberg fudge factor.
     ********************************************************************/
    private static final double FIRST_LAMBDA = 1.0;

    /*********************************************************************
     * Update parameter of the Marquardt-Levenberg fudge factor.
     ********************************************************************/
    private static final double LAMBDA_MAGSTEP = 4.0;

    /*********************************************************************
     * Maximal number of registration iterations per level, when accuracy is requested at the expense of speed. This
     * number must be corrected so that there are more iterations at the coarse levels of the pyramid than at the fine
     * levels.
     *
     * @see turboRegTransform#ITERATION_PROGRESSION
     ********************************************************************/
    private static final int MANY_ITERATIONS = 10;

    /*********************************************************************
     * Minimal update distance of the landmarks, in pixel units, when accuracy is requested at the expense of speed.
     * This distance does not depend on the pyramid level.
     ********************************************************************/
    private static final double PIXEL_HIGH_PRECISION = 0.001;

    /*********************************************************************
     * Minimal update distance of the landmarks, in pixel units, when speed is requested at the expense of accuracy.
     * This distance does not depend on the pyramid level.
     ********************************************************************/
    private static final double PIXEL_LOW_PRECISION = 0.1;

    /*********************************************************************
     * Multiplicative factor that determines how many more iterations are allowed for a pyramid level one unit coarser.
     ********************************************************************/
    private static final int ITERATION_PROGRESSION = 2;

    private boolean accelerated;

    private final boolean interactive;

    // private double c0;
    // private double c0u;
    // private double c0v;
    // private double c0uv;
    // private double c1;
    // private double c1u;
    // private double c1v;
    // private double c1uv;
    // private double c2;
    // private double c2u;
    // private double c2v;
    // private double c2uv;
    // private double c3;
    // private double c3u;
    // private double c3v;
    // private double c3uv;
    private double pixelPrecision;

    private double s;

    private double t;

    private double targetJacobian;

    private double x;

    private double y;

    private double[][] sourcePoint;

    private double[][] targetPoint;

    // private final double[] dxWeight = new double[4];
    // private final double[] dyWeight = new double[4];
    private final double[] xWeight = new double[4];

    private final double[] yWeight = new double[4];

    private final int[] xIndex = new int[4];

    private final int[] yIndex = new int[4];

    private float[] inImg;

    private float[] inMsk;

    private float[] outImg;

    private float[] outMsk;

    private float[] xGradient;

    private float[] yGradient;

    private int inNx;

    private int inNy;

    private int iterationCost;

    private int iterationPower;

    private int maxIterations;

    private int outNx;

    private int outNy;

    private int p;

    private int pyramidDepth;

    private int q;

    private TransformationType transformation;

    private int twiceInNx;

    private int twiceInNy;

    private turboRegImage sourceImg;

    private turboRegImage targetImg;

    private final turboRegMask sourceMsk;

    private final turboRegMask targetMsk;

    private turboRegPointHandler sourcePh;

    /*
     * ....................................................................
     * constructors
     * ....................................................................
     */
    /*********************************************************************
     * Keep a local copy of most everything. Select among the pre-stored constants.
     *
     * @param targetImg Target image pyramid.
     * @param targetMsk Target mask pyramid.
     * @param sourceImg Source image pyramid.
     * @param sourceMsk Source mask pyramid.
     * @param targetPh Target <code>turboRegPointHandler</code> object.
     * @param sourcePh Source <code>turboRegPointHandler</code> object.
     * @param transformation Transformation code.
     * @param accelerated Trade-off between speed and accuracy.
     * @param interactive Shows or hides the resulting image.
     ********************************************************************/
    public turboRegTransform(final turboRegImage sourceImg, final turboRegMask sourceMsk,
                             final turboRegPointHandler sourcePh, final turboRegImage targetImg,
                             final turboRegMask targetMsk, final turboRegPointHandler targetPh,
                             final TransformationType transformation, final boolean accelerated,
                             final boolean interactive) {
        this.sourceImg = sourceImg;
        this.sourceMsk = sourceMsk;
        this.sourcePh = sourcePh;
        this.targetImg = targetImg;
        this.targetMsk = targetMsk;
        this.transformation = transformation;
        this.accelerated = accelerated;
        this.interactive = interactive;
        sourcePoint = sourcePh.getPoints();
        targetPoint = targetPh.getPoints();
        if (accelerated) {
            pixelPrecision = PIXEL_LOW_PRECISION;
            maxIterations = FEW_ITERATIONS;
        } else {
            pixelPrecision = PIXEL_HIGH_PRECISION;
            maxIterations = MANY_ITERATIONS;
        }
    } /* end turboRegTransform */

    /*********************************************************************
     * Compute the final image.
     ********************************************************************/
    public void doBatchFinalTransform(final float[] pixels) {
        if (accelerated) {
            inImg = sourceImg.getImage();
        } else {
            inImg = sourceImg.getCoefficient();
        }
        inNx = sourceImg.getWidth();
        inNy = sourceImg.getHeight();
        twiceInNx = 2 * inNx;
        twiceInNy = 2 * inNy;
        outImg = pixels;
        outNx = targetImg.getWidth();
        outNy = targetImg.getHeight();
        final double[][] matrix = getTransformationMatrix(targetPoint, sourcePoint);
        switch (transformation) {
            case TRANSLATION: {
                translationTransform(matrix);
                break;
            }
            case RIGID_BODY:
            case SCALED_ROTATION:
            case AFFINE: {
                affineTransform(matrix);
                break;
            }
        }
    } /* end doBatchFinalTransform */

    /*********************************************************************
     * Compute the final image.
     ********************************************************************/
    public ImagePlus doFinalTransform(final int width, final int height) {
        if (accelerated) {
            inImg = sourceImg.getImage();
        } else {
            inImg = sourceImg.getCoefficient();
        }
        inMsk = sourceMsk.getMask();
        inNx = sourceImg.getWidth();
        inNy = sourceImg.getHeight();
        twiceInNx = 2 * inNx;
        twiceInNy = 2 * inNy;
        final ImageStack is = new ImageStack(width, height);
        final FloatProcessor dataFp = new FloatProcessor(width, height);
        is.addSlice("Data", dataFp);
        final FloatProcessor maskFp = new FloatProcessor(width, height);
        is.addSlice("Mask", maskFp);
        final ImagePlus imp = new ImagePlus("Output", is);
        imp.setSlice(1);
        outImg = (float[])dataFp.getPixels();
        imp.setSlice(2);
        final float[] outMsk = (float[])maskFp.getPixels();
        outNx = imp.getWidth();
        outNy = imp.getHeight();
        final double[][] matrix = getTransformationMatrix(targetPoint, sourcePoint);
        switch (transformation) {
            case TRANSLATION: {
                translationTransform(matrix, outMsk);
                break;
            }
            case RIGID_BODY:
            case SCALED_ROTATION:
            case AFFINE: {
                affineTransform(matrix, outMsk);
                break;
            }
        }
        imp.setSlice(1);
        imp.getProcessor().resetMinAndMax();
        if (interactive) {
            imp.show();
            imp.updateAndDraw();
        }
        return (imp);
    } /* end doFinalTransform */

    /*********************************************************************
     * Compute the final image.
     ********************************************************************/
    public float[] doFinalTransform(final turboRegImage sourceImg, final turboRegPointHandler sourcePh,
                                    final turboRegImage targetImg, final turboRegPointHandler targetPh,
                                    final TransformationType transformation, final boolean accelerated) {
        this.sourceImg = sourceImg;
        this.targetImg = targetImg;
        this.sourcePh = sourcePh;
        this.transformation = transformation;
        this.accelerated = accelerated;
        sourcePoint = sourcePh.getPoints();
        targetPoint = targetPh.getPoints();
        if (accelerated) {
            inImg = sourceImg.getImage();
        } else {
            inImg = sourceImg.getCoefficient();
        }
        inNx = sourceImg.getWidth();
        inNy = sourceImg.getHeight();
        twiceInNx = 2 * inNx;
        twiceInNy = 2 * inNy;
        outNx = targetImg.getWidth();
        outNy = targetImg.getHeight();
        outImg = new float[outNx * outNy];
        final double[][] matrix = getTransformationMatrix(targetPoint, sourcePoint);
        switch (transformation) {
            case TRANSLATION: {
                translationTransform(matrix);
                break;
            }
            case RIGID_BODY:
            case SCALED_ROTATION:
            case AFFINE: {
                affineTransform(matrix);
                break;
            }
        }
        return (outImg);
    } /* end doFinalTransform */

    /*********************************************************************
     * Refine the landmarks.
     ********************************************************************/
    public void doRegistration() {
        Stack<?> sourceImgPyramid;
        Stack<?> sourceMskPyramid;
        Stack<?> targetImgPyramid;
        Stack<?> targetMskPyramid;
        if (sourceMsk == null) {
            sourceImgPyramid = sourceImg.getPyramid();
            sourceMskPyramid = null;
            targetImgPyramid = (Stack<?>)targetImg.getPyramid().clone();
            targetMskPyramid = (Stack<?>)targetMsk.getPyramid().clone();
        } else {
            sourceImgPyramid = sourceImg.getPyramid();
            sourceMskPyramid = sourceMsk.getPyramid();
            targetImgPyramid = targetImg.getPyramid();
            targetMskPyramid = targetMsk.getPyramid();
        }
        pyramidDepth = targetImg.getPyramidDepth();
        iterationPower = (int)Math.pow(ITERATION_PROGRESSION, pyramidDepth);
        turboRegProgressBar.addWorkload(((pyramidDepth * maxIterations * iterationPower) / ITERATION_PROGRESSION)
                - ((iterationPower - 1) / (ITERATION_PROGRESSION - 1)));
        iterationCost = 1;
        scaleBottomDownLandmarks();
        while (!targetImgPyramid.isEmpty()) {
            iterationPower /= ITERATION_PROGRESSION;
            inNx = ((Integer)targetImgPyramid.pop()).intValue();
            inNy = ((Integer)targetImgPyramid.pop()).intValue();
            inImg = (float[])targetImgPyramid.pop();
            inMsk = (float[])targetMskPyramid.pop();
            outNx = ((Integer)sourceImgPyramid.pop()).intValue();
            outNy = ((Integer)sourceImgPyramid.pop()).intValue();
            outImg = (float[])sourceImgPyramid.pop();
            xGradient = (float[])sourceImgPyramid.pop();
            yGradient = (float[])sourceImgPyramid.pop();
            if (sourceMskPyramid == null) {
                outMsk = null;
            } else {
                outMsk = (float[])sourceMskPyramid.pop();
            }
            twiceInNx = 2 * inNx;
            twiceInNy = 2 * inNy;
            switch (transformation) {
                case TRANSLATION: {
                    targetJacobian = 1.0;
                    inverseMarquardtLevenbergOptimization((iterationPower * maxIterations) - 1);
                    break;
                }
                case RIGID_BODY: {
                    inverseMarquardtLevenbergRigidBodyOptimization((iterationPower * maxIterations) - 1);
                    break;
                }
                case SCALED_ROTATION: {
                    targetJacobian =
                            ((targetPoint[0][0] - targetPoint[1][0]) * (targetPoint[0][0] - targetPoint[1][0]))
                                    + ((targetPoint[0][1] - targetPoint[1][1]) * (targetPoint[0][1] - targetPoint[1][1]));
                    inverseMarquardtLevenbergOptimization((iterationPower * maxIterations) - 1);
                    break;
                }
                case AFFINE: {
                    targetJacobian =
                            ((targetPoint[1][0] - targetPoint[2][0]) * targetPoint[0][1])
                                    + ((targetPoint[2][0] - targetPoint[0][0]) * targetPoint[1][1])
                                    + ((targetPoint[0][0] - targetPoint[1][0]) * targetPoint[2][1]);
                    inverseMarquardtLevenbergOptimization((iterationPower * maxIterations) - 1);
                    break;
                }
            }
            scaleUpLandmarks();
            sourcePh.setPoints(sourcePoint);
            iterationCost *= ITERATION_PROGRESSION;
        }
        iterationPower /= ITERATION_PROGRESSION;
        inNx = targetImg.getWidth();
        inNy = targetImg.getHeight();
        inImg = targetImg.getCoefficient();
        inMsk = targetMsk.getMask();
        outNx = sourceImg.getWidth();
        outNy = sourceImg.getHeight();
        outImg = sourceImg.getImage();
        xGradient = sourceImg.getXGradient();
        yGradient = sourceImg.getYGradient();
        if (sourceMsk == null) {
            outMsk = null;
        } else {
            outMsk = sourceMsk.getMask();
        }
        twiceInNx = 2 * inNx;
        twiceInNy = 2 * inNy;
        if (accelerated) {
            turboRegProgressBar.skipProgressBar(iterationCost * (maxIterations - 1));
        } else {
            switch (transformation) {
                case RIGID_BODY: {
                    inverseMarquardtLevenbergRigidBodyOptimization(maxIterations - 1);
                    break;
                }
                case TRANSLATION:
                case SCALED_ROTATION:
                case AFFINE: {
                    inverseMarquardtLevenbergOptimization(maxIterations - 1);
                    break;
                }
            }
        }
        sourcePh.setPoints(sourcePoint);
        iterationPower = (int)Math.pow(ITERATION_PROGRESSION, pyramidDepth);
        turboRegProgressBar.workloadDone(((pyramidDepth * maxIterations * iterationPower) / ITERATION_PROGRESSION)
                - ((iterationPower - 1) / (ITERATION_PROGRESSION - 1)));
    } /* end doRegistration */

    /*
     * ....................................................................
     * private methods
     * ....................................................................
     */
    /*------------------------------------------------------------------*/
    private void affineTransform(final double[][] matrix) {
        double yx;
        double yy;
        double x0;
        double y0;
        int xMsk;
        int yMsk;
        int k = 0;
        turboRegProgressBar.addWorkload(outNy);
        yx = matrix[0][0];
        yy = matrix[1][0];
        for (int v = 0; (v < outNy); v++) {
            x0 = yx;
            y0 = yy;
            for (int u = 0; (u < outNx); u++) {
                x = x0;
                y = y0;
                xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                    xMsk += yMsk * inNx;
                    if (accelerated) {
                        outImg[k++] = inImg[xMsk];
                    } else {
                        xIndexes();
                        yIndexes();
                        x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                        y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                        xWeights();
                        yWeights();
                        outImg[k++] = (float)interpolate();
                    }
                } else {
                    outImg[k++] = 0.0F;
                }
                x0 += matrix[0][1];
                y0 += matrix[1][1];
            }
            yx += matrix[0][2];
            yy += matrix[1][2];
            turboRegProgressBar.stepProgressBar();
        }
        turboRegProgressBar.workloadDone(outNy);
    } /* affineTransform */

    /*------------------------------------------------------------------*/
    private void affineTransform(final double[][] matrix, final float[] outMsk) {
        double yx;
        double yy;
        double x0;
        double y0;
        int xMsk;
        int yMsk;
        int k = 0;
        turboRegProgressBar.addWorkload(outNy);
        yx = matrix[0][0];
        yy = matrix[1][0];
        for (int v = 0; (v < outNy); v++) {
            x0 = yx;
            y0 = yy;
            for (int u = 0; (u < outNx); u++) {
                x = x0;
                y = y0;
                xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                    xMsk += yMsk * inNx;
                    if (accelerated) {
                        outImg[k] = inImg[xMsk];
                    } else {
                        xIndexes();
                        yIndexes();
                        x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                        y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                        xWeights();
                        yWeights();
                        outImg[k] = (float)interpolate();
                    }
                    outMsk[k++] = inMsk[xMsk];
                } else {
                    outImg[k] = 0.0F;
                    outMsk[k++] = 0.0F;
                }
                x0 += matrix[0][1];
                y0 += matrix[1][1];
            }
            yx += matrix[0][2];
            yy += matrix[1][2];
            turboRegProgressBar.stepProgressBar();
        }
        turboRegProgressBar.workloadDone(outNy);
    } /* affineTransform */

    /*------------------------------------------------------------------*/
    // private void bilinearTransform(final double[][] matrix) {
    // double yx;
    // double yy;
    // double yxy;
    // double yyy;
    // double x0;
    // double y0;
    // int xMsk;
    // int yMsk;
    // int k = 0;
    // turboRegProgressBar.addWorkload(outNy);
    // yx = matrix[0][0];
    // yy = matrix[1][0];
    // yxy = 0.0;
    // yyy = 0.0;
    // for (int v = 0; (v < outNy); v++) {
    // x0 = yx;
    // y0 = yy;
    // for (int u = 0; (u < outNx); u++) {
    // x = x0;
    // y = y0;
    // xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
    // yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
    // if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk)
    // && (yMsk < inNy)) {
    // xMsk += yMsk * inNx;
    // if (accelerated) {
    // outImg[k++] = inImg[xMsk];
    // } else {
    // xIndexes();
    // yIndexes();
    // x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
    // y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
    // xWeights();
    // yWeights();
    // outImg[k++] = (float) interpolate();
    // }
    // } else {
    // outImg[k++] = 0.0F;
    // }
    // x0 += matrix[0][1] + yxy;
    // y0 += matrix[1][1] + yyy;
    // }
    // yx += matrix[0][2];
    // yy += matrix[1][2];
    // yxy += matrix[0][3];
    // yyy += matrix[1][3];
    // turboRegProgressBar.stepProgressBar();
    // }
    // turboRegProgressBar.workloadDone(outNy);
    // } /* bilinearTransform */

    /*------------------------------------------------------------------*/
    // private void bilinearTransform(final double[][] matrix, final float[]
    // outMsk) {
    // double yx;
    // double yy;
    // double yxy;
    // double yyy;
    // double x0;
    // double y0;
    // int xMsk;
    // int yMsk;
    // int k = 0;
    // turboRegProgressBar.addWorkload(outNy);
    // yx = matrix[0][0];
    // yy = matrix[1][0];
    // yxy = 0.0;
    // yyy = 0.0;
    // for (int v = 0; (v < outNy); v++) {
    // x0 = yx;
    // y0 = yy;
    // for (int u = 0; (u < outNx); u++) {
    // x = x0;
    // y = y0;
    // xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
    // yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
    // if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk)
    // && (yMsk < inNy)) {
    // xMsk += yMsk * inNx;
    // if (accelerated) {
    // outImg[k] = inImg[xMsk];
    // } else {
    // xIndexes();
    // yIndexes();
    // x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
    // y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
    // xWeights();
    // yWeights();
    // outImg[k] = (float) interpolate();
    // }
    // outMsk[k++] = inMsk[xMsk];
    // } else {
    // outImg[k] = 0.0F;
    // outMsk[k++] = 0.0F;
    // }
    // x0 += matrix[0][1] + yxy;
    // y0 += matrix[1][1] + yyy;
    // }
    // yx += matrix[0][2];
    // yy += matrix[1][2];
    // yxy += matrix[0][3];
    // yyy += matrix[1][3];
    // turboRegProgressBar.stepProgressBar();
    // }
    // turboRegProgressBar.workloadDone(outNy);
    // } /* bilinearTransform */

    /*------------------------------------------------------------------*/
    // private void computeBilinearGradientConstants() {
    // final double u1 = targetPoint[0][0];
    // final double u2 = targetPoint[1][0];
    // final double u3 = targetPoint[2][0];
    // final double u4 = targetPoint[3][0];
    // final double v1 = targetPoint[0][1];
    // final double v2 = targetPoint[1][1];
    // final double v3 = targetPoint[2][1];
    // final double v4 = targetPoint[3][1];
    // final double v12 = v1 - v2;
    // final double v13 = v1 - v3;
    // final double v14 = v1 - v4;
    // final double v23 = v2 - v3;
    // final double v24 = v2 - v4;
    // final double v34 = v3 - v4;
    // final double uv12 = u1 * u2 * v12;
    // final double uv13 = u1 * u3 * v13;
    // final double uv14 = u1 * u4 * v14;
    // final double uv23 = u2 * u3 * v23;
    // final double uv24 = u2 * u4 * v24;
    // final double uv34 = u3 * u4 * v34;
    // final double det = uv12 * v34 - uv13 * v24 + uv14 * v23 + uv23 * v14
    // - uv24 * v13 + uv34 * v12;
    // c0 = (-uv34 * v2 + uv24 * v3 - uv23 * v4) / det;
    // c0u = (u3 * v3 * v24 - u2 * v2 * v34 - u4 * v4 * v23) / det;
    // c0v = (uv23 - uv24 + uv34) / det;
    // c0uv = (u4 * v23 - u3 * v24 + u2 * v34) / det;
    // c1 = (uv34 * v1 - uv14 * v3 + uv13 * v4) / det;
    // c1u = (-u3 * v3 * v14 + u1 * v1 * v34 + u4 * v4 * v13) / det;
    // c1v = (-uv13 + uv14 - uv34) / det;
    // c1uv = (-u4 * v13 + u3 * v14 - u1 * v34) / det;
    // c2 = (-uv24 * v1 + uv14 * v2 - uv12 * v4) / det;
    // c2u = (u2 * v2 * v14 - u1 * v1 * v24 - u4 * v4 * v12) / det;
    // c2v = (uv12 - uv14 + uv24) / det;
    // c2uv = (u4 * v12 - u2 * v14 + u1 * v24) / det;
    // c3 = (uv23 * v1 - uv13 * v2 + uv12 * v3) / det;
    // c3u = (-u2 * v2 * v13 + u1 * v1 * v23 + u3 * v3 * v12) / det;
    // c3v = (-uv12 + uv13 - uv23) / det;
    // c3uv = (-u3 * v1 + u2 * v13 + u3 * v2 - u1 * v23) / det;
    // } /* end computeBilinearGradientConstants */

    /*------------------------------------------------------------------*/
    private double getAffineMeanSquares(final double[][] sourcePoint, final double[][] matrix) {
        final double u1 = sourcePoint[0][0];
        final double u2 = sourcePoint[1][0];
        final double u3 = sourcePoint[2][0];
        final double v1 = sourcePoint[0][1];
        final double v2 = sourcePoint[1][1];
        final double v3 = sourcePoint[2][1];
        final double uv32 = (u3 * v2) - (u2 * v3);
        final double uv21 = (u2 * v1) - (u1 * v2);
        final double uv13 = (u1 * v3) - (u3 * v1);
        final double det = uv32 + uv21 + uv13;
        double yx;
        double yy;
        double x0;
        double y0;
        double difference;
        double meanSquares = 0.0;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        if (outMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                        if (inMsk[(yMsk * inNx) + xMsk] != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                            y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                            xWeights();
                            yWeights();
                            difference = outImg[k] - interpolate();
                            meanSquares += difference * difference;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                        if ((outMsk[k] * inMsk[(yMsk * inNx) + xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                            y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                            xWeights();
                            yWeights();
                            difference = outImg[k] - interpolate();
                            meanSquares += difference * difference;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        }
        return (meanSquares / (area * Math.abs(det / targetJacobian)));
    } /* getAffineMeanSquares */

    /*------------------------------------------------------------------*/
    private double getAffineMeanSquares(final double[][] sourcePoint, final double[][] matrix, final double[] gradient) {
        final double u1 = sourcePoint[0][0];
        final double u2 = sourcePoint[1][0];
        final double u3 = sourcePoint[2][0];
        final double v1 = sourcePoint[0][1];
        final double v2 = sourcePoint[1][1];
        final double v3 = sourcePoint[2][1];
        double uv32 = (u3 * v2) - (u2 * v3);
        double uv21 = (u2 * v1) - (u1 * v2);
        double uv13 = (u1 * v3) - (u3 * v1);
        final double det = uv32 + uv21 + uv13;
        final double u12 = (u1 - u2) / det;
        final double u23 = (u2 - u3) / det;
        final double u31 = (u3 - u1) / det;
        final double v12 = (v1 - v2) / det;
        final double v23 = (v2 - v3) / det;
        final double v31 = (v3 - v1) / det;
        double yx;
        double yy;
        double x0;
        double y0;
        double difference;
        double meanSquares = 0.0;
        double g0;
        double g1;
        double g2;
        double dx0;
        double dx1;
        double dx2;
        double dy0;
        double dy1;
        double dy2;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        uv32 /= det;
        uv21 /= det;
        uv13 /= det;
        for (int i = 0; (i < transformation.getNumParam()); i++) {
            gradient[i] = 0.0;
        }
        if (outMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                        if (inMsk[(yMsk * inNx) + xMsk] != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                            y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                            xWeights();
                            yWeights();
                            difference = outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            g0 = ((u23 * v) - (v23 * u)) + uv32;
                            g1 = ((u31 * v) - (v31 * u)) + uv13;
                            g2 = ((u12 * v) - (v12 * u)) + uv21;
                            dx0 = xGradient[k] * g0;
                            dy0 = yGradient[k] * g0;
                            dx1 = xGradient[k] * g1;
                            dy1 = yGradient[k] * g1;
                            dx2 = xGradient[k] * g2;
                            dy2 = yGradient[k] * g2;
                            gradient[0] += difference * dx0;
                            gradient[1] += difference * dy0;
                            gradient[2] += difference * dx1;
                            gradient[3] += difference * dy1;
                            gradient[4] += difference * dx2;
                            gradient[5] += difference * dy2;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                        if ((outMsk[k] * inMsk[(yMsk * inNx) + xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                            y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                            xWeights();
                            yWeights();
                            difference = outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            g0 = ((u23 * v) - (v23 * u)) + uv32;
                            g1 = ((u31 * v) - (v31 * u)) + uv13;
                            g2 = ((u12 * v) - (v12 * u)) + uv21;
                            dx0 = xGradient[k] * g0;
                            dy0 = yGradient[k] * g0;
                            dx1 = xGradient[k] * g1;
                            dy1 = yGradient[k] * g1;
                            dx2 = xGradient[k] * g2;
                            dy2 = yGradient[k] * g2;
                            gradient[0] += difference * dx0;
                            gradient[1] += difference * dy0;
                            gradient[2] += difference * dx1;
                            gradient[3] += difference * dy1;
                            gradient[4] += difference * dx2;
                            gradient[5] += difference * dy2;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        }
        return (meanSquares / (area * Math.abs(det / targetJacobian)));
    } /* getAffineMeanSquares */

    /*------------------------------------------------------------------*/
    private double getAffineMeanSquares(final double[][] sourcePoint, final double[][] matrix,
                                        final double[][] hessian, final double[] gradient) {
        final double u1 = sourcePoint[0][0];
        final double u2 = sourcePoint[1][0];
        final double u3 = sourcePoint[2][0];
        final double v1 = sourcePoint[0][1];
        final double v2 = sourcePoint[1][1];
        final double v3 = sourcePoint[2][1];
        double uv32 = (u3 * v2) - (u2 * v3);
        double uv21 = (u2 * v1) - (u1 * v2);
        double uv13 = (u1 * v3) - (u3 * v1);
        final double det = uv32 + uv21 + uv13;
        final double u12 = (u1 - u2) / det;
        final double u23 = (u2 - u3) / det;
        final double u31 = (u3 - u1) / det;
        final double v12 = (v1 - v2) / det;
        final double v23 = (v2 - v3) / det;
        final double v31 = (v3 - v1) / det;
        double yx;
        double yy;
        double x0;
        double y0;
        double difference;
        double meanSquares = 0.0;
        double g0;
        double g1;
        double g2;
        double dx0;
        double dx1;
        double dx2;
        double dy0;
        double dy1;
        double dy2;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        uv32 /= det;
        uv21 /= det;
        uv13 /= det;
        for (int i = 0; (i < transformation.getNumParam()); i++) {
            gradient[i] = 0.0;
            for (int j = 0; (j < transformation.getNumParam()); j++) {
                hessian[i][j] = 0.0;
            }
        }
        if (outMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                        if (inMsk[(yMsk * inNx) + xMsk] != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                            y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                            xWeights();
                            yWeights();
                            difference = outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            g0 = ((u23 * v) - (v23 * u)) + uv32;
                            g1 = ((u31 * v) - (v31 * u)) + uv13;
                            g2 = ((u12 * v) - (v12 * u)) + uv21;
                            dx0 = xGradient[k] * g0;
                            dy0 = yGradient[k] * g0;
                            dx1 = xGradient[k] * g1;
                            dy1 = yGradient[k] * g1;
                            dx2 = xGradient[k] * g2;
                            dy2 = yGradient[k] * g2;
                            gradient[0] += difference * dx0;
                            gradient[1] += difference * dy0;
                            gradient[2] += difference * dx1;
                            gradient[3] += difference * dy1;
                            gradient[4] += difference * dx2;
                            gradient[5] += difference * dy2;
                            hessian[0][0] += dx0 * dx0;
                            hessian[0][1] += dx0 * dy0;
                            hessian[0][2] += dx0 * dx1;
                            hessian[0][3] += dx0 * dy1;
                            hessian[0][4] += dx0 * dx2;
                            hessian[0][5] += dx0 * dy2;
                            hessian[1][1] += dy0 * dy0;
                            hessian[1][2] += dy0 * dx1;
                            hessian[1][3] += dy0 * dy1;
                            hessian[1][4] += dy0 * dx2;
                            hessian[1][5] += dy0 * dy2;
                            hessian[2][2] += dx1 * dx1;
                            hessian[2][3] += dx1 * dy1;
                            hessian[2][4] += dx1 * dx2;
                            hessian[2][5] += dx1 * dy2;
                            hessian[3][3] += dy1 * dy1;
                            hessian[3][4] += dy1 * dx2;
                            hessian[3][5] += dy1 * dy2;
                            hessian[4][4] += dx2 * dx2;
                            hessian[4][5] += dx2 * dy2;
                            hessian[5][5] += dy2 * dy2;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                        if ((outMsk[k] * inMsk[(yMsk * inNx) + xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                            y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                            xWeights();
                            yWeights();
                            difference = outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            g0 = ((u23 * v) - (v23 * u)) + uv32;
                            g1 = ((u31 * v) - (v31 * u)) + uv13;
                            g2 = ((u12 * v) - (v12 * u)) + uv21;
                            dx0 = xGradient[k] * g0;
                            dy0 = yGradient[k] * g0;
                            dx1 = xGradient[k] * g1;
                            dy1 = yGradient[k] * g1;
                            dx2 = xGradient[k] * g2;
                            dy2 = yGradient[k] * g2;
                            gradient[0] += difference * dx0;
                            gradient[1] += difference * dy0;
                            gradient[2] += difference * dx1;
                            gradient[3] += difference * dy1;
                            gradient[4] += difference * dx2;
                            gradient[5] += difference * dy2;
                            hessian[0][0] += dx0 * dx0;
                            hessian[0][1] += dx0 * dy0;
                            hessian[0][2] += dx0 * dx1;
                            hessian[0][3] += dx0 * dy1;
                            hessian[0][4] += dx0 * dx2;
                            hessian[0][5] += dx0 * dy2;
                            hessian[1][1] += dy0 * dy0;
                            hessian[1][2] += dy0 * dx1;
                            hessian[1][3] += dy0 * dy1;
                            hessian[1][4] += dy0 * dx2;
                            hessian[1][5] += dy0 * dy2;
                            hessian[2][2] += dx1 * dx1;
                            hessian[2][3] += dx1 * dy1;
                            hessian[2][4] += dx1 * dx2;
                            hessian[2][5] += dx1 * dy2;
                            hessian[3][3] += dy1 * dy1;
                            hessian[3][4] += dy1 * dx2;
                            hessian[3][5] += dy1 * dy2;
                            hessian[4][4] += dx2 * dx2;
                            hessian[4][5] += dx2 * dy2;
                            hessian[5][5] += dy2 * dy2;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        }
        for (int i = 1; (i < transformation.getNumParam()); i++) {
            for (int j = 0; (j < i); j++) {
                hessian[i][j] = hessian[j][i];
            }
        }
        return (meanSquares / (area * Math.abs(det / targetJacobian)));
    } /* getAffineMeanSquares */

    /*------------------------------------------------------------------*/
    // private double getBilinearMeanSquares(final double[][] matrix) {
    // double yx;
    // double yy;
    // double yxy;
    // double yyy;
    // double x0;
    // double y0;
    // double difference;
    // double meanSquares = 0.0;
    // long area = 0L;
    // int xMsk;
    // int yMsk;
    // int k = 0;
    // if (inMsk == null) {
    // yx = matrix[0][0];
    // yy = matrix[1][0];
    // yxy = 0.0;
    // yyy = 0.0;
    // for (int v = 0; (v < outNy); v++) {
    // x0 = yx;
    // y0 = yy;
    // for (int u = 0; (u < outNx); u++, k++) {
    // if (outMsk[k] != 0.0F) {
    // x = x0;
    // y = y0;
    // xMsk = (0.0 <= x) ? ((int) (x + 0.5))
    // : ((int) (x - 0.5));
    // yMsk = (0.0 <= y) ? ((int) (y + 0.5))
    // : ((int) (y - 0.5));
    // if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk)
    // && (yMsk < inNy)) {
    // xIndexes();
    // yIndexes();
    // area++;
    // x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
    // y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
    // xWeights();
    // yWeights();
    // difference = interpolate() - (double) outImg[k];
    // meanSquares += difference * difference;
    // }
    // }
    // x0 += matrix[0][1] + yxy;
    // y0 += matrix[1][1] + yyy;
    // }
    // yx += matrix[0][2];
    // yy += matrix[1][2];
    // yxy += matrix[0][3];
    // yyy += matrix[1][3];
    // }
    // } else {
    // yx = matrix[0][0];
    // yy = matrix[1][0];
    // yxy = 0.0;
    // yyy = 0.0;
    // for (int v = 0; (v < outNy); v++) {
    // x0 = yx;
    // y0 = yy;
    // for (int u = 0; (u < outNx); u++, k++) {
    // x = x0;
    // y = y0;
    // xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
    // yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
    // if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk)
    // && (yMsk < inNy)) {
    // xMsk += yMsk * inNx;
    // if ((outMsk[k] * inMsk[xMsk]) != 0.0F) {
    // xIndexes();
    // yIndexes();
    // area++;
    // x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
    // y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
    // xWeights();
    // yWeights();
    // difference = interpolate() - (double) outImg[k];
    // meanSquares += difference * difference;
    // }
    // }
    // x0 += matrix[0][1] + yxy;
    // y0 += matrix[1][1] + yyy;
    // }
    // yx += matrix[0][2];
    // yy += matrix[1][2];
    // yxy += matrix[0][3];
    // yyy += matrix[1][3];
    // }
    // }
    // return (meanSquares / (double) area);
    // } /* getBilinearMeanSquares */

    /*------------------------------------------------------------------*/
    // private double getBilinearMeanSquares(final double[][] matrix,
    // final double[][] hessian, final double[] gradient) {
    // double yx;
    // double yy;
    // double yxy;
    // double yyy;
    // double x0;
    // double y0;
    // double uv;
    // double xGradient;
    // double yGradient;
    // double difference;
    // double meanSquares = 0.0;
    // double g0;
    // double g1;
    // double g2;
    // double g3;
    // double dx0;
    // double dx1;
    // double dx2;
    // double dx3;
    // double dy0;
    // double dy1;
    // double dy2;
    // double dy3;
    // long area = 0L;
    // int xMsk;
    // int yMsk;
    // int k = 0;
    // computeBilinearGradientConstants();
    // for (int i = 0; (i < transformation.getNumParam()); i++) {
    // gradient[i] = 0.0;
    // for (int j = 0; (j < transformation.getNumParam()); j++) {
    // hessian[i][j] = 0.0;
    // }
    // }
    // if (inMsk == null) {
    // yx = matrix[0][0];
    // yy = matrix[1][0];
    // yxy = 0.0;
    // yyy = 0.0;
    // for (int v = 0; (v < outNy); v++) {
    // x0 = yx;
    // y0 = yy;
    // for (int u = 0; (u < outNx); u++, k++) {
    // if (outMsk[k] != 0.0F) {
    // x = x0;
    // y = y0;
    // xMsk = (0.0 <= x) ? ((int) (x + 0.5))
    // : ((int) (x - 0.5));
    // yMsk = (0.0 <= y) ? ((int) (y + 0.5))
    // : ((int) (y - 0.5));
    // if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk)
    // && (yMsk < inNy)) {
    // area++;
    // xIndexes();
    // yIndexes();
    // x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
    // y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
    // xDxWeights();
    // yDyWeights();
    // difference = interpolate() - (double) outImg[k];
    // meanSquares += difference * difference;
    // xGradient = interpolateDx();
    // yGradient = interpolateDy();
    // uv = (double) u * (double) v;
    // g0 = c0uv * uv + c0u * (double) u + c0v
    // * (double) v + c0;
    // g1 = c1uv * uv + c1u * (double) u + c1v
    // * (double) v + c1;
    // g2 = c2uv * uv + c2u * (double) u + c2v
    // * (double) v + c2;
    // g3 = c3uv * uv + c3u * (double) u + c3v
    // * (double) v + c3;
    // dx0 = xGradient * g0;
    // dy0 = yGradient * g0;
    // dx1 = xGradient * g1;
    // dy1 = yGradient * g1;
    // dx2 = xGradient * g2;
    // dy2 = yGradient * g2;
    // dx3 = xGradient * g3;
    // dy3 = yGradient * g3;
    // gradient[0] += difference * dx0;
    // gradient[1] += difference * dy0;
    // gradient[2] += difference * dx1;
    // gradient[3] += difference * dy1;
    // gradient[4] += difference * dx2;
    // gradient[5] += difference * dy2;
    // gradient[6] += difference * dx3;
    // gradient[7] += difference * dy3;
    // hessian[0][0] += dx0 * dx0;
    // hessian[0][1] += dx0 * dy0;
    // hessian[0][2] += dx0 * dx1;
    // hessian[0][3] += dx0 * dy1;
    // hessian[0][4] += dx0 * dx2;
    // hessian[0][5] += dx0 * dy2;
    // hessian[0][6] += dx0 * dx3;
    // hessian[0][7] += dx0 * dy3;
    // hessian[1][1] += dy0 * dy0;
    // hessian[1][2] += dy0 * dx1;
    // hessian[1][3] += dy0 * dy1;
    // hessian[1][4] += dy0 * dx2;
    // hessian[1][5] += dy0 * dy2;
    // hessian[1][6] += dy0 * dx3;
    // hessian[1][7] += dy0 * dy3;
    // hessian[2][2] += dx1 * dx1;
    // hessian[2][3] += dx1 * dy1;
    // hessian[2][4] += dx1 * dx2;
    // hessian[2][5] += dx1 * dy2;
    // hessian[2][6] += dx1 * dx3;
    // hessian[2][7] += dx1 * dy3;
    // hessian[3][3] += dy1 * dy1;
    // hessian[3][4] += dy1 * dx2;
    // hessian[3][5] += dy1 * dy2;
    // hessian[3][6] += dy1 * dx3;
    // hessian[3][7] += dy1 * dy3;
    // hessian[4][4] += dx2 * dx2;
    // hessian[4][5] += dx2 * dy2;
    // hessian[4][6] += dx2 * dx3;
    // hessian[4][7] += dx2 * dy3;
    // hessian[5][5] += dy2 * dy2;
    // hessian[5][6] += dy2 * dx3;
    // hessian[5][7] += dy2 * dy3;
    // hessian[6][6] += dx3 * dx3;
    // hessian[6][7] += dx3 * dy3;
    // hessian[7][7] += dy3 * dy3;
    // }
    // }
    // x0 += matrix[0][1] + yxy;
    // y0 += matrix[1][1] + yyy;
    // }
    // yx += matrix[0][2];
    // yy += matrix[1][2];
    // yxy += matrix[0][3];
    // yyy += matrix[1][3];
    // }
    // } else {
    // yx = matrix[0][0];
    // yy = matrix[1][0];
    // yxy = 0.0;
    // yyy = 0.0;
    // for (int v = 0; (v < outNy); v++) {
    // x0 = yx;
    // y0 = yy;
    // for (int u = 0; (u < outNx); u++, k++) {
    // x = x0;
    // y = y0;
    // xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
    // yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
    // if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk)
    // && (yMsk < inNy)) {
    // xMsk += yMsk * inNx;
    // if ((outMsk[k] * inMsk[xMsk]) != 0.0F) {
    // area++;
    // xIndexes();
    // yIndexes();
    // x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
    // y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
    // xDxWeights();
    // yDyWeights();
    // difference = interpolate() - (double) outImg[k];
    // meanSquares += difference * difference;
    // xGradient = interpolateDx();
    // yGradient = interpolateDy();
    // uv = (double) u * (double) v;
    // g0 = c0uv * uv + c0u * (double) u + c0v
    // * (double) v + c0;
    // g1 = c1uv * uv + c1u * (double) u + c1v
    // * (double) v + c1;
    // g2 = c2uv * uv + c2u * (double) u + c2v
    // * (double) v + c2;
    // g3 = c3uv * uv + c3u * (double) u + c3v
    // * (double) v + c3;
    // dx0 = xGradient * g0;
    // dy0 = yGradient * g0;
    // dx1 = xGradient * g1;
    // dy1 = yGradient * g1;
    // dx2 = xGradient * g2;
    // dy2 = yGradient * g2;
    // dx3 = xGradient * g3;
    // dy3 = yGradient * g3;
    // gradient[0] += difference * dx0;
    // gradient[1] += difference * dy0;
    // gradient[2] += difference * dx1;
    // gradient[3] += difference * dy1;
    // gradient[4] += difference * dx2;
    // gradient[5] += difference * dy2;
    // gradient[6] += difference * dx3;
    // gradient[7] += difference * dy3;
    // hessian[0][0] += dx0 * dx0;
    // hessian[0][1] += dx0 * dy0;
    // hessian[0][2] += dx0 * dx1;
    // hessian[0][3] += dx0 * dy1;
    // hessian[0][4] += dx0 * dx2;
    // hessian[0][5] += dx0 * dy2;
    // hessian[0][6] += dx0 * dx3;
    // hessian[0][7] += dx0 * dy3;
    // hessian[1][1] += dy0 * dy0;
    // hessian[1][2] += dy0 * dx1;
    // hessian[1][3] += dy0 * dy1;
    // hessian[1][4] += dy0 * dx2;
    // hessian[1][5] += dy0 * dy2;
    // hessian[1][6] += dy0 * dx3;
    // hessian[1][7] += dy0 * dy3;
    // hessian[2][2] += dx1 * dx1;
    // hessian[2][3] += dx1 * dy1;
    // hessian[2][4] += dx1 * dx2;
    // hessian[2][5] += dx1 * dy2;
    // hessian[2][6] += dx1 * dx3;
    // hessian[2][7] += dx1 * dy3;
    // hessian[3][3] += dy1 * dy1;
    // hessian[3][4] += dy1 * dx2;
    // hessian[3][5] += dy1 * dy2;
    // hessian[3][6] += dy1 * dx3;
    // hessian[3][7] += dy1 * dy3;
    // hessian[4][4] += dx2 * dx2;
    // hessian[4][5] += dx2 * dy2;
    // hessian[4][6] += dx2 * dx3;
    // hessian[4][7] += dx2 * dy3;
    // hessian[5][5] += dy2 * dy2;
    // hessian[5][6] += dy2 * dx3;
    // hessian[5][7] += dy2 * dy3;
    // hessian[6][6] += dx3 * dx3;
    // hessian[6][7] += dx3 * dy3;
    // hessian[7][7] += dy3 * dy3;
    // }
    // }
    // x0 += matrix[0][1] + yxy;
    // y0 += matrix[1][1] + yyy;
    // }
    // yx += matrix[0][2];
    // yy += matrix[1][2];
    // yxy += matrix[0][3];
    // yyy += matrix[1][3];
    // }
    // }
    // for (int i = 1; (i < transformation.getNumParam()); i++) {
    // for (int j = 0; (j < i); j++) {
    // hessian[i][j] = hessian[j][i];
    // }
    // }
    // return (meanSquares / (double) area);
    // } /* getBilinearMeanSquares */

    /*------------------------------------------------------------------*/
    private double getRigidBodyMeanSquares(final double[][] matrix) {
        double yx;
        double yy;
        double x0;
        double y0;
        double difference;
        double meanSquares = 0.0;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        if (outMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                        if (inMsk[(yMsk * inNx) + xMsk] != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                            y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                            xWeights();
                            yWeights();
                            difference = outImg[k] - interpolate();
                            meanSquares += difference * difference;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                        if ((outMsk[k] * inMsk[(yMsk * inNx) + xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                            y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                            xWeights();
                            yWeights();
                            difference = outImg[k] - interpolate();
                            meanSquares += difference * difference;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        }
        return (meanSquares / area);
    } /* getRigidBodyMeanSquares */

    /*------------------------------------------------------------------*/
    private double getRigidBodyMeanSquares(final double[][] matrix, final double[] gradient

    ) {
        double yx;
        double yy;
        double x0;
        double y0;
        double difference;
        double meanSquares = 0.0;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        for (int i = 0; (i < transformation.getNumParam()); i++) {
            gradient[i] = 0.0;
        }
        if (outMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                        if (inMsk[(yMsk * inNx) + xMsk] != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                            y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                            xWeights();
                            yWeights();
                            difference = outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            gradient[0] += difference * ((yGradient[k] * (double)u) - (xGradient[k] * (double)v));
                            gradient[1] += difference * xGradient[k];
                            gradient[2] += difference * yGradient[k];
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                        if ((outMsk[k] * inMsk[(yMsk * inNx) + xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                            y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                            xWeights();
                            yWeights();
                            difference = outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            gradient[0] += difference * ((yGradient[k] * (double)u) - (xGradient[k] * (double)v));
                            gradient[1] += difference * xGradient[k];
                            gradient[2] += difference * yGradient[k];
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        }
        return (meanSquares / area);
    } /* getRigidBodyMeanSquares */

    /*------------------------------------------------------------------*/
    private double getRigidBodyMeanSquares(final double[][] matrix, final double[][] hessian, final double[] gradient) {
        double yx;
        double yy;
        double x0;
        double y0;
        double dTheta;
        double difference;
        double meanSquares = 0.0;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        for (int i = 0; (i < transformation.getNumParam()); i++) {
            gradient[i] = 0.0;
            for (int j = 0; (j < transformation.getNumParam()); j++) {
                hessian[i][j] = 0.0;
            }
        }
        if (outMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                        if (inMsk[(yMsk * inNx) + xMsk] != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                            y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                            xWeights();
                            yWeights();
                            difference = outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            dTheta = (yGradient[k] * (double)u) - (xGradient[k] * (double)v);
                            gradient[0] += difference * dTheta;
                            gradient[1] += difference * xGradient[k];
                            gradient[2] += difference * yGradient[k];
                            hessian[0][0] += dTheta * dTheta;
                            hessian[0][1] += dTheta * xGradient[k];
                            hessian[0][2] += dTheta * yGradient[k];
                            hessian[1][1] += xGradient[k] * xGradient[k];
                            hessian[1][2] += xGradient[k] * yGradient[k];
                            hessian[2][2] += yGradient[k] * yGradient[k];
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                        if ((outMsk[k] * inMsk[(yMsk * inNx) + xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                            y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                            xWeights();
                            yWeights();
                            difference = outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            dTheta = (yGradient[k] * (double)u) - (xGradient[k] * (double)v);
                            gradient[0] += difference * dTheta;
                            gradient[1] += difference * xGradient[k];
                            gradient[2] += difference * yGradient[k];
                            hessian[0][0] += dTheta * dTheta;
                            hessian[0][1] += dTheta * xGradient[k];
                            hessian[0][2] += dTheta * yGradient[k];
                            hessian[1][1] += xGradient[k] * xGradient[k];
                            hessian[1][2] += xGradient[k] * yGradient[k];
                            hessian[2][2] += yGradient[k] * yGradient[k];
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        }
        for (int i = 1; (i < transformation.getNumParam()); i++) {
            for (int j = 0; (j < i); j++) {
                hessian[i][j] = hessian[j][i];
            }
        }
        return (meanSquares / area);
    } /* getRigidBodyMeanSquares */

    /*------------------------------------------------------------------*/
    private double getScaledRotationMeanSquares(final double[][] sourcePoint, final double[][] matrix) {
        final double u1 = sourcePoint[0][0];
        final double u2 = sourcePoint[1][0];
        final double v1 = sourcePoint[0][1];
        final double v2 = sourcePoint[1][1];
        final double u12 = u1 - u2;
        final double v12 = v1 - v2;
        final double uv2 = (u12 * u12) + (v12 * v12);
        double yx;
        double yy;
        double x0;
        double y0;
        double difference;
        double meanSquares = 0.0;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        if (outMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                        if (inMsk[(yMsk * inNx) + xMsk] != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                            y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                            xWeights();
                            yWeights();
                            difference = outImg[k] - interpolate();
                            meanSquares += difference * difference;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                        if ((outMsk[k] * inMsk[(yMsk * inNx) + xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                            y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                            xWeights();
                            yWeights();
                            difference = outImg[k] - interpolate();
                            meanSquares += difference * difference;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        }
        return (meanSquares / ((area * uv2) / targetJacobian));
    } /* getScaledRotationMeanSquares */

    /*------------------------------------------------------------------*/
    private double getScaledRotationMeanSquares(final double[][] sourcePoint, final double[][] matrix,
                                                final double[] gradient) {
        final double u1 = sourcePoint[0][0];
        final double u2 = sourcePoint[1][0];
        final double v1 = sourcePoint[0][1];
        final double v2 = sourcePoint[1][1];
        final double u12 = u1 - u2;
        final double v12 = v1 - v2;
        final double uv2 = (u12 * u12) + (v12 * v12);
        final double c = (0.5 * ((u2 * v1) - (u1 * v2))) / uv2;
        final double c1 = u12 / uv2;
        final double c2 = v12 / uv2;
        final double c3 = (uv2 - (u12 * v12)) / uv2;
        final double c4 = (uv2 + (u12 * v12)) / uv2;
        final double c5 = c + (u1 * c1) + (u2 * c2);
        final double c6 = (c * ((u12 * u12) - (v12 * v12))) / uv2;
        final double c7 = c1 * c4;
        final double c8 = c1 - c2 - (c1 * c2 * v12);
        final double c9 = (c1 + c2) - (c1 * c2 * u12);
        final double c0 = c2 * c3;
        final double dgxx0 = (c1 * u2) + (c2 * v2);
        final double dgyx0 = 2.0 * c;
        final double dgxx1 = c5 + c6;
        final double dgyy1 = c5 - c6;
        double yx;
        double yy;
        double x0;
        double y0;
        double difference;
        double meanSquares = 0.0;
        double gxx0;
        double gxx1;
        double gxy0;
        double gxy1;
        double gyx0;
        double gyx1;
        double gyy0;
        double gyy1;
        double dx0;
        double dx1;
        double dy0;
        double dy1;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        for (int i = 0; (i < transformation.getNumParam()); i++) {
            gradient[i] = 0.0;
        }
        if (outMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                        if (inMsk[(yMsk * inNx) + xMsk] != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                            y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                            xWeights();
                            yWeights();
                            difference = outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            gxx0 = ((u * c1) + (v * c2)) - dgxx0;
                            gyx0 = ((v * c1) - (u * c2)) + dgyx0;
                            gxy0 = -gyx0;
                            gyy0 = gxx0;
                            gxx1 = ((v * c8) - (u * c7)) + dgxx1;
                            gyx1 = -c3 * gyx0;
                            gxy1 = c4 * gyx0;
                            gyy1 = dgyy1 - (u * c9) - (v * c0);
                            dx0 = (xGradient[k] * gxx0) + (yGradient[k] * gyx0);
                            dy0 = (xGradient[k] * gxy0) + (yGradient[k] * gyy0);
                            dx1 = (xGradient[k] * gxx1) + (yGradient[k] * gyx1);
                            dy1 = (xGradient[k] * gxy1) + (yGradient[k] * gyy1);
                            gradient[0] += difference * dx0;
                            gradient[1] += difference * dy0;
                            gradient[2] += difference * dx1;
                            gradient[3] += difference * dy1;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                        if ((outMsk[k] * inMsk[(yMsk * inNx) + xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                            y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                            xWeights();
                            yWeights();
                            difference = outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            gxx0 = ((u * c1) + (v * c2)) - dgxx0;
                            gyx0 = ((v * c1) - (u * c2)) + dgyx0;
                            gxy0 = -gyx0;
                            gyy0 = gxx0;
                            gxx1 = ((v * c8) - (u * c7)) + dgxx1;
                            gyx1 = -c3 * gyx0;
                            gxy1 = c4 * gyx0;
                            gyy1 = dgyy1 - (u * c9) - (v * c0);
                            dx0 = (xGradient[k] * gxx0) + (yGradient[k] * gyx0);
                            dy0 = (xGradient[k] * gxy0) + (yGradient[k] * gyy0);
                            dx1 = (xGradient[k] * gxx1) + (yGradient[k] * gyx1);
                            dy1 = (xGradient[k] * gxy1) + (yGradient[k] * gyy1);
                            gradient[0] += difference * dx0;
                            gradient[1] += difference * dy0;
                            gradient[2] += difference * dx1;
                            gradient[3] += difference * dy1;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        }
        return (meanSquares / ((area * uv2) / targetJacobian));
    } /* getScaledRotationMeanSquares */

    /*------------------------------------------------------------------*/
    private double getScaledRotationMeanSquares(final double[][] sourcePoint, final double[][] matrix,
                                                final double[][] hessian, final double[] gradient) {
        final double u1 = sourcePoint[0][0];
        final double u2 = sourcePoint[1][0];
        final double v1 = sourcePoint[0][1];
        final double v2 = sourcePoint[1][1];
        final double u12 = u1 - u2;
        final double v12 = v1 - v2;
        final double uv2 = (u12 * u12) + (v12 * v12);
        final double c = (0.5 * ((u2 * v1) - (u1 * v2))) / uv2;
        final double c1 = u12 / uv2;
        final double c2 = v12 / uv2;
        final double c3 = (uv2 - (u12 * v12)) / uv2;
        final double c4 = (uv2 + (u12 * v12)) / uv2;
        final double c5 = c + (u1 * c1) + (u2 * c2);
        final double c6 = (c * ((u12 * u12) - (v12 * v12))) / uv2;
        final double c7 = c1 * c4;
        final double c8 = c1 - c2 - (c1 * c2 * v12);
        final double c9 = (c1 + c2) - (c1 * c2 * u12);
        final double c0 = c2 * c3;
        final double dgxx0 = (c1 * u2) + (c2 * v2);
        final double dgyx0 = 2.0 * c;
        final double dgxx1 = c5 + c6;
        final double dgyy1 = c5 - c6;
        double yx;
        double yy;
        double x0;
        double y0;
        double difference;
        double meanSquares = 0.0;
        double gxx0;
        double gxx1;
        double gxy0;
        double gxy1;
        double gyx0;
        double gyx1;
        double gyy0;
        double gyy1;
        double dx0;
        double dx1;
        double dy0;
        double dy1;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        for (int i = 0; (i < transformation.getNumParam()); i++) {
            gradient[i] = 0.0;
            for (int j = 0; (j < transformation.getNumParam()); j++) {
                hessian[i][j] = 0.0;
            }
        }
        if (outMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                        if (inMsk[(yMsk * inNx) + xMsk] != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                            y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                            xWeights();
                            yWeights();
                            difference = outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            gxx0 = ((u * c1) + (v * c2)) - dgxx0;
                            gyx0 = ((v * c1) - (u * c2)) + dgyx0;
                            gxy0 = -gyx0;
                            gyy0 = gxx0;
                            gxx1 = ((v * c8) - (u * c7)) + dgxx1;
                            gyx1 = -c3 * gyx0;
                            gxy1 = c4 * gyx0;
                            gyy1 = dgyy1 - (u * c9) - (v * c0);
                            dx0 = (xGradient[k] * gxx0) + (yGradient[k] * gyx0);
                            dy0 = (xGradient[k] * gxy0) + (yGradient[k] * gyy0);
                            dx1 = (xGradient[k] * gxx1) + (yGradient[k] * gyx1);
                            dy1 = (xGradient[k] * gxy1) + (yGradient[k] * gyy1);
                            gradient[0] += difference * dx0;
                            gradient[1] += difference * dy0;
                            gradient[2] += difference * dx1;
                            gradient[3] += difference * dy1;
                            hessian[0][0] += dx0 * dx0;
                            hessian[0][1] += dx0 * dy0;
                            hessian[0][2] += dx0 * dx1;
                            hessian[0][3] += dx0 * dy1;
                            hessian[1][1] += dy0 * dy0;
                            hessian[1][2] += dy0 * dx1;
                            hessian[1][3] += dy0 * dy1;
                            hessian[2][2] += dx1 * dx1;
                            hessian[2][3] += dx1 * dy1;
                            hessian[3][3] += dy1 * dy1;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                        if ((outMsk[k] * inMsk[(yMsk * inNx) + xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int)x) : ((int)x - 1);
                            y -= (0.0 <= y) ? ((int)y) : ((int)y - 1);
                            xWeights();
                            yWeights();
                            difference = outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            gxx0 = ((u * c1) + (v * c2)) - dgxx0;
                            gyx0 = ((v * c1) - (u * c2)) + dgyx0;
                            gxy0 = -gyx0;
                            gyy0 = gxx0;
                            gxx1 = ((v * c8) - (u * c7)) + dgxx1;
                            gyx1 = -c3 * gyx0;
                            gxy1 = c4 * gyx0;
                            gyy1 = dgyy1 - (u * c9) - (v * c0);
                            dx0 = (xGradient[k] * gxx0) + (yGradient[k] * gyx0);
                            dy0 = (xGradient[k] * gxy0) + (yGradient[k] * gyy0);
                            dx1 = (xGradient[k] * gxx1) + (yGradient[k] * gyx1);
                            dy1 = (xGradient[k] * gxy1) + (yGradient[k] * gyy1);
                            gradient[0] += difference * dx0;
                            gradient[1] += difference * dy0;
                            gradient[2] += difference * dx1;
                            gradient[3] += difference * dy1;
                            hessian[0][0] += dx0 * dx0;
                            hessian[0][1] += dx0 * dy0;
                            hessian[0][2] += dx0 * dx1;
                            hessian[0][3] += dx0 * dy1;
                            hessian[1][1] += dy0 * dy0;
                            hessian[1][2] += dy0 * dx1;
                            hessian[1][3] += dy0 * dy1;
                            hessian[2][2] += dx1 * dx1;
                            hessian[2][3] += dx1 * dy1;
                            hessian[3][3] += dy1 * dy1;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        }
        for (int i = 1; (i < transformation.getNumParam()); i++) {
            for (int j = 0; (j < i); j++) {
                hessian[i][j] = hessian[j][i];
            }
        }
        return (meanSquares / ((area * uv2) / targetJacobian));
    } /* getScaledRotationMeanSquares */

    /*------------------------------------------------------------------*/
    private double[][] getTransformationMatrix(final double[][] fromCoord, final double[][] toCoord) {
        double[][] matrix = null;
        double[][] a = null;
        double[] v = null;
        switch (transformation) {
            case TRANSLATION: {
                matrix = new double[2][1];
                matrix[0][0] = toCoord[0][0] - fromCoord[0][0];
                matrix[1][0] = toCoord[0][1] - fromCoord[0][1];
                break;
            }
            case RIGID_BODY: {
                final double angle =
                        Math.atan2(fromCoord[2][0] - fromCoord[1][0], fromCoord[2][1] - fromCoord[1][1])
                                - Math.atan2(toCoord[2][0] - toCoord[1][0], toCoord[2][1] - toCoord[1][1]);
                final double c = Math.cos(angle);
                final double s = Math.sin(angle);
                matrix = new double[2][3];
                matrix[0][0] = (toCoord[0][0] - (c * fromCoord[0][0])) + (s * fromCoord[0][1]);
                matrix[0][1] = c;
                matrix[0][2] = -s;
                matrix[1][0] = toCoord[0][1] - (s * fromCoord[0][0]) - (c * fromCoord[0][1]);
                matrix[1][1] = s;
                matrix[1][2] = c;
                break;
            }
            case SCALED_ROTATION: {
                matrix = new double[2][3];
                a = new double[3][3];
                v = new double[3];
                a[0][0] = 1.0;
                a[0][1] = fromCoord[0][0];
                a[0][2] = fromCoord[0][1];
                a[1][0] = 1.0;
                a[1][1] = fromCoord[1][0];
                a[1][2] = fromCoord[1][1];
                a[2][0] = 1.0;
                a[2][1] = (fromCoord[0][1] - fromCoord[1][1]) + fromCoord[1][0];
                a[2][2] = (fromCoord[1][0] + fromCoord[1][1]) - fromCoord[0][0];
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
                matrix = new double[2][3];
                a = new double[3][3];
                v = new double[3];
                a[0][0] = 1.0;
                a[0][1] = fromCoord[0][0];
                a[0][2] = fromCoord[0][1];
                a[1][0] = 1.0;
                a[1][1] = fromCoord[1][0];
                a[1][2] = fromCoord[1][1];
                a[2][0] = 1.0;
                a[2][1] = fromCoord[2][0];
                a[2][2] = fromCoord[2][1];
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
        }
        return (matrix);
    } /* end getTransformationMatrix */

    /*------------------------------------------------------------------*/
    private double getTranslationMeanSquares(final double[][] matrix) {
        double dx = matrix[0][0];
        double dy = matrix[1][0];
        final double dx0 = dx;
        double difference;
        double meanSquares = 0.0;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        x = dx - Math.floor(dx);
        y = dy - Math.floor(dy);
        xWeights();
        yWeights();
        if (outMsk == null) {
            for (int v = 0; (v < outNy); v++) {
                y = dy++;
                yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                if ((0 <= yMsk) && (yMsk < inNy)) {
                    yMsk *= inNx;
                    yIndexes();
                    dx = dx0;
                    for (int u = 0; (u < outNx); u++, k++) {
                        x = dx++;
                        xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                        if ((0 <= xMsk) && (xMsk < inNx)) {
                            if (inMsk[yMsk + xMsk] != 0.0F) {
                                xIndexes();
                                area++;
                                difference = outImg[k] - interpolate();
                                meanSquares += difference * difference;
                            }
                        }
                    }
                } else {
                    k += outNx;
                }
            }
        } else {
            for (int v = 0; (v < outNy); v++) {
                y = dy++;
                yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                if ((0 <= yMsk) && (yMsk < inNy)) {
                    yMsk *= inNx;
                    yIndexes();
                    dx = dx0;
                    for (int u = 0; (u < outNx); u++, k++) {
                        x = dx++;
                        xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                        if ((0 <= xMsk) && (xMsk < inNx)) {
                            if ((outMsk[k] * inMsk[yMsk + xMsk]) != 0.0F) {
                                xIndexes();
                                area++;
                                difference = outImg[k] - interpolate();
                                meanSquares += difference * difference;
                            }
                        }
                    }
                } else {
                    k += outNx;
                }
            }
        }
        return (meanSquares / area);
    } /* end getTranslationMeanSquares */

    /*------------------------------------------------------------------*/
    private double getTranslationMeanSquares(final double[][] matrix, final double[] gradient) {
        double dx = matrix[0][0];
        double dy = matrix[1][0];
        final double dx0 = dx;
        double difference;
        double meanSquares = 0.0;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        for (int i = 0; (i < transformation.getNumParam()); i++) {
            gradient[i] = 0.0;
        }
        x = dx - Math.floor(dx);
        y = dy - Math.floor(dy);
        xWeights();
        yWeights();
        if (outMsk == null) {
            for (int v = 0; (v < outNy); v++) {
                y = dy++;
                yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                if ((0 <= yMsk) && (yMsk < inNy)) {
                    yMsk *= inNx;
                    yIndexes();
                    dx = dx0;
                    for (int u = 0; (u < outNx); u++, k++) {
                        x = dx++;
                        xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                        if ((0 <= xMsk) && (xMsk < inNx)) {
                            if (inMsk[yMsk + xMsk] != 0.0F) {
                                area++;
                                xIndexes();
                                difference = outImg[k] - interpolate();
                                meanSquares += difference * difference;
                                gradient[0] += difference * xGradient[k];
                                gradient[1] += difference * yGradient[k];
                            }
                        }
                    }
                } else {
                    k += outNx;
                }
            }
        } else {
            for (int v = 0; (v < outNy); v++) {
                y = dy++;
                yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                if ((0 <= yMsk) && (yMsk < inNy)) {
                    yMsk *= inNx;
                    yIndexes();
                    dx = dx0;
                    for (int u = 0; (u < outNx); u++, k++) {
                        x = dx++;
                        xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                        if ((0 <= xMsk) && (xMsk < inNx)) {
                            if ((outMsk[k] * inMsk[yMsk + xMsk]) != 0.0F) {
                                area++;
                                xIndexes();
                                difference = outImg[k] - interpolate();
                                meanSquares += difference * difference;
                                gradient[0] += difference * xGradient[k];
                                gradient[1] += difference * yGradient[k];
                            }
                        }
                    }
                } else {
                    k += outNx;
                }
            }
        }
        return (meanSquares / area);
    } /* end getTranslationMeanSquares */

    /*------------------------------------------------------------------*/
    private double
            getTranslationMeanSquares(final double[][] matrix, final double[][] hessian, final double[] gradient) {
        double dx = matrix[0][0];
        double dy = matrix[1][0];
        final double dx0 = dx;
        double difference;
        double meanSquares = 0.0;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        for (int i = 0; (i < transformation.getNumParam()); i++) {
            gradient[i] = 0.0;
            for (int j = 0; (j < transformation.getNumParam()); j++) {
                hessian[i][j] = 0.0;
            }
        }
        x = dx - Math.floor(dx);
        y = dy - Math.floor(dy);
        xWeights();
        yWeights();
        if (outMsk == null) {
            for (int v = 0; (v < outNy); v++) {
                y = dy++;
                yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                if ((0 <= yMsk) && (yMsk < inNy)) {
                    yMsk *= inNx;
                    yIndexes();
                    dx = dx0;
                    for (int u = 0; (u < outNx); u++, k++) {
                        x = dx++;
                        xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                        if ((0 <= xMsk) && (xMsk < inNx)) {
                            if (inMsk[yMsk + xMsk] != 0.0F) {
                                area++;
                                xIndexes();
                                difference = outImg[k] - interpolate();
                                meanSquares += difference * difference;
                                gradient[0] += difference * xGradient[k];
                                gradient[1] += difference * yGradient[k];
                                hessian[0][0] += xGradient[k] * xGradient[k];
                                hessian[0][1] += xGradient[k] * yGradient[k];
                                hessian[1][1] += yGradient[k] * yGradient[k];
                            }
                        }
                    }
                } else {
                    k += outNx;
                }
            }
        } else {
            for (int v = 0; (v < outNy); v++) {
                y = dy++;
                yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
                if ((0 <= yMsk) && (yMsk < inNy)) {
                    yMsk *= inNx;
                    yIndexes();
                    dx = dx0;
                    for (int u = 0; (u < outNx); u++, k++) {
                        x = dx++;
                        xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                        if ((0 <= xMsk) && (xMsk < inNx)) {
                            if ((outMsk[k] * inMsk[yMsk + xMsk]) != 0.0F) {
                                area++;
                                xIndexes();
                                difference = outImg[k] - interpolate();
                                meanSquares += difference * difference;
                                gradient[0] += difference * xGradient[k];
                                gradient[1] += difference * yGradient[k];
                                hessian[0][0] += xGradient[k] * xGradient[k];
                                hessian[0][1] += xGradient[k] * yGradient[k];
                                hessian[1][1] += yGradient[k] * yGradient[k];
                            }
                        }
                    }
                } else {
                    k += outNx;
                }
            }
        }
        for (int i = 1; (i < transformation.getNumParam()); i++) {
            for (int j = 0; (j < i); j++) {
                hessian[i][j] = hessian[j][i];
            }
        }
        return (meanSquares / area);
    } /* end getTranslationMeanSquares */

    /*------------------------------------------------------------------*/
    private double interpolate() {
        t = 0.0;
        for (int j = 0; (j < 4); j++) {
            s = 0.0;
            p = yIndex[j];
            for (int i = 0; (i < 4); i++) {
                s += xWeight[i] * inImg[p + xIndex[i]];
            }
            t += yWeight[j] * s;
        }
        return (t);
    } /* end interpolate */

    // /*------------------------------------------------------------------*/
    // private double interpolateDx() {
    // t = 0.0;
    // for (int j = 0; (j < 4); j++) {
    // s = 0.0;
    // p = yIndex[j];
    // for (int i = 0; (i < 4); i++) {
    // s += dxWeight[i] * (double) inImg[p + xIndex[i]];
    // }
    // t += yWeight[j] * s;
    // }
    // return (t);
    // } /* end interpolateDx */

    // /*------------------------------------------------------------------*/
    // private double interpolateDy() {
    // t = 0.0;
    // for (int j = 0; (j < 4); j++) {
    // s = 0.0;
    // p = yIndex[j];
    // for (int i = 0; (i < 4); i++) {
    // s += xWeight[i] * (double) inImg[p + xIndex[i]];
    // }
    // t += dyWeight[j] * s;
    // }
    // return (t);
    // } /* end interpolateDy */

    /*------------------------------------------------------------------*/
    private void inverseMarquardtLevenbergOptimization(int workload) {
        final double[][] attempt = new double[transformation.getNumParam() / 2][2];
        final double[][] hessian = new double[transformation.getNumParam()][transformation.getNumParam()];
        final double[][] pseudoHessian = new double[transformation.getNumParam()][transformation.getNumParam()];
        final double[] gradient = new double[transformation.getNumParam()];
        double[][] matrix = getTransformationMatrix(sourcePoint, targetPoint);
        double[] update = new double[transformation.getNumParam()];
        double bestMeanSquares = 0.0;
        double meanSquares = 0.0;
        double lambda = FIRST_LAMBDA;
        double displacement;
        int iteration = 0;
        switch (transformation) {
            case TRANSLATION: {
                bestMeanSquares = getTranslationMeanSquares(matrix, hessian, gradient);
                break;
            }
            case SCALED_ROTATION: {
                bestMeanSquares = getScaledRotationMeanSquares(sourcePoint, matrix, hessian, gradient);
                break;
            }
            case AFFINE: {
                bestMeanSquares = getAffineMeanSquares(sourcePoint, matrix, hessian, gradient);
                break;
            }
        }
        iteration++;
        do {
            for (int k = 0; (k < transformation.getNumParam()); k++) {
                pseudoHessian[k][k] = (1.0 + lambda) * hessian[k][k];
            }
            invertGauss(pseudoHessian);
            update = matrixMultiply(pseudoHessian, gradient);
            displacement = 0.0;
            for (int k = 0; (k < (transformation.getNumParam() / 2)); k++) {
                attempt[k][0] = sourcePoint[k][0] - update[2 * k];
                attempt[k][1] = sourcePoint[k][1] - update[(2 * k) + 1];
                displacement +=
                        Math.sqrt((update[2 * k] * update[2 * k]) + (update[(2 * k) + 1] * update[(2 * k) + 1]));
            }
            displacement /= 0.5 * transformation.getNumParam();
            matrix = getTransformationMatrix(attempt, targetPoint);
            switch (transformation) {
                case TRANSLATION: {
                    if (accelerated) {
                        meanSquares = getTranslationMeanSquares(matrix, gradient);
                    } else {
                        meanSquares = getTranslationMeanSquares(matrix, hessian, gradient);
                    }
                    break;
                }
                case SCALED_ROTATION: {
                    if (accelerated) {
                        meanSquares = getScaledRotationMeanSquares(attempt, matrix, gradient);
                    } else {
                        meanSquares = getScaledRotationMeanSquares(attempt, matrix, hessian, gradient);
                    }
                    break;
                }
                case AFFINE: {
                    if (accelerated) {
                        meanSquares = getAffineMeanSquares(attempt, matrix, gradient);
                    } else {
                        meanSquares = getAffineMeanSquares(attempt, matrix, hessian, gradient);
                    }
                    break;
                }
            }
            iteration++;
            if (meanSquares < bestMeanSquares) {
                bestMeanSquares = meanSquares;
                for (int k = 0; (k < (transformation.getNumParam() / 2)); k++) {
                    sourcePoint[k][0] = attempt[k][0];
                    sourcePoint[k][1] = attempt[k][1];
                }
                lambda /= LAMBDA_MAGSTEP;
            } else {
                lambda *= LAMBDA_MAGSTEP;
            }
            turboRegProgressBar.skipProgressBar(iterationCost);
            workload--;
        } while ((iteration < ((maxIterations * iterationPower) - 1)) && (pixelPrecision <= displacement));
        invertGauss(hessian);
        update = matrixMultiply(hessian, gradient);
        for (int k = 0; (k < (transformation.getNumParam() / 2)); k++) {
            attempt[k][0] = sourcePoint[k][0] - update[2 * k];
            attempt[k][1] = sourcePoint[k][1] - update[(2 * k) + 1];
        }
        matrix = getTransformationMatrix(attempt, targetPoint);
        switch (transformation) {
            case TRANSLATION: {
                meanSquares = getTranslationMeanSquares(matrix);
                break;
            }
            case SCALED_ROTATION: {
                meanSquares = getScaledRotationMeanSquares(attempt, matrix);
                break;
            }
            case AFFINE: {
                meanSquares = getAffineMeanSquares(attempt, matrix);
                break;
            }
        }
        iteration++;
        if (meanSquares < bestMeanSquares) {
            for (int k = 0; (k < (transformation.getNumParam() / 2)); k++) {
                sourcePoint[k][0] = attempt[k][0];
                sourcePoint[k][1] = attempt[k][1];
            }
        }
        turboRegProgressBar.skipProgressBar(workload * iterationCost);
    } /* end inverseMarquardtLevenbergOptimization */

    /*------------------------------------------------------------------*/
    private void inverseMarquardtLevenbergRigidBodyOptimization(int workload) {
        final double[][] attempt = new double[2][3];
        final double[][] hessian = new double[transformation.getNumParam()][transformation.getNumParam()];
        final double[][] pseudoHessian = new double[transformation.getNumParam()][transformation.getNumParam()];
        final double[] gradient = new double[transformation.getNumParam()];
        double[][] matrix = getTransformationMatrix(targetPoint, sourcePoint);
        double[] update = new double[transformation.getNumParam()];
        double bestMeanSquares = 0.0;
        double meanSquares = 0.0;
        double lambda = FIRST_LAMBDA;
        double angle;
        double c;
        double s;
        double displacement;
        int iteration = 0;
        for (int k = 0; (k < transformation.getNumParam()); k++) {
            sourcePoint[k][0] = matrix[0][0] + (targetPoint[k][0] * matrix[0][1]) + (targetPoint[k][1] * matrix[0][2]);
            sourcePoint[k][1] = matrix[1][0] + (targetPoint[k][0] * matrix[1][1]) + (targetPoint[k][1] * matrix[1][2]);
        }
        matrix = getTransformationMatrix(sourcePoint, targetPoint);
        bestMeanSquares = getRigidBodyMeanSquares(matrix, hessian, gradient);
        iteration++;
        do {
            for (int k = 0; (k < transformation.getNumParam()); k++) {
                pseudoHessian[k][k] = (1.0 + lambda) * hessian[k][k];
            }
            invertGauss(pseudoHessian);
            update = matrixMultiply(pseudoHessian, gradient);
            angle = Math.atan2(matrix[0][2], matrix[0][1]) - update[0];
            attempt[0][1] = Math.cos(angle);
            attempt[0][2] = Math.sin(angle);
            attempt[1][1] = -attempt[0][2];
            attempt[1][2] = attempt[0][1];
            c = Math.cos(update[0]);
            s = Math.sin(update[0]);
            attempt[0][0] = ((matrix[0][0] + update[1]) * c) - ((matrix[1][0] + update[2]) * s);
            attempt[1][0] = ((matrix[0][0] + update[1]) * s) + ((matrix[1][0] + update[2]) * c);
            displacement =
                    Math.sqrt((update[1] * update[1]) + (update[2] * update[2]))
                            + (0.25 * Math.sqrt((double)(inNx * inNx) + (double)(inNy * inNy)) * Math.abs(update[0]));
            if (accelerated) {
                meanSquares = getRigidBodyMeanSquares(attempt, gradient);
            } else {
                meanSquares = getRigidBodyMeanSquares(attempt, hessian, gradient);
            }
            iteration++;
            if (meanSquares < bestMeanSquares) {
                bestMeanSquares = meanSquares;
                for (int i = 0; (i < 2); i++) {
                    for (int j = 0; (j < 3); j++) {
                        matrix[i][j] = attempt[i][j];
                    }
                }
                lambda /= LAMBDA_MAGSTEP;
            } else {
                lambda *= LAMBDA_MAGSTEP;
            }
            turboRegProgressBar.skipProgressBar(iterationCost);
            workload--;
        } while ((iteration < ((maxIterations * iterationPower) - 1)) && (pixelPrecision <= displacement));
        invertGauss(hessian);
        update = matrixMultiply(hessian, gradient);
        angle = Math.atan2(matrix[0][2], matrix[0][1]) - update[0];
        attempt[0][1] = Math.cos(angle);
        attempt[0][2] = Math.sin(angle);
        attempt[1][1] = -attempt[0][2];
        attempt[1][2] = attempt[0][1];
        c = Math.cos(update[0]);
        s = Math.sin(update[0]);
        attempt[0][0] = ((matrix[0][0] + update[1]) * c) - ((matrix[1][0] + update[2]) * s);
        attempt[1][0] = ((matrix[0][0] + update[1]) * s) + ((matrix[1][0] + update[2]) * c);
        meanSquares = getRigidBodyMeanSquares(attempt);
        iteration++;
        if (meanSquares < bestMeanSquares) {
            for (int i = 0; (i < 2); i++) {
                for (int j = 0; (j < 3); j++) {
                    matrix[i][j] = attempt[i][j];
                }
            }
        }
        for (int k = 0; (k < transformation.getNumParam()); k++) {
            sourcePoint[k][0] =
                    ((targetPoint[k][0] - matrix[0][0]) * matrix[0][1])
                            + ((targetPoint[k][1] - matrix[1][0]) * matrix[1][1]);
            sourcePoint[k][1] =
                    ((targetPoint[k][0] - matrix[0][0]) * matrix[0][2])
                            + ((targetPoint[k][1] - matrix[1][0]) * matrix[1][2]);
        }
        turboRegProgressBar.skipProgressBar(workload * iterationCost);
    } /* end inverseMarquardtLevenbergRigidBodyOptimization */

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

    // /*------------------------------------------------------------------*/
    // private void marquardtLevenbergOptimization(int workload) {
    // final double[][] attempt = new double[transformation.getNumParam() /
    // 2][2];
    // final double[][] hessian = new
    // double[transformation.getNumParam()][transformation
    // .getNumParam()];
    // final double[][] pseudoHessian = new double[transformation
    // .getNumParam()][transformation.getNumParam()];
    // final double[] gradient = new double[transformation.getNumParam()];
    // double[][] matrix = getTransformationMatrix(targetPoint,
    // sourcePoint);
    // double[] update = new double[transformation.getNumParam()];
    // double bestMeanSquares = 0.0;
    // double meanSquares = 0.0;
    // double lambda = FIRST_LAMBDA;
    // double displacement;
    // int iteration = 0;
    // bestMeanSquares = getBilinearMeanSquares(matrix, hessian, gradient);
    // iteration++;
    // do {
    // for (int k = 0; (k < transformation.getNumParam()); k++) {
    // pseudoHessian[k][k] = (1.0 + lambda) * hessian[k][k];
    // }
    // invertGauss(pseudoHessian);
    // update = matrixMultiply(pseudoHessian, gradient);
    // displacement = 0.0;
    // for (int k = 0; (k < (transformation.getNumParam() / 2)); k++) {
    // attempt[k][0] = sourcePoint[k][0] - update[2 * k];
    // attempt[k][1] = sourcePoint[k][1] - update[2 * k + 1];
    // displacement += Math.sqrt(update[2 * k] * update[2 * k]
    // + update[2 * k + 1] * update[2 * k + 1]);
    // }
    // displacement /= 0.5 * (double) transformation.getNumParam();
    // matrix = getTransformationMatrix(targetPoint, attempt);
    // meanSquares = getBilinearMeanSquares(matrix, hessian, gradient);
    // iteration++;
    // if (meanSquares < bestMeanSquares) {
    // bestMeanSquares = meanSquares;
    // for (int k = 0; (k < (transformation.getNumParam() / 2)); k++) {
    // sourcePoint[k][0] = attempt[k][0];
    // sourcePoint[k][1] = attempt[k][1];
    // }
    // lambda /= LAMBDA_MAGSTEP;
    // } else {
    // lambda *= LAMBDA_MAGSTEP;
    // }
    // turboRegProgressBar.skipProgressBar(iterationCost);
    // workload--;
    // } while ((iteration < (maxIterations * iterationPower - 1))
    // && (pixelPrecision <= displacement));
    // invertGauss(hessian);
    // update = matrixMultiply(hessian, gradient);
    // for (int k = 0; (k < (transformation.getNumParam() / 2)); k++) {
    // attempt[k][0] = sourcePoint[k][0] - update[2 * k];
    // attempt[k][1] = sourcePoint[k][1] - update[2 * k + 1];
    // }
    // matrix = getTransformationMatrix(targetPoint, attempt);
    // meanSquares = getBilinearMeanSquares(matrix);
    // iteration++;
    // if (meanSquares < bestMeanSquares) {
    // for (int k = 0; (k < (transformation.getNumParam() / 2)); k++) {
    // sourcePoint[k][0] = attempt[k][0];
    // sourcePoint[k][1] = attempt[k][1];
    // }
    // }
    // turboRegProgressBar.skipProgressBar(workload * iterationCost);
    // } /* end marquardtLevenbergOptimization */

    /*------------------------------------------------------------------*/
    private double[] matrixMultiply(final double[][] matrix, final double[] vector) {
        final double[] result = new double[matrix.length];
        for (int i = 0; (i < matrix.length); i++) {
            result[i] = 0.0;
            for (int j = 0; (j < vector.length); j++) {
                result[i] += matrix[i][j] * vector[j];
            }
        }
        return (result);
    } /* end matrixMultiply */

    /*------------------------------------------------------------------*/
    private void scaleBottomDownLandmarks() {
        for (int depth = 1; (depth < pyramidDepth); depth++) {
            if (transformation == TransformationType.RIGID_BODY) {
                for (int n = 0; n < transformation.getNumParam(); n++) {
                    sourcePoint[n][0] *= 0.5;
                    sourcePoint[n][1] *= 0.5;
                    targetPoint[n][0] *= 0.5;
                    targetPoint[n][1] *= 0.5;
                }
            } else {
                for (int n = 0; (n < ((transformation.getNumParam()) / 2)); n++) {
                    sourcePoint[n][0] *= 0.5;
                    sourcePoint[n][1] *= 0.5;
                    targetPoint[n][0] *= 0.5;
                    targetPoint[n][1] *= 0.5;
                }
            }
        }
    } /* end scaleBottomDownLandmarks */

    /*------------------------------------------------------------------*/
    private void scaleUpLandmarks() {
        if (transformation == TransformationType.RIGID_BODY) {
            for (int n = 0; (n < transformation.getNumParam()); n++) {
                sourcePoint[n][0] *= 2.0;
                sourcePoint[n][1] *= 2.0;
                targetPoint[n][0] *= 2.0;
                targetPoint[n][1] *= 2.0;
            }
        } else {
            for (int n = 0; (n < ((transformation.getNumParam()) / 2)); n++) {
                sourcePoint[n][0] *= 2.0;
                sourcePoint[n][1] *= 2.0;
                targetPoint[n][0] *= 2.0;
                targetPoint[n][1] *= 2.0;
            }
        }
    } /* end scaleUpLandmarks */

    /*------------------------------------------------------------------*/
    private void translationTransform(final double[][] matrix) {
        double dx = matrix[0][0];
        double dy = matrix[1][0];
        final double dx0 = dx;
        int xMsk;
        int yMsk;
        x = dx - Math.floor(dx);
        y = dy - Math.floor(dy);
        if (!accelerated) {
            xWeights();
            yWeights();
        }
        int k = 0;
        turboRegProgressBar.addWorkload(outNy);
        for (int v = 0; (v < outNy); v++) {
            y = dy++;
            yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
            if ((0 <= yMsk) && (yMsk < inNy)) {
                yMsk *= inNx;
                if (!accelerated) {
                    yIndexes();
                }
                dx = dx0;
                for (int u = 0; (u < outNx); u++) {
                    x = dx++;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)) {
                        xMsk += yMsk;
                        if (accelerated) {
                            outImg[k++] = inImg[xMsk];
                        } else {
                            xIndexes();
                            outImg[k++] = (float)interpolate();
                        }
                    } else {
                        outImg[k++] = 0.0F;
                    }
                }
            } else {
                for (int u = 0; (u < outNx); u++) {
                    outImg[k++] = 0.0F;
                }
            }
            turboRegProgressBar.stepProgressBar();
        }
        turboRegProgressBar.workloadDone(outNy);
    } /* translationTransform */

    /*------------------------------------------------------------------*/
    private void translationTransform(final double[][] matrix, final float[] outMsk) {
        double dx = matrix[0][0];
        double dy = matrix[1][0];
        final double dx0 = dx;
        int xMsk;
        int yMsk;
        x = dx - Math.floor(dx);
        y = dy - Math.floor(dy);
        if (!accelerated) {
            xWeights();
            yWeights();
        }
        int k = 0;
        turboRegProgressBar.addWorkload(outNy);
        for (int v = 0; (v < outNy); v++) {
            y = dy++;
            yMsk = (0.0 <= y) ? ((int)(y + 0.5)) : ((int)(y - 0.5));
            if ((0 <= yMsk) && (yMsk < inNy)) {
                yMsk *= inNx;
                if (!accelerated) {
                    yIndexes();
                }
                dx = dx0;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = dx++;
                    xMsk = (0.0 <= x) ? ((int)(x + 0.5)) : ((int)(x - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)) {
                        xMsk += yMsk;
                        if (accelerated) {
                            outImg[k] = inImg[xMsk];
                        } else {
                            xIndexes();
                            outImg[k] = (float)interpolate();
                        }
                        outMsk[k] = inMsk[xMsk];
                    } else {
                        outImg[k] = 0.0F;
                        outMsk[k] = 0.0F;
                    }
                }
            } else {
                for (int u = 0; (u < outNx); u++, k++) {
                    outImg[k] = 0.0F;
                    outMsk[k] = 0.0F;
                }
            }
            turboRegProgressBar.stepProgressBar();
        }
        turboRegProgressBar.workloadDone(outNy);
    } /* translationTransform */

    //
    // /*------------------------------------------------------------------*/
    // private void xDxWeights() {
    // s = 1.0 - x;
    // dxWeight[0] = 0.5 * x * x;
    // xWeight[0] = x * dxWeight[0] / 3.0;
    // dxWeight[3] = -0.5 * s * s;
    // xWeight[3] = s * dxWeight[3] / -3.0;
    // dxWeight[1] = 1.0 - 2.0 * dxWeight[0] + dxWeight[3];
    // xWeight[1] = 2.0 / 3.0 + (1.0 + x) * dxWeight[3];
    // dxWeight[2] = 1.5 * x * (x - 4.0 / 3.0);
    // xWeight[2] = 2.0 / 3.0 - (2.0 - x) * dxWeight[0];
    // } /* xDxWeights */

    /*------------------------------------------------------------------*/
    private void xIndexes() {
        p = (0.0 <= x) ? ((int)x + 2) : ((int)x + 1);
        for (int k = 0; (k < 4); p--, k++) {
            q = (p < 0) ? (-1 - p) : (p);
            if (twiceInNx <= q) {
                q -= twiceInNx * (q / twiceInNx);
            }
            xIndex[k] = (inNx <= q) ? (twiceInNx - 1 - q) : (q);
        }
    } /* xIndexes */

    /*------------------------------------------------------------------*/
    private void xWeights() {
        s = 1.0 - x;
        xWeight[3] = (s * s * s) / 6.0;
        s = x * x;
        xWeight[2] = (2.0 / 3.0) - (0.5 * s * (2.0 - x));
        xWeight[0] = (s * x) / 6.0;
        xWeight[1] = 1.0 - xWeight[0] - xWeight[2] - xWeight[3];
    } /* xWeights */

    // /*------------------------------------------------------------------*/
    // private void yDyWeights() {
    // t = 1.0 - y;
    // dyWeight[0] = 0.5 * y * y;
    // yWeight[0] = y * dyWeight[0] / 3.0;
    // dyWeight[3] = -0.5 * t * t;
    // yWeight[3] = t * dyWeight[3] / -3.0;
    // dyWeight[1] = 1.0 - 2.0 * dyWeight[0] + dyWeight[3];
    // yWeight[1] = 2.0 / 3.0 + (1.0 + y) * dyWeight[3];
    // dyWeight[2] = 1.5 * y * (y - 4.0 / 3.0);
    // yWeight[2] = 2.0 / 3.0 - (2.0 - y) * dyWeight[0];
    // } /* yDyWeights */

    /*------------------------------------------------------------------*/
    private void yIndexes() {
        p = (0.0 <= y) ? ((int)y + 2) : ((int)y + 1);
        for (int k = 0; (k < 4); p--, k++) {
            q = (p < 0) ? (-1 - p) : (p);
            if (twiceInNy <= q) {
                q -= twiceInNy * (q / twiceInNy);
            }
            yIndex[k] = (inNy <= q) ? ((twiceInNy - 1 - q) * inNx) : (q * inNx);
        }
    } /* yIndexes */

    /*------------------------------------------------------------------*/
    private void yWeights() {
        t = 1.0 - y;
        yWeight[3] = (t * t * t) / 6.0;
        t = y * y;
        yWeight[2] = (2.0 / 3.0) - (0.5 * t * (2.0 - y));
        yWeight[0] = (t * y) / 6.0;
        yWeight[1] = 1.0 - yWeight[0] - yWeight[2] - yWeight[3];
    } /* yWeights */

} /* end class turboRegTransform */
