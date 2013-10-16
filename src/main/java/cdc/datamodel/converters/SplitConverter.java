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


package cdc.datamodel.converters;

import java.awt.BorderLayout;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.codehaus.janino.CompileException;
import org.codehaus.janino.ScriptEvaluator;
import org.codehaus.janino.Parser.ParseException;
import org.codehaus.janino.Scanner.ScanException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cdc.components.AbstractDataSource;
import cdc.configuration.Configuration;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.converters.ui.ScriptPanel;
import cdc.datamodel.converters.ui.SplitNamesFieldCreator;
import cdc.datamodel.converters.ui.SplitNamesValidator;
import cdc.gui.Configs;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.datasource.JDataSource;
import cdc.gui.components.dynamicanalysis.ConvAnalysisActionListener;
import cdc.gui.components.paramspanel.ParamPanelField;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.components.paramspanel.SeparatorPanelFieldCreator;
import cdc.gui.validation.Validator;
import cdc.utils.Log;
import cdc.utils.PrintUtils;
import cdc.utils.RJException;
import cdc.utils.StringUtils;
import edu.emory.mathcs.util.xml.DOMUtils;

public class SplitConverter extends AbstractColumnConverter {

	private static final String[] seps = new String[] {"\t", ";", ",", " "};
	private static final String[] labels = new String[] {"Tab", "Semicolon", "Comma", "Space"};

	public static class SplitVisibleComponent extends GUIVisibleComponent {

		private static final String PARAM_COL_NAME = "col-name";

//		private static class Creator extends DefaultParamPanelFieldCreator {
//			public ParamPanelField create(JComponent parent, String param, String label, String defaultValue) {
//				ParamPanelField field = super.create(parent, param, label, defaultValue);
//				field.addPropertyChangeListener(propertyListener);
//				return field;
//			}
//		}
		
		private ParamsPanel panel;
		private DataColumnDefinition column;
		private JButton visual;
		private ConvAnalysisActionListener analysisListener = null;
		private ScriptPanel scriptPanel;
		
		public Object generateSystemComponent() throws RJException {
			Map params = panel.getParams();
			params.put(PARAM_SCRIPT, scriptPanel.getScript());
			return new SplitConverter(panel.getParameterValue(PARAM_COL_NAME), params, column);
		}

		public JPanel getConfigurationPanel(Object[] params, int sizeX, int sizeY) {
			
			AbstractDataSource source = (AbstractDataSource) params[2];
			Window parent = (Window) params[3];
			JDataSource jDataSource = (JDataSource)params[4];
			analysisListener = new ConvAnalysisActionListener(parent, source, this, jDataSource);
			
			scriptPanel = new ScriptPanel(AbstractColumnConverter.getDefaultScript(SplitConverter.class), 
					String[].class, new String[] {"column", "splitStr", "outSize"}, new Class[] {String.class, String.class, Integer.TYPE});
				
			String[] def = new String[] {"out_1,out_2", DEFAULT_SPLIT};
			if (getRestoredParam(PARAM_COL_NAME) != null) {
				def[0] = getRestoredParam(PARAM_COL_NAME);
			}
			if (getRestoredParam(PARAM_SPLIT) != null) {
				def[1] = getRestoredParam(PARAM_SPLIT);
			}
			if (getRestoredParam(PARAM_SCRIPT) != null) {
				scriptPanel.setScript(getRestoredParam(PARAM_SCRIPT));
			}
			
			Map listeners = new HashMap();
			listeners.put(PARAM_SPLIT, new SeparatorPanelFieldCreator(seps, labels, 2, 3, analysisListener));
			listeners.put(PARAM_COL_NAME, new SplitNamesFieldCreator(analysisListener));
			panel = new ParamsPanel(new String[] {PARAM_COL_NAME, PARAM_SPLIT}, 
					new String[] {"Output attribute name", "Split character"}, 
					def, listeners);
			Map validators = new HashMap();
			validators.put(PARAM_SPLIT, new Validator() {
				public boolean validate(ParamsPanel paramsPanel, ParamPanelField paramPanelField, String parameterValue) {
					return !StringUtils.isNullOrEmptyNoTrim(parameterValue);
				}});
			
			validators.put(PARAM_COL_NAME, new SplitNamesValidator());
			panel.setValidators(validators);
			
			visual = Configs.getAnalysisButton();
			//visual.setPreferredSize(new Dimension(visual.getPreferredSize().width, 20));
			panel.append(visual);
			
			this.column = (DataColumnDefinition) params[0];
			
			visual.addActionListener(analysisListener);
			
			JTabbedPane tabs = new JTabbedPane();
			tabs.addTab("Configuration", panel);
			tabs.addTab("Converter script (advanced)", scriptPanel);
			
			JPanel mainPanel = new JPanel(new BorderLayout());
			mainPanel.add(tabs, BorderLayout.CENTER);
			return mainPanel;
		}

		public String toString() {
			return "Split converter";
		}

		public Class getProducedComponentClass() {
			return SplitConverter.class;
		}

		public boolean validate(JDialog dialog) {
			return panel.doValidate();
		}

		public void windowClosing(JDialog parent) {
			// TODO Auto-generated method stub
			
		}

	}

	public static final String PARAM_SPLIT = "split";
	public static final String PARAM_SIZE = "size";
	private static final String PARAM_SCRIPT = "script";
	public static final String DEFAULT_SPLIT = " ";
	
	private static final int logLevel = Log.getLogLevel(SplitConverter.class);
	
	private DataColumnDefinition[] in;
	private DataColumnDefinition[] out;
	private String split;
	
	private ScriptEvaluator scriptEvaluator;
	
	public SplitConverter(String columnName, Map params, DataColumnDefinition inputColumn) throws RJException {
		this(columnName, params, inputColumn, null);
	}
	
	public SplitConverter(String columnName, Map params, DataColumnDefinition inputColumn, String[][] emptyValues) throws RJException {
		super(params);
		
		split = DEFAULT_SPLIT;
		if (params.containsKey(PARAM_SPLIT)) {
			split = (String)params.get(PARAM_SPLIT);
		}
		
		//backwards compatibility
		if (params.containsKey(PARAM_SIZE)) {
			int size = Integer.parseInt((String)params.get(PARAM_SIZE));
			String names = "";
			for (int i = 0; i < size; i++) {
				if (i != 0) {
					names += ",";
				}
				names += columnName + "_" + i;
			}
			columnName = names;
			params.remove(PARAM_SIZE);
		}
		
		String[] names = columnName.split(",");
		this.in = new DataColumnDefinition[] {inputColumn};
		this.out = new DataColumnDefinition[names.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = new ConverterColumnWrapper(names[i], DataColumnDefinition.TYPE_STRING, inputColumn.getSourceName());
			if (emptyValues != null && emptyValues.length > i) {
				out[i].setEmptyValues(emptyValues[i]);
			}
		}
		
		try {
			if (params.get(PARAM_SCRIPT) == null) {
				params.put(PARAM_SCRIPT, AbstractColumnConverter.getDefaultScript(SplitConverter.class));
			}
			scriptEvaluator = new ScriptEvaluator((String)params.get(PARAM_SCRIPT), String[].class, new String[] {"column", "splitStr", "outSize"}, new Class[] {String.class, String.class, Integer.TYPE});
		} catch (CompileException e) {
			throw new RJException("Compilation exception for script", e);
		} catch (ParseException e) {
			throw new RJException("Parse exception for script", e);
		} catch (ScanException e) {
			throw new RJException("Scan exception for script", e);
		}
		
		Log.log(getClass(), "Converter created, split='" + split + "', out columns = " + PrintUtils.printArray(out), 1);
		
	}

	public DataCell[] convert(DataCell[] dataCells) throws RJException {
		String value = (String)dataCells[0].getValue();
		
		try {
			
			String[] out = (String[]) scriptEvaluator.evaluate(new Object[] {value, split, new Integer(this.out.length)});
			
			DataCell[] outCells = new DataCell[out.length];
			for (int i = 0; i < outCells.length; i++) {
				outCells[i] = new DataCell(this.out[i].getColumnType(), out[i]);
			}
			
			if (logLevel >= 2) {
				Log.log(getClass(), "Converted " + value + " -> " + PrintUtils.printArray(out), 2);
			}
			return outCells;
			
		} catch (InvocationTargetException e) {
			throw new RJException("Error when executing converter script", e);
		}
		
	}

	public DataColumnDefinition[] getExpectedColumns() {
		return in;
	}

	public DataColumnDefinition[] getOutputColumns() {
		return out;
	}
	
	public String getProperty(String propertyName) {
		super.getProperties().put(SplitVisibleComponent.PARAM_COL_NAME, restoreColNames());
		return super.getProperty(propertyName);
	}
	
	public Map getProperties() {
		super.getProperties().put(SplitVisibleComponent.PARAM_COL_NAME, restoreColNames());
		return super.getProperties();
	}
	
	private String restoreColNames() {
		StringBuffer str = new StringBuffer();
		for (int i = 0; i < out.length; i++) {
			if (i != 0) {
				str.append(",");
			}
			str.append(out[i].getColumnName());
		}
		return str.toString();
	}

	public static AbstractColumnConverter fromXML(Element element, Map genericColumns) throws RJException {
		String name = DOMUtils.getAttribute(element, Configuration.NAME_ATTR);
		String colName = DOMUtils.getAttribute(element, "column");
		Element paramsNode = DOMUtils.getChildElement(element, Configuration.PARAMS_TAG);
		Map params = Configuration.parseParams(paramsNode);
		DataColumnDefinition column = (DataColumnDefinition) genericColumns.get(colName); 
		return new SplitConverter(name, params, column, readEmptyValues(element));
	}

	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new SplitVisibleComponent();
	}

	public String toString() {
		return "Split converter";
	}

	public void saveToXML(Document doc, Element conv) {
		//System.out.println("FIX HERE!!!!!!!!!!!!!!!!!!!!!!!!!");
		DOMUtils.setAttribute(conv, Configuration.NAME_ATTR, restoreColNames());
		DOMUtils.setAttribute(conv, "column", in[0].getColumnName());
		saveEmptyValuesToXML(doc, conv, out);
		Configuration.appendParams(doc, conv, getProperties());
	}
}
