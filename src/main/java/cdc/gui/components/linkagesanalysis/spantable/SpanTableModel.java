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


package cdc.gui.components.linkagesanalysis.spantable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.table.DefaultTableModel;

public class SpanTableModel extends DefaultTableModel  {

	private List listSpan = new ArrayList();
	
	public SpanTableModel(int rowCnt, int colCnt) {
		super(rowCnt, colCnt);
	}
	
	public SpanTableModel(Object[] colNames, int rows) {
		super(colNames, rows);
	}
	
	public SpanTableModel(Object[] colNames) {
		super(colNames, 0);
	}
	
	public void addSpannedRows(Object[][] rows, SpanInfo[] span) {
		
		synchronized (this) {
			int currRowCnt = getRowCount();
			
			//Add rows
			for (int i = 0; i < rows.length; i++) {
				addRow(rows[i]);
			}
			
			//Calculate the info map data
			for (int i = 0; i < span.length; i++) {
				if (span[i].getRow() + span[i].getSpanHeight() > rows.length) {
					throw new IllegalArgumentException("span[i].getRow() + span[i].getSpanHeight() > rows.length");
				} else if (span[i].getCol() + span[i].getSpanWidth() > rows[span[i].getRow()].length) {
					throw new IllegalArgumentException("span[i].getCol() + span[i].getSpanWidth() > rows[span[i].getRow()].length");
				}
				span[i].setRowId(currRowCnt + span[i].getRow());
				listSpan.add(span[i]);
			}
		}
		
	}

	public int[] visibleRowAndColumn(int row, int col) {
		synchronized (this) {
			for (Iterator iterator = listSpan.iterator(); iterator.hasNext();) {
				SpanInfo span = (SpanInfo) iterator.next();
				int realRow = span.getRowId();
				if (realRow <= row && realRow + span.getSpanHeight() - 1 >= row && span.getCol() <= col && span.getCol() + span.getSpanWidth() - 1 >= col) {
					return new int[] {realRow, span.getCol()};
				}
			}
			return new int[] {row, col};
		}
	}

	public int[] spannedRowsAndColumns(int row, int col) {
		synchronized (this) {
			for (Iterator iterator = listSpan.iterator(); iterator.hasNext();) {
				SpanInfo span = (SpanInfo) iterator.next();
				int realRow = span.getRowId();
				if (realRow <= row && realRow + span.getSpanHeight() - 1 >= row && span.getCol() <= col && span.getCol() + span.getSpanWidth() - 1 >= col) {
					return new int[] {span.getSpanHeight(), span.getSpanWidth()};
				}
			}
			return new int[] {1, 1};
		}
	}
	
	public int getRowNumber(int row) {
		int r = 0;
		int num = 0;
		while (row >= r) {
			r += spannedRowsAndColumns(row, 0)[0];
			num++;
		}
		return num;
	}
	
	public void removeRow(int row) {
		synchronized (this) {	
			//Find the extent of current row
			int[] range = new int[] {row, row};
			boolean change = true;
			while (change) {
				change = false;
				
				int[] rows = visibleRowAndColumn(range[0], 0);
				if (rows[0] < range[0]) {
					range[0] = rows[0];
					change = true;
				}
				
				rows = visibleRowAndColumn(range[1], 0);
				int[] size = spannedRowsAndColumns(range[1], 0);
				if (rows[0] + size[0] - 1 > range[1]) {
					range[1] = rows[0] + size[0] - 1;
					change = true;
				}
			}
			
			//I know the extent. Remove the rows.
			for (int i = 0; i <= range[1] - range[0]; i++) {
				super.removeRow(range[0]);
			}
		
			//Remove the span information
			List toRemove = new ArrayList();
			for (Iterator iterator = listSpan.iterator(); iterator.hasNext();) {
				SpanInfo info = (SpanInfo) iterator.next();
				if (info.getRowId() >= range[0] && info.getRowId() + info.getSpanHeight() - 1 <= range[1]) {
					toRemove.add(info);
				}
				if (info.getRowId() > range[1]) {
					info.setRowId(info.getRowId() - (range[1] - range[0] + 1));
				}
			}
			listSpan.removeAll(toRemove);
		}
	}

}
