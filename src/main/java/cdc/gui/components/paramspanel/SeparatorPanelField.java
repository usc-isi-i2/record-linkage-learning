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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import cdc.gui.components.dynamicanalysis.ChangedConfigurationListener;

public class SeparatorPanelField extends ParamPanelField {

	private static final String LABEL_OTHER = "Other";
	
	private class Listener implements ActionListener {
		private int id;
		public Listener(int id) {
			this.id = id;
		}
		public void actionPerformed(ActionEvent arg0) {
			selectedButton = id;
			if (other != null) {
				other.setEnabled(false);
			}
		}
	}
	private class OtherListener implements ActionListener {
		private int id;
		public OtherListener(int id) {
			this.id = id;
		}
		public void actionPerformed(ActionEvent arg0) {
			selectedButton = id;
			other.setEnabled(true);
		}
	}
	
	public class ItemListenerProxy implements ItemListener {
		private ChangedConfigurationListener listener;
		public ItemListenerProxy(ChangedConfigurationListener propertyChangeListener) {
			this.listener = propertyChangeListener;
		}
		public void itemStateChanged(ItemEvent e) {
			listener.configurationChanged();
		}
	}
	
	public class KeyListenerProxy extends KeyAdapter {
		private ChangedConfigurationListener listener;
		public KeyListenerProxy(ChangedConfigurationListener listener) {
			this.listener = listener;
		}
		public void keyTyped(KeyEvent e) {
			listener.configurationChanged();
		}
	}
	
	private String[] values;
	
	private JLabel paramName;
	private JPanel panel;
	private JRadioButton[] separators;
	private ButtonGroup group = new ButtonGroup();
	private JTextField other;
	private int selectedButton = -1;
	
	private String userLabel;
	
	public SeparatorPanelField(JComponent parent, String param, String label, String defaultValue, String[] values, String[] labels, int x, int y, boolean otherEnabled, ChangedConfigurationListener listener) {
		
		//this.labels = labels;
		this.values = values;
		this.userLabel = label;
		
		panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		paramName = new JLabel(label);
		//paramName.setPreferredSize(new Dimension(200, (int)paramName.getPreferredSize().getHeight()));
		//panel.add(paramName);
		
		separators = new JRadioButton[labels.length + (otherEnabled ? 1 : 0)];
		for (int i = 0; i < separators.length - (otherEnabled ? 1 : 0); i++) {
			separators[i] = new JRadioButton(labels[i]);
			separators[i].addActionListener(new Listener(i));
			if (listener != null) {
				separators[i].addItemListener(new ItemListenerProxy(listener));
			}
		}
		
		if (otherEnabled) {
			separators[separators.length - 1] = new JRadioButton(LABEL_OTHER);
			separators[separators.length - 1].addActionListener(new OtherListener(separators.length - 1));
		}
		
		for (int i = 0; i < separators.length; i++) {
			group.add(separators[i]);
		}
		
		JPanel internalPanel = new JPanel();
		internalPanel.setLayout(new GridBagLayout());
		for (int i = 0; i < separators.length - 1; i++) {
			internalPanel.add(separators[i], getConstant(i));
		}
		
		if (otherEnabled) {
			other = new JTextField();
			other.setPreferredSize(new Dimension(24, 20));
			other.setEnabled(false);
	//		other.setDocument(new PlainDocument() {
	//			public void insertString(int arg0, String arg1, AttributeSet arg2) throws BadLocationException {
	//				if (arg0 + arg1.length() < 2) { 
	//					super.insertString(arg0, arg1, arg2);
	//				}
	//				if (other.getText().length() > 1) {
	//					other.setText(other.getText().substring(0, 1));
	//				}
	//			}
	//		});
			if (listener != null) {
				other.addKeyListener(new KeyListenerProxy(listener));
			}
		}
		JPanel smallPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		smallPanel.add(separators[separators.length - 1]);
		if (otherEnabled) {
			smallPanel.add(other);
		}
		internalPanel.add(smallPanel, getConstant(separators.length - 1));
		
		panel.add(internalPanel);
		
		if (defaultValue != null) {
			setValue(defaultValue);
		}
	}

	private GridBagConstraints getConstant(int i) {
		return new GridBagConstraints(i % 3, i / 3, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
	}

	public JComponent getComponentInputField() {
		return panel;
	}
	
	public JComponent getComponentLabel() {
		return paramName;
	}

	public String getValue() {
		if (selectedButton == separators.length - 1 && other != null) {
//			if (StringUtils.isNullOrEmpty(other.getText())) {
//				return null;
//			}
			return other.getText();
		}
		return values[selectedButton];
	}

	public void setValue(String val) {
		boolean found = false;
		for (int i = 0; i < values.length; i++) {
			if (values[i].equals(val)) {
				selectedButton = i;
				separators[i].setSelected(true);
				found = true;
			}
		}
		if (!found && other != null) {
			selectedButton = separators.length - 1;
			separators[selectedButton].setSelected(true);
			other.setText(val);
		}
	}

	public String getUserLabel() {
		return userLabel;
	}
	
	public void error(String message) {
		for (int i = 0; i < separators.length; i++) {
			separators[i].setForeground(message != null ? Color.RED : Color.WHITE);
		}
	}

	public void removeConfigurationChangeListener(ChangedConfigurationListener configurationListener) {
		throw new RuntimeException("Not yet implemented");
	}
	
	public void addConfigurationChangeListener(ChangedConfigurationListener configurationListener) {
		throw new RuntimeException("Not yet implemented");
	}


}
