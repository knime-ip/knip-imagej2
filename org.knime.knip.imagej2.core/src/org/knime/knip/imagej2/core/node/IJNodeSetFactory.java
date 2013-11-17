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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSetFactory;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.ConfigRO;
import org.knime.knip.imagej2.core.IJGateway;
import org.scijava.MenuEntry;

/**
 * Creates a set of ImageJ2 nodes based on the set of supported modules provided by {@link IJGateway}. The nodes are
 * placed at a common menu point.
 *
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class IJNodeSetFactory implements NodeSetFactory {

    public static final String IMAGEJ_MODULE_CLASS_KEY = "imagej_module_class_key";

    /**
     * common part of the category path the top entry that contains subentries and finally the created nodes.
     */
    private static final String CATEGORY_PATH_COMMON_PART = "/community/knip/Imagej2";

    public static final String IMAGEJ_VERSION_KEY = "imagej_version_key";

    private final String IMAGEJ_VERSION;// resolve this in the

    // constructor to avoid potential problems with timing dependencies

    /**
     * creates a new instance of the node set factory.
     */
    public IJNodeSetFactory() {
        IMAGEJ_VERSION = IJGateway.getImageJVersion();
    }

    private final HashMap<String, String> m_categories = new HashMap<String, String>();

    /**
     * loads the supported modules from {@link IJGateway} and creates node factory ids for all supported modules.
     * Furthermore associates the created ids with a category path which is automatically created from the ModuleInfo.
     *
     * @return node factory ids (the delegate class name) of all supported modules
     *
     */
    @Override
    public Collection<String> getNodeFactoryIds() {
        final List<ModuleInfo> moduleInfos = IJGateway.getSupportedModules();
        final List<String> moduleIds = new ArrayList<String>(moduleInfos.size());
        for (final ModuleInfo info : moduleInfos) {
            moduleIds.add(info.getDelegateClassName());

            String path = "";
            if (info.getMenuPath().size() > 0) {
                String tmp = "";
                for (final MenuEntry entry : info.getMenuPath()) {
                    path += tmp; // omit last value from the menu
                    tmp = "/" + entry.getName();
                }
            }
            m_categories.put(info.getDelegateClassName(), CATEGORY_PATH_COMMON_PART + path);

        }

        return moduleIds;
    }

    @Override
    public Class<? extends NodeFactory<? extends NodeModel>> getNodeFactory(final String id) {
        return IJNodeFactory.class;
    }

    @Override
    public String getCategoryPath(final String id) {
        return m_categories.get(id);
    }

    @Override
    public String getAfterID(final String id) {
        return "";
    }

    @Override
    public ConfigRO getAdditionalSettings(final String id) {
        final NodeSettings settings = new NodeSettings("imagej-factory");
        settings.addString(IMAGEJ_MODULE_CLASS_KEY, id);
        settings.addString(IMAGEJ_VERSION_KEY, IMAGEJ_VERSION);
        return settings;
    }

}
