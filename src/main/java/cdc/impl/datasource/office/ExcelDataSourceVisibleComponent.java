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


package cdc.impl.datasource.office;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JPanel;

import cdc.components.AbstractDataSource;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.paramspanel.FileChoosingPanelFieldCreator;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.validation.NonEmptyValidator;
import cdc.impl.datasource.office.paramspanel.SheetChooserCreator;
import cdc.utils.RJException;

public class ExcelDataSourceVisibleComponent extends GUIVisibleComponent {
	
	private ParamsPanel panel;
	
	public Object generateSystemComponent() throws RJException, IOException {
		AbstractDataSource source = new ExcelDataSource(
				panel.getParameterValue(AbstractDataSource.PARAM_SOURCE_NAME), 
				panel.getParams());
		return source;
	}
	
	public JPanel getConfigurationPanel(Object[] params, int sizeX, int sizeY) {
		
		Map creators = new HashMap();
		creators.put(ExcelDataSource.PARAM_FILE, new FileChoosingPanelFieldCreator(FileChoosingPanelFieldCreator.OPEN));
		creators.put(ExcelDataSource.PARAM_SHEET, new SheetChooserCreator());
		
		String[] availableparams = new String[] {AbstractDataSource.PARAM_SOURCE_NAME, ExcelDataSource.PARAM_FILE, ExcelDataSource.PARAM_SHEET};
		String[] defaults = new String[] {"excel-source", "", ""};
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
				new String[] {"Source name", "File name", "Sheet name"}, 
				defaults, 
				creators
		);
		
		Map validators = new HashMap();
		validators.put(AbstractDataSource.PARAM_SOURCE_NAME, new NonEmptyValidator());
		validators.put(ExcelDataSource.PARAM_FILE, new NonEmptyValidator());
		//validators.put(ExcelDataSource.PARAM_SHEET, new NonEmptyValidator());
		panel.setValidators(validators);
		
		return panel;
	}

	public String toString() {
		return "Excel file source";
	}

	public Class getProducedComponentClass() {
		return ExcelDataSource.class;
	}

	public boolean validate(JDialog dialog) {
		return panel.doValidate();
	}
}
