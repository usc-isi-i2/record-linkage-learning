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
import java.util.HashMap;
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
import cdc.impl.datasource.wrappers.ExternallySortingDataSource;
import cdc.utils.CompareFunctionInterface;
import cdc.utils.HTMLUtils;
import cdc.utils.Log;
import cdc.utils.PrintUtils;
import cdc.utils.RJException;

public class SNMJoin_v1 extends AbstractJoin {

	public class SNMJoinConnector {

		public boolean isCancelled() {
			return SNMJoin_v1.this.isCancelled();
		}

		public AbstractJoinCondition getJoinCondition() {
			return SNMJoin_v1.this.getJoinCondition();
		}

		public boolean isAnyJoinListenerRegistered() {
			return SNMJoin_v1.this.isAnyJoinListenerRegistered();
		}

		public void notifyNotJoined(DataRow rowA, DataRow rowB, int conf) throws RJException {
			SNMJoin_v1.this.notifyNotJoined(rowA, rowB, conf);
		}

		public void notifyJoined(DataRow rowA, DataRow rowB, DataRow row) throws RJException {
			SNMJoin_v1.this.notifyJoined(rowA, rowB, row);
		}

		public DataColumnDefinition[] getOutColumns() {
			return SNMJoin_v1.this.getOutColumns();
		}

		public void notifyTrashingJoined(DataRow disposed) throws RJException {
			SNMJoin_v1.this.notifyTrashingJoined(disposed);
		}

		public void notifyTrashingNotJoined(DataRow disposed) throws RJException {
			synchronized (mutex) {
				if (disposed.getSourceName().equals(getSourceA().getSourceName())) {
					minusA++;
				} else {
					minusB++;
				}
			}
			SNMJoin_v1.this.notifyTrashingNotJoined(disposed);
		}

		public int getWindow() {
			return SNMJoin_v1.this.window;
		}

		public DataColumnDefinition[] getRightOrder() {
			return rightOrder;
		}

		public DataColumnDefinition[] getLeftOrder() {
			return leftOrder;
		}

		public CompareFunctionInterface[] getCompareFunctions() {
			return functions;
		}

		public AbstractJoin getJoin() {
			return SNMJoin_v1.this;
		}

	}

	private Object mutex = new Object();
	
	public static final int DEFAULT_WINDOW_SIZE = 8;
	public static final String PARAM_WINDOW_SIZE = "window";
	public static final String PARAM_SORT_ORDER_B = "sort-order-left";
	public static final String PARAM_SORT_ORDER_A = "sort-order-right";
	
	private int window = DEFAULT_WINDOW_SIZE;

	private boolean closed = false;
	
	private ArrayBlockingQueue buffer = new ArrayBlockingQueue(100);
	private SNMJoinConnector connector = new SNMJoinConnector();
	private JoiningThread[] workers = null;

	private DataColumnDefinition[] leftOrder;
	private DataColumnDefinition[] rightOrder;
	private CompareFunctionInterface[] functions;
	
	private int minusA = 0;
	private int minusB = 0;
	
	private int readA;
	private int readB;
	
	public SNMJoin_v1(AbstractDataSource sourceA, AbstractDataSource sourceB, DataColumnDefinition outFormat[], AbstractJoinCondition condition, Map params) throws IOException, RJException {
		super(fixSource(sourceA, parseOrder((String)params.get(PARAM_SORT_ORDER_A), sourceA, condition.getLeftJoinColumns()), condition.getCompareFunctions(parseOrder((String)params.get(PARAM_SORT_ORDER_A), sourceA, condition.getLeftJoinColumns()), parseOrder((String)params.get(PARAM_SORT_ORDER_A), sourceA, condition.getLeftJoinColumns()))), 
				fixSource(sourceB, parseOrder((String)params.get(PARAM_SORT_ORDER_B), sourceB, condition.getRightJoinColumns()), condition.getCompareFunctions(parseOrder((String)params.get(PARAM_SORT_ORDER_B), sourceB, condition.getLeftJoinColumns()), parseOrder((String)params.get(PARAM_SORT_ORDER_B), sourceB, condition.getLeftJoinColumns()))), condition, outFormat, params);
		if (params.get(PARAM_WINDOW_SIZE) != null) {
			this.window = Integer.parseInt((String) params.get(PARAM_WINDOW_SIZE));
		}
		leftOrder = parseOrder((String)params.get(PARAM_SORT_ORDER_A), sourceA, condition.getLeftJoinColumns());
		rightOrder = parseOrder((String)params.get(PARAM_SORT_ORDER_B), sourceB, condition.getRightJoinColumns());
		functions = condition.getCompareFunctions(leftOrder, rightOrder);
		Log.log(getClass(), "Functions used for comparisons: " + PrintUtils.printArray(functions), 1);
		Log.log(getClass(), "SNM join created, window size = " + window, 1);
	}

	private static DataColumnDefinition[] parseOrder(String string, AbstractDataSource source, DataColumnDefinition[] defaultOrder) throws RJException {
		if (string == null) {
			System.out.println("Warning: Using order from join condition...");
			return defaultOrder;
		} else {
			String[] cols = string.split(",");
			DataColumnDefinition[] order = new DataColumnDefinition[cols.length];
			for (int i = 0; i < order.length; i++) {
				order[i] = source.getDataModel().getColumnByName(cols[i]);
				if (order[i] == null) {
					return null;
				}
			}
			return order;
		}
	}

	private static AbstractDataSource fixSource(AbstractDataSource source, DataColumnDefinition[] order, CompareFunctionInterface[] functions) throws IOException, RJException {
		if (source.canSort()) {
			source.setOrderBy(order);
			return source;
		} else {
			ExternallySortingDataSource sorter = new ExternallySortingDataSource(source.getSourceName(), source, order, functions, new HashMap());
			return sorter;
		}
	}

	protected DataRow doJoinNext() throws IOException, RJException {
		
		if (closed) {
			return null;
		}
		
		createWorkersIfNeeded();
		
		
		try {
			main: while (true) {
				calculateProgress();
				DataRow row = (DataRow) buffer.poll(100, TimeUnit.MILLISECONDS);
				if (row != null) {
					return row;
				} else {
					if (isCancelled()) {
						for (int i = 0; i < workers.length; i++) {
							if (workers[i] != null) {
								workers[i].interrupt();
							}
						}
						endSequence();
						return null;
					}
					for (int i = 0; i < workers.length; i++) {
						if (workers[i].checkError() != null) {
							setCancelled(true);
							throw workers[i].checkError();
						}
						if (!workers[i].isFinished()) {
							continue main;
						}
					}
					endSequence();
					return null;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		endSequence();
		return null;
	}

	private void endSequence() {
		Log.log(getClass(), "Linkage finished", 1);
		for (int i = 0; i < workers.length; i++) {
			readA += workers[i].getReadA();
			readB += workers[i].getReadB();
		}
	}

	private void createWorkersIfNeeded() throws IOException, RJException {
		if (workers == null) {
			//getSourceA().reset();
			//getSourceB().reset();
			
			//int numberOfCPU = CPUInfo.testNumberOfCPUs();
			
			//TODO Need to fix the bug with workload division
			int numberOfCPU = 1;
			//Log.log(getClass(), "Warning: ", 1);
			workers = new JoiningThread[numberOfCPU];
			Log.log(getClass(), "Left data source size: " + getSourceA().size(), 1);
			Log.log(getClass(), "Right data source size: " + getSourceB().size(), 1);
			if (workers.length == 1) {
				workers[0] = new JoiningThread(getSourceA().copy(), getSourceB().copy(), buffer, connector);
				workers[0].setName("SNM-joiner");
				workers[0].start();
			} else {
				long[][] workDiv = divideWork(workers.length);
				for (int i = 0; i < workers.length && !isCancelled(); i++) {
					workers[i] = new JoiningThread(i, getSourceA().copy(), getSourceB().copy(), workDiv[0], workDiv[1][i], buffer, connector);
					workers[i].setName("SNM-joiner-" + i);
					workers[i].start();
				}
			}	
		}
	}

	private long[][] divideWork(int cpuNum) throws IOException, RJException {
		long dataSize = getSourceA().size();
		long chunkSize = (long) (dataSize / (double)cpuNum);
		if (chunkSize == 0) chunkSize = 1;
		
		long[][] div = new long[2][cpuNum];
		long start = 1;
		for (int i = 0; i < cpuNum - 1; i++) {
			div[0][i] = start;
			div[1][i] = chunkSize;
			start += chunkSize;
		}
		
		//last cpu gets all what is left
		div[0][cpuNum - 1] = start;
		div[1][cpuNum - 1] = dataSize - start + 1;
		
		return div;
	}

	private void calculateProgress() {
		int progress = 0;
		if (workers != null) {
			for (int i = 0; i < workers.length; i++) {
				if (workers[i] != null) {
					progress += workers[i].getProgress();
				}
			}
			progress /= (double)workers.length;
		}
		setProgress(progress);
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
		if (!closed) {
			getSourceA().close();
			getSourceB().close();
			closed = true;
			if (workers != null) {
				for (int i = 0; i < workers.length; i++) {
					if (workers[i] != null) {
						workers[i].stopProcessing();
					}
				}
			}
		}
	}

	public String toString() {
		return "SNMJoin(window size " + window + ")";
	}

	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new SNMGUIVisibleComponent();
	}
	
//	protected void finalize() throws Throwable {
//		System.out.println(getClass() + " finalize");
//		//close();
//		super.finalize();
//	}
	
	public boolean newSourceA(AbstractDataSource source) throws IOException, RJException {
		DataColumnDefinition[] order = parseOrder(getProperty(PARAM_SORT_ORDER_A), source, getJoinCondition().getLeftJoinColumns());
		if (order == null) {
			return false;
		}
		return super.newSourceA(fixSource(source, order, getJoinCondition().getCompareFunctions(order, order)));
	}

	public boolean newSourceB(AbstractDataSource source) throws IOException, RJException {
		DataColumnDefinition[] order = parseOrder(getProperty(PARAM_SORT_ORDER_B), source, getJoinCondition().getRightJoinColumns());
		if (order == null) {
			return false;
		}
		return super.newSourceB(fixSource(source, order, getJoinCondition().getCompareFunctions(order, order)));
	}
	
	public String toHTMLString() {
		StringBuilder builder = new StringBuilder();
		builder.append(HTMLUtils.getHTMLHeader());
		builder.append(HTMLUtils.encodeTable(new String[][] {
				{"Search method:", "Sorted neighborhood method (SNM)"}, 
				{"Window size:", String.valueOf(window)},
				{"Sort order:", "Indicated by order of attributes in table below."}
			}));
		builder.append("Attributes mapping and distance function selection:<br>");
		builder.append(HTMLUtils.encodeJoinCondition(getJoinCondition()));
		builder.append("</html>");
		return builder.toString();
	}
	
	public boolean isProgressSupported() {
		return true;
	}
	
	public boolean isConfigurationProgressSupported() {
		return true;
	}
	
	public int getConfigurationProgress() {
		try {
			long s1 = 2;
			long s2 = 2;
			if (getSourceA() instanceof ExternallySortingDataSource) {
				s1 = ((ExternallySortingDataSource)getSourceA()).getRawDataSource().size();
			} else {
				s1 = getSourceA().size();
			}
			if (getSourceB() instanceof ExternallySortingDataSource) {
				s2 = ((ExternallySortingDataSource)getSourceB()).getRawDataSource().size();
			} else {
				s2 = getSourceB().size();
			}
			//long s1 = getSourceA().size();
			//long s2 = getSourceB().size();
			double share = s1 * Math.log(s1) / (s2 * Math.log(s2) + s1 * Math.log(s1));
			super.setConfigurationProgress((int) (getSourceA().getConfigurationProgress() * share + getSourceB().getConfigurationProgress() * (1-share)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return super.getConfigurationProgress();
	}

	protected void doReset(boolean deep) throws IOException, RJException {
		if (workers != null) {
			for (int i = 0; i < workers.length; i++) {
				if (workers[i] != null) {
					workers[i].stopProcessing();
				}
			}
			workers = null;
		}
		buffer = new ArrayBlockingQueue(100);
		//createWorkersIfNeeded();
		
		minusA = 0;
		minusB = 0;
		readA = 0;
		readB = 0;
	}
	
	public LinkageSummary getLinkageSummary() {
		return new LinkageSummary(readA, readB, getLinkedCnt());
	}
}
