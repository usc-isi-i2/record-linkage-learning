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
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cdc.gui.components.dynamicanalysis.ChangedConfigurationListener;

public class ComponentFactory {

	public static ComponentFactoryInterface jSpinnerFactory(SpinnerModel model) {
		return new JSpinnerFactory(model);
	}
	
	public static ComponentFactoryInterface jSpinnerFactory(SpinnerModel spinnerModelRange, Dimension dimension) {
		return new JSpinnerFactory(spinnerModelRange, dimension);
	}
	
	public static ComponentFactoryInterface labelFactory(String label) {
		return new LabelFactory(label);
	}
	
	public static ComponentFactoryInterface labelFactory(String label, Dimension d) {
		return new LabelFactory(label, d);
	}
	
	public static ComponentFactoryInterface compoundFactory(ComponentFactoryInterface[] children) {
		return new CompoundFactory(children);
	}
	
	private static class JSpinnerFactory implements ComponentFactoryInterface {
		
		private Map listeners = new HashMap();
		
		public class ChangeListenerProxy implements ChangeListener {
			private ChangedConfigurationListener listener;
			public ChangeListenerProxy(ChangedConfigurationListener propertyChangeListener) {
				listener = propertyChangeListener;
			}
			public void stateChanged(ChangeEvent e) {
				listener.configurationChanged();
			}
		}
		
		private SpinnerModel model;
		private Dimension prefSize;
		
		public JSpinnerFactory(SpinnerModel model, Dimension size) {
			this.model = model;
			this.prefSize = size;
		}
		
		public JSpinnerFactory(SpinnerModel model) {
			this.model = model;
		}
		
		public void addPropertyChangeListener(JComponent comp, ChangedConfigurationListener propertyChangeListener) {
			
			ChangeListenerProxy changeListenerProxy = new ChangeListenerProxy(propertyChangeListener);
			listeners.put(propertyChangeListener, changeListenerProxy);
			((JSpinner)comp).addChangeListener(changeListenerProxy);
			//((JSpinner.DefaultEditor)((JSpinner)comp).getEditor()).getTextField().addKeyListener(new KeyAdapterProxy(propertyChangeListener));
		}
		public void removoPropertyChangeListener(JComponent comp, ChangedConfigurationListener listener) {
			((JSpinner)comp).removeChangeListener((ChangeListener) listeners.remove(listener));
		}
		public JComponent createComponent() {
			JSpinner spinner = new JSpinner(model);
			if (prefSize != null) {
				spinner.setPreferredSize(prefSize);
			}
			return spinner;
		}
		public String retrieveValue(JComponent input) {
			return String.valueOf(((JSpinner)input).getValue());
		}
		public void setValue(JComponent input, String val) {
			((JSpinner)input).setValue(new Double(val));
		}
		public boolean isInputComponent() {
			return true;
		}
	}
	
	private static class LabelFactory implements ComponentFactoryInterface {

		private String label;
		private Dimension prefSize;
		
		public LabelFactory(String label) {
			this.label = label;
		}
		
		public LabelFactory(String label, Dimension size) {
			this.label = label;
			this.prefSize = size;
		}
		
		public void addPropertyChangeListener(JComponent radioParamPanelField, ChangedConfigurationListener propertyChangeListener) {
		}

		public JComponent createComponent() {
			JLabel label = new JLabel(this.label);
			if (prefSize != null) {
				label.setPreferredSize(prefSize);
			}
			return label;
		}

		public boolean isInputComponent() {
			return false;
		}

		public void removoPropertyChangeListener(JComponent comp, ChangedConfigurationListener listener) {
		}

		public String retrieveValue(JComponent input) {
			return null;
		}

		public void setValue(JComponent input, String val) {
		}
		
	}
	
	private static class CompoundFactory implements ComponentFactoryInterface {

		private class CompoundPanel extends JPanel {
			
			public CompoundPanel(FlowLayout flowLayout) {
				super(flowLayout);
			}

			public void setEnabled(boolean arg0) {
				super.setEnabled(arg0);
				for (int i = 0; i < children.length; i++) {
					children[i].setEnabled(arg0);
				}
			}
			
			public JComponent[] children;
		}
		
		private ComponentFactoryInterface[] childrenFactories;
		
		public CompoundFactory(ComponentFactoryInterface[] children) {
			this.childrenFactories = children;
		}
		
		public void addPropertyChangeListener(JComponent comp, ChangedConfigurationListener propertyChangeListener) {
			for (int i = 0; i < childrenFactories.length; i++) {
				childrenFactories[i].addPropertyChangeListener(getChild(comp, i), propertyChangeListener);
			}
		}

		public JComponent createComponent() {
			CompoundPanel panel = new CompoundPanel(new FlowLayout(FlowLayout.LEFT));
			JComponent[] components = new JComponent[childrenFactories.length];
			for (int i = 0; i < childrenFactories.length; i++) {
				components[i] = childrenFactories[i].createComponent();
				panel.add(components[i]);
			}
			panel.children = components;
			return panel;
		}

		public void removoPropertyChangeListener(JComponent comp, ChangedConfigurationListener listener) {
			for (int i = 0; i < childrenFactories.length; i++) {
				childrenFactories[i].removoPropertyChangeListener(getChild(comp, i), listener);
			}
		}

		public String retrieveValue(JComponent input) {
			int n = 0;
			StringBuilder param = new StringBuilder();
			for (int i = 0; i < childrenFactories.length; i++) {
				String value = childrenFactories[i].retrieveValue(getChild(input, i));
				if (value != null) {
					if (n++ != 0) {
						param.append(",");
					}
					param.append(value);
				}
			}
			return param.toString();
		}

		public void setValue(JComponent input, String val) {
			String[] valArray = val.split(",");
			int n = 0;
			for (int i = 0; i < childrenFactories.length; i++) {
				if (childrenFactories[i].isInputComponent()) {
					if (valArray.length <= n) {
						childrenFactories[i].setValue(getChild(input, i), valArray[0]);
					} else {
						childrenFactories[i].setValue(getChild(input, i), valArray[n++]);
					}
				}
			}
		}

		private JComponent getChild(JComponent input, int i) {
			return ((CompoundPanel)input).children[i];
		}

		public boolean isInputComponent() {
			return true;
		}
		
	}
	
}
