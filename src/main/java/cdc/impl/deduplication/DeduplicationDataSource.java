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


package cdc.impl.deduplication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.components.AtomicCondition;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.converters.ModelGenerator;
import cdc.impl.datasource.wrappers.BufferedData;
import cdc.impl.join.blocking.BucketManager;
import cdc.impl.resultsavers.CSVFileSaver;
import cdc.utils.CPUInfo;
import cdc.utils.Log;
import cdc.utils.RJException;
import cdc.utils.RowUtils;

public class DeduplicationDataSource extends AbstractDataSource {

	private AbstractDataSource parent;
	private volatile boolean cancel = false;
	private boolean initialized = false;
	private BufferedData deduplicatedData;
	private BucketManager buckets;
	private DeduplicationConfig config;
	private int sizeDedup;
	private AtomicInteger nDuplicates = new AtomicInteger(0);
	
	private CSVFileSaver minusSaver = null;
	
	private CountDownLatch latch;
	private volatile RJException exception;
	private DeduplicationThread[] threads;
	private AtomicInteger progressDataSource = new AtomicInteger(0);
	private AtomicInteger progressDedupe = new AtomicInteger(0);
	
	private AtomicInteger duplicateId = new AtomicInteger(0);
	private int inputRecordsCnt = 0;
	
	public DeduplicationDataSource(AbstractDataSource parentSource, DeduplicationConfig config) {
		super(parentSource.getSourceName(), parentSource.getProperties());
		this.config = config;
		parent = parentSource;
	}
	
	public boolean canSort() {
		return false;
	}

	protected void doClose() throws IOException, RJException {
		if (deduplicatedData != null) {
			deduplicatedData.close();
		}
		synchronized (this) {
			if (threads != null) {
				for (int i = 0; i < threads.length; i++) {
					threads[i].cancel();
					try {
						threads[i].join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		if (buckets != null) {
			buckets.cleanup();
		}
		initialized = false;
		
	}

	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}
	
	public AbstractDataSource copy() throws IOException, RJException {
		initialize();
		DeduplicationDataSource that = new DeduplicationDataSource(parent, config);
		that.initialized = true;
		that.inputRecordsCnt = inputRecordsCnt;
		that.nDuplicates = nDuplicates;
		that.deduplicatedData = deduplicatedData.copy();
		that.config = config;
		return that;
	}

	protected void doReset() throws IOException, RJException {
		//System.out.println("Resetting!!!!!");
		cancel = false;
		if (!initialized) {
			parent.reset();
			progressDataSource.set(0);
			progressDedupe.set(0);
		} else {
			synchronized (this) {
				if (threads != null) {
					for (int i = 0; i < threads.length; i++) {
						threads[i].cancel();
						try {
							threads[i].join();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
			progressDedupe.set(0);
			duplicateId.set(0);
			deduplicatedData.reset();
		}
	}

	public boolean equals(Object arg0) {
		if (!(arg0 instanceof DeduplicationDataSource)) {
			return false;
		}
		DeduplicationDataSource that = (DeduplicationDataSource)arg0;
		return this.parent.equals(that.parent);
	}

	protected DataRow nextRow() throws IOException, RJException {
		initialize();
		return deduplicatedData.getDataRow();
	}

	public long size() throws IOException, RJException {
		initialize();
		return deduplicatedData.getSize();
	}

	private synchronized void initialize() throws IOException, RJException {
		if (initialized || cancel) {
			return;
		}
		sizeDedup = 0;
		inputRecordsCnt = 0;
		nDuplicates.set(0);
		
		if (config.getMinusFile() != null) {
			Map props = new HashMap();
			props.put(CSVFileSaver.OUTPUT_FILE_PROPERTY, config.getMinusFile());
			props.put(CSVFileSaver.SAVE_CONFIDENCE, "false");
			props.put(CSVFileSaver.SAVE_SOURCE_NAME, "false");
			minusSaver = new CSVFileSaver(props);
		}
		
		Log.log(getClass(), "Deduplication begins for data source " + getSourceName(), 1);
		buckets = new BucketManager(config.getHashingFunction());
		deduplicatedData = new BufferedData(parent.size());
		DataRow row;
		while ((row = parent.getNextRow()) != null && !cancel) {
			buckets.addToBucketLeftSource(row);
			progressDataSource.set((int) (parent.position() * 100 / parent.size()));
			inputRecordsCnt++;
		}
		Log.log(getClass(), "Deduplication of " + getSourceName() + ": added " + inputRecordsCnt + " rows to bucket manager.", 2);
		buckets.addingCompleted();
		Log.log(getClass(), "Deduplication: buckets generated", 2);
		
		Log.log(getClass(), "Using " + CPUInfo.testNumberOfCPUs() + " core(s) for deduplication.");
		latch = new CountDownLatch(CPUInfo.testNumberOfCPUs());
		
		if (cancel) {
			return;
		}
		
		synchronized (this) {
			threads = new DeduplicationThread[CPUInfo.testNumberOfCPUs()];
			for (int i = 0; i < threads.length; i++) {
				threads[i] = new DeduplicationThread(i);
				threads[i].start();
			}
		}
		
		try {
			while (latch.getCount() != 0) {
				latch.await(100, TimeUnit.MILLISECONDS);
				int progress = 0;
				for (int i = 0; i < threads.length; i++) {
					progress += threads[i].completed.get();
				}
				if (inputRecordsCnt != 0) {
					progressDedupe.set(progress * 100 / inputRecordsCnt);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		synchronized (deduplicatedData) {
			deduplicatedData.addingCompleted();
		}
		for (int i = 0; i < threads.length; i++) {
			sizeDedup += threads[i].getNonDuplicatesCnt();
			//nDuplicates += threads[i].getDuplicatesCnt();
		}
		if (minusSaver != null) {
			minusSaver.flush();
			minusSaver.close();
		}
		
		if (exception != null) {
			throw exception;
		}
		
		Log.log(getClass(), "Deduplication finished for data source " + getSourceName() + ". Identified " + nDuplicates + " duplicates." , 1);
		Log.log(getClass(), "Size of data in deduplicated " + getSourceName() + ": " + sizeDedup + " (" + deduplicatedData.getSize() + ")" , 1);
		if (!cancel) {
			initialized = true;
		}
	}
	
	public ModelGenerator getDataModel() {
		return parent.getDataModel();
	}
	
	public DataColumnDefinition[] getAvailableColumns() {
		return parent.getAvailableColumns();
	}
	
	public int getConfigurationProgress() {
		return parent.getConfigurationProgress();
	}
	
	public AtomicCondition[] getFilterCondition() {
		return parent.getFilterCondition();
	}
	
	public AbstractDataSource getRawDataSource() {
		return parent;
	}
	
	private class DeduplicationThread extends Thread {
		
		private volatile boolean cancel = false;
		//private int duplicatesCnt = 0;
		private int nonDuplicatesCnt = 0;
		private AtomicInteger completed = new AtomicInteger(0);
		
		private DataColumnDefinition[] model;
		
		public DeduplicationThread(int id) {
			setName("DeduplicationThread-" + id);
		}
		
		public void run() {
			Log.log(getClass(), "Thread " + getName() + " starts.", 2);
			
			DataRow[][] bucket;
			try {
				while (!cancel && (bucket = buckets.getBucket()) != null) {
					deduplicate(bucket[0]);
					completed.addAndGet(bucket[0].length);
				}
			} catch (IOException e) {
				exception = new RJException("Error reading data from BucketsManager", e);
			} catch (RJException e) {
				exception = e;
			}
			
			latch.countDown();
			Log.log(getClass(), "Thread " + getName() + " completed work.", 2);
		}
		
		private void deduplicate(DataRow[] dataRows) throws IOException, RJException {
			//the two arrays below can be combined into one (store special object)
			List duplicates = new ArrayList();
			List scores = new ArrayList();
			for (int i = 0; i < dataRows.length; i++) {
				if (dataRows[i] == null) {
					continue;
				}
				DataRow rowToSave = RowUtils.copyRow(dataRows[i]);
				for (int j = i + 1; j < dataRows.length; j++) {
					if (dataRows[j] == null) {
						continue;
					}
					int acceptance = config.getAcceptanceLevel();
					int score = duplicate(rowToSave, dataRows[j]);
					if (score >= acceptance) {
						updateRowToSaveWithEmptyData(rowToSave, dataRows[j]);
						duplicates.add(dataRows[j]);
						scores.add(new Integer(score));
						dataRows[j] = null;
						nDuplicates.incrementAndGet();
					}
				}
				
				synchronized (deduplicatedData) {
					deduplicatedData.addRow(rowToSave);
				}
				nonDuplicatesCnt++;
				if (duplicates.size() != 0 && minusSaver != null) {
					//this is the original record that wasn't counted...
					//nDuplicates.incrementAndGet();
					
					synchronized (minusSaver) {
						int id = duplicateId.incrementAndGet();
						minusSaver.saveRow(extendRow(dataRows[i], id, 0));
						for (Iterator iterator = duplicates.iterator(), it2 = scores.iterator(); iterator.hasNext() && it2.hasNext();) {
							DataRow duplicate = (DataRow) iterator.next();
							int score = ((Integer)it2.next()).intValue();
							minusSaver.saveRow(extendRow(duplicate, id, score));
						}
					}
				}
				
				dataRows[i] = null;
				duplicates.clear();
			}
		}

		private void updateRowToSaveWithEmptyData(DataRow rowToSave, DataRow dataRow) {
			DataColumnDefinition[] model = dataRow.getRowModel();
			DataCell[] cellsToSave = rowToSave.getData();
			DataCell[] cellsFromDuplicate = dataRow.getData();
			for (int i = 0; i < cellsFromDuplicate.length; i++) {
				if (cellsToSave[i].isEmpty(model[i])) {
					cellsToSave[i].setValue(cellsFromDuplicate[i].getValue());
				}
			}
		}

		private DataRow extendRow(DataRow dataRow, int id, int score) {
			DataCell[] oldCells = dataRow.getData();
			DataCell[] cells = new DataCell[oldCells.length + 2];
			cells[0] = new DataCell(DataColumnDefinition.TYPE_STRING, String.valueOf(id));
			cells[1] = new DataCell(DataColumnDefinition.TYPE_STRING, String.valueOf(score));
			System.arraycopy(oldCells, 0, cells, 2, oldCells.length);
			if (model == null) {
				DataColumnDefinition[] oldModel = dataRow.getRowModel();
				model = new DataColumnDefinition[oldModel.length + 2];
				model[0] = new DataColumnDefinition("Duplicate ID", DataColumnDefinition.TYPE_STRING, oldModel[0].getSourceName());
				model[1] = new DataColumnDefinition("Duplicate score", DataColumnDefinition.TYPE_STRING, oldModel[0].getSourceName());
				System.arraycopy(oldModel, 0, model, 2, oldModel.length);
			}
			return new DataRow(model, cells);
		}

		private int duplicate(DataRow r1, DataRow r2) {
			DataColumnDefinition[] cols = config.getTestedColumns();
			AbstractDistance[] distances = config.getTestCondition();
			double[] emptyMatches = config.getEmptyMatchScore();
			int[] weights = config.getWeights();
			int acceptance = config.getAcceptanceLevel();
			int sum = 0;
			int weightsToGo = 100;
			for (int i = 0; i < distances.length; i++) {
				DataCell cellA = r1.getData(cols[i]);
				DataCell cellB = r2.getData(cols[i]);
				if (emptyMatches[i] != 0 && (cellA.isEmpty(cols[i]) || cellB.isEmpty(cols[i]))) {
					sum += weights[i] * emptyMatches[i];
				} else {
					double score  = distances[i].distance(cellA, cellB);
					sum += score * weights[i] / 100.0;
				}
				weightsToGo -= weights[i];
				if (weightsToGo + sum < acceptance) {
					return sum;
				}
				//Below was removed as now we want to see the the full score of duplicates
//				else if (sum >= acceptance) {
//					Log.log(getClass(), "Duplicates identified:\n   " + r1 + "\n   " + r2, 3);
//					return true;
//				}
			}
			return sum;
			//return false;
		}
		
		public void cancel() {
			cancel = true;
		}
		
//		public int getDuplicatesCnt() {
//			return duplicatesCnt;
//		}
		
		public int getNonDuplicatesCnt() {
			return nonDuplicatesCnt;
		}
		
	}

	public int getProgress() {
		return (1 * progressDataSource.get() + 2 * progressDedupe.get()) / 3;
	}

	public int getDuplicatesCount() {
		return nDuplicates.get();
	}

	public int getInputRecordsCount() {
		return inputRecordsCnt;
	}
	
	public void cancel() {
		cancel = true;
		synchronized (this) {
			if (threads != null) {
				for (int i = 0; i < threads.length; i++) {
					threads[i].cancel();
				}
			}
		}
	}
	
}
