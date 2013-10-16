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

import java.util.Iterator;

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class SheetsIterator implements Iterator {
	
	private HSSFWorkbook workbook;
	private String[] sheetNames;
	
	private Iterator rowIterator;
	private HSSFSheet sheet;
	private HSSFFormulaEvaluator evaluator;
	
	private int sheetNumber = 0;
	
	public SheetsIterator(HSSFWorkbook workbook) {
		this(workbook, null);
	}
	
	public SheetsIterator(HSSFWorkbook workbook, String[] sheetNames) {
		this.workbook = workbook;
		this.sheetNames = sheetNames;
		if (sheetNames == null) {
			this.sheetNames = new String[workbook.getNumberOfSheets()];
			for (int i = 0; i < this.sheetNames.length; i++) {
				this.sheetNames[i] = workbook.getSheetName(i);
			}
		}
	}
	
	public boolean hasNext() {
		while (sheet == null || !rowIterator.hasNext()) {
			if (sheetNumber == sheetNames.length) {
				return false;
			}
			//time to move to next sheet
			while ((sheet = workbook.getSheet(sheetNames[sheetNumber++])) == null) {
				if (sheetNumber == sheetNames.length) {
					return false;
				}
			}
			evaluator = new HSSFFormulaEvaluator(sheet, workbook);
			rowIterator = sheet.rowIterator();
			//skipping header
			rowIterator.next();
		}
		return rowIterator.hasNext();
	}

	public Object next() {
		return rowIterator.next();
	}
	
	public HSSFFormulaEvaluator getEvaluator() {
		return evaluator;
	}

	public void remove() {
		evaluator = null;
		sheet = null;
		if (rowIterator != null) {
			rowIterator.remove();
			rowIterator = null;
		}
	}

	public int countRecords() {
		int total = 0;
		for (int i = 0; i < sheetNames.length; i++) {
			HSSFSheet sheet = workbook.getSheet(sheetNames[i]);
			if (sheet != null) {
				total += sheet.getPhysicalNumberOfRows() - 1;
			}
		}
		return total;
	}
}
