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


package cdc.impl.datasource.text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JPanel;

import cdc.components.AbstractDataSource;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.paramspanel.FileChoosingPanelFieldCreator;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.components.paramspanel.SeparatorPanelFieldCreator;
import cdc.gui.validation.NonEmptyValidator;
import cdc.utils.RJException;

public class CSVConfigurationPanel extends GUIVisibleComponent {
	
	private static final String[] seps = new String[] {"\t", ";", ",", " "};
	private static final String[] labels = new String[] {"Tab", "Semicolon", "Comma", "Space"};

	
	private ParamsPanel panel;
	
	public Object generateSystemComponent() throws RJException, IOException {
		AbstractDataSource source = new CSVDataSource(
				panel.getParameterValue(AbstractDataSource.PARAM_SOURCE_NAME), 
				panel.getParams());
		return source;
	}
	
	public JPanel getConfigurationPanel(Object[] params, int sizeX, int sizeY) {
		
		Map creators = new HashMap();
		creators.put(CSVDataSource.PARAM_INPUT_FILE, new FileChoosingPanelFieldCreator(FileChoosingPanelFieldCreator.OPEN));
		creators.put(CSVDataSource.PARAM_DELIM, new SeparatorPanelFieldCreator(seps, labels, 2, 3));
		
		String[] availableparams = new String[] {AbstractDataSource.PARAM_SOURCE_NAME, CSVDataSource.PARAM_INPUT_FILE, CSVDataSource.PARAM_DELIM};
		String[] defaults = new String[] {"csv-source", "", ","};
		for (int i = 0; i < defaults.length; i++) {
			if (getRestoredParam(availableparams[i]) != null) {
				defaults[i] = getRestoredParam(availableparams[i]);
				continue;
			}
			if (AbstractDataSource.PARAM_SOURCE_NAME.equals(availableparams[i])) {
				defaults[i] = (String)params[0];
			}
		}
		
		panel = new ParamsPanel(
				availableparams, 
				new String[] {"Source name", "File name", "Column separator"}, 
				defaults, 
				creators
		);
		
		Map validators = new HashMap();
		validators.put(AbstractDataSource.PARAM_SOURCE_NAME, new NonEmptyValidator());
		validators.put(CSVDataSource.PARAM_INPUT_FILE, new NonEmptyValidator());
		panel.setValidators(validators);
		
		return panel;
	}

	public String toString() {
		return "CSV file source";
	}

	public Class getProducedComponentClass() {
		return CSVDataSource.class;
	}

	public boolean validate(JDialog dialog) {
		return panel.doValidate();
	}
}
