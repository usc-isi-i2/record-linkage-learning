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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.components.AbstractJoinCondition;
import cdc.components.LinkageSummary;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.gui.GUIVisibleComponent;
import cdc.utils.CPUInfo;
import cdc.utils.HTMLUtils;
import cdc.utils.Log;
import cdc.utils.PrintUtils;
import cdc.utils.Props;
import cdc.utils.RJException;

public class NestedLoopJoin extends AbstractJoin {

	private static int logLevel = Log.getLogLevel(NestedLoopJoin.class);
	
	
	public class NLJConnector {
		public boolean isCancelled() {
			return NestedLoopJoin.this.isCancelled();
		}
		public int getLogLevel() {
			return logLevel;
		}
		public DataColumnDefinition[] getOutColumns() {
			return NestedLoopJoin.this.getOutColumns();
		}
		public AbstractJoinCondition getJoinCondition() {
			return NestedLoopJoin.this.getJoinCondition();
		}
		public boolean isAnyJoinListenerRegistered() {
			return NestedLoopJoin.this.isAnyJoinListenerRegistered();
		}
		public void notifyNotJoined(DataRow rowA, DataRow rowB, int conf) throws RJException {
			NestedLoopJoin.this.notifyNotJoined(rowA, rowB, conf);
		}
		public void notifyJoined(DataRow rowA, DataRow rowB, DataRow row) throws RJException {
			NestedLoopJoin.this.notifyJoined(rowA, rowB, row);
		}
		public void notifyTrashingNotJoined(DataRow dataRow) throws RJException {
			NestedLoopJoin.this.notifyTrashingNotJoined(dataRow);
		}
		public void notifyTrashingJoined(DataRow dataRow) throws RJException {
			NestedLoopJoin.this.notifyTrashingJoined(dataRow);
		}
		public AbstractJoin getJoin() {
			return NestedLoopJoin.this;
		}
	}
	
	private NLJThread[] workers;
	private ArrayBlockingQueue buffer = new ArrayBlockingQueue(Props.getInteger("intrathread-buffer"));
	private boolean closed = false;

	private int readA;
	private int readB;
	
	private NLJConnector connector = new NLJConnector();
	
	public NestedLoopJoin(AbstractDataSource sourceA, AbstractDataSource sourceB, DataColumnDefinition outFormat[], AbstractJoinCondition cond, Map props) throws RJException {
		super(sourceA, sourceB, cond, outFormat, props);
		Log.log(NestedLoopJoin.class, "Join operator created", 1);
		Log.log(NestedLoopJoin.class, "Out model: " + PrintUtils.printArray(getOutColumns()), 2);
	}

	protected DataRow doJoinNext() throws IOException, RJException {
		
		if (closed) {
			return null;
		}
		
		createWorkersIfNeeded();
		
		try {
			main: while (true) {
				DataRow row = (DataRow) buffer.poll(100, TimeUnit.MILLISECONDS);
				calculateProgress();
				if (row != null) {
					return row;
				} else if (isCancelled()) {
					updateSrcStats();
					return null;
				} else {
					for (int i = 0; i < workers.length; i++) {
						if (!workers[i].isFinished()) {
							continue main;
						}
					}
					updateSrcStats();
					return null;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void updateSrcStats() {
		for (int i = 0; i < workers.length; i++) {
			readA += workers[i].getReadA();
		}
		readB = workers[0].getReadB();
	}

	protected DataRow[] doJoinNext(int size) throws IOException, RJException {
		List joinResult = new ArrayList();
		DataRow result;
		while (((result = joinNext()) != null) && joinResult.size() < size) {
			joinResult.add(result);
		}
		return (DataRow[]) joinResult.toArray(new DataRow[] {});
	}

	protected void doClose() throws IOException, RJException {
		Log.log(NestedLoopJoin.class, "Join operator closing", 1);
		if (!closed) {
			getSourceA().close();
			getSourceB().close();
			closed = true;
		}
		if (workers != null) {
			for (int i = 0; i < workers.length; i++) {
				workers[i].stopProcessing();
			}
			workers = null;
		}
		buffer = null;
	}

	public String toString() {
		return "NestedLoopJoin";
	}

	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new NLJGUIVisibleComponent();
	}

	protected void doReset(boolean deep) throws IOException, RJException {
		getSourceA().reset();
		getSourceB().reset();
		if (workers != null) {
			for (int i = 0; i < workers.length; i++) {
				workers[i].stopProcessing();
			}
			workers = null;
		}
		readA = 0;
		readB = 0;
		createWorkersIfNeeded();
	}
	
	private void createWorkersIfNeeded() throws IOException, RJException {
		if (workers == null) {
			workers = new NLJThread[CPUInfo.testNumberOfCPUs()];
			for (int i = 0; i < workers.length; i++) {
				workers[i] = new NLJThread(getSourceA(), getSourceB().copy(), buffer, connector);
				workers[i].start();
			}
		}
	}

	protected void finalize() throws Throwable {
		//System.out.println(getClass() + " finalize");
		//close();
	}
	
	public String toHTMLString() {
		StringBuilder builder = new StringBuilder();
		builder.append(HTMLUtils.getHTMLHeader());
		builder.append(HTMLUtils.encodeTable(new String[][] {
				{"Search method:", "Nested loop join (NLJ)"}, 
			}));
		builder.append("Attributes mapping and distance function selection:<br>");
		builder.append(HTMLUtils.encodeJoinCondition(getJoinCondition()));
		builder.append("</html>");
		return builder.toString();
	}
	
	private void calculateProgress() {
		try {
			int progress = (int) Math.round(getSourceA().position() / (double)getSourceA().size() * 100);
			setProgress(progress);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean isProgressSupported() {
		return true;
	}
	
	public LinkageSummary getLinkageSummary() {
		return new LinkageSummary(readA, readB, getLinkedCnt());
	}
	
}
