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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.node.ExecutionContext;
import org.knime.knip.imagej2.core.adapter.IJAdapterProvider;
import org.knime.knip.imagej2.core.adapter.IJOutputAdapter;
import org.knime.knip.imagej2.core.adapter.ModuleItemConfig;
import org.knime.knip.imagej2.core.adapter.ModuleItemDataValueConfig;
import org.knime.knip.imagej2.core.adapter.ModuleItemRowConfig;
import org.knime.knip.imagej2.core.imagejdialog.SettingsModelImageJDlg;
import org.scijava.module.Module;
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
public class StandardIJCellFactory extends AbstractIJCellFactory {

    private final ModuleInfo m_moduleInfo;

    /**
     * settings model that contains the values for basic ImageJ dialog parameters.
     */
    private final SettingsModelImageJDlg m_dialogModuleSettings;

    /**
     * List of all {@link ModuleItemConfig} (similar to a SettingsModel) together with the dialog settings model these
     * objects cover the configuration of all module items (parameters) of the module.
     */
    private final List<ModuleItemConfig> m_moduleItemConfigs;

    /**
     * maps the unique parameter identifiers of {@link ModuleItemDataValueConfig}s to cell ids.
     */
    private final HashMap<String, Integer> m_identifier2CellID;

    /**
     * TODO
     *
     * @param moduleInfo
     * @param dialogModuleSettings
     * @param moduleItemConfigs
     * @param identifier2CellID
     * @param exec
     */
    public StandardIJCellFactory(final ModuleInfo moduleInfo, final SettingsModelImageJDlg dialogModuleSettings,
                                 final List<ModuleItemConfig> moduleItemConfigs,
                                 final HashMap<String, Integer> identifier2CellID, final ExecutionContext exec) {
        super(exec);
        m_moduleInfo = moduleInfo;
        m_dialogModuleSettings = dialogModuleSettings;
        m_moduleItemConfigs = moduleItemConfigs;
        m_identifier2CellID = identifier2CellID;
    }

    /**
     * creates a new module for the processed row, configures it and sets all required DataValues the executes the
     * module. Supports {@link ModuleItemRowConfig} and {@link ModuleItemDataValueConfig} (uses the identifier2CellID
     * mapping for configuration of the data values)
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        final Module module = AbstractIJNodeModel.createDialogConfiguredModule(m_moduleInfo, m_dialogModuleSettings);

        configureRowConfigItems(row, module, m_moduleItemConfigs);
        configureDataValueConfigItems(row, module, m_moduleItemConfigs, m_identifier2CellID);

        //execute the configured plugin
        final List<DataCell> resCells = executeRowModule(module);
        return resCells.toArray(new DataCell[resCells.size()]);
    }

    @Override
    public DataColumnSpec[] getColumnSpecs() {
        final List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();
        for (final ModuleItem<?> item : m_moduleInfo.outputs()) {

            final IJOutputAdapter<?> outputAdapter = IJAdapterProvider.getOutputAdapter(item.getType());

            final DataType[] dataTypes = outputAdapter.getDataTypes();

            int i = 1;
            for (final DataType type : dataTypes) {
                final String appendix = (i > 1) ? String.valueOf(i) : "";
                colSpecs.add(new DataColumnSpecCreator(item.getName() + appendix, type).createSpec());
                i++;
            }
        }
        return colSpecs.toArray(new DataColumnSpec[colSpecs.size()]);
    }

}
