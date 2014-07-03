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
package org.knime.knip.imagej2.core.adapter.impl.basicinput;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.imagej2.core.adapter.DialogComponentGroup;
import org.knime.knip.imagej2.core.adapter.DialogComponentGroup.PLACEMENT_HINT;
import org.knime.knip.imagej2.core.adapter.IJAdvancedInputAdapter;
import org.knime.knip.imagej2.core.adapter.ModuleItemRowConfig;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;

/**
 * Encapsulates the main functionality of basic input adapters. Provides two DialogComponents, a checkbox that activates
 * the column binding of a parameter and a column selection component to select an appropriate column
 * 
 * 
 * @param <IJ_OBJ>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractBasicInputAdapter<IJ_OBJ> implements IJAdvancedInputAdapter<IJ_OBJ> {

    /** by default column binding for basic input types is not active. */
    protected final boolean BINDING_ACTIVE_DEFAULT = false;

    /**
     * helper method to retrieve a type dependent infix for the settings string.
     * 
     * @return a type dependent infix
     */
    protected abstract String getSettingsNameInfix();

    /**
     * helper method to retrieve the class of the DataValue that is used by the adapter.
     * 
     * @return class of the DataValue, that is used by the adapter
     */
    protected abstract Class<? extends DataValue> getDataValueClass();

    /**
     * Configuration of the parameters.
     * 
     * @param module module to be configured
     * @param row row in table
     * @param item dialog item used
     * @param columnIndex index of column of interest
     * 
     */
    protected abstract void configModuleItem(Module module, DataRow row, ModuleItem item, int columnIndex);

    /**
     * creates a column selection and a checkbox that activates/deactivates it.
     * 
     * {@inheritDoc}
     */
    @Override
    public DialogComponentGroup createDialogComponents(final ModuleItem<IJ_OBJ> item) {
        final DialogComponent[] components = new DialogComponent[2];
        final SettingsModelString columnModel = createColumnModel(item.getName());

        components[0] = new DialogComponentColumnNameSelection(columnModel, "", 0, false, getDataValueClass());
        components[1] = new DialogComponentBoolean(createActivationModel(item.getName(), columnModel), "activate");

        return new DialogComponentGroup(components, PLACEMENT_HINT.HORIZ_WRAP_2, "binding to \"" + item.getLabel()
                + "\"");
    }

    @Override
    public ModuleItemRowConfig createModuleItemConfig(final ModuleItem item) {

        // create model components
        final SettingsModelString settingsModelColumn = createColumnModel(item.getName());
        final SettingsModelBoolean settingsModelActivation = createActivationModel(item.getName(), settingsModelColumn);

        final SettingsModel[] settingsModels = new SettingsModel[]{settingsModelColumn, settingsModelActivation};

        // create module item config
        return new AbstractModuleItemBasicInputConfig(settingsModels) {

            private int m_colIndex;

            @Override
            public boolean setDataTableSpec(final DataTableSpec inSpecs, final Module module)
                    throws InvalidSettingsException {
                m_colIndex =
                        NodeUtils.silentOptionalAutoColumnSelection(inSpecs, settingsModelColumn, getDataValueClass());

                return (m_colIndex != -1);
            }

            @Override
            protected void configureModuleItem(final Module module, final DataRow row) {
                if (isActive()) {
                    // actual configuration is done in the sub class
                    configModuleItem(module, row, item, m_colIndex);
                }
            }

            @Override
            public void resolveHandledModuleItems(final Module module, final boolean preTest) {
                if (isActive()) {
                    module.setResolved(item.getName(), true);
                }

            }

            @Override
            public ModuleItem<?> getItem() {
                return item;
            }

            /**
             * checks additionaly if a valid column is selected
             */
            @Override
            public boolean isActive() {
                if ((m_colIndex != -1) && settingsModelActivation.getBooleanValue()) {
                    return true;
                } else {
                    return false;
                }
            }

        };
    }

    /**
     * 
     * @param name becomes part of the settings model identifier
     * @return a column selection component
     */
    private SettingsModelString createColumnModel(final String name) {
        return new SettingsModelString("IJBasicAdapter" + getSettingsNameInfix() + "Col_" + name, "");
    }

    /**
     * create the settings model for the activate checkbox and bind it to the column selection such that the column
     * selection dialog component can be activated and deactivated by the user.
     * 
     * @param name becomes part of the settings model identifier
     * @param colModel the column model that should be activated and deactivated
     * @return a boolean settings model for a checkbox that activates / deactivates the columns selection
     */
    private SettingsModelBoolean createActivationModel(final String name, final SettingsModelString colModel) {

        final SettingsModelBoolean act =
                new SettingsModelBoolean("IJBasicAdapter" + getSettingsNameInfix() + "Active_" + name,
                        BINDING_ACTIVE_DEFAULT);

        act.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent arg0) {
                colModel.setEnabled(act.getBooleanValue());
            }
        });

        // ensure that listener and default value are equal
        colModel.setEnabled(BINDING_ACTIVE_DEFAULT);

        return act;
    }

}
