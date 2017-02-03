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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.imagej2.core.IJGateway;
import org.knime.knip.imagej2.core.adapter.DataValueConfigGuiInfos;
import org.knime.knip.imagej2.core.adapter.ModuleItemConfig;
import org.knime.knip.imagej2.core.adapter.ModuleItemDataValueConfig;
import org.knime.knip.imagej2.core.adapter.ModuleItemRowConfig;
import org.knime.knip.imagej2.core.adapter.PersistentModuleItemConfig;
import org.knime.knip.imagej2.core.adapter.impl.basicinput.AbstractModuleItemBasicInputConfig;
import org.knime.knip.imagej2.core.imagejdialog.DialogComponentImageJDlg;
import org.knime.knip.imagej2.core.imagejdialog.SettingsModelImageJDlg;
import org.scijava.Context;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;
import org.scijava.service.Service;

/**
 * Provides a basic set of methods that are common to all IJNodeModels:
 *
 *
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class AbstractIJNodeModel extends NodeModel implements BufferedDataTableHolder {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AbstractIJNodeModel.class);

    /**
     * @return the settings model for the ImageJ dialog.
     */
    static SettingsModelImageJDlg createImageJDlgModel() {
        return new SettingsModelImageJDlg("imagej_dlg_settings_model");
    }

    /**
     * @param identifier
     * @return a settings model for a column selection combo box. Should be used in combination with
     *         {@link AbstractIJNodeDialog#createColumnSelectionDCG(ModuleItemDataValueConfig)}
     */
    static SettingsModelString createColumnSelectionStringSM(final String identifier) {
        return new SettingsModelString("ItemValueConfig_" + identifier, "");
    }

    /**
     * needed to implement TableCellViewProvider data table for the table cell
     * view
     */
    protected BufferedDataTable m_data;

    //might be paired with a dummy settings model if no ImageJ dialog exists
    /** settings model of the ImageJ dialog. */
    protected SettingsModelImageJDlg m_imageJDlGSettingsModel;

    /**
     * settings models of column selection combo boxes. The key string has to be the unique identifier of the ModuleItem
     * parameter.
     */
    protected LinkedHashMap<String, SettingsModelString> m_columnSelectionSettingsModels;

    /**
     * stores DataValue types associated with the unique identifiers of ModuleItem parameters.
     */
    protected HashMap<String, Class<? extends DataValue>> m_columnSelectionDataValues;

    /**
     * {@link ModuleInfo} of the wrapped {@link Module}
     */
    protected final ModuleInfo m_moduleInfo;

    //constructor

    /**
     * @param moduleInfo {@link ModuleInfo} of the wrapped {@link Module}
     * @param nrInDataPorts number of inports
     * @param nrOutDataPorts number of out ports
     */
    protected AbstractIJNodeModel(final ModuleInfo moduleInfo, final int nrInDataPorts, final int nrOutDataPorts) {
        super(nrInDataPorts, nrOutDataPorts);
        m_moduleInfo = moduleInfo;
        m_columnSelectionSettingsModels = new LinkedHashMap<String, SettingsModelString>();
        m_columnSelectionDataValues = new HashMap<String, Class<? extends DataValue>>();
        m_imageJDlGSettingsModel = createImageJDlgModel();
    }

    /**
     * creates the actual data table this method is called from {@link #execute(BufferedDataTable[], ExecutionContext)}.
     *
     * @param inData
     * @param exec
     * @return outgoing {@link BufferedDataTable}
     * @throws CanceledExecutionException
     */
    protected abstract BufferedDataTable[] createResultTable(final BufferedDataTable[] inData,
                                                             final ExecutionContext exec)
            throws CanceledExecutionException;

    /**
     * @return a list of all module item configs the node model uses. This is e.g. used to test the module status
     *         (everything resolved).
     */
    protected abstract List<ModuleItemConfig> getModuleItemConfigs();

    //column selection combo boxes

    /**
     * Adds settings models for column binding combo boxes to the node model. The settings models fit the data values
     * that are required by the {@link ModuleItemDataValueConfig}. This methods should be used in combination with the
     * {@link AbstractIJNodeDialog#createColumnSelectionDCG(ModuleItemDataValueConfig)} method.
     *
     * @param config
     */
    protected void addColumnSelectionSettingsModel(final ModuleItemDataValueConfig config) {
        final DataValueConfigGuiInfos[] infos = config.getGuiMetaInfo();
        for (final DataValueConfigGuiInfos guiInfo : infos) {
            final String identifier = guiInfo.identifier;
            m_columnSelectionDataValues.put(identifier, guiInfo.inValue);
            m_columnSelectionSettingsModels.put(identifier, createColumnSelectionStringSM(identifier));
        }
    }

    /**
     * convenience method that tests all settings models of column binding combo boxes against the in spec and tries to
     * configure them with auto column selection if necessary.
     *
     * @param inSpec
     * @throws InvalidSettingsException is thrown when no appropriate column could be found
     */
    protected void testColumnSelectionSettings(final DataTableSpec inSpec) throws InvalidSettingsException {
        for (final String identifier : m_columnSelectionSettingsModels.keySet()) {
            final SettingsModelString sms = m_columnSelectionSettingsModels.get(identifier);
            final DataColumnSpec spec = inSpec.getColumnSpec(sms.getStringValue());
            if (spec != null) {
                if (!spec.getType().isCompatible(m_columnSelectionDataValues.get(identifier))) {
                    throw new InvalidSettingsException("Column selection for " + identifier
                            + " is invalid (incompatible type)");
                }
            } else {
                NodeUtils.autoColumnSelection(inSpec, sms, m_columnSelectionDataValues.get(identifier),
                                              AbstractIJNodeModel.class);
            }
        }
    }

    /**
     * creates a map from unique parameter identifier to column index for all settings models of column binding combo
     * boxes. This map allows to retrieve selected column for an input parameter and allows thus the per row
     * configuration with DataValues.
     *
     * @param inSpec
     * @return a mapping from unique parameter identifiers to the indices of the columns that are bound to the
     *         parameters
     */
    protected HashMap<String, Integer> createColumnSelectionIdentifier2IDMapping(final DataTableSpec inSpec) {
        final HashMap<String, Integer> ret = new HashMap<String, Integer>();

        for (final String key : m_columnSelectionSettingsModels.keySet()) {
            final int colId = inSpec.findColumnIndex(m_columnSelectionSettingsModels.get(key).getStringValue());
            ret.put(key, colId);
        }

        return ret;
    }

    //

    /**
     * convenience implementation of the execute method performs some basic checks and that may throw
     * {@link InvalidSettingsException} and prints some warnings if column binding is used. Also handels the standard
     * view. The actual creation of the resulting table uses
     * {@link #createResultTable(BufferedDataTable[], ExecutionContext)} and is implemented in child classes.
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        if (inData.length > 0) {
            // may throw and invalid settings exception
            testModuleConfiguration(inData[0].getDataTableSpec());
            testModuleConfiguration(inData[0].getDataTableSpec());

            // warn if a ImageJ Dialog parameter is bound to a column
            for (final ModuleItemConfig itemConfig : getModuleItemConfigs()) {
                if ((itemConfig instanceof AbstractModuleItemBasicInputConfig)
                        && ((AbstractModuleItemBasicInputConfig)itemConfig).isActive()) {
                    LOGGER.warn("an ImageJ Dialog parameter is resolved via column binding" + "\n"
                            + "and parameter restrictions (min, max, even, ...) can not be checked");
                }
            }
        }

        // ///////////////
        final BufferedDataTable[] result = createResultTable(inData, exec);

        // data for the table cell view
        m_data = result[0];

        return result;
    }

    /**
     * tests if the provided inSpecs fulfill the requirements of the ImageJ plugin and if further configuration via the
     * KNIME node configuration dialog are required.
     *
     * @param inSpec
     * @throws InvalidSettingsException
     */
    protected void testModuleConfiguration(final DataTableSpec inSpec) throws InvalidSettingsException {
        // configure dialog
        final Module module = createDialogConfiguredModule(m_moduleInfo, m_imageJDlGSettingsModel);

        //resolve to default for standard values
        resolveToDefault(module, inSpec);

        // resolve in specs
        resolveConfigs(module, inSpec);

        testModule(module);
    }

    /**
     * @param module
     * @param inSpec
     */
    private void resolveToDefault(final Module module, final DataTableSpec inSpec) {
        for (final ModuleItem<?> item : m_moduleInfo.inputs()) {
            if (!((Service.class).isAssignableFrom(item.getType()))
                    && !((Context.class).isAssignableFrom(item.getType()))) {
                //not a service
                if (module.getInput(item.getName()) != null) {
                    //set default value
                    module.setResolved(item.getName(), true);
                }
            }
        }
    }

    /**
     * Tries to set the status of ModuleItems from {@link #getModuleItemConfigs()} to resolved. Supports
     * {@link ModuleItemRowConfig} and {@link ModuleItemDataValueConfig}. The module item configs are called in preTest
     * mode => the data doesn't have to be set in order to get resolved status but configuration information like table
     * specs.
     *
     * @param module
     * @param inSpec
     * @throws InvalidSettingsException
     */
    protected void resolveConfigs(final Module module, final DataTableSpec inSpec) throws InvalidSettingsException {
        for (final ModuleItemConfig itemConfig : getModuleItemConfigs()) {
            if (itemConfig instanceof ModuleItemRowConfig) {
                ((ModuleItemRowConfig)itemConfig).setDataTableSpec(inSpec, module);
                ((ModuleItemRowConfig)itemConfig).resolveHandledModuleItems(module, true);
            } else if (itemConfig instanceof ModuleItemDataValueConfig) {
                ((ModuleItemDataValueConfig)itemConfig).resolveHandledModuleItems(module, true);
            }
        }
    }

    // also needed by IJCellFactory
    /**
     * creates a Module based on the provided moduleInfo and sets values for the ModuleItems that can be resolved with
     * the provided dialogModuleSettings. (These are the ModuleItems that can be set in the associated
     * {@link DialogComponentImageJDlg})
     *
     * @param moduleInfo specification of the module that should be returned
     * @param dialogModuleSettings SettingsModel of a {@link DialogComponentImageJDlg}
     * @return a partially configured Module that is created based on the provided ModuleInfo and has been configured
     *         with all basic input parameters that are handled by the {@link DialogComponentImageJDlg} of the provided
     *         SettingsModel
     */
    static Module createDialogConfiguredModule(final ModuleInfo moduleInfo,
                                               final SettingsModelImageJDlg dialogModuleSettings) {

        Module module = null;
        try {
            module = moduleInfo.createModule();
            // inject context
            IJGateway.getImageJContext().inject(module);

            // load the dialog matched settings with the dialog settings
            dialogModuleSettings.configureModule(module);
        } catch (final ModuleException e) {
            e.printStackTrace();
        }

        return module;
    }

    /**
     * Tests for the specified module if all required parameters are set to resolved (except for services)
     *
     * @param module
     * @throws InvalidSettingsException if one or more parameters could not be resolved
     *             {@link #resolveConfigs(Module, DataTableSpec)}
     */
    protected void testModule(final Module module) throws InvalidSettingsException {
        for (final ModuleItem<?> item : m_moduleInfo.inputs()) {
            if (item.isRequired() && !module.isInputResolved(item.getName())) {
                if (!((Service.class).isAssignableFrom(item.getType()))
                        && !(Context.class.isAssignableFrom(item.getType()))) {
                    // services are not resolved
                    throw new InvalidSettingsException(
                            "Configuration required: at least one required input (" + item.getName() + ") is not set.");
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_imageJDlGSettingsModel.saveSettingsTo(settings);

        //save persistent ModuleItemConfigs
        for (final ModuleItemConfig itemConfig : getModuleItemConfigs()) {
            if (itemConfig instanceof PersistentModuleItemConfig) {
                ((PersistentModuleItemConfig)itemConfig).saveSettingsTo(settings);
            }
        }

        //save column selections of ModuleItemDataValueConfig
        for (final SettingsModelString sms : m_columnSelectionSettingsModels.values()) {
            sms.saveSettingsTo(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_imageJDlGSettingsModel.validateSettings(settings);

        //validate persistent ModuleItemConfigs
        for (final ModuleItemConfig itemConfig : getModuleItemConfigs()) {
            if (itemConfig instanceof PersistentModuleItemConfig) {
                ((PersistentModuleItemConfig)itemConfig).validateSettings(settings);
            }
        }

        //validate column selections of ModuleItemDataValueConfig
        for (final SettingsModelString sms : m_columnSelectionSettingsModels.values()) {
            sms.validateSettings(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_imageJDlGSettingsModel.loadSettingsFrom(settings);

        //load persistent ModuleItemConfigs
        for (final ModuleItemConfig itemConfig : getModuleItemConfigs()) {
            if (itemConfig instanceof PersistentModuleItemConfig) {
                ((PersistentModuleItemConfig)itemConfig).loadSettingsFrom(settings);
            }
        }

        //load column selections of ModuleItemDataValueConfig
        for (final SettingsModelString sms : m_columnSelectionSettingsModels.values()) {
            sms.loadSettingsFrom(settings);
        }
    }

    // methods that are only needed if a table cell viewer is used

    @Override
    protected void reset() {
        m_data = null;
    }

    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{m_data};
    }

    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_data = tables[0];
    }

}
