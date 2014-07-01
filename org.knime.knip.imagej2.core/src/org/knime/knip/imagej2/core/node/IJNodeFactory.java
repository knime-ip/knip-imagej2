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

import ij.ImagePlus;

import java.net.URL;
import java.util.Iterator;

import net.imagej.Dataset;
import net.imagej.display.ImageDisplay;
import net.imglib2.img.Img;
import net.imglib2.labeling.Labeling;
import net.imglib2.meta.ImgPlus;

import org.knime.core.data.DataValue;
import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.knip.base.nodes.view.TableCellViewNodeView;
import org.knime.knip.imagej2.core.IJGateway;
import org.knime.knip.imagej2.core.adapter.IJAdapterProvider;
import org.knime.knip.imagej2.core.adapter.IJInputAdapter;
import org.knime.knip.imagej2.core.adapter.IJStandardInputAdapter;
import org.knime.node2012.FullDescriptionDocument.FullDescription;
import org.knime.node2012.InPortDocument.InPort;
import org.knime.node2012.IntroDocument.Intro;
import org.knime.node2012.KnimeNodeDocument;
import org.knime.node2012.KnimeNodeDocument.KnimeNode;
import org.knime.node2012.OptionDocument.Option;
import org.knime.node2012.OutPortDocument.OutPort;
import org.knime.node2012.PortsDocument.Ports;
import org.knime.node2012.TabDocument.Tab;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;
import org.scijava.service.Service;

/**
 * Node factory for ImageJ2 plugins. Contains internal settings that associate an instance of IJNodeFactory with a
 * specific ImageJ2 plugin, that is created as KNIME node by the factory instance.
 *
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class IJNodeFactory extends DynamicNodeFactory<AbstractIJNodeModel> {

    /**
     * defines which ImageJ plugin is wrapped as KNIME node by this factory instance.
     */
    private ModuleInfo m_moduleInfo;

    @Override
    public AbstractIJNodeModel createNodeModel() {

        if (useValueToCell()) {
            return new ValueToCellIJNodeModel(m_moduleInfo, getNrInports(), getNrOutports());
        } else {
            return new StandardIJNodeModel(m_moduleInfo, getNrInports(), getNrOutports());
        }
    }

    /**
     * @return one if at least one return type of the ImageJ plugin can be displayed by a {@link TableCellViewNodeView}
     *         else zero
     */
    @Override
    protected int getNrNodeViews() {
        boolean imgOutput = false;

        for (final ModuleItem<?> item : m_moduleInfo.outputs()) {
            if (item.getType().equals(Dataset.class) || item.getType().equals(ImageDisplay.class)
                    || item.getType().equals(ImagePlus.class) || item.getType().equals(Img.class)
                    || item.getType().equals(ImgPlus.class) || item.getType().equals(Labeling.class)) {
                imgOutput = true;
                break;
            }
        }

        if (imgOutput) {
            return 1;
        }
        return 0;
    }

    /**
     * @return returns a new node view if {@link IJNodeFactory#getNrNodeViews()} equals one. else returns null
     *
     *
     *         {@inheritDoc}
     *
     */
    @Override
    public NodeView<AbstractIJNodeModel> createNodeView(final int viewIndex, final AbstractIJNodeModel nodeModel) {
        if (getNrNodeViews() == 1) {
            return new TableCellViewNodeView<AbstractIJNodeModel>(nodeModel);
        }
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        if (useValueToCell()) {
            //passed all tests such that the constructor of the dialog
            //can succeed
            return new ValueToCellIJNodeDialog(m_moduleInfo);
        } else {
            return new StandardIJNodeDialog(m_moduleInfo);
        }
    }

    /**
     * Automatically creates node descriptions based on the meta data that is provided with the ModuleInfo of an ImageJ2
     * plugin.<br>
     * <br>
     * Creates: <br>
     * - (optional) a description for a table cell viewer if the output matches one of the predefined displayable types <br>
     * - an icon from the plugin jar or the standard plugin icon<br>
     * - a menu category from the plugin menu path - a description of the plugin - a basic static port description
     *
     * {@inheritDoc}
     *
     * @deprecated
     *
     */
    @Deprecated
    @Override
    protected void addNodeDescription(final KnimeNodeDocument doc) {
        final KnimeNode node = doc.addNewKnimeNode();

        // TYPE
        node.setType(KnimeNode.Type.OTHER);

        // VIEW
        if (getNrNodeViews() > 0) {
            TableCellViewNodeView.addViewDescriptionTo(node.addNewViews());
        }

        // ICON
        String path = "default_icon.png";
        try {
            if (m_moduleInfo.getIconPath().length() > 0) {
                URL iconURL =
                        Class.forName(m_moduleInfo.getDelegateClassName()).getResource(m_moduleInfo.getIconPath());
                path = iconURL.getPath();
            }
        } catch (final ClassNotFoundException e) {
            // use fallback icon
        }
        node.setIcon(path);

        // MENU
        String name;
        if (m_moduleInfo.getMenuPath().size() > 0) {
            name = m_moduleInfo.getMenuPath().getLeaf().toString();
        } else {
            // fallback
            final String[] names = m_moduleInfo.getDelegateClassName().split("\\.");
            name = names[names.length - 1];
        }
        node.setName(name);
        node.setShortDescription(m_moduleInfo.getTitle() + " ");

        // full description
        FullDescription desc = node.addNewFullDescription();
        Intro intro = desc.addNewIntro();
        intro.addNewP().newCursor().setTextValue(m_moduleInfo.getDescription() + " ");
        intro.addNewP().newCursor().setTextValue("(based on ImageJ " + " 2_0_x" + ")");

        //variable description if any
        Tab tab = null;
        boolean createdTab = false;

        for (ModuleItem<?> item : m_moduleInfo.inputs()) {
            if (isParameter(item)) {
                if (!createdTab) {
                    tab = desc.addNewTab();
                    tab.setName("ImageJ Dialog");
                    createdTab = true;
                }
                if (tab == null) {
                    throw new IllegalStateException("Tab shouldn't be null in IJNodeFactory");
                }

                Option opt = tab.addNewOption();
                opt.setName(item.getLabel());

                if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                    opt.newCursor().setTextValue(item.getDescription());
                } else {
                    opt.newCursor().setTextValue("- description missing -");
                }
            }
        }

        // adding the ports to the description
        if ((getNrInports() > 0) || (getNrOutports() > 0)) {
            final Ports ports = node.addNewPorts();

            if (getNrInports() == 1) {
                final InPort inPort = ports.addNewInPort();
                inPort.newCursor().setTextValue("Images");
                inPort.setName("Images");
                inPort.setIndex(0);
                inPort.newCursor().setTextValue("Images");
            }
            if (getNrOutports() == 1) {
                final OutPort outPort = ports.addNewOutPort();
                outPort.setName("Processed Images");
                outPort.setIndex(0);
                outPort.newCursor().setTextValue("Processed Images");
            }
        }
    }

    private boolean isParameter(final ModuleItem<?> item) {
        // not a service
        if ((org.scijava.service.Service.class.isAssignableFrom(item.getType()))) {
            return false;
        }

        //testing if we use data values for this parameter
        final IJInputAdapter<?> inputAdapter = IJAdapterProvider.getInputAdapter(item.getType());
        if (inputAdapter == null || inputAdapter instanceof IJStandardInputAdapter) {
            return false;
        }

        return true;
    }

    /**
     * loads additional factory settings that allow to load different IJNodeFactory instances for the different ImageJ
     * plugins. Essentially an identifier is loaded that allows reloading of meta data that is specific to the
     * associated ImageJ plugin of the IJNodeFactory
     *
     * {@inheritDoc}
     *
     */
    @Override
    public void loadAdditionalFactorySettings(final ConfigRO config) throws InvalidSettingsException {
        String version = config.getString(IJNodeSetFactory.IMAGEJ_VERSION_KEY);
        String moduleClass = config.getString(IJNodeSetFactory.IMAGEJ_MODULE_CLASS_KEY);
        m_moduleInfo = IJGateway.getModuleInfo(version, moduleClass);
        super.loadAdditionalFactorySettings(config);
    }

    /**
     * saves additional factory settings such that different IJNodeFactory instances can be loaded for the different
     * ImageJ Plugins. Essentially stores an identifier that allows reloading of the specific information that belongs
     * to a specific Plugin.
     *
     * {@inheritDoc}
     *
     */
    @Override
    public void saveAdditionalFactorySettings(final ConfigWO config) {
        config.addString(IJNodeSetFactory.IMAGEJ_MODULE_CLASS_KEY, m_moduleInfo.getDelegateClassName());
        config.addString(IJNodeSetFactory.IMAGEJ_VERSION_KEY, IJGateway.getImageJVersion());
        super.saveAdditionalFactorySettings(config);
    }

    /**
     * @return 0 or 1
     */
    private int getNrInports() {
        int nrInputPorts = 0;
        if (m_moduleInfo.inputs().iterator().hasNext()) {
            // at least one hasNext() => provide an input port
            nrInputPorts = 1;
        }
        return nrInputPorts;
    }

    /**
     * @return 0 or 1
     */
    private int getNrOutports() {
        int nrOutputPorts = 0;
        if (m_moduleInfo.outputs().iterator().hasNext()) {
            // at least one hasNext() => provide an output port
            nrOutputPorts = 1;
        }
        return nrOutputPorts;
    }

    /**
     * Tests if the ValueToCell dialog/model and cell factory can be used to present this module to the KNIME user. This
     * is the case if exactly one ImageJ input and output exists (except for basic ImageJ dialog inputs). Additionally
     * the selected adapters are only allowed to use one column and the input adapter must be a
     * {@link IJStandardInputAdapter}
     *
     * @return true if the ValueToCell dialog/model/cell factory can be used
     */
    @SuppressWarnings("unchecked")
    private boolean useValueToCell() {
        Iterator<ModuleItem<?>> iter;
        ModuleItem<?> item = null;
        Class<? extends DataValue>[] adapptedValues;

        //exactly one ImageJ output
        iter = m_moduleInfo.outputs().iterator();
        int nrOutputs = 0;
        while (iter.hasNext()) {
            item = iter.next();
            nrOutputs++;
        }

        if (nrOutputs != 1) {
            return false;
        }

        if (item == null) {
            throw new IllegalStateException("Item shoudln't be null in IJNodeFactory");
        }

        //with one KNIME output
        adapptedValues = IJAdapterProvider.getOutputAdapter(item.getType()).getDataValueTypes();
        if (adapptedValues.length != 1) {
            return false;
        }

        //exactly one not dialog bound ImageJ input
        iter = m_moduleInfo.inputs().iterator();
        int nrInputs = 0;
        while (iter.hasNext()) {
            final ModuleItem<?> tmpItem = iter.next();
            if (!IJGateway.isIJDialogInputType(tmpItem.getType())
                    && !(Service.class.isAssignableFrom(tmpItem.getType()))) {
                nrInputs++;
                item = tmpItem;
            }
        }

        if (nrInputs != 1) {
            return false;
        }

        //with one KNIME standard input adapter
        if (!(IJAdapterProvider.getInputAdapter(item.getType()) instanceof IJStandardInputAdapter)) {
            return false;
        }

        //that requires one input
        @SuppressWarnings("rawtypes")
        final IJStandardInputAdapter adapter =
                (IJStandardInputAdapter)IJAdapterProvider.getInputAdapter(item.getType());
        if (adapter.createModuleItemConfig(item).getGuiMetaInfo().length != 1) {
            return false;
        }

        //matched all tests one standard input adapter one output adapter
        //both using exactly one column and the input adapter does not convert to a basic type
        return true;
    }

}
