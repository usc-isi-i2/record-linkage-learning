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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cdc.configuration.Configuration;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.converters.AbstractColumnConverter;
import cdc.datamodel.converters.ModelGenerator;
import cdc.impl.deduplication.DeduplicationConfig;
import cdc.impl.deduplication.DeduplicationDataSource;
import cdc.impl.join.strata.StrataJoinWrapper;
import cdc.utils.Log;
import cdc.utils.RJException;
import edu.emory.mathcs.util.xml.DOMUtils;

public abstract class AbstractDataSource extends SystemComponent {
	
	public static final String PARAM_SOURCE_NAME = "source-name";

	private static Object monitor = new Object();
	private static boolean stop = false;
	
	private DataColumnDefinition[] availableColumns;
	private String sourceName;
	private ModelGenerator modelGenerator;
	private Map columnsIndex = null;
	
	private int position = 0;
	
	private int strataRejected = 0;
	private int filterRejected = 0;
	
	private AtomicCondition[] filterStrata;
	private Filter filter;

	private AbstractDataSource preprocessedDataSource;
	
	private DeduplicationConfig dedupConfig = null;
	
	public AbstractDataSource(String name, Map params) {
		super(params);
		params.put(PARAM_SOURCE_NAME, name);
		this.sourceName = name;
	}
	
	public AbstractDataSource(String name, DataColumnDefinition[] model, Map params) {
		super(params);
		params.put(PARAM_SOURCE_NAME, name);
		this.sourceName = name;
		//this.modelGenerator = new ModelGenerator(model);
		//buildIndex();
		availableColumns = model;
		preprocessedDataSource = this;
	}

	public String getSourceName() {
		return this.sourceName;
	}
	
	public synchronized final DataRow getNextRow() throws IOException, RJException {
		DataRow row;
		while ((row = nextRow()) != null) {
			position++;
			if (filter != null && !filter.isSatisfied(row)) {
				filterRejected++;
				continue;
			}
			if (filterStrata == null || filterStrata.length == 0) {
				return row;
			}
			for (int i = 0; i < filterStrata.length; i++) {
				if (filterStrata[i].isSatisfied(row)) {
					row.setProperty(StrataJoinWrapper.PROPERTY_STRATUM_NAME, filterStrata[i].getStratumName());
					return row;
				}
			}
			strataRejected++;
		}
		logSummaryIfNeeded();
		return null;
	}
	
	private void logSummaryIfNeeded() {
		Log.log(getClass(), "Data source " + getSourceName() + " provided all records. Filtered out " + filterRejected + " records. Stratification filtered out " + strataRejected + " records.", 2);
	}

	public synchronized final DataRow[] getNextRows(int n)  throws IOException, RJException {
		List l = new ArrayList();
		DataRow row;
		while (l.size() < n && (row = getNextRow()) != null) {
			l.add(row);
		}
		if (l.isEmpty()) {
			return null;
		} else {
			return (DataRow[]) l.toArray(new DataRow[] {});
		}
	}
	
	public final void reset() throws IOException, RJException {
		doReset();
		strataRejected = 0;
		filterRejected = 0;
		position = 0;
	}
	
	public void saveToXML(Document doc, Element node) {
		
		Configuration.appendParams(doc, node, getProperties());
		//row model
		Element rowModel = DOMUtils.createChildElement(doc, node, Configuration.ROW_MODEL_TAG);
		ModelGenerator model = getDataModel();
		AbstractColumnConverter convs[] = model.getConverters();
		for (int i = 0; i < convs.length; i++) {
			Element conv = DOMUtils.createChildElement(doc, rowModel, Configuration.ROW_TAG);
			DOMUtils.setAttribute(conv, Configuration.CONVERTER_ATTR, convs[i].getClass().getName());
			convs[i].saveToXML(doc, conv);
		}
		if (dedupConfig != null || filter != null) {
			Element preprocessing = DOMUtils.createChildElement(doc, node, Configuration.PREPROCESSING_TAG);
			if (dedupConfig != null) {
				Element dedupElement = DOMUtils.createChildElement(doc, preprocessing, Configuration.DEDUPLICATION_TAG);
				dedupConfig.saveToXML(doc, dedupElement);
			}
			if (filter != null) {
				Element filterElement = DOMUtils.createChildElement(doc, preprocessing, Configuration.FILTER_TAG);
				DOMUtils.setAttribute(filterElement, "condition", filter.toString());
			}
		}
	}
	
	public static AbstractDataSource fromXML(Element node) throws RJException, IOException {
		Element paramsElement = DOMUtils.getChildElement(node, Configuration.PARAMS_TAG);
		Map params = Configuration.parseParams(paramsElement);
		
		String sourceName = DOMUtils.getAttribute(node, Configuration.NAME_ATTR);
		String className = DOMUtils.getAttribute(node, Configuration.CLASS_ATTR);
		
		try {
			Class clazz=null;
			try{
			clazz = Class.forName(className);
			}
			catch(Exception e){
				System.out.println(e.getMessage());
			}
			Constructor constructor = clazz.getConstructor(new Class[] {String.class, Map.class});
			AbstractDataSource dataSource = (AbstractDataSource) constructor.newInstance(new Object[] {sourceName, params});
			Element model = DOMUtils.getChildElement(node, Configuration.ROW_MODEL_TAG);
			if (model != null) {
				dataSource.setModel(Configuration.readRowModelConfiguration(model, dataSource.getAvailableColumns()));
			}
			Element preprocessing = DOMUtils.getChildElement(node, Configuration.PREPROCESSING_TAG);
			if (preprocessing != null) {
				Element dedup = DOMUtils.getChildElement(preprocessing, Configuration.DEDUPLICATION_TAG);
				if (dedup != null) {
					dataSource.setDeduplicationConfig(DeduplicationConfig.fromXML(dataSource, dedup));
				}
				Element filterElement = DOMUtils.getChildElement(preprocessing, Configuration.FILTER_TAG);
				if (filterElement != null) {
					dataSource.setFilter(new Filter(filterElement.getAttribute("condition"), dataSource.getDataModel().getOutputFormat()));
				}
			}
			return dataSource;
		} catch (Exception e) {
			throw new RJException("Error reading join configuration", e);
		}
	}
	
	public DataColumnDefinition[] getAvailableColumns() {
		return availableColumns;
	}
	
	public ModelGenerator getDataModel() {
		return modelGenerator;
	}

	public void setModel(ModelGenerator generator) {
		this.modelGenerator = generator;
		if (generator != null) {
			buildIndex();
		}
	}
	
//	public DataColumnDefinition getDataColumnDefinition(String columnName) {
//		return (DataColumnDefinition) columnsIndex.get(columnName);
//	}

	public void setOrderBy(DataColumnDefinition[] orderBy) throws IOException, RJException {
		throw new RuntimeException("AbstractDataSource does not implement setOrderBy! You need to use data source that overrides this method");
	}

	private void buildIndex() {
		columnsIndex = new HashMap();
		for (int i = 0; i < modelGenerator.getOutputFormat().length; i++) {
			columnsIndex.put(modelGenerator.getOutputFormat()[i].getColumnName(), modelGenerator.getOutputFormat()[i]);
		}
	}
	
	public abstract boolean equals(Object arg0);
	
	protected static boolean areTheSameProperties(AbstractDataSource s1, AbstractDataSource s2) {
		Map first = s1.getProperties();
		Map second = s2.getProperties();
		removeNulls(first);
		removeNulls(second);
		//System.out.println("Props 1: " + first);
		//System.out.println("Props 2: " + second);
		if (first.size() != second.size()) return false;
		Iterator params = first.keySet().iterator();
		while (params.hasNext()) {
			String paramName = (String) params.next();
			Object p1 = first.get(paramName);
			Object p2 = second.get(paramName);
			if ((p1 == null && p2 != null) || !p1.equals(p2)) return false;
		}
		return true;
	}

	private static void removeNulls(Map map) {
		Iterator params = map.keySet().iterator();
		ArrayList keysToRemove = new ArrayList();
		while (params.hasNext()) {
			String paramName = (String) params.next();
			if (map.get(paramName) == null) {
				keysToRemove.add(paramName);
			}
		}
		for (Iterator iterator = keysToRemove.iterator(); iterator.hasNext();) {
			String pName = (String) iterator.next();
			map.remove(pName);
		}
	}
	
	public void setFilter(Filter filter) {
		this.filter = filter;
	}
	
	public Filter getFilter() {
		return filter;
	}
	
	public void setStratumCondition(AtomicCondition[] condition) throws IOException, RJException {
		//System.out.println(getClass().getName() + " Setting filter: " + PrintUtils.printArray(condition));
		this.filterStrata = condition;
		//doReset();
	}
	

	public AtomicCondition[] getFilterCondition() {
		return this.filterStrata;
	}

	public static void requestStop(boolean b) {
		synchronized (monitor) {
			//new Exception().printStackTrace();
			//System.out.println("Setting stop to: " + b);
			stop = b;
		}
	}
	
	public static boolean isStopRequested() {
		synchronized (monitor) {
			if (stop) {
				System.out.println("WARNING: stop requested flag is on in data source");
			}
			return stop;
		}
	}
	
	public long position() {
		return position;
	}
	
	public int getConfigurationProgress() {
		throw new RuntimeException("This class does not implement Configuration Progress notification.");
	}
	
	public boolean isConfigurationProgressSupported() {
		return false;
	}
	
	public void setStart(long start) throws IOException, RJException {
		throw new RuntimeException("By default datasources do not implement this method as it can be quite specific. Please implement it if you want to use it.");
	}
	
	public void setLimit(long limit) throws IOException, RJException {
		throw new RuntimeException("By default datasources do not implement this method as it can be quite specific. Please implement it if you want to use it.");
	}
	
	public void skip(long count) throws IOException, RJException {
		for (long i = 0; i < count; i++) {
			getNextRow();
		}
	}
	
	public void setDeduplicationConfig(DeduplicationConfig deduplication) {
		dedupConfig = deduplication;
		configureDeduplication();
	}

	private void configureDeduplication() {
		if (dedupConfig == null) {
			preprocessedDataSource = this;
		} else {
			preprocessedDataSource = new DeduplicationDataSource(this, dedupConfig);
		}
	}
	
	public DeduplicationConfig getDeduplicationConfig() {
		return dedupConfig;
	}
	
	public AbstractDataSource getPreprocessedDataSource() {
		if (preprocessedDataSource == null) {
			configureDeduplication();
		}
		return preprocessedDataSource;
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
	}
	
	public void close() throws IOException, RJException {
		//The following is to break the circular reference
		if (this.preprocessedDataSource == this) {
			this.preprocessedDataSource = null;
		}
		doClose();
	}
	
	protected abstract DataRow nextRow() throws IOException, RJException;
	protected abstract void doClose() throws IOException, RJException;
	protected abstract void doReset() throws IOException, RJException;
	public abstract boolean canSort();
	public abstract long size() throws IOException, RJException;
	public abstract AbstractDataSource copy() throws IOException, RJException;
	
}
