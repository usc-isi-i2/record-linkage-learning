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

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.JDialog;
import javax.swing.JFrame;

import cdc.datamodel.converters.AbstractColumnConverter;
import cdc.datamodel.converters.DummyConverter;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.OptionDialog;
import cdc.gui.components.datasource.JDataSource;
import cdc.gui.external.JXErrorDialog;
import cdc.utils.RJException;
import cdc.utils.RowUtils;

public class AddOutModelListener implements ActionListener {

	private JDataSource model;
	private JDataSource.Brick inColumn;
	private GUIVisibleComponent  converter;
	
	public AddOutModelListener(JDataSource model, JDataSource.Brick inColumn, GUIVisibleComponent converter) {
		this.inColumn = inColumn;
		this.converter = converter;
		this.model = model;
	}
	
	public AddOutModelListener(JDataSource model, JDataSource.Brick inColumn) {
		this.inColumn = inColumn;
		this.model = model;
	}
	
	public void actionPerformed(ActionEvent e) {
		
		if (this.converter == null) {
			//DataColumnDefinition newCol = findNonConflicting(inColumn.col);
			AbstractColumnConverter converter = new DummyConverter(inColumn.col.getColumnName(), inColumn.col, inColumn.col, new HashMap());
			fixConverterOutColumns(converter);
			model.addConnection(converter);
		} else {
			OptionDialog dialog;
			
			Window parent  = getParentFrame(model);
			if (parent instanceof JDialog) {
				dialog = new OptionDialog((JDialog)parent, "Configure converter");
			} else {
				dialog = new OptionDialog((JFrame)parent, "Configure converter");
			}
			dialog.setMainPanel(this.converter.getConfigurationPanel(
					new Object[] {inColumn.col, model.getColumnsForConverter(null), model.getDataSource(), dialog, model}, -1, -1));
			dialog.addOptionDialogListener(this.converter);
			int result = dialog.getResult();
			
			if (result == OptionDialog.RESULT_CANCEL) {
				return;
			}
			try {
				AbstractColumnConverter createdConverter = (AbstractColumnConverter) this.converter.generateSystemComponent();
				fixConverterOutColumns(createdConverter);
				model.addConnection(createdConverter);
			} catch (RJException ex) {
				JXErrorDialog.showDialog(parent, ex.getMessage(), ex);
				return;
			} catch (IOException ex) {
				JXErrorDialog.showDialog(parent, ex.getMessage(), ex);
				return;
			}
		}
		
	}
	
	private void fixConverterOutColumns(AbstractColumnConverter conv) {
		RowUtils.fixConverter(conv, model);
	}


	private Window getParentFrame(Component c) {
		while (!(c instanceof Window)) {
			if (c.getParent() == null) {
				return null;
			} else {
				return getParentFrame(c.getParent());
			}
		}
		return (Window)c;
	}

}
