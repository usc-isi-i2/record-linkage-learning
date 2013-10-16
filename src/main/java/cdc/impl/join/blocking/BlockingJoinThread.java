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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import cdc.components.AbstractJoin;
import cdc.components.EvaluatedCondition;
import cdc.datamodel.DataRow;
import cdc.impl.join.blocking.BlockingJoin.BlockingJoinConnector;
import cdc.impl.join.blocking.BlockingJoin.Wrapper;
import cdc.utils.Log;
import cdc.utils.RJException;
import cdc.utils.RowUtils;

/**
 * This class represents a thread that does the linkage in BlockingJoin.
 * The BlockingJoin can start as many threads as needed to speedup the work.
 * The threads are getting new job assignment from bucket manager.
 * @author pjurczy
 *
 */
public class BlockingJoinThread extends Thread {
	
	/**
	 * A reference to BlockingJoinConnector - a communication interface between thread and BlockingJoin class.
	 */
	private BlockingJoinConnector join;
	
	/**
	 * Bucket manager that provides data
	 */
	private BucketManager bucketManager;
	
	/**
	 * Output queue for linkages.
	 */
	private ArrayBlockingQueue resultsBuffer;
	
	/**
	 * The bucket currently being used by this thread
	 */
	private DataRow[][] activeBucket;
	
	/**
	 * Pointers to currently considered records from the bucket
	 */
	private int index1;
	private int index2;
	
	/**
	 * Just a statistics - to be able to estimate the progress
	 */
	private AtomicLong bucketsCompleted = new AtomicLong(0);
	private AtomicInteger completedWithinBucket = new AtomicInteger(0);
	
	/**
	 * Number of joined records by this thread
	 */
	private long joined = 0;
	
	/**
	 * Info about error - so that it can be passed up.
	 */
	private volatile RJException error;
	
	/**
	 * Signal of thread being done with work
	 */
	private volatile boolean finished;
	
	/**
	 * Signal to force stop thread
	 */
	private volatile boolean forceFinish = false;
	
	/**
	 * Another statistics - # of records processed by this thread. Used for logging. 
	 */
	private int tB = 0;
	private int tA = 0;
	
	/**
	 * Creates a new thread
	 * @param manager bucket manager that will provide data
	 * @param resultBuffer buffer for the results
	 * @param join reference to the join connector
	 */
	public BlockingJoinThread(BucketManager manager, ArrayBlockingQueue resultBuffer, BlockingJoinConnector join) {
		this.join = join;
		this.bucketManager = manager;
		this.resultsBuffer = resultBuffer;
	}
	
	/**
	 * The main working function...
	 */
	public void run() {
		
		Log.log(getClass(), "Thread " + getName() + " is starting.", 2);
		
		
		try {
			
			//The process continues until there is a data in bucket manager
		main: while (true) {
			
			//Load new bucket if necessary
			if (activeBucket == null || (index1 == activeBucket[0].length)) {
				do {
					activeBucket = bucketManager.getBucket();
					bucketsCompleted.incrementAndGet();
					completedWithinBucket.set(0);
					if (activeBucket == null) {
						break main;
					}
					tA += activeBucket[0].length; tB += activeBucket[1].length;
					if (activeBucket[0].length == 0) {
						//report to minus all the records
						for (int i = 0; i < activeBucket[1].length; i++) {
							join.notifyTrashingNotJoined(activeBucket[1][i]);
						}
					}
					if (activeBucket[1].length == 0) {
						//report to minus all the records
						for (int i = 0; i < activeBucket[0].length; i++) {
							join.notifyTrashingNotJoined(activeBucket[0][i]);
						}
					}
				} while (activeBucket[0].length == 0 || activeBucket[1].length == 0);
				index1 = index2 = 0;
				//System.out.println("Buckets: " + activeBucket[0].length + " <--> " + activeBucket[1].length);
			}
			
			//Process the bucket (or continue processing previously started bucket)
			long completed = 0;
			for (; index1 < activeBucket[0].length; index1++) {
				for (; index2 < activeBucket[1].length; index2++) {
					completed++;
					completedWithinBucket.set((int)(completed / (double)(activeBucket[0].length * activeBucket[1].length)));
					DataRow rowA = activeBucket[0][index1];
					DataRow rowB = activeBucket[1][index2];
					EvaluatedCondition eval;
					if ((eval = join.getJoinCondition().conditionSatisfied(rowA, rowB)).isSatisfied()) {
						DataRow joined = RowUtils.buildMergedRow(join.getJoin(), rowA, rowB, join.getOutColumns(), eval);
						if (join.isAnyJoinListenerRegistered()) {
							join.notifyJoined(rowA, rowB, joined);
						}
						this.joined++;
						Wrapper w = new Wrapper();
						w.row = joined;
						resultsBuffer.put(w);
					} else {
						if (join.isAnyJoinListenerRegistered()) {
							join.notifyNotJoined(rowA, rowB, eval.getConfidence());
						}
					}
					
					if (join.isCancelled() || forceFinish ) {
						break main;
					}
				}
				index2 = 0;
				if (activeBucket[0][index1].getProperty(AbstractJoin.PROPERTY_JOINED) != null) {
					join.notifyTrashingJoined(activeBucket[0][index1]);
				} else if (RowUtils.shouldReportTrashingNotJoined(join.getJoin(), activeBucket[0][index1])) {
					join.notifyTrashingNotJoined(activeBucket[0][index1]);
				}
			}
			for (index2=0; index2 < activeBucket[1].length; index2++) {
				if (activeBucket[1][index2].getProperty(AbstractJoin.PROPERTY_JOINED) != null) {
					join.notifyTrashingJoined(activeBucket[1][index2]);
				} else if (RowUtils.shouldReportTrashingNotJoined(join.getJoin(), activeBucket[1][index2])) {
					join.notifyTrashingNotJoined(activeBucket[1][index2]);
				}
			}
			
		}
			
		} catch (RJException e) {
			synchronized (this) {
				error = e;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			synchronized (this) {
				error = new RJException("Exception in joining thread", e);
			}
		} catch (Exception e) {
			synchronized (this) {
				error = new RJException("Exception in joining thread", e);
			}
		}
		endSequence();
		synchronized (this) {
			finished = true;
		}
	}

	private void endSequence() {
		Log.log(getClass(), "Thread " + getName() + " finished its job. Joined records: " + joined + "; used buckets: " + bucketsCompleted.get() + " tested records: " + tA + "<->" + tB, 2);
		this.bucketManager = null;
		this.activeBucket = null;
	}

	/**
	 * Reports number of completed buckets by this thread
	 * @return
	 */
	public long getCompletedBuckets() {
		return bucketsCompleted.get();
	}

	/**
	 * Reports number of completed records within the bucket 
	 * @return
	 */
	public long getCompletedWithinBucket() {
		return completedWithinBucket.get();
	}
	
	/**
	 * Indication whether the thread is done.
	 * @return
	 */
	public boolean done() {
		synchronized (this) {
			return finished;
		}
	}
	
	/**
	 * Returns the stored error, if any
	 * @return
	 */
	public RJException getError() {
		synchronized (this) {
			return error;
		}
	}

	/**
	 * Stops the work of this thread
	 */
	public void stopProcessing() {
		try {
			interrupt();
			forceFinish = true;
			synchronized (this) {
				while (!finished) {
					this.wait();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
