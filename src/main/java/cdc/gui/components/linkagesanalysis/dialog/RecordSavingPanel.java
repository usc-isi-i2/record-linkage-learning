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

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import cdc.gui.components.uicomponents.FileLocationPanel;

public class RecordSavingPanel extends JPanel {

	public static final int RESULT_OK = 1;
	public static final int RESULT_CANCEL = 1;
	
	private FileLocationPanel filePanel = new FileLocationPanel("File name:", "saved.csv", 20, FileLocationPanel.SAVE);
	private JRadioButton buttonAll;
	private JRadioButton buttonCur;
	
	public RecordSavingPanel() {
		
		setLayout(new GridBagLayout());
		
		filePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(filePanel, new GridBagConstraints(0, 0, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		
		JLabel saveType = new JLabel("Save:");
		buttonAll = new JRadioButton("All records");
		buttonCur = new JRadioButton("Only records from current page");
		
		ButtonGroup group = new ButtonGroup();
		group.add(buttonAll);
		group.add(buttonCur);
		buttonCur.setSelected(true);
		
		add(saveType, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		add(buttonAll, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		add(buttonCur, new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		
	}

	public String getFileName() {
		return filePanel.getFileName();
	}

	public boolean isSaveAll() {
		return buttonAll.isSelected();
	}

}
