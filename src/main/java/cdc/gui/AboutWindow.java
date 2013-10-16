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


package cdc.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class AboutWindow extends JDialog {
	
	public AboutWindow() {
		super(MainFrame.main);
		setModal(true);
		setTitle("About FRIL");
		setSize(600, 400);
		setLocationRelativeTo(MainFrame.main);
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		JPanel main = new JPanel(new GridBagLayout());
		main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(main);
		
		main.add(new JLabel("FRIL: A Fine-Grained Record Integration and Linkage Tool"), new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 5));
		main.add(new JLabel("Version: " + MainFrame.main.getPropertiesVersion().getProperty(MainFrame.VERSION_PROPERTY_V)), new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 5));
		main.add(new JLabel("Author: Pawel Jurczyk"), new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 5));
		main.add(new JLabel("List of changes:"), new GridBagConstraints(0, 3, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 5));
		
		String historyText = readHistoryText(MainFrame.VERSION_LIST_OF_CHANGES_FILE);
		JTextArea area = new JTextArea(historyText);
		area.setEditable(false);
		JScrollPane scroll = new JScrollPane(area);
		main.add(scroll, new GridBagConstraints(0, 4, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
	}

	private String readHistoryText(String versionListOfChangesFile) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(versionListOfChangesFile));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line).append("\n");
			}
			return builder.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return "Error reading changelog file: " + versionListOfChangesFile;
		}
	}
	
}
