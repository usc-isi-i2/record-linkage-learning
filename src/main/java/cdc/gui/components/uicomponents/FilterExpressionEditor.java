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
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import cdc.components.AbstractDataSource;
import cdc.components.Filter;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.OptionDialog;
import cdc.gui.components.paramspanel.ComboBoxPanelFieldCreator;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.external.JXErrorDialog;
import cdc.utils.RJException;

public class FilterExpressionEditor extends JPanel {
	
	private static final String[] CONDITIONS = {"==", "!=", "<=", "<", ">", ">="};
	
	private JTextArea filter;
	private JButton addFilterExpr;
	private JButton validateFilterExpr;
	private JButton clearFilter;
	private JDialog activeWizard;
	
	private DataColumnDefinition[] columns;
	
	public FilterExpressionEditor(JDialog parent, AbstractDataSource src) {
		this(parent, src.getDataModel().getOutputFormat(), src.getFilter());
	}
	
	public FilterExpressionEditor(JDialog parent, DataColumnDefinition[] col, Filter oldFilter) {
		
		this.activeWizard = parent;
		this.columns = col;
	
		filter = new JTextArea();
		JScrollPane scrollFilter = new JScrollPane(filter);
		//filter.setBorder(BorderFactory.createEtchedBorder());
		
		addFilterExpr = new JButton("Add filter expression");
		addFilterExpr.setPreferredSize(new Dimension(addFilterExpr.getPreferredSize().width, 20));
		addFilterExpr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Map listeners = new HashMap();
				listeners.put("attribute", new ComboBoxPanelFieldCreator(getAttributes()));
				listeners.put("condition", new ComboBoxPanelFieldCreator(CONDITIONS));
				ParamsPanel panel = new ParamsPanel(new String[] {"attribute", "condition", "value"}, new String[] {"Attribute", "Condition", "Value"}, listeners);
				OptionDialog dialog = new OptionDialog(activeWizard, "Add filter expression");
				dialog.setMainPanel(panel);
				if (dialog.getResult() == OptionDialog.RESULT_OK) {
					if (!filter.getText().trim().isEmpty()) {
						filter.append(" and ");
					}
					filter.append("(");
					String col = panel.getParameterValue("attribute");
					String con = panel.getParameterValue("condition");
					String val = panel.getParameterValue("value");
					if (col.indexOf(' ') != -1) {
						col = "\"" + col + "\"";
					}
					try {
						Double.parseDouble(val);
					} catch (NumberFormatException ex) {
						val = "\"" + val + "\"";
					}
					filter.append(col);
					filter.append(" ");
					filter.append(con);
					filter.append(" ");
					filter.append(val);
					filter.append(")");
				}
			}

		});
		clearFilter = new JButton("Clear filter");
		clearFilter.setPreferredSize(new Dimension(clearFilter.getPreferredSize().width, 20));
		clearFilter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				filter.setText("");
			}
		});
		validateFilterExpr = new JButton("Validate filter");
		validateFilterExpr.setPreferredSize(new Dimension(validateFilterExpr.getPreferredSize().width, 20));
		validateFilterExpr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Filter f = new Filter(filter.getText(), columns);
					filter.setText(f.toString());
					JOptionPane.showMessageDialog(FilterExpressionEditor.this, "Filter expression is correct.");
				} catch (RJException e1) {
					JXErrorDialog.showDialog(activeWizard, "Error in filter expression", e1);
				}
			}
		});
		
		if (oldFilter != null) {
			setEnabled(true);
			filter.setText(fix(oldFilter).toString());
		} else {
			setEnabled(false);
		}
		
		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonsPanel.add(addFilterExpr);
		buttonsPanel.add(clearFilter);
		buttonsPanel.add(validateFilterExpr);
		
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.insets = new Insets(0, 5, 0, 5);
		this.add(scrollFilter, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets(0, 0, 5, 0);
		this.add(buttonsPanel, c);
	}
	
	private Filter fix(Filter filter) {
		try {
			filter.removeNonExistentColumns(columns);
			return filter;
		} catch (RJException e) {
			JXErrorDialog.showDialog(activeWizard, "Error when compiling filter condition", e);
			return null;
		}
	}

	private String[] getAttributes() {
		String[] attrs = new String[columns.length];
		for (int i = 0; i < attrs.length; i++) {
			attrs[i] = columns[i].getColumnName();
		}
		return attrs;
	}
	
	public void setEnabled(boolean enabled) {
		filter.setEnabled(enabled);
		addFilterExpr.setEnabled(enabled);
		clearFilter.setEnabled(enabled);
		validateFilterExpr.setEnabled(enabled);
	}
	
	public Filter getFilter() throws RJException {
		String filterExpression = filter.getText();
		return new Filter(filterExpression, columns);
	}

}
