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


package cdc.gui.components.paramspanel;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cdc.gui.components.dynamicanalysis.ChangedConfigurationListener;

public class CheckBoxParamPanleField extends ParamPanelField {

	private class ActionListenerProxy implements ActionListener {

		private ChangedConfigurationListener listener;
		
		public ActionListenerProxy(ChangedConfigurationListener listener) {
			this.listener = listener;
		}
		
		public void actionPerformed(ActionEvent arg0) {
			listener.configurationChanged();
		}
		
	}
	
	private JCheckBox checkBox;
	private Map listeners = new HashMap();
	private JPanel panel;
	private JLabel label;
	
	public CheckBoxParamPanleField(JComponent parent, String param, String label, String defaultValue) {
		checkBox = new JCheckBox();
		
		panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(Box.createRigidArea(new Dimension(20, 20)));
		panel.add(checkBox);
		this.label = new JLabel(label);
		
		setValue(defaultValue);
	}

	public void addConfigurationChangeListener(ChangedConfigurationListener configurationListener) {
		ActionListenerProxy proxy = new ActionListenerProxy(configurationListener);
		listeners.put(configurationListener, proxy);
		checkBox.addActionListener(proxy);
	}

	public void error(String message) {
		throw new RuntimeException("Not implemented");
	}

	public JComponent getComponentInputField() {
		return panel;
	}

	public String getUserLabel() {
		return checkBox.getText();
	}

	public String getValue() {
		return String.valueOf(checkBox.isSelected());
	}

	public void removeConfigurationChangeListener(ChangedConfigurationListener configurationListener) {
		checkBox.removeActionListener((ActionListener) listeners.get(configurationListener));
	}

	public void setValue(String val) {
		checkBox.setSelected(Boolean.parseBoolean(val));
	}

	public JComponent getComponentLabel() {
		return label;
	}

}
