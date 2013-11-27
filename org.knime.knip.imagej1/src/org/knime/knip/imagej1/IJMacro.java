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

import ij.ImagePlus;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.measure.ResultsTable;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.ops.operation.Operations;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.NodeLogger;
import org.knime.knip.imagej2.core.util.IJToImg;
import org.knime.knip.imagej2.core.util.ImgToIJ;

/**
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class IJMacro {

	// private final String m_ijDirectory;
	private final boolean m_redirectStreams;

	private final String m_code;

	private ImgPlus<? extends RealType<?>> m_resImg;

	private ResultsTable m_resTable;

	public IJMacro(final String code) {
		this(false, code);
	}

	public IJMacro(final boolean redirectStreams, final String code) {
		// m_ijDirectory = ijDirectory;
		m_redirectStreams = redirectStreams;
		m_code = code;
		Interpreter.batchMode = true;
	}

	public final RealType<?> runOn(final ImgPlus<? extends RealType<?>> img,
			final RealType<?> matchingType) {
		final Map<String, ImgPlus<? extends RealType<?>>> map = new HashMap<String, ImgPlus<? extends RealType<?>>>();
		map.put("A", img);
		return runOn(map, matchingType);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public final RealType<?> runOn(
			final Map<String, ImgPlus<? extends RealType<?>>> imgs,
			RealType<?> matchingType) {
		// final String oldUserDir = System.getProperty("user.dir");
		final PrintStream out = System.out;
		final PrintStream err = System.err;
		try {
			// if (m_ijDirectory != null) {
			// NodeLogger
			// .getLogger(IJMacro.class)
			// .info("System property \"user.dir\" temporary redirected to provide ImageJ plugins path.");
			// System.setProperty("user.dir", m_ijDirectory);
			// }
			// Redirect System.out and System.err WARN: this
			// workaround may
			// affect the system.out of parallel running nodes -> is
			// there a
			// better way? but anyway, system.out is a bad habit
			if (m_redirectStreams) {
				System.setErr(new NodeLoggerPrintStream(NodeLogger
						.getLogger(IJMacro.class), NodeLogger.LEVEL.ERROR));
				System.setOut(new NodeLoggerPrintStream(NodeLogger
						.getLogger(IJMacro.class), NodeLogger.LEVEL.INFO));
			}
			m_resTable = ResultsTable.getResultsTable();
			// TODO Run different ImageJ instances?
			synchronized (m_resTable) {
				final Interpreter inter = new Interpreter();
				// Prepare images
				for (final Entry<String, ImgPlus<? extends RealType<?>>> pair : imgs
						.entrySet()) {
					final ImagePlus plus = Operations.compute(new ImgToIJ(),
							pair.getValue());
					plus.setTitle(pair.getKey());
					Interpreter.addBatchModeImage(plus);
					WindowManager.setTempCurrentImage(plus);
				}
				m_resTable.reset();
				// This must be the run method with two string
				// arguments
				inter.run(m_code, "");
				final ImagePlus resPlus = Interpreter.getLastBatchModeImage();
				if (resPlus != null) {
					final Img<?> org = imgs.get(resPlus.getTitle());
					// If the image was only modified,
					// truncate to the same
					// dimensionality
					final int ndim = org != null ? org.numDimensions() : -1;

					if (matchingType == null) {
						matchingType = IJToImg.createMatchingType(resPlus);
					}

					final Img<? extends RealType<?>> res = Operations.compute(
							new IJToImg(matchingType, false, ndim), resPlus);

					if ((org != null) && (org instanceof ImgPlusMetadata)) {
						// If the image was only
						// modified and it holds meta
						// data, drag them along
						m_resImg = new ImgPlus(res, (ImgPlusMetadata) org);
					} else {
						m_resImg = new ImgPlus(res);
					}
				}

				// m_resTable = parseResultTable(rt);
				// Clean up
				while (WindowManager.getImageCount() > 0) {
					final ImagePlus current = WindowManager.getCurrentImage();
					Interpreter.removeBatchModeImage(current);
					current.close();
				}
				WindowManager.closeAllWindows();
			}
		} finally {
			// System.setProperty("user.dir", oldUserDir);
			System.setErr(out);
			System.setOut(err);
		}

		return matchingType;
	}

	// private static TableModel parseResultTable(ResultsTable res) {
	// int rows = res.getCounter();
	// if (rows == 0)
	// return new DefaultTableModel(new Object[0][0], new String[0]);
	// // Ignore null columns
	// int cols = 1;
	// for (int i = 0; i < res.getLastColumn(); i++) {
	// if (res.getColumn(i) != null)
	// cols++;
	// }
	// Object[][] data = new Object[rows][1 + cols];
	// String[] columns = new String[1 + cols];
	// columns[0] = "Label";
	// String label;
	// for (int i = 0; i < res.getCounter(); i++) {
	// label = res.getLabel(i);
	// data[i][0] = label != null ? label : "";
	// }
	// int r, ijc = 0;
	// float[] column;
	// for (int realc = 1; realc <= cols; realc++) {
	// while ((column = res.getColumn(ijc)) == null)
	// ijc++;
	// columns[realc] = res.getColumnHeading(ijc);
	// for (r = 0; r < res.getCounter(); r++) {
	// data[r][realc] = column[r];
	// }
	// ijc++;
	// }
	// return new DefaultTableModel(data, columns);
	// }

	public final ImgPlus<? extends RealType<?>> resImgPlus() {
		return m_resImg;
	}

	public final ResultsTable resTable() {
		return m_resTable;
	}
}
