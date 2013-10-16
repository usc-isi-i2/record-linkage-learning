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


package cdc.gui.components.datasource.ui;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import cdc.datamodel.converters.AbstractColumnConverter;
import cdc.gui.OptionDialog;
import cdc.gui.components.datasource.JDataSource;
import cdc.gui.components.datasource.JDataSource.Brick;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.validation.ColumnNameValidator;
import cdc.gui.validation.CompoundValidator;
import cdc.gui.validation.NonEmptyValidator;
import cdc.gui.validation.Validator;
import cdc.utils.GuiUtils;
import cdc.utils.RowUtils;

public class RenameListener implements ActionListener {

	private Brick brick;
	private Window window;
	private JDataSource model;
	private AbstractColumnConverter converter;

	public RenameListener(Window parent, Brick b, JDataSource model, AbstractColumnConverter conv) {
		this.brick = b;
		this.window = parent;
		this.model = model;
		this.converter = conv;
	}
	
	public void actionPerformed(ActionEvent arg0) {
		
		ParamsPanel panel = new ParamsPanel(new String[] {"name"}, new String[] {"Column name"}, new String[] {brick.col.getColumnName()});
		Map validators = new HashMap();
		validators.put("name", new CompoundValidator(new Validator[] {new NonEmptyValidator(), new ColumnNameValidator()}));
		panel.setValidators(validators);
		
		OptionDialog dialog = new OptionDialog(window, "Rename column");
		dialog.setMainPanel(panel);
		dialog.pack();
		dialog.setLocation(GuiUtils.getCenterLocation(window, dialog));
		if (dialog.getResult() == OptionDialog.RESULT_OK) {
			//model.renameColumn(brick.col, panel.getParameterValue("name"));
			brick.col.setName(panel.getParameterValue("name"));
			
			RowUtils.fixConverter(converter, model, model.getIndexOf(converter));
			model.updateUI();
		}
		
	}

}
