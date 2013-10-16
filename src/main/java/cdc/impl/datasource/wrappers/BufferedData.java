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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import cdc.components.AbstractDataSource;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.impl.datastream.DataRowInputStream;
import cdc.impl.datastream.DataRowOutputStream;
import cdc.utils.CompareFunctionInterface;
import cdc.utils.RJException;
import cdc.utils.RowUtils;
import cdc.utils.Utils;

public class BufferedData {

	private class ActiveInput {
		public SortThread thread;
		public DataRow row;
	}
	
	private static final int BUFFER_SIZE = ExternallySortingDataSource.BUFFER_SIZE;
	
	private DataColumnDefinition[] orderBy;
	
	private String sourceName;
	private File file;
	private DataRowInputStream is;
	private DataRowOutputStream os;
	private Map headerProps;
	
	private DataRow[] buffer = new DataRow[BUFFER_SIZE];
	private int bufferPosition = 0;
	private long start = 0;
	private long limit = Long.MAX_VALUE;
	private long taken = 0;
	private int readNext = 0;
	
	private long size = 0;
		
	public BufferedData(long size) {
		String fileName = System.currentTimeMillis() + "_" + hashCode() + ".dat";
		file = new File(fileName);
		file.deleteOnExit();
		this.size = size;
	}
	
	public BufferedData(File f) throws FileNotFoundException, IOException, RJException {
		file = f;
		is = new DataRowInputStream(SortedData.createInputStream(file));
		sourceName = is.getHeader().dataSourceName;
	}
	
	public BufferedData(SortThread[] workers, DataColumnDefinition[] orderBy, CompareFunctionInterface[] functions, AtomicInteger configurationProgress, long size) throws IOException, RJException {
		
		int base = configurationProgress.get();
		this.size = size;
		
		this.orderBy = orderBy;
		file = Utils.createBufferFile(this);
		List inputs = new ArrayList();
		for (int i = 0; i < workers.length; i++) {
			size += workers[i].getRowsNumber();
			ActiveInput input = new ActiveInput();
			input.thread = workers[i];
			input.row = workers[i].getNextSortedRow();
			if (input.row != null) {
				inputs.add(input);
			} else {
				//input.thread.getSortedData().close();
			}
		}
	
		long completed = 0;
		while (!inputs.isEmpty()) {
			ActiveInput bestInput = (ActiveInput)inputs.get(0);
			int best = 0;
			for (int i = 1; i < inputs.size(); i++) {
				ActiveInput secondInput = (ActiveInput)inputs.get(i);
				if (RowUtils.compareRows(bestInput.row, secondInput.row, orderBy, orderBy, functions) >= 0) {
					best = i;
					bestInput = secondInput;
				}
			}
			sourceName = bestInput.row.getSourceName();
			addRow(bestInput.row);
			bestInput.row = bestInput.thread.getNextSortedRow();
			if (bestInput.row == null) {
				//bestInput.thread.getSortedData().close();
				inputs.remove(best);
			}
			completed++;
			configurationProgress.set(base + (int)(completed / (double)size * ( 100 - base)));
			if (AbstractDataSource.isStopRequested()) {
				if (os != null) {
					os.close();
					file.delete();
				}
				for (Iterator iterator = inputs.iterator(); iterator.hasNext();) {
					ActiveInput input = (ActiveInput) iterator.next();
					input.thread.getSortedData().cleanup();
				}
				return;
			}
		}
		addingCompleted();
	}

	public DataRow getDataRow() throws IOException, RJException {
		if (taken >= limit) {
			return null;
		}
		taken++;
		if (is == null) {
			if (readNext >= bufferPosition) {
				return null;
			}
			return buffer[readNext++];
		} else {
			return is.readDataRow();
		}
	}
	
	public void setStart(long start) throws IOException, RJException {
		this.start = start;
		skipToPosition();
	}
	
	private void skipToPosition() throws IOException, RJException {
		if (start >= limit) {
			return;
		}
		if (is == null) {
			//just set buffer position
			if (start != 0) {
				readNext = (int)start - 1;
			}
		} else {
			//This is very simple skipping.
			//Should use something more sophisticated if possible
			while (start != taken + 1) {
				getDataRow();
			}
			taken = 0;
		}
	}

	public void setLimit(long limit) {
		this.limit = limit;
		this.taken = 0;
	}
	
	public void addRow(DataRow row) throws IOException {
		buffer[bufferPosition++] = row;
		if (bufferPosition == BUFFER_SIZE) {
			//need to flush to file
			if (os == null) {
				os = new DataRowOutputStream(sourceName, row.getRowModel(), SortedData.createOutputStream(file));
				os.addHeaderMetadata("complete", "true");
				os.addHeaderMetadata("order-by", orderBy);
				os.addHeaderMetadata("size", new Long(size));
				if (headerProps != null) {
					for (Iterator iterator = headerProps.keySet().iterator(); iterator.hasNext();) {
						String propName = (String) iterator.next();
						os.addHeaderMetadata(propName, headerProps.get(propName));
					}
				}
			}
			writeBuffer();
		}
	}
	
//	private long computeSize(SortThread[] workers) {
//		long size = 0;
//		for (int i = 0; i < workers.length; i++) {
//			size += workers[i].getRowsNumber();
//		}
//		return  size;
//	}

	private void writeBuffer() throws IOException {
		for (int i = 0; i < bufferPosition; i++) {
			os.writeDataRow(buffer[i]);
		}
		bufferPosition = 0;
	}

	public void addingCompleted() throws IOException, RJException {
		if (os != null) {
			writeBuffer();
			os.close();
			os = null;
			is = new DataRowInputStream(SortedData.createInputStream(file));
		}
	}

	public void close() throws IOException {
		if (is != null) {
			is.close();
		}
		if (os != null) {
			os.close();
		}
		is = null;
		file = null;
		buffer = null;
	}

	public BufferedData copy() throws FileNotFoundException, IOException, RJException {
		BufferedData that = new BufferedData(size);
		if (is != null) {
			//the case of file being used...
			that.file = file;
			that.is = new DataRowInputStream(SortedData.createInputStream(file));
			that.sourceName = that.is.getHeader().dataSourceName;
		} else {
			//the case of data in memory...
			that.file = null;
			that.is = null;
			that.buffer = buffer;
			that.bufferPosition = bufferPosition;
		}
		return that;
	}

	public void skip(long cnt) throws IOException, RJException {
		if (is == null) {
			//just set buffer position
			readNext = readNext + (int)cnt;
		} else {
			//This is very simple skipping.
			//Should use something more sophisticated if possible
			for (long i = 0; i < cnt; i++) {
				getDataRow();
			}
		}
	}

	public long getSize() {
		if (is == null) {
			return bufferPosition;
		} else {
			Long size = (Long) is.getHeader().getMetadata("size");
			if (size == null) {
				return -1;
			} else {
				return size.longValue();
			}
		}
	}
	
	public void reset() throws IOException, RJException {
		if (is != null) {
			is.close();
			is = new DataRowInputStream(SortedData.createInputStream(file));
		} else {
			readNext = 0;
		}
	}
}
