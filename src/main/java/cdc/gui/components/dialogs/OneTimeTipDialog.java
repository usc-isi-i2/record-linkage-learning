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


package cdc.gui.components.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import cdc.gui.MainFrame;

public class OneTimeTipDialog extends JDialog {

	public static final String LINKAGE_RESULTS_VIEWER = "linkage-viewer-first-time-warning";
	public static final String LINKAGE_RESULTS_VIEWER_MESSAGE = "In addition to using the FRIL-integrated results viewer,\nyou can always locate the file containing linkage results\nand open it in any program that accepts csv files.\n \nThe file containing linkage results is configured in result\nsaver configuration.";
	
	public static final String MINUS_VIEWER = "minus-first-time-warning";
	public static final String MINUS_VIEWER_MESSAGE = "In addition to using the FRIL-integrated viewer for not joined data,\nyou can always locate the file containing the data and open it in any\nprogram that accepts csv files.\n \nThe file containing not joined data is located in the same directory as\nresults of the linkage. The file name starts with 'minus-', followed by\nthe source name.";
	
	public static final String LINKAGE_MODE_DEFAULT = "linkage-mode-first-time-warning";
	public static final String LINKAGE_MODE_MESSAGE = "The default mode of FRIL is the linkage mode. If you want to\ndeduplicate the data, you can switch the mode to deduplication.\n \nTo switch between the modes, choose between options in Mode\nmenu item.";
	
	public static final String DEDUPLICATED_VIEWER = "linkage-viewer-deduplicated-data-warning";
	public static final String DEDUPLICATED_VIEWER_MESSAGE = "In addition to using the FRIL-integrated viewer for the data,\nyou can locate the file containing the data and open it in any\nprogram that accepts csv files.\n \nThe file containing deduplicated data is configured in result\nsaver configuration.";
	
	public static final String DUPLICATES_VIEWER = "linkage-viewer-duplicates-warning";
	public static final String DUPLICATES_VIEWER_MESSAGE =   "In addition to using the FRIL-integrated viewer for the data,\nyou can always locate the file containing the duplicates and\nopen it in any program that accepts csv files.\n \nThe file containing deduplicates is configured in data source\nconfiguration.";
	
	public static final String MEMORY_DIALOG = "There might be insufficient memory available for Java.\n \nConsider changing the value of parameter Xmx in startup\nscript (the file you used to start FRIL).";
	public static final String MEMORY = "memory-message";
	
	public static final String NOT_SAVED_COMPARED_COLUMNS_DIALOG = "Some columns that are used in the linkage condition are\nnot saved into the results file.\nThese columns will not be visible in the results viewer.";
	public static final String NOT_SAVED_COMPARED_COLUMNS = "not-saved-compared-columns";
	
	private JButton button;
	private JCheckBox notShowCheck;
	
	public OneTimeTipDialog(Window parent, String title, String message) {
		super(parent, title);
		super.setModal(true);
		super.setResizable(false);
		
		Icon icon = UIManager.getIcon("OptionPane.informationIcon");
		JPanel messagePanel = new JPanel(new GridBagLayout());
		String[] msgs = message.split("\n");
		for (int i = 0; i < msgs.length; i++) {
			JLabel label = new JLabel(msgs[i], JLabel.LEFT);
			messagePanel.add(label, new GridBagConstraints(0, i, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		}
		
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(new JLabel(icon), new GridBagConstraints(0, 0, 1, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(20, 20, 20, 20), 0, 0));
		panel.add(messagePanel, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(15, 0, 15, 15), 0, 0));
		
		
		JPanel buttons = new JPanel(new GridBagLayout());
		notShowCheck = new JCheckBox("Do not show this message in future");
		buttons.add(notShowCheck, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		button = new JButton("OK");
		buttons.add(button, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		panel.add(buttons, new GridBagConstraints(0, 1, 2, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 15, 15, 15), 0, 0));
		add(panel);
		
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				OneTimeTipDialog.this.setVisible(false);
			}
		});
		
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		
		pack();
		super.setLocationRelativeTo(parent);
	}
	
	private boolean shouldStop() {
		return notShowCheck.isSelected();
	}

	public static void showInfoDialogIfNeeded(String paramName, String message) {
		showInfoDialogIfNeeded("Information", paramName, message);
	}
	
	public static void showInfoDialogIfNeeded(String title, String paramName, String message) {
		showInfoDialogIfNeeded(MainFrame.main, title, paramName, message);
	}

	public static void showInfoDialogIfNeeded(Window parent, String title, String paramName, String message) {
		if (MainFrame.main.getPersistentParam(paramName) != null && MainFrame.main.getPersistentParam(paramName).equals("true")) {
			return;
		}
		
		OneTimeTipDialog dialog = new OneTimeTipDialog(parent, title, message);
		dialog.setVisible(true);
		MainFrame.main.setPersistentParam(paramName, String.valueOf(dialog.shouldStop()));
	}
	
}
