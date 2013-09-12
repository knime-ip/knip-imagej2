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
package org.knime.knip.imagej2.base.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.knime.knip.imagej2.base.IMAGEJ_BASE_Plugin;

/**
 * contains the logic behind the plugin folder selection.
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class PluginFolderSelectionController {

    /**
     * opens a directory choose dialog and changes the 'plugins' folder path in the preference properties if the user
     * selects an appropriate folder. Also updates the pathLabel according to the result.
     * 
     * @param pathLabel
     * @param parentShell the parent needed to display dialogs
     * 
     */
    public void selectDirButtonPressed(final Label pathLabel, final Shell parentShell) {
        final DirectoryDialog dialog = new DirectoryDialog(parentShell, SWT.SHEET);
        dialog.setText("Choose the knime plugins directory");

        final String dir = dialog.open();
        if (dir != null) {
            if (IMAGEJ_BASE_Plugin.checkFolderPath(dir)) {
                // selected a valid path
                IMAGEJ_BASE_Plugin.getDefault().getPreferenceStore()
                        .setValue(IMAGEJ_BASE_Plugin.PLUGIN_FOLDER_PATH, dir);
                reload(pathLabel);
            } else {
                final MessageBox messageDialog = new MessageBox(parentShell, (SWT.ICON_WARNING | SWT.OK));
                messageDialog.setMessage("the selected directory is not a knime/plugins directory");
                messageDialog.open();
            }
        }
    }

    /**
     * @param pathLabel sets the label content to the 'plugins' folder path from the preference properties
     */
    public void reload(final Label pathLabel) {
        pathLabel.setText(IMAGEJ_BASE_Plugin.getEclipsePluginFolderPath());
    }

}
