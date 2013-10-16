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


package cdc.impl.datasource.office;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import cdc.components.AbstractDataSource;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.converters.ModelGenerator;
import cdc.gui.GUIVisibleComponent;
import cdc.utils.RJException;

public class ExcelDataSource extends AbstractDataSource {

	public static final String PARAM_FILE = "file-name";
	public static final String PARAM_SHEET = "sheet-name";
	
	private static NumberFormat formatter = NumberFormat.getInstance();
	static {
		if (formatter instanceof DecimalFormat) {
			((DecimalFormat)formatter).setDecimalSeparatorAlwaysShown(false);
		}
	}
	
	private boolean filesOpen = false;
	private HSSFWorkbook workbook;
	private SheetsIterator iterator;
	
	private InputStream stream;
	private boolean closed = false;
	
	public ExcelDataSource(String name, Map params) throws IOException, RJException {
		super(name, readDataModel(name, params), params);
	}

	private static DataColumnDefinition[] readDataModel(String sourceName, Map params) throws IOException, RJException {
		BufferedInputStream is = null;
		try {
			is = new BufferedInputStream(new FileInputStream((String)params.get(PARAM_FILE)));
			HSSFWorkbook wb = new HSSFWorkbook(new POIFSFileSystem(is));
			String[] sheets;
			if (params.get(PARAM_SHEET) != null) {
				sheets = new String[] {(String)params.get(PARAM_SHEET)};
			} else {
				sheets = new String[wb.getNumberOfSheets()];
				for (int i = 0; i < sheets.length; i++) {
					sheets[i] = wb.getSheetName(i);
				}
			}
			if (sheets.length == 0) {
				throw new RJException("Excel file " + params.get(PARAM_FILE) + " does not provide any sheets.");
			}
			List cols = new ArrayList();
			HSSFSheet sheet = wb.getSheet(sheets[0]);
			if (sheet == null) {
				//System.out.println("Thorwing: " + "Sheet " + sheets[0] + " is not provided by file " + params.get(PARAM_FILE));
				throw new RJException("Sheet '" + sheets[0] + "' is not provided by file " + params.get(PARAM_FILE));
			}
			HSSFFormulaEvaluator evaluator = new HSSFFormulaEvaluator(sheet, wb);
			//first row should provide data model
			HSSFRow row = sheet.getRow(0);
			evaluator.setCurrentRow(row);
			for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
				HSSFCell cell = row.getCell(i);
				cols.add(new ExcelDataColumnDefinition(decodeValue(cell, evaluator), DataColumnDefinition.TYPE_STRING, sourceName, i));
			}
			for (int i = 1; i < sheets.length; i++) {
				sheet = wb.getSheet(sheets[i]);
				if (sheet == null) {
					throw new RJException("Sheet '" + params.get(PARAM_SHEET) + "' is not provided by file " + params.get(PARAM_FILE));
				}
				evaluator = new HSSFFormulaEvaluator(sheet, wb);
				//first row should provide data model
				row = sheet.getRow(0);
				evaluator.setCurrentRow(row);
				List localCols = new ArrayList();
				for (i = 0; i < row.getPhysicalNumberOfCells(); i++) {
					HSSFCell cell = row.getCell(i);
					DataColumnDefinition col = new ExcelDataColumnDefinition(decodeValue(cell, evaluator), DataColumnDefinition.TYPE_STRING, sourceName, i);
					localCols.add(col);
				}
				List toRemove = new ArrayList();
				for (Iterator iterator = cols.iterator(); iterator.hasNext();) {
					DataColumnDefinition object = (DataColumnDefinition) iterator.next();
					if (!localCols.contains(object)) {
						toRemove.add(object);
					}
				}
				cols.removeAll(toRemove);
			}
			
			return (DataColumnDefinition[]) cols.toArray(new DataColumnDefinition[] {});
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	private static String decodeValue(HSSFCell cell, HSSFFormulaEvaluator evaluator) throws RJException {
		if (cell == null) {
			return "";
		}
		switch (evaluator.evaluateInCell(cell).getCellType()) {
		case HSSFCell.CELL_TYPE_BLANK:
			return "";
		case HSSFCell.CELL_TYPE_BOOLEAN:
			return String.valueOf(cell.getBooleanCellValue());
		case HSSFCell.CELL_TYPE_ERROR:
			return "";
		case HSSFCell.CELL_TYPE_FORMULA:
			break;
		case HSSFCell.CELL_TYPE_NUMERIC:
			if (HSSFDateUtil.isCellDateFormatted(cell)) {
				return cell.toString();
			} else {
				return formatter.format(cell.getNumericCellValue());
			}
		case HSSFCell.CELL_TYPE_STRING:
			return cell.getRichStringCellValue().getString();
		}
		throw new RJException("Error reading data from Excel input file");
 	}

	public boolean canSort() {
		return false;
	}

	protected void doClose() throws IOException, RJException {
		closed  = true;
		if (stream != null) {
			stream.close();
			stream = null;
		}
		workbook = null;
		if (iterator != null) {
			iterator.remove();
			iterator = null;
		}
		filesOpen = false;
	}

	public AbstractDataSource copy() throws IOException, RJException {
		ExcelDataSource source = new ExcelDataSource(getSourceName(), getProperties());
		source.setModel(getDataModel());
		source.setFilter(getFilter());
		return source;
		
	}

	protected void doReset() throws IOException, RJException {
		if (stream != null) {
			stream.close();
		}
		openFile();
		filesOpen = true;
	}

	public boolean equals(Object arg0) {
		if (!(arg0 instanceof ExcelDataSource)) {
			return false;
		}
		return areTheSameProperties(this, (AbstractDataSource)arg0);
	}

	protected DataRow nextRow() throws IOException, RJException {
		if (!filesOpen) {
			openFile();
		}
		
		if (iterator.hasNext()) {
			HSSFRow row = (HSSFRow) iterator.next();
			iterator.getEvaluator().setCurrentRow(row);
			ModelGenerator generator = getDataModel();
			DataColumnDefinition[] inputColumns = generator.getInputFormat();
			DataCell[] rowCols = new DataCell[inputColumns.length];
			for (int i = 0; i < rowCols.length; i++) {
				DataColumnDefinition col = generator.getInputFormat()[i];
				rowCols[i] = new DataCell(col.getColumnType(), decodeValue(row.getCell(((ExcelDataColumnDefinition)col).getCellId()), iterator.getEvaluator()));
			}
			return new DataRow(generator.getOutputFormat(), generator.generateOutputRow(rowCols), getSourceName());
		} else {
			return null;
		}
	}

	public long size() throws IOException, RJException {
		if (closed) {
			return -1;
		}
		if (!filesOpen) {
			openFile();
		}
		return iterator.countRecords();
	}

	private void openFile() throws IOException, RJException {
		stream = new BufferedInputStream(new FileInputStream(getProperty(PARAM_FILE)));
		workbook = new HSSFWorkbook(new POIFSFileSystem(stream));
		iterator = new SheetsIterator(workbook, getProperty(PARAM_SHEET) == null ? null : new String[] {getProperty(PARAM_SHEET)});
		filesOpen = true;
		closed = false;
	}
	
	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new ExcelDataSourceVisibleComponent();
	}

	public static void main(String[] args) throws IOException, RJException {
		Map params = new HashMap();
		params.put(PARAM_FILE, "data-sample/data-100.xls");
		params.put(PARAM_SHEET, "data-100");
		AbstractDataSource source = new ExcelDataSource("test", params);
		drain(source);
	}

	private static void drain(AbstractDataSource source) throws IOException, RJException {
		DataRow row;
		System.out.println("Size: " + source.size());
		int r = 0;
		while ((row = source.getNextRow()) != null) {
			System.out.println("Row: " + r++ + " " + row);
		}
	}
	
}
