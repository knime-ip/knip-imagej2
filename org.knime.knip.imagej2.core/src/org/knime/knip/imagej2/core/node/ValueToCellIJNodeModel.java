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

import imagej.module.ModuleInfo;
import imagej.module.ModuleItem;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.imagej2.core.adapter.IJAdapterProvider;
import org.knime.knip.imagej2.core.adapter.IJInputAdapter;
import org.knime.knip.imagej2.core.adapter.IJStandardInputAdapter;
import org.knime.knip.imagej2.core.adapter.ModuleItemConfig;
import org.knime.knip.imagej2.core.adapter.ModuleItemDataValueConfig;

/**
 * Special implementation that uses ValueToCell GUI components in the dialog and allows to process multiple input
 * columns at once. Only applicable if certain conditions like only one ImageJ in and output... are fulfilled (see
 * {@link IJNodeFactory#useValueToCell()} for more details).
 *
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ValueToCellIJNodeModel extends AbstractIJNodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ValueToCellIJNodeModel.class);

    /**
     * column creation modes
     */
    public static final String[] COL_CREATION_MODES = new String[]{"New Table", "Append", "Replace"};

    /**
     * @return the node model for the column suffix
     */
    public static SettingsModelString createColSuffixNodeModel() {
        return new SettingsModelString("column_suffix", "");
    }

    /**
     * @return the settings model for the column creation mode
     */
    public static SettingsModelString createColCreationModeModel() {
        return new SettingsModelString("column_creation_mode", COL_CREATION_MODES[0]);
    }

    /**
     * @return settings model for the column selection
     */
    public static SettingsModelFilterString createColumnSelectionModel() {
        return new SettingsModelFilterString("column_selection");
    }

    /*
     * Settings for the column suffix.
     */
    private final SettingsModelString m_colSuffix = createColSuffixNodeModel();

    /*
     * Settings for the column creation mode
     */
    private final SettingsModelString m_colCreationMode = createColCreationModeModel();

    /*
     * Settings to store the selected columns.
     */
    private final SettingsModelFilterString m_columns = createColumnSelectionModel();

    /**
     * list of the module item configs collected from the used input adapters.
     */
    private final LinkedList<ModuleItemConfig> m_moduleItemConfigs;

    private ModuleItemDataValueConfig m_valueConfig;

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected ValueToCellIJNodeModel(final ModuleInfo moduleInfo, final int nrInDataPorts, final int nrOutDataPorts) {
        super(moduleInfo, nrInDataPorts, nrOutDataPorts);

        m_moduleItemConfigs = new LinkedList<ModuleItemConfig>();
        for (final ModuleItem<?> item : m_moduleInfo.inputs()) {
            final IJInputAdapter inputAdapter = IJAdapterProvider.getInputAdapter(item.getType());
            if (inputAdapter != null) {
                final ModuleItemConfig config = inputAdapter.createModuleItemConfig(item);
                m_moduleItemConfigs.add(config);

                if (inputAdapter instanceof IJStandardInputAdapter) {
                    m_valueConfig = (ModuleItemDataValueConfig)config;
                }
            }
        }
    }

    /**
     * creates a new table, appends the resulting columns or replaces the source columns. Supports the processing of
     * multiple columns at once.
     */
    @Override
    protected BufferedDataTable[] createResultTable(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws CanceledExecutionException {

        BufferedDataTable[] res;
        final BufferedDataTable inTable = inData[0];
        final int[] selectedColIndices = getSelectedColumnIndices(inTable.getDataTableSpec());
        final ValueToCellIJCellFactory cellFac =
                new ValueToCellIJCellFactory(m_moduleInfo, m_imageJDlGSettingsModel, m_moduleItemConfigs,
                        m_valueConfig, selectedColIndices, createSpecNames(inTable.getSpec()), exec);

        exec.setProgress("Processing ...");
        if (m_colCreationMode.getStringValue().equals(COL_CREATION_MODES[0])) {

            final RowIterator it = inTable.iterator();
            final BufferedDataContainer con = exec.createDataContainer(new DataTableSpec(cellFac.getColumnSpecs()));
            DataRow row;
            final int rowCount = inTable.getRowCount();
            int i = 0;

            while (it.hasNext()) {
                row = it.next();
                con.addRowToTable(new DefaultRow(row.getKey(), cellFac.getCells(row)));
                exec.checkCanceled();
                cellFac.setProgress(i, rowCount, row.getKey(), exec);
                i++;
            }
            con.close();
            res = new BufferedDataTable[]{con.getTable()};
        } else {
            final ColumnRearranger colRearranger = new ColumnRearranger(inTable.getDataTableSpec());
            if (m_colCreationMode.getStringValue().equals(COL_CREATION_MODES[1])) {
                colRearranger.append(cellFac);
            } else {
                colRearranger.replace(cellFac, selectedColIndices);
            }

            res = new BufferedDataTable[]{exec.createColumnRearrangeTable(inTable, colRearranger, exec)};
        }

        if (cellFac.getMissingCellCount() > 0) {
            setWarningMessage(cellFac.getMissingCellCount() + " cells coud not be created!");
        }

        // data for the table cell view
        m_data = res[0];
        return res;
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
        }

        ////////
        final DataTableSpec inSpec = inSpecs[0];

        final int[] selectedColIndices = getSelectedColumnIndices(inSpec);

        if (selectedColIndices.length == 0) {
            throw new InvalidSettingsException("No columns of the required type "
                    + m_valueConfig.getGuiMetaInfo()[0].inValue.getSimpleName() + " coud be found.");
        }

        final CellFactory cellFac =
                new ValueToCellIJCellFactory(m_moduleInfo, m_imageJDlGSettingsModel, m_moduleItemConfigs,
                        m_valueConfig, selectedColIndices, createSpecNames(inSpec), null);

        if (m_colCreationMode.getStringValue().equals(COL_CREATION_MODES[0])) {
            return new DataTableSpec[]{new DataTableSpec(cellFac.getColumnSpecs())};
        } else if (m_colCreationMode.getStringValue().equals(COL_CREATION_MODES[1])) {
            final ColumnRearranger colRearranger = new ColumnRearranger(inSpec);
            colRearranger.append(cellFac);
            return new DataTableSpec[]{colRearranger.createSpec()};

        } else {
            final ColumnRearranger colRearranger = new ColumnRearranger(inSpec);
            colRearranger.replace(cellFac, selectedColIndices);
            return new DataTableSpec[]{colRearranger.createSpec()};
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_colSuffix.saveSettingsTo(settings);
        m_colCreationMode.saveSettingsTo(settings);
        m_columns.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_colSuffix.validateSettings(settings);
        m_colCreationMode.validateSettings(settings);
        m_columns.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_colSuffix.loadSettingsFrom(settings);
        m_colCreationMode.loadSettingsFrom(settings);
        m_columns.loadSettingsFrom(settings);
    }

    /*
     * Retrieves the selected column indices from the given DataTableSpec
     * and the column selection. If the selection turned out to be invalid,
     * all columns are selected.
     */
    private int[] getSelectedColumnIndices(final DataTableSpec inSpec) {
        final List<String> colNames;
        if (m_columns.getIncludeList().size() == 0) {
            colNames = new ArrayList<String>();
            collectAllColumns(colNames, inSpec);
            m_columns.setIncludeList(colNames);
        } else {
            colNames = new ArrayList<String>();
            colNames.addAll(m_columns.getIncludeList());
            if (!validateColumnSelection(colNames, inSpec)) {
                setWarningMessage("Invalid column selection. All columns are selected!");
                collectAllColumns(colNames, inSpec);
            }
        }

        // get column indices
        final List<Integer> colIndices = new ArrayList<Integer>(colNames.size());
        for (int i = 0; i < colNames.size(); i++) {
            final int colIdx = inSpec.findColumnIndex(colNames.get(i));
            if (colIdx == -1) {
                // can not occur, actually
                LOGGER.warn("Column " + colNames.get(i) + " doesn't exist!");
            } else {
                colIndices.add(colIdx);
            }
        }

        final int[] colIdx = new int[colIndices.size()];
        for (int i = 0; i < colIdx.length; i++) {
            colIdx[i] = colIndices.get(i);
        }

        return colIdx;

    }

    /**
     * Helper to collect all columns that are compatible with the type of the value config
     */
    private void collectAllColumns(final List<String> colNames, final DataTableSpec spec) {
        //indexing 0 is ok because more types cannot be used together with ValueToCell...
        final Class<? extends DataValue> dataType = m_valueConfig.getGuiMetaInfo()[0].inValue;

        colNames.clear();
        for (final DataColumnSpec c : spec) {
            if (c.getType().isCompatible(dataType)) {
                colNames.add(c.getName());
            }
        }
        if (colNames.size() == 0) {
            LOGGER.warn("No columns of type " + dataType.getSimpleName() + " available!");
            return;
        }
        LOGGER.info("All available columns of type " + dataType.getSimpleName() + " are selected!");

    }

    /** Checks if a column is not present in the DataTableSpec */
    private boolean validateColumnSelection(final List<String> colNames, final DataTableSpec spec) {
        for (int i = 0; i < colNames.size(); i++) {
            final int colIdx = spec.findColumnIndex(colNames.get(i));
            if (colIdx == -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * creates column names for the selected column indices by appending the column suffix to the original column name.
     *
     * @param inSpec
     * @return column names for result columns
     */
    private String[] createSpecNames(final DataTableSpec inSpec) {
        final int[] indices = getSelectedColumnIndices(inSpec);
        final String[] ret = new String[indices.length];

        for (int i = 0; i < indices.length; i++) {
            final DataColumnSpec cs = inSpec.getColumnSpec(indices[i]);
            ret[i] = cs.getName() + m_colSuffix.getStringValue();
        }

        return ret;
    }

    @Override
    protected List<ModuleItemConfig> getModuleItemConfigs() {
        return m_moduleItemConfigs;
    }

}
