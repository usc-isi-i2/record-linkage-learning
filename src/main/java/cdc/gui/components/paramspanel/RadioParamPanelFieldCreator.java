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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JRadioButton;

import cdc.gui.components.dynamicanalysis.ChangedConfigurationListener;

public class RadioParamPanelFieldCreator implements FieldCreator {

	public class RadioListener implements ItemListener {
		private JComponent component;
		public RadioListener(JComponent component) {
			this.component = component;
		}
		public void itemStateChanged(ItemEvent e) {
			JRadioButton src = (JRadioButton)e.getSource();
			component.setEnabled(src.isSelected());
			if (src.isSelected()) {
				List list = (List) componentsMap.get(group);
				if (list != null) {
					for (Iterator iterator = list.iterator(); iterator.hasNext();) {
						WeakReference ref = (WeakReference) iterator.next();
						if (ref.get() != null) {
							if (component != ref.get()) {
								((JComponent)ref.get()).setEnabled(false);
							}
						}
					}
				}
			}
		}
	}

	private static Map componentsMap = new HashMap();
	private ButtonGroup group;
	private ComponentFactoryInterface factory;
	private ChangedConfigurationListener listener;
	
	public RadioParamPanelFieldCreator(ComponentFactoryInterface factory, ChangedConfigurationListener listener) {
		this.factory = factory;
		this.listener = listener;
		componentsMap.put(group, new ArrayList());
	}
	
	public RadioParamPanelFieldCreator(ButtonGroup group, ComponentFactoryInterface factory, ChangedConfigurationListener listener) {
		this.group = group;
		this.factory = factory;
		this.listener = listener;
		componentsMap.put(group, new ArrayList());
	}
	
	public RadioParamPanelFieldCreator(ComponentFactoryInterface factory) {
		this.factory = factory;
		this.group = new ButtonGroup();
		componentsMap.put(group, new ArrayList());
	}
	
	public RadioParamPanelFieldCreator(ButtonGroup group, ComponentFactoryInterface factory) {
		this.factory = factory;
		this.group = group;
		componentsMap.put(group, new ArrayList());
	}
	
	public ParamPanelField create(JComponent parent, String param, String label, String defaultValue) {
		JRadioButton button = new JRadioButton(label);
		JComponent component = factory.createComponent();
		component.setEnabled(false);
		registerComponent(component);
		button.addItemListener(new RadioListener(component));
		group.add(button);
		if (group.getButtonCount() == 1) {
			button.setSelected(true);
		}
		if (!defaultValue.equals("0")) {
			button.setSelected(true);
		}
		
		RadioParamPanelField field = new RadioParamPanelField(factory, button, component, label, defaultValue);
		if (listener != null) {
			field.addConfigurationChangeListener(listener);
		}
		return field;
	}

	private void registerComponent(JComponent component) {
		List list = (List) componentsMap.get(group);
		list.add(new WeakReference(component));
	}

}
