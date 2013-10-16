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


package cdc.impl.join.snm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.components.EvaluatedCondition;
import cdc.datamodel.DataRow;
import cdc.impl.join.snm.SNMJoin_v1.SNMJoinConnector;
import cdc.utils.Log;
import cdc.utils.RJException;
import cdc.utils.RowUtils;

public class JoiningThread extends Thread {
	
	private class Shift {
		public Shift(int n) {
			this.n = n;
		}
		int n;
		public String toString() {
			return String.valueOf(n);
		}
	}

	private static final String PROP_IS_OUT_OF_BUFFER = "out";
	
	private AbstractDataSource sourceA;
	private AbstractDataSource sourceB; 
	private ArrayBlockingQueue outBuffer; 
	private SNMJoinConnector connector;
	private long[] sourceAStartIndexes;
	private long workSize;
	private int id;
	
	private volatile boolean stop;
	private volatile boolean finished;
	private AtomicInteger progress = new AtomicInteger(0);
	private RJException error;
	
	private DataRow nextA;
	private DataRow nextB;

	private LinkedList buffer = new LinkedList();
	private int bufferedLeft = 0;
	private int bufferedRight = 0;
	
	private List mapLeft = new ArrayList();
	private List mapRight = new ArrayList();
	
	private int addedLeft = 0;
	private int addedRight = 0;
	
	private int testedNumber = 0;
	private int lastIndex = -1;
	private int prevIndex = 0;
	
	private boolean first = true;
	
	private boolean leftEnded = false;
	private int rightAfterLeftEnded = 0;
	private long readA = 0;
	private long readB = 0;

	public JoiningThread(int id, AbstractDataSource sourceA, AbstractDataSource sourceB, long[] startIndexes, long workSize, ArrayBlockingQueue buffer, SNMJoinConnector connector) {
		this.sourceA = sourceA;
		this.sourceB = sourceB;
		this.outBuffer = buffer;
		this.connector = connector;
		this.sourceAStartIndexes = startIndexes;
		this.id = id;
		this.workSize = workSize;
	}
	
	public JoiningThread(AbstractDataSource sourceA, AbstractDataSource sourceB, ArrayBlockingQueue buffer, SNMJoinConnector connector) {
		this.sourceA = sourceA;
		this.sourceB = sourceB;
		this.outBuffer = buffer;
		this.connector = connector;
	}
	
	public void run() {
		Log.log(getClass(), "Thread " + getName() + " starts.", 2);
		if (sourceAStartIndexes != null) {
			Log.log(getClass(), "Work assignment summary: start position(left source)=" + sourceAStartIndexes[id] + ", working set size=" + workSize, 2);
		}
		try {
			positionDataSources();
			if (!shouldCancel()) {
				linkData();
			}
			finishProcess();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (RJException e) {
			error = e;
		} catch (IOException e) {
			error = new RJException("I/O Exception occured.", e);
		} catch (Exception e) {
			error = new RJException("Exception occured.", e);
		}
		synchronized (this) {
			finished = true;
			notifyAll();
		}
		Log.log(getClass(), "Thread " + getName() + " finishes. Size of data read: " + sourceA.getSourceName() + "=" + readA + ", " + sourceB.getSourceName() + "=" + readB, 2);
	}

	private void finishProcess() throws IOException, RJException {
		int n = 0;
		boolean first = true;
		DataRow row;
		Log.log(getClass(), "Thread finished linkage. Draining data sources. (readFromLeft=" + readA + ")", 1);
		
		//The below was added on 07/28/09 - fixed missing record in minus file.
		if (nextA != null) {
			if (RowUtils.shouldReportTrashingNotJoined(connector.getJoin(), nextA)) {
				connector.notifyTrashingNotJoined(nextA);
			}
		}
		if (nextB != null) {
			if (RowUtils.shouldReportTrashingNotJoined(connector.getJoin(), nextB)) {
				connector.notifyTrashingNotJoined(nextB);
			}
		}
		
		while ((row = sourceA.getNextRow()) != null) {
			readA++;
			if (first) {
				Log.log(getClass(), "First leftover in " + sourceA.getSourceName() + ": " + row, 2);
			}
			if (RowUtils.shouldReportTrashingNotJoined(connector.getJoin(), row)) {
				connector.notifyTrashingNotJoined(row);
			}
			n++;
			first = false;
		}
		if (n != 0) {
			Log.log(getClass(), "Leftovers in " + sourceA.getSourceName() + ": " + n, 2);
		}
		n = 0;
		first = true;
		while ((row = sourceB.getNextRow()) != null) {
			if (first) {
				Log.log(getClass(), "First leftover in " + sourceB.getSourceName() + ": " + row, 2);
			}
			if (RowUtils.shouldReportTrashingNotJoined(connector.getJoin(), row)) {
				connector.notifyTrashingNotJoined(row);
			}
			n++;
			first = false;
		}
		if (n != 0) {
			Log.log(getClass(), "Leftovers in " + sourceB.getSourceName() + ": " + n, 2);
		}
	}

	private boolean shouldCancel() {
		return connector.isCancelled() || stop;
	}

	private void positionDataSources() throws IOException, RJException {
		if (sourceAStartIndexes == null) {
			//this is the only thread, and no positioning is required.
			Log.log(getClass(), "Thread " + getName() + ": no data positioning. Ready to join.", 2);
			return;
		}
		if (id != 0) {
			//start with item before window - to find right position in sourceB compensating cut comaprisons
			long start = sourceAStartIndexes[id] - connector.getWindow() + 1;
			if (start < 0) start = 0;
			if (shouldCancel()) {
				return;
			}
			sourceA.setStart(start);
			DataRow lastFromPrevious = sourceA.getNextRow();
			if (shouldCancel() || lastFromPrevious == null) {
				return;
			}
			//move to true start
			sourceA.skip(connector.getWindow() - 2);
			
			if (shouldCancel()) {
				return;
			}
			//find start position in right source
			DataRow rowRight = null;
			while ((rowRight = sourceB.getNextRow()) != null) {
				int compare = RowUtils.compareRows(lastFromPrevious, rowRight, connector.getLeftOrder(), connector.getRightOrder(), connector.getCompareFunctions());
				if (compare <= 0) {
					break;
				}
				if (shouldCancel()) {
					return;
				}
			}
			nextB = rowRight;
		}
		if (shouldCancel()) {
			return;
		}
		sourceA.setLimit(workSize);
		Log.log(getClass(), "Thread " + getName() + " finished data positioning. Ready to join.", 2);
	}
	
	private void linkData() throws RJException, IOException, InterruptedException {
		if (first) {
			nextA = getNextA();
			if (nextB == null) {
				nextB = getNextB();
			}
			first  = false;
		}
		while (true) {
//			boolean report = false;
			if (connector.isCancelled() || stop) {
				return;
			}
			
			calculateProgress();
			
			DataRow[] candidates = getNextCandidates();
			DataRow rowA, rowB;
			
			if (candidates == null) {
				return;
			}
//			if (candidates[1].getData("id").getValue().equals("1101")) {
//				System.out.println("Hello...");
//				report = true;
//			}
			
			if (candidates[0].getSourceName().equals(connector.getJoinCondition().getLeftJoinColumns()[0].getSourceName())) {
				rowA = candidates[0];
				rowB = candidates[1];
			} else {
				rowA = candidates[1];
				rowB = candidates[0];
			}
			
			DataRow row1Projected = RowUtils.buildSubrow(rowA, connector.getJoinCondition().getLeftJoinColumns());
			DataRow row2Projected = RowUtils.buildSubrow(rowB, connector.getJoinCondition().getRightJoinColumns());
			
			Log.log(getClass(), "Row1: " + row1Projected, 3);
			Log.log(getClass(), "Row2: " + row2Projected, 3);
			EvaluatedCondition eval;
			if ((eval = connector.getJoinCondition().conditionSatisfied(rowA, rowB)).isSatisfied()) {
				DataRow joined = RowUtils.buildMergedRow(connector.getJoin(), rowA, rowB, connector.getOutColumns(), eval);
				if (connector.isAnyJoinListenerRegistered()) {
					connector.notifyJoined(rowA, rowB, joined);
				}
				outBuffer.put(joined);
			} else {
				if (connector.isAnyJoinListenerRegistered()) {
					connector.notifyNotJoined(rowA, rowB, eval.getConfidence());
				}
				String property = rowA.getProperty(PROP_IS_OUT_OF_BUFFER);
				if (property != null && property.equals("t")) {
					dispose(rowA);
				}
				String property2 = rowB.getProperty(PROP_IS_OUT_OF_BUFFER);
				if (property2 != null && property2.equals("t")) {
					dispose(rowB);
				}
			}
			//log("\n");
		}
	}

	private DataRow[] getNextCandidates() throws IOException, RJException {
		
		while (bufferedLeft < connector.getWindow() || bufferedRight < connector.getWindow()) {
			if (nextA != null && nextB != null) {
				if (RowUtils.compareRows(nextA, nextB, connector.getLeftOrder(), connector.getRightOrder(), connector.getCompareFunctions()) < 0) {
					// left is smaller
					buffer.add(nextA);
					nextA = getNextA();
					bufferedLeft++;
					addedLeft++;
					
					mapLeft.add(new Shift(addedRight));
					addedRight = 0;
				} else {
					buffer.add(nextB);
					nextB = getNextB();
					bufferedRight++;
					addedRight++;
					
					mapRight.add(new Shift(addedLeft));
					addedLeft = 0;
				}
			} else {
				if (nextA != null && bufferedLeft < connector.getWindow()) {
					buffer.add(nextA);
					nextA = getNextA();
					bufferedLeft++;
					addedLeft++;
					
					mapLeft.add(new Shift(addedRight));
					addedRight = 0;
				} else if (nextB != null && bufferedRight < connector.getWindow()) {
					buffer.add(nextB);
					nextB = getNextB();
					bufferedRight++;
					addedRight++;
					
					mapRight.add(new Shift(addedLeft));
					addedLeft = 0;
				} else {
					break;
				}
			}
		}
		
		while (buffer.size() != 0) {
			
			DataRow first = (DataRow) buffer.getFirst();
			
			lastIndex = getIndex(first, testedNumber);
			if (lastIndex == -1) {
				substractedFromBuffer((DataRow)buffer.removeFirst());
				dispose(first);
				testedNumber = 0;
				prevIndex = 0;
				continue;
			}
			
			DataRow second = (DataRow)buffer.get(lastIndex);
			testedNumber++;
			
			if (testedNumber >= connector.getWindow()) {
				substractedFromBuffer((DataRow)buffer.removeFirst());
				testedNumber = 0;
				prevIndex = 0;
			}
			
			return new DataRow[] {first, second};
		}
		return null;
	}

	private DataRow getNextB() throws IOException, RJException {
		if (leftEnded && rightAfterLeftEnded > connector.getWindow()) {
			return null;
		}
		if (leftEnded) {
			rightAfterLeftEnded++;
		}
		DataRow nextRow = sourceB.getNextRow();
		if (nextRow != null) {
			readB++;
			nextRow.setProperty(PROP_IS_OUT_OF_BUFFER, "f");
		}
		return nextRow;
	}

	private DataRow getNextA() throws IOException, RJException {
		DataRow row = sourceA.getNextRow();
		if (row != null) {
			row.setProperty(PROP_IS_OUT_OF_BUFFER, "f");
			readA++;
		}
		if (row == null) {
			leftEnded  = true;
		}
		return row;
	}

	private void dispose(DataRow disposed) throws RJException {
		if (disposed != null) {
			if (disposed.getProperty(AbstractJoin.PROPERTY_JOINED) != null) {
				connector.notifyTrashingJoined(disposed);
			} else {
				if (RowUtils.shouldReportTrashingNotJoined(connector.getJoin(), disposed)) {
					connector.notifyTrashingNotJoined(disposed);
				}
			}
		}
	}

	private int getIndex(DataRow row, int position) {
		
		List activeMap = null;
		if (row.getSourceName().equals(sourceA.getSourceName())) {
			activeMap = mapRight;
		} else {
			activeMap = mapLeft;
		}
		
		if (activeMap.size() <= position) {
			return -1;
		}
		
		prevIndex += ((Shift)activeMap.get(position)).n;
		if (position != 0) prevIndex++;
		
		return prevIndex;
	}

	private void substractedFromBuffer(DataRow row) throws RJException {
		//dispose(row);
		row.setProperty(PROP_IS_OUT_OF_BUFFER, "t");
		if (row.getSourceName().equals(sourceA.getSourceName())) {
			bufferedLeft--;
			mapLeft.remove(0);
			if (mapRight.size() != 0) {
				((Shift)mapRight.get(0)).n--;
			}
		} else {
			bufferedRight--;
			mapRight.remove(0);
			if (mapLeft.size() != 0) {
				((Shift)mapLeft.get(0)).n--;
			}
		}
	}

	private void calculateProgress() throws IOException, RJException {
		if (sourceAStartIndexes == null) {
			progress.set((int) (sourceA.position() / (double)sourceA.size() * 100));
		} else {
			progress.set((int) (sourceA.position() / (double)workSize * 100));
		}
	}

//	private double getPosition() {
//		long pos = sourceA.position();
//		System.out.println(getName() + " -> " + pos + "   " + sourceAStartIndexes[id]);
//		return pos - sourceAStartIndexes[id] < 0 ? 0 : pos - sourceAStartIndexes[id];
//	}

	public void stopProcessing() {
		try {
			synchronized(this) {
				stop = true;
				interrupt();
				while (!finished) {
					wait();
				}
			}
			this.sourceA = null;
			this.sourceB = null;
			this.buffer = null;
			this.connector = null;
			this.mapLeft = null;
			this.mapRight = null;
			this.nextA = null;
			this.nextB = null;
			this.outBuffer = null;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public RJException checkError() {
		return error;
	}

	public boolean isFinished() {
		synchronized (this) {
			return finished;
		}
	}

	public int getProgress() {
		return progress.get();
	}
	
	public long getReadA() {
		return readA;
	}
	
	public long getReadB() {
		return readB;
	}
}
