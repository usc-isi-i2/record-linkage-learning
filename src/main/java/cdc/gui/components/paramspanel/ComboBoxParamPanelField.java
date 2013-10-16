/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is the FRIL Framework.
 *
 * The Initial Developers of the Original Code are
 * The Department of Math and Computer Science, Emory University and 
 * The Centers for Disease Control and Prevention.
 * Portions created by the Initial Developer are Copyright (C) 2008
 * the Initial Developer. All Rights Reserved.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */ 


package cdc.gui.components.paramspanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cdc.gui.Configs;
import cdc.gui.components.dynamicanalysis.ChangedConfigurationListener;

public class ComboBoxParamPanelField extends ParamPanelField {

	private class Option {
		private String label;
		private String value;

		public Option(String label, String value) {
			this.label = label;
			this.value = value;
		}

		public String getLabel() {
			return label;
		}

		public String getValue() {
			return value;
		}
		
		public String toString() {
			return getLabel();
		}
	}
	
	private class ActionListenerProxy implements ActionListener {

		private ChangedConfigurationListener listener;
		
		public ActionListenerProxy(ChangedConfigurationListener listener) {
			this.listener = listener;
		}
		
		public void actionPerformed(ActionEvent arg0) {
			listener.configurationChanged();
		}
		
	}
	
	private JLabel paramName;
	private JLabel error;
	private JComboBox combo;
	private JPanel paramPanel;
	private String userLabel;
	private Option[] opts;
	private Map listeners = new HashMap();
	
	public ComboBoxParamPanelField(JComponent parent, String param, String label, String defaultValue, String[] options, String[] labels) {
		opts = new Option[options.length];
		for (int i = 0; i < opts.length; i++) {
			opts[i] = new Option(labels != null ? labels[i] : options[i], options[i]);
		}
		combo = new JComboBox(opts);
		
		paramPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		paramName = new JLabel(label);
		
		error = new JLabel(Configs.errorInfoIcon);
		error.setHorizontalAlignment(JLabel.CENTER);
		error.setVerticalAlignment(JLabel.CENTER);
		error.setVisible(false);
		error.setForeground(Color.red);
		JPanel errorPanel = new JPanel(null);
		error.setPreferredSize(new Dimension(20, 20));
		error.setBounds(0,0,20,20);
		errorPanel.add(error);
		errorPanel.setPreferredSize(new Dimension(20, 20));
		
		//paramName.setPreferredSize(new Dimension(200, (int)paramName.getPreferredSize().getHeight()));
		
		//combo.setPreferredSize(new Dimension(200, (int)combo.getPreferredSize().getHeight()));
		//paramPanel.add(paramName);
		paramPanel.add(errorPanel);
		paramPanel.add(combo);
		
		this.userLabel = label;
		if (defaultValue != null) {
			setValue(defaultValue);
		}
	}

	public void error(String message) {
		if (message != null) {
			error.setVisible(true);
			error.setToolTipText(message);
			combo.setBackground(Color.RED);
		} else {
			error.setVisible(false);
			combo.setBackground(Color.WHITE);
		}
	}

	public JComponent getComponentInputField() {
		return paramPanel;
	}
	
	public JComponent getComponentLabel() {
		return paramName;
	}

	public String getUserLabel() {
		return userLabel;
	}

	public String getValue() {
		if (combo.getSelectedItem() == null) {
			((Option) combo.getSelectedItem()).getValue();
		}
		return ((Option) combo.getSelectedItem()).getValue();
	}

	public void setValue(String val) {
		for (int i = 0; i < opts.length; i++) {
			if (opts[i].getValue().equals(val)) {
				combo.setSelectedIndex(i);
				break;
			}
		}
	}

	
	public void addConfigurationChangeListener(ChangedConfigurationListener configurationListener) {
		ActionListenerProxy proxy = new ActionListenerProxy(configurationListener);
		listeners.put(configurationListener, proxy);
		combo.addActionListener(proxy);
	}

	public void removeConfigurationChangeListener(ChangedConfigurationListener configurationListener) {
		combo.removeActionListener((ActionListener) listeners.get(configurationListener));
	}

}
