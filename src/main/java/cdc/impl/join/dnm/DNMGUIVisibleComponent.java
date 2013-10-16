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


package cdc.impl.join.dnm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoinCondition;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.OptionDialog;
import cdc.gui.components.table.TablePanel;
import cdc.utils.RJException;

public class DNMGUIVisibleComponent extends GUIVisibleComponent {

	private class IDToNameMapping {
		private int id;
		private String label;

		public IDToNameMapping(int id, String label) {
			this.id = id;
			this.label = label;
		}
		
		public int getId() {
			return id;
		}

		public String toString() {
			return label;
		}
	}
	
	private AbstractDataSource sourceA;
	private AbstractDataSource sourceB;
	private AbstractJoinCondition joinCondition;
	private DataColumnDefinition[] outModel;
	
	private JDialog parent;
	private TablePanel tablePanel;
	
	public Object generateSystemComponent() throws RJException, IOException {
		Map params = new HashMap();
		Object[] clustering = tablePanel.getRows();
		String ids = "";
		for (int i = 0; i < clustering.length; i++) {
			Object[] row = (Object[]) clustering[i];
			for (int j = 0; j < joinCondition.getDistanceFunctions().length; j++) {
				if (row[0].equals(joinCondition.getDistanceFunctions()[j]) &&
						row[1].equals(joinCondition.getLeftJoinColumns()[j]) &&
						row[2].equals(joinCondition.getRightJoinColumns()[j])) {
					ids += j + ",";
					break;
				}
			}
		}
		if (ids.length() != 0) {
			ids = ids.substring(0, ids.length() - 1);
		}
		params.put(DNMJoin.PROP_CLUSTER_PARAMS, ids);
		return new DNMJoin(sourceA, sourceB, joinCondition, outModel, params);
	}

	public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
		
		this.sourceA = (AbstractDataSource) objects[0];
		this.sourceB = (AbstractDataSource) objects[1];
		this.outModel = (DataColumnDefinition[]) objects[2];
		this.joinCondition = (AbstractJoinCondition) objects[3];
		this.parent = (JDialog)objects[4];
		
		tablePanel = new TablePanel(new String[] {"Distance function", "Column (" + sourceA.getSourceName() + ")",
				"Column(" + sourceB.getSourceName() + ")"}, false);
		
		tablePanel.addAddButtonListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				List list = new ArrayList();
				for (int i = 0; i < joinCondition.getDistanceFunctions().length; i++) {
					if (!ClusteringFunctionFactory.canBeUsed(joinCondition.getDistanceFunctions()[i])) {
						continue;
					}
					list.add(new IDToNameMapping(i, joinCondition.getLeftJoinColumns()[i].getColumnName() + " <--> " + joinCondition.getRightJoinColumns()[i].getColumnName()));
				}
				IDToNameMapping[] available = (IDToNameMapping[]) list.toArray(new IDToNameMapping[] {});
				for (int j = 0; j < available.length; j++) {
					addMapping(available[j]);
				}
				JPanel addPanel = new JPanel(new BorderLayout());
				JComboBox combo = new JComboBox(available);
				combo.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
				JLabel label = new JLabel("Choose clustering attribute");
				label.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
				addPanel.add(label, BorderLayout.NORTH);
				addPanel.add(combo, BorderLayout.CENTER);
				OptionDialog dialog = new OptionDialog(parent, "Add clustering attribute");
				dialog.setMainPanel(addPanel);
				dialog.setLocationRelativeTo(parent);
				if (dialog.getResult() == OptionDialog.RESULT_OK) {
					addMapping((IDToNameMapping) combo.getSelectedItem());
				}
			}

		});
		
		tablePanel.setSize(sizeX, sizeY);
		tablePanel.setPreferredSize(new Dimension(sizeX, sizeY));
		
		List list = new ArrayList();
		for (int i = 0; i < joinCondition.getDistanceFunctions().length; i++) {
			if (!ClusteringFunctionFactory.canBeUsed(joinCondition.getDistanceFunctions()[i])) {
				continue;
			}
			list.add(new IDToNameMapping(i, joinCondition.getLeftJoinColumns()[i].getColumnName() + " <--> " + joinCondition.getRightJoinColumns()[i].getColumnName()));
		}
		IDToNameMapping[] available = (IDToNameMapping[]) list.toArray(new IDToNameMapping[] {});
		for (int j = 0; j < available.length; j++) {
			addMapping(available[j]);
		}
		
		return tablePanel;	
	}
	
	private void addMapping(IDToNameMapping smapping) {
		int sel = smapping.getId();
		Object[] rows = tablePanel.getRows();
		boolean found = false;
		if (rows != null) {
			for (int j = 0; j < rows.length; j++) {
				Object[] row = (Object[])rows[j];
				if (row[0].equals(joinCondition.getDistanceFunctions()[sel]) &&
						row[1].equals(joinCondition.getLeftJoinColumns()[sel]) &&
						row[2].equals(joinCondition.getRightJoinColumns()[sel])) {
					found = true;
				}
			}
		}
		if (!found) {
			tablePanel.addRow(new Object[] {joinCondition.getDistanceFunctions()[sel], joinCondition.getLeftJoinColumns()[sel], joinCondition.getRightJoinColumns()[sel]});
		}
	}

	public Class getProducedComponentClass() {
		return DNMJoin.class;
	}

	public String toString() {
		return "Distance neighbourhood method (under development)";
	}

	public boolean validate(JDialog dialog) {
		return true;
	}
	
	public void setSize(int sizeX, int sizeY) {
		tablePanel.setSize(sizeX, sizeY);
		tablePanel.setPreferredSize(new Dimension(sizeX, sizeY));
		parent.validate();
		parent.repaint();
	}

}
