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


package cdc.gui.wizards.specific.actions.strata;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cdc.components.AtomicCondition;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.DialogListener;

public class StratumAttributePanel extends JPanel implements DialogListener {
	
	private JComboBox columns;
	private JComboBox conditions;
	private JTextField value;
	
	public StratumAttributePanel(DataColumnDefinition[] availableColumns) {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		columns = new JComboBox(availableColumns);
		//columns.setPreferredSize(new Dimension(columns.getPreferredSize().width, 20));
		conditions = new JComboBox(AtomicCondition.conds);
		//conditions.setPreferredSize(new Dimension(conditions.getPreferredSize().width, 20));
		value = new JTextField(10);
		value.setPreferredSize(new Dimension(150, 20));
		
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel label = new JLabel("Column:");
		label.setPreferredSize(new Dimension(100, 20));
		panel.add(label);
		panel.add(columns);
		add(panel);
		
		panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		label = new JLabel("Condition:");
		label.setPreferredSize(new Dimension(100, 20));
		panel.add(label);
		panel.add(conditions);
		add(panel);
		
		panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		label = new JLabel("Value:");
		label.setPreferredSize(new Dimension(100, 20));
		panel.add(label);
		panel.add(value);
		add(panel);
	}

	public Object[] getData() {
		return new Object[] {columns.getSelectedItem(), conditions.getSelectedItem(), value.getText()};
	}

	public void restore(Object[] object) {
		int n = -1;
		for (int i = 0; i < columns.getItemCount(); i++) {
			if (columns.getItemAt(i).equals(object[0])) {
				n = i;
				break;
			}
		}
		columns.setSelectedIndex(n);
		conditions.setSelectedItem(object[1]);
		value.setText((String)object[2]);
	}

	public void cancelPressed(JDialog parent) {}

	public boolean okPressed(JDialog parent) {
		if (!(conditions.getSelectedItem().equals("==") || conditions.getSelectedItem().equals("!="))) {
			try {
				Double.parseDouble(value.getText());
				return true;
			} catch (NumberFormatException e) {
				value.setBackground(Color.red);
				value.setToolTipText("Expected numeric value for this type of condition");
				return false;
			}
		}
		return true;
	}

	public void windowClosing(JDialog parent) {
		// TODO Auto-generated method stub
		
	}
	
}
