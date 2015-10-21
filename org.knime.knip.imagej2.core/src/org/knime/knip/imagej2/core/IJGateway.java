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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bushe.swing.event.EventService;
import org.knime.core.node.NodeLogger;
import org.knime.knip.imagej2.core.adapter.IJAdapterProvider;
import org.knime.knip.scijava.core.ResourceAwareClassLoader;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.command.CommandInfo;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.menu.MenuService;
import org.scijava.module.MethodCallException;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;
import org.scijava.module.ModuleService;
import org.scijava.object.ObjectService;
import org.scijava.options.OptionsService;
import org.scijava.plugin.DefaultPluginFinder;
import org.scijava.plugin.PluginIndex;
import org.scijava.service.Service;
import org.scijava.tool.ToolService;
import org.scijava.ui.UIService;
import org.scijava.util.ClassUtils;
import org.scijava.util.ColorRGB;

import net.imagej.DatasetService;
import net.imagej.autoscale.AutoscaleService;
import net.imagej.units.UnitService;

/**
 * provides access to the ImageJ context and loads supported ImageJ Plugins. The class implements the singleton pattern
 * and ensures that only one ImageJ Context is created.
 *
 *
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public final class IJGateway {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(IJGateway.class);

    // CONSTANTS

    /**
     * all types that can be supported as input type for an ImageJ plugin as a
     * {@link org.knime.knip.imagej2.core.imagejdialog panel} derived from the ImageJ dialog.
     */
    public static final Class<?>[] SUPPORTED_IJ_DIALOG_TYPES =
            {Number.class, byte.class, double.class, float.class, int.class, long.class, short.class, String.class,
                    Character.class, char.class, Boolean.class, boolean.class, File.class, ColorRGB.class};

    /**
     * all services that are supported out of the box by the IJ Adapters. Mainly services that are actually not
     * supported but will do no harm like the MenuService
     */
    @SuppressWarnings("unchecked")
    private static final Class<? extends Service>[] SUPPORTED_SERVICES =
            new Class[]{UnitService.class, UIService.class, MenuService.class, ToolService.class, EventService.class, ObjectService.class,
                    AutoscaleService.class, OptionsService.class, DatasetService.class};

    // MEMBERS

    /** singleton instance on IJGateway (maybe headless or not). */
    private static IJGateway instance = null;

    /** the one and unique ImageJ context. */
    private final Context m_imageJContext;

    /**
     * moduleinfos of all modules that are supported i.e. use only supported input and output parameters, can run
     * headless...
     */
    private final List<ModuleInfo> m_supportedModulesInfos;

    /**
     * associates the delgeateClassNames of the supported modules with their module infos.
     */
    private final Map<String, ModuleInfo> m_delegateClassName2ModuleInfo;

    /** version number of ImageJ. **/
    private String m_imagejVersion;

    /** Singleton on ModuleService */
    private ModuleService m_moduleService;

    /** Singleton on ObjectService */
    private ObjectService m_objectService;

    /**
     * @return the singelton instance of IJGateway
     */
    public static synchronized IJGateway getInstance() {

        if (instance == null) {
            instance = new IJGateway();
        }

        return instance;
    }

    /**
     * creates the ImageJ context and initializes the list of supported modules.
     */
    private IJGateway() {

        // set log level
        if (System.getProperty(LogService.LOG_LEVEL_PROPERTY) == null) {
            System.setProperty(LogService.LOG_LEVEL_PROPERTY, "error");
        }

        // create ImageJ context with all available services
        m_imageJContext = new Context(new PluginIndex(
                new DefaultPluginFinder(new ResourceAwareClassLoader(getClass().getClassLoader(), getClass()))));

        // get list of modules, and filter them to those acceptable to
        // KNIME/KNIP
        final List<ModuleInfo> moduleInfos = getModuleService().getModules();
        m_supportedModulesInfos = findSupportedModules(moduleInfos);
        m_delegateClassName2ModuleInfo = new HashMap<String, ModuleInfo>(m_supportedModulesInfos.size());
        for (final ModuleInfo info : m_supportedModulesInfos) {
            m_delegateClassName2ModuleInfo.put(info.getDelegateClassName(), info);
        }

    }

    /**
     * @return the version of ImageJ2
     */
    public static String getImageJVersion() {
        return getInstance().m_imagejVersion;
    }

    /**
     * @return the imageJ context of the singleton instance
     */
    public static Context getImageJContext() {
        return getInstance().m_imageJContext;
    }

    /**
     * @return the ModuleInfos of the modules ImageJ finds and KNIME supports
     */
    public static List<ModuleInfo> getSupportedModules() {
        return getInstance().m_supportedModulesInfos;
    }

    /**
     * @param moduleInfoDelegateClassName
     * @param version imagej version at which the node was last saved
     * @return the module info associated with the delegate class name
     */
    public static ModuleInfo getModuleInfo(final String version, final String moduleInfoDelegateClassName) {
        return getInstance().m_delegateClassName2ModuleInfo.get(moduleInfoDelegateClassName);
    }

    /**
     * tests a type against the internal list of ImageJ dialog input types.
     *
     * @param type the type to test
     * @return true if this type can be handled by the ImageJ dialog
     */
    public static synchronized boolean isIJDialogInputType(final Class<?> type) {
        boolean ret = false;
        for (final Class<?> c : SUPPORTED_IJ_DIALOG_TYPES) {
            if (c.isAssignableFrom(type)) {
                ret = true;
            }
        }
        return ret;
    }

    // PRIVATE HELPERS

    /**
     * tests if modules can run headless, have a static amount of inputs and outputs and whether KNIME supports these
     * input and output types.
     *
     * @param modules the candidates
     * @return a filtered list that contains only plugins that can run in KNIME
     */
    private List<ModuleInfo> findSupportedModules(final List<ModuleInfo> modules) {
        final ArrayList<ModuleInfo> supportedModules = new ArrayList<ModuleInfo>();
        for (final ModuleInfo info : modules) {

            if (info.canRunHeadless()) {
                if (!isDynamicPlugin(info)) {

                    //test if at least one input or output exists
                    boolean hasInOrOutput = false;

                    try {

                        // test inputs
                        boolean inputsOK = true;
                        for (final ModuleItem<?> item : info.inputs()) {
                            final Class<?> type = item.getType();
                            hasInOrOutput = true;
                            if (!isSupportedInputType(info, type)) {
                                inputsOK = false;
                                break;
                            }
                        }

                        if (inputsOK) { // test outputs
                            boolean outputsOK = true;
                            for (final ModuleItem<?> item : info.outputs()) {
                                final Class<?> type = item.getType();
                                hasInOrOutput = true;
                                if (!isSupportedOutputType(type)) {
                                    outputsOK = false;
                                    break;
                                }
                            }

                            if (outputsOK && hasInOrOutput) { // && inputsOK
                                supportedModules.add(info);
                            }
                        }

                    } catch (Throwable t) {
                        //we have to catch throwable to detect errors caused by missing class definitions
                        LOGGER.debug(t);
                        LOGGER.error("error during ImageJ plugin discovery " + t.getMessage());
                    }
                }
            }
        }

        return supportedModules;
    }

    /**
     * tests if the plugin has dynamically generated inputs and outputs.
     *
     * @param info module info of the plugin
     * @return true if the plugin has dynamically generated inputs or outputs
     */
    private static boolean isDynamicPlugin(final ModuleInfo info) {
        // SO UGLY! TODO: add API to ModuleInfo to query whether the module has
        // dynamically generated inputs and outputs (isStatic? isDynamic?)
        final String className = info.getDelegateClassName();
        final Class<?> c;
        if (info instanceof CommandInfo) {
            try {
                c = ((CommandInfo)info).loadClass();
            } catch (final InstantiableException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return true;
            }
        } else {
            c = ClassUtils.loadClass(className);
        }

        return DynamicCommand.class.isAssignableFrom(c);
    }

    /**
     * test if the type is supported as input type.
     *
     * @param type
     * @return true if KNIME can handle the specified input type e.g. with an adapter or the ImageJ parameter dialog
     * @throws ModuleException
     * @throws MethodCallException
     */
    private boolean isSupportedInputType(final ModuleInfo info, final Class<?> type) {

        // test for classes that can be mapped to the image j generated dialog
        for (final Class<?> candidate : SUPPORTED_IJ_DIALOG_TYPES) {
            if (candidate.isAssignableFrom(type)) {
                return true;
            }
        }

        //         test for supported services
        for (final Class<?> candidate : SUPPORTED_SERVICES) {
            if (candidate.isAssignableFrom(type)) {
                return true;
            }
        }

        // test for addapter supported services
        if (IJAdapterProvider.getKnownServiceTypes().contains(type)) {
            return true;
        }

        // test for adaptable types
        if (IJAdapterProvider.getKnownInputTypes().contains(type)) {
            return true;
        }

        // check for objects with lists
        if (isMultipleChoiceObject(type)) {
            return true;
        }
        return false;
    }

    /**
     * @param type
     * @return true if type is a plugin for a multi-type object
     */
    public boolean isMultipleChoiceObject(final Class<?> type) {
        return getObjectService().getObjects(type).size() > 0;
    }

    /**
     * tests if the type is supported as output type.
     *
     * @param type
     * @return true if KNIME can handle the output type i.e. an adapter for this type exists
     */
    private static boolean isSupportedOutputType(final Class<?> type) {

        // test for adaptable types
        if (IJAdapterProvider.getKnownOutputTypes().contains(type)) {
            return true;
        }

        return false;
    }

    /**
     * @return {@link ObjectService} (singleton)
     */
    public ObjectService getObjectService() {
        if (m_objectService == null) {
            m_objectService = m_imageJContext.getService(ObjectService.class);
        }

        return m_objectService;
    }

    /**
     * @return {@link ModuleService} (singleton)
     */
    public ModuleService getModuleService() {
        if (m_moduleService == null) {
            m_moduleService = m_imageJContext.getService(ModuleService.class);
        }

        return m_moduleService;
    }
}
