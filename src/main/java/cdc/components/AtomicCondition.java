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


package cdc.components;

import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.utils.Log;
import cdc.utils.StringUtils;

public class AtomicCondition implements Condition {

	public static String[] conds = new String[] {"<=", "<", "==", "!=", ">", ">="};
	
	private int internalLogLevel = 1;
	
	private DataColumnDefinition column;
	private int condition = - 1;
	private double numericVal;
	private String stringVal;
	private String stratumName;
	
	public AtomicCondition(DataColumnDefinition dataColumnDefinition, String operator, String value, String stratumName) {
		this.column = dataColumnDefinition;
		this.stratumName = stratumName;
		for (int i = 0; i < conds.length; i++) {
			if (conds[i].equals(operator)) {
				condition = i;
			}
		}
		//System.out.format("Operator: %s Code->%s\n", new String[] {operator, String.valueOf(condition)});
		if (condition == 2 || condition == 3) {
			stringVal = value;
		} else {
			numericVal = Double.parseDouble(value);
		}
		if (condition == -1) {
			throw new RuntimeException("Unexpected operator for stratum attribute: " + operator);
		}
	}
	
	public boolean isSatisfied(DataRow row) {
		DataCell cell = row.getData(column);
		String str = cell.getValue().toString();
		try {
			switch (condition) {
			case 0:
				if (StringUtils.isNullOrEmpty(str)) {
					return false;
				}
				return Double.parseDouble(str) <= numericVal;
			case 1:
				if (StringUtils.isNullOrEmpty(str)) {
					return false;
				}
				return Double.parseDouble(str) < numericVal;
			case 2:
				return str.equals(stringVal);
			case 3:
				return !str.equals(stringVal);
			case 4:
				if (StringUtils.isNullOrEmpty(str)) {
					return false;
				}
				return Double.parseDouble(str) > numericVal;
			case 5:
				if (StringUtils.isNullOrEmpty(str)) {
					return false;
				}
				return Double.parseDouble(str) >= numericVal;
			default:
				return false;
			}
		} catch (NumberFormatException e) {
			Log.log(getClass(), "Warning: " + e.getMessage(), internalLogLevel);
			if (internalLogLevel == 1) {
				Log.log(getClass(), "Further messages will be logged in higher log level (" + 2 + ")", internalLogLevel);
				internalLogLevel = 2;
			}
			return false;
		}
	}

	public DataColumnDefinition getColumn() {
		return column;
	}

	public String getCondition() {
		return conds[condition];
	}

	public String getValue() {
		if (stringVal == null) {
			return String.valueOf(numericVal);
		} else {
			return stringVal;
		}
	}
	
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof AtomicCondition)) {
			return false;
		}
		AtomicCondition that = (AtomicCondition) arg0;
		return that.column.equals(column) && that.condition == this.condition && that.numericVal == numericVal && 
			((that.stringVal == null && stringVal == null) || (that.stringVal != null && stringVal != null && that.stringVal.equals(stringVal)));
	}
	
	public String toString() {
		return column + conds[condition] + (stringVal == null ? String.valueOf(numericVal) : stringVal);
	}

	public String getStratumName() {
		// TODO Auto-generated method stub
		return stratumName;
	}

}
