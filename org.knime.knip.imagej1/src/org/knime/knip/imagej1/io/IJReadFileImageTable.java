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
package org.knime.knip.imagej1.io;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.io.IOException;
import java.util.Vector;

import net.imglib2.img.Img;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.ShortType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;

/**
 * Implements a <code>DataTable</code> that read image data from files.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class IJReadFileImageTable implements DataTable {

	private final NodeLogger LOGGER = NodeLogger
			.getLogger(IJReadFileImageTable.class);

	/*
	 * Reference to the execution context to set the progress.
	 */
	private ExecutionContext m_exec;

	/*
	 * List of the files to open (full path and file name)
	 */

	/** The row header prefix that is used if nothing else is specified. */
	public static final String STANDARD_ROW_PREFIX = "SAMPLE_";

	/** Suffix of image files. */
	public static final String SUFFIX = ".tif";

	/*
	 * Holds the file list.
	 */
	private String[] m_fileReferences;

	/*
	 * The current position of the iterator in the file list.
	 */
	private int m_idx;

	/*
	 * Number of errors occured, while the opening images by means of the
	 * iterator.
	 */
	private int m_numErrors;

	private ImgPlusCellFactory m_imgCellFactory;

	/**
	 * Creates an new and empty ImageTable and is useful to get the table
	 * specification without actually knowing the content.
	 * 
	 * @param metaDataColumns
	 *            the meta data keys
	 * @param planeSelection
	 * @param omexml
	 * 
	 */
	public IJReadFileImageTable() {
	}

	/**
	 * Constructor for an ImageTable.
	 * 
	 * @param dTableSpec
	 *            The DataTableSpec
	 * @param exec
	 * @param planeSelection
	 * @param files
	 *            The files to open
	 */
	public IJReadFileImageTable(final ExecutionContext exec,
			final String[] filelist) {

		m_fileReferences = filelist;
		m_exec = exec;
		m_imgCellFactory = new ImgPlusCellFactory(exec);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataTableSpec getDataTableSpec() {
		final int col = 0;

		DataColumnSpecCreator creator;
		final DataColumnSpec[] cspecs = new DataColumnSpec[1];
		creator = new DataColumnSpecCreator("Image", ImgPlusCell.TYPE);
		cspecs[col] = creator.createSpec();

		return new DataTableSpec(cspecs);
	}

	/**
	 * 
	 * @return true, if an error occurred while iterating through the filelist
	 *         to open the images.
	 */
	public boolean hasAnErrorOccured() {
		return m_numErrors > 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RowIterator iterator() {

		m_idx = 0;
		m_numErrors = 0;

		return new RowIterator() {

			@Override
			public boolean hasNext() {
				return m_idx < m_fileReferences.length;
			}

			@SuppressWarnings("unchecked")
			@Override
			public DataRow next() {
				String rowHeaderName = STANDARD_ROW_PREFIX;
				final Vector<DataCell> row = new Vector<DataCell>();

				// create RowHeader
				// rowHeaderName =
				// new
				// File(m_files[m_pos].getReference()).getName();
				rowHeaderName = m_fileReferences[m_idx];
				final DataCell[] result = new DataCell[1];

				final ImagePlus ijImagePlus = new ImagePlus(
						m_fileReferences[m_idx]);

				// read image(s)
				final int[] srcDims = ijImagePlus.getDimensions();

				final AxisType[] labels = new AxisType[srcDims.length];
				labels[0] = Axes.get("X");
				labels[1] = Axes.get("Y");
				// if (labels.length > 1)
				// labels[2] = Axes.CHANNEL;
				// for (int i = 3; i < labels.length; i++) {
				// labels[i] = Axes.UNKNOWN;
				// }

				Img<? extends RealType<?>> resImg = null;

				final ImageProcessor ip = ijImagePlus.getProcessor();
				if (ip == null) {
					LOGGER.error("Can not open the file "
							+ m_fileReferences[m_idx]);
					m_numErrors++;

				} else {
					final Object pixels = ip.getPixels();
					if (ip instanceof ByteProcessor) {
						resImg = new PlanarImgFactory().create(new int[] {
								srcDims[0], srcDims[1] }, new ByteType());
					} else if (ip instanceof ShortProcessor) {
						resImg = new PlanarImgFactory().create(new int[] {
								srcDims[0], srcDims[1] }, new ShortType());

					} else {
						LOGGER.error("Can not open the file "
								+ m_fileReferences[m_idx]);
						m_numErrors++;
					}
					System.arraycopy(pixels, 0, ((PlanarImg) resImg)
							.getPlane(0).getCurrentStorageArray(), 0, ip
							.getPixelCount());

					try {
						result[0] = m_imgCellFactory.createCell(new ImgPlus(
								resImg, m_fileReferences[m_idx], labels));
					} catch (final IOException e) {
						LOGGER.error("Error creating ImgPlusCell.", e);
						result[0] = DataType.getMissingCell();
					}

				}

				for (final DataCell cell : result) {
					if (cell == null) {
						row.add(DataType.getMissingCell());
					} else {

						row.add(cell);
					}
				}

				DataCell[] rowvalues = new DataCell[row.size()];
				rowvalues = row.toArray(rowvalues);
				m_idx++;
				m_exec.setProgress((double) m_idx / m_fileReferences.length);

				return new DefaultRow(new RowKey(rowHeaderName), rowvalues);

			}

		};

	}
}
