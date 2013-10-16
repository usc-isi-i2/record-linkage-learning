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


package cdc.impl.join.snm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoinCondition;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.components.table.TablePanel;
import cdc.gui.validation.NonEmptyValidator;
import cdc.gui.validation.NumberValidator;
import cdc.impl.datasource.text.CSVDataSource;
import cdc.utils.RJException;

public class SNMGUIVisibleComponent extends GUIVisibleComponent {

	private AbstractDataSource sourceA;
	private AbstractDataSource sourceB;
	private AbstractJoinCondition joinCondition;
	private DataColumnDefinition[] outModel;
	
	private ParamsPanel paramsPanel;
	private TablePanel sortOrder;
	private JPanel panel;
	
	public Object generateSystemComponent() throws RJException, IOException {
		Object[] sort = sortOrder.getRows();
		DataColumnDefinition[][] s = new DataColumnDefinition[sort.length][];
		for (int i = 0; i < s.length; i++) {
			s[i] = new DataColumnDefinition[] {(DataColumnDefinition) ((Object[])sort[i])[0], (DataColumnDefinition) ((Object[])sort[i])[1]};
		}
		Map params = paramsPanel.getParams();
		String orderA = new String();
		String orderB = new String();
		for (int i = 0; i < sort.length; i++) {
			if (i != 0) {
				orderA += ",";
				orderB += ",";
			}
			orderA += ((DataColumnDefinition)((Object[])sort[i])[0]).getColumnName();
			orderB += ((DataColumnDefinition)((Object[])sort[i])[1]).getColumnName();
		}
		params.put(SNMJoin_v1.PARAM_SORT_ORDER_A, orderA);
		params.put(SNMJoin_v1.PARAM_SORT_ORDER_B, orderB);
		return new SNMJoin_v1(sourceA.getPreprocessedDataSource(), sourceB.getPreprocessedDataSource(), outModel, joinCondition, params);
	}

	public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
		
		this.sourceA = (AbstractDataSource) objects[0];
		this.sourceB = (AbstractDataSource) objects[1];
		this.outModel = (DataColumnDefinition[]) objects[2];
		this.joinCondition = (AbstractJoinCondition) objects[3];
		
		String[] availableparams = new String[] {SNMJoin_v1.PARAM_WINDOW_SIZE};
		String[] defaults = new String[] {String.valueOf(SNMJoin_v1.DEFAULT_WINDOW_SIZE)};
		for (int i = 0; i < defaults.length; i++) {
			if (getRestoredParam(availableparams[i]) != null) {
				defaults[i] = getRestoredParam(availableparams[i]);
			}
		}
		
		paramsPanel = new ParamsPanel(
				availableparams,
				new String[] {"Window size"},
				defaults
		);
		
		Map validators = new HashMap();
		validators.put(SNMJoin_v1.PARAM_WINDOW_SIZE, new NumberValidator(NumberValidator.INTEGER));
		validators.put(CSVDataSource.PARAM_INPUT_FILE, new NonEmptyValidator());
		paramsPanel.setValidators(validators);
		sortOrder = new TablePanel(new String[] {sourceA.getSourceName(), sourceB.getSourceName()}, false, false);
		if (getRestoredParam(SNMJoin_v1.PARAM_SORT_ORDER_A) != null && getRestoredParam(SNMJoin_v1.PARAM_SORT_ORDER_B) != null) {
			String[] orderA = getRestoredParam(SNMJoin_v1.PARAM_SORT_ORDER_A).split(",");
			boolean[] used = new boolean[joinCondition.getLeftJoinColumns().length];
			for (int i = 0; i < orderA.length; i++) {
				DataColumnDefinition[] joinCols = joinCondition.getLeftJoinColumns();
				for (int j = 0; j < joinCols.length; j++) {
					if (orderA[i].equals(joinCols[j].getColumnName())) {
						sortOrder.addRow(new Object[] {joinCondition.getLeftJoinColumns()[j], joinCondition.getRightJoinColumns()[j]});
						used[j] = true;
						break;
					}
				}
			}
			for (int i = 0; i < used.length; i++) {
				if (!used[i]) {
					sortOrder.addRow(new Object[] {joinCondition.getLeftJoinColumns()[i], joinCondition.getRightJoinColumns()[i]});
				}
			}
		} else {
			for (int i = 0; i < joinCondition.getDistanceFunctions().length; i++) {
				sortOrder.addRow(new Object[] {joinCondition.getLeftJoinColumns()[i], joinCondition.getRightJoinColumns()[i]});
			}
		}
		paramsPanel.setBorder(BorderFactory.createTitledBorder("Miscellaneous parameters"));
		
		panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		panel.add(paramsPanel, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		
		JPanel sort = new JPanel(new BorderLayout());
		sort.setBorder(BorderFactory.createTitledBorder("Sort order"));
		sort.add(sortOrder);
		panel.add(sort, c);
//		c = new GridBagConstraints();
//		c.gridx = 0;
//		c.gridy = 2;
//		c.fill = GridBagConstraints.BOTH;
//		c.weightx = 1;
//		c.weighty = 1;
//		panel.add(sortOrder, c);
		return panel;
	}

	public Class getProducedComponentClass() {
		return SNMJoin_v1.class;
	}

	public String toString() {
		return "Sorted neighbourhood method";
	}

	public boolean validate(JDialog dialog) {
		return paramsPanel.doValidate();
	}
	
	public void setSize(int x, int y) {
		panel.setPreferredSize(new Dimension(x, y));
	}
	
}
