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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.utils.CompareFunctionInterface;
import cdc.utils.Log;
import cdc.utils.RJException;

public class SortThread extends Thread {
	
	private class Wrapper {
		DataRow object;
		public Wrapper(DataRow o) {
			this.object = o;
		}
	}
	
	private Object monitor = new Object();
	private SortedData sortedData;
	
	private BlockingQueue queue = new ArrayBlockingQueue(10000);
	
	private volatile boolean finished = false;
	private volatile boolean dataComplete = false;
	private volatile boolean stopProcessing = false;
	private volatile long size = 0;
	
	public SortThread(String sourceName, DataColumnDefinition[] orderBy, CompareFunctionInterface[] functions) {
		sortedData = new SortedData(sourceName, orderBy, functions);
		start();
	}
	
	public void run() {
		try {
			while (!stopProcessing) {
				synchronized(monitor) {
					if (dataComplete) {
						Log.log(getClass(), getName() + ": Thread recognized reading finished.", 2);
						sortedData.complete();
						finished = true;
						monitor.notifyAll();
						break;
					} else if (sortedData.shouldFlush()) {
						Log.log(getClass(), getName() + ": Flushing data to temporary file.", 2);
						sortedData.flush();
					} else {
						Log.log(getClass(), getName() + ": Thread goes to sleep.", 2);
						try {
							monitor.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
			if (sortedData.usedExternalFile() && !stopProcessing) {
				Log.log(getClass(), getName() + ": Thread will read data asynchronously.", 2);
				DataRow row;
				while (!stopProcessing && (row = sortedData.getNextSortedRow()) != null) {
					queue.put(new Wrapper(row));
				}
				if (!stopProcessing) {
					queue.put(new Wrapper(null));
				}
			}
			if (stopProcessing) {
				finished = true;
				queue.offer(new Wrapper(null));
			}
			Log.log(getClass(), getName() + ": Thread completed its work. Data size: " + size, 1);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (RJException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void addDataRow(DataRow row) throws FileNotFoundException, IOException {
		synchronized (monitor) {
			if (row == null) {
				Log.log(getClass(), getName() + ": Thread completes reading.", 1);
				dataComplete = true;
				monitor.notifyAll();
			} else {
				size++;
				sortedData.addRow(row);
				if (sortedData.shouldFlush()) {
					monitor.notifyAll();
				}
			}
		}
	}
	
	public DataRow getNextSortedRow() throws IOException, RJException {
		synchronized (monitor) {
			while (!finished) {
				try {
					monitor.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (sortedData.usedExternalFile()) {
				try {
					return ((Wrapper) queue.take()).object;
				} catch (InterruptedException e) {
					e.printStackTrace();
					return null;
				}
			} else {
				return sortedData.getNextSortedRow();
			}
		}
	}

	public SortedData getSortedData() {
		synchronized (monitor) {
			while (!finished) {
				try {
					monitor.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return sortedData;
		}
	}

	public void stopProcessing() {
		stopProcessing = true;
		sortedData.interruptAction();
		synchronized (monitor) {
			monitor.notifyAll();
		}
		while (!finished) {
			synchronized (this) {
				try {
					wait(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public long getRowsNumber() {
		return size;
	}

	public void cleanup() {
		this.sortedData.cleanup();
		this.sortedData = null;
		this.queue = null;
	}
	
}
