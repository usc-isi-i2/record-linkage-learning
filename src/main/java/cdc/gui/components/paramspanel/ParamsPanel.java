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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cdc.gui.DialogListener;
import cdc.gui.validation.Validator;
import cdc.utils.StringUtils;

public class ParamsPanel extends JPanel implements DialogListener {
	
	private ParamPanelField[] fields;
	private String[] params;
	private FieldCreator defaultCreator = new DefaultParamPanelFieldCreator();
	private Map validators = new HashMap();
	private int index = 0;
	private Map listeners;
	private String[] labels;
	private String[] defaultValues;
	
	private List appended = new ArrayList();
	
	public ParamsPanel(String[] params, String[] labels) {
		this(params, labels, null, null);
	}
	
	public ParamsPanel(String[] params, String[] labels, Map listeners) {
		this(params, labels, null, listeners);
	}
	
	public ParamsPanel(String[] params, String[] labels, String[] defaultValues) {
		this(params, labels, defaultValues, null);
	}
	
	public ParamsPanel(String[] params, String[] labels, String[] defaultValues, Map listeners) {
		if (listeners == null) {
			listeners = new HashMap();
		}
		this.listeners = listeners;
		setLayout(new GridBagLayout());
		createFields(params, labels, defaultValues);
		
	}
	
	private void createFields(String[] params, String[] labels, String[] defaultValues) {
		index = 0;
		this.removeAll();
		this.params = params;
		this.labels = labels;
		this.defaultValues = defaultValues;
		this.fields = new ParamPanelField[params.length];
		for (int i = 0; i < labels.length; i++) {
			FieldCreator creator = (FieldCreator)listeners.get(params[i]);
			if (creator == null) {
				creator = defaultCreator;
			}
			if (defaultValues != null) {
				fields[i] = creator.create(this, params[i], labels[i], defaultValues[i]);
			} else {
				fields[i] = creator.create(this, params[i], labels[i], null);
			}
			GridBagConstraints[] c = getNextConstraint();
			JComponent componentLabel = fields[i].getComponentLabel();
			JComponent componentInputField = fields[i].getComponentInputField();
			if (componentLabel != null) {
				add(componentLabel, c[0]);
			}
			if (componentInputField != null) {
				add(componentInputField, c[1]);
			}
		}
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.VERTICAL;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 200;
		add(Box.createRigidArea(new Dimension(2, 2)), c);
		
		for (Iterator iterator = appended.iterator(); iterator.hasNext();) {
			JComponent comp = (JComponent) iterator.next();
			JPanel holder = new JPanel();
			holder.add(comp);
			GridBagConstraints[] cArr = getNextConstraint();
			cArr[0].gridwidth = 2;
			add(holder, cArr[0]);
		}
	}

	public void insertField(int position, String param, String label, String defaultValue) {
		insertField(position, param, label, defaultValue, null);
	}
	
	public void insertField(int position, String param, String label, String defaultValue, FieldCreator creator) {
		String[] newParams = new String[this.params.length + 1];
		String[] newLabels = new String[this.labels.length + 1];
		String[] defaultVals = new String[this.defaultValues.length + 1];
		
		System.arraycopy(this.params, 0, newParams, 0, position);
		System.arraycopy(this.params, position, newParams, position + 1, this.params.length - position);
		newParams[position] = param;
		
		System.arraycopy(this.labels, 0, newLabels, 0, position);
		System.arraycopy(this.labels, position, newLabels, position + 1, this.labels.length - position);
		newLabels[position] = label;
		
		System.arraycopy(this.defaultValues, 0, defaultVals, 0, position);
		System.arraycopy(this.defaultValues, position, defaultVals, position + 1, this.defaultValues.length - position);
		defaultVals[position] = defaultValue;
		
		if (creator != null) {
			listeners.put(param, creator);
		}
		
		createFields(newParams, newLabels, defaultVals);
	}
	
	private GridBagConstraints[] getNextConstraint() {
		GridBagConstraints c1 = new GridBagConstraints();
		c1.insets = new Insets(5, 10, 5, 5);
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.anchor = GridBagConstraints.CENTER;
		c1.weightx = 0;
		c1.gridx = 0;
		c1.gridy = index;
		
		GridBagConstraints c2 = new GridBagConstraints();
		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.anchor = GridBagConstraints.PAGE_START;
		c2.weightx = 1;
		c2.gridx = 1;
		c2.gridy = index++;
		
		return new GridBagConstraints[] {c1, c2};
	}

	public ParamsPanel() {
		add(new JLabel("No parameters need to be configured."));
		this.params = new String[] {};
	}

	public void setValidators(Map validators) {
		this.validators  = validators;
	}
	
	public String getParameterValue(String paramName) {
		for (int i = 0; i < params.length; i++) {
			if (params[i].equals(paramName)) {
				if (StringUtils.isNullOrEmptyNoTrim(fields[i].getValue())) {
					return null;
				}
				return fields[i].getValue();
			}
		}
		return null;
	}

	public Map getParams() {
		Map params = new HashMap();
		for (int i = 0; i < this.params.length; i++) {
			params.put(this.params[i], getParameterValue(this.params[i]));
		}
		return params;
	}

	public void append(JComponent slope) {
		appended.add(slope);
		JPanel holder = new JPanel();
		holder.add(slope);
		GridBagConstraints[] c = getNextConstraint();
		c[0].gridwidth = 2;
		add(holder, c[0]);
	}

	public void cancelPressed(JDialog parent) {
	}

	public boolean okPressed(JDialog parent) {
		return doValidate();
	}
	
	public void windowClosing(JDialog parent) {
	}

	public boolean doValidate() {
		boolean ret = true;
		for (int i = 0; i < params.length; i++) {
			Validator v = (Validator) validators.get(params[i]);
			if (v != null && !v.validate(this, fields[i], getParameterValue(params[i]))) {
				ret = false;
			}
		}
		return ret;
	}

	public void setPropertyValue(String property, String value) {
		for (int i = 0; i < params.length; i++) {
			if (params[i].equals(property)) {
				this.defaultValues[i] = value;
				this.fields[i].setValue(value);
			}
		}
	}
	
	
}
