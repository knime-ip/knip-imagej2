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
package org.knime.knip.imagej2.core;

import java.io.File;

import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class KNIMEIMAGEJPlugin extends AbstractUIPlugin {

    /** The plugin ID. */
    public static final String PLUGIN_ID = "org.knime.knip.imagej2";

    /** id of the eclipse folder path property. */
    public static final String PLUGIN_FOLDER_PATH = "IMAGEJ_BASE_PLUGIN_ECLIPSE_FOLDER_PATH";

    // The shared instance.
    private static KNIMEIMAGEJPlugin plugin;

    /**
     * The constructor.
     */
    public KNIMEIMAGEJPlugin() {
        plugin = this;
    }

    /**
     * Returns the shared instance.
     * 
     * @return Singleton instance of the plugin
     */
    public static KNIMEIMAGEJPlugin getDefault() {
        return plugin;
    }

    /**
     * @return the folder path from the properties or an empty string if no correct folder path is specified
     */
    public static String getEclipsePluginFolderPath() {
        if (getDefault().getPreferenceStore().contains(KNIMEIMAGEJPlugin.PLUGIN_FOLDER_PATH)) {
            final String path = getDefault().getPreferenceStore().getString(KNIMEIMAGEJPlugin.PLUGIN_FOLDER_PATH);
            if (checkFolderPath(path)) {
                return path;
            }
        }
        return "";
    }

    /**
     * @param path string that should be tested
     * @return true if the path string points to a 'plugins' directory false for all other cases including the empty
     *         string and null
     */
    public static boolean checkFolderPath(String path) {
        if (path == null) {
            return false;
        }

        path = path.trim();
        if (path.length() == 0) { // no value selected
            return false;
        }
        final File file = new File(path);

        if (file.isDirectory()) {
            return file.getName().equals("plugins");
        } else {
            return false;
        }
    }

}
