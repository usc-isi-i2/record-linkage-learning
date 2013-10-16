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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import cdc.components.AbstractDataSource;
import cdc.components.Filter;
import cdc.gui.components.uicomponents.FileLocationPanel;
import cdc.gui.components.uicomponents.FilterExpressionEditor;
import cdc.gui.external.JXErrorDialog;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.impl.deduplication.DeduplicationConfig;
import cdc.impl.deduplication.gui.DeduplicationWizard;
import cdc.utils.RJException;

public class DSConfigurePreprocessingAction extends WizardAction {
	
	private DSConfigureTypeAction sourceAction;
	private JButton button;
	private DeduplicationConfig config;
	private Filter filterExpression = null;
	private AbstractWizard activeWizard;
	private JCheckBox dedupOn;
	private AbstractDataSource originalDataSource;
	private boolean showDedupe = true;
	
	//private JLabel duplicatesFileLabel;
	
	//private JButton chooseFileButton;
	private JCheckBox saveDuplicates;
	//private JTextField duplicatesFile;
	
	private JCheckBox filterOn;
	private FilterExpressionEditor expressionEditor;
	private FileLocationPanel filePanel;
	
	public DSConfigurePreprocessingAction(DSConfigureTypeAction sourceAction, AbstractDataSource originalDataSource) {
		this.sourceAction = sourceAction;
		this.originalDataSource = originalDataSource;
	}

	public DSConfigurePreprocessingAction(DSConfigureTypeAction sourceAction, AbstractDataSource originalDataSource, boolean enabled) {
		this.sourceAction = sourceAction;
		this.originalDataSource = originalDataSource;
		this.showDedupe = enabled;
	}
	
	public JPanel beginStep(AbstractWizard wizard) {
		this.activeWizard = wizard;
		JPanel dedupePanel = new JPanel(new GridBagLayout());
		dedupePanel.setBorder(BorderFactory.createTitledBorder("Data source deduplication"));
		
		dedupOn = new JCheckBox("Perform de-duplication for the data source");
		saveDuplicates = new JCheckBox("Save duplicates into file");
		filePanel = new FileLocationPanel("Duplicates file:", "duplicates.csv", 20, FileLocationPanel.SAVE);
		button = new JButton("Preferences");
		JPanel dedupeEnable = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		button.setPreferredSize(new Dimension(button.getPreferredSize().width, 20));
		dedupeEnable.add(dedupOn);
		dedupeEnable.add(button);
		dedupePanel.add(dedupeEnable, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		
		saveDuplicates.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				enableMinusFileOption();
			}
		});
		dedupePanel.add(saveDuplicates, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		
		
		dedupePanel.add(filePanel, new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,10,0,0), 0, 0));
		
		if (!showDedupe) {
			dedupOn.setEnabled(false);
			dedupOn.setSelected(true);
			button.setVisible(false);
		}
		
		JPanel filterPanel = new JPanel(new GridBagLayout());
		filterPanel.setBorder(BorderFactory.createTitledBorder("Data source filtering"));
		filterOn = new JCheckBox("Use only records that satisfy the following condition:");
		filterOn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				expressionEditor.setEnabled(filterOn.isSelected());
			}
		});
		
//		if (!showDedupe) {
//			enableDuplicatesComponents(true);
//			dedupOn.setEnabled(false);
//			dedupOn.setSelected(true);
//			config = new DeduplicationConfig(sourceAction.getDataSource());
//		} else {
//			dedupOn.setEnabled(true);
			if (originalDataSource != null && originalDataSource.getDeduplicationConfig() != null) {
				config = originalDataSource.getDeduplicationConfig();
				config.fixIfNeeded(sourceAction.getDataSource());
				if (config.getMinusFile() != null) {
					saveDuplicates.setSelected(true);
					filePanel.setFileName(config.getMinusFile());
				}
				enableDuplicatesComponents(true);
				dedupOn.setSelected(true);
				button.setEnabled(true);
			} else {
				config = new DeduplicationConfig(sourceAction.getDataSource());
				enableDuplicatesComponents(dedupOn.isSelected());
			}
//		}
		dedupOn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				enableDuplicatesComponents(((JCheckBox)e.getSource()).isSelected());
			}
		});
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AbstractDataSource source = sourceAction.getDataSource();
				DeduplicationWizard wizard = new DeduplicationWizard(source, config, activeWizard, button);
				if (wizard.getResult() == AbstractWizard.RESULT_OK) {
					config = wizard.getDeduplicationConfig();
				}
			}
		});
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		filterPanel.add(filterOn, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.insets = new Insets(0, 5, 0, 5);
		filterPanel.add(expressionEditor = new FilterExpressionEditor(activeWizard, originalDataSource == null ? sourceAction.getDataSource() : originalDataSource), c);
		
		JPanel main = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		main.add(dedupePanel, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		main.add(filterPanel, c);
		
		if (originalDataSource != null && originalDataSource.getFilter() != null) {
			filterOn.setSelected(true);
			expressionEditor.setEnabled(true);
		}
		
		return main;
	}

	private void enableDuplicatesComponents(boolean enabled) {
		button.setEnabled(enabled);
		saveDuplicates.setEnabled(enabled);
		enableMinusFileOption();
	}

	private void enableMinusFileOption() {
		filePanel.setEnabled(saveDuplicates.isSelected());
	}

	public void dispose() {
		this.sourceAction = null;
	}

	public boolean endStep(AbstractWizard wizard) {
		
		if (saveDuplicates.isSelected() && !correct(filePanel.getFileName())) {
			filePanel.setError(true);
			JOptionPane.showMessageDialog(activeWizard, "Duplicates file name cannot be empty.");
			return false;
		} else if (saveDuplicates.isSelected()){
			config.setMinusFile(filePanel.getFileName().isEmpty() ? null : filePanel.getFileName());
		} else {
			config.setMinusFile(null);
		}
		
		try {
			if (filterOn.isSelected()) {
				this.filterExpression = expressionEditor.getFilter();
			} else {
				this.filterExpression = null;
			}
			return true;
		} catch (RJException e) {
			JXErrorDialog.showDialog(activeWizard, "Error in fiter expression", e);
			return false;
		}
	}

	private boolean correct(String text) {
		return !text.trim().isEmpty();
	}

	public void setSize(int width, int height) {
	}

	public DeduplicationConfig getDeduplicationConfig() {
		if (dedupOn.isSelected()) {
			return config;
		} else {
			return null;
		}
	}
	
	public Filter getFilter() {
		return filterExpression;
	}

}
