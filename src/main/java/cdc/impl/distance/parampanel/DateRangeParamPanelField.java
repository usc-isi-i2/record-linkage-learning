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


package cdc.impl.distance.parampanel;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cdc.gui.components.dynamicanalysis.ChangedConfigurationListener;
import cdc.gui.components.paramspanel.ParamPanelField;

public class DateRangeParamPanelField extends ParamPanelField {

	private Option[] values = new Option[]{
			new Option("Milisecond(s)", 1L),
			new Option("Second(s)", 1000L),
			new Option("Minute(s)", 60 * 1000L),
			new Option("Hour(s)", 60 * 60 * 1000L),
			new Option("Day(s)", 24 * 60 * 60 * 1000L),
			new Option("Year(s)", 365 * 24 * 60 * 60 * 1000L),
	};
	
	private static class Option {
		
		long value;
		String label;
		
		public Option(String label, long value) {
			this.label = label;
			this.value = value;
		}
		
		public String toString() {
			return label;
		}
		
	}
	
	private class ChangeListenerProxy implements ChangeListener {
		
		private ChangedConfigurationListener listener;
		
		public ChangeListenerProxy(ChangedConfigurationListener listener) {
			this.listener = listener;
		}
		
		public void stateChanged(ChangeEvent arg0) {
			listener.configurationChanged();
		}
		
	}
	
	private class ActionListenerProxy implements ActionListener {

		private ChangedConfigurationListener listener;
		
		public ActionListenerProxy(ChangedConfigurationListener listener) {
			this.listener = listener;
		}
		
		public void actionPerformed(ActionEvent arg0) {
			listener.configurationChanged();
		}
		
	}
	
	private JLabel paramName;
	private JComboBox multiplier1;
	private JSpinner value1;
	private JPanel panel;
	private String userLabel;
	private Map listeners = new HashMap();
	private Map listenersCombo = new HashMap();
	
	public DateRangeParamPanelField(JComponent parent, String param, String label, String defaultValue) {
		multiplier1 = new JComboBox(values);
		multiplier1.setPreferredSize(new Dimension(multiplier1.getPreferredSize().width, 20));
		value1 = new JSpinner(new SpinnerNumberModel(0, 0, 999999, 1));
		value1.setPreferredSize(new Dimension(60, 20));
		paramName = new JLabel(label);
		//paramName.setPreferredSize(new Dimension(225, (int)paramName.getPreferredSize().getHeight()));
		
		panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		//panel.add(paramName);
		panel.add(value1);
		panel.add(multiplier1);
		this.userLabel = label;
		setValue(defaultValue);
	}

	public void addConfigurationChangeListener(ChangedConfigurationListener configurationListener) {
		ChangeListenerProxy proxy = new ChangeListenerProxy(configurationListener);
		value1.addChangeListener(proxy);
		listeners.put(configurationListener, proxy);
		ActionListenerProxy proxyA = new ActionListenerProxy(configurationListener);
		multiplier1.addActionListener(proxyA);
		listenersCombo.put(configurationListener, proxyA);
	}

	public void error(String message) {
		throw new RuntimeException("Not implemented.");
	}

	public JComponent getComponentInputField() {
		return panel;
	}

	public JComponent getComponentLabel() {
		return paramName;
	}
	
	public String getUserLabel() {
		return userLabel;
	}

	public String getValue() {
		long multi = ((Option)multiplier1.getSelectedItem()).value;
		int value = Integer.parseInt(this.value1.getValue().toString());
		return String.valueOf(multi * value);
	}

	public void removeConfigurationChangeListener(ChangedConfigurationListener configurationListener) {
		value1.removeChangeListener((ChangeListener) listeners.get(configurationListener));
		listeners.remove(configurationListener);
		multiplier1.removeActionListener((ActionListener) listenersCombo.get(configurationListener));
		listenersCombo.remove(configurationListener);
	}

	public void setValue(String val) {
		long value1 = Long.parseLong(val);
		for (int i = values.length - 1; i >= 0; i--) {
			if (value1 % values[i].value == 0) {
				this.value1.setValue(new Integer((int) (value1 / values[i].value)));
				this.multiplier1.setSelectedIndex(i);
				break;
			}
		}
	}

}
