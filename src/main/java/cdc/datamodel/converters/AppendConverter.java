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
import javax.swing.JComponent;
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
import cdc.gui.Configs;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.datasource.JDataSource;
import cdc.gui.components.dynamicanalysis.ConvAnalysisActionListener;
import cdc.gui.components.paramspanel.DefaultParamPanelFieldCreator;
import cdc.gui.components.paramspanel.ParamPanelField;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.validation.ColumnNameValidator;
import cdc.gui.validation.CompoundValidator;
import cdc.gui.validation.NonEmptyValidator;
import cdc.gui.validation.Validator;
import cdc.utils.Log;
import cdc.utils.RJException;
import edu.emory.mathcs.util.xml.DOMUtils;

public class AppendConverter extends AbstractColumnConverter {
	
	public static class TrimVisibleComponent extends GUIVisibleComponent {

		private static final String PARAM_OUT_NAME = "out-name";
		private DataColumnDefinition column;
		private ParamsPanel panel;
		private JButton visual;
		private ConvAnalysisActionListener analysisListener = null;
		private ScriptPanel scriptPanel;
		
		private class CreatorName extends DefaultParamPanelFieldCreator {
			public ParamPanelField create(JComponent parent, String param, String label, String defaultValue) {
				ParamPanelField field = super.create(parent, param, label, defaultValue);
				field.addConfigurationChangeListener(analysisListener);
				return field;
			}
		}
		
		public Object generateSystemComponent() throws RJException {
			Map props = panel.getParams();
			props.put(PARAM_SCRIPT, scriptPanel.getScript());
			return new AppendConverter(panel.getParameterValue(PARAM_OUT_NAME), props, column);
		}

		public JPanel getConfigurationPanel(Object[] inParams, int sizeX, int sizeY) {
			
			AbstractDataSource source = (AbstractDataSource) inParams[2];
			Window parent = (Window) inParams[3];
			JDataSource jDataSource = (JDataSource)inParams[4];
			analysisListener = new ConvAnalysisActionListener(parent, source, this, jDataSource);
			
			scriptPanel = new ScriptPanel(AbstractColumnConverter.getDefaultScript(AppendConverter.class), 
					String.class, new String[] {"column", "appendFront", "appendEnd"}, new Class[] {String.class, String.class, String.class});
			
			String[] defs = new String[] {null, "", ""};
			if (getRestoredParam(PARAM_OUT_NAME) != null) {
				defs[0] = getRestoredParam(PARAM_OUT_NAME);
			}
			if (getRestoredParam(PROPERTY_APPEND_FRONT) != null) {
				defs[1] = getRestoredParam(PROPERTY_APPEND_FRONT);
			}
			if (getRestoredParam(PROPERTY_APPEND_END) != null) {
				defs[2] = getRestoredParam(PROPERTY_APPEND_END);
			}
			if (getRestoredParam(PARAM_SCRIPT) != null) {
				scriptPanel.setScript(getRestoredParam(PARAM_SCRIPT));
			}
			
			Map creators = new HashMap();
			creators.put(PROPERTY_APPEND_FRONT, new CreatorName());
			creators.put(PROPERTY_APPEND_END, new CreatorName());
			creators.put(PARAM_OUT_NAME, new CreatorName());
			panel = new ParamsPanel(new String[] {PARAM_OUT_NAME, PROPERTY_APPEND_FRONT, PROPERTY_APPEND_END}, 
					new String[] {"Output attribute name", "Append to the front of string", "Append to the end of string"}, 
					defs, creators);
			
			Map validators = new HashMap();
			validators.put(PARAM_OUT_NAME, new CompoundValidator(new Validator[] {new NonEmptyValidator(), new ColumnNameValidator()}));
			panel.setValidators(validators);
					
			visual = Configs.getAnalysisButton();
			//visual.setPreferredSize(new Dimension(visual.getPreferredSize().width, 20));
			panel.append(visual);
			
			
			this.column = (DataColumnDefinition) inParams[0];
			
			visual.addActionListener(analysisListener);
			
			JTabbedPane tabs = new JTabbedPane();
			tabs.addTab("Configuration", panel);
			tabs.addTab("Converter script (advanced)", scriptPanel);
			
			JPanel mainPanel = new JPanel(new BorderLayout());
			mainPanel.add(tabs, BorderLayout.CENTER);
			return mainPanel;
		}

		public String toString() {
			return "Append converter";
		}
		
		public Class getProducedComponentClass() {
			return AppendConverter.class;
		}

		public boolean validate(JDialog dialog) {
			return panel.doValidate();
		}

		public void windowClosing(JDialog parent) {	
		}
		
	}

	public static final String PROPERTY_APPEND_FRONT = "front";
	public static final String PROPERTY_APPEND_END = "end";
	private static final String PARAM_SCRIPT = "script";
	
	
	private DataColumnDefinition[] in;
	private DataColumnDefinition[] out;
	private String appendFront = "";
	private String appendEnd = "";
	
	private ScriptEvaluator scriptEvaluator;
	
	public AppendConverter(String columnName, Map props, DataColumnDefinition column) throws RJException {
		super(props);
		out = new DataColumnDefinition[1];
		out[0] = new ConverterColumnWrapper(columnName, DataColumnDefinition.TYPE_STRING, column.getSourceName());
		out[0].setEmptyValues(column.getEmptyValues());
		this.in = new DataColumnDefinition[] {column};
		props.put(TrimVisibleComponent.PARAM_OUT_NAME, columnName);
		appendFront = (String) props.get(PROPERTY_APPEND_FRONT);
		if (appendFront == null) {
			appendFront = "";
		}
		appendEnd = (String) props.get(PROPERTY_APPEND_END);
		if (appendEnd == null) {
			appendEnd = "";
		}
		try {
			if (props.get(PARAM_SCRIPT) == null) {
				props.put(PARAM_SCRIPT, AbstractColumnConverter.getDefaultScript(AppendConverter.class));
			}
			scriptEvaluator = new ScriptEvaluator((String)props.get(PARAM_SCRIPT), String.class, new String[] {"column", "appendFront", "appendEnd"}, new Class[] {String.class, String.class, String.class});
		} catch (CompileException e) {
			throw new RJException("Compilation exception for script", e);
		} catch (ParseException e) {
			throw new RJException("Parse exception for script", e);
		} catch (ScanException e) {
			throw new RJException("Scan exception for script", e);
		}
		
		Log.log(this.getClass(), "Converter created - front=" + appendFront + ", end=" + appendEnd, 2);
	}

	public DataCell[] convert(DataCell[] dataCells) throws RJException {
		String col = dataCells[0].getValue().toString();
		Log.log(this.getClass(), "Before converting: " + col, 3);
		try {
			String val = (String) scriptEvaluator.evaluate(new Object[] {col, appendFront, appendEnd});
			Log.log(this.getClass(), "After converting: " + val, 3);
			return new DataCell[] {new DataCell(dataCells[0].getValueType(), val)};
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
		super.getProperties().put(TrimVisibleComponent.PARAM_OUT_NAME, out[0].getColumnName());
		return super.getProperty(propertyName);
	}
	
	public Map getProperties() {
		super.getProperties().put(TrimVisibleComponent.PARAM_OUT_NAME, out[0].getColumnName());
		return super.getProperties();
	}
	
	public static AbstractColumnConverter fromXML(Element element, Map genericColumns) throws RJException {
		
		String name = DOMUtils.getAttribute(element, Configuration.NAME_ATTR);
		String columnName = DOMUtils.getAttribute(element, "column");
		Element paramsNode = DOMUtils.getChildElement(element, Configuration.PARAMS_TAG);
		Map params = Configuration.parseParams(paramsNode);
		DataColumnDefinition column = (DataColumnDefinition) genericColumns.get(columnName);
		column.setEmptyValues(getEmptyValues(readEmptyValues(element), 0));
		return new AppendConverter(name, params, column);
	}
	
	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new TrimVisibleComponent();
	}
	
	public String toString() {
		return "Append converter";
	}

	public void saveToXML(Document doc, Element conv) {
		DOMUtils.setAttribute(conv, Configuration.NAME_ATTR, out[0].getColumnName());
		DOMUtils.setAttribute(conv, "column", in[0].getColumnName());
		saveEmptyValuesToXML(doc, conv, out);
		Configuration.appendParams(doc, conv, getProperties());
	}
}
