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


package cdc.utils;

import cdc.components.AbstractJoinCondition;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.converters.AbstractColumnConverter;
import cdc.datamodel.converters.DummyConverter;
import cdc.datamodel.converters.ModelGenerator;
import cdc.impl.conditions.WeightedJoinCondition;

public class HTMLUtils {

	public static String encodeTable(String[][] strings) {
		return encodeTable(strings, false);
	}
	
	public static String encodeTable(String[][] strings, boolean lines) {
		StringBuilder b =  new StringBuilder();
		b.append("<table ").append(lines ? "border=\"1\">" : ">");
		for (int i = 0; i < strings.length; i++) {
			b.append("<tr>");
			for (int j = 0; j < strings[i].length; j++) {
				b.append("<td>");
				if (j == 0) {
					b.append("<b>");
				}
				b.append(strings[i][j]);
				if (j == 0) {
					b.append("</b>");
				}
				b.append("</td>");
			}
			b.append("</tr>");
		}
		b.append("</table>");
		return b.toString();
	}

	public static String encodeSourceDataModel(ModelGenerator dataModel) {
		StringBuilder builder = new StringBuilder();
		builder.append("Attributes selection:");
		AbstractColumnConverter[] converters = dataModel.getConverters();
		String[][] table = new String[converters.length + 1][3];
		table[0][0] = "Attribute name(s)";
		table[0][1] = "Input attribute(s)";
		table[0][2] = "Converter";
		for (int i = 1; i < table.length; i++) {
			table[i][0] = encodeDataColumns(converters[i - 1].getOutputColumns());
			table[i][1] = encodeDataColumns(converters[i - 1].getExpectedColumns());
			if (converters[i - 1] instanceof DummyConverter) {
				table[i][2] = "None";
			} else {
				table[i][2] = converters[i - 1].toString();
			}
		} 
		builder.append(encodeTable(table, true));
		return builder.toString();
	}

//	private static String encodeConverter(AbstractColumnConverter converter) {
//		StringBuilder builder = new StringBuilder();
//		DataColumnDefinition[] in = converter.getExpectedColumns();
//		DataColumnDefinition[] out = converter.getOutputColumns();
//		
//			table[i][0] = encodeStrings();
//			if (converter instanceof DummyConverter) {
//				builder.append("Attribute ");
//				builder.append("'");
//				builder.append(out[0].getColumnName());
//				builder.append("'");
//			} else {
//				builder.append("Attribute").append(out.length > 1 ? "s " : " ");
//				for (int i = 0; i < out.length; i++) {
//					if (i > 0) {
//						builder.append(", ");
//					}
//					builder.append("'");
//					builder.append(out[i].getColumnName());
//					builder.append("'");
//				}
//				builder.append("<br>Created from attribute").append(in.length > 1 ? "s: " : ": ");
//				for (int i = 0; i < in.length; i++) {
//					if (i > 0) {
//						if (i == in.length - 1) {
//							builder.append(" and ");
//						} else {
//							builder.append(", ");
//						}
//					}
//					builder.append("'");
//					builder.append(in[i].getColumnName());
//					builder.append("'");
//				}
//			}
//		}
//		return builder.toString();
//	}

	private static String encodeDataColumns(DataColumnDefinition[] columns) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < columns.length; i++) {
			if (i > 0) {
				b.append("<br>");
			}
			b.append(columns[i].getColumnName());
		}
		return b.toString();
	}

	public static String encodeJoinCondition(AbstractJoinCondition joinCondition) {
		StringBuilder b = new StringBuilder();
		String[][] table = null;
		table = new String[joinCondition.getDistanceFunctions().length + 1][];
		if (joinCondition instanceof WeightedJoinCondition) {
			WeightedJoinCondition c = (WeightedJoinCondition)joinCondition;
			table[0] = new String[4];
			table[0][0] = "Attribute (" + c.getLeftJoinColumns()[0].getSourceName() + ")";
			table[0][1] = "Attribute (" + c.getRightJoinColumns()[0].getSourceName() + ")";
			table[0][2] = "Distance function";
			table[0][3] = "Weight";
			for (int i = 1; i < table.length; i++) {
				table[i] = new String[4];
				table[i][0] = c.getLeftJoinColumns()[i - 1].getColumnName();
				table[i][1] = c.getRightJoinColumns()[i - 1].getColumnName();
				table[i][2] = c.getDistanceFunctions()[i - 1].toString();
				table[i][3] = String.valueOf(c.getWeights()[i - 1]);
			}
		} else {
			
		}
		return b.append(encodeTable(table, true)).toString();
	}

	public static String getHTMLHeader() {
		return "<html>";
	}

}
