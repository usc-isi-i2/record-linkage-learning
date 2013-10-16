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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;

import cdc.gui.GUIVisibleComponent;

class ComboListener implements ActionListener, ComponentListener {
	
	private Object[] params;
	private JDialog wizard;
	private Map panels = new HashMap();
	private JPanel internalPanel;
	private int sizeX;
	private int sizeY;
	private GUIVisibleComponent componentGenerator;
	private GUIVisibleComponent[] comboComponents;
	
	public ComboListener(JDialog wizard, JPanel panel, Object[] params) {
		this(wizard, panel, params, null);
	}
	
	public ComboListener(JDialog wizard, JPanel panel, Object[] params, GUIVisibleComponent[] components) {
		this.wizard = wizard;
		this.internalPanel = panel;
		this.params = params;
		this.sizeX = -1;
		this.sizeY = -1;
		this.comboComponents = components;
	}
	
	public ComboListener(JDialog wizard, JPanel panel, Object[] params, int sizeX, int sizeY) {
		this(wizard, panel, params, sizeX, sizeY, null);
	}
	
	public ComboListener(JDialog wizard, JPanel panel, Object[] params, int sizeX, int sizeY, GUIVisibleComponent[] components) {
		this.wizard = wizard;
		this.internalPanel = panel;
		this.params = params;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.comboComponents = components;
	}

	public void actionPerformed(ActionEvent e) {
		JComboBox source = (JComboBox)e.getSource();
		//This is to fix a problem in OSX where you can deselect an item
		if (source.getSelectedIndex() == -1) {
			source.setSelectedIndex(0);
		}
		if (comboComponents == null) {
			componentGenerator = (GUIVisibleComponent) source.getSelectedItem();
		} else {
			componentGenerator = comboComponents[source.getSelectedIndex()];
		}
		JComponent newPanel = (JComponent) panels.get(new Integer(source.getSelectedIndex()));
		if (newPanel == null) {
			newPanel = componentGenerator.getConfigurationPanel(params, sizeX, sizeY);
			panels.put(new Integer(source.getSelectedIndex()), newPanel);
		}
		internalPanel.setLayout(new GridBagLayout());
		internalPanel.removeAll();
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		internalPanel.add(newPanel, c);
		if (sizeX != -1) {
			this.sizeX = sizeX - 60;
			this.sizeY = sizeY - 30;
			componentGenerator.setSize(sizeX, sizeY);
		}
		this.wizard.validate();
		this.wizard.repaint();
	}

	public void componentResized(ComponentEvent arg0) {
		JComponent source = (JComponent)arg0.getSource();
		this.sizeX = source.getSize().width - 60;
		this.sizeY = source.getSize().height - 30;
		componentGenerator.setSize(sizeX, sizeY);
	}

	public void componentHidden(ComponentEvent arg0) {}
	public void componentMoved(ComponentEvent arg0) {}
	public void componentShown(ComponentEvent arg0) {}

}
