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


package cdc.impl.join.strata;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.components.AbstractJoin;
import cdc.components.AbstractJoinCondition;
import cdc.components.SystemComponent;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.MainFrame;
import cdc.gui.external.JXErrorDialog;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.specific.actions.LinkageConfigureStrataAction;
import cdc.utils.RJException;

public class StrataJoinGUIVisibleComponent extends GUIVisibleComponent {

	private LinkageConfigureStrataAction strataChooser;
	private GUIVisibleComponent[] joins;
	private JTabbedPane joinConfigs;
	private JRadioButton sameConfigs;
	private JRadioButton diffConfigs;
	private AbstractWizard wizard;
	private JPanel panel;
	private StrataJoinWrapper join;
	private boolean sameEnabled = true;
	
	public StrataJoinGUIVisibleComponent(AbstractWizard wizard, GUIVisibleComponent[] specificJoins, LinkageConfigureStrataAction strata) {
		this.joins = specificJoins;
		this.strataChooser = strata;
		this.wizard = wizard;
	}
	
	public Object generateSystemComponent() throws RJException, IOException {
		if (joinConfigs.getSelectedIndex() == joinConfigs.getTabCount() - 1) {
			// one for all
			return new StrataJoinWrapper((AbstractJoin) joins[joinConfigs.getSelectedIndex()].generateSystemComponent());
		} else {
			//normal mode
			AbstractJoin[] joins = new AbstractJoin[joinConfigs.getTabCount() - 1];
			for (int i = 0; i < joins.length; i++) {
				joins[i] = (AbstractJoin) this.joins[i].generateSystemComponent();
			}
			return new StrataJoinWrapper(strataChooser.getStrata(), joins);
		}
	}

	public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
		sameConfigs = new JRadioButton("Use the same join configuration for all strata");
		diffConfigs = new JRadioButton("Use different join configuration for each strata");
		diffConfigs.setEnabled(false);
		diffConfigs.setToolTipText("This option was disabled due to possible inconsistencies it could create.\nYou can achieve the same functionality by running FRIL linkage more than once and using data source filtering.");
		sameConfigs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				enableDisableTabs();
				if (((JRadioButton)e.getSource()).isSelected()) {
					joinConfigs.setSelectedIndex(joinConfigs.getTabCount() - 1);
				}
			}
		});
		diffConfigs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				enableDisableTabs();
				if (((JRadioButton)e.getSource()).isSelected()) {
					joinConfigs.setSelectedIndex(0);
				}
			}
		});
		JPanel radioPanel = new JPanel(new GridLayout(1, 2));
		radioPanel.add(sameConfigs);
		radioPanel.add(diffConfigs);
		
		ButtonGroup group = new ButtonGroup();
		group.add(sameConfigs);
		group.add(diffConfigs);
		if (!validOptionSameConfig((AbstractJoinCondition[]) objects[3])) {
			sameEnabled = false;
			sameConfigs.setEnabled(false);
			diffConfigs.setEnabled(false);
		}
		sameConfigs.setSelected(sameEnabled);
		diffConfigs.setSelected(!sameEnabled);
		
		//int selected = 0;
		joinConfigs = new JTabbedPane();
		try {
			for (int k = 0; k < joins.length - 1; k++) {
				JComponent comp = generateComponent(k, wizard, objects, sizeX, sizeY);
				joinConfigs.addTab(strataChooser.getStrata()[k].getName(), comp);
			}
			JComponent comp = generateComponent(joins.length - 1, wizard, objects, sizeX, sizeY);
			joinConfigs.addTab("All strata", comp);
		} catch (Exception e) {
			JXErrorDialog.showDialog(MainFrame.main, "Error", e);
			return new JPanel();
		}
		enableDisableTabs();
		if (sameEnabled) {
			joinConfigs.setSelectedIndex(joinConfigs.getTabCount() - 1);
		} else {
			joinConfigs.setSelectedIndex(0);
		}
		
		panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		panel.add(radioPanel, c);
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 1.0;
		c.weightx = 1.0;
		panel.add(joinConfigs, c);
		return panel;
	}
	
	private boolean validOptionSameConfig(AbstractJoinCondition[] conds) {
		AbstractDistance[] distances = conds[0].getDistanceFunctions();
		for (int i = 0; i < distances.length; i++) {
			for (int j = 1; j < conds.length; j++) {
				if (lookup(conds[0].getLeftJoinColumns()[i], conds[0].getRightJoinColumns()[i], conds[j]) == -1) {
					return false;
				}
			}
		}
		return true;
	}

	private int lookup(DataColumnDefinition col1, DataColumnDefinition col2, AbstractJoinCondition cond) {
		for (int j = 0; j < cond.getDistanceFunctions().length; j++) {
			if (cond.getLeftJoinColumns()[j].equals(col1) && cond.getRightJoinColumns()[j].equals(col2)) {
				return j;
			}
		}
		return -1;
	}

	private void enableDisableTabs() {
		boolean enable = diffConfigs.isSelected();
		for (int i = 0; i < joinConfigs.getTabCount() - 1; i++) {
			joinConfigs.setEnabledAt(i, enable);
		}
		joinConfigs.setEnabledAt(joinConfigs.getTabCount() - 1, !enable);
	}

	private JComponent generateComponent(int k, AbstractWizard wizard, Object[] objects, int sizeX, int sizeY) throws IOException, RJException {
		AbstractDataSource leftSource = (AbstractDataSource) objects[0];
		AbstractDataSource rightSource = (AbstractDataSource) objects[1];
		Object joinOutColumns = objects[2]; 
		AbstractJoinCondition[] joinConds = (AbstractJoinCondition[])objects[3];
		JScrollPane scrollPanel = new JScrollPane(joins[k].getConfigurationPanel(
				new Object[] {leftSource.copy(), rightSource.copy(), joinOutColumns, 
						k == joinConds.length ? getCompoundCondition(joinConds) : joinConds[k], wizard}, sizeX, sizeY));
		return scrollPanel;
	}

	private AbstractJoinCondition getCompoundCondition(AbstractJoinCondition[] joinConds) {
		StrataJoinCondition cond = new StrataJoinCondition(null);
		for (int i = 0; i < joinConds.length; i++) {
			cond.addStrata(strataChooser.getStrata()[i], joinConds[i]);
		}
		return cond;
	}

	public Class getProducedComponentClass() {
		return joins[0].getProducedComponentClass();
	}

	public String toString() {
		return joins[0].toString();
	}

	public boolean validate(JDialog dialog) {
		for (int i = 0; i < joins.length; i++) {
			if (joinConfigs.isEnabledAt(i)) {
				if (!joins[i].validate(wizard)) {
					joinConfigs.setSelectedIndex(i);
					return false;
				}
			}
		}
		return true;
	}
	
	public void setSize(int x, int y) {
		panel.setPreferredSize(new Dimension(x, y));	
	}
	
	public void restoreValues(SystemComponent component) {
		if (!(component instanceof StrataJoinWrapper)) {
			return;
		}
		join = (StrataJoinWrapper) component;
		if (join.isSameJoinConfigs()) {
			sameEnabled = true;
			joins[joins.length - 1].restoreValues(join.getJoins()[0]);
		} else {
			sameEnabled = false;
			for (int i = 0; i < joins.length - 1 && i < join.getJoins().length; i++) {
				joins[i].restoreValues(join.getJoins()[i]);
			}
		}
	}

}
