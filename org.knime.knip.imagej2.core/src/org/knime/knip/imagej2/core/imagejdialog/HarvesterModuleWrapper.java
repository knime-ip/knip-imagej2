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
package org.knime.knip.imagej2.core.imagejdialog;

import java.util.HashSet;
import java.util.Map;

import org.knime.knip.imagej2.core.adapter.IJAdapterProvider;
import org.knime.knip.imagej2.core.adapter.IJInputAdapter;
import org.knime.knip.imagej2.core.adapter.IJStandardInputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.FileAdapter;
import org.scijava.module.Module;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;

/**
 * encapsulates a module and blocks the calls to most setters and executing methods like run and preview.. .Therefore it
 * is save to use this class during harvesting without e.g. triggering the preview method
 *
 * Basically it works like a read only version of the object with the exception that certain set calls have to be
 * implemented because the module is used as data basis by the widgets.
 *
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class HarvesterModuleWrapper implements Module {

    private final Module m_module;

    private final HashSet<String> m_notHarvested;

    /**
     * @param module the module that should be wrapped for secure harvesting
     */
    public HarvesterModuleWrapper(final Module module) {
        m_module = module;
        m_notHarvested = new HashSet<>();

        //ignore all items that cannot be part of the input panel
        for (final ModuleItem<?> item : m_module.getInfo().inputs()) {
            IJInputAdapter<?> o = IJAdapterProvider.getInputAdapter(item.getType());
            //do not allow those items, that are IJStandardInputAdapter (like image input etc.)
            //if there is NO input adapter, allow them and hope that imagej provides a dialog component (TODO: is this the right approach?? what if there is no input adapter AND imagej DOESN'T provide any widget)
            if (o != null && o instanceof IJStandardInputAdapter) {
                m_notHarvested.add(item.getName());
            } else if (o instanceof FileAdapter) {
                m_notHarvested.add(item.getName());
            }
        }
    }

    @Override
    public void run() {
        // not supported in harvester mode
    }

    @Override
    public void preview() {
        // not supported in harvester mode
    }

    @Override
    public void cancel() {
        // not supported in harvester mode
    }

    @Override
    public void initialize() {
        // not supported in harvester mode
    }

    @Override
    public ModuleInfo getInfo() {
        return m_module.getInfo();
    }

    @Override
    public Object getDelegateObject() {
        return m_module.getDelegateObject();
    }

    @Override
    public Object getInput(final String name) {
        return m_module.getInput(name);
    }

    @Override
    public Object getOutput(final String name) {
        return m_module.getOutput(name);
    }

    @Override
    public Map<String, Object> getInputs() {
        return m_module.getInputs();
    }

    @Override
    public Map<String, Object> getOutputs() {
        return m_module.getOutputs();
    }

    @Override
    public void setInput(final String name, final Object value) {
        m_module.setInput(name, value);
    }

    @Override
    public void setOutput(final String name, final Object value) {
        m_module.setOutput(name, value);
    }

    @Override
    public void setInputs(final Map<String, Object> inputs) {
        // not supported in harvester mode
    }

    @Override
    public void setOutputs(final Map<String, Object> outputs) {
        // not supported in harvester mode
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInputResolved(final String name) {
        if (m_notHarvested.contains(name)) {
            return true;
        } else {
            return m_module.isInputResolved(name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOutputResolved(final String name) {
        return m_module.isOutputResolved(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resolveInput(final String name) {
       m_module.resolveInput(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resolveOutput(final String name) {
        m_module.resolveOutput(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unresolveInput(final String name) {
        m_module.unresolveInput(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unresolveOutput(final String name) {
        m_module.unresolveOutput(name);
    }

}
