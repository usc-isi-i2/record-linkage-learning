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


public class DataColumnDefinition implements Serializable {
	 
	public static final int TYPE_STRING = 1;
	public static final int TYPE_DATE = 2;
	public static final int TYPE_NUMERIC = 3;
	
	private String columnName;
	private int type;
	private String sourceName;
	private int hash = 0;
	
	private String[] emptyValues;
	
	private boolean key = false;
	
	public DataColumnDefinition(String columnName, int type, String sourceName) {
		this.columnName = columnName;
		this.type = type;
		this.sourceName = sourceName;
		this.hash = (sourceName + "_" + columnName).hashCode();
	}
	
	public String getColumnName() {
		return columnName;
	}
	
	public int getColumnType() {
		return type;
	}
	
	public String getSourceName() {
		return sourceName;
	}
	
	public int hashCode() {
		return hash;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof DataColumnDefinition)) {
			return false;
		}
		DataColumnDefinition that = (DataColumnDefinition)obj;
		return this.columnName.equals(that.columnName) && this.sourceName.equals(that.sourceName) && getClass().equals(that.getClass());
	}
	
	public String toString() {
		return columnName + "@" + sourceName;
	}

	public boolean isKey() {
		return key;
	}

	public void setKey(boolean key) {
		this.key = key;
	}

	public void setName(String parameterValue) {
		columnName = parameterValue;
	}
	
	public String[] getEmptyValues() {
		return emptyValues;
	}
	
	public void setEmptyValues(String[] emptyValues) {
		this.emptyValues = emptyValues;
	}
	
	public static final String normalizeColumnName(String name) {
		//Column name cannot contain any non-word character (see java Pattern doc for definition of non-word chars)
		return name.replaceAll("\\W", "");
	}

}
