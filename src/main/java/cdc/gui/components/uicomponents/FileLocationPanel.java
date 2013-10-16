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


package cdc.gui.components.uicomponents;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cdc.gui.components.paramspanel.FileChoosingPanelFieldCreator;

public class FileLocationPanel extends JPanel {

	public static final int SAVE = FileChoosingPanelFieldCreator.SAVE;
	public static final int OPEN = FileChoosingPanelFieldCreator.OPEN;
	
	private JTextField file;
	private JButton button;
	private int type;
	private JLabel fileLabel;
	
	public FileLocationPanel(String fLabel, String defFileName, int inputWidth, int type) {
		this.type = type;
		setLayout(new FlowLayout(FlowLayout.LEFT));
		fileLabel = new JLabel(fLabel);
		file = new JTextField("", 20);
		button = new JButton("...");
		button.setPreferredSize(new Dimension(30, 20));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser(new File("."));	
				int retVal = FileLocationPanel.this.type == FileChoosingPanelFieldCreator.OPEN ? chooser.showOpenDialog(null) : chooser.showSaveDialog(null);
				if (retVal == JFileChooser.APPROVE_OPTION) {
					file.setText(chooser.getSelectedFile().getAbsolutePath());
				}
			}
		});
		if (defFileName != null) {
			file.setText(defFileName);
		}
		add(fileLabel);
		add(file);
		add(button);
	}

	public FileLocationPanel(String fLabel, String defFileName, int inputWidth) {
		this(fLabel, defFileName, inputWidth, OPEN);
	}
	
	public FileLocationPanel(String fLabel, String defFileName) {
		this(fLabel, defFileName, 20, OPEN);
	}
	
	public FileLocationPanel(String fLabel) {
		this(fLabel, null);
	}

	public String getFileName() {
		return file.getText().trim();
	}
	
	public void setFileName(String loc) {
		file.setText(loc);
	}
	
	public void setEnabled(boolean enabled) {
		file.setEditable(enabled);
		button.setEnabled(enabled);
		fileLabel.setEnabled(enabled);
	}

	public void setError(boolean error) {
		if (error) {
			file.setBackground(Color.red);
		} else {
			file.setBackground(Color.white);
		}
	}

}
