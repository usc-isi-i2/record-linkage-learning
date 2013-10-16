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


package cdc.gui.components.uicomponents;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;

import cdc.gui.MainFrame;
import cdc.gui.OptionDialog;
import cdc.gui.external.JXErrorDialog;
import cdc.utils.Log;
import cdc.utils.Props;

public class PropertiesPanel extends JPanel {
	
	private static PropertiesPanel panel;
	
	private static final int NAME_COLUMN = 0;
	private static final int VALUE_COLUMN = 1;
	
	private JTable table;
	private DefaultTableModel model;
	private List list;
	private Properties editors = new Properties();
	
	public PropertiesPanel(Properties props) {
		setLayout(new BorderLayout());
		
		loadEditors();
		
		String[][] data = new String[props.keySet().size()][];
		int k = 0;
		list = new ArrayList(props.keySet());
		Collections.sort(list);
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			String name = (String) iterator.next();
			data[k] = new String[2];
			data[k][0] = name;
			data[k++][1] = props.getProperty(name);
		}
		model = new DefaultTableModel(data, new String[] {"Property", "Value"});
		table = new JTable(model) {
			public boolean isCellEditable(int arg0, int arg1) {
				return arg1 == VALUE_COLUMN;
			}
			public TableCellEditor getCellEditor(int arg0, int arg1) {
				return PropertiesPanel.this.getCellEditor(arg0);
			}
		};
		table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		JScrollPane scroll = new JScrollPane(table);
		add(scroll);
	}
	
	private void loadEditors() {
		File f = new File("editors.properties");
		if (f.exists()) {
			try {
				FileInputStream is = new FileInputStream(f);
				editors.load(is);
			} catch (IOException e) {
				JXErrorDialog.showDialog(MainFrame.main, e.getLocalizedMessage(), e);
			}
		} else {
			Log.log(getClass(), "Warning: File editors.properties was not found.");
		}
	}

	private TableCellEditor getCellEditor(int listIndex) {
		String property = (String) list.get(listIndex);
		String editor = editors.getProperty(property);
		if (editor == null || editor.equals("text")) {
			JTextField field = new JTextField();
			field.setBorder(null);
			return new DefaultCellEditor(field);
		} else if (editor.equals("boolean")) {
			String[] vals = new String[] {"Yes", "No"};
			return new DefaultCellEditor(new JComboBox(vals));
		} else if (editor.equals("integer") || editor.equals("long") || editor.equals("real")) {
			//For now - just a text field.
			//TODO use a JSpinner for numbers
			JTextField field = new JTextField();
			field.setBorder(null);
			return new DefaultCellEditor(field);
		}
		return null;
	}
	
	public Properties getProperties() {
		Properties props = new Properties();
		for (int i = 0; i < model.getRowCount(); i++) {
			String name = (String) model.getValueAt(i, NAME_COLUMN);
			String value = (String) model.getValueAt(i, VALUE_COLUMN);
			//System.out.println(name + "--" + value);
			props.put(name, value);
		}
		return props;
	}
	
	public static void main(String[] args) {
		OptionDialog dialog = new OptionDialog((JDialog)null, "Properties");
		panel = new PropertiesPanel(Props.getProperties());
		dialog.setMainPanel(panel);
		if (dialog.getResult() == OptionDialog.RESULT_OK) {
			panel.getProperties();
		}
	}
	
}
