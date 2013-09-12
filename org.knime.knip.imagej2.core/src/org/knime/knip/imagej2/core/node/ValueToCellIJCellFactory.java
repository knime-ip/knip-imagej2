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

import imagej.module.Module;
import imagej.module.ModuleInfo;
import imagej.module.ModuleItem;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.ExecutionContext;
import org.knime.knip.imagej2.core.adapter.IJAdapterProvider;
import org.knime.knip.imagej2.core.adapter.IJOutputAdapter;
import org.knime.knip.imagej2.core.adapter.ModuleItemConfig;
import org.knime.knip.imagej2.core.adapter.ModuleItemDataValueConfig;
import org.knime.knip.imagej2.core.imagejdialog.SettingsModelImageJDlg;

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
public class ValueToCellIJCellFactory extends AbstractIJCellFactory {

    private final ModuleInfo m_moduleInfo;

    //TODO remove the lock if the concurrency error in DefaultAutoscaleService gets fixed
    private static Integer lock = new Integer(4);

    /**
     * indices of the columns that have been selected as input values (as the one input value process one after the
     * other).
     */
    private final int[] m_selectedColIndices;

    /**
     * the names that should be used for the result columns (same order as the selected column indices)
     */
    private final String[] m_colNames;

    /**
     * settings model that contains the values for basic ImageJ dialog parameters.
     */
    private final SettingsModelImageJDlg m_dialogModuleSettings;

    /**
     * {@link ModuleItemConfig} (similar to a settings model) of the one module input.
     */
    private final ModuleItemDataValueConfig m_valueConfig;

    /**
     * {@link ModuleItemConfig}s due to the requirements for input adapters this can only be components from the column
     * binding tab and the value config
     */
    private final List<ModuleItemConfig> m_moduleItemConfigs;

    /**
     * There is one ImageJ input adapter that can be configured with valueConfig. The module can be used to process
     * multiple columns (multiple executions per row) in this case the input adapter is configured multiple times. The
     * selectedColIndices provide the indices of the input columns and colNames provides (in the same order) the names
     * of the resulting columns. Additionally ImageJDialog settings can be resolved a.) with the ImageJ dialog settings
     * or b.) with moduleItemConfigs from the column binding tab.
     * 
     * @param moduleInfo
     * @param dialogModuleSettings {@link #m_dialogModuleSettings}
     * @param moduleItemConfigs {@link #m_moduleItemConfigs}
     * @param valueConfig {@link #m_valueConfig}
     * @param selectedColIndices {@link #m_selectedColIndices}
     * @param colNames {@link #m_colNames}
     * @param exec
     */
    public ValueToCellIJCellFactory(final ModuleInfo moduleInfo, final SettingsModelImageJDlg dialogModuleSettings,
                                    final List<ModuleItemConfig> moduleItemConfigs,
                                    final ModuleItemDataValueConfig valueConfig, final int[] selectedColIndices,
                                    final String[] colNames, final ExecutionContext exec) {
        super(exec);
        m_moduleInfo = moduleInfo;
        m_dialogModuleSettings = dialogModuleSettings;
        m_moduleItemConfigs = moduleItemConfigs;
        m_valueConfig = valueConfig;
        m_selectedColIndices = selectedColIndices;
        m_colNames = colNames;
    }

    /**
     * creates a (multiple) new module(s) for the processed row, configures it and sets all required DataValues then
     * executes the module. The {@link #m_valueConfig} is configured using the DataValues from the
     * {@link #m_selectedColIndices}. Additionally basic ImageJ dialog parameters can be resolved.
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        final List<DataCell> resCells = new ArrayList<DataCell>();
        for (int i = 0; i < m_selectedColIndices.length; i++) {
            if (row.getCell(m_selectedColIndices[i]).isMissing()) {
                resCells.add(DataType.getMissingCell());
            } else {
                final Module module =
                        AbstractIJNodeModel.createDialogConfiguredModule(m_moduleInfo, m_dialogModuleSettings);
                //data value config
                m_valueConfig.setConfigurationData(new DataValue[]{row.getCell(m_selectedColIndices[i])});
                m_valueConfig.resolveHandledModuleItems(module, false);

                synchronized (lock) {
                    m_valueConfig.configureModuleItem(module);
                }

                //remaining config only for row configs => column binding tab yes
                //valueConfig no
                configureRowConfigItems(row, module, m_moduleItemConfigs);

                //execute - and add one data cell per iteration
                resCells.add(executeRowModule(module).get(0));
            }
        }

        return resCells.toArray(new DataCell[resCells.size()]);
    }

    @Override
    public DataColumnSpec[] getColumnSpecs() {
        final DataColumnSpec[] colSpec = new DataColumnSpec[m_selectedColIndices.length];
        final ModuleItem<?> item = m_moduleInfo.outputs().iterator().next();
        final IJOutputAdapter<?> outputAdapter = IJAdapterProvider.getOutputAdapter(item.getType());

        final DataType dataType = outputAdapter.getDataTypes()[0];

        for (int i = 0; i < m_selectedColIndices.length; i++) {
            colSpec[i] = new DataColumnSpecCreator(m_colNames[i], dataType).createSpec();
        }

        return colSpec;
    }

}
