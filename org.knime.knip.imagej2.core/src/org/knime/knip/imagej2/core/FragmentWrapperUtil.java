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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.knime.core.node.NodeLogger;

/**
 * Holds the necessary methods to wrap an ImageJ plugin jar into an eclipse fragment. Provides convenience methods to
 * install / uninstall such fragments by copying them into the plugins folder of eclipse.
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public final class FragmentWrapperUtil {

    private FragmentWrapperUtil() {

    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FragmentWrapperUtil.class);

    /**
     * identifier of the property file of wrapped fragments. Serves also as identifier for such plugins
     */
    public static final String FRAGMENT_WRAPPER_PROPERTY_FILE = "ORG.KNIME.KNIP.IMAGEJ.FRAGMENT_WRAPPER_PROPERTIES";

    /** plugin installer version. */
    public static final String IMAGEJ_PLUGIN_INSTALLER_VERSION = "0.5";

    /**
     * installs an ImageJ plugin into the plugins folder of an eclipse/knime installation.
     * 
     * @param pluginDir path to /knime/plugins
     * @param jarFile contains the ImageJ plugin
     * @return true if everything worked out as intended
     */
    public static boolean installImageJPlugin(final File pluginDir, final File jarFile) {
        String pluginName = jarFile.getName();

        // remove .jar
        if (pluginName.endsWith(".jar")) {
            pluginName = pluginName.substring(0, pluginName.length() - 4);
        }

        final File pluginFolder = new File(pluginDir + File.separator + pluginName);
        final File libFolder = new File(pluginDir + File.separator + pluginName + File.separator + "lib");
        final File metaInfFolder = new File(pluginDir + File.separator + pluginName + File.separator + "META-INF");

        if (pluginFolder.exists()) {
            LOGGER.error("could not install the plugin " + pluginName
                    + " probably there is already a folder with this name.");
        } else {

            boolean success = true;

            // create folders
            pluginFolder.mkdir();
            libFolder.mkdir();
            metaInfFolder.mkdir();

            // create manifest and marker file
            success &=
                    writeTextFile(pluginFolder, FRAGMENT_WRAPPER_PROPERTY_FILE,
                                  "ImageJ2 plugin installation (installer version " + IMAGEJ_PLUGIN_INSTALLER_VERSION
                                          + ")");
            success &= writeTextFile(metaInfFolder, "MANIFEST.MF", createManifestText(pluginName));

            // copy the jar
            final File jarCopy = new File(libFolder + File.separator + pluginName + ".jar");
            success &= copyFile(jarFile, jarCopy);

            if (success) {
                LOGGER.warn("to complete the installation of " + pluginName + " please restart knime.");
                return true;
            }
        }
        return false;
    }

    /**
     * removes an ImageJ plugin from the plugins directory of an eclipse/knime installation.
     * 
     * @param pluginDir path to /knime/plugins
     * @param pluginName the name of the plugin folder
     * @return true if everything worked out as intended
     */
    public static boolean uninstallImageJPlugin(final File pluginDir, final String pluginName) {
        final File plugin = new File(pluginDir.getAbsolutePath() + File.separator + pluginName);

        if (!deleteRecursive(plugin)) {
            deleteRecursiveOnExit(plugin);
            LOGGER.error("problems occured while uninstalling the ImageJ2 plugin " + pluginName + "\n"
                    + " KNIME will try to delete the files on shutdown but" + "\n"
                    + " some files may still remain in the plugins directory");
            return false;
        } else {
            LOGGER.warn("to complete the uninstallation of " + pluginName + " please restart knime.");
            return true;
        }
    }

    /**
     * produces a bytewise copy of a file.
     * 
     * @param from
     * @param to
     * @return true if the operation could be executed
     */
    private static boolean copyFile(final File from, final File to) {
        InputStream in;
        try {
            in = new FileInputStream(from);
            final OutputStream out = new FileOutputStream(to);
            // Transfer bytes from in to out
            final byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (final IOException e) {
            LOGGER.error("plugin installation failed: file " + from + " could not be copied into the plugin folder "
                    + to);
            return false;
        }
        return true;
    }

    /**
     * creates a file with the specified text.
     * 
     * @param dir
     * @param fileName
     * @param text
     * @return true if the operation could be executed
     */
    private static boolean writeTextFile(final File dir, final String fileName, final String text) {
        final File file = new File(dir.getAbsolutePath() + File.separator + fileName);

        BufferedWriter output;
        try {
            output = new BufferedWriter(new FileWriter(file));
            output.write(text);
            output.close();
        } catch (final IOException e) {
            LOGGER.error("plugin installation failed: file " + fileName + " in folder " + dir + " could not be created");
            return false;
        }
        return true;
    }

    /*
     * contains a dependency towards the Fragment-Host => changes of
     * org.knime.knip.imagej2 have to be reflected here
     */
    /**
     * @param pluginName the name of the jar file that should be installed without .jar and without parent directories
     *            e.g. org.knime.knip.myPlugin instead of C://x//y//org.knime.knip.myPlugin.jar
     * @return the textual content of a manifest file that declares the specified plugin as fragment
     */
    private static String createManifestText(final String pluginName) {
        final String ret =
                "Manifest-Version: 1.0" + "\n" + "Fragment-Host: org.knime.knip.imagej2.core;bundle-version=\"0.1.0\""
                        + "\n" + "Bundle-Version: 1.0.0." + (System.currentTimeMillis() / 1000) + "\n"
                        + "Bundle-ClassPath: lib/" + pluginName + ".jar" + "\n"
                        + "Bundle-Name: org.knime.knip.imagej.wrapped." + pluginName + "\n"
                        + "Bundle-ManifestVersion: 2" + "\n" + "Bundle-SymbolicName: org.knime.knip.imagej.wrapped."
                        + pluginName + ";singleton:=true" + "\n" + "Bundle-RequiredExecutionEnvironment: JavaSE-1.6";
        return ret;
    }

    /**
     * @param dir
     * @return true if all files and folders could be deleted
     */
    private static boolean deleteRecursive(final File dir) {
        boolean ret = true;
        if (dir.isDirectory()) {
            final String[] entries = dir.list();
            for (final String entry : entries) {
                final File actFile = new File(dir.getPath(), entry);
                ret &= deleteRecursive(actFile);
            }
        }
        return (ret & dir.delete());
    }

    /**
     * try to delete the file and its childs on exit.
     * 
     * @param dir
     */
    private static void deleteRecursiveOnExit(final File dir) {
        dir.deleteOnExit(); // reverse order deletion start here
        if (dir.isDirectory()) {
            final String[] entries = dir.list();
            for (final String entry : entries) {
                final File actFile = new File(dir.getPath(), entry);
                deleteRecursiveOnExit(actFile);
            }
        }
    }

}
