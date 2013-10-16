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


package cdc.impl.em.actions;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import cdc.components.AbstractJoinCondition;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.impl.join.blocking.EqualityBlockingFunction;
import cdc.impl.join.blocking.BlockingFunction;
import cdc.impl.join.blocking.SoundexBlockingFunction;

public class ConfigureBlockingMethod extends WizardAction {

	private static final String EXPLAIN_EQUALITY = "<html>Records which have exactly the same value" +
			" for blocking attribute are classified to the same block. Recommended for such fields as zip code or " +
			"date</html>";
	private static final String EXPLAIN_SOUNDEX = "<html>Records which have the same Soundex code for blocking" +
			" attribute will be classified to the same block. Not recommended for attributes other than names.</html>";
	
	private class ComboPosition {
		String label;
		int position;
		public String toString() {
			return label;
		}
	}
	
	private ComboPosition[] columns;
	
	private JComboBox comboColumns;
	private JRadioButton button1 = new JRadioButton("Block using equality function");
	private JRadioButton button2 = new JRadioButton("Block using Soundex function");
	private JSpinner length = new JSpinner(new SpinnerNumberModel(5, 3, 20, 1));
	private JPanel panel;
	private int row;
	
	public ConfigureBlockingMethod(AbstractJoinCondition condition) {
		
		panel = new JPanel(new GridBagLayout());
		ButtonGroup group = new ButtonGroup();
		group.add(button1);
		group.add(button2);
		
		
		columns = new ComboPosition[condition.getLeftJoinColumns().length];
		for (int i = 0; i < columns.length; i++) {
			columns[i] = new ComboPosition();
			columns[i].label = condition.getLeftJoinColumns()[i] + 
			" and " + condition.getRightJoinColumns()[i];
			columns[i].position = i;
		}
		comboColumns = new JComboBox(columns);
		
		JPanel panel00 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel00.add(new JLabel("Attributes:"));
		panel00.add(comboColumns);
		
		GridBagConstraints c = getNextConstraints();
		panel.add(new JLabel("Choose blocking attribute:"), c);
		c = getNextConstraints();
		c.insets = new Insets(0, 40, 0, 0);
		panel.add(panel00, c);
		
		EnableDisableListener eDListener1 = new EnableDisableListener();
		button1.addActionListener(eDListener1);
		JLabel explain1 = (JLabel) eDListener1.addComponent(new JLabel(EXPLAIN_EQUALITY));
		
		panel.add(new JLabel("Choose blocking function:"), getNextConstraints());
		c = getNextConstraints();
		panel.add(button1, c);
		c = getNextConstraints();
		c.insets = new Insets(0, 40, 0, 0);
		panel.add(explain1, c);
		
		EnableDisableListener eDListener2 = new EnableDisableListener();
		button2.addActionListener(eDListener2);
		JLabel explain2 = (JLabel) eDListener2.addComponent(new JLabel(EXPLAIN_SOUNDEX));
		eDListener2.addComponent(explain2);
		JPanel flow3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		flow3.add(eDListener2.addComponent(new JLabel("Soundex length:")));
		flow3.add(eDListener2.addComponent(length));
		
		c = getNextConstraints();
		panel.add(button2, c);
		c = getNextConstraints();
		c.insets = new Insets(0, 40, 0, 0);
		panel.add(explain2, c);
		c = getNextConstraints();
		c.insets = new Insets(0, 40, 0, 0);
		panel.add(flow3, c);
		
		eDListener1.setOtherListeners(new EnableDisableListener[] {eDListener2});
		eDListener2.setOtherListeners(new EnableDisableListener[] {eDListener1});
		button2.setSelected(true);
		eDListener2.actionPerformed(new ActionEvent(button2, 0, null));
		
	}
	
	private GridBagConstraints getNextConstraints() {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = row ++;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		return c;
	}
	
	public JPanel beginStep(AbstractWizard wizard) {
		return panel;
	}

	public boolean endStep(AbstractWizard wizard) {
		return true;
	}
	
	public BlockingFunction getHashingFunction(DataColumnDefinition[][] columns) {
		if (button1.isSelected()) {
			return new EqualityBlockingFunction(columns);
		} else {
			return new SoundexBlockingFunction(columns, Integer.parseInt(String.valueOf(length.getValue())));
		}
	}
	
	public int getBlockingAttribute() {
		return ((ComboPosition)comboColumns.getSelectedItem()).position;
	}
	
	public void setSize(int width, int height) {
	}

	public void dispose() {
		this.button1 = null;
		this.button2 = null;
		this.columns = null;
		this.comboColumns = null;
		this.length = null;
		this.panel = null;
	}

}
