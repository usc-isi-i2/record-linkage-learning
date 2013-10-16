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


package cdc.gui.components.uicomponents;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import cdc.components.AbstractDistance;
import cdc.components.AbstractStringDistance;
import cdc.impl.join.blocking.BlockingFunctionFactory;
import cdc.impl.join.blocking.BlockingFunctionFactory.BlockingFunctionDescriptor;

public class BlockingAttributePanel extends JPanel {
	
	private JComboBox comboBox;
	private JRadioButton equality = new JRadioButton("Value of blocking attribute");
	private JRadioButton soundexCode = new JRadioButton("Soundex code of blocking attribute");
	private JRadioButton prefix = new JRadioButton("Prefix of blocking attribute");
	
	private JTextField soundexLength = new JTextField("5", 5);
	private JTextField prefixLength = new JTextField("4", 5);
	
	private AbstractDistance[] distances;
	private JLabel soundexLenLabel = new JLabel("Soundex code length: ");
	private JLabel prefixLenLabel = new JLabel("Prefix length: ");
	
	public BlockingAttributePanel(String[] columns, AbstractDistance[] distances) {
		this(columns, distances, null);
	}
	
	public BlockingAttributePanel(String[] columns, AbstractDistance[] distances, String information) {
		
		this.distances = distances;
		
		comboBox = new JComboBox(columns);
		ButtonGroup group = new ButtonGroup();
		group.add(equality);
		group.add(prefix);
		group.add(soundexCode);
		
		setLayout(new GridBagLayout());
		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		p1.setBorder(BorderFactory.createTitledBorder("Blocking attribute"));
		p1.add(new JLabel("Blocking attribute:"));
		p1.add(comboBox);
		add(p1, new GridBagConstraints(0, 0, 2, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(10, 0, 20, 0), 0, 0));
		
		JPanel p2 = new JPanel(new GridBagLayout());
		p2.setBorder(BorderFactory.createTitledBorder("Blocking method"));
		p2.add(new JLabel("Block records that have the same: "), new GridBagConstraints(0, 0, 2, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(10, 10, 0, 0), 0, 0));
		p2.add(equality, new GridBagConstraints(0, 3, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 30, 10, 0), 0, 0));
		
		
		p2.add(soundexCode, new GridBagConstraints(0, 4, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 30, 0, 0), 0, 0));
		p2.add(soundexLenLabel, new GridBagConstraints(0, 5, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 80, 0, 0), 0, 0));
		p2.add(soundexLength, new GridBagConstraints(1, 5, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		
		
		p2.add(prefix, new GridBagConstraints(0, 6, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 30, 0, 0), 0, 0));
		p2.add(prefixLenLabel, new GridBagConstraints(0, 7, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 80, 0, 0), 0, 0));
		p2.add(prefixLength, new GridBagConstraints(1, 7, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		
		int currRow = 8;
		if (information != null) {
			p2.add(new JPanel(), new GridBagConstraints(0, currRow++, 3, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(20, 0, 20, 0), 0, 0));
			p2.add(new InformationPanel(information), new GridBagConstraints(0, currRow++, 3, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		}
		
		p2.add(new JPanel(), new GridBagConstraints(0, currRow, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		add(p2, new GridBagConstraints(0, 1, 2, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 20, 0), 0, 0));
	
		
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				makeThingsEnabled();
			}
		});
		
		soundexCode.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				makeThingsEnabled();
			}
		});
		
		prefix.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				makeThingsEnabled();
			}
		});
		
		comboBox.setSelectedIndex(0);
		equality.setSelected(true);
		makeThingsEnabled();
	}
	
	private void makeThingsEnabled() {
		int selected = comboBox.getSelectedIndex();
		if (distances[selected] instanceof AbstractStringDistance) {
			equality.setEnabled(false);
			if (equality.isSelected()) {
				soundexCode.setSelected(true);
			}
			equality.setEnabled(false);
			soundexCode.setEnabled(true);
			prefix.setEnabled(true);
			enableTextFields();
		} else {
			equality.setSelected(true);
			equality.setEnabled(false);
			soundexCode.setEnabled(false);
			prefix.setEnabled(false);
			enableTextFields();
		}
	}
	
	private void enableTextFields() {
		if (!equality.isSelected()) {
			soundexLength.setEnabled(soundexCode.isSelected());
			prefixLength.setEnabled(!soundexCode.isSelected());
			soundexLenLabel.setEnabled(soundexCode.isSelected());
			prefixLenLabel.setEnabled(!soundexCode.isSelected());
		} else {
			soundexLength.setEnabled(false);
			prefixLength.setEnabled(false);
			soundexLenLabel.setEnabled(false);
			prefixLenLabel.setEnabled(false);
		}
	}
	
	public void setBlockingAttribute(int itemId) {
		this.comboBox.setSelectedIndex(itemId);
		enableTextFields();
	}
	
	public int getBlockingAttributeId() {
		return comboBox.getSelectedIndex();
	}
	
	public String getBlockingFunction() {
		if (equality.isSelected()) {
			return BlockingFunctionFactory.EQUALITY;
		} else if (prefix.isSelected()) {
			return BlockingFunctionFactory.PREFIX + "(" + prefixLength.getText() + ")";
		} else {
			return BlockingFunctionFactory.SOUNDEX + "(" + soundexLength.getText() + ")";
		} 
	}
	
	public void setBlockingFunction(String function) {
		BlockingFunctionDescriptor descriptor = BlockingFunctionFactory.parseBlockingFunctionDescriptor(function);
		if (descriptor.function.equals(BlockingFunctionFactory.EQUALITY)) {
			equality.setSelected(true);
		} else if (descriptor.function.equals(BlockingFunctionFactory.SOUNDEX)) {
			soundexCode.setSelected(true);
			soundexLength.setText(String.valueOf(descriptor.arguments[0]));
		} else {
			prefix.setSelected(true);
			prefixLength.setText(String.valueOf(descriptor.arguments[0]));
		}
		enableTextFields();
	}
		
}
