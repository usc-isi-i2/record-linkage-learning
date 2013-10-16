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
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;
import cdc.components.AbstractDataSource;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.converters.ModelGenerator;
import cdc.gui.GUIVisibleComponent;
import cdc.impl.datasource.wrappers.ExternallySortingDataSource;
import cdc.utils.HTMLUtils;
import cdc.utils.Log;
import cdc.utils.PrintUtils;
import cdc.utils.RJException;
import cdc.utils.Utils;

public class CSVDataSource extends AbstractDataSource {

	private static final int logLevel = Log.getLogLevel(CSVDataSource.class);
	
	public static final char DEFAULT_DELIM = ';';
	
	public static final String PARAM_DELIM = "column-separator";
	public static final String PARAM_INPUT_FILE = "input-file";
	
	private String inputFilePath;
	private CSVReader parser;
	private boolean opened = false;
	private char delim = DEFAULT_DELIM;
	
	private int size = -1;
	
	public CSVDataSource(String sourceName, Map params) throws IOException, RJException {
		super(sourceName, readDataModel(sourceName, Utils.getParam(params, PARAM_INPUT_FILE, true), Utils.getParam(params, PARAM_DELIM, true).charAt(0)), params);
		inputFilePath = Utils.getParam(params, PARAM_INPUT_FILE, true);
		this.delim = Utils.getParam(params, PARAM_DELIM, true).charAt(0);
		size = LineNumber.size(Utils.parseFilePath(inputFilePath)[0]) - 1;
		Log.log(getClass(), getSourceName() + ": Number of records in data source: " + size, 1);
		Log.log(CSVDataSource.class, "Data source created. Delim is: " + delim + "; file=" + inputFilePath, 1);
	}

	public CSVDataSource(String sourceName, DataColumnDefinition[] model, String path, char delim, Map params) throws IOException, RJException {
		super(sourceName, model, params);
		inputFilePath = Utils.getParam(params, PARAM_INPUT_FILE, true);
		this.delim = Utils.getParam(params, PARAM_DELIM, true).charAt(0);
		size = LineNumber.size(Utils.parseFilePath(inputFilePath)[0]) - 1;
		Log.log(getClass(), getSourceName() + ": Number of records in data source: " + size, 1);
		Log.log(CSVDataSource.class, "Data source created. Delim is: " + delim + "; file=" + inputFilePath, 1);
	}
	
	private void doOpenFile() throws IOException, RJException {
		Log.log(CSVDataSource.class, "Opening input file", 2);
		//parser = new CSVReader(new BufferedReader(new FileReader(inputFile)), delim);
		parser = new CSVReader(new BufferedReader(Utils.openTextFileForReading(inputFilePath, true)), delim);
		opened = true;
		//skip the first, header line
		parser.readNext();
		Log.log(CSVDataSource.class, "File opened", 2);
	}
	
	public DataRow nextRow() throws IOException, RJException {
		return getNextRow(getDataModel());
	}

	private DataRow getNextRow(ModelGenerator generator) throws IOException, RJException {
		Log.log(CSVDataSource.class, "Next row called", 3);
		if (!opened) {
			doOpenFile();
		} else if (generator == null) {
			throw new RJException("Output row model generator cannot be null.");
		}
		DataCell[] rowCols = new DataCell[generator.getInputFormat().length];
		String[] csvRow;
		while ((csvRow = parser.readNext()) != null && csvRow.length == 1 && csvRow[0].trim().equals(""));
		if (csvRow == null) {
			return null;
		}
		if (logLevel >= 3) {
			Log.log(CSVDataSource.class, "Row retrieved: " + PrintUtils.printArray(csvRow), 3);
		}
		int nn = 0;
		try {
			for (int i = 0; i < rowCols.length; i++) {
				nn = i;
				rowCols[i] = new DataCell(
						generator.getInputFormat()[i].getColumnType(), 
						parseData(generator.getInputFormat()[i].getColumnType(), getValue(csvRow, ((CSVDataColumnDefinition)generator.getInputFormat()[i]).getColumnPosition())));
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new RJException("Encountered error when parsing data source. Check field separator (currently set to '" + delim + "') and check data model. " +
					"Requested column number " + (((CSVDataColumnDefinition)generator.getInputFormat()[nn]).getColumnPosition() + 1) + " of row: \n'" + PrintUtils.printArray(csvRow) + "'"); 
		}
		DataRow row = new DataRow(generator.getOutputFormat(), generator.generateOutputRow(rowCols), this.getSourceName());
		if (logLevel >= 2) {
			Log.log(CSVDataSource.class, "Row retrieved: " + row, 3);
		}
		return row;
	}
	
	private String getValue(String[] csvRow, int columnPosition) {
		if (columnPosition >= csvRow.length) {
			return "";
		} else {
			return csvRow[columnPosition].trim();
		}
		
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
//		List list = new ArrayList();
//		DataRow row = null;
//		while (list.size() < size && (row = getNextRow()) != null) {
//			list.add(row);
//		}
//		return (DataRow[]) list.toArray(new DataRow[] {});
//	}

	protected void doClose() throws IOException {
		Log.log(CSVDataSource.class, "Closing data source", 1);
		if (opened) {
			parser.close();
			opened = false;
		}
	}

	protected void doReset() throws IOException {
		Log.log(CSVDataSource.class, "Resetting data source", 2);
		if (parser != null) {
			parser.close();
			parser = new CSVReader(new BufferedReader(Utils.openTextFileForReading(inputFilePath, true)), delim);
			parser.readNext();
		}
	}

	public boolean canSort() {
		return false;
	}

	public static final DataColumnDefinition[] readDataModel(String sourceName, String fileName) throws IOException, RJException {
		return readDataModel(sourceName, fileName, DEFAULT_DELIM);
	}
	
	public static final DataColumnDefinition[] readDataModel(String sourceName, String fileName, char delim) throws IOException, RJException {
		Log.log(CSVDataSource.class, "Reading data model", 1);
		CSVReader parser = new CSVReader(new BufferedReader(Utils.openTextFileForReading(fileName, true)), delim);
		//skip the first, header line
		String[] csvRow = parser.readNext();
		Log.log(CSVDataSource.class, "Row retrieved from csv: " + PrintUtils.printArray(csvRow), 2);
		DataColumnDefinition[] model = new CSVDataColumnDefinition[csvRow.length];
		for (int i = 0; i < csvRow.length; i++) {
			model[i] = new CSVDataColumnDefinition(csvRow[i], i, DataColumnDefinition.TYPE_STRING, sourceName);
		}
		Log.log(CSVDataSource.class, "Data model read: " + PrintUtils.printArray(model), 2);
		return model;
	}

	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new CSVConfigurationPanel();
	}
	
	protected void finalize() throws Throwable {
		//System.out.println(getClass() + " finalize");
		close();
	}

	public boolean equals(Object arg0) {
		if (!(arg0 instanceof CSVDataSource)) return false;
		CSVDataSource that = (CSVDataSource)arg0;
		return areTheSameProperties(this, that);
	}
	
	public AbstractDataSource copy() throws IOException, RJException {
		CSVDataSource dataSource = new CSVDataSource(getSourceName(), getProperties());
		dataSource.setModel(getDataModel());
		dataSource.setFilter(getFilter());
		return dataSource;
	}
	
	public long size() {
		return size;
	}
	
	public String toHTMLString() {
		StringBuilder b = new StringBuilder();
		b.append(HTMLUtils.getHTMLHeader());
		b.append(HTMLUtils.encodeTable(new String[][] {{"Source name:", getSourceName()}, {"Source type: ", "CSV data source"}}));
		b.append(HTMLUtils.encodeSourceDataModel(getDataModel()));
		b.append("</html>");
		return b.toString();
	}
	
	public static void main(String[] args) throws IOException, RJException {
		Map props = new HashMap();
		props.put(PARAM_INPUT_FILE, "data-sample/data-100.csv");
		props.put(PARAM_DELIM, ",");
		AbstractDataSource test = new CSVDataSource("test", props);
		test = new ExternallySortingDataSource("test", test, test.getAvailableColumns(), null, new HashMap());
		//test.setStart(50);
		//test.setLimit(100);
		drain(test);
//		test.reset();
//		drain(test);
//		test.reset();
//		drain(test);
	}

	private static void drain(AbstractDataSource test) throws IOException, RJException {
		DataRow row;
		int size = 0;
		BufferedWriter writer = new BufferedWriter(new FileWriter("sorted-100.txt"));
		while ((row = test.getNextRow()) != null) {
			writer.write(row.toString());
			writer.write("\n");
			size++;
		}
		writer.flush();
		writer.close();
		System.out.println("Size: " + size);
	}
}
