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
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import cdc.components.AbstractDataSource;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.impl.datasource.sampling.DataSampleInterface;
import cdc.impl.datasource.sampling.FirstNSampler;
import cdc.impl.datasource.sampling.FullSampler;
import cdc.impl.datasource.sampling.RandomSampler;
import cdc.utils.RJException;

public class ConfigureSourcesAction extends WizardAction {

	private static final double DESIRED = 1000000;
	private AbstractDataSource sourceA;
	private AbstractDataSource sourceB;
	
	private JPanel panel;
	private JRadioButton sampleBeginning;
	private JRadioButton sampleRandom;
	private JRadioButton sampleBlocking;
	
	private JSpinner fieldSourceA;
	private JSpinner fieldSourceB;
	private JSpinner percentageRandom;
	private JSpinner numberBlocks;
	private int row = 0;
	
	public ConfigureSourcesAction(AbstractDataSource sourceA, AbstractDataSource sourceB) throws IOException, RJException {
		
		this.sourceA = sourceA;
		this.sourceB = sourceB;
		
		panel = new JPanel(new GridBagLayout());
		panel.add(new JLabel("Choose sampling method for EM computation:"), getNextConstraints());
		
		long m = sourceA.size() * sourceB.size();
		int perc = (int) ((DESIRED / (double)m) * 100);
		if (perc < 1) {
			perc = 1;
		}
		if (perc > 100) {
			perc = 100;
		}
		
		percentageRandom = new JSpinner(new SpinnerNumberModel(perc, 1, 100, 1));
		fieldSourceA = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
		fieldSourceB = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
		numberBlocks = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
		
		ButtonGroup group = new ButtonGroup();
		sampleRandom = new JRadioButton("Sample random percentage of records from data sources");
		sampleBeginning = new JRadioButton("Sample first n records from data sources");
		sampleBlocking = new JRadioButton("Sample using blocking");
		group.add(sampleBeginning);
		group.add(sampleRandom);
		group.add(sampleBlocking);
		
		EnableDisableListener eDListener1 = new EnableDisableListener();
		sampleRandom.addActionListener(eDListener1);
		JPanel randomInputPanel = new JPanel();
		randomInputPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		randomInputPanel.add(eDListener1.addComponent(new JLabel("Percentage of records in sample:")));
		randomInputPanel.add(eDListener1.addComponent(percentageRandom));
		
		GridBagConstraints c = getNextConstraints();
		panel.add(sampleRandom, c);
		c = getNextConstraints();
		c.insets = new Insets(0, 40, 0, 0);
		panel.add(randomInputPanel, c);
		
		EnableDisableListener eDListener2 = new EnableDisableListener();
		sampleBeginning.addActionListener(eDListener2);
		JPanel fixedInputPanel = new JPanel();
		fixedInputPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		fixedInputPanel.add(eDListener2.addComponent(new JLabel("Number of records from source '" + sourceA.getSourceName() + "':")));
		fixedInputPanel.add(eDListener2.addComponent(fieldSourceA));
		
		panel.add(sampleBeginning, c = getNextConstraints());
		c = getNextConstraints();
		c.insets = new Insets(0, 40, 0, 0);
		panel.add(fixedInputPanel, c);
		
		fixedInputPanel = new JPanel();
		fixedInputPanel.setLayout(new FlowLayout());
		fixedInputPanel.add(eDListener2.addComponent(new JLabel("Number of records from source '" + sourceB.getSourceName() + "':")));
		fixedInputPanel.add(eDListener2.addComponent(fieldSourceB));
		c = getNextConstraints();
		c.insets = new Insets(0, 40, 0, 0);
		panel.add(fixedInputPanel, c);
		
		EnableDisableListener eDListener3 = new EnableDisableListener();
		sampleBlocking.addActionListener(eDListener3);
		JPanel chooseBlocks = new JPanel();
		chooseBlocks.setLayout(new FlowLayout(FlowLayout.LEFT));
		chooseBlocks.add(eDListener3.addComponent(new JLabel("Number of used blocks:")));
		chooseBlocks.add(eDListener3.addComponent(numberBlocks));
		
		panel.add(sampleBlocking, c = getNextConstraints());
		c = getNextConstraints();
		c.insets = new Insets(0, 40, 0, 0);
		panel.add(chooseBlocks, c);
		
		eDListener1.setOtherListeners(new EnableDisableListener[] {eDListener2, eDListener3});
		eDListener2.setOtherListeners(new EnableDisableListener[] {eDListener1, eDListener3});
		eDListener3.setOtherListeners(new EnableDisableListener[] {eDListener2, eDListener1});
		
		sampleRandom.setSelected(true);
		eDListener1.actionPerformed(new ActionEvent(sampleRandom, 0, null));
	}
	
	private GridBagConstraints getNextConstraints() {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = row ++;
		c.anchor = GridBagConstraints.LINE_START;
		return c;
	}
	
	public JPanel beginStep(AbstractWizard wizard) {
		return panel;
	}

	public boolean endStep(AbstractWizard wizard) {
		return true;
	}

	public void setSize(int width, int height) {
	}

	public boolean needsBlocking() {
		return sampleBlocking.isSelected();
	}
	
	public DataSampleInterface getSamplingConfigurationForSourceA() throws IOException, RJException {
		return getSampler(sourceA, Integer.parseInt(String.valueOf(fieldSourceA.getValue())));
	}
	
	public DataSampleInterface getSamplingConfigurationForSourceB() throws IOException, RJException {
		return getSampler(sourceB, Integer.parseInt(String.valueOf(fieldSourceB.getValue())));
	}

	private DataSampleInterface getSampler(AbstractDataSource source, int n) throws IOException, RJException {
		if (sampleBeginning.isSelected()) {
			return new FirstNSampler(source, n);
		} else if (sampleBlocking.isSelected()) {
			return new FullSampler(source, Integer.parseInt(String.valueOf(numberBlocks.getValue())));
		} else {
			return new RandomSampler(source, Integer.parseInt(String.valueOf(percentageRandom.getValue())));
		}
	}

	public void dispose() {
		this.fieldSourceA = null;
		this.fieldSourceB = null;
		this.numberBlocks = null;
		this.panel = null;
		this.percentageRandom = null;
		this.sampleBeginning = null;
		this.sampleBlocking = null;
		this.sampleRandom = null;
		this.sourceA = null;
		this.sourceB = null;
	}

}
