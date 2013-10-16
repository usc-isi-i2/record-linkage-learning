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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

public class DataRow implements Externalizable {
	
	private static int id = 1;
	
	private String sourceName;
	
	private int recordId;
	private DataCell[] cells;
	private transient RowModel model;
	
	//private String hashString;
	
	private Map properties = null;

	private boolean firstHashCode = true;
	private int hashCode = 0;
	
	public synchronized static final int getId() {
		return id++;
	}
	
	public DataRow() {
	}
	
	public DataRow(DataColumnDefinition[] rowModel, DataCell[] data) {
		this(rowModel, data, "Unknown data source");
	}
	
	public DataRow(DataColumnDefinition[] rowModel, DataCell[] data, String sourceName) {
		model = RowModel.getRowModel(rowModel);
		cells = data;
		this.sourceName = sourceName;
		this.recordId = getId();
		if (rowModel.length != data.length) {
			throw new RuntimeException("Row model has to have the same number of items as row cells number.");
		}
	}
	
	public DataCell getData(DataColumnDefinition cell) {
		if (cell instanceof PropertyBasedColumn) {
			return new DataCell(DataColumnDefinition.TYPE_STRING, getProperty(((PropertyBasedColumn)cell).getColumnName()));
		}
		return cells[model.getCellId(cell)];
	}
	
	public DataCell getData(String name) {
		return cells[model.getCellId(name)];
	}
	
	public DataCell[] getData() {
		return cells;
	}

	public DataColumnDefinition[] getRowModel() {
		return model.getColumns();
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof DataRow)) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		DataRow that = (DataRow)obj;
		if (this.cells.length != that.cells.length) {
			return false;
		}
		for (int i = 0; i < this.cells.length; i++) {
			DataCell c1 = this.cells[i];
			DataCell c2 = that.cells[i];
			if (!c1.equals(c2)) {
				return false;
			}
		}
		return true;
	}
	
	public int hashCode() {
		if (firstHashCode) {
			firstHashCode = false;
			hashCode = cells[0].hashCode();
			for (int j = 1; j < cells.length; j++) {
				hashCode = hashCode ^ cells[j].hashCode();
			}
		}
//		if (hashString == null) {
//			StringBuffer buffer = new StringBuffer();
//			for (int i = 0; i < cells.length; i++) {
//				buffer.append(cells[i].hashCode());
//				if (i != 0) {
//					buffer.append("_");
//				}
//			}
//			hashString = buffer.toString();
//		}
//		return hashString.hashCode();
		return hashCode;
	}
	
	public String toString() {
		return this.toString(model.getColumns(), true);
	}

	public String getSourceName() {
		return sourceName;
	}

	public String toString(DataColumnDefinition[] outFormatter, boolean fullSourceInfo) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < outFormatter.length; i++) {
			if (i > 0) {
				buffer.append(", ");
			}
			buffer.append(outFormatter[i].getColumnName()).append("=").append(cells[i].getValue());
		}
		return buffer.toString();
	}
	
	public String toString(DataColumnDefinition[] outFormatter) {
		return this.toString(outFormatter, true);
	}
	
	public void setProperty(String name, Object value) {
		if (properties == null) {
			properties = new HashMap(8);
		}
		properties.put(name, value);
	}
	
	public String getProperty(String name) {
		if (properties == null) return null;
		return (String)properties.get(name);
	}
	
	public Object getObjectProperty(String name) {
		if (properties == null) return null;
		return properties.get(name);
	}

	public Map getProperties() {
		return properties;
	}
	
	public void setProperies(Map props) {
		this.properties = props;
	}
	
	public int getRecordId() {
		return recordId;
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		
		int colsLen = in.readInt();
		DataColumnDefinition[] cols = new DataColumnDefinition[colsLen];
		for (int i = 0; i < cols.length; i++) {
			String columnName = (String) in.readObject();
			String sourceName = (String) in.readObject();
			int type = in.readInt();
			cols[i] = new DataColumnDefinition(columnName, type, sourceName);
		}
		model = RowModel.getRowModel(cols);
		cells = new DataCell[colsLen];
		for (int i = 0; i < cells.length; i++) {
			String value = (String) in.readObject();
			cells[i] = new DataCell(cols[i].getColumnType(), value);
			//cells[i].setParentRow(this);
		}
		sourceName = (String) in.readObject();
		properties = (Map) in.readObject();
		recordId = in.readInt();
	}

	public void writeExternal(ObjectOutput oo) throws IOException {
		
		DataColumnDefinition[] cols = model.getColumns();
		oo.writeInt(cols.length);
		for (int i = 0; i < cols.length; i++) {
			oo.writeObject(cols[i].getColumnName());
			oo.writeObject(cols[i].getSourceName());
			oo.writeInt(cols[i].getColumnType());
		}
		for (int i = 0; i < cells.length; i++) {
			oo.writeObject(cells[i].getValue());
		}
		oo.writeObject(sourceName);
		oo.writeObject(properties);
		oo.writeInt(recordId);
	}
	
}
