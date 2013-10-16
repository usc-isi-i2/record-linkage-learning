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


package cdc.components;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cdc.components.ManualDecisionModule.ManualDecision;
import cdc.configuration.Configuration;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.gui.MainFrame;
import cdc.gui.components.statistics.JoinStatisticalData;
import cdc.impl.join.common.DataSourceNotJoinedJoinListener;
import cdc.utils.RJException;
import cdc.utils.RowUtils;
import edu.emory.mathcs.util.xml.DOMUtils;

public abstract class AbstractJoin extends SystemComponent {
	
	public static final String PROPERTY_CONFIDNCE = "join-confidence";
	public static final String PROPERTY_SRCA_ID = "id-a";
	public static final String PROPERTY_SRCB_ID = "id-b";
	public static final String PROPERTY_JOINED = "was-joined";
	public static final String PROPERTY_MANUAL_REVIEW = "manual";
	
	public static final String PROPERTY_RECORD_SRCA = "rec-a";
	public static final String PROPERTY_RECORD_SRCB = "rec-b";
	
	public static final int PROPERTY_MANUAL_REVIEW_CNT = 0;
	public static final int PROPERTY_JOIN_MULTIPLICITY = 1;
	
	
	private AbstractDataSource sourceA;
	private AbstractDataSource sourceB;
	private DataColumnDefinition[] outColumns;
	private AbstractJoinCondition joinCondition;
	private boolean done = false;
	
	private ManualDecisionModule manualDecisionModule = new ManualDecisionModule();
	
	private int linked = 0;
	
	private Object mutex = new Object();
	
	private List listeners = null;
	
	private boolean summaryLeft = false;
	private boolean summaryRight = false;
	private volatile AtomicBoolean cancel = new AtomicBoolean(false);
	private volatile AtomicInteger progress = new AtomicInteger(0);
	private volatile AtomicInteger progressConfig = new AtomicInteger(0);
	private JoinStatisticalData statsListener;
	
	private List manualDecisions = new ArrayList();
	
	private CountCache[] caches = {new CountCache(), new CountCache()};

	public AbstractJoin(AbstractDataSource sourceA, AbstractDataSource sourceB, AbstractJoinCondition condition, DataColumnDefinition[] outColumns, Map params) throws RJException {
		super(params);
		this.sourceA = sourceA;
		this.sourceB = sourceB;
		this.outColumns = outColumns;
		this.joinCondition = condition;
		//this.addJoinListener(statsListener = new JoinStatisticalData());
	}

	public AbstractDataSource getSourceA() {
		return sourceA;
	}

	public AbstractDataSource getSourceB() {
		return sourceB;
	}
	
	public AbstractDataSource getSource(String sourceName) {
		if (sourceA.getSourceName().equals(sourceName)) {
			return sourceA;
		} else if (sourceB.getSourceName().equals(sourceName)) {
			return sourceB;
		} else {
			return null;
		}
	}

	public void setOutColumns(DataColumnDefinition[] outColumns) {
		this.outColumns = outColumns;
	}
	
	public DataColumnDefinition[] getOutColumns() {
		return outColumns;
	}
	
	public AbstractJoinCondition getJoinCondition() {
		return joinCondition;
	}
	
	public void setJoinCondition(AbstractJoinCondition condition) {
		joinCondition = condition;
	}
	
	public void enableSummaryForLeftSource(String filePrefix) throws RJException {
		addJoinListener(new DataSourceNotJoinedJoinListener(filePrefix, getSourceA()));
		summaryLeft = true;
	}
	
	public void enableSummaryForRightSource(String filePrefix) throws RJException {
		//System.out.println("Generic Enable on " + this.hashCode());
		addJoinListener(new DataSourceNotJoinedJoinListener(filePrefix, getSourceB()));
		summaryRight = true;
	}
	
	public boolean isSummaryForLeftSourceEnabled() {
		return summaryLeft;
	}
	
	public boolean isSummaryForRightSourceEnabled() {
		return summaryRight;
	}
	
	public void saveToXML(Document doc, Element node) {
		DOMUtils.setAttribute(node, "summary-left", String.valueOf(summaryLeft));
		DOMUtils.setAttribute(node, "summary-right", String.valueOf(summaryRight));
		Configuration.appendParams(doc, node, getProperties());
		Element conditions = DOMUtils.createChildElement(doc, node, Configuration.JOIN_CONDITION_TAG);
		DOMUtils.setAttribute(conditions, Configuration.CLASS_ATTR, joinCondition.getClass().getName());
		joinCondition.saveToXML(doc, conditions);
		Element cols = DOMUtils.createChildElement(doc, node, Configuration.ROW_MODEL_TAG);
		for (int i = 0; i < outColumns.length; i++) {
			Element col = DOMUtils.createChildElement(doc, cols, Configuration.ROW_TAG);
			DOMUtils.setAttribute(col, Configuration.COLUMN_DATASOURCE_ATTR, outColumns[i].getSourceName());
			DOMUtils.setAttribute(col, Configuration.NAME_ATTR, outColumns[i].getColumnName());	
		}
	}
	
	public static AbstractJoin fromXML(AbstractDataSource leftSource, AbstractDataSource rightSource, Element node) throws RJException {
		String className = DOMUtils.getAttribute(node, Configuration.CLASS_ATTR);
		boolean leftSummary = new Boolean(DOMUtils.getAttribute(node, "summary-left")).booleanValue();
		boolean rightSummary = new Boolean(DOMUtils.getAttribute(node, "summary-right")).booleanValue();
		Element joinConditionElement = DOMUtils.getChildElement(node, Configuration.JOIN_CONDITION_TAG);
		if (joinConditionElement == null) {
			throw new RJException("Join has to contain configuration of join condition.");
		}
		AbstractJoinCondition cond = Configuration.readConditionConfiguration(leftSource, rightSource, joinConditionElement);
		Map params = new HashMap();
		Element paramsElement = DOMUtils.getChildElement(node, Configuration.PARAMS_TAG);
		if (paramsElement != null) {
			params = Configuration.parseParams(paramsElement);
		}
		
		Element rowModelConfig = DOMUtils.getChildElement(node, Configuration.ROW_MODEL_TAG);
		if (rowModelConfig == null) {
			throw new RJException("Join has to contain configuration of output row model (tag " + Configuration.ROW_MODEL_TAG + ").");
		}
		DataColumnDefinition[] rowModel = Configuration.readRowModelConfiguration(rowModelConfig, 
				new AbstractDataSource[] {leftSource, rightSource});
		
		try {
			Class clazz = Class.forName(className);
			Constructor c = clazz.getConstructor(new Class[] {AbstractDataSource.class, AbstractDataSource.class, DataColumnDefinition[].class, AbstractJoinCondition.class, Map.class});
			AbstractJoin join = (AbstractJoin) c.newInstance(new Object[] {leftSource.getPreprocessedDataSource(), rightSource.getPreprocessedDataSource(), rowModel, cond, params});
			if (leftSummary) {
				join.enableSummaryForLeftSource("");
			}
			if (rightSummary) {
				join.enableSummaryForRightSource("");
			}
			return join;
		} catch (Exception e) {
			throw new RJException("Error reading join configuration", e);
		}
	}
	
	public void addJoinListener(JoinListener listener) throws RJException {
		//System.out.println("Adding: " + listener);
		synchronized (mutex) {
			if (listeners == null) {
				listeners = new ArrayList();
			}
			if (!listeners.contains(listener)) {
				listeners.add(listener);
			}
			listener.joinConfigured();
		}
	}
	
	public void notifyJoined(DataRow rowA, DataRow rowB, DataRow joinedRow) throws RJException {
		synchronized (mutex) {
			if (listeners != null) {
				for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
					JoinListener listener = (JoinListener) iterator.next();
					listener.rowsJoined(rowA, rowB, joinedRow, joinCondition);
				}
			}
		}
	}
	
	public void notifyNotJoined(DataRow rowA, DataRow rowB, int confidence) throws RJException {
		synchronized (mutex) {
			if (listeners != null) {
				for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
					JoinListener listener = (JoinListener) iterator.next();
					listener.rowsNotJoined(rowA, rowB, confidence, joinCondition);
				}
			}
		}
	}
	
	public void notifyTrashingJoined(DataRow row) throws RJException {
		synchronized (mutex) {
			if (listeners != null) {
				for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
					JoinListener listener = (JoinListener) iterator.next();
					listener.trashingJoinedTuple(row);
				}
			}
		}
	}
	
	public void notifyTrashingNotJoined(DataRow row) throws RJException {
		synchronized (mutex) {
			if (listeners != null) {
				for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
					//System.out.println("Listeners: " + listeners);
					JoinListener listener = (JoinListener) iterator.next();
					listener.trashingNotJoinedTuple(row);
				}
			}
		}
	}
	
	public void removeJoinListener(JoinListener listener) throws RJException {
		synchronized (mutex) {
			if (listeners != null) {
				listeners.remove(listener);
				listener.close();
				if (listeners.isEmpty()) {
					listeners = null;
				}
			}
		}
	}
	
	public List getListeners() {
		synchronized (listeners) {
			return new ArrayList(listeners);
		}
	}
	
	protected boolean isAnyJoinListenerRegistered() {
		synchronized (mutex) {
			return listeners != null;
		}
	}
	
	public CountCache getCache(int cacheID) {
		return caches[cacheID];
	}
	
	public void close() throws IOException, RJException {
		doClose();
		closeListeners();
		resetCache();
	}
	
	public void closeListeners() throws RJException {
		synchronized(mutex) {
			if (listeners != null) {
				for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
					JoinListener l = (JoinListener) iterator.next();
					l.close();
				}
				//listeners.clear();
			}
		}
	}
	
	public void reset(boolean deep) throws IOException, RJException {
		if (statsListener != null) {
			removeJoinListener(statsListener); 
			statsListener = null;
		};
		//addJoinListener(statsListener = new JoinStatisticalData());
		synchronized(mutex) {
			if (listeners != null) {
				for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
					JoinListener l = (JoinListener) iterator.next();
					l.reset();
				}
			}
		}
		progress.set(0);
		progressConfig.set(0);
		done = false;
		linked = 0;
		manualDecisions.clear();
		doReset(deep);
		enableJoinStatistics();
		resetCache();
	}
	
	private void resetCache() {
		for (int i = 0; i < caches.length; i++) {
			caches[i].reset();
		}
	}

	public void reset() throws IOException, RJException {
		reset(false);
	}
	
	public void setCancelled(boolean cancel) {
		this.cancel.set(cancel);
		AbstractDataSource.requestStop(cancel);
	}
	
	public boolean isCancelled() {
		return cancel.get();
	}
	
	public DataRow joinNext() throws IOException, RJException {
		while (true) {
			while (!done) {
				DataRow row = doJoinNext();
				if (MainFrame.main != null && row != null && row.getProperty(PROPERTY_MANUAL_REVIEW) != null) {
					manualDecisionModule.addRow(row);
				} else if (row != null) {
					linked++;
					return row;
				} else {
					done = true;
				}
			} 
					
			while (true) {
				ManualDecision decision = manualDecisionModule.getNextDecidedRow();
				if (decision != null) {
					manualDecisions.add(decision);
					if (decision.isAccepted()) {
						linked++;
						return decision.getRow();
					}
				} else {
					break;
				}
			}
			AbstractDataSource.requestStop(false);
			for (Iterator iterator = manualDecisions.iterator(); iterator.hasNext();) {
				ManualDecision decision = (ManualDecision) iterator.next();
				if (decision.isAccepted()) {
					RowUtils.linkageManuallyAccepted(this, decision.getRow());
				} else {
					RowUtils.linkageManuallyRejected(this, decision.getRow());
				}
				dealWithManualDecision(decision);
			}
			return null;
		}
	}
	
	private void dealWithManualDecision(ManualDecision decision) throws RJException {
		DataRow row = decision.getRow();
		DataRow rowA = (DataRow) row.getObjectProperty(PROPERTY_RECORD_SRCA);
		DataRow rowB = (DataRow) row.getObjectProperty(PROPERTY_RECORD_SRCB);
		if (!decision.isAccepted()) {
			if (RowUtils.shouldReportTrashingNotJoinedAfterManualReview(this, rowA)) {
				notifyTrashingNotJoined(rowA);
			}
			if (RowUtils.shouldReportTrashingNotJoinedAfterManualReview(this, rowB)) {
				notifyTrashingNotJoined(rowB);
			}
		}
	}
	
	protected abstract void doClose() throws IOException, RJException;
	protected abstract void doReset(boolean deep) throws IOException, RJException;
	protected abstract DataRow[] doJoinNext(int size) throws IOException, RJException;
	protected abstract DataRow doJoinNext() throws IOException, RJException;
	
	public Object getEffectiveJoinClass() {
		return getClass();
	}

	public boolean newSourceA(AbstractDataSource source) throws IOException, RJException {
		List columns = Arrays.asList(source.getDataModel().getOutputFormat());
		for (int i = 0; i < joinCondition.getLeftJoinColumns().length; i++) {
			if (!columns.contains(joinCondition.getLeftJoinColumns()[i])) {
				return false;
			}
		}
		sourceA.close();
		sourceA = source;
		reset(true);
		return true;
	}

	public boolean newSourceB(AbstractDataSource source) throws IOException, RJException {
		List columns = Arrays.asList(source.getDataModel().getOutputFormat());
		for (int i = 0; i < joinCondition.getRightJoinColumns().length; i++) {
			if (!columns.contains(joinCondition.getRightJoinColumns()[i])) {
				return false;
			}
		}
		sourceB.close();
		sourceB = source;
		reset(true);
		return true;
	}
	
	protected void setProgress(int progress) {
		this.progress.set(progress);
	}
	
	public int getProgress() {
		return progress.intValue();
	}
	
	protected void setConfigurationProgress(int progress) {
		this.progressConfig.set(progress);
	}
	
	public int getConfigurationProgress() {
		return progressConfig.intValue();
	}
	
	public boolean isProgressSupported() {
		return false;
	}
	
	public boolean isConfigurationProgressSupported() {
		return false;
	}

	public JoinStatisticalData getJoinStatisticsListener() {
		return statsListener;
	}
	
	public JoinStatisticalData enableJoinStatistics() throws RJException {
		if (statsListener != null) {
			return statsListener;
		}
		statsListener = new JoinStatisticalData();
		addJoinListener(statsListener);
		return statsListener;
	}

	public void removeAllJoinListeners() {
		synchronized (mutex) {
			listeners.clear();
		}
	}
	
	public int getLinkedCnt() {
		return linked;
	}
	
	public abstract LinkageSummary getLinkageSummary();
	
}
