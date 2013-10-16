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


package cdc.impl.datasource.wrappers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

import cdc.components.AbstractDataSource;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.utils.CompareFunctionInterface;
import cdc.utils.Log;
import cdc.utils.PrintUtils;
import cdc.utils.RJException;
import cdc.utils.RowUtils;

public class SortingDataSource extends AbstractDataSource {

	private class RowComparator implements Comparator {

		private DataColumnDefinition[] comparedFields;
		private CompareFunctionInterface[] compareFunctions;
		
		public RowComparator(CompareFunctionInterface[] comps, DataColumnDefinition[] orderBy) {
			this.comparedFields = orderBy;
			this.compareFunctions = comps;
		}
		
		public int compare(Object o1, Object o2) {
			return RowUtils.compareRows((DataRow)o1, (DataRow)o2, comparedFields, comparedFields, compareFunctions);
		}
		
	}
	
	private AbstractDataSource parentSource;
	private DataRow[] rows;
	private int nextRow = -1;
	private RowComparator comparator;
	
	public SortingDataSource(String name, AbstractDataSource parentSource, DataColumnDefinition[] orderBy, CompareFunctionInterface[] functions, Map params) {
		super(name, params);
		this.parentSource = parentSource;
		this.comparator = new RowComparator(functions, orderBy);
		Log.log(SortingDataSource.class, "Sorting data source created", 1);
	}

	public DataRow nextRow() throws IOException, RJException {
		Log.log(SortingDataSource.class, "Next called", 2);
		if (nextRow == -1) {
			initialize();
		}
		if (nextRow >= rows.length) {
			return null;
		}
		return rows[nextRow++];
	}

//	public DataRow[] getNextRows(int size) throws IOException, RJException {
//		if (nextRow == -1) {
//			initialize();
//		}
//		if (nextRow >= rows.length) {
//			return new DataRow[0];
//		}
//		int returnSize = nextRow + size > rows.length ? rows.length - nextRow : size;
//		DataRow[] returnedRows = new DataRow[returnSize];
//		for (int i = 0; i < returnedRows.length; i++) {
//			returnedRows[i] = rows[nextRow++];
//		}
//		return returnedRows;
//	}

	private void initialize() throws IOException, RJException {
		List list = new ArrayList();
		DataRow row;
		Log.log(SortingDataSource.class, "Initialization of data source", 1);
//		int i = 0;
		while ((row = parentSource.getNextRow()) != null) {
			list.add(row);
//			int index = Collections.binarySearch(list, row, comparator);
//			if (index < 0) {
//				index = -index - 1;
//			}
//			list.add(index, row);
//			i++;
//			if (i % 10000 == 0) Log.log(SortingDataSource.class, "Sorted: " + i);
		}
		Log.log(SortingDataSource.class, "Sort is about to start", 1);
		Collections.sort(list, comparator);
		nextRow = 0;
		Log.log(SortingDataSource.class, "Initialization completed", 1);
		rows = (DataRow[]) list.toArray(new DataRow[] {});
	}
	
	protected void doClose() throws IOException, RJException {
		Log.log(SortingDataSource.class, "Closing data source");
		this.parentSource.close();
	}

	protected void doReset() throws IOException {
		Log.log(SortingDataSource.class, "Reset called on data source");
		if (nextRow != -1) {
			nextRow = 0;
		}
	}

	public boolean canSort() {
		return true;
	}
	
	public void setOrderBy(DataColumnDefinition[] orderBy, CompareFunctionInterface[] functions) {
		Log.log(SortingDataSource.class, "Setting order by: " + PrintUtils.printArray(orderBy), 1);
		this.comparator = new RowComparator(functions, orderBy);
	}

	public Node saveToXML() {
		return null;
	}
	
	public boolean equals(Object arg0) {
		throw new RuntimeException("Should not be called!");
	}

	public AbstractDataSource copy() throws IOException, RJException {
		throw new RuntimeException("Not implemented");
	}

	public long size() throws IOException, RJException {
		return parentSource.size();
	}
}
