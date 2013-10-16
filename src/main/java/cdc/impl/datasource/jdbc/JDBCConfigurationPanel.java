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


package cdc.impl.datasource.jdbc;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;

import cdc.components.AbstractDataSource;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.paramspanel.DefaultParamPanelField;
import cdc.gui.components.paramspanel.DefaultParamPanelFieldCreator;
import cdc.gui.components.paramspanel.JDBCConnectionPanelField;
import cdc.gui.components.paramspanel.JDBCConnectionPanelFieldCreator;
import cdc.gui.components.paramspanel.ParamPanelField;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.validation.NonEmptyValidator;
import cdc.utils.RJException;

public class JDBCConfigurationPanel extends GUIVisibleComponent {

	private ParamsPanel panel;
	private JDBCConnectionPanelField jdbcField;
	private DefaultParamPanelField driverField;
	
	public JPanel getConfigurationPanel(Object[] params, int sizeX, int sizeY) {
		
		jdbcField = null;
		driverField = null;
		
		String[] availableparams = new String[] {AbstractDataSource.PARAM_SOURCE_NAME, JDBCDataSource.PARAM_URL, JDBCDataSource.PARAM_DRIVER, JDBCDataSource.PARAM_TABLE, JDBCDataSource.PARAM_TEST_SELECT, JDBCDataSource.PARAM_USER, JDBCDataSource.PARAM_PASSWORD};
		String[] defaults = new String[] {"jdbc-source", "", "", "", "", "", ""};
		for (int i = 0; i < defaults.length; i++) {
			if (getRestoredParam(availableparams[i]) != null) {
				defaults[i] = getRestoredParam(availableparams[i]);
				continue;
			}
			if (AbstractDataSource.PARAM_SOURCE_NAME.equals(availableparams[i])) {
				defaults[i] = (String)params[0];
			}
		}
		
		Map creators = new HashMap();
		creators.put(JDBCDataSource.PARAM_URL, new JDBCConnectionPanelFieldCreator1());
		creators.put(JDBCDataSource.PARAM_DRIVER, new DriverPanelFieldCreator());
		
		panel = new ParamsPanel(
				availableparams, 
				new String[] {"Source name", "Database location", "Driver class", "Source table", "Test select", "User name", "Password"}, 
				defaults, creators);
		
		Map validators = new HashMap();
		validators.put(AbstractDataSource.PARAM_SOURCE_NAME, new NonEmptyValidator());
		validators.put(JDBCDataSource.PARAM_DRIVER, new NonEmptyValidator());
		validators.put(JDBCDataSource.PARAM_TABLE, new NonEmptyValidator());
		validators.put(JDBCDataSource.PARAM_TEST_SELECT, new NonEmptyValidator());
		validators.put(JDBCDataSource.PARAM_URL, new NonEmptyValidator());
		panel.setValidators(validators);
		
		return panel;
	}

	public Object generateSystemComponent() throws RJException {
		AbstractDataSource source = new JDBCDataSource(
				panel.getParameterValue(AbstractDataSource.PARAM_SOURCE_NAME),
				panel.getParams());
		return source;
	}

	public String toString() {
		return "JDBC data source (MS SQL Server, Oracle etc.)";
	}

	public Class getProducedComponentClass() {
		return JDBCDataSource.class;
	}

	public boolean validate(JDialog dialog) {
		return panel.doValidate();
	}
	
	private class JDBCConnectionPanelFieldCreator1 extends JDBCConnectionPanelFieldCreator {
		public ParamPanelField create(JComponent parent, String param, String label, String defaultValue) {
			jdbcField = (JDBCConnectionPanelField) super.create(parent, param, label, defaultValue);
			if (jdbcField != null) {
				jdbcField.setDriverParamPanelField(driverField);
			}
			return jdbcField;
		}
	}
	
	private class DriverPanelFieldCreator extends DefaultParamPanelFieldCreator {
		public ParamPanelField create(JComponent parent, String param, String label, String defaultValue) {
			driverField = (DefaultParamPanelField) super.create(parent, param, label, defaultValue);
			if (jdbcField != null) {
				jdbcField.setDriverParamPanelField(driverField);
			}
			return driverField;
		}
	}

}
