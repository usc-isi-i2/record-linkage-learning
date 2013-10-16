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


package cdc.impl.join.blocking;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.datamodel.DataRow;
import cdc.utils.Log;
import cdc.utils.RJException;

/**
 * Thread that reads data from input source and attempts to add the record to bucket manager.
 * As add method in bucket manager calculates value of hashing function and then schedules the operation,
 * this is potential speedup of processing as the evaluation of hashing function can be pricy...
 * @author Pawel Jurczyk
 *
 */
public class HashingThread extends Thread {
	
	/**
	 * Reference to parent join.
	 */
	private AbstractJoin join;
	
	/**
	 * First data source
	 */
	private AbstractDataSource sourceA;
	
	/**
	 * Second data source
	 */
	private AbstractDataSource sourceB;
	
	/**
	 * Latch that should be notified when processing is completed.
	 */
	private CountDownLatch latch;
	
	/**
	 * The bucket manager that will accept the records
	 */
	private BucketManager manager;
	
	/**
	 * Just for statistical purposes - size of input data processed by this thread.
	 */
	private int readA = 0;
	private int readB = 0;
	
	/**
	 * An exception that occurred during the processing.
	 */
	private volatile RJException error;
	
	/**
	 * Creates new hashing thread
	 * @param join
	 * @param latch
	 * @param manager
	 */
	public HashingThread(AbstractJoin join, CountDownLatch latch, BucketManager manager) {
		this.sourceA = join.getSourceA();
		this.sourceB = join.getSourceB();
		this.latch = latch;
		this.manager = manager;
		this.join = join;
	}

	/**
	 * The mail work function.
	 */
	public void run() {
		Log.log(getClass(), "Thread " + getName() + " is starting.");
		
		try {
			DataRow row;
			while ((row = sourceA.getNextRow()) != null && !join.isCancelled()) {
				manager.addToBucketLeftSource(row);
				readA++;
			}
			while ((row = sourceB.getNextRow()) != null && !join.isCancelled()) {
				manager.addToBucketRightSource(row);
				readB++;
			}
		} catch (RJException e) {
			error = e;
		} catch (IOException e) {
			error = new RJException("Exception", e);
		}
		
		latch.countDown();
		manager = null;
		sourceA = null;
		sourceB = null;
		latch = null;
		Log.log(getClass(), "Thread " + getName() + " is done.");
	}
	
	/**
	 * Report any error that could occur in run
	 * @return
	 */
	public RJException getError() {
		return error;
	}
	
	/**
	 * Returns number of records read by this thread from first data source.
	 * @return
	 */
	public int getReadA() {
		return readA;
	}
	
	/**
	 * Returns number of records read by this thread from second data source.
	 * @return
	 */
	public int getReadB() {
		return readB;
	}
	
}
