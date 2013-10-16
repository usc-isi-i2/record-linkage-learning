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


package cdc.gui.components.paramspanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import cdc.gui.Configs;
import cdc.gui.components.dynamicanalysis.ChangedConfigurationListener;
import cdc.utils.GuiUtils;
import cdc.utils.StringUtils;
import cdc.utils.Utils;
import cdc.utils.Utils.Encoding;

public class FileChoosingPanelField extends ParamPanelField {

	public class DocumentChangedAction implements DocumentListener {
		private ChangedConfigurationListener listener;
		public DocumentChangedAction(ChangedConfigurationListener listener) {
				this.listener = listener;
		}
		public void changedUpdate(DocumentEvent arg0) {
			listener.configurationChanged();
		}
		public void insertUpdate(DocumentEvent arg0) {
			listener.configurationChanged();
		}
		public void removeUpdate(DocumentEvent arg0) {
			listener.configurationChanged();
		}
	}
	
	private Map listeners = new HashMap();
	private JPanel mainPanel;
	private JTextField field;
	private String userLabel;
	private JLabel error;
	private JLabel paramName;
	
	private int type;
	private JComboBox encodingCombo;
	private JLabel encodingLabel;
	private JLabel codingTextLabel;
	private JButton encodingButton;
	private JPanel encPanel;
	
	public FileChoosingPanelField(JComponent parent, int type, String param, String label, String defaultValue) {
		
		this.userLabel = label;
		this.type = type;
		
		mainPanel = new JPanel(new GridBagLayout());
		field = new JTextField(GuiUtils.EMPTY);
		
		field.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent arg0) {
				field.selectAll();
			}
			public void focusLost(FocusEvent arg0) {
				if (StringUtils.isNullOrEmpty(field.getText())) {
					field.setText(GuiUtils.EMPTY);
				}
			}
		});
		
		field.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent arg0) {
				handleEdit(arg0);
			}

			public void insertUpdate(DocumentEvent arg0) {
				handleEdit(arg0);
			}

			public void removeUpdate(DocumentEvent arg0) {
				handleEdit(arg0);
			}
			private void handleEdit(DocumentEvent evt) {
				if (evt.getDocument().toString().equals("")) {
					field.setText(GuiUtils.EMPTY);
				}
			}
		});
		
		paramName = new JLabel(label);
		//paramName.setPreferredSize(new Dimension(200, (int)paramName.getPreferredSize().getHeight()));
		field.setPreferredSize(new Dimension(200, (int)field.getPreferredSize().getHeight()));
		
		error = new JLabel(Configs.errorInfoIcon);
		error.setHorizontalAlignment(JLabel.CENTER);
		error.setVerticalAlignment(JLabel.CENTER);
		error.setVisible(false);
		error.setForeground(Color.red);
		JPanel errorPanel = new JPanel(null);
		error.setPreferredSize(new Dimension(20, 20));
		error.setBounds(0,0,20,20);
		errorPanel.add(error);
		errorPanel.setPreferredSize(new Dimension(20, 20));
		
		//panel.add(paramName);
		mainPanel.add(errorPanel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,10,0,0), 0, 0));
		mainPanel.add(field, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
		
		JButton button = new JButton("...");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File parent = new File(".");
				if (!field.getText().equals(GuiUtils.EMPTY)) {
					File f = new File(field.getText());
					if (f.getParentFile() != null) {
						parent = f.getParentFile();
					}
				}
				JFileChooser chooser = new JFileChooser(parent);	
				int retVal = (FileChoosingPanelField.this.type == FileChoosingPanelFieldCreator.OPEN ? chooser.showOpenDialog(null) : chooser.showSaveDialog(null));
				if (retVal == JFileChooser.APPROVE_OPTION) {
					field.setText(chooser.getSelectedFile().getAbsolutePath());
					testEncoding();
				}
			}
		});
		button.setPreferredSize(new Dimension(30, 20));
		mainPanel.add(button, new GridBagConstraints(2, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
	
		field.getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {
				testEncoding();
			}
			public void insertUpdate(DocumentEvent e) {
				testEncoding();
			}
			public void changedUpdate(DocumentEvent e) {
				testEncoding();
			}
		});
		
		encodingButton = new JButton("Modify");
		encodingButton.setPreferredSize(new Dimension(encodingButton.getPreferredSize().width, 20));
		encodingButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				codingTextLabel.setText("Encoding: ");
				encodingLabel.setVisible(false);
				encodingCombo.setVisible(true);
				encodingButton.setEnabled(false);
				encPanel.validate();
				//encPanel.repaint();
			}
		});
		
		codingTextLabel = new JLabel("Encoding (auto): ");
		encodingCombo = new JComboBox(Utils.SUPPORTED_ENCODINGS);
		encodingCombo.setSelectedItem(Utils.DEFAULT_ENCODING);
		encodingLabel = new JLabel("ASCII");
		encPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		encPanel.add(codingTextLabel);
		encPanel.add(encodingLabel);
		encPanel.add(encodingCombo);
		encodingCombo.setVisible(false);
		mainPanel.add(encPanel, new GridBagConstraints(0, 1, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,30,0,0), 0, 0));
		mainPanel.add(encodingButton, new GridBagConstraints(2, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
		setValue(defaultValue);
	}
	
	private void testEncoding() {
		try {
			Encoding e = Utils.recognizeEncoding(new File(field.getText()));
			encodingCombo.setSelectedItem(e);
			encodingLabel.setText(e.toString());
		} catch (IOException ex) {
			//encodingCombo.setSelectedItem(Utils.DEFAULT_ENCODING);
			//encodingLabel.setText(Utils.DEFAULT_ENCODING.toString());
		}
		
	}
	
	public JComponent getComponentInputField() {	
		return mainPanel;
	}
	
	public JComponent getComponentLabel() {
		return paramName;
	}

	public String getValue() {
		if (field.getText().equals(GuiUtils.EMPTY)) {
			return null;
		}
		if (!encodingCombo.getSelectedItem().equals(Utils.DEFAULT_ENCODING)) {
			return field.getText() + "#ENC=" + ((Encoding)encodingCombo.getSelectedItem()).getCharset() + "#";
		} else {
			return field.getText();
		}
	}

	public void setValue(String val) {
		if (!StringUtils.isNullOrEmpty(val)) {
			String[] parsedFile = Utils.parseFilePath(val);
			if (parsedFile.length == 1) {
				this.field.setText(parsedFile[0]);
			} else {
				this.field.setText(parsedFile[0]);
				this.encodingLabel.setText(Utils.getEncodingForName(parsedFile[1]).toString());
				this.encodingCombo.setSelectedItem(Utils.getEncodingForName(parsedFile[1]));
			}
		}
	}

	public String getUserLabel() {
		return userLabel;
	}
	
	public void error(String message) {
		if (message != null) {
			error.setVisible(true);
			error.setToolTipText(message);
			field.setBackground(Color.RED);
		} else {
			error.setVisible(false);
			field.setBackground(Color.WHITE);
		}
	}

	public void addConfigurationChangeListener(ChangedConfigurationListener configurationListener) {
		DocumentChangedAction l = new DocumentChangedAction(configurationListener);
		listeners.put(configurationListener, l);
		field.getDocument().addDocumentListener(l);
	}
	
	public void removeConfigurationChangeListener(ChangedConfigurationListener listener) {
		field.getDocument().removeDocumentListener((DocumentListener) listeners.remove(listener));
	}

}
