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


package cdc.configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.components.AbstractJoinCondition;
import cdc.components.AbstractResultsSaver;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.converters.AbstractColumnConverter;
import cdc.datamodel.converters.DummyConverter;
import cdc.datamodel.converters.ModelGenerator;
import cdc.impl.resultsavers.ResultSaversGroup;
import cdc.utils.PrintUtils;
import cdc.utils.RJException;
import edu.emory.mathcs.util.xml.DOMUtils;

public class Configuration {
	
	public static final String DEFAULT_CONFIGURATION_FILE = "config.xml";
	
	public static final String LEFT_SOURCE_TAG = "left-data-source";
	public static final String RIGHT_SOURCE_TAG = "right-data-source";
	public static final String JOIN_TAG = "join";
	public static final String RESULTS_SAVERS_TAG = "results-savers";
	public static final String RESULTS_SAVER_TAG = "results-saver";
	
	public static final String CLASS_ATTR = "class";
	
	public static final String PARAMS_TAG = "params";
	public static final String PARAM_TAG = "param";
	
	public static final String NAME_ATTR = "name";
	public static final String VALUE_ATTR = "value";
	public static final String CONVERTER_ATTR = "converter";
	
	public static final String ROW_MODEL_TAG = "row-model";
	public static final String ROW_TAG = "column";
	
	public static final String COLUMN_DATASOURCE_ATTR = "source";
	
	public static final String JOIN_CONDITION_TAG = "join-condition";

	public static final String PREPROCESSING_TAG = "preprocessing";

	public static final String DEDUPLICATION_TAG = "deduplication";

	public static final String FILTER_TAG = "filter-condition";

	public static boolean stopForced = false;
	
	private File config;
	private AbstractDataSource[] sources = new AbstractDataSource[2];
	private AbstractJoin join;
	private AbstractResultsSaver saver;
	
	private ConfiguredSystem system;
	
	private ConfigurationListener listener;
	
	public static Configuration getConfiguration() throws RJException, IOException {
		return new Configuration(DEFAULT_CONFIGURATION_FILE, false, null);
	}
	
	public static Configuration getConfiguration(String configFile) throws RJException, IOException {
		return new Configuration(configFile, false, null);
	}
	
	public static Configuration getConfiguration(String configFile, boolean resetConfiguration) throws RJException, IOException {
		return new Configuration(configFile, resetConfiguration, null);
	}
	
	public Configuration(String fileName, boolean reset) throws RJException, IOException {
		this(fileName, reset, null);
	}
	
	public Configuration(String fileName, boolean reset, ConfigurationListener listener) throws RJException, IOException {
		config = new File(fileName);
		this.listener = listener;
		System.out.println(config.exists());
		
		BufferedReader br = new BufferedReader(new FileReader(config));
		 String line = null;
		 while ((line = br.readLine()) != null) {
		   System.out.println(line);
		 }
		
		
		if (config.isDirectory()) {
			throw new RJException("Configuration must be a file, not a directory");
		}
		if (!reset && !config.exists()) {
			throw new RJException("Configuration file does not exist");
		}
		if (reset && config.exists()) {
			config.delete();
			config.createNewFile();
		}
		if (!reset) {
			readConfiguration();
		}
	}
	
	public AbstractDataSource[] getDataSources() {
		return sources;
	}
	
	public AbstractJoin getJoin() {
		return join;
	}
	
	public AbstractResultsSaver getResultsSaver() {
		return saver;
	}
	
	public void setDataSources(AbstractDataSource[] sources) {
		this.sources = sources;
	}
	
	public void setJoin(AbstractJoin join) {
		this.join = join;
	}
	
//	public void addAbstractResultSavers(AbstractResultsSaver[] savers) {
//		this.savers.addAll(Arrays.asList(savers));
//	}
	
	private void readConfiguration() throws IOException, RJException {
		try {
			DocumentBuilder builder = DOMUtils.createDocumentBuilder(false, false);
			Document doc = builder.parse(config);
			Node baseNode = doc.getDocumentElement();
			boolean dedupe = Boolean.parseBoolean(DOMUtils.getAttribute((Element)baseNode, "deduplication", "false"));
			if (listener != null) {
				listener.configurationModeDetermined(dedupe);
			}
			NodeList list = baseNode.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				Node child = list.item(i);
				System.out.println(child.getNodeName());
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					readSpecificConfiguration((Element)child);
				}
				if (stopForced) {
					break;
				}

				//This is to make the components appear on GUI panel as they are being configured
				//This has to be like that due to some stupid dependencies between components
				//I know this is really lame :)
				if (!dedupe) {
					system = new ConfiguredSystem(sources[0], sources[1], join, saver);
				} else {
					system = new ConfiguredSystem(sources[0]);
				}
				if (listener != null) {
					listener.systemUpdated(system);
				}
			}	
			//System.out.println("Configuration read done.");
		} catch (SAXException e) {
			throw new RJException("Error when reading configuration", e);
		}
	}
	
	public ConfiguredSystem getSystem() {
		return system;
	}
	
	private void readSpecificConfiguration(Element child) throws RJException {
		String nodeName = child.getNodeName();
		String className = DOMUtils.getAttribute((Element)child, CLASS_ATTR);
		if (className == null && !nodeName.equals(RESULTS_SAVERS_TAG)) {
			throw new RJException("XML tag " + child.getNodeName() + " requires specification of attribute " + CLASS_ATTR);
		}
		ConfigurationPhase phase = null;
		try {
			if (nodeName.equals(LEFT_SOURCE_TAG)) {
				notifyBegin(phase = ConfigurationPhase.loadingLeftSourcePhase);
				sources[0] = AbstractDataSource.fromXML(child);
				notifyEnd(ConfigurationPhase.loadingLeftSourcePhase);
			} else if (nodeName.equals(RIGHT_SOURCE_TAG)) {
				notifyBegin(phase = ConfigurationPhase.loadingRightSourcePhase);
				sources[1] = AbstractDataSource.fromXML(child);
				notifyEnd(ConfigurationPhase.loadingRightSourcePhase);
			} else if (nodeName.equals(JOIN_TAG)) {
				Class clazz = Class.forName(className);
				notifyBegin(phase = ConfigurationPhase.loadingJoinProcessPhase);
				Method fromXMLMethod = clazz.getMethod("fromXML", new Class[] {AbstractDataSource.class, AbstractDataSource.class, Element.class});
				join = (AbstractJoin) fromXMLMethod.invoke(null, new Object[] {sources[0], sources[1], child});
				//join = AbstractJoin.fromXML(sources[0], sources[1], child);
				notifyEnd(ConfigurationPhase.loadingJoinProcessPhase);
			} else if (nodeName.equals(RESULTS_SAVERS_TAG)) {
				notifyBegin(phase = ConfigurationPhase.loadingResultSaversPhase);
				NodeList list = child.getChildNodes();
				List savers = new ArrayList();
				for (int i = 0; i < list.getLength(); i++) {
					Node saver = list.item(i);
					if (saver.getNodeType() == Node.ELEMENT_NODE && saver.getNodeName().equals(RESULTS_SAVER_TAG)) {
						className = DOMUtils.getAttribute((Element)saver, CLASS_ATTR);
						if (className == null) {
							throw new RJException("XML tag " + saver.getNodeName() + " requires specification of attribute " + CLASS_ATTR);
						}
						Class clazz = Class.forName(className);
						Method fromXMLMethod = clazz.getMethod("fromXML", new Class[] {Element.class});
						savers.add(fromXMLMethod.invoke(null, new Object[] {saver}));
					}
				}
				if (savers.size() == 1) {
					saver = (AbstractResultsSaver) savers.get(0);
				} else {
					//backward compatibility...
					AbstractResultsSaver[] saversArr = (AbstractResultsSaver[]) savers.toArray(new AbstractResultsSaver[] {});
					saver = new ResultSaversGroup(saversArr);
				}
				notifyEnd(ConfigurationPhase.loadingResultSaversPhase);
			} else {
				throw new RJException("Unsupported tag in configuration: " + nodeName);
			}
		} catch (ClassNotFoundException e) {
			notifyError(phase);
			throw new RJException("Class configured in tag " + nodeName + "(" + className + ") not found. Make sure the name and classpath are correct", e);
		} catch (SecurityException e) {
			notifyError(phase);
			throw new RJException("Class " + className + " must implement proper fromXML method", e);
		} catch (NoSuchMethodException e) {
			notifyError(phase);
			throw new RJException("Class " + className + " must implement proper fromXML method", e);
		} catch (IllegalArgumentException e) {
			notifyError(phase);
			throw new RJException("Class " + className + ", exception executing method fromXML", e);
		} catch (IllegalAccessException e) {
			notifyError(phase);
			throw new RJException("Class " + className + " must implement  proper fromXML method", e);
		} catch (InvocationTargetException e) {
			notifyError(phase);
			throw new RJException("Class " + className + ", exception executing method fromXML", e.getCause());
		} catch (Exception e) {
			notifyError(phase);
			throw new RJException("Error when reading configuration.", e);
		}
	}

	private void notifyError(ConfigurationPhase phase) {
		if (listener != null) {
			listener.configurationEvent(this, phase, ConfigurationPhase.ERROR);
		}
	}

	private void notifyBegin(ConfigurationPhase phase) {
		if (listener != null) {
			listener.configurationEvent(this, phase, ConfigurationPhase.START);
		}
	}

	private void notifyEnd(ConfigurationPhase phase) {
		if (listener != null) {
			listener.configurationEvent(this, phase, ConfigurationPhase.END);
		}
	}
	
	public void saveConfiguration() {
		DocumentBuilder builder = DOMUtils.createDocumentBuilder();
		Document doc = builder.newDocument();
		Node mainNode = doc.createElement("configuration");
		doc.appendChild(mainNode);
		System.out.println(mainNode);
	}

	public static Map parseParams(Element paramsElement) throws RJException {
		Map params = new HashMap();
		if (paramsElement == null) {
			return params;
		}
		Element[] elements = DOMUtils.getChildElements(paramsElement);
		for (int i = 0; i < elements.length; i++) {
			String name = DOMUtils.getAttribute(elements[i], NAME_ATTR);
			String value = DOMUtils.getAttribute(elements[i], VALUE_ATTR);
			if (isNullOrEmpty(name)) {
				throw new RJException("Each param node has to have attributes " + NAME_ATTR + " and " + VALUE_ATTR);
			}
			if (!isNullOrEmpty(value)) {
				params.put(name, value);
			}
		}
		return params;
	}

	public static ModelGenerator readRowModelConfiguration(Element rowModelConfig, DataColumnDefinition[] availableColumns) throws RJException {
		List cols = new ArrayList();
		Map columns = new HashMap();
		for (int i = 0; i < availableColumns.length; i++) {
			columns.put(availableColumns[i].getColumnName(), availableColumns[i]);
		}
		Element[] rows = DOMUtils.getChildElements(rowModelConfig);
		for (int i = 0; i < rows.length; i++) {
			if (!rows[i].getNodeName().equals(ROW_TAG)) {
				throw new RJException("Tag " + ROW_MODEL_TAG + " can contain only tags " + ROW_TAG);
			}
			String rowName = DOMUtils.getAttribute(rows[i], Configuration.NAME_ATTR);
			String converter = DOMUtils.getAttribute(rows[i], Configuration.CONVERTER_ATTR);
			AbstractColumnConverter converterImpl = null;
			if (isNullOrEmpty(converter)) {
				if (!columns.containsKey(rowName)) {
					throw new RJException("Data source " + availableColumns[0].getSourceName() + " does not provide column " + rowName + ". Available columns: " + columns.keySet());
				}
				converterImpl = new DummyConverter(rowName, (DataColumnDefinition)columns.get(rowName), null);
			} else {
				try {
					Class clazz = Class.forName(converter);
					Method fromXMLMethod = clazz.getMethod("fromXML", new Class[] {Element.class, Map.class});
					converterImpl = (AbstractColumnConverter) fromXMLMethod.invoke(null, new Object[] {rows[i], columns});
					DataColumnDefinition[] eCols = converterImpl.getExpectedColumns();
					for (int j = 0; j < eCols.length; j++) {
						if (!columns.containsKey(eCols[j].getColumnName())) {
							throw new RJException("Data source " + availableColumns[0].getSourceName() + " does not provide column " + rowName + ". Available columns: " + columns.keySet());
						}
					}
					DataColumnDefinition[] outputColumns = converterImpl.getOutputColumns();
					for (int j = 0; j < outputColumns.length; j++) {
						columns.put(outputColumns[j].getColumnName(), outputColumns[j]);
					}
				} catch (ClassNotFoundException e) {
					throw new RJException("Class configured in tag " + converter + "not found. Make sure the name and classpath are correct", e);
				} catch (SecurityException e) {
					throw new RJException("Class " + converter + " must implement proper fromXML method", e);
				} catch (NoSuchMethodException e) {
					throw new RJException("Class " + converter + " must implement proper fromXML method", e);
				} catch (IllegalArgumentException e) {
					throw new RJException("Class " + converter + ", exception executing method fromXML", e);
				} catch (IllegalAccessException e) {
					throw new RJException("Class " + converter + " must implement  proper fromXML method", e);
				} catch (InvocationTargetException e) {
					throw new RJException("Class " + converter + ", exception executing method fromXML", e.getCause());
				}
			}
			cols.add(converterImpl);
		}
		return new ModelGenerator((AbstractColumnConverter[])cols.toArray(new AbstractColumnConverter[] {}));
	}
	
	public static boolean isNullOrEmpty(String str) {
		return str == null || str.equals("");
	}
	
	public static DataColumnDefinition[] readRowModelConfiguration(Element rowModelConfig, AbstractDataSource[] abstractDataSources) throws RJException {
		List cols = new ArrayList();
		Map sources = new HashMap();
		for (int i = 0; i < abstractDataSources.length; i++) {
			sources.put(abstractDataSources[i].getSourceName(), abstractDataSources[i]);
		}
		Element[] columns = DOMUtils.getChildElements(rowModelConfig);
		for (int i = 0; i < columns.length; i++) {
			if (!columns[i].getNodeName().equals(ROW_TAG)) {
				throw new RJException("Tag " + ROW_MODEL_TAG + " can contain only tags " + ROW_TAG);
			}
			String sourceName = DOMUtils.getAttribute(columns[i], Configuration.COLUMN_DATASOURCE_ATTR);
			String rowName = DOMUtils.getAttribute(columns[i], Configuration.NAME_ATTR);
			
			AbstractDataSource source = (AbstractDataSource) sources.get(sourceName);
			if (source == null) {
				throw new RJException("Data source " + sourceName + " is not defined.");
			}
			DataColumnDefinition column = source.getDataModel().getColumnByName(rowName);
			if (column == null) {
				throw new RJException("Data source " + sourceName + " does not provide column " + rowName + ". Available columns: " + PrintUtils.printArray(source.getDataModel().getOutputFormat()));
			}
			cols.add(column);
		}
		return (DataColumnDefinition[]) cols.toArray(new DataColumnDefinition[] {});
	}

	public static void saveToXML(ConfiguredSystem system, File f) {
		DocumentBuilder builder = DOMUtils.createDocumentBuilder(false, false);
		Document doc = builder.newDocument();
		Element mainElement = doc.createElement("configuration");
		
		if (system.isDeduplication()) {
			DOMUtils.setAttribute(mainElement, "deduplication", "true");
		}
		
		if (system.getSourceA() != null) {
			Element leftSource = DOMUtils.createChildElement(doc, mainElement, LEFT_SOURCE_TAG);
			DOMUtils.setAttribute(leftSource, CLASS_ATTR, system.getSourceA().getClass().getName());
			DOMUtils.setAttribute(leftSource, NAME_ATTR, system.getSourceA().getSourceName());
			system.getSourceA().saveToXML(doc, leftSource);
			mainElement.appendChild(leftSource);
		}
		
		if (system.getSourceB() != null) {
			Element rightSource = DOMUtils.createChildElement(doc, mainElement, RIGHT_SOURCE_TAG);
			DOMUtils.setAttribute(rightSource, CLASS_ATTR, system.getSourceB().getClass().getName());
			DOMUtils.setAttribute(rightSource, NAME_ATTR, system.getSourceB().getSourceName());
			system.getSourceB().saveToXML(doc, rightSource);
			mainElement.appendChild(rightSource);
		}
		
		if (system.getJoin() != null) {
			Element join = DOMUtils.createChildElement(doc, mainElement, JOIN_TAG);
			DOMUtils.setAttribute(join, CLASS_ATTR, system.getJoin().getClass().getName());
			system.getJoin().saveToXML(doc, join);
		}
		
		if (system.getResultSaver() != null) {
			Element savers = DOMUtils.createChildElement(doc, mainElement, RESULTS_SAVERS_TAG);
			Element saver = DOMUtils.createChildElement(doc, savers, RESULTS_SAVER_TAG);
			DOMUtils.setAttribute(saver, CLASS_ATTR, system.getResultSaver().getClass().getName());
			system.getResultSaver().saveToXML(doc, saver);
		}
		
		doc.appendChild(mainElement);
		
		try {
			DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
			DOMImplementationLS domImplLS = (DOMImplementationLS)registry.getDOMImplementation("LS");
			LSSerializer lsSer = domImplLS.createLSSerializer();
			DOMConfiguration config = lsSer.getDomConfig();
			config.setParameter("format-pretty-print", new Boolean(true));
			LSOutput out = domImplLS.createLSOutput();
			FileOutputStream fs = new FileOutputStream(f);
			out.setByteStream(fs);
			lsSer.write(doc, out);
			fs.flush();
			fs.close();
			
//			OutputFormat format = new OutputFormat();
//			format.setIndenting(true);
//			format.setLineWidth(300);
//			XMLSerializer serializer = new XMLSerializer(format);		    
//	    	serializer.setOutputCharStream(new java.io.FileWriter(f));
//	    	serializer.serialize(doc);
	    } catch (IOException e) {
	    	e.printStackTrace();
	    } catch (ClassCastException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static void appendParams(Document doc, Element node, Map properties) {
		if (properties == null) return;
		Element props = DOMUtils.createChildElement(doc, node, PARAMS_TAG);
		Iterator iter = properties.keySet().iterator();
		while (iter.hasNext()) {
			String name = (String) iter.next();
			String value = (String) properties.get(name);
			Element prop = DOMUtils.createChildElement(doc, props, PARAM_TAG);
			DOMUtils.setAttribute(prop, NAME_ATTR, name);
			DOMUtils.setAttribute(prop, VALUE_ATTR, value);
		}
	}

	public static AbstractJoinCondition readConditionConfiguration(AbstractDataSource leftSource, 
			AbstractDataSource rightSource, Element joinConditionElement) throws RJException {
		String className = DOMUtils.getAttribute(joinConditionElement, CLASS_ATTR);
		if (className == null) {
			throw new RJException("Condition tag in join configuration have to provide class attribute");
		}
		try {
			Class clazz = Class.forName(className);
			Method fromXMLMethod = clazz.getMethod("fromXML", new Class[] {AbstractDataSource.class, AbstractDataSource.class, Element.class});
			return (AbstractJoinCondition) fromXMLMethod.invoke(null, new Object[] {leftSource, rightSource, joinConditionElement});
		} catch (ClassNotFoundException e) {
			throw new RJException("Class " + className + ") not found. Make sure the name and classpath are correct", e);
		} catch (SecurityException e) {
			throw new RJException("Class " + className + " must implement proper fromXML method", e);
		} catch (NoSuchMethodException e) {
			throw new RJException("Class " + className + " must implement proper fromXML method", e);
		} catch (IllegalArgumentException e) {
			throw new RJException("Class " + className + ", exception executing method fromXML", e);
		} catch (IllegalAccessException e) {
			throw new RJException("Class " + className + " must implement  proper fromXML method", e);
		} catch (InvocationTargetException e) {
			throw new RJException("Class " + className + ", exception executing method fromXML", e.getCause());
		}
	}
}
