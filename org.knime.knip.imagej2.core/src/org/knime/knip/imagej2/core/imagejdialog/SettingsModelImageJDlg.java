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

import imagej.module.Module;
import imagej.module.ModuleInfo;
import imagej.module.ModuleItem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.xmlbeans.impl.util.Base64;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.imagej2.core.IJGateway;
import org.knime.knip.imagej2.core.adapter.IJAdapterProvider;

/**
 * Partial configuration of a {@link Module} by the basic input parameters of an ImageJ parameter dialog. The parameters
 * get added to an {@link ExtendedInputPanel} and presented to the user with the associated
 * {@link DialogComponentImageJDlg}. This SettingsModel allows to make a configuration persistent in a {@link Config}
 * object.
 *
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SettingsModelImageJDlg extends SettingsModel {

    /**
     * the data values that are set via the Dialog. Associates an item name with its data value.
     */
    private final Map<String, Object> m_itemName2Value;

    private static final String ITEM_KEYS_KEY = "SMIJD_ItemKey";

    private final String m_configName;

    // module info needed to get item type
    private ModuleInfo m_info;

    /**
     * @param configName a name that is suitable for {@link SettingsModel#getConfigName}
     */
    public SettingsModelImageJDlg(final ModuleInfo info, final String configName) {
        m_configName = configName;
        m_info = info;
        m_itemName2Value = new HashMap<String, Object>(10);
    }

    /**
     * sets the values of the ModuleItems that are handled by this SettingsModel and changes their status to resolved.
     *
     * @param module a partially resolved Module where the basic input parameters have been configured and resolved
     */
    public void configureModule(final Module module) {

        for (final String name : m_itemName2Value.keySet()) {
            ModuleItem<?> item = m_info.getInput(name);

            final Object o = m_itemName2Value.get(name);
            final Object value;

            if(null == IJAdapterProvider.getNativeAdapter(item.getType())){
                value = IJGateway.getInstance().getObject(item.getType(), o);
            }else{
                value = o;
            }

            module.setInput(name, value);
            module.setResolved(name, true);
        }

    }

    /**
     * @param itemNames identifies an item by name
     * @return the values of all items that are part of the model and specified in the provided item set
     */
    public Map<String, Object> getItemValues(final Set<String> itemNames) {
        final Map<String, Object> ret = new HashMap<String, Object>();
        for (final String itemName : itemNames) {
            if (m_itemName2Value.containsKey(itemName)) {
                ret.put(itemName, m_itemName2Value.get(itemName));
            }
        }
        return ret;
    }

    /**
     * sets the value of the item with the specified name in the model. E.g. could set the value of an input parameter
     * threshold that is handled in a {@link DialogComponentImageJDlg}.
     *
     * @param itemName identifies an item by name
     * @param value new value of the identified item
     */
    public void setItemValue(final String itemName, final Object value) {
        m_itemName2Value.put(itemName, value);
    }

    @Override
    protected <T extends SettingsModel> T createClone() {
        return (T)new SettingsModelImageJDlg(m_info, m_configName);
    }

    @Override
    protected String getModelTypeID() {
        return "SMID_imagejdlg";
    }

    @Override
    protected String getConfigName() {
        return m_configName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        try {
            loadSettingsForModel(settings);
        } catch (final InvalidSettingsException e) {
            // ignore keep old value for the item
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    /**
     * not implemented
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {

        final String[] keyValues = settings.getStringArray(ITEM_KEYS_KEY);

        for (final String key : keyValues) {

            final String itemString = settings.getString(key);
            final Object item = fromBase64String(itemString);

            if (item != null) {
                m_itemName2Value.put(key, item);
            } else {
                throw new InvalidSettingsException("The item with the identifier " + key + " could not be restored.");
            }

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {

        final String[] keyValues = m_itemName2Value.keySet().toArray(new String[]{});
        settings.addStringArray(ITEM_KEYS_KEY, keyValues);

        for (final String key : m_itemName2Value.keySet()) {

            final Object item;
            if (IJAdapterProvider.getNativeAdapter(m_info.getInput(key).getType()) != null) {
                item = m_itemName2Value.get(key);
            } else {
                item = m_itemName2Value.get(key).toString();
            }

            final String itemString = toBase64String(item);

            if (!itemString.isEmpty()) {
                settings.addString(key, itemString);
            }

        }
    }

    /**
     * encodes the bytes of an object as a base 64 string.
     *
     * @param object
     * @return a string representation of an object in base 64 encoding
     */
    private String toBase64String(final Object object) {
        String ret = "";

        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(object);
            baos.flush();
            out.close();
            baos.close();

            final byte[] byteObject = baos.toByteArray();
            new Base64();
            final byte[] encodedObject = Base64.encode(byteObject);

            ret = new String(encodedObject);
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    /**
     * @param stringRepresentation base64 encoded string representation of the bytecode of an object
     * @return the decoded object
     */
    private Object fromBase64String(final String stringRepresentation) {
        Object ret = null;

        new Base64();
        final byte[] decodedObject = Base64.decode(stringRepresentation.getBytes());

        try {
            final ByteArrayInputStream bis = new ByteArrayInputStream(decodedObject);
            ObjectInputStream ois;
            ois = new ObjectInputStream(bis);
            ret = ois.readObject();
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final ClassNotFoundException e) {
            e.printStackTrace();
        }

        return ret;
    }

    @Override
    public String toString() {
        return m_configName;
    }
}
