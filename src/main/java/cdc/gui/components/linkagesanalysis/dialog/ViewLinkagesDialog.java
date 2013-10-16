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


package cdc.gui.components.linkagesanalysis.dialog;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;

import cdc.components.AbstractDistance;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.components.linkagesanalysis.ThreadCreatorInterface;

public class ViewLinkagesDialog extends JDialog {
	
	private LinkagesWindowPanel panel;
	
	public ViewLinkagesDialog(DataColumnDefinition[][] columns, boolean showDataSourceName, DataColumnDefinition confidenceColumn, DataColumnDefinition stratum, DataColumnDefinition[][] comparedColumns, AbstractDistance[] dst, ThreadCreatorInterface threadCreator) {
		this(columns, showDataSourceName, confidenceColumn, stratum, comparedColumns, dst, threadCreator, false);
	}
	
	public ViewLinkagesDialog(DataColumnDefinition[][] columns, boolean showDataSourceName, DataColumnDefinition confidenceColumn, DataColumnDefinition stratum, DataColumnDefinition[][] comparedColumns, AbstractDistance[] dst, ThreadCreatorInterface threadCreator, boolean acceptRejectOption) {
	
		setModal(true);
	
		panel = new LinkagesWindowPanel(this, columns, showDataSourceName, confidenceColumn, stratum, comparedColumns, dst, threadCreator, acceptRejectOption);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (getDefaultCloseOperation() == JDialog.DO_NOTHING_ON_CLOSE) {
					return;
				}
				panel.cancelThread();
			}
			public void windowClosed(WindowEvent e) {
				panel.cancelThread();
			}
		});
		
		setSize(700, 500);
		setLayout(new BorderLayout());
		//add(panel.getToolBar(), BorderLayout.PAGE_START);
		add(panel, BorderLayout.CENTER);
		//add(panel.getStatusBar(), BorderLayout.SOUTH);
		
		setLocationRelativeTo(null);
		
	}

	public LinkagesWindowPanel getLinkageWindowPanel() {
		return panel;
	}
	
}
