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


package cdc.impl.join.nestedloop;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import cdc.components.AbstractDataSource;
import cdc.components.EvaluatedCondition;
import cdc.datamodel.DataRow;
import cdc.impl.join.nestedloop.NestedLoopJoin.NLJConnector;
import cdc.utils.Log;
import cdc.utils.RJException;
import cdc.utils.RowUtils;

public class NLJThread extends Thread {
	
	private static final int BUFFER_SIZE = 200;
	
	private AbstractDataSource sourceA;
	private AbstractDataSource sourceB;
	private ArrayBlockingQueue buffer;
	private NLJConnector connector;
	
	private volatile boolean completed = false;
	private volatile boolean stopped = false;
	private volatile RJException error;
	
	private int readA = 0;
	private int readB = 0;
	
	private int step = 1;
	
	public NLJThread(AbstractDataSource sourceA, AbstractDataSource sourceB, ArrayBlockingQueue resultBuffer, NLJConnector join) {
		this.sourceA = sourceA;
		this.sourceB = sourceB;
		this.buffer = resultBuffer;
		this.connector = join;
	}
	
	public void run() {
		DataRow rowA[];
		try {
			Log.log(getClass(), "Thread starts.", 2);
			main: while ((rowA = fillInBuffer(sourceA, BUFFER_SIZE)) != null) {
				readA += rowA.length;
				DataRow rowB[];
				Log.log(NestedLoopJoin.class, "Outer loop starts", 3);
				while ((rowB = fillInBuffer(sourceB, 1)) != null) {
					readB += rowB.length;
					for (int i = 0; i < rowA.length; i++) {
						for (int j = 0; j < rowB.length; j++) {
							calculateProgress();
							Log.log(NestedLoopJoin.class, "Inner loop starts", 4);
							EvaluatedCondition eval;
							if ((eval = connector.getJoinCondition().conditionSatisfied(rowA[i], rowB[j])).isSatisfied()) {
								DataRow row = RowUtils.buildMergedRow(connector.getJoin(), rowA[i], rowB[j], connector.getOutColumns(), eval);
								if (connector.getLogLevel() >= 3) {
									Log.log(NestedLoopJoin.class, "Row joined: " + row, 4);
								}
								if (connector.isAnyJoinListenerRegistered()) {
									connector.notifyJoined(rowA[i], rowB[j], row);
								}
								buffer.put(row);
							} else {
								step++;
								//this is only to see debug info...
								if (step % 1000 == 0 && connector.isAnyJoinListenerRegistered()) {
									step = 1;
									connector.notifyNotJoined(rowA[i], rowB[j], eval.getConfidence());
								}
							}
						}
					}
					if (connector.isCancelled() || stopped) {
						break main;
					}
				}
				
				for (int i = 0; i < rowA.length; i++) {
					if (RowUtils.shouldReportTrashingNotJoined(connector.getJoin(), rowA[i])) {
						connector.notifyTrashingNotJoined(rowA[i]);
					}
				}
				Log.log(NestedLoopJoin.class, "Inner source read fully", 3);
				sourceB.reset();
			}
			
			//System.out.println(getName() + " read " + readA + " rows.");
			
		} catch (RJException e) {
			error = e;
		} catch (IOException e) {
			error = new RJException("Error while joining data", e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			try {
				sourceB.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (RJException e) {
				e.printStackTrace();
			}
		}
		
		Log.log(getClass(), "Thread completed.", 2);
		
		synchronized (this) {
			completed = true;
			this.notifyAll();
		}
		
	}

	private DataRow[] fillInBuffer(AbstractDataSource source, int size) throws IOException, RJException {
		return source.getNextRows(size);
	}

	private void calculateProgress() {
		
	}
	
	public RJException getError() {
		try {
			synchronized (this) {
				while (!completed) {
					this.wait();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return error;
	}

	public void stopProcessing() {
		stopped = true;
		//just wait for finish...
		getError();
	}

	public boolean isFinished() {
		synchronized (this) {
			return completed;
		}
	}
	
	public int getReadA() {
		return readA;
	}
	
	public int getReadB() {
		return readB;
	}
	
}
