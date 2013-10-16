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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import cdc.gui.OptionDialog;
import cdc.utils.Props;
import cdc.utils.RJException;

public class JDBCConfigurationPanel extends JPanel {
	
	private static final String DB = Props.getString("supported-databases");
	private static final String[] FIELD_LABELS = {"Host address", "Port number", "Database name"};
	
	private static final String PROTOCOL = "$protocol$";
	private static final String HOST = "$host$";
	private static final String PORT = "$port$";
	private static final String DBNAME = "$db$";
	
	private static final String[] params = new String[] {HOST, PORT, DBNAME};
	
	private String[][] dbParsed;
	private String[] protocols;
	private String[] formats;
	
	private JComboBox dbType;
	private JComponent[] fields;
	private String[] drivers;
	
	public JDBCConfigurationPanel() {
		initialize();
	}
	
	public JDBCConfigurationPanel(String connString) throws RJException {
		initialize();
		if (connString != null) {
			parse(connString);
		}
	}

	private void selectionChanged() {
		int i = dbType.getSelectedIndex();
		fields = new JComponent[3];
		fields[0] = new JTextField();
		if ("0".equals(dbParsed[i][2])) {
			fields[0].setEnabled(false);
		}
		fields[0].setPreferredSize(new Dimension(150, 20));
		SpinnerNumberModel model = new SpinnerNumberModel(1433, 1, 65536, 1);
		fields[1] = new JSpinner(model);
		((JSpinner)fields[1]).setEditor(new JSpinner.NumberEditor((JSpinner)fields[1], "#"));
		if ("0".equals(dbParsed[i][3])) {
			fields[1].setEnabled(false);
		}
		fields[2] = new JTextField();
		if ("0".equals(dbParsed[i][4])) {
			fields[i].setEnabled(false);
		}
		fields[2].setPreferredSize(new Dimension(150, 20));
		
		removeAll();
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.anchor = GridBagConstraints.WEST;
		add(new JLabel("Database type"), c);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 0;
		c.insets = new Insets(5, 5, 0 ,0);
		c.fill = GridBagConstraints.BOTH;
		add(dbType, c);
		
		for (int j = 0; j < fields.length; j++) {
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = j + 1;
			c.weightx = 0;
			c.anchor = GridBagConstraints.WEST;
			add(new JLabel(FIELD_LABELS[j], JLabel.LEFT), c);
			
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = j + 1;
			c.weightx = 0;
			c.fill = GridBagConstraints.BOTH;
			c.insets = new Insets(5, 5, 0 ,0);
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			panel.add(fields[j]);
			//panel.add(Box.createRigidArea(new Dimension(10, 10)));
			add(panel, c);
			
			c = new GridBagConstraints();
			c.gridx = 2;
			c.gridy = j + 1;
			c.weightx = 1;
			c.fill = GridBagConstraints.BOTH;
			add(Box.createRigidArea(new Dimension(10, 10)), c);
		}
		
		updateUI();
	}
	
	private void initialize() {
		String[] dbs = DB.split(";");
		String[] labels = new String[dbs.length];
		protocols = new String[dbs.length];
		formats = new String[dbs.length];
		dbParsed = new String[dbs.length][];
		drivers = new String[dbs.length];
		for (int i = 0; i < labels.length; i++) {
			String[] db = dbs[i].split(",");
			dbParsed[i] = db;
			labels[i] = db[0];
			protocols[i] = db[1];
			formats[i] = db[5];
			if (db.length > 6) {
				drivers[i] = db[6];
			} else {
				drivers[i] = null;
			}
			
		}
		
		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		dbType = new JComboBox(labels);
		dbType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				selectionChanged();
			}});
		dbType.setSelectedIndex(0);
	}

	private void parse(String connString) throws RJException {
		try {
			for (int i = 0; i < protocols.length; i++) {
				if (connString.startsWith(protocols[i])) {
					String connection = connString.substring(protocols[i].length(), connString.length());
					String connectionFormat = formats[i].substring(PROTOCOL.length(), formats[i].length());
					dbType.setSelectedIndex(i);
					int dollar;
					while ((dollar = connectionFormat.indexOf('$')) != -1) {
						String next = connectionFormat.substring(0, dollar);
						connectionFormat = connectionFormat.substring(next.length(), connectionFormat.length());
						connection = connection.substring(next.length(), connection.length());
						if (connectionFormat.indexOf('$') != -1) {
							String tmp = connectionFormat.substring(connectionFormat.indexOf('$', 1) + 1, connectionFormat.length());
							if (tmp.indexOf('$') != -1) {
								next = tmp.substring(0, tmp.indexOf('$'));
							} else {
								next = tmp;
							}
						} else {
							next = connection;
						}
						
						String paramName = connectionFormat.substring(0, connectionFormat.indexOf('$', 1) + 1);
						String paramValue = connection.substring(0, next.equals("") ? connection.length() : connection.indexOf(next));
						for (int j = 0; j < params.length; j++) {
							if (paramName.equals(params[j])) {
								setValue(j, paramValue);
							}
						}
						connection = connection.substring(paramValue.length(), connection.length());
						connectionFormat = connectionFormat.substring(paramName.length(), connectionFormat.length());
					}
					return;
				}
			}
			throw new RJException("Error parsing connection string. Protocol not found. See misc.properties for list of supported protocols.");
		} catch (Exception e) {
			throw new RJException("Error parsing connection string.", e);
		}
	}
	
	private void setValue(int j, String paramValue) {
		if (j == 1) {
			((JSpinner)fields[1]).setValue(new Integer(paramValue));
		} else {
			((JTextField)fields[j]).setText(paramValue);
		}
	}

	public String getConnectionString() {
		int selected = dbType.getSelectedIndex();
		return formats[selected].replace(HOST, value(0)).replace(PORT, value(1)).replace(DBNAME, value(2)).replace(PROTOCOL, protocols[selected]);
	}

	private String value(int i) {
		if (i == 1) {
			return ((JSpinner)fields[1]).getValue().toString();
		}
		return ((JTextField)fields[i]).getText();
	}
	
	public static void main(String[] args) throws RJException {
		OptionDialog dialog = new OptionDialog((JDialog)null, "JDBC configuration");
		System.out.println(new File(".").toURI());
		JDBCConfigurationPanel panel = new JDBCConfigurationPanel("jdbc:jtds:sqlserver://aaaa.com:1398/data");
		dialog.setMainPanel(panel);
		dialog.setVisible(true);
		if (dialog.getResult() == OptionDialog.RESULT_OK) {
			System.out.println(panel.getConnectionString());
		}
	}

	public String getDriver() {
		return drivers[this.dbType.getSelectedIndex()];
	}
}
