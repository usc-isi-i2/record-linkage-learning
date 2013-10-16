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


package cdc.impl.deduplication.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.Configs;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.dynamicanalysis.AnalysisWindowProvider;
import cdc.gui.components.uicomponents.LabelWithSliderPanel;
import cdc.gui.external.JXErrorDialog;
import cdc.impl.conditions.AbstractConditionPanel;
import cdc.impl.conditions.ConditionItem;
import cdc.utils.GuiUtils;
import cdc.utils.RJException;

public class DeduplicationConditionPanel extends AbstractConditionPanel {

	private JComboBox avaialbleMethods = new JComboBox(GuiUtils.getAvailableDistanceMetrics());
	private GUIVisibleComponent componentCreator;
	private GUIVisibleComponent oldCreator;
	private Window parent;
	private AnalysisWindowProvider analysisButtonListener;
	private JPanel comboSpecificPanel;
	private DefaultListModel attributesListModel = new DefaultListModel();
	private JList attributesList = new JList(attributesListModel);
	private AbstractDistance distance;
	private LabelWithSliderPanel emptyScore = new LabelWithSliderPanel("Score for matching empty value", 0.0, 1.0, 0.0);
	
	private JTextField weight = new JTextField(3);
	
	public DeduplicationConditionPanel(AbstractDataSource source, Window parent) {
		
		this.parent = parent;
		analysisButtonListener = new AnalysisWindowProvider(parent, source, this);
		for (int i = 0; i < avaialbleMethods.getItemCount(); i++) {
			GUIVisibleComponent gui = (GUIVisibleComponent) avaialbleMethods.getItemAt(i);
			gui.addChangedConfigurationListener(analysisButtonListener);
		}
		
		setLayout(new GridBagLayout());
		
		DataColumnDefinition[] availableAttributes = source.getDataModel().getOutputFormat();
		attributesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		for (int i = 0; i < availableAttributes.length; i++) {
			attributesListModel.addElement(availableAttributes[i]);
		}
		attributesList.setSelectedIndex(0);
		JPanel attributesPanel = new JPanel();
		attributesPanel.setBorder(BorderFactory.createTitledBorder("Available columns"));
		JScrollPane scroll = new JScrollPane(attributesList);
		scroll.setPreferredSize(new Dimension(400, 100));
		attributesPanel.add(scroll);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH;
		this.add(attributesPanel, c);
		
		JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		comboPanel.add(new JLabel("Distance metric"));
		comboPanel.add(avaialbleMethods);
		avaialbleMethods.addActionListener(new ActionListener() {
			private Map cache = new HashMap();
			public void actionPerformed(ActionEvent e) {
				if (oldCreator != null) {
					oldCreator.configurationPanelClosed();
				}
				componentCreator = (GUIVisibleComponent) avaialbleMethods.getSelectedItem();
				JPanel cachedPanel = (JPanel) cache.get(componentCreator);
				if (cachedPanel == null) {
					cachedPanel = (JPanel)componentCreator.getConfigurationPanel(new Object[] {new Boolean(false), DeduplicationConditionPanel.this, DeduplicationConditionPanel.this.parent, attributesList, attributesList, new Integer(100)}, 400, 170);
					cache.put(componentCreator, cachedPanel);
				}
				comboSpecificPanel.removeAll();
				comboSpecificPanel.add(cachedPanel);
				DeduplicationConditionPanel.this.validate();
				DeduplicationConditionPanel.this.repaint();
				oldCreator = componentCreator;
				analysisButtonListener.configurationChanged();
			}
		});
		
		comboSpecificPanel = new JPanel();
		JScrollPane comboSpecificScroll = new JScrollPane(comboSpecificPanel);
		comboSpecificScroll.setPreferredSize(new Dimension(500, 180));
		
		JPanel methodSelectionPanel = new JPanel();
		methodSelectionPanel.setLayout(new GridBagLayout());
		methodSelectionPanel.setBorder(BorderFactory.createTitledBorder("Select distance metric"));
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		methodSelectionPanel.add(comboPanel, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		methodSelectionPanel.add(comboSpecificScroll, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 2;
		this.add(methodSelectionPanel, c);
		
		JPanel emptyVals = new JPanel(new GridBagLayout());
		emptyVals.setBorder(BorderFactory.createTitledBorder("Empty values"));
		emptyVals.add(emptyScore, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 5, 0, 0), 0, 0));
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 0;
		this.add(emptyVals, c);
		
		
		JLabel label = new JLabel("Condition weight: ");
		label.setPreferredSize(new Dimension(120, 20));
		JPanel weightsSumPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		weightsSumPanel.setBorder(BorderFactory.createTitledBorder("Select weight"));
		weightsSumPanel.add(label);
		//weight.setPreferredSize(new Dimension(40, 20));
		//weight.setBorder(BorderFactory.createEtchedBorder());
		weight.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {analysisButtonListener.configurationChanged();}
			public void insertUpdate(DocumentEvent e) {analysisButtonListener.configurationChanged();}
			public void removeUpdate(DocumentEvent e) {analysisButtonListener.configurationChanged();}
		});
		weightsSumPanel.add(weight);
		
		c = new GridBagConstraints();
		c.gridx = 0; 
		c.gridy = 3;
		c.weightx = 0.6;
		c.fill = GridBagConstraints.BOTH;
		this.add(weightsSumPanel, c);
		
		JPanel showExamples = new JPanel(new FlowLayout());
		showExamples.setBorder(BorderFactory.createTitledBorder("Dynamic analysis"));
		JButton examplesButton = Configs.getAnalysisButton();
		examplesButton.addActionListener(analysisButtonListener);
		showExamples.add(examplesButton);
		c = new GridBagConstraints();
		c.gridx = 1; 
		c.gridy = 3;
		c.weightx = 0.4;
		c.fill = GridBagConstraints.BOTH;
		this.add(showExamples, c);
		avaialbleMethods.setSelectedIndex(0);
		
		emptyScore.addSliderListener(analysisButtonListener);
		
		
	}
	
	public void restoreValues(AbstractDistance distance, DataColumnDefinition attribute, int weight, double emptyMatchScore) {
		for (int i = 0; i < attributesListModel.getSize(); i++) {
			if (attributesListModel.get(i).equals(attribute)) {
				attributesList.setSelectedIndex(i);
				break;
			}
		}
		for (int i = 0; i < avaialbleMethods.getItemCount(); i++) {
			if (((GUIVisibleComponent)avaialbleMethods.getItemAt(i)).getProducedComponentClass().equals(distance.getClass())) {
				((GUIVisibleComponent)avaialbleMethods.getItemAt(i)).restoreValues(distance);
				avaialbleMethods.setSelectedIndex(i);
				break;
			}
		}
		this.weight.setText(String.valueOf(weight));
		emptyScore.setValue(emptyMatchScore);
	}

	public ConditionItem getConditionItem() {
		if (distance == null) {
			return null;
		}
		ConditionItem conditionItem = new ConditionItem((DataColumnDefinition)attributesList.getSelectedValue(), (DataColumnDefinition)attributesList.getSelectedValue(), distance, Integer.parseInt(weight.getText()));
		conditionItem.setEmptyMatchScore(emptyScore.getValueDouble());
		return conditionItem;
	}

	public void cancelPressed(JDialog parent) {
	}

	public boolean okPressed(JDialog parent) {
		distance = null;
		if (attributesList.getSelectedIndex() == -1) {
			JOptionPane.showMessageDialog(parent, "Please select column");
			return false;
		}
		
		try {
			Integer.parseInt(weight.getText());
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(parent, "Weight should be an integer number.");
			return false;
		}
		
		if (!((GUIVisibleComponent)avaialbleMethods.getSelectedItem()).validate(parent)) {
			return false;
		}
		
		try {
			distance = (AbstractDistance) componentCreator.generateSystemComponent();
			return true;
		} catch (RJException e) {
			JXErrorDialog.showDialog(parent, "Error creating distance method", e);
		} catch (IOException e) {
			JXErrorDialog.showDialog(parent, "Error creating distance method", e);
		}
		return false;
	}

	public void windowClosing(JDialog parent) {
	}

}
