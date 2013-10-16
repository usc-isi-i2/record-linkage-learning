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

import java.io.Serializable;

import cdc.utils.StringUtils;


public class DataCell implements Comparable, Serializable {
	
	//private DataRow parentRow;
	private Object value;
	private int type;
	
	public DataCell(int type, Object value) {
		this.type = type;
		this.value = value;
	}
	
	public Object getValue() {
		return this.value;
	}
	
	public int getValueType() {
		return this.type;
	}

	public int compareTo(Object o) {
//		if (this.value == null && data.value == null) {
//			return 0;
//		} else if (this.value != null) {
//			return 1;
//		} else if (data.value != null) {
//			return -1;
//		}
		DataCell data = (DataCell)o;
		if (this.type != data.getValueType()) {
			throw new RuntimeException("Compared DataCells have to be the same type.");
		}
		if (this.type == DataColumnDefinition.TYPE_NUMERIC) {
			Number n1 = (Number) this.value;
			Number n2 = (Number) data.getValue();
			if (n1.doubleValue() > n2.doubleValue()) {
				return 1;
			} else if (n1.doubleValue() < n2.doubleValue()) {
				return -1;
			} else {
				return 0;
			}
		} else if (this.type == DataColumnDefinition.TYPE_STRING) {
			return ((String)this.value).toLowerCase()/*.replace((char)126, (char)64).replace((char)58, (char)47).replace((char)93, (char)47).replace((char)45, (char)45)*/
			.compareTo(((String)data.getValue()).toLowerCase()/*.replace((char)126, (char)64).replace((char)58, (char)47).replace((char)93, (char)47).replace((char)45, (char)45)*/);
		} else {
			throw new RuntimeException("Not supported currently!");
		}
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof DataCell)) {
			return false;
		}
		DataCell data = (DataCell)obj;
//		if (this.type != data.getValueType()) {
//			return false;
//		}
		return this.value.equals(data.value);
	}
	
	public int hashCode() {
//		if (this.value == null) {
//			return 0;
//		}
		return this.value.hashCode();
	}

//	public DataRow getParentRow() {
//		return parentRow;
//	}
//
//	public void setParentRow(DataRow parentRow) {
//		this.parentRow = parentRow;
//	}
	
	public String toString() {
		return "Cell[value='" + value + "']";
	}
	
	public String toFullString() {
		//return "Cell(source=" + parentRow.getSourceName() + ",value='" + value + "')";	
		return toString();
	}

//	public void discard() {
//		value = null;
//		//columnName = null;
//		parentRow = null;
//	}

	public void setValue(Object value) {
		this.value = value;
	}

	public boolean isEmpty(DataColumnDefinition columnType) {
		if (StringUtils.isNullOrEmpty(value.toString())) {
			return true;
		}
		String[] emptys = columnType.getEmptyValues();
		if (emptys != null) {
			for (int i = 0; i < emptys.length; i++) {
				if (value.toString().equals(emptys[i])) {
					return true;
				}
			}
		}
		return false;
	}
}
