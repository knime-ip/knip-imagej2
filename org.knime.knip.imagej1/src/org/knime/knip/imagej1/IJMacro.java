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
package org.knime.knip.imagej1;

import org.knime.knip.imagej2.core.util.IJToImg;
import org.knime.knip.imagej2.core.util.ImgToIJ;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.measure.ResultsTable;
import ij.text.TextPanel;
import net.imagej.ImgPlus;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.type.numeric.RealType;

/**
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @param <T>
 */
public class IJMacro<T extends RealType<T>> {

    // private final String m_ijDirectory;
    private final String m_code;

    private ImgPlus<? extends RealType<?>> m_resImg;

    private ResultsTable m_resTable;

    /**
     * @param code
     */
    public IJMacro(final String code) {
        m_code = code;
        Interpreter.batchMode = true;
    }

    /**
     * @param img
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public final void run(final ImgPlus<T> img) {

        m_resTable = ResultsTable.getResultsTable();

        // We fake a text panel as some result tables require one..

        // TODO Run different ImageJ instances?
        synchronized (m_resTable) {
            IJ.setTextPanel(new TextPanel("Dummy"));
            final Interpreter inter = new Interpreter();
            // Prepare images
            final ImagePlus plus = ImgToIJ.wrap(img);
            plus.setTitle(img.getName());
            Interpreter.addBatchModeImage(plus);
            WindowManager.setTempCurrentImage(plus);
            m_resTable.reset();

            // This must be the run method with two string
            // arguments
            inter.run(m_code, "");

            final ImagePlus resPlus = Interpreter.getLastBatchModeImage();
            if (resPlus != null) {
                // If the image was only modified,
                // truncate to the same
                // dimensionality
                Img res = (Img)Operations.compute(new IJToImg(IJToImg.createMatchingType(resPlus), false, 5), resPlus);

                final Img<? extends RealType<?>> cleanRes =
                        new ImgView(SubsetOperations.subsetview(res, res), img.factory());

                if (cleanRes.numDimensions() == img.numDimensions()) {
                    // If the image was only
                    // modified and it holds meta
                    // data, drag them along
                    m_resImg = new ImgPlus(cleanRes, img);
                } else {
                    m_resImg = new ImgPlus(cleanRes);
                }
            }

             m_resTable = ResultsTable.getResultsTable();
            // Clean up
            while (WindowManager.getImageCount() > 0) {
                final ImagePlus current = WindowManager.getCurrentImage();
                Interpreter.removeBatchModeImage(current);
                current.close();
            }
            WindowManager.closeAllWindows();
        }
    }

    /**
     * @return the result {@link ImgPlus}
     */
    public final ImgPlus<? extends RealType<?>> resImgPlus() {
        return m_resImg;
    }

    /**
     * @return {@link ResultsTable} of this macro
     */
    public final ResultsTable resTable() {
        return m_resTable;
    }
}
