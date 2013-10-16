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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
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
import cdc.gui.MainFrame;
import cdc.gui.OptionDialog;
import cdc.gui.components.datasource.JDataSource;
import cdc.gui.components.dynamicanalysis.ConvAnalysisActionListener;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.components.table.TablePanel;
import cdc.gui.validation.ColumnNameValidator;
import cdc.gui.validation.CompoundValidator;
import cdc.gui.validation.NonEmptyValidator;
import cdc.gui.validation.Validator;
import cdc.utils.RJException;
import edu.emory.mathcs.util.xml.DOMUtils;

public class ReplaceConverter extends AbstractColumnConverter {

	public static final String PROP_RULE_PREFIX = "replace";
	public static final String PROP_RULE_SUBS = "new-string";
	public static final String PROP_OUT_NAME = "out-column";
	private static final String PARAM_SCRIPT = "script";
	
	private static class VisibleComponent extends GUIVisibleComponent {
		
		private static final String PARAM_WITH = "with";
		private static final String PARAM_REGEX = "regex";
		private DataColumnDefinition column;
		private ParamsPanel params;
		private TablePanel table;
		private JButton visual;
		private ConvAnalysisActionListener analysisListener;
		private ScriptPanel scriptPanel;
		
		public Object generateSystemComponent() throws RJException, IOException {
			Map props = new HashMap();
			Object[] rows = table.getRows();
			for (int i = 0; i < rows.length; i++) {
				props.put(PROP_RULE_PREFIX + (i + 1), ((Object[])rows[i])[0]);
				props.put(PROP_RULE_SUBS + (i + 1), ((Object[])rows[i])[1]);
			}
			props.put(PROP_OUT_NAME, params.getParameterValue(PROP_OUT_NAME));
			props.put(PARAM_SCRIPT, scriptPanel.getScript());
			return new ReplaceConverter(params.getParameterValue(PROP_OUT_NAME), props, column);
		}

		public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
			
			this.column = (DataColumnDefinition) objects[0];
			AbstractDataSource source = (AbstractDataSource) objects[2];
			Window parent = (Window) objects[3];
			JDataSource jDataSource = (JDataSource)objects[4];
			analysisListener = new ConvAnalysisActionListener(parent, source, this, jDataSource);
			
			scriptPanel = new ScriptPanel(AbstractColumnConverter.getDefaultScript(ReplaceConverter.class), String.class, 
					new String[] {"column", "lookFor", "replaceWith"}, 
					new Class[] {String.class, String[].class, String[].class});
			
			String[] defaults = new String[] {getRestoredParam(PROP_OUT_NAME)};
			int n = 1;
			List regs = new ArrayList();
			while (true) {
				if (getRestoredParam(PROP_RULE_PREFIX + n) == null) {
					break;
				}
				regs.add(new Object[] {getRestoredParam(PROP_RULE_PREFIX + n), getRestoredParam(PROP_RULE_SUBS + n)});
				n++;
			}
			if (getRestoredParam(PARAM_SCRIPT) != null) {
				scriptPanel.setScript(getRestoredParam(PARAM_SCRIPT));
			}
			
			params = new ParamsPanel(new String[] {PROP_OUT_NAME}, new String[] {"Output attribute name"}, defaults);
			Map validators = new HashMap();
			validators.put(PROP_OUT_NAME, new CompoundValidator(new Validator[] {new NonEmptyValidator(), new ColumnNameValidator()}));
			params.setValidators(validators);
			
			table = new TablePanel(new String[] {"Regular expression", "Replace with"}, true);
			for (Iterator iterator = regs.iterator(); iterator.hasNext();) {
				Object[] object = (Object[]) iterator.next();
				table.addRow(object);
			}
			table.setBorder(BorderFactory.createTitledBorder("Replacement rules"));
			table.addAddButtonListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					ParamsPanel panel = new ParamsPanel(new String[] {PARAM_REGEX, PARAM_WITH}, new String[] {"Regular expression", "Replace with"});
					Map validators = new HashMap();
					validators.put(PARAM_REGEX, new NonEmptyValidator());
					//validators.put(PARAM_WITH, new NonEmptyValidator());
					panel.setValidators(validators);
					OptionDialog dialog = new OptionDialog(MainFrame.main, "Add new replacement rule");
					dialog.setMainPanel(panel);
					if (dialog.getResult() == OptionDialog.RESULT_OK) {
						table.addRow(new Object[] {panel.getParameterValue(PARAM_REGEX), panel.getParameterValue(PARAM_WITH)});
					}
				}
			});
			table.addEditButtonListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					int id = table.getSelectedRowId()[0];
					String[] defaults = new String[2];
					Object[] row = (Object[]) table.getSelectedRows()[0];
					defaults[0] = row[0].toString();
					defaults[1] = (row[1] == null ? null : row[1].toString());
					ParamsPanel panel = new ParamsPanel(new String[] {PARAM_REGEX, PARAM_WITH}, 
							new String[] {"Regular expression", "Replace with"}, defaults);
					Map validators = new HashMap();
					validators.put(PARAM_REGEX, new NonEmptyValidator());
					//validators.put(PARAM_WITH, new NonEmptyValidator());
					panel.setValidators(validators);
					OptionDialog dialog = new OptionDialog(MainFrame.main, "Edit replacement rule");
					dialog.setMainPanel(panel);
					if (dialog.getResult() == OptionDialog.RESULT_OK) {
						table.replaceRow(id, new Object[] {panel.getParameterValue(PARAM_REGEX), panel.getParameterValue(PARAM_WITH)});
					}
				}
			});
			table.addTablePropertyChangeListener(analysisListener);
			
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(params, BorderLayout.NORTH);
			panel.add(table, BorderLayout.CENTER);
			panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			panel.setPreferredSize(new Dimension(550, 300));
			
			visual = Configs.getAnalysisButton();
			//visual.setPreferredSize(new Dimension(visual.getPreferredSize().width, 20));
			JPanel visualWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
			visualWrapper.add(visual);
			panel.add(visualWrapper, BorderLayout.SOUTH);
			
			visual.addActionListener(analysisListener);
			
			JTabbedPane tabs = new JTabbedPane();
			tabs.addTab("Configuration", panel);
			tabs.addTab("Converter script (advanced)", scriptPanel);
			
			JPanel mainPanel = new JPanel(new BorderLayout());
			mainPanel.add(tabs, BorderLayout.CENTER);
			return mainPanel;
		}

		public Class getProducedComponentClass() {
			return ReplaceConverter.class;
		}

		public String toString() {
			return "Replace string conv.";
		}

		public boolean validate(JDialog dialog) {
			if (!params.doValidate()) {
				return false;
			}
			if (table.getRows().length == 0) {
				JOptionPane.showMessageDialog(MainFrame.main, "At least one replacement rule is required", "No replacement rule specified", JOptionPane.OK_OPTION);
				return false;
			}
			return true;
		}

		public void windowClosing(JDialog parent) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	
	private String[] rules;
	private String[] subs;
	private DataColumnDefinition[] in;
	private DataColumnDefinition[] out;
	
	private ScriptEvaluator scriptEvaluator;
	
	public ReplaceConverter(String columnName, Map props, DataColumnDefinition in) throws RJException {
		super(props);
		this.in = new DataColumnDefinition[] {in};
		this.out = new DataColumnDefinition[] {new ConverterColumnWrapper(columnName, in.getColumnType(), in.getSourceName())};
		out[0].setEmptyValues(in.getEmptyValues());
		List rules = new ArrayList();
		List newStrings = new ArrayList();
		int i = 1;
		do {
			String rule = (String) props.get(PROP_RULE_PREFIX + i);
			String with = (String) props.get(PROP_RULE_SUBS + i);
			if (rule == null && with == null) {
				break;
			} else if (rule == null) {
				throw new RJException("Replace rule cannot be null");
			} else {
				if (with == null) {
					with = "";
				}
				rules.add(rule);
				newStrings.add(with);
			}
			i++;
		} while (true);
		this.rules = (String[]) rules.toArray(new String[] {});
		this.subs = (String[]) newStrings.toArray(new String[] {});
		
		try {
			if (props.get(PARAM_SCRIPT) == null) {
				props.put(PARAM_SCRIPT, AbstractColumnConverter.getDefaultScript(ReplaceConverter.class));
			}
			scriptEvaluator = new ScriptEvaluator((String)props.get(PARAM_SCRIPT), String.class, 
					new String[] {"column", "lookFor", "replaceWith"}, 
					new Class[] {String.class, String[].class, String[].class});
		} catch (CompileException e) {
			throw new RJException("Compilation exception for script", e);
		} catch (ParseException e) {
			throw new RJException("Parse exception for script", e);
		} catch (ScanException e) {
			throw new RJException("Scan exception for script", e);
		}
	}

	public DataCell[] convert(DataCell[] dataCells) throws RJException {
		String val = dataCells[0].getValue().toString();
		try {
			val = (String)scriptEvaluator.evaluate(new Object[] {val, rules, subs});
		} catch (InvocationTargetException e) {
			throw new RJException("Error when executing converter script", e);
		}
		return new DataCell[] {new DataCell(dataCells[0].getValueType(), val)};
	}

	public DataColumnDefinition[] getExpectedColumns() {
		return in;
	}

	public DataColumnDefinition[] getOutputColumns() {
		return out;
	}
	
	public String getProperty(String propertyName) {
		super.getProperties().put(PROP_OUT_NAME, out[0].getColumnName());
		return super.getProperty(propertyName);
	}
	
	public Map getProperties() {
		super.getProperties().put(PROP_OUT_NAME, out[0].getColumnName());
		return super.getProperties();
	}

	public void saveToXML(Document doc, Element conv) {
		DOMUtils.setAttribute(conv, Configuration.NAME_ATTR, out[0].getColumnName());
		DOMUtils.setAttribute(conv, "column", in[0].getColumnName());
		saveEmptyValuesToXML(doc, conv, out);
		Configuration.appendParams(doc, conv, getProperties());
	}

//	public void updateName(String newName) {
//		setName(newName);
//		out = new DataColumnDefinition[] {new DataColumnDefinition(newName, out[0].getColumnType(), out[0].getSourceName())};
//	}
	
	public static AbstractColumnConverter fromXML(Element element, Map genericColumns) throws RJException {
		
		String name = DOMUtils.getAttribute(element, Configuration.NAME_ATTR);
		String columnName = DOMUtils.getAttribute(element, "column");
		Element paramsNode = DOMUtils.getChildElement(element, Configuration.PARAMS_TAG);
		Map params = Configuration.parseParams(paramsNode);
		DataColumnDefinition column = (DataColumnDefinition) genericColumns.get(columnName);
		column.setEmptyValues(getEmptyValues(readEmptyValues(element), 0));
		return new ReplaceConverter(name, params, column);
	}
	
	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new VisibleComponent();
	}
	
	public String toString() {
		return "Replace string conv.";
	}

}
