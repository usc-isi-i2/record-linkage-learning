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


package cdc.datamodel.converters.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import cdc.datamodel.DataColumnDefinition;
import cdc.gui.components.dynamicanalysis.ChangedConfigurationListener;
import cdc.gui.components.paramspanel.ParamPanelField;
import cdc.utils.StringUtils;

public class SplitNamesField extends ParamPanelField {
	
	private class RadioListener implements ActionListener {
		public void actionPerformed(ActionEvent arg0) {
			updateSelected();
		}
	}
	
	public class DocumentChangedAction implements DocumentListener {
		private ChangedConfigurationListener listener;
		public DocumentChangedAction(ChangedConfigurationListener listener, JTextField source) {
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
	
	public class ChangeListenerProxy implements ChangeListener {
		private ChangedConfigurationListener listener;
		public ChangeListenerProxy(ChangedConfigurationListener propertyChangeListener) {
			listener = propertyChangeListener;
		}
		public void stateChanged(ChangeEvent e) {
			listener.configurationChanged();
		}
	}
	
	private String userLabel;
	
	private JRadioButton radio1 = new JRadioButton("Generate names");
	private JRadioButton radio2 = new JRadioButton("Manually enter names");
	
	private JLabel paramName;
	private JTextField[] columnNames;
	private JLabel[] labels;
	private JPanel panel;
	private JPanel component;
	private JTextField edit;
	private JComponent parent;
	private JSpinner spinner;
	
	private Map listeners = new HashMap();
	private Map listenersSpinner = new HashMap();
	
	public SplitNamesField(JComponent parent, String param, String label, String defaultValue) {
		this.userLabel = label;
		this.parent = parent;
		panel = new JPanel();
		
		ButtonGroup group = new ButtonGroup();
		group.add(radio1);
		group.add(radio2);
		
		radio1.addActionListener(new RadioListener());
		radio2.addActionListener(new RadioListener());
		
		edit = new JTextField(defaultValue);
		edit.setPreferredSize(new Dimension(100, 20));
		edit.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent arg0) {
				updateNames();
			}
			public void insertUpdate(DocumentEvent arg0) {
				updateNames();
			}
			public void removeUpdate(DocumentEvent arg0) {
				updateNames();
			}
		});
		
		JPanel mainPanel = new JPanel(new GridBagLayout());
		
		JPanel size = new JPanel(new FlowLayout(FlowLayout.LEFT));
		size.add(new JLabel("Number of output attributes:"));
		spinner = new JSpinner(new SpinnerNumberModel(2, 2, 20, 1));
		spinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				int size = ((Integer)((JSpinner)arg0.getSource()).getValue()).intValue();
				createComponents(size);
			}
		});
		size.add(spinner);
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.PAGE_START;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 0.0;
		c.gridx = 0;
		c.gridy = 0;
		mainPanel.add(size, c);
		
		JPanel panel1 = new JPanel(new GridBagLayout());
		panel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.PAGE_START;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1.0;
		c.gridx = 0;
		c.gridy = 1;
		mainPanel.add(panel1, c);
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.PAGE_START;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1.0;
		c.gridx = 1;
		c.gridy = 1;
		mainPanel.add(panel, c);
		
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.PAGE_START;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 0.0;
		c.gridx = 0;
		c.gridy = 0;
		panel1.add(radio1, c);
		
		JPanel innerPanel = new JPanel();
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.PAGE_START;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1.0;
		c.gridx = 0;
		c.gridy = 1;
		//c.insets = new Insets(0, 10, 5, 0);
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		p.add(new JLabel("Name: "));
		p.add(edit);
		innerPanel.add(p);
		JScrollPane scroll = new JScrollPane(innerPanel);
		scroll.setPreferredSize(new Dimension(200, 120));
		panel1.add(scroll, c);
		
		createComponents(2);
		
		component = new JPanel(new FlowLayout(FlowLayout.LEFT));
		paramName = new JLabel(label);
		
		//component.add(paramName);
		component.add(mainPanel);
		
		setValue(defaultValue);
		
	}

	private void diffNames(String[] strs) {
		edit.setText("");
		radio2.setSelected(true);
		createComponents(strs.length);
		for (int i = 0; i < strs.length; i++) {
			columnNames[i].setText(strs[i]);
		}
		updateSelected();
	}

	private void updateSelected() {
		if (radio1.isSelected()) {
			for (int i = 0; i < columnNames.length; i++) {
				columnNames[i].setEnabled(false);
			}
			edit.setEnabled(true);
		} else {
			for (int i = 0; i < columnNames.length; i++) {
				columnNames[i].setEnabled(true);
			}
			edit.setEnabled(false);
		}
	}

	private void updateNames() {
		for (int i = 0; i < columnNames.length; i++) {
			columnNames[i].setText(edit.getText() + "_" + i);
		}
	}

	private void createComponents(int cols) {
		panel.removeAll();
		
		for (Iterator iterator = listeners.keySet().iterator(); iterator.hasNext();) {
			ChangedConfigurationListener l = (ChangedConfigurationListener) iterator.next();
			removeConfigurationChangeListener(l);
		}
		
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.PAGE_START;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1.0;
		c.gridx = 0;
		c.gridy = 0;
		panel.add(radio2, c);
		
		JPanel innerPanel = new JPanel(new GridBagLayout());
		JScrollPane scroll = new JScrollPane(innerPanel);
		scroll.setPreferredSize(new Dimension(200, 120));
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.PAGE_START;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 1;
		panel.add(scroll, c);
		
		JTextField[] newCols = new JTextField[cols];
		labels = new JLabel[cols];
		for (int i = 0; i < newCols.length; i++) {
			newCols[i] = new JTextField();
			newCols[i].setPreferredSize(new Dimension(100, 20));
			if (columnNames != null && i < columnNames.length) {
				newCols[i].setText(columnNames[i].getText());
			} else if (radio1.isSelected()) {
				newCols[i].setText(edit.getText() + "_" + i);
			}
			labels[i] = new JLabel("Column " + (i + 1) + ": ");
			labels[i].setPreferredSize(new Dimension(66, 20));
			
			JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
			p.add(labels[i]);
			p.add(newCols[i]);
			c = new GridBagConstraints();
			c.anchor = GridBagConstraints.PAGE_START;
			c.fill = GridBagConstraints.BOTH;
			c.gridx = 0;
			c.gridy = i;
			innerPanel.add(p, c);
		}
		
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = newCols.length + 1;
		c.weighty = 1.0;
		innerPanel.add(Box.createRigidArea(new Dimension(5, 5)), c);
		
		columnNames = newCols;
		updateSelected();
		panel.updateUI();
		parent.updateUI();
		
		for (Iterator iterator = listeners.keySet().iterator(); iterator.hasNext();) {
			ChangedConfigurationListener l = (ChangedConfigurationListener) iterator.next();
			addConfigurationChangeListener(l);
		}
	}

	public void addConfigurationChangeListener(ChangedConfigurationListener configurationListener) {
		List l = new ArrayList();
		DocumentListener listener = new DocumentChangedAction(configurationListener, edit);
		l.add(listener);
		
		edit.getDocument().addDocumentListener(listener);
		for (int i = 0; i < columnNames.length; i++) {
			listener = new DocumentChangedAction(configurationListener, columnNames[i]);
			l.add(listener);
			columnNames[i].getDocument().addDocumentListener(listener);
		}
		listeners.put(configurationListener, l);
		ChangeListenerProxy proxy = new ChangeListenerProxy(configurationListener);
		listenersSpinner.put(configurationListener, proxy);
		spinner.addChangeListener(proxy);
	}

	public void error(String message) {
		throw new RuntimeException("Not supprted. Use SplitNamesValidator instead!");
	}

	public JComponent getComponentInputField() {
		return component;
	}
	
	public JComponent getComponentLabel() {
		return paramName;
	}

	public String getUserLabel() {
		return userLabel;
	}

	public String getValue() {
		String s = "";
		for (int i = 0; i < columnNames.length; i++) {
			if (i != 0) {
				s += ",";
			}
			s += columnNames[i].getText();
		}
		return s;
	}

	public void setValue(String val) {
		String[] strs = val.split(",");
		spinner.setValue(new Integer(strs.length));
		if (strs.length == 0) {
			return;
		}
		boolean allOk = true;
		Pattern p = Pattern.compile("_[0-9]+$");
		int n = -1;
		for (int i = 0; i < strs.length; i++) {
			Matcher m = p.matcher(strs[i]);
			if (!m.find()) {
				allOk = false;
				break;
			}
			if (i == 0) {
				n = m.start();
			} else {
				if (n != m.start()) {
					allOk = false;
					break;
				}
			}
		}
		if (!allOk) {
			diffNames(strs);
			return;
		}
		Matcher m = p.matcher(strs[0]);
		m.find();
		int rootEnd = m.start();
		String root = strs[0].substring(0, rootEnd);
		edit.setText(root);
		radio1.setSelected(true);
		updateNames();
		updateSelected();
	}

	public void removeConfigurationChangeListener(ChangedConfigurationListener configurationListener) {
		List toRemove = (List) listeners.get(configurationListener);
		for (Iterator iterator = toRemove.iterator(); iterator.hasNext();) {
			DocumentListener object = (DocumentListener) iterator.next();
			edit.getDocument().removeDocumentListener(object);
			for (int i = 0; i < columnNames.length; i++) {
				columnNames[i].getDocument().removeDocumentListener(object);
			}
		}
		ChangeListenerProxy proxy = (ChangeListenerProxy)listenersSpinner.get(configurationListener);
		spinner.removeChangeListener(proxy);
	}

	public boolean doValidation() {
		edit.setBackground(Color.white);
		for (int i = 0; i < columnNames.length; i++) {
			columnNames[i].setBackground(Color.white);
		}
		if (radio1.isSelected() && (StringUtils.isNullOrEmpty(edit.getText()) || wrongName(edit.getText()))) {
			edit.setBackground(Color.red);
			return false;
		} else if (radio2.isSelected()) {
			boolean allOK = true;
			for (int i = 0; i < columnNames.length; i++) {
				if (StringUtils.isNullOrEmpty(columnNames[i].getText()) || wrongName(columnNames[i].getText())) {
					allOK = false;
					columnNames[i].setBackground(Color.red);
				}
			}
			if (!allOK) {
				return false;
			}
		}
		return true;
	}

	private boolean wrongName(String text) {
		boolean errorValue = !text.equals(DataColumnDefinition.normalizeColumnName(text));
		if (errorValue) {
			String message = "Column names can only use letters, numbers or underscore character.";
			JOptionPane.showMessageDialog(panel, message);
		}
		return errorValue;
	}

}
