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
package org.knime.knip.imagej2.core.preferences;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.knime.knip.imagej2.core.FragmentWrapperUtil;
import org.knime.knip.imagej2.core.KNIMEIMAGEJPlugin;

/**
 * contains the logic needed to add and remove ImageJ plugins (uses {@link FragmentWrapperUtil}).
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class PluginListController {

    /**
     * holds a representation of all managed plugins. This contains plugins that change their state. E.g. a plugin can
     * be in removal state and may therefore not be found in the plugin folder still it is important to know that the
     * plugin has been removed and the operation will be finished after the next restart. This information is stored
     * here. The variable may however be cleared e.g. if the user changes the path to the 'plugins' directory.
     */
    private static final HashMap<String, PluginState> m_pluginManager = new HashMap<String, PluginState>();

    /**
     * detects the presence of the fragment wrapper property file that indicates that a plugin in the plugins folder is
     * a wrapped ImageJ plugin.
     */
    private final FilenameFilter m_nameFilter = new FragmentWrapperFilenameFilter();

    /**
     * @param pluginList is filled with all ImageJ plugins from the plugins folder specified in the preference
     *            properties and all plugins in intermediate states like removing...
     */
    public void reload(final List pluginList) {
        pluginList.removeAll();

        // retrieve installed plugins
        final String[] activePluginNames = listInstalledPlugins();
        for (final String activePlugin : activePluginNames) {
            if (!m_pluginManager.containsKey(activePlugin)) {
                m_pluginManager.put(activePlugin, new PluginState(activePlugin, PluginState.STATE.ACTIVE));
            }
        }

        // add all to the list
        for (final String pluginKey : m_pluginManager.keySet()) {
            pluginList.add(m_pluginManager.get(pluginKey).toString());
        }
    }

    /**
     * resets the tracking of plugin changes e.g. if the plugin dir changes.
     */
    public static void resetPluginManagement() {
        m_pluginManager.clear();
    }

    /**
     * displays an file selection dialog to allow the users the selection of a local ImageJ plugin jar. If the
     * installation of the selected plugin succeeds adjusts the pluginList
     * 
     * @param pluginList
     * @param parentShell the parent needed to display dialogs
     * 
     * @return true if a plugin has beend added successfully
     */
    public boolean addButtonPressed(final List pluginList, final Shell parentShell) {
        boolean success = false;

        if (KNIMEIMAGEJPlugin.checkFolderPath(KNIMEIMAGEJPlugin.getEclipsePluginFolderPath())) {

            final File jarPath = selectImageJPluginJar(parentShell);
            if (jarPath != null) {
                // cut off .jar
                final String identifier = jarPath.getName().substring(0, jarPath.getName().length() - 4);

                if (m_pluginManager.containsKey(identifier)) {
                    final MessageBox messageDialog = new MessageBox(parentShell, (SWT.ICON_WARNING | SWT.OK));
                    messageDialog.setMessage("The plugin " + identifier
                            + " is already installed. To manually update a plugin remove it first.");
                    messageDialog.open();
                } else {
                    final File pluginDir = new File(KNIMEIMAGEJPlugin.getEclipsePluginFolderPath());
                    success = FragmentWrapperUtil.installImageJPlugin(pluginDir, jarPath);

                    if (success) {
                        m_pluginManager.put(identifier, new PluginState(identifier, PluginState.STATE.ADDING));
                    } else {
                        m_pluginManager.put(identifier, new PluginState(identifier, PluginState.STATE.UNKNOWN));
                    }
                    reload(pluginList); // reload needed to ensure that the
                    // result is still consistent if
                    // something went wrong

                }
            }
        }

        if (success) {
            return true;
        }
        return false;
    }

    /**
     * triggers the uninstallation of the selected plugin and adjusts the pluginList.
     * 
     * @param pluginList
     */
    public void removeButtonPressed(final List pluginList) {
        if (pluginList.getSelectionIndex() != -1) {
            final String pluginName = pluginList.getItem(pluginList.getSelectionIndex());

            // something to remove has to be part of the observed plugins and be
            // in active state
            if (m_pluginManager.containsKey(pluginName)
                    && (m_pluginManager.get(pluginName).m_state == PluginState.STATE.ACTIVE)) {
                if (KNIMEIMAGEJPlugin.checkFolderPath(KNIMEIMAGEJPlugin.getEclipsePluginFolderPath())) {
                    final File pluginDir = new File(KNIMEIMAGEJPlugin.getEclipsePluginFolderPath());

                    final boolean success = FragmentWrapperUtil.uninstallImageJPlugin(pluginDir, pluginName);

                    if (success) {
                        m_pluginManager.get(pluginName).m_state = PluginState.STATE.REMOVING;
                    } else {
                        m_pluginManager.get(pluginName).m_state = PluginState.STATE.UNKNOWN;
                    }

                    reload(pluginList); // reload needed to ensure that the
                    // result is still consistent if
                    // something went wrong
                }
            }
        }
    }

    /**
     * if the plugin folder path from the preference properties points to a 'plugins' directory an open dialog appears
     * and the user may select an ImageJ plugin jar.
     * 
     * @param parentShell the parent needed to display dialogs
     * 
     * @return path of a user selected ImageJ plugin jar or null
     */
    private File selectImageJPluginJar(final Shell parentShell) {
        if (!KNIMEIMAGEJPlugin.checkFolderPath(KNIMEIMAGEJPlugin.getEclipsePluginFolderPath())) {
            // the set folder path is not correct
            final MessageBox messageDialog = new MessageBox(parentShell, (SWT.ICON_WARNING | SWT.OK));
            messageDialog.setMessage("The KNIME plugins directory has to be specified first");
            messageDialog.open();
            return null;
        } else {

            final FileDialog dialog = new FileDialog(parentShell, SWT.SHEET);

            dialog.setFilterExtensions(new String[]{"*.jar"});
            dialog.setText("Choose an ImageJ plugin jar to install it");

            String dir = dialog.open();
            if (dir != null) {
                dir = dir.trim();
                // is something selected
                if (dir.length() > 0) {
                    final File jarFile = new File(dir);
                    if (jarFile.isFile()) {
                        return jarFile;
                    }
                }
            }

            return null;
        }
    }

    /**
     * lists the installed ImageJ plugin wrapper fragments at the plugins folder.
     * 
     * @return the names of the installed plugins or and empty array if the plugin folder specification is not correct.
     */
    private String[] listInstalledPlugins() {
        final String folderPath = KNIMEIMAGEJPlugin.getEclipsePluginFolderPath();
        if (!KNIMEIMAGEJPlugin.checkFolderPath(folderPath)) { // no correct
            // folder path
            // => no
            // dynamic
            // loading
            return new String[]{};
        } else {
            final ArrayList<String> wrappedFragments = new ArrayList<String>();
            final File pluginDir = new File(folderPath);

            for (final File dir : pluginDir.listFiles()) {
                if (dir.isDirectory()) {
                    final File[] fragmentWrapperProperty = dir.listFiles(m_nameFilter);

                    if (fragmentWrapperProperty.length > 0) {
                        // found the wrapper property add folder name
                        wrappedFragments.add(dir.getName());
                    }

                }
            }
            return wrappedFragments.toArray(new String[]{});
        }
    }

    /**
     * tests by filename if a file is a fragment wrapper property file. Such a file identifies a wrapped ImageJ plugin.
     * 
     * @author zinsmaie
     * 
     */
    private class FragmentWrapperFilenameFilter implements FilenameFilter {

        @Override
        public boolean accept(final File dir, final String name) {
            return name.equals(FragmentWrapperUtil.FRAGMENT_WRAPPER_PROPERTY_FILE);
        }
    }

}
