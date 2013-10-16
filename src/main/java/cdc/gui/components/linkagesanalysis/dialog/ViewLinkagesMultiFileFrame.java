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

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import cdc.datamodel.DataColumnDefinition;
import cdc.gui.Configs;
import cdc.gui.components.linkagesanalysis.ThreadCreatorInterface;

public class ViewLinkagesMultiFileFrame extends JFrame {

	private LinkagesWindowPanel[] panels;
	
	public ViewLinkagesMultiFileFrame(String[] tabNames, DataColumnDefinition[][][] columns, boolean showDataSourceName, DataColumnDefinition[] confidenceColumn, DataColumnDefinition[] stratum, DataColumnDefinition[][][] comparedColumns, ThreadCreatorInterface[] threadCreator) {
		this(tabNames, columns, showDataSourceName, confidenceColumn, stratum, comparedColumns, threadCreator, false);
	}
	
	public ViewLinkagesMultiFileFrame(String[] tabNames, DataColumnDefinition[][][] columns, boolean showDataSourceName, DataColumnDefinition[] confidenceColumn, DataColumnDefinition stratum[], DataColumnDefinition[][][] comparedColumns, ThreadCreatorInterface[] threadCreator, boolean acceptRejectOption) {
	
		super.setIconImage(Configs.appIcon);
		
		//create multiple panels...
		panels = new LinkagesWindowPanel[tabNames.length];
		for (int i = 0; i < threadCreator.length; i++) {
			panels[i] = new LinkagesWindowPanel(this, columns[i], showDataSourceName, confidenceColumn[i], stratum[i], comparedColumns[i], null, threadCreator[i], acceptRejectOption);
		}
	
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (getDefaultCloseOperation() == JDialog.DO_NOTHING_ON_CLOSE) {
					return;
				}
				for (int i = 0; i < panels.length; i++) {
					panels[i].cancelThread();
				}
			}
			public void windowClosed(WindowEvent e) {
				for (int i = 0; i < panels.length; i++) {
					panels[i].cancelThread();
				}
			}
		});
		
		setSize(700, 550);
		setLayout(new BorderLayout());
		
		JTabbedPane tabs = new JTabbedPane();
		for (int i = 0; i < panels.length; i++) {
			panels[i].setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			tabs.addTab(tabNames[i], panels[i]);
		}
		add(tabs, BorderLayout.CENTER);
		
		setLocationRelativeTo(null);
		
	}

}
