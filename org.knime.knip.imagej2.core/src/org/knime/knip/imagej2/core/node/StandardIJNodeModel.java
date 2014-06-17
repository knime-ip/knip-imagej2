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
package org.knime.knip.imagej2.core.node;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.knip.imagej2.core.adapter.IJAdapterProvider;
import org.knime.knip.imagej2.core.adapter.IJInputAdapter;
import org.knime.knip.imagej2.core.adapter.IJStandardInputAdapter;
import org.knime.knip.imagej2.core.adapter.ModuleItemConfig;
import org.knime.knip.imagej2.core.adapter.ModuleItemDataValueConfig;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;

/**
 * Standard implementation that can be used for all modules (in contrast to the ValueToCell implementation)
 *
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class StandardIJNodeModel extends AbstractIJNodeModel {

    /** settings model for the append results to talbe check box */
    private final SettingsModelBoolean m_appendColumns = createAppendColumnsModel();

    /**
     * list of the module item configs collected from the used input adapters.
     */
    private final List<ModuleItemConfig> m_moduleItemConfigs;

    /** {@link #m_appendColumns} */
    static SettingsModelBoolean createAppendColumnsModel() {
        return new SettingsModelBoolean("append_columns", false);
    }

    /**
     * TODO
     *
     * @param moduleInfo
     * @param nrInputPorts
     * @param nrOutputPorts
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public StandardIJNodeModel(final ModuleInfo moduleInfo, final int nrInputPorts, final int nrOutputPorts) {
        super(moduleInfo, nrInputPorts, nrOutputPorts);
        m_moduleItemConfigs = new LinkedList<ModuleItemConfig>();

        for (final ModuleItem<?> item : m_moduleInfo.inputs()) {
            final IJInputAdapter inputAdapter = IJAdapterProvider.getInputAdapter(item.getType());
            if (inputAdapter != null) {
                m_moduleItemConfigs.add(inputAdapter.createModuleItemConfig(item));

                if (inputAdapter instanceof IJStandardInputAdapter) {
                    final ModuleItemDataValueConfig config =
                            ((IJStandardInputAdapter)inputAdapter).createModuleItemConfig(item);
                    addColumnSelectionSettingsModel(config);
                }
            }
        }
    }

    /**
     * tests the provided inSpecs and configures the node with the columns if possible. Else asks for further user
     * interaction, e.g. to set values for required parameters.
     *
     * {@inheritDoc}
     *
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        if (inSpecs.length > 0) {
            // may throw an invalid settings exception
            testModuleConfiguration(inSpecs[0]);
            testColumnSelectionSettings(inSpecs[0]);
        }

        ////////////////
        StandardIJCellFactory cellFac;
        if (inSpecs.length > 0) {
            cellFac =
                    new StandardIJCellFactory(m_moduleInfo, m_imageJDlGSettingsModel, m_moduleItemConfigs,
                            createColumnSelectionIdentifier2IDMapping(inSpecs[0]), null);
        } else {
            cellFac =
                    new StandardIJCellFactory(m_moduleInfo, m_imageJDlGSettingsModel, m_moduleItemConfigs,
                            new HashMap<String, Integer>(), null);
        }

        if (m_appendColumns.getBooleanValue() && (inSpecs.length > 0)) {
            final ColumnRearranger rearranger = new ColumnRearranger(inSpecs[0]);
            rearranger.append(cellFac);
            return new DataTableSpec[]{rearranger.createSpec()};

        } else {
            return new DataTableSpec[]{new DataTableSpec(cellFac.getColumnSpecs())};
        }

    }

    /**
     * creates the result table either a new table or by appending the result columns
     */
    @Override
    protected BufferedDataTable[] createResultTable(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws CanceledExecutionException {

        StandardIJCellFactory cellFac;
        if (inData.length > 0) {
            cellFac =
                    new StandardIJCellFactory(m_moduleInfo, m_imageJDlGSettingsModel, m_moduleItemConfigs,
                            createColumnSelectionIdentifier2IDMapping(inData[0].getDataTableSpec()), exec);
        } else {
            cellFac =
                    new StandardIJCellFactory(m_moduleInfo, m_imageJDlGSettingsModel, m_moduleItemConfigs,
                            new HashMap<String, Integer>(), exec);
        }

        if (inData.length == 0) {
            final BufferedDataContainer con = exec.createDataContainer(new DataTableSpec(cellFac.getColumnSpecs()));

            final RowKey key = new RowKey("R1");
            con.addRowToTable(new DefaultRow(key, cellFac.getCells(null)));

            cellFac.setProgress(1, 1, key, exec);

            con.close();
            return new BufferedDataTable[]{con.getTable()};
        } else if (m_appendColumns.getBooleanValue()) {

            final ColumnRearranger rearranger = new ColumnRearranger(inData[0].getDataTableSpec());
            rearranger.append(cellFac);

            // create output
            final BufferedDataTable[] ret = {exec.createColumnRearrangeTable(inData[0], rearranger, exec)};

            // test for errors
            if (cellFac.getMissingCellCount() > 0) {
                setWarningMessage(cellFac.getMissingCellCount() + " cells could not be created");
            }

            return ret;
        } else {
            final BufferedDataContainer con = exec.createDataContainer(new DataTableSpec(cellFac.getColumnSpecs()));
            final RowIterator rowIt = inData[0].iterator();
            final int rowCount = inData[0].getRowCount();
            int curRowNr = 0;

            while (rowIt.hasNext()) {
                final DataRow row = rowIt.next();
                con.addRowToTable(new DefaultRow(row.getKey(), cellFac.getCells(row)));

                cellFac.setProgress(curRowNr++, rowCount, row.getKey(), exec);

            }

            if (cellFac.getMissingCellCount() > 0) {
                setWarningMessage(cellFac.getMissingCellCount() + " cells could not be created");
            }
            con.close();

            return new BufferedDataTable[]{con.getTable()};
        }
    }

    @Override
    protected List<ModuleItemConfig> getModuleItemConfigs() {
        return m_moduleItemConfigs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_appendColumns.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_appendColumns.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_appendColumns.loadSettingsFrom(settings);
    }

}
