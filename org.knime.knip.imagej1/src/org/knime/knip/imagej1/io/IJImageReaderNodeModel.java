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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

/**
 * This Node reads images.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class IJImageReaderNodeModel extends NodeModel {

	/**
	 * Key for the settings holding the file list.
	 */
	public static final String CFG_FILE_LIST = "file_list";

	/**
	 * Key to store the directory history.
	 */
	public static final String CFG_DIR_HISTORY = "imagereader_dirhistory";

	/*
	 * Settings for the file list.
	 */
	private final SettingsModelStringArray m_files = new SettingsModelStringArray(
			CFG_FILE_LIST, new String[] {});

	/*
	 * Collection of all settings.
	 */

	private final Collection<SettingsModel> m_settingsCollection;

	/**
	 * The image out port of the Node.
	 */
	public static final int IMAGEOUTPORT = 0;

	/*
	 * The image-output-table DataSpec
	 */
	private DataTableSpec m_outspec;

	/**
	 * Initializes the ImageReader
	 */
	public IJImageReaderNodeModel() {
		super(0, 1);
		m_settingsCollection = new ArrayList<SettingsModel>();
		m_settingsCollection.add(m_files);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		//
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		final IJReadFileImageTable tab = new IJReadFileImageTable();
		// tab.setDimLabelProperty(m_planeSelect.getDimLabelsAsString());
		m_outspec = tab.getDataTableSpec();

		return new DataTableSpec[] { m_outspec };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		if (m_outspec == null) {
			final DataTableSpec[] inSpecs = new DataTableSpec[inData.length];
			for (int i = 0; i < inSpecs.length; i++) {
				inSpecs[i] = inData[i].getDataTableSpec();
			}
			configure(inSpecs);
		}
		final String[] fnames = m_files.getStringArrayValue();

		// String[] metaDataColumns =
		// m_metadatakeys.getStringArrayValue();
		final IJReadFileImageTable dt = new IJReadFileImageTable(exec, fnames);
		// dt.setDimLabelProperty(m_planeSelect.getDimLabelsAsString());
		final BufferedDataTable[] out = new BufferedDataTable[] { exec
				.createBufferedDataTable(dt, exec) };
		if (dt.hasAnErrorOccured()) {
			setWarningMessage("Some errors occured opening images or image planes!");
		}

		return out;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		for (final SettingsModel sm : m_settingsCollection) {
			sm.saveSettingsTo(settings);
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		for (final SettingsModel sm : m_settingsCollection) {
			sm.validateSettings(settings);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		for (final SettingsModel sm : m_settingsCollection) {
			sm.loadSettingsFrom(settings);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		//
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		//
	}
}
