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


package cdc.gui.wizards.specific.actions;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.components.AbstractJoinCondition;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.OptionDialog;
import cdc.gui.components.table.TablePanel;
import cdc.gui.components.uicomponents.AvaialbleColumnsPanel;
import cdc.gui.external.JXErrorDialog;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.impl.join.strata.DataStratum;
import cdc.impl.join.strata.StrataJoinWrapper;
import cdc.utils.GuiUtils;
import cdc.utils.RJException;

public class LinkageConfigureConditionsAction extends WizardAction {

	public class PasteActionListener implements ActionListener {

		private int id;
		
		public PasteActionListener(int id) {
			this.id = id;
		}

		public void actionPerformed(ActionEvent arg0) {
			pasteCombo[id].setSelectedIndex(0);
			comboPanel[id].setVisible(false);
			pastePanel[id].setVisible(true);
		}

	}

	public class CancelPasteActionListener implements ActionListener {

		private int id;

		public CancelPasteActionListener(int id) {
			this.id = id;
		}

		public void actionPerformed(ActionEvent arg0) {
			comboPanel[id].setVisible(true);
			pastePanel[id].setVisible(false);
		}

	}
	
	public class ComboSelectListener implements ActionListener {
		
		private int id;

		public ComboSelectListener(int id) {
			this.id = id;
		}
		
		public void actionPerformed(ActionEvent arg0) {
			if (pasteCombo[id].getSelectedIndex() != 0) {
				try {
					int n = -1;
					String selectedStr = (String) pasteCombo[id].getSelectedItem();
					for (int i = 0; i < strataChooser.getStrata().length; i++) {
						if (selectedStr.equals(strataChooser.getStrata()[i].getName())) {
							n = i;
						}
					}
					AbstractJoinCondition that = (AbstractJoinCondition) ((GUIVisibleComponent)activeCombo[n].getSelectedItem()).generateSystemComponent();
					if (joinCondition == null) {
						joinCondition = new AbstractJoinCondition[pasteCombo.length];
					}
					joinCondition[id] = (AbstractJoinCondition) that.clone();
					
					GUIVisibleComponent[] joinTypes = GuiUtils.getJoinConditions();
					int selected = -1;
					for (int i = 0; i < joinTypes.length; i++) {
						if (joinCondition[id].getClass().equals(joinTypes[i].getProducedComponentClass())) {
							selected = i;
						}
					}
					tabs.setComponentAt(id, getStratumJoinConditionPanel(parent, id));
					activeCombo[id].setSelectedIndex(selected);
					//tabs.removeTabAt(id);
				} catch (Exception e) {
					e.printStackTrace();
					JXErrorDialog.showDialog(parent, "Error when pasting condition", e);
				}
				
				comboPanel[id].setVisible(true);
				pastePanel[id].setVisible(false);
			}
		}
		
	}

	private AbstractWizard parent;
	
	private DSConfigureTypeAction leftSourceAction;
	private DSConfigureTypeAction rightSourceAction;
	
	private LinkageConfigureStrataAction strataChooser;
	
	private AbstractDataSource sourceA;
	private AbstractDataSource sourceB;
	
	private AbstractJoinCondition joinCondition[];
	private JComboBox[] activeCombo;
	
	private TablePanel tablePanel;
	private JTabbedPane tabs;
	
	private JButton[] paste;
	private JButton[] cancel;
	private JPanel[] comboPanel;
	private JPanel[] pastePanel;
	private JComboBox[] pasteCombo;

	private AbstractJoin restoredJoin = null;
	
	public LinkageConfigureConditionsAction(DSConfigureTypeAction left, DSConfigureTypeAction right, LinkageConfigureStrataAction joinStratificationConfiguration) {
		leftSourceAction = left;
		rightSourceAction = right;
		this.strataChooser = joinStratificationConfiguration;
		createTable();
		fillInDefaultValues();
	}
	
	public LinkageConfigureConditionsAction(AbstractDataSource sourceA, AbstractDataSource sourceB, LinkageConfigureStrataAction joinStratificationConfiguration) {
		this.sourceA = sourceA;
		this.sourceB = sourceB;
		this.strataChooser = joinStratificationConfiguration;
		createTable();
		fillInDefaultValues();
	}

	private void fillInDefaultValues() {
		addCols(sourceA.getDataModel().getOutputFormat());
		addCols(sourceB.getDataModel().getOutputFormat());
	}

	private void addCols(DataColumnDefinition[] outputFormat) {
		for (int i = 0; i < outputFormat.length; i++) {
			tablePanel.addRow(new DataColumnDefinition[] {outputFormat[i]});
		}
	}

	private void createTable() {
		tablePanel = new TablePanel(new String[] {"Selected columns"}, false);
		tablePanel.addAddButtonListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					OptionDialog dialog = new OptionDialog(parent, "Choose column");
					AvaialbleColumnsPanel panel = new AvaialbleColumnsPanel(getAllAvailableColumns());
					dialog.setMainPanel(panel);
					dialog.setLocationRelativeTo((JButton)e.getSource());
					if (dialog.getResult() == OptionDialog.RESULT_OK) {
						DataColumnDefinition[] cols = panel.getSelectedColumns();
						for (int i = 0; i < cols.length; i++) {
							tablePanel.addRow(new DataColumnDefinition[] {cols[i]});
						}
					}
				}
			});
		tablePanel.setSize(500, 130);
		tablePanel.setPreferredSize(new Dimension(500, 130));
	}

	public JPanel beginStep(AbstractWizard wizard) {
		wizard.getMainPanel().setLayout(new BoxLayout(wizard.getMainPanel(), BoxLayout.LINE_AXIS));
		JPanel panelCond;
		parent = wizard;
		restoreJoinIfPossible();
		if (strataChooser.getStrata() == null) {
			activeCombo = new JComboBox[1];
			comboPanel = new JPanel[1];
			pastePanel = new JPanel[1];
			paste = new JButton[1];
			cancel = new JButton[1];
			pasteCombo = new JComboBox[1];
			panelCond = getStratumJoinConditionPanel(wizard, 0);
		} else {
			panelCond = new JPanel(new BorderLayout());
			tabs = new JTabbedPane();
			panelCond.add(tabs, BorderLayout.CENTER);
			DataStratum strata[] = strataChooser.getStrata();
			activeCombo = new JComboBox[strata.length];
			comboPanel = new JPanel[strata.length];
			pastePanel = new JPanel[strata.length];
			paste = new JButton[strata.length];
			cancel = new JButton[strata.length];
			pasteCombo = new JComboBox[strata.length];
			for (int i = 0; i < strata.length; i++) {
				tabs.addTab(strata[i].getName(), getStratumJoinConditionPanel(wizard, i));
			}
		}
		
		GUIVisibleComponent[] joinTypes = GuiUtils.getJoinConditions();
		int selected = 0;
		for (int i = 0; i < joinTypes.length; i++) {
			if (joinCondition == null || joinCondition[i] == null) {
				continue;
			}
			if (joinCondition != null && joinCondition[i].getClass().equals(joinTypes[i].getProducedComponentClass())) {
				selected = i;
			}
		}
		for (int i = 0; i < activeCombo.length; i++) {
			
			activeCombo[i].setSelectedIndex(selected);
		}
		
		panelCond.setBorder(BorderFactory.createTitledBorder("Join condition"));
		
		JPanel panelOut = new JPanel();
		panelOut.setPreferredSize(new Dimension(600, 160));
		panelOut.setMinimumSize(new Dimension(600, 160));
		panelOut.setMaximumSize(new Dimension(6000, 160));
		panelOut.setBorder(BorderFactory.createTitledBorder("Output columns"));
		panelOut.add(tablePanel);
		
		JPanel buffer = new JPanel();
		buffer.setLayout(new BoxLayout(buffer, BoxLayout.PAGE_AXIS));
		buffer.add(panelCond);
		buffer.add(panelOut);
		return buffer;
	}

	private void restoreJoinIfPossible() {
		//joinCondition = null;
		if (joinCondition != null) {
			//make sure that the join condition is ok
			if (strataChooser.getStrata() == null || joinCondition.length == strataChooser.getStrata().length) {
				return;
			} else {
				AbstractJoinCondition[] oldJoinConditions = joinCondition;
				joinCondition = new AbstractJoinCondition[strataChooser.getStrata().length];
				for (int i = 0; i < joinCondition.length; i++) {
					if (i < oldJoinConditions.length) {
						joinCondition[i] = oldJoinConditions[i];
					} else if (oldJoinConditions.length == 1) {
						joinCondition[i] = oldJoinConditions[0];
					} else {
						joinCondition[i] = null;
					}
					
				}
			}
			return;
		}
		if (restoredJoin == null) {
			return;
		}
		if (restoredJoin instanceof StrataJoinWrapper && strataChooser != null && strataChooser.getStrata() != null) {
			AbstractJoinCondition[] conds = ((StrataJoinWrapper)restoredJoin).getJoinConditions();
			String[] strataNames = ((StrataJoinWrapper)restoredJoin).getStrataNames();
			joinCondition = new AbstractJoinCondition[strataChooser.getStrata().length];
			for (int i = 0; i < strataNames.length; i++) {
				DataStratum[] strata = strataChooser.getStrata();
				for (int j = 0; j < strata.length; j++) {
					if (strata[j].getName().equals(strataNames[i])) {
						joinCondition[j] = conds[i];
					}
				}
			}
		} else {
			if (strataChooser.getStrata() == null || strataChooser.getStrata().length == 1) {
				joinCondition = new AbstractJoinCondition[1];
				joinCondition[0] = restoredJoin.getJoinCondition();
			}
		}
	}

	private JPanel getStratumJoinConditionPanel(AbstractWizard wizard, int id) {
		JPanel panelCond = new JPanel();
		panelCond.setLayout(new BorderLayout());
		JPanel insideComboPanel = new JPanel();
		activeCombo[id] = new JComboBox();
		GUIVisibleComponent[] joinTypes = GuiUtils.getJoinConditions();
		for (int i = 0; i < joinTypes.length; i++) {
			activeCombo[id].addItem(joinTypes[i]);
		}
		AbstractJoinCondition conditionToUse  = joinCondition == null ? null : joinCondition[id];
		ComboListener comboListener = new ComboListener(wizard, insideComboPanel, new Object[] {
				this.sourceA != null ? this.sourceA : leftSourceAction.getDataSource(),
				this.sourceB != null ? this.sourceB : rightSourceAction.getDataSource(),
				wizard, conditionToUse
		});
		insideComboPanel.addComponentListener(comboListener);
		activeCombo[id].addActionListener(comboListener);
		panelCond.setLayout(new BorderLayout());
		JLabel activeLabel = new JLabel("Join condition type:");
		comboPanel[id] = new JPanel(new FlowLayout(FlowLayout.LEFT));
		comboPanel[id].add(activeLabel);
		comboPanel[id].add(activeCombo[id]);
		pastePanel[id] = new JPanel(new FlowLayout(FlowLayout.LEFT));
		pastePanel[id].setVisible(false);
		cancel[id] = new JButton("Cancel");
		cancel[id].setPreferredSize(new Dimension(120, 20));
		cancel[id].addActionListener(new CancelPasteActionListener(id));
		paste[id] = new JButton("Copy from");
		paste[id].setPreferredSize(new Dimension(120, 20));
		paste[id].addActionListener(new PasteActionListener(id));
		
		if (strataChooser.getStrata() != null) {
			String[] strata = new String[strataChooser.getStrata().length];
			strata[0] = "Select...";
			int skip = 0;
			for (int i = 0; i < strata.length; i++) {
				if (i != id) {
					strata[i + 1 + skip] = strataChooser.getStrata()[i].getName();
				} else {
					skip = -1;
				}
			}
			pasteCombo[id] = new JComboBox(strata);
			pasteCombo[id].setPreferredSize(new Dimension(200, pasteCombo[id].getPreferredSize().height));
			pasteCombo[id].addActionListener(new ComboSelectListener(id));
			comboPanel[id].add(paste[id]);
			
			pastePanel[id].add(new JLabel("Copy condition from: "));
			pastePanel[id].add(pasteCombo[id]);
			pastePanel[id].add(cancel[id]);
		}
		
		activeCombo[id].setPreferredSize(new Dimension(200, activeCombo[id].getPreferredSize().height));
		JPanel pan = new JPanel(new FlowLayout(FlowLayout.LEFT));
		pan.add(comboPanel[id]);
		if (strataChooser.getStrata() != null) {
			pan.add(pastePanel[id]);
		}
		panelCond.add(pan, BorderLayout.NORTH);
		panelCond.add(insideComboPanel, BorderLayout.CENTER);
			        
		return panelCond;
	}
	
	public boolean endStep(AbstractWizard wizard) {
		for (int i = 0; i < activeCombo.length; i++) {
			if (!((GUIVisibleComponent)this.activeCombo[i].getSelectedItem()).validate(wizard)) {
				if (tabs != null) {
					tabs.setSelectedIndex(i);
				}
				return false;
			}
		}
		if (strataChooser.getStrata() == null) {
			joinCondition = new AbstractJoinCondition[1];
		} else {
			joinCondition = new AbstractJoinCondition[strataChooser.getStrata().length];
		}
		try {
			for (int j = 0; j < joinCondition.length; j++) {
				joinCondition[j] = (AbstractJoinCondition) ((GUIVisibleComponent)activeCombo[j].getSelectedItem()).generateSystemComponent();
			}
		} catch (RJException e) {
			JXErrorDialog.showDialog(wizard, "Error creating join condition", e);
		} catch (IOException e) {
			JXErrorDialog.showDialog(wizard, "Error creating join condition", e);
		}
		
		if (tablePanel.getRows().length == 0) {
			JOptionPane.showMessageDialog(wizard, "At least one output column is required.");
			return false;
		}
		return true;
	}

	private DataColumnDefinition[] getAllAvailableColumns() {
		DataColumnDefinition[] c1 = getLeftColumns();
		DataColumnDefinition[] c2 = getRightColumns();
		DataColumnDefinition[] out = new DataColumnDefinition[c1.length + c2.length];
		System.arraycopy(c1, 0, out, 0, c1.length);
		System.arraycopy(c2, 0, out, c1.length, c2.length);
		return out;
	}

	private DataColumnDefinition[] getLeftColumns() {
		if (sourceA != null) {
			return sourceA.getDataModel().getSortedOutputColumns();
		} else {
			return leftSourceAction.getDataSource().getDataModel().getSortedOutputColumns();
		}
	}

	private DataColumnDefinition[] getRightColumns() {
		if (sourceB != null) {
			return sourceB.getDataModel().getSortedOutputColumns();
		} else {
			return rightSourceAction.getDataSource().getDataModel().getSortedOutputColumns();
		}
	}
	
	public AbstractJoinCondition[] getJoinConditons() {
		return joinCondition;
	}

	public DataColumnDefinition[] getOutColumns() {
		Object[] rows = tablePanel.getRows();
		DataColumnDefinition[] columns = new DataColumnDefinition[rows.length];
		for (int i = 0; i < columns.length; i++) {
			columns[i] = (DataColumnDefinition)(((Object[])rows[i])[0]);
		}
		return columns;
	}

	public void setJoin(AbstractJoin join) {
		this.restoredJoin  = join;
		DataColumnDefinition[] out = join.getOutColumns();
		tablePanel.removeAllRows();
		for (int j = 0; j < out.length; j++) {
			tablePanel.addRow(new DataColumnDefinition[] {out[j]});
		}
//		System.out.println("setJoin not implemented.");
//		if (join instanceof StrataJoinWrapper) {
//			
//		} else {
//			joinCondition = new AbstractJoinCondition[joins.length];
//			for (int i = 0; i < joins.length; i++) {
//				joinCondition[i] = joins[i].getJoinCondition();
//			}
//		}
	}
	
	public void setSize(int width, int height) {
	}

	public void dispose() {
		this.activeCombo = null;
		this.cancel = null;
		this.comboPanel = null;
		this.joinCondition = null;
		this.leftSourceAction = null;
		this.parent = null;
		this.paste = null;
		this.pasteCombo = null;
		this.pastePanel = null;
		this.restoredJoin = null;
		this.rightSourceAction = null;
		this.sourceA = null;
		this.sourceB = null;
		this.strataChooser = null;
		this.tablePanel = null;
		this.tabs = null;
	}
}
