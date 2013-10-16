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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import cdc.components.AbstractDataSource;
import cdc.components.AtomicCondition;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.MainFrame;
import cdc.gui.OptionDialog;
import cdc.gui.components.table.TablePanel;
import cdc.impl.join.strata.DataStratum;

public class StratumInnerPanel extends JPanel {

	private JTabbedPane parent;
	private JTextField title;
	private JLabel titleLabel;
	private TablePanel stratumConfigA;
	private TablePanel stratumConfigB;
	
	private DataColumnDefinition[] columnsA;
	private DataColumnDefinition[] columnsB;
	
	public StratumInnerPanel(JTabbedPane tabs, String strTitle, AbstractDataSource sourceA, AbstractDataSource sourceB) {
		super(new GridBagLayout());
		this.parent = tabs;
		
		JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		titleLabel = new JLabel("Strata name:");
		title = new JTextField();
		title.setPreferredSize(new Dimension(150, 20));
		title.setText(strTitle);
		titlePanel.add(titleLabel);
		titlePanel.add(title);
		
		title.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent arg0) {
				refreshName();
			}
			public void insertUpdate(DocumentEvent arg0) {
				refreshName();
			}
			public void removeUpdate(DocumentEvent arg0) {
				refreshName();
			}
		});
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.BOTH;
		add(titlePanel, c);
		
		columnsA = sourceA.getDataModel().getSortedOutputColumns();
		columnsB = sourceB.getDataModel().getSortedOutputColumns();
		
		stratumConfigA = new TablePanel(new String[] {"Atribute", "Condition", "Value"}, true, TablePanel.BUTTONS_TOP);
		stratumConfigB = new TablePanel(new String[] {"Atribute", "Condition", "Value"}, true, TablePanel.BUTTONS_TOP);
		JPanel configPanel = new JPanel(new GridBagLayout());
		JPanel testA = new JPanel(new GridBagLayout());
		testA.setBorder(BorderFactory.createTitledBorder("Source '" + sourceA.getSourceName() + "' stratum condition"));
		JPanel testB = new JPanel(new GridBagLayout());
		testB.setBorder(BorderFactory.createTitledBorder("Source '" + sourceB.getSourceName() + "' stratum condition"));
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		configPanel.add(testA, c);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		configPanel.add(testB, c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		testA.add(stratumConfigA, c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		testB.add(stratumConfigB, c);
		//stratumConfigA.setPreferredSize(new Dimension(290, 300));
		//stratumConfigB.setPreferredSize(new Dimension(290, 300));
		
		stratumConfigA.addAddButtonListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				OptionDialog dialog = new OptionDialog(MainFrame.main, "Add new stratum attribute");
				StratumAttributePanel panel = new StratumAttributePanel(columnsA);
				dialog.setMainPanel(panel);
				if (dialog.getResult() == OptionDialog.RESULT_OK) {
					stratumConfigA.addRow(panel.getData());
				}
			}
		});
		
		stratumConfigB.addAddButtonListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				OptionDialog dialog = new OptionDialog(MainFrame.main, "Add new stratum attribute");
				StratumAttributePanel panel = new StratumAttributePanel(columnsB);
				dialog.setMainPanel(panel);
				if (dialog.getResult() == OptionDialog.RESULT_OK) {
					stratumConfigB.addRow(panel.getData());
				}
			}
		});
		stratumConfigA.addEditButtonListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				OptionDialog dialog = new OptionDialog(MainFrame.main, "Edit stratum attribute");
				StratumAttributePanel panel = new StratumAttributePanel(columnsA);
				panel.restore((Object[])stratumConfigA.getSelectedRows()[0]);
				dialog.setMainPanel(panel);
				if (dialog.getResult() == OptionDialog.RESULT_OK) {
					stratumConfigA.replaceRow(stratumConfigA.getSelectedRowId()[0], panel.getData());
				}
			}
		});
		stratumConfigB.addEditButtonListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				OptionDialog dialog = new OptionDialog(MainFrame.main, "Edit stratum attribute");
				StratumAttributePanel panel = new StratumAttributePanel(columnsB);
				panel.restore((Object[])stratumConfigB.getSelectedRows()[0]);
				dialog.setMainPanel(panel);
				if (dialog.getResult() == OptionDialog.RESULT_OK) {
					stratumConfigB.replaceRow(stratumConfigB.getSelectedRowId()[0], panel.getData());
				}
			}
		});
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		c.weightx = 1;
		add(configPanel, c);
	}
	
	public StratumInnerPanel(JTabbedPane tabs, String name, AbstractDataSource sourceA, AbstractDataSource sourceB, DataStratum stratum) {
		this(tabs, name, sourceA, sourceB);
		AtomicCondition[] condsA = stratum.getSourceA();
		for (int i = 0; i < condsA.length; i++) {
			stratumConfigA.addRow(new Object[] {condsA[i].getColumn(), condsA[i].getCondition(), condsA[i].getValue()});
		}
		AtomicCondition[] condsB = stratum.getSourceB();
		for (int i = 0; i < condsB.length; i++) {
			stratumConfigB.addRow(new Object[] {condsB[i].getColumn(), condsB[i].getCondition(), condsB[i].getValue()});
		}
	}

	private void refreshName() {
		((StrataChooser.TabHeader)parent.getTabComponentAt(parent.indexOfComponent(this))).setTitle(title.getText());
	}
	
	public void setEnabled(boolean arg0) {
		title.setEnabled(arg0);
		titleLabel.setEnabled(arg0);
		stratumConfigA.setEnabled(arg0);
		stratumConfigB.setEnabled(arg0);
	}

	public String getStratumName() {
		return title.getText();
	}
	
	public AtomicCondition[] getLeftStratum() {
		return parseConds(stratumConfigA);
	}

	public AtomicCondition[] getRightStratum() {
		return parseConds(stratumConfigB);
	}

	private AtomicCondition[] parseConds(TablePanel stratumConfig) {
		Object[] rows = stratumConfig.getRows();
		AtomicCondition conds[] = new AtomicCondition[rows.length];
		for (int i = 0; i < conds.length; i++) {
			Object[] row = (Object[]) rows[i];
			conds[i] = new AtomicCondition((DataColumnDefinition)row[0], (String)row[1], (String)row[2], title.getText());
		}
		return conds;
	}
	
}
