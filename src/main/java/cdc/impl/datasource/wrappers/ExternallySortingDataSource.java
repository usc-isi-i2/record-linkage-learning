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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JOptionPane;

import cdc.components.AbstractDataSource;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.converters.ModelGenerator;
import cdc.gui.MainFrame;
import cdc.impl.datastream.DataFileHeader;
import cdc.impl.datastream.DataRowInputStream;
import cdc.utils.CPUInfo;
import cdc.utils.CompareFunctionInterface;
import cdc.utils.Log;
import cdc.utils.PrintUtils;
import cdc.utils.Props;
import cdc.utils.RJException;
import cdc.utils.RowUtils;
import cdc.utils.Utils;

public class ExternallySortingDataSource extends AbstractDataSource {
	
	public static final long VALID_PERIOD = Props.getLong("externally-sorting-wrapper-valid-interval");
	public static final int BUFFER_SIZE = getBufferSize();
	
	private static int getBufferSize() {
		return Props.getInteger("externally-sorting-wrapper-buffer-size");
	}
	
	private static final double PART_READING = 0.75;
	
	private int cpunumber = CPUInfo.testNumberOfCPUs();
	
	private AbstractDataSource parentSource;
	private boolean closed = false;
	
	private CompareFunctionInterface[] functions;
	private DataColumnDefinition[] orderBy;
	
	private boolean initialized = false;
	
	private BufferedData sortedData;
	private SortThread[] workers;
	
	private int readItems = 0;
	
	private volatile AtomicInteger configurationProgress = new AtomicInteger(0);
	
	public ExternallySortingDataSource(String name, AbstractDataSource dataSource, DataColumnDefinition[] columns, CompareFunctionInterface[] functions, Map params) throws IOException, RJException {
		super(name, params);
		this.parentSource = dataSource;
		this.orderBy = columns;
		this.functions = functions;
		Log.log(getClass(), "Creating source with order by : " + PrintUtils.printArray(orderBy),1);
	}
	
	private synchronized void initialize() throws IOException, RJException {
		if (initialized) {
			return;
		}
		if (isStopRequested()) {
			return;
		}
		if (!cashedData()) {
			long t1 = System.currentTimeMillis();
			sortData();
			if (isStopRequested()) {
				return;
			}
			long t2 = System.currentTimeMillis();
			Log.log(getClass(), "Data sorted in time " + (t2-t1) + "ms.",1);
		}
		initialized = true;
		closed = false;
	}

	public boolean canSort() {
		return true;
	}
	
	public DataColumnDefinition[] getAvailableColumns() {
		return parentSource.getAvailableColumns();
	}
	
	public void setOrderBy(DataColumnDefinition[] orderBy, CompareFunctionInterface[] functions) throws IOException, RJException {
		this.orderBy = orderBy;
		this.functions = functions;
		Log.log(getClass(), "Setting order by to: " + PrintUtils.printArray(orderBy),1);
		if (!cashedData()) {
			sortData();
		}
	}


	private boolean cashedData() throws IOException, RJException {
		File localDir = new File(".");
		String[] files = localDir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				int dot = name.lastIndexOf('.');
				if (dot < 0) return false;
				String fName = name.substring(0, dot);
				String fExt = name.substring(dot + 1, name.length());
				try {
					if ("bin".equals(fExt)) {
						String[] strs = fName.split("_");
						Long.parseLong(strs[0]);
						Long.parseLong(strs[1]);
						return true;
					}
				} catch (NumberFormatException e) {}
				return false;
			}
		});
		
		for (int i = 0; i < files.length; i++) {
			File f = new File(files[i]);
			try {
				DataRowInputStream is = new DataRowInputStream(SortedData.createInputStream(f));
				DataFileHeader header = is.getHeader();
				if (isValid(header, parentSource, orderBy)) {
					Log.log(getClass(), "Using buffered data " + f.getName(), 1);
					sortedData = new BufferedData(f);
					return true;
				} else if (isExpired(header)) {
					Log.log(getClass(), "Data file cannot be used: " + f.getName(), 1);
					f.delete();
				}
			} catch (IOException e) {
				Log.log(getClass(), "Could not open file, or file is not data file" + f.getName(), 1);
			}
		}
		Log.log(getClass(), "Buffered data not found", 1);
		return false;
	}

	private synchronized void sortData() throws IOException, RJException {
		
		try {
			System.gc();
			
			Log.log(getClass(), "Sort data starting: " + PrintUtils.printArray(orderBy),2);
			
			//scatter data into multiple files
			workers = new SortThread[cpunumber];
			for (int i = 0; i < workers.length; i++) {
				workers[i] = new SortThread(parentSource.getSourceName(), orderBy, functions);
				workers[i].setName(parentSource.getSourceName() + "-sorter-" + i);
			}
			
			DataRow row = null;
			int readFromSource = 0;
			int roundRobin = 0;
			//distribute data
			
			if (isStopRequested()) {
				return;
			}
			
			synchronized (parentSource) {
				parentSource.reset();
				int size = 0;
				while ((row = parentSource.getNextRow()) != null) {
					size++;
					workers[roundRobin].addDataRow(row);
					roundRobin = (roundRobin + 1) % workers.length;
					readFromSource++;
					configurationProgress.set((int) (100 * PART_READING * (parentSource.position() / (double)parentSource.size())));
					if (isStopRequested()) {
						for (int i = 0; i < workers.length; i++) {
							workers[i].stopProcessing();
							workers[i].getSortedData().cleanup();
						}
						return;
					}
				}
				Log.log(getClass(), "Read all data from source " + parentSource.getSourceName() + ". # of records: " + size ,1);
				
				//signal end of data
				for (int i = 0; i < workers.length; i++) {
					workers[i].addDataRow(null);
				}
				
				//merge data
				sortedData = new BufferedData(workers, orderBy, functions, configurationProgress, parentSource.size());
			}
			configurationProgress.set(100);
			readItems = 0;
			Log.log(getClass(), "Data sorted (size of sorted data: " + sortedData.getSize() + ")", 1);
			
			System.gc();
		} finally {
			if (isStopRequested()) {
				if (sortedData != null) {
					sortedData.close();
					sortedData = null;
				}
			}
		}
	}

	protected void doClose() throws IOException, RJException {
		Log.log(getClass(), "Closing data source", 1);
		if (closed) {
			return;
		}
		if (workers != null) {
			for (int i = 0; i < workers.length; i++) {
				workers[i].cleanup();
			}
		}
		workers = null;
		closed = true;
		if (sortedData != null) {
			sortedData.close();
		}
		sortedData = null;
		initialized = false;
		synchronized (parentSource) {
			parentSource.close();
		}
	}
	

	public DataRow nextRow() throws IOException, RJException {
		
		//if (closed) {
		//	throw new RJException("Cannot get next row on closed data source");
		//}
		
		initialize();
		
		if (isStopRequested()) {
			return null;
		}
		
		Log.log(getClass(), "getNextRow()", 3);
		DataRow row = sortedData.getDataRow();
		RowUtils.resetRow(row);
		if (row == null) {
			Log.log(getClass(), "Finished reading sorted data of source " + parentSource.getSourceName() + ". Read items = " + readItems, 1);
			return null;
		}
		readItems++;
		return row;
		
	}

	protected void doReset() throws IOException, RJException {
		initialized = false;
		configurationProgress.set(0);
		synchronized (parentSource) {
			parentSource.reset();
		}
	}
	
	protected void finalize() throws Throwable {
		close();
	}
	
	public boolean equals(Object arg0) {
		throw new RuntimeException("Should not be called!");
	}

	public AbstractDataSource copy() throws IOException, RJException {
		ExternallySortingDataSource that = new ExternallySortingDataSource(getSourceName(), parentSource.copy(), orderBy, functions, getProperties());
		initialize();
		that.setStratumCondition(getFilterCondition());
		that.initialized = initialized;
		if (initialized) {
			that.sortedData = sortedData.copy();
		}
		return that;
	}
	

	public ModelGenerator getDataModel() {
		return parentSource.getDataModel();
	}

	public long size() throws IOException, RJException {
		initialize();
		if (sortedData == null) {
			synchronized (parentSource) {
				return parentSource.size();
			}
		}
		long size = sortedData.getSize();
		if (size == -1) {
			synchronized (parentSource) {
				return parentSource.size();
			}
		} else {
			return size;
		}
	}
	
	public boolean isConfigurationProgressSupported() {
		return true;
	}
	
	public int getConfigurationProgress() {
		return configurationProgress.get();
	}
	
	public boolean isValid(DataFileHeader header, AbstractDataSource parent, DataColumnDefinition[] orderBy) {
		
		if (header.getMetadata("complete") == null || !"true".equals(header.getMetadata("complete"))) {
			return false;
		}
		
		DataColumnDefinition[] cols = parent.getDataModel().getOutputFormat();

		String recordedSourceName = header.getSourceName();
		DataColumnDefinition[] recordedColumns = header.getMetadataAsColumnsArray("columns");
		DataColumnDefinition[] recordedOrderBy = header.getMetadataAsColumnsArray("order-by");
		
		for (int i = 0; i < cols.length; i++) {
			DataColumnDefinition[] generics = new DataColumnDefinition[] {cols[i]};
			for (int k = 0; k < generics.length; k++) {
				for (int j = 0; j < recordedColumns.length; j++) {					
					if (generics[k].equals(recordedColumns[j])) {
						break;
					}
					if (j == recordedColumns.length - 1) {
						return false;
					}
				}
			}
			Log.log(getClass(), "Saved column: " + recordedColumns[i] + ", expected: " + cols[i].getColumnName(), 2);
		}
		
		
		if (orderBy.length != recordedOrderBy.length) {
			return false;
		} else {
			for (int i = 0; i < orderBy.length; i++) {
				Log.log(getClass(), "Saved order by column: " + recordedOrderBy[i] + ", expected: " + orderBy[i].getColumnName(), 2);
				if (!recordedOrderBy[i].equals(orderBy[i])) {
					return false;
				}
			}
		}
		
		if (Utils.isTextMode()) {
			return  parent.getSourceName().equals(recordedSourceName) && header.getTimestamp() + VALID_PERIOD > System.currentTimeMillis();
		} else if (parent.getSourceName().equals(recordedSourceName)) {
			if (JOptionPane.showConfirmDialog(MainFrame.main, "Buffered file for source " + recordedSourceName + " found.\n" +
					"Date of creation: " + new Date(header.getTimestamp()) + "\nWould you like to use it?", "Buffered data found",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				return true;
			}
			
		}
		return false;
	}
	
	private boolean isExpired(DataFileHeader header) {
		return header.getTimestamp() + VALID_PERIOD < System.currentTimeMillis();
	}
	
	public void setStart(long start) throws IOException, RJException {
		initialize();
		if (isStopRequested()) {
			return;
		}
		sortedData.setStart(start);
	}
	
	public void setLimit(long limit) throws IOException, RJException {
		initialize();
		if (isStopRequested()) {
			return;
		}
		sortedData.setLimit(limit);
	}
	
	public void skip(long count) throws IOException, RJException {
		initialize();
		if (isStopRequested()) {
			return;
		}
		sortedData.skip(count);
	}

	public AbstractDataSource getRawDataSource() {
		return parentSource;
	}
	
}
