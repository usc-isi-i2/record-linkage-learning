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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cdc.gui.components.dynamicanalysis.ChangedConfigurationListener;
import cdc.gui.components.paramspanel.ParamPanelField;

public class TrimConverterField extends ParamPanelField {
	
	public static final int OPTION_CUT = 1;
	public static final int OPTION_LEAVE = 2;
	
	public class ItemListenerProxy implements ItemListener {
		private ChangedConfigurationListener listener;
		public ItemListenerProxy(ChangedConfigurationListener propertyChangeListener) {
			this.listener = propertyChangeListener;
		}
		public void itemStateChanged(ItemEvent e) {
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
	
	public class ActionListenerProxy implements ActionListener {
		private ChangedConfigurationListener listener;
		public ActionListenerProxy(ChangedConfigurationListener propertyChangeListener) {
			listener = propertyChangeListener;
		}
		public void actionPerformed(ActionEvent e) {
			listener.configurationChanged();
		}
		
	}
	
	private JCheckBox enable;
	private JRadioButton radioCut;
	private JRadioButton radioLeave;
	private ButtonGroup group;
	
	private JSpinner cutNumber;
	private JSpinner leaveNumber;
	
	private JLabel labelCut = new JLabel("Cut");
	private JLabel labelLeave = new JLabel("Leave");
	private JLabel labelCut1 = new JLabel(" characters");
	private JLabel labelLeave1 = new JLabel(" characters");
	private JLabel paramName;
	
	private JPanel panel;
	private String userLabel;
	
	public TrimConverterField(JComponent parent, String param, String label, String defaultValue, ChangedConfigurationListener listener) {
		this.userLabel = label;
		JPanel filler = new JPanel();
		filler.setMinimumSize(new Dimension(40,20));
		enable = new JCheckBox("Enable");
		radioCut = new JRadioButton();
		radioLeave = new JRadioButton();
		radioCut.setSelected(true);
		radioLeave.setSelected(false);
		group = new ButtonGroup();
		group.add(radioCut);
		group.add(radioLeave);
		cutNumber = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
		leaveNumber = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
		
		enable.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (enable.isSelected()) {
					radioCut.setEnabled(true);
					radioLeave.setEnabled(true);
					if (radioCut.isSelected()) {
						cutNumber.setEnabled(true);
						labelCut.setEnabled(true);
						labelCut1.setEnabled(true);
						labelLeave.setEnabled(false);
						labelLeave1.setEnabled(false);
					} else {
						leaveNumber.setEnabled(true);
						labelCut.setEnabled(false);
						labelCut1.setEnabled(false);
						labelLeave.setEnabled(true);
						labelLeave1.setEnabled(true);
					}
				} else {
					radioCut.setEnabled(false);
					radioLeave.setEnabled(false);
					cutNumber.setEnabled(false);
					leaveNumber.setEnabled(false);
					labelCut.setEnabled(false);
					labelCut1.setEnabled(false);
					labelLeave.setEnabled(false);
					labelLeave1.setEnabled(false);
				}
			}
		});
		radioCut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (radioCut.isSelected()) {
					leaveNumber.setEnabled(false);
					cutNumber.setEnabled(true);
					labelCut.setEnabled(true);
					labelCut1.setEnabled(true);
					labelLeave.setEnabled(false);
					labelLeave1.setEnabled(false);
				}
			}
		});
		radioLeave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (radioLeave.isSelected()) {
					leaveNumber.setEnabled(true);
					cutNumber.setEnabled(false);
					labelCut.setEnabled(false);
					labelCut1.setEnabled(false);
					labelLeave.setEnabled(true);
					labelLeave1.setEnabled(true);
				}
			}
		});
		
		paramName = new JLabel(label);
		//paramName.setPreferredSize(new Dimension(200, (int)paramName.getPreferredSize().getHeight()));
		
		panel = new JPanel();
		panel.setLayout(new FlowLayout());
		//panel.add(paramName);
		panel.add(enable);
		panel.add(filler);
		panel.add(radioCut);
		panel.add(labelCut);
		panel.add(cutNumber);
		panel.add(labelCut1);
		panel.add(filler);
		panel.add(radioLeave);
		panel.add(labelLeave);
		panel.add(leaveNumber);
		panel.add(labelLeave1);
		setValue(defaultValue);
		
		if (listener != null) {
			radioCut.addItemListener(new ItemListenerProxy(listener));
			radioLeave.addItemListener(new ItemListenerProxy(listener));
			leaveNumber.addChangeListener(new ChangeListenerProxy(listener));
			cutNumber.addChangeListener(new ChangeListenerProxy(listener));
			enable.addActionListener(new ActionListenerProxy(listener));
		}
	}
	
	public void addConfigurationChangeListener(ChangedConfigurationListener configurationListener) {
		throw new RuntimeException("Not implemented");
	}

	public void error(String message) {
		// TODO Auto-generated method stub
		
	}

	public JComponent getComponentInputField() {
		return panel;
	}
	
	public JComponent getComponentLabel() {
		return paramName;
	}

	public String getUserLabel() {
		return userLabel;
	}

	public String getValue() {
		return (enable.isSelected() ? "1" : "0") + "," + 
			(radioCut.isSelected() ? OPTION_CUT : OPTION_LEAVE) + "," + 
			(radioCut.isSelected() ? cutNumber.getValue() : leaveNumber.getValue());
	}

	public void setValue(String val) {
		String[] values = val.split(",");
		if (Integer.parseInt(values[0]) != 0) {
			enable.setSelected(true);
		} else {
			enable.setSelected(false);
		}
		int option = Integer.parseInt(values[1]);
		int length = Integer.parseInt(values[2]);
		cutNumber.setValue(new Integer(length));
		leaveNumber.setValue(new Integer(length));
		if (Integer.parseInt(values[0]) != 0) {
			if (option == OPTION_CUT) {
				radioCut.setSelected(true);
				cutNumber.setEnabled(true);
				leaveNumber.setEnabled(false);
				labelCut.setEnabled(true);
				labelCut1.setEnabled(true);
				labelLeave.setEnabled(false);
				labelLeave1.setEnabled(false);
			} else {
				radioLeave.setSelected(true);
				cutNumber.setEnabled(false);
				leaveNumber.setEnabled(true);
				labelCut.setEnabled(false);
				labelCut1.setEnabled(false);
				labelLeave.setEnabled(true);
				labelLeave1.setEnabled(true);
			}
			radioCut.setEnabled(true);
			radioLeave.setEnabled(true);
		} else {
			radioCut.setEnabled(false);
			radioLeave.setEnabled(false);
			cutNumber.setEnabled(false);
			leaveNumber.setEnabled(false);
			labelCut.setEnabled(false);
			labelCut1.setEnabled(false);
			labelLeave.setEnabled(false);
			labelLeave1.setEnabled(false);
		}
	}

	public void removeConfigurationChangeListener(ChangedConfigurationListener configurationListener) {
		throw new RuntimeException("Not implemented");
	}

}
