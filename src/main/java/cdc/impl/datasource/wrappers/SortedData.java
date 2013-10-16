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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.impl.datastream.DataRowInputStream;
import cdc.impl.datastream.DataRowOutputStream;
import cdc.utils.CPUInfo;
import cdc.utils.CompareFunctionInterface;
import cdc.utils.Log;
import cdc.utils.RJException;
import cdc.utils.RowUtils;
import cdc.utils.Utils;

public class SortedData {
	
	private class ActiveInput {
		public DataRowInputStream is;
		public DataRow row;
		public File file;
	}
	
	private class RowComparator implements Comparator {
		private DataColumnDefinition[] comparedFields;
		private CompareFunctionInterface[] functions;
		
		public RowComparator(CompareFunctionInterface[] functions, DataColumnDefinition[] orderBy) {
			this.comparedFields = orderBy;
			this.functions = functions;
		}
		
		public int compare(Object o1, Object o2) {
			return RowUtils.compareRows((DataRow)o1, (DataRow)o2, comparedFields, comparedFields, functions);
		}
		
	}
	
	private int BUFFER_SIZE = ExternallySortingDataSource.BUFFER_SIZE / CPUInfo.testNumberOfCPUs();
	
	private String sourceName;
	private DataColumnDefinition[] rowModel;
	private DataColumnDefinition[] orderBy;
	private CompareFunctionInterface[] functions;
	private Comparator comparator;
	
	private DataRow[] buffer;
	private volatile int position = 0;
	private volatile int returnIndex = 0;

	private volatile boolean completed = false;
	
	private List files = new ArrayList();
	private List inputs = new ArrayList();

	private boolean firstTime = true;

	private volatile boolean fileUsed = false;
	private volatile boolean interrupted = false;
	
	//private CacheInterface cache;
	
	public SortedData(int bufferSize, String sourceName, DataColumnDefinition[] orderBy, CompareFunctionInterface[] functions) {
		this.sourceName = sourceName;
		this.orderBy = orderBy;
		this.functions = functions;
		this.BUFFER_SIZE = bufferSize;
		this.buffer = new DataRow[BUFFER_SIZE];
		this.comparator = new RowComparator(functions, orderBy);
	}
	
//	public SortedData(CacheInterface propsCache, int bufferSize, String sourceName, DataColumnDefinition[] orderBy, CompareFunctionInterface[] functions) {
//		this.sourceName = sourceName;
//		this.orderBy = orderBy;
//		this.functions = functions;
//		this.cache = propsCache;
//		this.BUFFER_SIZE = bufferSize;
//		this.buffer = new DataRow[BUFFER_SIZE];
//		this.comparator = new RowComparator(functions, orderBy);
//	}
	
	public SortedData(String sourceName, DataColumnDefinition[] orderBy, CompareFunctionInterface[] functions) {
		this.sourceName = sourceName;
		this.orderBy = orderBy;
		this.functions = functions;
		this.buffer = new DataRow[BUFFER_SIZE];
		this.comparator = new RowComparator(functions, orderBy);
	}

	public void addRow(DataRow row) throws FileNotFoundException, IOException {
		if (rowModel == null) {
			rowModel = row.getRowModel();
		}
		flushIfNeeded();
		buffer[position++] = row;
	}
	
	public DataRow getNextSortedRow() throws IOException, RJException {
		try {
			if (!completed) {
				complete();
			}
			if (fileUsed) {
				return mergeNext();
			} else {
				if (returnIndex == position) {
					return null;
				}
				return buffer[returnIndex++];
			}
		} catch (EOFException e) {
			return null;
		}
	}

	public void complete() throws FileNotFoundException, IOException, RJException {
		//merge files
		if (files.size() != 0) {
			flush();
			Log.log(getClass(), "Data divided among temporary files. Will merge during output reading.", 2);
			buffer = null;
		} else {
			Log.log(getClass(), "Data fits into memory - no need to sort externally", 2);
			Arrays.sort(buffer, 0, position, comparator);
		}
		completed  = true;
	}

	public boolean shouldFlush() {
		return position == BUFFER_SIZE;
	}

	public void flush() throws FileNotFoundException, IOException {
		writeBuffer();
		position = 0;
	}
	
	public void flushIfNeeded() throws FileNotFoundException, IOException {
		if (shouldFlush()) {
			flush();
		}
	}
	
	private DataRow mergeNext() throws IOException, RJException {
		try {
			if (firstTime ) {
				firstTime = false;
				inputs = new ArrayList();
				for (int i = 0; i < files.size(); i++) {
					DataRowInputStream is = new DataRowInputStream(createInputStream((File) files.get(i)));
					ActiveInput input = new ActiveInput();
					input.is = is;
					input.row = is.readDataRow();
					input.file = (File) files.get(i);
					if (input.row != null) {
						inputs.add(input);
					} else {
						is.close();
						((File)files.get(i)).delete();
						Log.log(getClass(), "File with no data " + ((File)files.get(i)).getName(), 2);
					}
				}
			}
			while (!inputs.isEmpty()) {
				DataRow row = ((ActiveInput)inputs.get(0)).row;
				if (rowModel == null) {
					rowModel = row.getRowModel();
				}
				int best = 0;
				for (int i = 1; i < inputs.size(); i++) {
					DataRow secondRow = ((ActiveInput)inputs.get(i)).row;
					if (RowUtils.compareRows(row, secondRow, orderBy, orderBy, functions) >= 0) {
						best = i;
						row = secondRow;
					}
				}
				synchronized (this) {
					if (interrupted) {
						return null;
					}
					if (consumeRow(((ActiveInput)inputs.get(best)))) {
						inputs.remove(best);
					}
				}
				return row;
			}
		} catch (ClassNotFoundException e) {
			throw new RJException("Error!", e);
		}
		Log.log(getClass(), "Files have been merged. No more data.", 2);
		return null;
	}

	private boolean consumeRow(ActiveInput activInput) throws IOException, ClassNotFoundException, RJException {
		try {
			activInput.row = activInput.is.readDataRow();
			if (activInput.row == null) {
				activInput.is.close();
				activInput.file.delete();
				return true;
			}
			return false;
		} catch (EOFException e) {
			activInput.is.close();
			activInput.file.delete();
			Log.log(getClass(), "File reading completed: " + activInput.file.getName(), 2);
		}
		return true;
	}

	private void writeBuffer() throws FileNotFoundException, IOException {
		//sort buffer
		Arrays.sort(buffer, 0, position, comparator);
		
		//write file
		File f = Utils.createBufferFile(this);
		files.add(f);
		f.deleteOnExit();
		DataRowOutputStream oos = new DataRowOutputStream(sourceName, rowModel, createOutputStream(f));
		oos.addHeaderMetadata("order-by", orderBy);
		//oos.addHeaderMetadata("functions", functions);
		for (int i = 0; i < position; i++) {
			if (interrupted) {
				oos.close();
				return;
			}
			oos.writeDataRow(buffer[i]);
			//buffer[i].discard();
			buffer[i] = null;
		}
		oos.close();
		Log.log(getClass(), "Data (" + position + " rows) from buffer saved into " + f.getName(), 2);
		
		fileUsed = true;
		System.gc();
	}
	
	public static InputStream createInputStream(File file) throws FileNotFoundException, IOException {
		return new GZIPInputStream(new BufferedInputStream(new FileInputStream(file)));
	}

	public static OutputStream createOutputStream(File file) throws FileNotFoundException, IOException {
		return new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
	}

	public DataRow[] getBufferedData() {
		return buffer;
	}

	public DataColumnDefinition[] getOrderBy() {
		return orderBy;
	}

	public boolean usedExternalFile() {
		return fileUsed;
	}

	public void interruptAction() {
		interrupted  = true;
	}
	
	public void cleanup() {
		synchronized (this) {
			this.comparator = null;
//			try {
//				if (cache != null) {
//					cache.trash();
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
			//cache = null;
			for (Iterator iterator = inputs.iterator(); iterator.hasNext();) {
				ActiveInput input = (ActiveInput) iterator.next();
				try {
					input.is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			for (Iterator iterator = files.iterator(); iterator.hasNext();) {
				File toDelete = (File) iterator.next();
				toDelete.delete();
			}
		}
	}
	
//	public static void main(String[] args) throws FileNotFoundException, IOException, RJException {
//		DataColumnDefinition[] cols = new DataColumnDefinition[] {new DataColumnDefinition("a", DataColumnDefinition.TYPE_STRING, "")};
//		DataCell[] r1 = new DataCell[] {new DataCell(DataColumnDefinition.TYPE_STRING, "1")};
//		DataCell[] r2 = new DataCell[] {new DataCell(DataColumnDefinition.TYPE_STRING, "2")};
//		Integer prop = new Integer();
//		prop.i = 2000;
//		DataRow row1 = new DataRow(cols, r1);
//		row1.setProperty("p", prop);
//		DataRow row2 = new DataRow(cols, r2);
//		row2.setProperty("p", prop);
//		
//		SortedData data = new SortedData(1, "dd", cols, new CompareFunctionInterface[] {new StringComparator()});
//		data.addRow(row1);
//		data.addRow(row2);
//		
//		row1 = data.getNextSortedRow();
//		row2 = data.getNextSortedRow();
//		
//		Integer pp1 = (Integer) row1.getObjectProperty("p");
//		Integer pp2 = (Integer) row2.getObjectProperty("p");
//		pp1.i = 10;
//		
//		System.out.println(pp2.i);
//		
//	}
//	private static class Integer implements Serializable {
//		int i;
//	}

}
