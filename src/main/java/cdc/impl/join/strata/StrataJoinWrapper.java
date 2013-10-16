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


package cdc.impl.join.strata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.components.AbstractJoinCondition;
import cdc.components.AtomicCondition;
import cdc.components.CountCache;
import cdc.components.JoinListener;
import cdc.components.LinkageSummary;
import cdc.configuration.Configuration;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.impl.join.common.DataSourceNotJoinedJoinListener;
import cdc.utils.RJException;
import edu.emory.mathcs.util.xml.DOMUtils;

public class StrataJoinWrapper extends AbstractJoin {
	
	public static final String PROPERTY_STRATUM_NAME = "stratum-name";
	private AbstractJoin[] optimizedJoins;
	private AbstractJoin[] joins;
	private DataStratum[] strata;
	private AtomicInteger activeJoin = new AtomicInteger(0);
	
	private boolean sameJoinConfigs = false;
	//private JoinListener listener;
	private boolean[] set = new boolean[0];
	
	private LinkageSummary dataSourceSummary = null;
	
	private boolean enabledLeft = false;
	private boolean enabledRight = false;
	
	private int linked;
	
	public StrataJoinWrapper(DataStratum[] strata, AbstractJoin[] joins) throws IOException, RJException {
		super(joins[0].getSourceA(), joins[0].getSourceB(), null, joins[0].getOutColumns(), null);
		this.strata = strata;
		this.optimizedJoins = this.joins = joins;
		for (int i = 0; i < joins.length; i++) {
			StrataJoinCondition condition = new StrataJoinCondition(null);
			condition.addStrata(strata[i], joins[i].getJoinCondition());
			joins[i].setJoinCondition(condition);
		}
		sameJoinConfigs = false;
		set = new boolean[joins.length];
		setFilter(activeJoin.get());
	}

	public StrataJoinWrapper(AbstractJoin join) throws IOException, RJException {
		super(join.getSourceA(), join.getSourceB(), null, join.getOutColumns(), null);
		sameJoinConfigs = true;
		optimizedJoins = new AbstractJoin[] {join};
		set = new boolean[1];
		setFilter(activeJoin.get());
	}

	private StrataJoinWrapper(AbstractJoin[] joins) throws IOException, RJException {
		super(joins[0].getSourceA(), joins[0].getSourceB(), null, joins[0].getOutColumns(), null);
		this.optimizedJoins = this.joins = joins;
		sameJoinConfigs = false;
		strata = new DataStratum[joins.length];
		for (int i = 0; i < joins.length; i++) {
			strata[i] = ((StrataJoinCondition)joins[i].getJoinCondition()).getStrata()[0];
			joins[i].removeAllJoinListeners();
			joins[i].setJoinCondition(((StrataJoinCondition)joins[i].getJoinCondition()).getJoinConditions()[0]);
		}
		set = new boolean[joins.length];
		setFilter(activeJoin.get());
	}

	public AbstractJoinCondition getJoinCondition() {
		AbstractJoinCondition[] subconds = getJoinConditions();
		DataStratum[] strata = getStrata();
		StrataJoinCondition cond = new StrataJoinCondition(null);
		for (int i = 0; i < strata.length; i++) {
			cond.addStrata(strata[i], subconds[i]);
		}
		return cond;
	}
	
	public AbstractJoinCondition[] getJoinConditions() {
		if (sameJoinConfigs) {
			StrataJoinCondition strataCond = (StrataJoinCondition) optimizedJoins[0].getJoinCondition();
			return strataCond.getJoinConditions();
		} else {
			AbstractJoinCondition[] conds = new AbstractJoinCondition[joins.length];
			for (int i = 0; i < conds.length; i++) {
				conds[i] = joins[i].getJoinCondition();
				if (conds[i] instanceof StrataJoinCondition) {
					conds[i] = ((StrataJoinCondition)conds[i]).getJoinConditions()[0];
				}
			}
			return conds;
		}
	}
	
	public DataStratum[] getStrata() {
		if (sameJoinConfigs) {
			StrataJoinCondition strataCond = (StrataJoinCondition) optimizedJoins[0].getJoinCondition();
			return strataCond.getStrata();
		} else {
			return strata;
		}
	}
	
	public String[] getStrataNames() {
		if (sameJoinConfigs) {
			StrataJoinCondition strataCond = (StrataJoinCondition) optimizedJoins[0].getJoinCondition();
			String[] strs = new String[strataCond.getStrata().length];
			for (int i = 0; i < strs.length; i++) {
				strs[i] = strataCond.getStrata()[i].getName();
			}
			return strs;
		} else {
			String[] strata = new String[this.strata.length];
			for (int i = 0; i < strata.length; i++) {
				strata[i] = this.strata[i].getName();
			}
			return strata;
		}
	}

	protected void doClose() throws IOException, RJException {
		synchronized (this) {
			if (optimizedJoins != null) {
				for (int i = 0; i < optimizedJoins.length; i++) {
					if (optimizedJoins[i] != null) {
						optimizedJoins[i].close();
					}
				}
				
				optimizedJoins = null;
				joins = null;
				//listener = null;
			}
		}
	}

	protected void doReset(boolean deep) throws IOException, RJException {
		synchronized (this) {
			if (optimizedJoins != null) {
				for (int i = 0; i < optimizedJoins.length; i++) {
					if (optimizedJoins[i] != null) {
						optimizedJoins[i].reset(deep);
					}
				}
				//listener = null;
				activeJoin.set(0);
				set = new boolean[optimizedJoins.length];
				setFilter(activeJoin.get());
			}
		}
		linked = 0;
	}

	public DataRow doJoinNext() throws IOException, RJException {
		DataRow next = null;
		if (optimizedJoins != null) {
			while ((next = optimizedJoins[activeJoin.get()].joinNext()) == null) {
				this.dataSourceSummary = optimizedJoins[activeJoin.get()].getLinkageSummary();
				if (isCancelled()) {
					return null;
				}
				optimizedJoins[activeJoin.get()].getSourceA().reset();
				optimizedJoins[activeJoin.get()].getSourceB().reset();
				//optimizedJoins[activeJoin.get()].closeListeners();
				synchronized (set) {
					activeJoin.incrementAndGet();
				}
				if (activeJoin.get() == optimizedJoins.length) {
					return null;
				} else {
					setFilter(activeJoin.get());
				}
			}
		}
		if (next != null) {
			linked++;
		}
		return next;
	}

	private void setFilter(int joinId) throws IOException, RJException {
		AtomicCondition[] condsA;
		AtomicCondition[] condsB;
		if (sameJoinConfigs) {
			List condsAList = new ArrayList();
			List condsBList = new ArrayList();
			StrataJoinCondition cond = (StrataJoinCondition) optimizedJoins[joinId].getJoinCondition();
			for (int i = 0; i < cond.getJoinConditions().length; i++) {
				DataStratum stratum = cond.getStrata()[i];
				for (int j = 0; j < stratum.getSourceA().length; j++) {
					if (!condsAList.contains(stratum.getSourceA()[j])) {
						condsAList.add(stratum.getSourceA()[j]);
					}
				}
				for (int j = 0; j < stratum.getSourceB().length; j++) {
					if (!condsBList.contains(stratum.getSourceB()[j])) {
						condsBList.add(stratum.getSourceB()[j]);
					}
				}
			}
			condsA = (AtomicCondition[]) condsAList.toArray(new AtomicCondition[] {});
			condsB = (AtomicCondition[]) condsBList.toArray(new AtomicCondition[] {});
		} else {
			DataStratum stratum = getStrata()[joinId];
			condsA = stratum.getSourceA();
			condsB = stratum.getSourceB();
		}
		this.optimizedJoins[joinId].getSourceA().setStratumCondition(condsA);
		this.optimizedJoins[joinId].getSourceB().setStratumCondition(condsB);
		//this.optimizedJoins[joinId].reset();
//		synchronized (set) {
//			if (listener != null && !set[joinId]) {
//				this.optimizedJoins[joinId].addJoinListener(listener);
//			}
//		}
	}

	protected DataRow[] doJoinNext(int size) throws IOException, RJException {
		throw new RuntimeException("Not implemented");
	}

	public AbstractJoin[] getJoins() {
		if (sameJoinConfigs) {
			return optimizedJoins;
		} else {
			return joins;
		}
	}
	
	public Object getEffectiveJoinClass() {
		if (optimizedJoins != null) {
			if (!sameJoinConfigs) {
				return joins[0].getEffectiveJoinClass();
			} else {
				return optimizedJoins[0].getEffectiveJoinClass();
			}
		}
		return null;
	}

	public boolean isSameJoinConfigs() {
		return sameJoinConfigs;
	}
	
	public CountCache getCache(int cacheID) {
		int active = activeJoin.get();
		//This is really ugly hack, but strata join wrapper is not supprted any more...
		if (active >= optimizedJoins.length) {
			return optimizedJoins[optimizedJoins.length - 1].getCache(cacheID);
		}
		return optimizedJoins[activeJoin.get()].getCache(cacheID);
	}

	public void saveToXML(Document doc, Element node) {
		DOMUtils.setAttribute(node, "summary-left", String.valueOf(isSummaryForLeftSourceEnabled()));
		DOMUtils.setAttribute(node, "summary-right", String.valueOf(isSummaryForRightSourceEnabled()));
		DOMUtils.setAttribute(node, "uniform-config", String.valueOf(sameJoinConfigs));
		Configuration.appendParams(doc, node, getProperties());
		Element joins = DOMUtils.createChildElement(doc, node, "joins");
		if (sameJoinConfigs) {
			for (int i = 0; i < this.optimizedJoins.length; i++) {
				Element col = DOMUtils.createChildElement(doc, joins, Configuration.JOIN_TAG);
				DOMUtils.setAttribute(col, Configuration.CLASS_ATTR, optimizedJoins[i].getClass().getName());
				this.optimizedJoins[i].saveToXML(doc, col);
			}
		} else {
			for (int i = 0; i < this.joins.length; i++) {
				Element col = DOMUtils.createChildElement(doc, joins, Configuration.JOIN_TAG);
				DOMUtils.setAttribute(col, Configuration.CLASS_ATTR, this.joins[i].getClass().getName());
				this.joins[i].saveToXML(doc, col);
			}
		}
	}
	
	public void enableSummaryForLeftSource(String filePrefix) throws RJException {
		if (sameJoinConfigs) {
			optimizedJoins[0].enableSummaryForLeftSource("");
			addAllMinusListeners(optimizedJoins[0].getListeners(), optimizedJoins[0].getJoinCondition().getLeftJoinColumns()[0].getSourceName());
		} else {
			DataStratum[] strata = getStrata();
			for (int i = 0; i < optimizedJoins.length; i++) {
				optimizedJoins[i].enableSummaryForLeftSource("[" + strata[i].getName() + "]");
				addAllMinusListeners(optimizedJoins[i].getListeners(), optimizedJoins[i].getJoinCondition().getLeftJoinColumns()[0].getSourceName());
			}
		}
		enabledLeft = true;
	}
	
	public void enableSummaryForRightSource(String filePrefix) throws RJException {
		if (sameJoinConfigs) {
			optimizedJoins[0].enableSummaryForRightSource("");
			addAllMinusListeners(optimizedJoins[0].getListeners(), optimizedJoins[0].getJoinCondition().getRightJoinColumns()[0].getSourceName());
		} else {
			DataStratum[] strata = getStrata();
			for (int i = 0; i < optimizedJoins.length; i++) {
				optimizedJoins[i].enableSummaryForRightSource("[" + strata[i].getName() + "]");
				addAllMinusListeners(optimizedJoins[i].getListeners(), optimizedJoins[i].getJoinCondition().getRightJoinColumns()[0].getSourceName());
			}
		}
		enabledRight = true;
	}
	
	
	private void addAllMinusListeners(List listeners, String sourceName) throws RJException {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			JoinListener listener = (JoinListener) iterator.next();
			if (listener instanceof DataSourceNotJoinedJoinListener) {
				DataSourceNotJoinedJoinListener nJL = (DataSourceNotJoinedJoinListener)listener;
				if (nJL.getSource().getSourceName().equals(sourceName)) {
					super.addJoinListener(nJL);
				}
			}
		}
	}

	public static AbstractJoin fromXML(AbstractDataSource leftSource, AbstractDataSource rightSource, Element node) throws RJException {
		boolean summaryLeft = Boolean.parseBoolean(DOMUtils.getAttribute(node, "summary-left"));
		boolean summaryRight = Boolean.parseBoolean(DOMUtils.getAttribute(node, "summary-right"));
		boolean uniformConfig = Boolean.parseBoolean(DOMUtils.getAttribute(node, "uniform-config"));
		Element joinsNode = DOMUtils.getChildElement(node, "joins");
		Element[] joins = DOMUtils.getChildElements(joinsNode);
		List joinList = new ArrayList();
		List strataNames = new ArrayList();
		for (int i = 0; i < joins.length; i++) {
			if (joins[i].getNodeName().equals(Configuration.JOIN_TAG)) {
				strataNames.add(DOMUtils.getAttribute(joins[i], "stratum-name"));
				joinList.add(AbstractJoin.fromXML(leftSource, rightSource, joins[i]));
			}
		}
		AbstractJoin join;
		try {
			if (uniformConfig) {
				join = new StrataJoinWrapper((AbstractJoin) joinList.get(0));
			} else {
				join = new StrataJoinWrapper((AbstractJoin[])joinList.toArray(new AbstractJoin[] {}));
			}
			if (summaryLeft) {
				join.enableSummaryForLeftSource("");
			}
			if (summaryRight) {
				join.enableSummaryForRightSource("");
			}
			return join;
		} catch (IOException e) {
			throw new RJException("Error when reading configuration.", e);
		}
	}
	
	public void addJoinListener(JoinListener listener) throws RJException {
		synchronized (set) {
			super.addJoinListener(listener);
			synchronized (this) {
				if (optimizedJoins != null) {
					for (int i = 0; i < optimizedJoins.length; i++) {
						optimizedJoins[i].addJoinListener(listener);
					}
				}
			}
		}
	}
	
	public void removeJoinListener(JoinListener listener) throws RJException {
		synchronized (set) {
			super.removeJoinListener(listener);
			synchronized (this) {
				if (optimizedJoins != null) {
					for (int i = 0; i < optimizedJoins.length; i++) {
						optimizedJoins[i].removeJoinListener(listener);
					}
				}
			}
		}
	}
	
	public void closeListeners() throws RJException {
		synchronized(set) {
			if (optimizedJoins != null)
			for (int i = 0; i < optimizedJoins.length; i++) {
				optimizedJoins[i].closeListeners();
			}
		}
	}
	
	public void notifyTrashingNotJoined(DataRow row) throws RJException {
		synchronized(set) {
			int jId = activeJoin.get();
			if (jId == optimizedJoins.length) {
				jId--;
			}
			optimizedJoins[jId].notifyTrashingNotJoined(row);
		}
	}
	
	public void setCancelled(boolean cancel) {
		if (optimizedJoins != null) {
			for (int i = 0; i < optimizedJoins.length; i++) {
				optimizedJoins[i].setCancelled(cancel);
			}
		}
		super.setCancelled(cancel);
	}
	
	public boolean isSummaryForLeftSourceEnabled() {
		return enabledLeft;
	}
	
	public boolean isSummaryForRightSourceEnabled() {
		return enabledRight;
	}
	
	public boolean newSourceA(AbstractDataSource source) throws IOException, RJException {
		if (optimizedJoins != null) {
			List columns = Arrays.asList(source.getDataModel().getOutputFormat());
			if (!sameJoinConfigs) {
				for (int i = 0; i < strata.length; i++) {
					if (!testColumns(columns, strata[i].getSourceA())) {
						return false;
					}
				}
			}
			for (int i = 0; i < optimizedJoins.length; i++) {
				AbstractJoinCondition cond = optimizedJoins[i].getJoinCondition();
				if (sameJoinConfigs) {
					for (int j = 0; j < ((StrataJoinCondition)cond).getStrata().length; j++) {
						if (!testColumns(columns, ((StrataJoinCondition)cond).getStrata()[j].getSourceA())) {
							return false;
						}
					}
				}
				if (!testColumns(columns, cond.getLeftJoinColumns())) {
					return false;
				}
			}
			for (int i = 0; i < optimizedJoins.length; i++) {
				optimizedJoins[i].newSourceA(source);
			}
		}
		return true;
	}
	
	public boolean newSourceB(AbstractDataSource source) throws IOException, RJException {
		if (optimizedJoins != null) {
			List columns = Arrays.asList(source.getDataModel().getOutputFormat());
			if (!sameJoinConfigs) {
				for (int i = 0; i < strata.length; i++) {
					if (!testColumns(columns, strata[i].getSourceB())) {
						return false;
					}
				}
			}
			for (int i = 0; i < optimizedJoins.length; i++) {
				AbstractJoinCondition cond = optimizedJoins[i].getJoinCondition();
				if (sameJoinConfigs) {
					for (int j = 0; j < ((StrataJoinCondition)cond).getStrata().length; j++) {
						if (!testColumns(columns, ((StrataJoinCondition)cond).getStrata()[j].getSourceB())) {
							return false;
						}
					}
				}
				if (!testColumns(columns, cond.getRightJoinColumns())) {
					return false;
				}
			}
			for (int i = 0; i < optimizedJoins.length; i++) {
				optimizedJoins[i].newSourceB(source);
			}
		}
		return true;
	}

	private boolean testColumns(List columns, AtomicCondition[] conds) {
		for (int i = 0; i < conds.length; i++) {
			if (!columns.contains(conds[i].getColumn())) {
				return false;
			}
		}
		return true;
	}

	private boolean testColumns(List columns, DataColumnDefinition[] dataColumnDefinitions) {
		for (int i = 0; i < dataColumnDefinitions.length; i++) {
			if (!columns.contains(dataColumnDefinitions[i])) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isConfigurationProgressSupported() {
		return true;
	}
	
	public int getConfigurationProgress() {
		synchronized (this) {
			if (optimizedJoins != null) {
				int active = activeJoin.get();
				if (active < optimizedJoins.length) {
					setConfigurationProgress(optimizedJoins[active].getConfigurationProgress());
				}
			}
		}
	return super.getConfigurationProgress();
	}
	
	public boolean isProgressSupported() {
		return true;
	}
	
	public int getProgress() {
		synchronized (this) {
			if (optimizedJoins != null) {
				int active = activeJoin.get();
				if (active < optimizedJoins.length) {
					int each = 100 / optimizedJoins.length;
					setProgress(each * active + optimizedJoins[active].getProgress() / optimizedJoins.length);
				} else {
					setProgress(100);
				}
			}
		}
		return super.getProgress();
	}
	
	public LinkageSummary getLinkageSummary() {
		if (dataSourceSummary != null) {
			return new LinkageSummary(dataSourceSummary.getCntReadSrcA(), dataSourceSummary.getCntReadSrcB(), linked);
		} else {
			return new LinkageSummary(-1, -1, linked);
		}
	}
}
