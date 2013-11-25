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
package org.knime.knip.imagej2.core.adapter;

import imagej.module.Module;
import imagej.module.ModuleItem;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.knip.imagej2.core.imagejdialog.DialogComponentImageJDlg;
import org.knime.knip.imagej2.core.node.AbstractIJNodeModel;

/**
 *
 * Supports the binding of row values to {@link ModuleItem}s and provides a basic implementation to make settings like
 * column selections persistent.
 *
 * ModuleItemColumnConfigs connect the {@link AbstractIJNodeModel} and the {@link DialogComponentImageJDlg} in the same way as
 * {@link SettingsModel}s.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public interface ModuleItemRowConfig extends PersistentModuleItemConfig {

    /**
     * @param row sets the data row that is used to configure the module when
     *            {@link ModuleItemConfig#configureModuleItem(Module)} is called
     */
    public void setConfigurationData(DataRow row);

    /**
     * sets the inSpecs to allow the selection of columns from the set of columns available at the input port. A typical
     * implementation would e.g. trigger a column auto selection to bind columns to the {@link ModuleItem} /s that will
     * be configured
     *
     * @param inSpecs specification of the table at the input port
     * @param module that should be configured
     * @return true if a column could be set that can be used to configure the {@link ModuleItem}s that are supported by
     *         this ModuleItemColumnConfig.
     * @throws InvalidSettingsException
     */
    public boolean setDataTableSpec(DataTableSpec inSpecs, Module module) throws InvalidSettingsException;

}
