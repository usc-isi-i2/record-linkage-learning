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


package cdc.impl.distance.parampanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import cdc.gui.Configs;
import cdc.gui.components.dynamicanalysis.ChangedConfigurationListener;
import cdc.gui.components.paramspanel.ParamPanelField;

public class DateFormatPanelField extends ParamPanelField {

	public class DocumentChangedAction implements DocumentListener {
		private ChangedConfigurationListener listener;
		public DocumentChangedAction(ChangedConfigurationListener listener) {
				this.listener = listener;
		}
		public void changedUpdate(DocumentEvent arg0) {
			listener.configurationChanged();
		}
		public void insertUpdate(DocumentEvent arg0) {
			listener.configurationChanged();
		}
		public void removeUpdate(DocumentEvent arg0) {
			listener.configurationChanged();
		}
	}

	private Map listeners = new HashMap();
	private String label;

	private JLabel paramName;
	private JLabel active = new JLabel(Configs.busyIcon);
	private JTextField format;
	private JComboBox suggestions;
	
	private JPanel component;
	
	public DateFormatPanelField(JComponent parent, String param, String label, String defaultValue) {
		this.format = new JTextField(defaultValue);
		this.format.setPreferredSize(new Dimension(180, 20));
		this.label = label;
		this.suggestions = new JComboBox(new String[] {"No suggestions available"});
		this.suggestions.setEnabled(false);
		this.suggestions.setPreferredSize(new Dimension(180, 20));
		this.suggestions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (suggestions.getSelectedItem() != null && suggestions.getSelectedIndex() != 0) {
					format.setText(suggestions.getSelectedItem().toString());
				}
			}
		});
		
		component = new JPanel(new FlowLayout(FlowLayout.LEFT));
		paramName = new JLabel(label);
		//jLabel.setPreferredSize(new Dimension(200, 20));
		//component.add(jLabel);
		
		JPanel optionPanel = new JPanel(new GridBagLayout());
		//optionPanel.setPreferredSize(new Dimension(200, 50));
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		optionPanel.add(format, c);
		
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		active.setVisible(false);
		active.setPreferredSize(new Dimension(20, 20));
		optionPanel.add(active, c);
		
//		c = new GridBagConstraints();
//		c.gridx = 0;
//		c.gridy = 1;
//		optionPanel.add(new JLabel("Suggestions:"), c);
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		optionPanel.add(suggestions, c);
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 1;
		optionPanel.add(Box.createRigidArea(new Dimension(20, 20)), c);
		component.add(optionPanel);
	}

	public void error(String message) {
		format.setBackground(Color.red);
	}

	public JComponent getComponentInputField() {
		return component;
	}
	
	public JComponent getComponentLabel() {
		return paramName;
	}

	public String getUserLabel() {
		return label;
	}

	public String getValue() {
		return format.getText();
	}
	
	public void setValue(String val) {
		format.setText(val);
	}

	public void addConfigurationChangeListener(ChangedConfigurationListener configurationListener) {
		DocumentListener l = new DocumentChangedAction(configurationListener);
		listeners.put(configurationListener, l);
		this.format.getDocument().addDocumentListener(l);
	}
	
	public void removeConfigurationChangeListener(ChangedConfigurationListener listener) {
		this.format.getDocument().removeDocumentListener((DocumentListener) listeners.remove(listener));
	}
	
	public void setSuggestions(String[] suggestions) {
		active.setVisible(false);
		this.suggestions.removeAllItems();
		if (suggestions == null || suggestions.length == 0) {
			this.suggestions.addItem("No suggestions available");
			this.suggestions.setEnabled(false);
		} else {
			this.suggestions.addItem("Select...");
			for (int i = 0; i < suggestions.length; i++) {
				this.suggestions.addItem(suggestions[i]);
			}
			this.suggestions.setEnabled(true);
		}
	}
	
	public void startWork() {
		active.setVisible(true);
		suggestions.setEnabled(false);
	}

}
