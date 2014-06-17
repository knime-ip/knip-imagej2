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

import java.util.LinkedList;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.knip.imagej2.core.IJGateway;
import org.knime.knip.imagej2.core.adapter.DialogComponentGroup;
import org.knime.knip.imagej2.core.adapter.IJAdapterProvider;
import org.knime.knip.imagej2.core.adapter.IJAdvancedInputAdapter;
import org.knime.knip.imagej2.core.adapter.IJInputAdapter;
import org.knime.knip.imagej2.core.adapter.IJStandardInputAdapter;
import org.knime.knip.imagej2.core.adapter.ModuleItemDataValueConfig;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;

/**
 * Standard implementation that can be used for all modules (in contrast to the ValueToCell implementation)<br>
 * <br>
 * Creates a standard module dialog that displays the ImageJ dialog (if one exists), allows the binding of column values
 * to ImageJ dialog settings and resolves additional parameters (e.g. input image) with column bindings. Supports
 * appending new results to the table and the creation of a new table.
 *
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class StandardIJNodeDialog extends AbstractIJNodeDialog {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(StandardIJNodeDialog.class);

    /**
     * Creates a {@link StandardIJNodeDialog}
     *
     * @param moduleInfo {@link ModuleInfo} which will be used to create the dialog panel
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public StandardIJNodeDialog(final ModuleInfo moduleInfo) {
        super(moduleInfo);

        boolean useOptionTab = false;
        if (hasNoneEmptyFreeImageJDialog()) {
            createNewGroup("ImageJ Dialog");
            addImageJDialogIfNoneEmpty();
            closeCurrentGroup();
            useOptionTab = true;
        }

        //add KNIME settings if additional input components exist
        final LinkedList<DialogComponentGroup> additionalInputDCGs = new LinkedList<DialogComponentGroup>();
        for (final ModuleItem<?> item : moduleInfo.inputs()) {
            final IJInputAdapter inputAdapter = IJAdapterProvider.getInputAdapter(item.getType());
            if (inputAdapter != null) {
                if (!(IJGateway.isIJDialogInputType(item.getType()))) {
                    //create/get the gui components for the input adapter
                    DialogComponentGroup group = null;
                    if (inputAdapter instanceof IJStandardInputAdapter) {
                        final ModuleItemDataValueConfig config =
                                ((IJStandardInputAdapter)inputAdapter).createModuleItemConfig(item);
                        group = createColumnSelectionDCG(config);
                    } else if (inputAdapter instanceof IJAdvancedInputAdapter) {
                        group = ((IJAdvancedInputAdapter)inputAdapter).createDialogComponents(item);
                    } else {
                        LOGGER.error("StandardIJNodeDialog: can not create dialog components for an adapter.");
                    }
                    additionalInputDCGs.add(group);
                }
            }
        }
        if (additionalInputDCGs.size() > 0) {
            useOptionTab = true;
            createNewGroup("KNIME Settings");
            for (final DialogComponentGroup dcg : additionalInputDCGs) {
                addComponents(dcg);
            }
            addDialogComponent(new DialogComponentBoolean(StandardIJNodeModel.createAppendColumnsModel(),
                    "Append columns?"));
            closeCurrentGroup();
        }

        if (!useOptionTab) {
            removeTab("Options");
        }

        //add the column bindings tab if necessary
        createColumnBindingTab(moduleInfo);
    }
}
