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
import java.awt.event.ComponentListener;
import java.io.IOException;
import java.util.EventListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.configuration.ConfigurationPhase;
import cdc.gui.CancelThreadListener;
import cdc.gui.ConfigLoadDialog;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.external.JXErrorDialog;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.impl.join.strata.DataStratum;
import cdc.impl.join.strata.StrataJoinGUIVisibleComponent;
import cdc.utils.GuiUtils;
import cdc.utils.Props;
import cdc.utils.RJException;

public class LinkageConfigureSearchAction extends WizardAction {

	private static final String LABEL_SUMMARY = "Create summary for not joined data in source ";
	
	private JPanel buffer;
	private JComboBox activeCombo;
	
	private JCheckBox checkBoxLeft = new JCheckBox();
	private JCheckBox checkBoxRight = new JCheckBox();
	
	private DSConfigureTypeAction leftSource;
	private DSConfigureTypeAction rightSource;
	private AbstractDataSource sourceA;
	private AbstractDataSource sourceB;
	private LinkageConfigureStrataAction strataChooser;
	private LinkageConfigureConditionsAction joinConfig;
	
	private AbstractJoin join;
	private GUIVisibleComponent[] comps;

	private JScrollPane scrollPanel;
	
	public LinkageConfigureSearchAction(DSConfigureTypeAction leftSource, DSConfigureTypeAction rightSource, LinkageConfigureStrataAction strata, LinkageConfigureConditionsAction joinConfig) {
		this.leftSource = leftSource;
		this.rightSource = rightSource;
		this.strataChooser = strata;
		this.joinConfig = joinConfig;
	}
	
	public LinkageConfigureSearchAction(AbstractDataSource sourceA, AbstractDataSource sourceB,
			LinkageConfigureStrataAction strata, LinkageConfigureConditionsAction joinFieldsConfiguration) {
		this.sourceA = sourceA;
		this.sourceB = sourceB;
		this.strataChooser = strata;
		this.joinConfig = joinFieldsConfiguration;
	}

	public JPanel beginStep(AbstractWizard wizard) {
		wizard.getMainPanel().setLayout(new BorderLayout());
		JPanel internalPanel = new JPanel();
		internalPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
		if (scrollPanel != null) {
			removeAll(scrollPanel);
		}
		scrollPanel = new JScrollPane(internalPanel);
		
		int selected = 0;
		if (activeCombo != null) {
			removeAll(activeCombo);
		}
		activeCombo = new JComboBox();
		activeCombo.setPreferredSize(new Dimension(350, (int)activeCombo.getPreferredSize().getHeight()));
		if (strataChooser.getStrata() != null) {
			comps = new GUIVisibleComponent[GuiUtils.getAvailableJoins().length];
			DataStratum[] strata = strataChooser.getStrata();
			for (int i = 0; i < comps.length; i++) {
				GUIVisibleComponent[] children = new GUIVisibleComponent[strata.length + 1];
				for (int j = 0; j < children.length; j++) {
					children[j] = GuiUtils.getAvailableJoins()[i];
				}
				comps[i] = new StrataJoinGUIVisibleComponent(wizard, children, strataChooser);
				activeCombo.addItem(comps[i]);
			}
			
			ComboListener listener = new ComboListener(wizard, internalPanel, new Object[] {
					getLeftSource(),
					getRightSource(),
					joinConfig.getOutColumns(), 
					joinConfig.getJoinConditons(),
					wizard
			});
			activeCombo.addActionListener(listener);
			scrollPanel.addComponentListener(listener);
		} else {
			comps = GuiUtils.getAvailableJoins();
			for (int i = 0; i < comps.length; i++) {
				activeCombo.addItem(comps[i]);
				if (join != null && comps[i].getProducedComponentClass().equals(join.getClass())) {
					comps[i].restoreValues(join);
					selected = i;
				}
			}
			
			ComboListener listener = new ComboListener(wizard, internalPanel, new Object[] {
					getLeftSource(),
					getRightSource(),
					joinConfig.getOutColumns(), 
					joinConfig.getJoinConditons()[0],
					wizard
			});
			activeCombo.addActionListener(listener);
			scrollPanel.addComponentListener(listener);
		}
		
		JLabel activeLabel = new JLabel("Join method type:");
		JPanel comboPanel = new JPanel();
		comboPanel.setLayout(new BorderLayout());
				
		JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		typePanel.add(activeLabel);
		typePanel.add(activeCombo);
		comboPanel.add(typePanel, BorderLayout.NORTH);
		
		comboPanel.add(scrollPanel, BorderLayout.CENTER);
		activeCombo.setSelectedIndex(selected);
		
		JPanel minusPanel = new JPanel(new BorderLayout());
		minusPanel.add(checkBoxLeft, BorderLayout.NORTH);
		minusPanel.add(checkBoxRight, BorderLayout.CENTER);
		comboPanel.add(minusPanel, BorderLayout.SOUTH);
		
		checkBoxLeft.setText(LABEL_SUMMARY + sourceA.getSourceName());
		checkBoxRight.setText(LABEL_SUMMARY + sourceB.getSourceName());
		
		ensureSummaryBoxes();
		activeCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ensureSummaryBoxes();
			}
		});
		
		restoreIfNeeded();
		
		buffer = comboPanel;
		return buffer;

	}

	private void ensureSummaryBoxes() {
		boolean leftJoinSupported = Props.getBoolean("summary-left-enabled-" + ((GUIVisibleComponent)activeCombo.getSelectedItem()).getProducedComponentClass().getName(), true);
		boolean rightJoinSupported = Props.getBoolean("summary-right-enabled-" + ((GUIVisibleComponent)activeCombo.getSelectedItem()).getProducedComponentClass().getName(), true);
		if (join != null) {
			if (join.isSummaryForLeftSourceEnabled() && leftJoinSupported) {
				checkBoxLeft.setSelected(true);
			}
			if (join.isSummaryForRightSourceEnabled() && rightJoinSupported) {
				checkBoxRight.setSelected(true);
			}
		}
		checkBoxLeft.setEnabled(leftJoinSupported);
		if (!leftJoinSupported) {
			checkBoxLeft.setSelected(false);
		}
		checkBoxRight.setEnabled(rightJoinSupported);
		if (!rightJoinSupported) {
			checkBoxRight.setSelected(false);
		}
	}

	private void removeAll(JComboBox activeCombo) {
		EventListener[] listeners = activeCombo.getListeners(ActionListener.class);
		for (int i = 0; i < listeners.length; i++) {
			activeCombo.removeActionListener((ActionListener) listeners[i]);
		}
	}
	
	private void removeAll(JScrollPane activeCombo) {
		EventListener[] listeners = activeCombo.getListeners(ComponentListener.class);
		for (int i = 0; i < listeners.length; i++) {
			activeCombo.removeComponentListener((ComponentListener) listeners[i]);
		}
	}

	private void restoreIfNeeded() {
		if (join == null) {
			activeCombo.setSelectedIndex(0);
			return;
		}
		int selected = 0;
		for (int i = 0; i < comps.length; i++) {
			if (join != null && comps[i].getProducedComponentClass().equals(join.getEffectiveJoinClass())) {
				selected = i;
			}
		}
		comps[selected].restoreValues(join);
		activeCombo.setSelectedIndex(selected);
	}

	private AbstractDataSource getLeftSource() {
		if (sourceA != null) {
			return sourceA;
		} else {
			return leftSource.getDataSource();
		}
	}
	
	private AbstractDataSource getRightSource() {
		if (sourceB != null) {
			return sourceB;
		} else {
			return rightSource.getDataSource();
		}
	}

	public boolean endStep(AbstractWizard wizard) {
		
		if (!((GUIVisibleComponent)this.activeCombo.getSelectedItem()).validate(wizard)) {
			return false;
		}
		
		if (join != null) {
			try {
				join.close();
			} catch (IOException e) {
				JXErrorDialog.showDialog(wizard, "Error", e);
			} catch (RJException e) {
				JXErrorDialog.showDialog(wizard, "Error", e);
			}
		}
		
		
		
		ConfigLoadDialog dialog = new ConfigLoadDialog(new String[] {ConfigurationPhase.loadingJoinProcessPhase.getPhaseName()});
		CofigurationLoaderThread thread = new CofigurationLoaderThread(dialog, (GUIVisibleComponent)this.activeCombo.getSelectedItem());
		dialog.addCancelListener(new CancelThreadListener(thread));
		thread.start();
		dialog.started();
		this.join = thread.getJoin();
		
		if (join == null) {
			return false;
		}
		
		if (thread.isStopScheduled()) {
			this.join = null;
			return false;
		}
		
		try {
			if (checkBoxLeft.isSelected()) {
				this.join.enableSummaryForLeftSource("");
			}
			if (checkBoxRight.isSelected()) {
				this.join.enableSummaryForRightSource("");
			}
		} catch (RJException e) {
			JXErrorDialog.showDialog(wizard, "Error", e);
		}
			
		return this.join != null;
	}

	public AbstractJoin getJoin() {
		return join;
	}
	
	public void setJoin(AbstractJoin join) {
		this.join = join;
	}
	
	public void setSize(int width, int height) {
		new Exception().printStackTrace();
	}

	public void dispose() {
		removeAll(activeCombo);
		removeAll(scrollPanel);
		this.strataChooser.dispose();
		this.activeCombo = null;
		this.scrollPanel = null;
		this.buffer = null;
		this.checkBoxLeft = null;
		this.checkBoxRight = null;
		this.comps = null;
		this.join = null;
		this.joinConfig = null;
		this.leftSource = null;
		this.rightSource = null;
		this.sourceA = null;
		this.sourceB = null;
		this.strataChooser = null;
	}

}
