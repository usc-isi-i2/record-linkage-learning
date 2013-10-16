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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import cdc.components.AbstractResultsSaver;
import cdc.gui.DialogListener;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.MainFrame;
import cdc.gui.OptionDialog;
import cdc.gui.components.table.TablePanel;
import cdc.gui.external.JXErrorDialog;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.impl.join.strata.StrataJoinWrapper;
import cdc.impl.resultsavers.DeduplicatingResultsSaver;
import cdc.impl.resultsavers.ResultSaversGroup;
import cdc.utils.GuiUtils;
import cdc.utils.RJException;

public class ResultsConfigureSaversAction extends WizardAction {
	
	class RadioSelector implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			updateSelection();
		}
	}
	
	private static final String resolve = "<html>When two different records are linked with the same record from other file and have the same match score:</html>";
	private static final String dedupeParam = "deduplication";
	private static final String dedupeLeftConfig = "left";
	private static final String dedupeRightConfig = "right";
	private static final String dedupeBothConfig = "both";
	private static final String resolveParam = "delete-duplicates";
	private static final String resolveParamAsk = "ask";
	
	private TablePanel table;
	
	private JRadioButton noDedupe;
	private JRadioButton dedupeLeft;
	private JRadioButton dedupeRight;
	private JRadioButton dedupeBoth;
	
	private JLabel resolveLabel;
	private JRadioButton resolveDelete;
	private JRadioButton resolveDoNothing;
	private JRadioButton resolveManual;

	private AbstractWizard parent;
	
	private class NewSaverPanel extends JPanel implements DialogListener {
		private AbstractResultsSaver saver;
		private JComboBox activeCombo;
		public NewSaverPanel(OptionDialog dialog, AbstractResultsSaver saver) {
			JPanel internalPanel = new JPanel();
			internalPanel.setPreferredSize(new Dimension(350, 100));
			activeCombo = new JComboBox();
			activeCombo.setPreferredSize(new Dimension(350, (int)activeCombo.getPreferredSize().getHeight()));
			GUIVisibleComponent[] comps = GuiUtils.getAvailableSavers();
			for (int i = 0; i < comps.length; i++) {
				activeCombo.addItem(comps[i]);
			}
			activeCombo.addActionListener(new ComboListener(dialog, internalPanel, null));
			int selected = 0;
			if (saver != null) {
				for (int i = 0; i < comps.length; i++) {
					if (comps[i].getProducedComponentClass().equals(saver.getClass())) {
						comps[i].restoreValues(saver);
						selected = i;
						break;
					}
				}
			}
			
			JLabel activeLabel = new JLabel("Result saver type:");
			JPanel comboPanel = new JPanel();
			comboPanel.setLayout(new BoxLayout(comboPanel, BoxLayout.PAGE_AXIS));
			
			JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			typePanel.add(activeLabel);
			typePanel.add(activeCombo);
			comboPanel.add(typePanel);
			
			JScrollPane scrollPanel = new JScrollPane(internalPanel);
			scrollPanel.setPreferredSize(new Dimension(550, 150));
			comboPanel.add(scrollPanel);
			activeCombo.setSelectedIndex(selected);
			add(comboPanel);
		}
		
		public AbstractResultsSaver getConfiguredSaver() {
			return saver; 
		}

		public void cancelPressed(JDialog parent) {
		}

		public boolean okPressed(JDialog parent) {
			try {
				GUIVisibleComponent compGen = (GUIVisibleComponent) activeCombo.getSelectedItem();
				saver = (AbstractResultsSaver) compGen.generateSystemComponent();
				return true;
			} catch (RJException e) {
				JXErrorDialog.showDialog(parent, "Error creating results saver", e);
			} catch (IOException e) {
				JXErrorDialog.showDialog(parent, "Error creating results saver", e);
			}
			return false;
		}

		public void windowClosing(JDialog parent) {
		}
		
	}
	
	public JPanel beginStep(AbstractWizard wizard) {
		parent = wizard;
		
		table = new TablePanel(new String[] {"Result saver type", "Parameters"}, true);
		table.setBorder(BorderFactory.createTitledBorder("Results savers"));
		table.addAddButtonListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				OptionDialog newSaver = new OptionDialog(parent, "New results saver");
				NewSaverPanel panel = new NewSaverPanel(newSaver, null);
				newSaver.setMainPanel(panel);
				if (newSaver.getResult() == OptionDialog.RESULT_OK) {
					AbstractResultsSaver saver = panel.getConfiguredSaver();
					table.addRow(new Object[] {saver, saver.getProperties()});
				}
			}
		});
		table.addEditButtonListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				OptionDialog newSaver = new OptionDialog(parent, "Edit results saver");
				NewSaverPanel panel = new NewSaverPanel(newSaver, (AbstractResultsSaver)((Object[])table.getSelectedRows()[0])[0]);
				newSaver.setMainPanel(panel);
				if (newSaver.getResult() == OptionDialog.RESULT_OK) {
					AbstractResultsSaver saver = panel.getConfiguredSaver();
					table.replaceRow(table.getSelectedRowId()[0], new Object[] {saver, saver.getProperties()});
				}
			}
		});
		
		JPanel deduplication = new JPanel();
		deduplication.setBorder(BorderFactory.createTitledBorder("Results deduplication"));
		
		JPanel conflicts = new JPanel();
		conflicts.setBorder(BorderFactory.createTitledBorder("Deduplication conflicts"));
		
		prepareDeduplication(deduplication);
		prepareConflictResolving(conflicts);
		
		if (MainFrame.main.getConfiguredSystem().getJoin() instanceof StrataJoinWrapper) {
			dedupeBoth.setEnabled(false);
			dedupeLeft.setEnabled(false);
			dedupeRight.setEnabled(false);
			noDedupe.setEnabled(true);
			updateSelection();
		}
		
		JPanel buffer = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		buffer.add(table, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.BOTH;
		buffer.add(deduplication, c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.BOTH;
		buffer.add(conflicts, c);
		
		return buffer;
	}

	private void prepareConflictResolving(JPanel conflicts) {
		
		resolveLabel = new JLabel();
		resolveLabel.setHorizontalAlignment(JLabel.LEFT);
		resolveDoNothing = new JRadioButton("Do nothing");
		resolveDelete = new JRadioButton("Choose one linkage randomly and delete others");
		resolveManual = new JRadioButton("Ask me what to do");
		
		ButtonGroup group1 = new ButtonGroup();
		group1.add(resolveDoNothing);
		group1.add(resolveDelete);
		group1.add(resolveManual);
		resolveDoNothing.setSelected(true);
		resolveDelete.setEnabled(false);
		resolveDoNothing.setEnabled(false);
		resolveManual.setEnabled(false);
		resolveLabel.setText(String.format(resolve, new Object[] {getColor(resolveDoNothing)}));
		
		conflicts.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 20, 0, 20), 0, 0);
		conflicts.add(resolveLabel, c);
		c = new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
		conflicts.add(resolveDoNothing, c);
		c = new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
		conflicts.add(resolveDelete, c);
		c = new GridBagConstraints(0, 3, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
		conflicts.add(resolveManual, c);
		
	}

	private void prepareDeduplication(JPanel deduplication) {
		String s1 = MainFrame.main.getConfiguredSystem().getSourceA().getSourceName();
		String s2 = MainFrame.main.getConfiguredSystem().getSourceB().getSourceName();;
		noDedupe = new JRadioButton("No deduplication");
		dedupeLeft = new JRadioButton(String.format("<html>Every record from source '%s' can be linked with at most one record from source '%s'</html>", new Object[] {s1, s2}));
		dedupeRight = new JRadioButton(String.format("<html>Every record from source '%s' can be linked with at most one record from source '%s'</html>", new Object[] {s2, s1}));
		dedupeBoth = new JRadioButton(String.format("<html>Every record from source '%s' can be linked with at most one record from source '%s' and ", new Object[] {s1, s2}) + 
				String.format("every record from source '%s' can be linked with at most one record from source '%s'</html>", new Object[] {s2, s1}));
		
		ButtonGroup group = new ButtonGroup();
		group.add(noDedupe);
		group.add(dedupeLeft);
		group.add(dedupeRight);
		group.add(dedupeBoth);
		
		noDedupe.addActionListener(new RadioSelector());
		dedupeLeft.addActionListener(new RadioSelector());
		dedupeRight.addActionListener(new RadioSelector());
		dedupeBoth.addActionListener(new RadioSelector());
		noDedupe.setSelected(true);
		
		deduplication.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
		deduplication.add(noDedupe, c);
		c = new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
		deduplication.add(dedupeLeft, c);
		c = new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
		deduplication.add(dedupeRight, c);
		c = new GridBagConstraints(0, 3, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
		deduplication.add(dedupeBoth, c);
		dedupeBoth.setVerticalTextPosition(SwingConstants.TOP);
	}

	private String getColor(JRadioButton label) {
		Color c = UIManager.getColor(label.isEnabled() ? "Label.foreground" : "Label.disabledForeground");
		c = c == null ? new Color(0,0,0) : c;
		return pad(Integer.toHexString(c.getRed())) + pad(Integer.toHexString(c.getGreen())) + pad(Integer.toHexString(c.getBlue()));
	}

	private String pad(String hexString) {
		return hexString.length() == 1 ? "0" + hexString : hexString;
	}

	public boolean endStep(AbstractWizard wizard) {
		if (table.getRows().length == 0) {
			JOptionPane.showMessageDialog(wizard, "At least one results saver is required.");
			return false;
		}
		return true;
	}

	public AbstractResultsSaver getResultsSaver() {
		Object[] rows = table.getRows();
		AbstractResultsSaver[] cols = new AbstractResultsSaver[rows.length];
		for (int i = 0; i < cols.length; i++) {
			cols[i] = (AbstractResultsSaver) ((Object[])rows[i])[0];
		}
		if (noDedupe.isSelected()) {
			return cols.length == 1 ? cols[0] : new ResultSaversGroup(cols);
		}
		try {
			return new DeduplicatingResultsSaver(cols, getDedupeConfig());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private Map getDedupeConfig() {
		Map map = new HashMap();
		if (dedupeLeft.isSelected()) {
			map.put(dedupeParam, dedupeLeftConfig);
			map.put(resolveParam, getParamResolve());
		} else if (dedupeRight.isSelected()) {
			map.put(dedupeParam, dedupeRightConfig);
			map.put(resolveParam, getParamResolve());
		} else if (dedupeBoth.isSelected()) {
			map.put(dedupeParam, dedupeBothConfig);
			map.put(resolveParam, getParamResolve());
		}
		return map;
	}
	
	private String getParamResolve() {
		if (resolveDelete.isSelected()) {
			return "true";
		} else if (resolveManual.isSelected()) {
			return resolveParamAsk;
		} else {
			return "false";
		}
	}

	public void setResultSavers(AbstractResultsSaver savers) {
		if (savers == null) return;
		table.removeAllRows();
		
		if (savers instanceof ResultSaversGroup) {
			noDedupe.setSelected(true);
			AbstractResultsSaver[] children = ((ResultSaversGroup)savers).getChildren();
			for (int i = 0; i < children.length; i++) {
				table.addRow(new Object[] {children[i], children[i].getProperties()});
			}
		} else if (savers instanceof DeduplicatingResultsSaver) {
			Map props = ((DeduplicatingResultsSaver)savers).getProperties();
			if (props.get(dedupeParam).equals(dedupeLeftConfig)) {
				dedupeLeft.setSelected(true);
			} else if (props.get(dedupeParam).equals(dedupeRightConfig)) {
				dedupeRight.setSelected(true);
			} else if (props.get(dedupeParam).equals(dedupeBothConfig)) {
				dedupeBoth.setSelected(true);
			} else {
				noDedupe.setSelected(true);
			}
			if (props.get(resolveParam).equals("true")) {
				resolveDelete.setSelected(true);
			} else if (props.get(resolveParam).equals(resolveParamAsk)) {
				resolveManual.setSelected(true);
			} else {
				resolveDoNothing.setSelected(true);
			}
			updateSelection();
			AbstractResultsSaver[] children = ((DeduplicatingResultsSaver)savers).getChildren();
			for (int i = 0; i < children.length; i++) {
				table.addRow(new Object[] {children[i], children[i].getProperties()});
			}
		} else {
			table.addRow(new Object[] {savers, savers.getProperties()});
		}
	}
	
	private void updateSelection() {
		resolveDoNothing.setEnabled(!noDedupe.isSelected());
		resolveDelete.setEnabled(!noDedupe.isSelected());
		resolveManual.setEnabled(!noDedupe.isSelected());
		//separator.setEnabled(!noDedupe.isSelected());
		//resolveLabel.setText(String.format(resolve, new Object[] {getColor(resolveDoNothing)}));
	}

	public void dispose() {
		table = null;
	}

	public void setSize(int width, int height) {
		
	}

}

