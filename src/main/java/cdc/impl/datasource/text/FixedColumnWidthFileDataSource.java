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


package cdc.impl.datasource.text;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

import cdc.components.AbstractDataSource;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.gui.GUIVisibleComponent;
import cdc.utils.HTMLUtils;
import cdc.utils.Log;
import cdc.utils.RJException;
import cdc.utils.Utils;

public class FixedColumnWidthFileDataSource extends AbstractDataSource {

	private static final int logLevel = Log.getLogLevel(FixedColumnWidthFileDataSource.class);
	
	public static final String SCHEMA_FILE_EXT = "fields";
	public static final String PARAM_FILE_NAME = "input-file";
	public static final String PARAM_SCHEMA_FILE = "schema-file";
	
	private boolean closed = false;
	private BufferedReader isReader = null;
	private String filePath = null;

	private int size = -1;
	
	public FixedColumnWidthFileDataSource(String name, Map params) throws RJException, IOException {
		super(name, 
				readSchema(Utils.getParam(params, PARAM_SCHEMA_FILE, false) != null ? Utils.getParam(params, PARAM_SCHEMA_FILE, false) : getGenericSchemaFileName(Utils.getParam(params, PARAM_FILE_NAME, true)), name), 
				params);
		checkFile(Utils.getParam(params, PARAM_FILE_NAME, true));
		Log.log(FixedColumnWidthFileDataSource.class, "Data source created", 1);
	}
	
	private void checkFile(String fileName) throws RJException, IOException {
		this.filePath = fileName;
		size  = LineNumber.size(Utils.parseFilePath(fileName)[0]);
		Log.log(getClass(), getSourceName() + ": Number of records in data source: " + size, 1);
	}

	public static String getGenericSchemaFileName(String fileName) {
		int schemaPosition = 0;
		fileName = Utils.parseFilePath(fileName)[0];
		if ((schemaPosition = fileName.lastIndexOf('.')) == -1) {
			return fileName + "." + SCHEMA_FILE_EXT;
		} else {
			String file = fileName.substring(0, schemaPosition);
			return file + "." + SCHEMA_FILE_EXT;
		}
	}
	
	public static DataColumnDefinition[] readSchema(String schemaFile, String sourceName) throws RJException {
		Log.log(FixedColumnWidthFileDataSource.class, "Reading schema from file " + schemaFile, 1);
		int index = 0;
		List columns = new ArrayList();
		try {
			BufferedReader reader = new BufferedReader(Utils.openTextFileForReading(schemaFile, true));
			String schemaLine = null;
			while ((schemaLine = reader.readLine()) != null && (schemaLine = schemaLine.trim()) != "") {
				schemaLine = schemaLine.replaceAll("  ", " ");
				int separator = schemaLine.lastIndexOf(" ");
				if (separator == -1) {
					throw new RJException("Error in schema definition file: " + schemaFile + " in line: " + schemaLine);
				}
				String[] schema = new String[2];
				schema[0] = schemaLine.substring(0, separator);
				schema[1] = schemaLine.substring(separator + 1, schemaLine.length());
				int length = Integer.parseInt(schema[1]);
				Log.log(FixedColumnWidthFileDataSource.class, "Adding column: " + schema[0] + "(len: " + length + ")", 2);
				columns.add(new FixedWidthColumnDefinition(schema[0], DataColumnDefinition.TYPE_STRING, sourceName, index, index + length));
				index += length;
			}
		} catch (FileNotFoundException e) {
			throw new RJException("Cannot open schema definition file: " + schemaFile, e);
		} catch (IOException e) {
			throw new RJException("Error reading schema definition file: " + schemaFile, e);
		}
		Log.log(FixedColumnWidthFileDataSource.class, "Schema succesfully read", 1);
		return (DataColumnDefinition[]) columns.toArray(new DataColumnDefinition[] {});
	}

	private void ensureOpen() throws RJException, IOException {
		if (closed) {
			reset();
			closed = false;
		}
		if (this.isReader == null) {
			this.isReader = new BufferedReader(Utils.openTextFileForReading(this.filePath, true));
		}
	}
	
	public boolean canSort() {
		return false;
	}

	protected void doClose() throws IOException, RJException {
		Log.log(FixedColumnWidthFileDataSource.class, "Close called", 1);
		if (closed) {
			return;
		}
		if (this.isReader != null) {
			this.isReader.close();
		}
		closed = true;
	}

	public DataRow nextRow() throws IOException, RJException {
		Log.log(FixedColumnWidthFileDataSource.class, "Next row called", 3);
		ensureOpen();
		String line = this.isReader.readLine();
		if (line == null) {
			return null;
		}
		DataColumnDefinition[] model = getDataModel().getInputFormat();
		DataCell[] cells = new DataCell[model.length];
		for (int i = 0; i < cells.length; i++) {
			String value = line.substring(((FixedWidthColumnDefinition)model[i]).getStartPosition(), ((FixedWidthColumnDefinition)model[i]).getEndPosition());
			if (logLevel >= 3) {
				Log.log(FixedColumnWidthFileDataSource.class, "Value retrieved: " + value, 3);
			}
			value = value.trim();
			cells[i] = new DataCell(model[i].getColumnType(), parseData(model[i].getColumnType(), value));
		}
		DataRow row = new DataRow(getDataModel().getOutputFormat(), getDataModel().generateOutputRow(cells), getSourceName());
		if (logLevel >= 3) {
			Log.log(FixedColumnWidthFileDataSource.class, "Row read: " + row, 3);
		}
		return row;
	}

	private Object parseData(int columnType, String string) {
		if (columnType == DataColumnDefinition.TYPE_NUMERIC) {
			return new Double(string);
		} else if (columnType == DataColumnDefinition.TYPE_DATE) {
			return new Date(string);
		} else {
			return string;
		}
	}
	
//	public DataRow[] getNextRows(int size) throws IOException, RJException {
//		List rows = new ArrayList();
//		DataRow row = null;
//		while (rows.size() != size && (row = getNextRow()) != null) {
//			rows.add(row);
//		}
//		return (DataRow[]) rows.toArray(new DataRow[] {});
//	}

	protected void doReset() throws IOException, RJException {
		Log.log(FixedColumnWidthFileDataSource.class, "Reset called", 2);
		if (this.isReader != null) {
			this.isReader.close();
		}
		this.isReader = new BufferedReader(Utils.openTextFileForReading(this.filePath, true));
	}

	public Node saveToXML() {
		return null;
	}

	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new FixedWidthConfigurationPanel();
	}
	
	protected void finalize() throws Throwable {
		//System.out.println(getClass() + " finalize");
		close();
	}
	
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof FixedColumnWidthFileDataSource)) return false;
		FixedColumnWidthFileDataSource that = (FixedColumnWidthFileDataSource)arg0;
		return areTheSameProperties(this, that);
	}

	public AbstractDataSource copy() throws IOException, RJException {
		AbstractDataSource dataSource = new FixedColumnWidthFileDataSource(getSourceName(), getProperties());
		dataSource.setModel(getDataModel());
		dataSource.setFilter(getFilter());
		return dataSource;
	}
	
	public String toHTMLString() {
		StringBuilder b = new StringBuilder();
		b.append(HTMLUtils.getHTMLHeader());
		b.append(HTMLUtils.encodeTable(new String[][] {{"Source name:", getSourceName()}, {"Source type: ", "Fixed column width input file"}}));
		b.append(HTMLUtils.encodeSourceDataModel(getDataModel()));
		b.append("</html>");
		return b.toString();
	}

	public long size() {
		return size;
	}
}
