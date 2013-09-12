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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.scijava.service.Service;

/**
 * collects all available {@link IJAdapterFactory} instances from the extension point and provides access to the
 * included in and output adapters.
 * 
 * implements a singelton pattern
 * 
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public final class IJAdapterProvider {

    private final NodeLogger LOGGER = NodeLogger.getLogger(IJAdapterProvider.class);

    /** The id of the ijadaptorfactory extension point. */
    private static final String EXT_POINT_ID = "org.knime.knip.imagej2.core.ijadapter";

    /**
     * The attribute of the ijadapterfactory point pointing to the factory class.
     */
    private static final String EXT_POINT_ATTR_DF = "factory-class";

    /**
     * all adapter factories that have been registered at the construction time via the extension point.
     */
    private final List<IJAdapterFactory> m_factories = new ArrayList<IJAdapterFactory>();

    private final Map<Class<?>, IJOutputAdapter<?>> m_OutputAdapters = new HashMap<Class<?>, IJOutputAdapter<?>>();

    private final Map<Class<?>, IJInputAdapter<?>> m_InputAdapters = new HashMap<Class<?>, IJInputAdapter<?>>();

    private final Set<Class<? extends Service>> m_ServiceAdapters = new HashSet<Class<? extends Service>>();

    /** the single instance of this class. */
    private static IJAdapterProvider m_instance;

    // METHODS //

    private IJAdapterProvider() {
        registerExtensionPoints();
        collectAdapters();
    }

    /**
     * @return the singelton instance of the adapter provider.
     */
    private static synchronized IJAdapterProvider getInstance() {
        if (m_instance == null) {
            m_instance = new IJAdapterProvider();
        }

        return m_instance;
    }

    // access

    /**
     * @return ImageJ types that can be processed with registered InputAdapters
     */
    public static Set<Class<?>> getKnownInputTypes() {
        return getInstance().m_InputAdapters.keySet();
    }

    /**
     * @return ImageJ types that can be processed with registered OutputAdapters
     */
    public static Set<Class<?>> getKnownOutputTypes() {
        return getInstance().m_OutputAdapters.keySet();
    }

    /**
     * @return ImageJ Services that are supported by Input and/or Output adapters
     */
    public static Set<Class<? extends Service>> getKnownServiceTypes() {
        return getInstance().m_ServiceAdapters;
    }

    /**
     * returns the first registred adapter that handles this type.
     * 
     * @param <IJ_OBJ> an ImageJ type
     * @param type the class of the ImageJ type
     * @return an input adapter that handles the specified type
     */
    public static <IJ_OBJ> IJInputAdapter<IJ_OBJ> getInputAdapter(final Class<IJ_OBJ> type) {
        final Object o = getInstance().m_InputAdapters.get(type);

        if (o != null) {
            return (IJInputAdapter<IJ_OBJ>)o;
        } else {
            return null;
        }
    }

    /**
     * returns the first registred adapter that handles this type.
     * 
     * @param <IJ_OBJ> an ImageJ type
     * @param type the class of the ImageJ type
     * @return an output adapter that handles the specified type
     */
    public static <IJ_OBJ> IJOutputAdapter<IJ_OBJ> getOutputAdapter(final Class<IJ_OBJ> type) {
        final Object o = getInstance().m_OutputAdapters.get(type);

        if (o != null) {
            return (IJOutputAdapter<IJ_OBJ>)o;
        } else {
            return null;
        }

    }

    // helper

    // TODO implement something better than first come first serve to select a
    // adapter if multiple adapter are specified for the same conversion
    // type
    /**
     * collect the concrete adapters from the factories. The ImageJ input/output type identifies the adapter
     */
    private void collectAdapters() {
        for (final IJAdapterFactory factory : m_factories) {
            // Output
            for (final IJOutputAdapter<?> outputAdapter : factory.getOutputAdapters()) {
                if (m_OutputAdapters.containsKey(outputAdapter.getIJType())) {
                    LOGGER.warn("An ImageJ output to KNIME converter for the ImageJ type " + outputAdapter.getIJType()
                            + " already exists.");
                } else {
                    m_OutputAdapters.put(outputAdapter.getIJType(), outputAdapter);
                }
            }
            // Input
            for (final IJInputAdapter inputAdapter : factory.getInputAdapters()) {
                if (m_InputAdapters.containsKey(inputAdapter.getIJType())) {
                    LOGGER.warn("An KNIME to ImageJ input converter for the ImageJ type " + inputAdapter.getIJType()
                            + " already exists.");
                } else {
                    m_InputAdapters.put(inputAdapter.getIJType(), inputAdapter);
                }
            }
            // Service note service adapters are just collected to inform the
            // gateway about supported services
            for (final IJServiceAdapter serviceAdapter : factory.getServiceAdapters()) {
                if (m_ServiceAdapters.contains(serviceAdapter.getIJType())) {
                    LOGGER.warn("A service adapter for the ImageJ type " + serviceAdapter.getIJType()
                            + " already exists.");
                } else {
                    m_ServiceAdapters.add(serviceAdapter.getIJType());
                }
            }

        }
    }

    /**
     * Registers all extension point implementations.
     */
    private void registerExtensionPoints() {
        try {
            final IExtensionRegistry registry = Platform.getExtensionRegistry();
            final IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
            if (point == null) {
                LOGGER.error("Invalid extension point: " + EXT_POINT_ID);
                throw new IllegalStateException("ACTIVATION ERROR: " + " --> Invalid extension point: " + EXT_POINT_ID);
            }
            for (final IConfigurationElement elem : point.getConfigurationElements()) {
                final String operator = elem.getAttribute(EXT_POINT_ATTR_DF);
                final String decl = elem.getDeclaringExtension().getUniqueIdentifier();

                if ((operator == null) || operator.isEmpty()) {
                    LOGGER.error("The extension '" + decl + "' doesn't provide the required attribute '"
                            + EXT_POINT_ATTR_DF + "'");
                    LOGGER.error("Extension " + decl + " ignored.");
                    continue;
                }

                try {
                    final IJAdapterFactory factory =
                            (IJAdapterFactory)elem.createExecutableExtension(EXT_POINT_ATTR_DF);
                    m_factories.add(factory);
                } catch (final Throwable t) {
                    LOGGER.error("Problems during initialization of " + "Table Cell View (with id '" + operator + "'.)");
                    if (decl != null) {
                        LOGGER.error("Extension " + decl + " ignored.", t);
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Exception while registering " + "TableCellView extensions");
        }
    }

}
