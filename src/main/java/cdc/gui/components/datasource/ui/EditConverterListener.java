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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.JDialog;
import javax.swing.JFrame;

import cdc.datamodel.converters.AbstractColumnConverter;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.OptionDialog;
import cdc.gui.components.datasource.JDataSource;
import cdc.gui.components.datasource.JDataSource.Brick;
import cdc.gui.external.JXErrorDialog;
import cdc.utils.RJException;
import cdc.utils.RowUtils;

public class EditConverterListener implements ActionListener {

	private JDataSource model;
	private AbstractColumnConverter createdConverter;
	private Brick inColumn;
	
	public EditConverterListener(JDataSource.Brick inColumn, JDataSource model, AbstractColumnConverter copy) {
		this.createdConverter = copy;
		this.model = model;
		this.inColumn = inColumn;
	}
	
	public void actionPerformed(ActionEvent e) {
		
		OptionDialog dialog;
		
		Window parent  = getParentFrame(model);
		if (parent instanceof JDialog) {
			dialog = new OptionDialog((JDialog)parent, "Configure converter");
		} else {
			dialog = new OptionDialog((JFrame)parent, "Configure converter");
		}
		GUIVisibleComponent converter = readGUIConponent();
		converter.restoreValues(createdConverter);
		dialog.setMainPanel(converter.getConfigurationPanel(
				new Object[] {inColumn.col, model.getColumnsForConverter(createdConverter), model.getDataSource(), dialog, model}, -1, -1));
		dialog.addOptionDialogListener(converter);
		int result = dialog.getResult();
		
		if (result == OptionDialog.RESULT_CANCEL) {
			return;
		}
		try {
			int index = model.getIndexOf(createdConverter);
			AbstractColumnConverter newConv = (AbstractColumnConverter) converter.generateSystemComponent();
			fixConverterOutColumns(newConv, index);
			model.replaceConnection(index, newConv);
		} catch (RJException ex) {
			JXErrorDialog.showDialog(parent, ex.getMessage(), ex);
			return;
		} catch (IOException ex) {
			JXErrorDialog.showDialog(parent, ex.getMessage(), ex);
			return;
		}
		
	}
	
	private GUIVisibleComponent readGUIConponent() {
		try {
			Method m = createdConverter.getClass().getMethod("getGUIVisibleComponent", new Class[] {});
			return (GUIVisibleComponent) m.invoke(null, new Object[] {});
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private void fixConverterOutColumns(AbstractColumnConverter conv, int index) {
		RowUtils.fixConverter(conv, model, index);
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
