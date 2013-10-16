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


package cdc.gui.components.datasource.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import cdc.datamodel.DataColumnDefinition;
import cdc.gui.MainFrame;
import cdc.gui.OptionDialog;
import cdc.gui.components.datasource.JDataSource.Brick;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.components.table.TablePanel;
import cdc.gui.validation.NonEmptyValidator;

public class AddEmptyValuesListener implements ActionListener{

	private MainFrame main;
	private DataColumnDefinition column;
	
	private OptionDialog dialog;
	private TablePanel table;
	
	public AddEmptyValuesListener(MainFrame main, Brick brick) {
		this.main = main;
		this.column = brick.col;
	}

	public void actionPerformed(ActionEvent e) {
	
		table = new TablePanel(new String[] {"Empty values"}, false, true);
		String[] emptyValues = column.getEmptyValues();
		if (emptyValues != null) {
			for (int i = 0; i < emptyValues.length; i++) {
				table.addRow(new Object[] {emptyValues[i]});
			}
		}
		
		table.addAddButtonListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				OptionDialog addEmptyValue = new OptionDialog(dialog, "Add empty value");
				ParamsPanel panel = new ParamsPanel(new String[] {"value"}, new String[] {"New empty value"});
				Map validators = new HashMap();
				validators.put("value", new NonEmptyValidator());
				panel.setValidators(validators);
				addEmptyValue.setMainPanel(panel);
				//addEmptyValue.setPreferredSize(new Dimension(300, 200));
				addEmptyValue.pack();
				if (addEmptyValue.getResult() == OptionDialog.RESULT_OK) {
					table.addRow(new Object[] {panel.getParameterValue("value")});
				}
			}
		});
		
		dialog = new OptionDialog(main, "Empty values (attribute " + column.getColumnName() + ")");
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(table, BorderLayout.CENTER);
		dialog.setMainPanel(mainPanel);
		dialog.setPreferredSize(new Dimension(400, 300));
		if (dialog.getResult() == OptionDialog.RESULT_OK) {
			Object[] emptysObj = table.getRows();
			String[] emptysStr = new String[emptysObj.length];
			for (int i = 0; i < emptysStr.length; i++) {
				emptysStr[i] = (String)((Object[])emptysObj[i])[0];
			}
			column.setEmptyValues(emptysStr);
		}
	}

}
