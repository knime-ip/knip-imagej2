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

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page of the KNIME ImageJ plugin allows to install ImageJ plugins from local jar files. This class is
 * mainly responsible for the creation of the GUI and uses the {@link PluginListController} and the
 * {@link PluginFolderSelectionController} to do things.
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImageJPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private List m_pluginList;

    private Label m_pathLabel;

    private Button m_selectDirButton;

    //makes sure that the choose dir button stays disabled until restart
    private static boolean m_disableSelectedDirButton = false;

    /**
     * holds the needed logic to controll the selection of the eclipse plugin folder.
     */
    private final PluginFolderSelectionController m_folderSelectionController = new PluginFolderSelectionController();

    /**
     * holds the needed logic to add and remove plugins and fills the plugin list from the eclipse plugin path.
     */
    private final PluginListController m_listController = new PluginListController();

    public ImageJPreferencePage() {
        super();
        noDefaultAndApplyButton();
    }

    @Override
    protected Control createContents(final Composite parent) {
        final Composite contentComposite = new Composite(parent, SWT.NULL);

        final GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        contentComposite.setLayout(layout);
        contentComposite.setFont(parent.getFont());

        // add components here
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        final Composite dirSelect = createDirSelectComp(contentComposite);
        dirSelect.setLayoutData(data);
        final Composite pluginInstall = createPluginInstallComp(contentComposite);
        data = new GridData(GridData.FILL_HORIZONTAL);
        pluginInstall.setLayoutData(data);
        final Composite messageLabel = createMessageLabel(contentComposite);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.verticalSpan = 20;
        messageLabel.setLayoutData(data);

        contentComposite.pack();

        // both are initialized now
        initialDataImport(m_pluginList, m_pathLabel);

        return contentComposite;
    }

    /**
     * @param parent
     * @return composite[path label and choose button]
     */
    private Composite createPluginInstallComp(final Composite parent) {
        final Composite contentComposite = new Composite(parent, SWT.NULL);

        final GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        contentComposite.setLayout(layout);
        contentComposite.setFont(parent.getFont());

        GridData data;

        // label
        final Label desc = new Label(contentComposite, SWT.HORIZONTAL);
        desc.setText("installed ImageJ2 plugins:");

        // just a space filler
        //                Label emptyLabel = new Label(contentComposite, SWT.HORIZONTAL);

        // plugin list
        m_pluginList = new List(contentComposite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.heightHint = 200;
        m_pluginList.setLayoutData(data);

        // buttons
        final Composite buttonBox = new Composite(contentComposite, SWT.NULL);

        final GridLayout layout2 = new GridLayout();
        layout2.numColumns = 1;
        layout2.marginHeight = 0;
        layout2.marginWidth = 0;
        buttonBox.setLayout(layout2);
        buttonBox.setFont(parent.getFont());

        // add
        final Button add = new Button(buttonBox, SWT.PUSH);
        add.setText("add");
        data = new GridData(GridData.FILL_HORIZONTAL);
        add.setLayoutData(data);
        add.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (m_pluginList != null) {
                    addButtonPressed(m_pluginList);
                }
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
            }
        });

        // remove
        final Button remove = new Button(buttonBox, SWT.PUSH);
        remove.setText("remove");
        data = new GridData(GridData.FILL_HORIZONTAL);
        remove.setLayoutData(data);
        remove.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (m_pluginList != null) {
                    removeButtonPressed(m_pluginList);
                }
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
            }
        });

        return contentComposite;
    }

    /**
     * 
     * @param parent
     * @return composite[plugin list, add and remove button]
     */
    private Composite createDirSelectComp(final Composite parent) {
        final Composite contentComposite = new Composite(parent, SWT.NULL);

        final GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        contentComposite.setLayout(layout);
        contentComposite.setFont(parent.getFont());

        GridData data;
        final Device device = Display.getCurrent();
        final Color white = new Color(device, 255, 255, 255);

        // label
        final Label desc = new Label(contentComposite, SWT.HORIZONTAL);
        desc.setText("selected knime\\plugins directory:");

        // just a space filler
        //                Label emptyLabel = new Label(contentComposite, SWT.HORIZONTAL);

        // text display
        m_pathLabel = new Label(contentComposite, SWT.BORDER | SWT.SINGLE);
        data = new GridData(GridData.FILL_HORIZONTAL);
        m_pathLabel.setBackground(white);
        m_pathLabel.setLayoutData(data);
        m_pathLabel.setText("");

        // button
        m_selectDirButton = new Button(contentComposite, SWT.PUSH);
        m_selectDirButton.setText("choose");
        m_selectDirButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                if ((m_pluginList != null) && (m_pathLabel != null)) {
                    selectDirButtonPressed(m_pluginList, m_pathLabel);
                }
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
            }
        });

        if (m_disableSelectedDirButton) {
            m_selectDirButton.setEnabled(false);
        }

        return contentComposite;
    }

    /**
     * @param parent
     * @return composite[message label]
     */
    private Composite createMessageLabel(final Composite parent) {
        final Composite contentComposite = new Composite(parent, SWT.NULL);

        final GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        contentComposite.setLayout(layout);
        contentComposite.setFont(parent.getFont());

        // message label
        GridData data;
        final Label descLabel = new Label(contentComposite, SWT.CENTER | SWT.HORIZONTAL | SWT.WRAP);
        descLabel.setText("Use this dialog to install/uninstall annotated ImageJ2 plugins from jar files." + "\n"
                + "In order to complete the process a restart of KNIME will be required.");
        data = new GridData(SWT.END);

        descLabel.setLayoutData(data);

        return contentComposite;
    }

    @Override
    public void init(final IWorkbench workbench) {

    }

    protected void selectDirButtonPressed(final List pluginList, final Label pathLabel) {
        m_folderSelectionController.selectDirButtonPressed(pathLabel, getShell());
        PluginListController.resetPluginManagement();
        m_listController.reload(pluginList);
    }

    protected void addButtonPressed(final List pluginList) {
        final boolean addedSomething = m_listController.addButtonPressed(pluginList, getShell());
        if (addedSomething) {
            m_selectDirButton.setEnabled(false);
            m_disableSelectedDirButton = true;
            setMessage("to complete please restart KNIME", INFORMATION);
        }
    }

    protected void removeButtonPressed(final List pluginList) {
        m_listController.removeButtonPressed(pluginList);
        m_selectDirButton.setEnabled(false);
        m_disableSelectedDirButton = true;
        setMessage("to complete please restart KNIME", INFORMATION);
    }

    protected void initialDataImport(final List pluginList, final Label pathLabel) {
        m_folderSelectionController.reload(pathLabel);
        m_listController.reload(pluginList);
    }
}
