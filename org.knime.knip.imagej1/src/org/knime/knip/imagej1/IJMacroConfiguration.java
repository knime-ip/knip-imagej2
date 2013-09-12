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
package org.knime.knip.imagej1;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.knip.base.nodes.io.kernel.SerializableConfiguration;
import org.knime.knip.base.nodes.io.kernel.SerializableSetting;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 * @author schoenenf
 */
public abstract class IJMacroConfiguration extends
		SerializableConfiguration<String> {

	private final static DecimalFormat FORMAT_INTEGER = new DecimalFormat("#0");

	private final static DecimalFormat FORMAT_DOUBLE = new DecimalFormat("#0.0");

	private final String[] m_cachedSettings;

	private JPanel m_jpCodePanel;

	private JCheckBox m_jcbUseCode;

	private JTextArea m_jtaCodeView;

	private JPanel m_jpContent;

	private GridBagConstraints m_gbc;

	private final ArrayList<Settings> m_settings;

	private final ActionListener m_updateTemplateListener = new ActionListener() {

		@Override
		public void actionPerformed(final ActionEvent arg0) {
			updateTemplate();
		}
	};

	public IJMacroConfiguration() {
		initGui();
		m_settings = new ArrayList<IJMacroConfiguration.Settings>();
		codeOptions();
		m_cachedSettings = new String[m_settings.size()];
	}

	private final void initGui() {
		m_jtaCodeView = new JTextArea();
		m_jtaCodeView.setLineWrap(true);
		m_jtaCodeView.setWrapStyleWord(true);
		m_jtaCodeView.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(final FocusEvent e) {
				m_jcbUseCode.setSelected(true);
			}
		});
		m_jcbUseCode = new JCheckBox("Use modified code!");
		m_jcbUseCode.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				updateTemplate();
				for (final Component c : m_jpContent.getComponents()) {
					c.setEnabled(!m_jcbUseCode.isSelected());
				}
			}
		});
		m_jpCodePanel = new JPanel(new BorderLayout());
		m_jpCodePanel.add(new JScrollPane(m_jtaCodeView), BorderLayout.CENTER);
		m_jpCodePanel.add(m_jcbUseCode, BorderLayout.SOUTH);
		m_jpContent = new JPanel(new GridBagLayout());
		m_gbc = new GridBagConstraints();
		m_gbc.gridy = 0;
		m_gbc.ipadx = 5;
		m_gbc.ipady = 5;
		m_gbc.fill = GridBagConstraints.BOTH;
	}

	private void updateTemplate() {
		if (!m_jcbUseCode.isSelected()) {
			try {
				validate();
			} catch (InvalidSettingsException e) {
			}
			m_jtaCodeView.setText(String.format(codeTemplate(),
					m_cachedSettings));
		}
	}

	protected abstract String codeTemplate();

	protected abstract void codeOptions();

	protected abstract Class<? extends IJMacroConfiguration> configrationClass();

	protected final void loadFromSetting(final MacroSetting setting) {
		System.arraycopy(setting.cachedSettings, 0, m_cachedSettings, 0,
				m_cachedSettings.length);
		for (int i = 0; i < m_cachedSettings.length; i++) {
			m_settings.get(i).setValue(m_cachedSettings[i]);
		}
		m_jcbUseCode.setSelected(setting.usedTemplate);
		m_jtaCodeView.setText(setting.code);
	}

	/*
	 * SerializableConfiguration API
	 */

	@Override
	public JComponent getPreviewPanel() {
		m_updateTemplateListener.actionPerformed(null);
		return m_jpCodePanel;
	}

	@Override
	public JComponent getConfigurationPanel() {
		return m_jpContent;
	}

	@Override
	public SerializableSetting<String> getSetting() {
		try {
			validate();
		} catch (InvalidSettingsException e) {
		}
		String code;
		if (m_jcbUseCode.isSelected()) {
			code = m_jtaCodeView.getText();
		} else {
			code = String.format(codeTemplate(), m_cachedSettings);
		}
		return new MacroSetting(code, m_cachedSettings,
				m_jcbUseCode.isSelected(), configrationClass());
	}

	@Override
	public void validate() throws InvalidSettingsException {
		if (m_jtaCodeView != null) {
			for (int i = 0; i < m_cachedSettings.length; i++) {
				final String error = m_settings.get(i).validate();
				if (error != null) {
					throw new InvalidSettingsException(error);
				}
				m_cachedSettings[i] = m_settings.get(i).getValue();
			}
		}
	}

	/*
	 * ImageJ settings helper
	 */
	protected final void addMessage(final String label) {
		final JLabel jl = new JLabel(label);
		m_gbc.gridx = 0;
		m_gbc.weightx = 1;
		m_jpContent.add(jl, m_gbc);
		m_gbc.gridy++;
	}

	protected final void addInteger(final String label, final int def) {
		final JLabel jl = new JLabel(label + ":");
		m_gbc.gridx = 0;
		m_gbc.weightx = 0;
		m_jpContent.add(jl, m_gbc);
		final JFormattedTextField jft = new JFormattedTextField(FORMAT_INTEGER);
		jft.addActionListener(m_updateTemplateListener);
		jft.setText(String.valueOf(def));
		m_gbc.gridx = 1;
		m_gbc.weightx = 1;
		m_jpContent.add(jft, m_gbc);
		m_gbc.gridy++;
		m_settings.add(new SettingsInteger(jft));
	}

	protected final void addDouble(final String label, final double def) {
		m_gbc.gridx = 0;
		m_gbc.weightx = 0;
		m_gbc.gridwidth = 1;
		m_jpContent.add(new JLabel(label + ":"), m_gbc);
		final JFormattedTextField jft = new JFormattedTextField(FORMAT_DOUBLE);
		jft.addActionListener(m_updateTemplateListener);
		jft.setText(String.valueOf(def));
		m_gbc.gridx = 1;
		m_gbc.weightx = 1;
		m_gbc.gridwidth = 5;
		m_jpContent.add(jft, m_gbc);
		m_gbc.gridy++;
		m_settings.add(new SettingsDouble(jft));
	}

	protected final void addRange(final String label, final double min,
			final double max) {
		m_gbc.gridx = 0;
		m_gbc.weightx = 0;
		m_gbc.gridwidth = 1;
		m_jpContent.add(new JLabel(label + ":"), m_gbc);
		m_gbc.gridx = 1;
		m_jpContent.add(new JLabel("["), m_gbc);

		final JFormattedTextField jftMin = new JFormattedTextField(
				FORMAT_DOUBLE);
		jftMin.addActionListener(m_updateTemplateListener);
		m_gbc.gridx = 2;
		m_gbc.weightx = 1;
		m_jpContent.add(jftMin, m_gbc);

		m_gbc.gridx = 3;
		m_gbc.weightx = 0;
		m_jpContent.add(new JLabel(".."), m_gbc);

		final JFormattedTextField jftMax = new JFormattedTextField(
				FORMAT_DOUBLE);
		jftMax.addActionListener(m_updateTemplateListener);
		m_gbc.gridx = 4;
		m_gbc.weightx = 1;
		m_jpContent.add(jftMax, m_gbc);

		if ((m_cachedSettings != null)
				&& (m_cachedSettings[m_gbc.gridy] != null)) {
			final String[] minmax = m_cachedSettings[m_gbc.gridy].split("-");
			jftMin.setText(minmax[0]);
			jftMax.setText(minmax[1]);
		} else {
			jftMin.setText(String.valueOf(min));
			jftMax.setText(String.valueOf(max));
		}

		m_gbc.gridx = 5;
		m_gbc.weightx = 0;
		m_jpContent.add(new JLabel("]"), m_gbc);
		m_gbc.gridy++;
		m_settings.add(new SettingsRange(jftMin, jftMax));
	}

	protected final void addCheckbox(final String label, final String name,
			final boolean def) {
		m_gbc.gridx = 1;
		m_gbc.weightx = 1;
		m_gbc.gridwidth = 5;
		final JCheckBox jcb = new JCheckBox(label);
		jcb.addActionListener(m_updateTemplateListener);
		jcb.setSelected(def);
		m_jpContent.add(jcb, m_gbc);
		m_gbc.gridy++;
		m_settings.add(new SettingsBoolean(name, jcb));
	}

	protected final void addChoice(final String label, final String[] values,
			final String def) {
		m_gbc.gridx = 0;
		m_gbc.weightx = 0;
		m_gbc.gridwidth = 1;
		m_jpContent.add(new JLabel(label + ":"), m_gbc);
		final JComboBox jcb = new JComboBox(values);
		jcb.addActionListener(m_updateTemplateListener);
		m_gbc.gridx = 1;
		m_gbc.weightx = 1;
		m_gbc.gridwidth = 5;
		m_jpContent.add(jcb, m_gbc);
		m_gbc.gridy++;
		m_settings.add(new SettingsChoice(jcb));
	}

	private abstract class Settings {
		public abstract String validate();

		public abstract void setValue(String value);

		public abstract String getValue();

		@Override
		public String toString() {
			return getValue();
		}
	}

	private class SettingsInteger extends Settings {
		private final JFormattedTextField m_jft;

		public SettingsInteger(final JFormattedTextField jft) {
			m_jft = jft;
		}

		@Override
		public String validate() {
			try {
				Integer.parseInt(m_jft.getText());
			} catch (final NumberFormatException e) {
				return e.getMessage();
			}
			return null;
		}

		@Override
		public String getValue() {
			return m_jft.getText();
		}

		@Override
		public void setValue(final String value) {
			m_jft.setText("" + value);
		}
	}

	private class SettingsDouble extends Settings {
		private final JFormattedTextField m_jft;

		public SettingsDouble(final JFormattedTextField jft) {
			m_jft = jft;
		}

		@Override
		public String validate() {
			try {
				Double.parseDouble(m_jft.getText());
			} catch (final NumberFormatException e) {
				return e.getMessage();
			}
			return null;
		}

		@Override
		public String getValue() {
			return m_jft.getText();
		}

		@Override
		public void setValue(final String value) {
			m_jft.setText("" + value);
		}
	}

	private class SettingsRange extends Settings {
		private final JFormattedTextField m_jftMin;

		private final JFormattedTextField m_jftMax;

		public SettingsRange(final JFormattedTextField jftMin,
				final JFormattedTextField jftMax) {
			m_jftMin = jftMin;
			m_jftMax = jftMax;
		}

		@Override
		public String validate() {
			try {
				Double.parseDouble(m_jftMin.getText());
				Double.parseDouble(m_jftMax.getText());
			} catch (final NumberFormatException e) {
				return e.getMessage();
			}
			return null;
		}

		@Override
		public String getValue() {
			return m_jftMin.getText() + "-" + m_jftMax.getText();
		}

		@Override
		public void setValue(final String value) {
			final String[] minmax = value.split("-");
			m_jftMin.setText(minmax[0]);
			m_jftMax.setText(minmax[1]);
		}
	}

	private class SettingsBoolean extends Settings {
		private final String m_name;

		private final JCheckBox m_jcb;

		public SettingsBoolean(final String name, final JCheckBox jcb) {
			m_name = name;
			m_jcb = jcb;
		}

		@Override
		public String validate() {
			return null;
		}

		@Override
		public String getValue() {
			if (m_jcb.isSelected()) {
				return m_name;
			}
			return "";
		}

		@Override
		public void setValue(final String value) {
			m_jcb.setSelected(!value.isEmpty());
		}
	}

	private class SettingsChoice extends Settings {
		private final JComboBox m_jcb;

		public SettingsChoice(final JComboBox jcb) {
			m_jcb = jcb;
		}

		@Override
		public String validate() {
			return null;
		}

		@Override
		public String getValue() {
			return (String) m_jcb.getSelectedItem();
		}

		@Override
		public void setValue(final String value) {
			m_jcb.setSelectedItem(value);
		}
	}
}

class MacroSetting extends SerializableSetting<String> {

	private static final long serialVersionUID = 1L;

	final String code;

	final String[] cachedSettings;

	final boolean usedTemplate;

	final Class<? extends IJMacroConfiguration> config;

	public MacroSetting(final String code, final String[] cachedSettings,
			final boolean usedTemplate,
			final Class<? extends IJMacroConfiguration> config) {
		super();
		this.code = code;
		this.cachedSettings = cachedSettings;
		this.usedTemplate = usedTemplate;
		this.config = config;
	}

	@Override
	public String get() {
		return code;
	}

	@Override
	protected SerializableConfiguration<String> createConfiguration() {
		try {

			final IJMacroConfiguration conf = config.newInstance();
			conf.loadFromSetting(this);
			return conf;
		} catch (final InstantiationException e) {
			e.printStackTrace();
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
}
