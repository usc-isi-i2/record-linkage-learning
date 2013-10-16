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


package cdc.datamodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cdc.utils.PrintUtils;

public class RowModel {
	
	private static Map models = new HashMap();

	private List columns = new ArrayList();
	private List columnsByName = new ArrayList();
	private DataColumnDefinition[] columnsInt;
	
	public synchronized static RowModel getRowModel(DataColumnDefinition[] columns) {
		String code = encode(columns);
		RowModel model = (RowModel) models.get(code);
		if (model == null) {
			model = new RowModel(columns);
			models.put(code, model);
		}
		return model;
	}
	
	private static String encode(DataColumnDefinition[] columns) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < columns.length; i++) {
			if (i > 0) {
				buffer.append("_");
			}
			buffer.append(columns[i].toString());
		}
		return buffer.toString();
	}

	private RowModel(DataColumnDefinition[] columns) {
		this.columnsInt = columns;
		for (int i = 0; i < columns.length; i++) {
			this.columns.add(columns[i]);
			this.columnsByName.add(columns[i].getColumnName());
		}
	}

	public int getCellId(DataColumnDefinition cell) {
		int index = columns.indexOf(cell);
		if (index == -1) {
			throw new RuntimeException("Column " + cell.getColumnName() + " not provided by data source " + cell.getSourceName() + ". Columns are: " + PrintUtils.printArray(columnsInt));
		}
		return index;
	}

	public DataColumnDefinition[] getColumns() {
		return columnsInt;
	}

	public int getCellId(String cell) {
		int index = columnsByName.indexOf(cell);
		if (index == -1) {
			throw new RuntimeException("Column " + cell + " not provided by this data source. Columns are: " + PrintUtils.printArray(columnsInt));
		}
		return index;
	}
	
}
