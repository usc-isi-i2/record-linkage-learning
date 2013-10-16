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
import java.io.IOException;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import cdc.components.AbstractDataSource;
import cdc.datamodel.converters.ModelGenerator;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.external.JXErrorDialog;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.utils.GuiUtils;
import cdc.utils.RJException;

public class DSConfigureTypeAction extends WizardAction {

	private JPanel buffer;
	private JComboBox activeCombo;
	private AbstractDataSource dataSource;
	
	private int lastSelected = -1;
	private String defaultName;
	
	public DSConfigureTypeAction(String defaultSourceName) {
		this.defaultName = defaultSourceName;
	}
	
	public JPanel beginStep(AbstractWizard wizard) {
		wizard.getMainPanel().setLayout(new BorderLayout());
		if (buffer == null) {
			JPanel internalPanel = new JPanel();
			
			int selected = 1;
			activeCombo = new JComboBox();
			activeCombo.setPreferredSize(new Dimension(350, (int)activeCombo.getPreferredSize().getHeight()));
			GUIVisibleComponent[] comps = GuiUtils.getAvailableSources();
			for (int i = 0; i < comps.length; i++) {
				activeCombo.addItem(comps[i]);
				if (dataSource != null && comps[i].getProducedComponentClass().equals(dataSource.getClass())) {
					comps[i].restoreValues(dataSource);
					selected = i;
					lastSelected = selected;
				}
			}
			
			activeCombo.addActionListener(new ComboListener(wizard, internalPanel, new Object[] {defaultName},
					(int)wizard.getPreferredSize().getWidth() - 20, 
					(int)wizard.getPreferredSize().getHeight() - 170));
			JLabel activeLabel = new JLabel("Data source type:");
			JPanel comboPanel = new JPanel();
			comboPanel.setLayout(new BorderLayout());
			
			JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			typePanel.add(activeLabel);
			typePanel.add(activeCombo);
			comboPanel.add(typePanel, BorderLayout.NORTH);
			
			JScrollPane scrollPanel = new JScrollPane(internalPanel);
			comboPanel.add(scrollPanel, BorderLayout.CENTER);
			activeCombo.setSelectedIndex(selected);
			
			buffer = comboPanel;
		}
		return buffer;
		
	}
	
	public boolean endStep(AbstractWizard wizard) {
		if (!((GUIVisibleComponent)this.activeCombo.getSelectedItem()).validate(wizard)) {
			return false;
		}
		boolean altered = true;
		try {
			ModelGenerator generator = null;
			if (lastSelected == activeCombo.getSelectedIndex() && dataSource != null) {
				altered = false;
				generator = dataSource.getDataModel();
			}
			lastSelected = activeCombo.getSelectedIndex();
			AbstractDataSource oldSource = dataSource;
			dataSource = (AbstractDataSource) ((GUIVisibleComponent)this.activeCombo.getSelectedItem()).generateSystemComponent();
			if (!dataSource.equals(oldSource)) {
				altered = true;
			}
			if (!altered) {
				dataSource.setModel(generator);
			}
			return true;
		} catch (RJException e) {
			JXErrorDialog.showDialog(wizard, e.getMessage(), e);
		} catch (IOException e) {
			JXErrorDialog.showDialog(wizard, e.getMessage(), e);
		}
		return false;
	}
	
	public void setDataSource(AbstractDataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	public AbstractDataSource getDataSource() {
		return dataSource;
	}
	
	public void setSize(int width, int height) {
	}

	public void dispose() {
		this.activeCombo = null;
		this.buffer = null;
		this.dataSource = null;
	}

}
