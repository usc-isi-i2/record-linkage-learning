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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

import cdc.datamodel.DataColumnDefinition;
import cdc.gui.Configs;
import cdc.gui.components.table.TablePanel;

public class ColumnConfigDialog extends AbstractColumnConfigDialog {

	private int result = RESULT_CANCEL;
	
	private TablePanel comparedAttributes;
	private TablePanel leftSrcAttributes;
	private TablePanel rightSrcAttributes;
	
	private JPanel colorOddPick;
	private JPanel colorEvenPick;
	private JPanel colorDiffPick;
	private JPanel colorMouseOverPick;
	
	private DataColumnDefinition[][] comparedColumns;
	private DataColumnDefinition[][] dataModel;
	
	private ColorConfig colors = ColorConfig.getDefault();
	
	public ColumnConfigDialog(Window viewLinkagesDialog, ColorConfig colors, DataColumnDefinition[][] comparedColumns, DataColumnDefinition[][] dataModel, DataColumnDefinition[][] usedModel) {
		super(viewLinkagesDialog, "Preferences");
		setModal(true);
		setSize(600, 500);
		this.colors = colors;
		
		this.comparedColumns = comparedColumns;
		this.dataModel = dataModel;
		
		JPanel buttons = createButtons();
		
		JPanel lists = createTables();
		lists.setBorder(BorderFactory.createTitledBorder("Visible attributes"));
		
		JPanel colorsPanel = createColorConfig();
		colorsPanel.setBorder(BorderFactory.createTitledBorder("Colors"));
		
		setLayout(new GridBagLayout());
		add(lists, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		add(colorsPanel, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		add(buttons, new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		
		fillInLists(usedModel);
		
	}

	private JPanel createColorConfig() {
		JPanel panel = new JPanel(new GridBagLayout());
		colorOddPick = new JPanel();
		colorEvenPick = new JPanel();
		colorDiffPick = new JPanel();
		colorMouseOverPick = new JPanel();
		colorOddPick.setBackground(colors.getOddRowColor());
		colorOddPick.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		colorOddPick.setPreferredSize(new Dimension(60, 20));
		colorEvenPick.setBackground(colors.getEvenRowColor());
		colorEvenPick.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		colorEvenPick.setPreferredSize(new Dimension(60, 20));
		colorDiffPick.setBackground(colors.getDiffColor());
		colorDiffPick.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		colorDiffPick.setPreferredSize(new Dimension(60, 20));
		colorMouseOverPick.setBackground(colors.getMouseOverColor());
		colorMouseOverPick.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		colorMouseOverPick.setPreferredSize(new Dimension(60, 20));
		
		JButton oddButton = Configs.getColorChooseButton();
		panel.add(new JLabel("Odd row color", JLabel.LEFT), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,5), 0, 0));
		panel.add(colorOddPick, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,5,5), 0, 0));
		panel.add(oddButton, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,5), 0, 0));
		panel.add(new JPanel(), new GridBagConstraints(3, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,5,0,5), 0, 0));
		oddButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = getColor(colors.getOddRowColor());
				colors.setOddRowColor(c);
				colorOddPick.setBackground(c);
			}
		});
		
		JButton evenButton = Configs.getColorChooseButton();
		panel.add(new JLabel("Even row color", JLabel.LEFT), new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,5), 0, 0));
		panel.add(colorEvenPick, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,5,5), 0, 0));
		panel.add(evenButton, new GridBagConstraints(2, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,5), 0, 0));
		panel.add(new JPanel(), new GridBagConstraints(3, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,5,0,5), 0, 0));
		evenButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = getColor(colors.getEvenRowColor());
				colors.setEvenRowColor(c);
				colorEvenPick.setBackground(c);
			}
		});
		
		JButton diffButton = Configs.getColorChooseButton();
		panel.add(new JLabel("Difference color", JLabel.LEFT), new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,5), 0, 0));
		panel.add(colorDiffPick, new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,5,5), 0, 0));
		panel.add(diffButton, new GridBagConstraints(2, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,5), 0, 0));
		panel.add(new JPanel(), new GridBagConstraints(3, 2, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,5,0,5), 0, 0));
		diffButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = getColor(colors.getDiffColor());
				colors.setDiffColor(c);
				colorDiffPick.setBackground(c);
			}
		});
		
		JButton moverButton = Configs.getColorChooseButton();
		panel.add(new JLabel("Highlighted row color", JLabel.LEFT), new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,5), 0, 0));
		panel.add(colorMouseOverPick, new GridBagConstraints(1, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,5), 0, 0));
		panel.add(moverButton, new GridBagConstraints(2, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,5), 0, 0));
		panel.add(new JPanel(), new GridBagConstraints(3, 3, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,5,0,5), 0, 0));
		moverButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = getColor(colors.getMouseOverColor());
				colors.setMouseOverColor(c);
				colorMouseOverPick.setBackground(c);
			}
		});
		
		return panel;
	}
	
	private Color getColor(Color initial) {
		return JColorChooser.showDialog(this, "Choose color", initial);
	}

	private void fillInLists(DataColumnDefinition[][] usedModel) {
		for (int i = 0; i < comparedColumns[0].length; i++) {
			String str = generateName(i);
			comparedAttributes.addRow(new Object[] {new Boolean(isInOption(usedModel, new DataColumnDefinition[] {comparedColumns[0][i], comparedColumns[1][i]})), str, new Integer(i)});
		}
		
		for (int i = 0; i < dataModel[0].length; i++) {
			if (compared(dataModel[0][i], comparedColumns[0])) {
				continue;
			}
			leftSrcAttributes.addRow(new Object[] {new Boolean(isInOption(usedModel, new DataColumnDefinition[] {dataModel[0][i], null})), dataModel[0][i].getColumnName(), new Integer(i)});
		}
		for (int i = 0; i < dataModel[1].length; i++) {
			if (compared(dataModel[1][i], comparedColumns[1])) {
				continue;
			}
			rightSrcAttributes.addRow(new Object[] {new Boolean(isInOption(usedModel, new DataColumnDefinition[] {null, dataModel[1][i]})), dataModel[1][i].getColumnName(), new Integer(i)});
		}
		
	}

	private boolean isInOption(DataColumnDefinition[][] usedModel, DataColumnDefinition[] col) {
		for (int i = 0; i < usedModel[0].length; i++) {
			if (usedModel[0][i] == null && col[0] == null) {
				if (usedModel[1][i].equals(col[1])) {
					return true;
				}
			} else if (usedModel[1][i] == null && col[1] == null) {
				if (usedModel[0][i].equals(col[0])) {
					return true;
				}
			} else {
				if (usedModel[0][i] == null || usedModel[1][i] == null || col[0] == null || col[1] == null) {
					continue;
				}
				if (usedModel[1][i].equals(col[1]) && usedModel[0][i].equals(col[0])) {
					return true;
				}
			}
		}
		return false;
	}

	private String generateName(int i) {
		return comparedColumns[0][i].getColumnName() + "/" + comparedColumns[1][i].getColumnName();
	}

	private boolean compared(DataColumnDefinition dataColumnDefinition, DataColumnDefinition[] candidates) {
		for (int i = 0; i < candidates.length; i++) {
			if (candidates[i].equals(dataColumnDefinition)) {
				return true;
			}
		}
		return false;
	}

	private JPanel createTables() {
		JPanel panel = new JPanel(new GridBagLayout());
		
		panel.add(new JLabel("Left source attributes"), new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10,10,0,5), 0, 0));
		panel.add(new JLabel("Compared attributes"), new GridBagConstraints(2, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10,5,0,5), 0, 0));
		panel.add(new JLabel("Right source attributes"), new GridBagConstraints(4, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10,5,0,10), 0, 0));
		
		panel.add(leftSrcAttributes = new TablePanel(new String[] {"", "Attribute"}, false, false), new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,10,5,5), 0, 0));
		panel.add(comparedAttributes = new TablePanel(new String[] {"", "Attribute"}, false, false), new GridBagConstraints(2, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,5,5,5), 0, 0));
		panel.add(rightSrcAttributes = new TablePanel(new String[] {"", "Attribute"}, false, false), new GridBagConstraints(4, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,5,5,10), 0, 0));
		
		leftSrcAttributes.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		rightSrcAttributes.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		comparedAttributes.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		
		comparedAttributes.setEditableColumns(new int[] {0});
		comparedAttributes.setColumnClasses(new Class[] {Boolean.class, Object.class});
		comparedAttributes.getTable().getColumnModel().getColumn(0).setPreferredWidth(7);
		
		leftSrcAttributes.setEditableColumns(new int[] {0});
		leftSrcAttributes.setColumnClasses(new Class[] {Boolean.class, Object.class});
		leftSrcAttributes.getTable().getColumnModel().getColumn(0).setPreferredWidth(7);
		
		rightSrcAttributes.setEditableColumns(new int[] {0});
		rightSrcAttributes.setColumnClasses(new Class[] {Boolean.class, Object.class});
		rightSrcAttributes.getTable().getColumnModel().getColumn(0).setPreferredWidth(7);
		
		return panel;
	}

	private JPanel createButtons() {
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton OK = new JButton("OK");
		JButton cancel = new JButton("Cancel");
		OK.setPreferredSize(PREFERRED_SIZE);
		cancel.setPreferredSize(PREFERRED_SIZE);
		buttons.add(OK);
		buttons.add(cancel);
		OK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				result = RESULT_OK;
				setVisible(false);
			}
		});
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				result = RESULT_CANCEL;
				setVisible(false);
			}
		});
		return buttons;
	}

	public int getResult() {
		setVisible(true);
		return result;
	}

	public DataColumnDefinition[][] getConfiguredColumns() {
		
		List cols = new ArrayList();
		Object[] rows = comparedAttributes.getRows();
		for (int i = 0; i < rows.length; i++) {
			if (((Boolean)((Object[])rows[i])[0]).booleanValue()) {
				int id = getConditionColumn((String)((Object[])rows[i])[1]);
				cols.add(new DataColumnDefinition[] {comparedColumns[0][id], comparedColumns[1][id]});
			}
		}
		
		rows = leftSrcAttributes.getRows();
		for (int i = 0; i < rows.length; i++) {
			if (((Boolean)((Object[])rows[i])[0]).booleanValue()) {
				int id = getColumn((String)((Object[])rows[i])[1], 0);
				cols.add(new DataColumnDefinition[] {dataModel[0][id], null});
			}
		}
		
		rows = rightSrcAttributes.getRows();
		for (int i = 0; i < rows.length; i++) {
			if (((Boolean)((Object[])rows[i])[0]).booleanValue()) {
				int id = getColumn((String)((Object[])rows[i])[1], 1);
				cols.add(new DataColumnDefinition[] {null, dataModel[1][id]});
			}
		}
		
		//transpose the data
		DataColumnDefinition[][] columns = (DataColumnDefinition[][]) cols.toArray(new DataColumnDefinition[][] {});
		DataColumnDefinition[][] columnsT = new DataColumnDefinition[2][columns.length];
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < columnsT[i].length; j++) {
				columnsT[i][j] = columns[j][i];
			}
		}
		return columnsT;
	}

	private int getColumn(String string, int src) {
		for (int i = 0; i < dataModel[src].length; i++) {
			if (string.equals(dataModel[src][i].getColumnName())) {
				return i;
			}
		}
		return -1;
	}

	private int getConditionColumn(String string) {
		for (int i = 0; i < comparedColumns[0].length; i++) {
			String col = generateName(i);
			if (col.equals(string)) {
				return i;
			}
		}
		return -1;
	}

	public ColorConfig getColorConfig() {
		return colors;
	}

}
