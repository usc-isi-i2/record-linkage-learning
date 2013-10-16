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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import cdc.gui.Configs;
import cdc.gui.components.dynamicanalysis.ChangedConfigurationListener;

public class RadioParamPanelField extends ParamPanelField {

	public class ItemListenerProxy implements ItemListener {
		private ChangedConfigurationListener listener;
		public ItemListenerProxy(ChangedConfigurationListener propertyChangeListener) {
			this.listener = propertyChangeListener;
		}
		public void itemStateChanged(ItemEvent e) {
			if (radio.isSelected()) {
				listener.configurationChanged();
			}
		}
	}

	private Map listeners = new HashMap();
	private JRadioButton radio;
	private JComponent input;
	private String userLabel;
	private ComponentFactoryInterface factory;
	private JPanel panel;
	private JLabel error;
	
	public RadioParamPanelField(ComponentFactoryInterface factory, JRadioButton button, JComponent component, String userLabel, String defaultValue) {
		this.radio = button;
		this.input = component;
		this.userLabel = userLabel;
		this.panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		this.factory = factory;
		
		button.setPreferredSize(new Dimension(200, 20));
		factory.setValue(input, defaultValue);
		
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
		
		panel.add(errorPanel);
		panel.add(input);
	}

	public void error(String message) {
		if (message != null) {
			error.setVisible(true);
			error.setToolTipText(message);
			input.setBackground(Color.RED);
		} else {
			error.setVisible(false);
			input.setBackground(Color.WHITE);
		}
	}

	public JComponent getComponentInputField() {
		return panel;
	}
	
	public JComponent getComponentLabel() {
		return radio;
	}

	public String getUserLabel() {
		return this.userLabel;
	}

	public String getValue() {
		if (!radio.isSelected()) {
			return null;
		}
		return factory.retrieveValue(input);
	}

	public void setValue(String val) {
		factory.setValue(input, val);
	}

	public void addConfigurationChangeListener(ChangedConfigurationListener configurationListener) {
		ItemListenerProxy l = new ItemListenerProxy(configurationListener);
		listeners.put(configurationListener, l);
		factory.addPropertyChangeListener(input, configurationListener);
		radio.addItemListener(l);
	}
	
	public void removeConfigurationChangeListener(ChangedConfigurationListener listener) {
		factory.removoPropertyChangeListener(input, listener);
		radio.removeItemListener((ItemListener) listeners.get(listener));
	}

}
