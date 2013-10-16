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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.gui.MainFrame;
import cdc.gui.components.dialogs.OneTimeTipDialog;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.gui.wizards.specific.actions.strata.StrataChooser;
import cdc.impl.join.strata.DataStratum;
import cdc.impl.join.strata.StrataJoinWrapper;

public class LinkageConfigureStrataAction extends WizardAction {

	public static final String STRATIFICATION_ENABLED = "STRATIFICATION_ENABLED";
	public static final String MESSAGE = "Using the data stratificaton is discouraged at this time.\n" +
										 "If you use it, some options of FRIL will be disabled (e.g.,\n" +
										 "results desuplication). If you need to use different linkage\n" +
										 "conditions for different records, you can enable filtering\n" +
										 "in the data sources instead and run linkage a few times using\n" +
										 "different configurations (different filter and linkage condition" +
										 "configurations).";
	
	private JRadioButton strataOff = new JRadioButton("Do not stratify data", true);
	private JRadioButton strataOn = new JRadioButton("Stratify data");
	private JPanel strataPanel;
	private StrataChooser strataConfiguration;
	private DSConfigureTypeAction leftSourceAction;
	private DSConfigureTypeAction rightSourceAction;
	
	public LinkageConfigureStrataAction(AbstractDataSource sourceA, AbstractDataSource sourceB) {
		ButtonGroup group = new ButtonGroup();
		group.add(strataOff);
		group.add(strataOn);
		strataPanel = new JPanel(new FlowLayout());
		strataPanel.add(strataOff);
		strataPanel.add(strataOn);
		
		strataOn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (((JRadioButton)arg0.getSource()).isSelected()) {
					strataConfiguration.setEnabled(true);
					OneTimeTipDialog.showInfoDialogIfNeeded(STRATIFICATION_ENABLED, MESSAGE);
				}
			}
		});
		
		strataOff.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (((JRadioButton)arg0.getSource()).isSelected()) {
					strataConfiguration.setEnabled(false);
				}
			}
		});
		
		this.strataConfiguration = new StrataChooser(sourceA, sourceB);
		strataConfiguration.setEnabled(false);
	}

	public LinkageConfigureStrataAction(DSConfigureTypeAction leftSourceAction, DSConfigureTypeAction rightSourceAction) {
		this.leftSourceAction = leftSourceAction;
		this.rightSourceAction = rightSourceAction;
	}

	public JPanel beginStep(AbstractWizard wizard) {
		if (strataConfiguration == null) {
			this.strataConfiguration = new StrataChooser(leftSourceAction.getDataSource(), rightSourceAction.getDataSource());
			strataConfiguration.setEnabled(false);
		}
		JPanel stepPanel = new JPanel(new BorderLayout());
		stepPanel.add(strataPanel, BorderLayout.NORTH);
		stepPanel.add(strataConfiguration, BorderLayout.CENTER);
		return stepPanel;
	}

	public boolean endStep(AbstractWizard wizard) {
		if (strataOff.isSelected()) {
			return true;
		}
		DataStratum[] strata = strataConfiguration.getStratumConfiguration();
		for (int i = 0; i < strata.length; i++) {
			for (int j = 0; j < strata.length; j++) {
				if (j != i && strata[i].getName().equals(strata[j].getName())) {
					JOptionPane.showMessageDialog(MainFrame.main, "Each stratum has to have a unique name.", "Stratum name error", JOptionPane.OK_OPTION);
					return false;
				}
			}
		}
		
		int empty = -1;
		for (int i = 0; i < strata.length; i++) {
			if (strata[i].getSourceA().length == 0 && strata[i].getSourceB().length == 0) {
				empty = i;
				break;
			}
		}
		if (empty != -1) {
			int res = JOptionPane.showConfirmDialog(MainFrame.main, "At least one stratum does not have any stratum condition.\nDo you want to proceed?", 
					"Stratum condition empty", JOptionPane.YES_NO_OPTION);
			return res == JOptionPane.YES_OPTION;
		}
		return true;
	}

	public void setSize(int width, int height) {
		System.out.println("WARNING: Set size not yet implemented in " + getClass());
	}
	
	public void setJoin(AbstractJoin join) {
		if (join instanceof StrataJoinWrapper) {
			strataOn.setSelected(true);
			DataStratum[] strata = ((StrataJoinWrapper)join).getStrata();
			strataConfiguration.restoreStrata(strata);
			strataConfiguration.setEnabled(true);
			OneTimeTipDialog.showInfoDialogIfNeeded(STRATIFICATION_ENABLED, MESSAGE);
		} else {
			strataOff.setSelected(true);
		}
	}
	
	public DataStratum[] getStrata() {
		if (strataOn.isSelected()) {
			return strataConfiguration.getStratumConfiguration();
		} else {
			return null;
		}
	}

	public void dispose() {
		this.leftSourceAction = null;
		this.rightSourceAction = null;
		this.strataConfiguration = null;
		this.strataOff = null;
		this.strataOn = null;
		this.strataPanel = null;
	}
}
