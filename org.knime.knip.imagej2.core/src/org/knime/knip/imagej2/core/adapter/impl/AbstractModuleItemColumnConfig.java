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
package org.knime.knip.imagej2.core.adapter.impl;

import org.knime.core.data.DataRow;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.knip.imagej2.core.adapter.ModuleItemRowConfig;
import org.scijava.module.Module;

/**
 * provides a basic implementation of a column based ModuleItemConfig.<br>
 * <br>
 * Includes a {@link SettingsModel} based default implementation of the persistence methods such that
 * {@link org.knime.core.node.defaultnodesettings.DialogComponent DialogComponents} can be used to add settings to the
 * node configuration dialog.
 *
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class AbstractModuleItemColumnConfig implements ModuleItemRowConfig {

    // take care of the settings models to allow save and load operations

    private final SettingsModel[] m_settingsModels;

    /**
     * creates a new ModuleItemColumnConfig that uses the provided SettingsModels to implement the persistence methods.
     * Therefore standard {@link org.knime.core.node.defaultnodesettings#DialogComponent DialogComponents} can be used
     * to implement the dialogs (e.g. select columns etc.) and their settings models will be used to achieve
     * persistence.
     *
     * @param settingsModels
     */
    public AbstractModuleItemColumnConfig(final SettingsModel[] settingsModels) {
        m_settingsModels = settingsModels;
    }

    // build a single convenience method that replaces setConfigurationData and
    // configureModuleItem and has some basic error protection

    /** helper item to store the current data row. */
    private DataRow m_currentDataRow = null;

    @Override
    public void setConfigurationData(final DataRow row) {
        m_currentDataRow = row;
    }

    @Override
    public void configureModuleItem(final Module module) {
        if (m_currentDataRow != null) {
            configureModuleItem(module, m_currentDataRow);
        }
    }

    /**
     * Configures parts of a module (typically one ModuleItem) with data from the provided DataRow <br>
     * <br>
     * convenient method that unifies the calls to {@link AbstractModuleItemColumnConfig#setConfigurationData(DataRow)
     * setConfigurationData} and {@link AbstractModuleItemColumnConfig#configureModuleItem(Module) configureModule} in
     * one method.
     *
     * @param module
     * @param row the actual data row that is used for configuration
     */
    protected abstract void configureModuleItem(Module module, DataRow row);

    // basic persistence support

    @Override
    public final void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (final SettingsModel settingsModel : m_settingsModels) {
            settingsModel.validateSettings(settings);
        }
    }

    @Override
    public final void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (final SettingsModel settingsModel : m_settingsModels) {
            settingsModel.loadSettingsFrom(settings);
        }
    }

    @Override
    public final void saveSettingsTo(final NodeSettingsWO settings) {
        for (final SettingsModel settingsModel : m_settingsModels) {
            settingsModel.saveSettingsTo(settings);
        }
    }
}
