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


package cdc.datamodel.converters;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cdc.configuration.Configuration;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.utils.RJException;
import edu.emory.mathcs.util.xml.DOMUtils;

public class DummyConverter extends AbstractColumnConverter {

	private DataColumnDefinition[] columnIn;
	private DataColumnDefinition[] columnOut;
	
	public DummyConverter(String name, DataColumnDefinition column, Map props) {
		super(props);
		columnIn = new DataColumnDefinition[] {column};
		columnOut = new DataColumnDefinition[] {new ConverterColumnWrapper(column)};
	}
	
	public DummyConverter(String name, DataColumnDefinition columnIn, DataColumnDefinition columnOut, Map props) {
		super(props);
		this.columnIn = new DataColumnDefinition[] {columnIn};
		if (columnOut instanceof ConverterColumnWrapper) {
			this.columnOut = new DataColumnDefinition[] {columnOut};
		} else {
			this.columnOut = new DataColumnDefinition[] {new ConverterColumnWrapper(columnOut)};
		}
	}

	public DataCell[] convert(DataCell[] dataCells) {
		return dataCells;
	}

	public DataColumnDefinition[] getExpectedColumns() {
		return columnIn;
	}

	public DataColumnDefinition[] getOutputColumns() {
		return columnOut;
	}
	
	public static AbstractColumnConverter fromXML(Element element, Map genericColumns) throws RJException {
		
		String name = DOMUtils.getAttribute(element, Configuration.NAME_ATTR);
		String columnName = DOMUtils.getAttribute(element, "column");
		Element paramsNode = DOMUtils.getChildElement(element, Configuration.PARAMS_TAG);
		Map params = Configuration.parseParams(paramsNode);
		DataColumnDefinition column = (DataColumnDefinition) genericColumns.get(columnName);
		DataColumnDefinition newColumn = new DataColumnDefinition(name, column.getColumnType(), column.getSourceName());
		newColumn.setEmptyValues(getEmptyValues(readEmptyValues(element), 0));
		return new DummyConverter(name, column, newColumn, params);
	}
	
	public void saveToXML(Document doc, Element conv) {
		DOMUtils.setAttribute(conv, "column", this.columnIn[0].getColumnName());
		DOMUtils.setAttribute(conv, Configuration.NAME_ATTR, this.columnOut[0].getColumnName());
		saveEmptyValuesToXML(doc, conv, columnOut);
		Configuration.appendParams(doc, conv, getProperties());
	}

//	public void updateName(String newName) {
//		setName(newName);
//		DataColumnDefinition newColumn = new DataColumnDefinition(newName, columnIn[0].getColumnType(), columnIn[0].getSourceName());
//		this.columnOut = new DataColumnDefinition[] {newColumn};
//	}

}
