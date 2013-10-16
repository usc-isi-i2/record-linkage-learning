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


package cdc.gui.components.linkagesanalysis.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;

public class DetailsDialogSingleSource extends AbstractDetailsDialog {
	
	private DataColumnDefinition[][] dataModel;
	
	private JTable left;
	
	public DetailsDialogSingleSource(Window parent, DataColumnDefinition[][] dataModel) {
		super(parent, "Record details");
		setSize(500, 300);
		this.dataModel = dataModel;
		
		TableModel leftModel = getMinusModel();
		left = new JTable(leftModel) {
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		
		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(new JScrollPane(left));
		leftPanel.setBorder(BorderFactory.createTitledBorder("Record attributes"));
		
		setLayout(new GridBagLayout());
		
		add(leftPanel, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
	}

	private DefaultTableModel getMinusModel() {
		return new DefaultTableModel(new String[] {"Attribute name", "Attribute value"}, 0);
	}

	public void setDetail(DataRow object) {
		left.setModel(getMinusModel());
		List leftModel = new ArrayList(Arrays.asList(dataModel[0]));
		for (Iterator iterator = leftModel.iterator(); iterator.hasNext();) {
			DataColumnDefinition col = (DataColumnDefinition) iterator.next();
			((DefaultTableModel)left.getModel()).addRow(new Object[] {col.getColumnName(), object.getData(col).getValue()});
		}
	}
	
}
