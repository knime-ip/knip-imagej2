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

import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.knip.imagej2.core.adapter.DataValueConfigGuiInfos;
import org.knime.knip.imagej2.core.adapter.IJAdapterProvider;
import org.knime.knip.imagej2.core.adapter.IJInputAdapter;
import org.knime.knip.imagej2.core.adapter.IJStandardInputAdapter;

/**
 * Special implementation that uses ValueToCell GUI components in the dialog and allows to process multiple input
 * columns at once. Only applicable if certain conditions like only one ImageJ in and output... are fulfilled (see
 * {@link IJNodeFactory#useValueToCell()} for more details). <br>
 * <br>
 * Supports the processing of multiple columns at once. Appending and replacing of result columns and the creation of a
 * new resul table.
 *
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ValueToCellIJNodeDialog extends AbstractIJNodeDialog {

    /**
     * Note that the constructor makes certain assumptions that should be tested with
     * {@link IJNodeFactory#useValueToCell()}.
     *
     * @param moduleInfo
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ValueToCellIJNodeDialog(final ModuleInfo moduleInfo) {
        super(moduleInfo);

        //add the ImageJ dialog component if it exists
        if (hasNoneEmptyFreeImageJDialog()) {
            createNewGroup("ImageJ Dialog");
            addImageJDialogIfNoneEmpty();
            closeCurrentGroup();
        } else {
            removeTab("Options");
        }

        //create selection tab
        DataValueConfigGuiInfos valueToCellInfos = null;
        for (final ModuleItem<?> item : moduleInfo.inputs()) {
            final IJInputAdapter inputAdapter = IJAdapterProvider.getInputAdapter(item.getType());
            if ((inputAdapter != null) && (inputAdapter instanceof IJStandardInputAdapter)) {
                valueToCellInfos =
                        ((IJStandardInputAdapter)inputAdapter).createModuleItemConfig(item).getGuiMetaInfo()[0];
                break;
            }
        }
        addSelectionDCs(valueToCellInfos);

        //add the column bindings tab if necessary
        createColumnBindingTab(moduleInfo);
    }

    @SuppressWarnings({"unchecked"})
    private void addSelectionDCs(final DataValueConfigGuiInfos valueToCellInfos) {
        createNewTab("Column Selection");

        createNewGroup("Creation Mode");
        addDialogComponent(new DialogComponentStringSelection(ValueToCellIJNodeModel.createColCreationModeModel(),
                "Column Creation Mode", ValueToCellIJNodeModel.COL_CREATION_MODES));

        createNewGroup("Column suffix");
        addDialogComponent(new DialogComponentString(ValueToCellIJNodeModel.createColSuffixNodeModel(), "Column suffix"));

        createNewGroup(valueToCellInfos.label + " column selection");
        addDialogComponent(new DialogComponentColumnFilter(ValueToCellIJNodeModel.createColumnSelectionModel(), 0,
                false, valueToCellInfos.inValue));
    }
}
