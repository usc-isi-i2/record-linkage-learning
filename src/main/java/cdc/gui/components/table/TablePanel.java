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


package cdc.gui.components.table;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import cdc.gui.Configs;
import cdc.gui.components.dynamicanalysis.ChangedConfigurationListener;

public class TablePanel extends JPanel {

	public class TableModelProxy implements TableModelListener {

		private ChangedConfigurationListener listener;
		
		public TableModelProxy(ChangedConfigurationListener propertyListener) {
			this.listener = propertyListener;
		}

		public void tableChanged(TableModelEvent arg0) {
			listener.configurationChanged();
		}

	}

	private class UpDownButtonEnablerListener implements ListSelectionListener {
		private JButton button;
		public UpDownButtonEnablerListener(JButton button) {
			this.button = button;
		}
		public void valueChanged(ListSelectionEvent e) {
			int first = e.getFirstIndex();
			int last = e.getLastIndex();
			if (e.getFirstIndex() != -1) {
				int min = Math.min(first, last);
				int max = Math.max(first, last);
				for (int i = min; i <= max; i++) {
					if (!table.isRowSelected(i)) {
						continue;
					}
					Object marker = tableModel.getValueAt(i, 0);
					if (marker instanceof MarkedItem && !((MarkedItem)marker).movable) {
						button.setEnabled(false);
						return;
					}
				}
				button.setEnabled(true);
			} else {
				button.setEnabled(false);
			}
		}
	}
	
	private class RemoveButtonEnablerListener implements ListSelectionListener {
		private JButton button;
		public RemoveButtonEnablerListener(JButton button) {
			this.button = button;
		}
		public void valueChanged(ListSelectionEvent e) {
			int first = e.getFirstIndex();
			int last = e.getLastIndex();
			if (e.getFirstIndex() != -1) {
				int min = Math.min(first, last);
				int max = Math.max(first, last);
				for (int i = min; i <= max; i++) {
					if (!table.isRowSelected(i)) {
						continue;
					}
					Object marker = tableModel.getValueAt(i, 0);
					if (marker instanceof MarkedItem && !((MarkedItem)marker).removable) {
						button.setEnabled(false);
						return;
					}
				}
				button.setEnabled(true);
			} else {
				button.setEnabled(false);
			}
		}
	}

	public static final int BUTTONS_TOP = 1;
	public static final int BUTTONS_LEFT = 2;
	
	private JTable table;
	private DefaultTableModel tableModel;
	
	private JButton add = new JButton(Configs.addButtonIcon);
	private JButton edit = new JButton(Configs.editButtonIcon);
	private JButton remove = new JButton(Configs.removeButtonIcon);
	
	private Class[] columnClasses;
	private int[] editableColumns;
	
	
	
	public TablePanel(String[] columns, boolean editOn) {
		this(columns, editOn, BUTTONS_LEFT);
	}
	
	public TablePanel(String[] columns, boolean editOn, int buttonsOrientation) {
		this(columns, editOn, true, buttonsOrientation, null, true);
	}
	
	public TablePanel(String[] columns, boolean editOn, boolean addRemoveOn) {
		this(columns, editOn, addRemoveOn, BUTTONS_LEFT, null, true);
	}
	
	public TablePanel(String[] columns, boolean editOn, boolean addRemoveOn, boolean upDownEnabled) {
		this(columns, editOn, addRemoveOn, BUTTONS_LEFT, null, upDownEnabled);
	}
	
	public TablePanel(String[] columns, Class[] columnClasses) {
		this(columns, true, columnClasses);
	}
	
	public TablePanel(String[] columns, boolean editOn, Class[] columnClasses) {
		this(columns, editOn, BUTTONS_LEFT, columnClasses);
	}
	
	public TablePanel(String[] columns, boolean editOn, int buttonsOrientation, Class[] columnClasses) {
		this(columns, editOn, true, buttonsOrientation, columnClasses, true);
	}
	
	public TablePanel(String[] columns, boolean editOn, boolean addRemoveOn, Class[] columnClasses) {
		this(columns, editOn, addRemoveOn, BUTTONS_LEFT, columnClasses, true);
	}
	
	public TablePanel(String[] columns) {
		this(columns, true);
	}
	
	public TablePanel(String[] columns, boolean editOn, boolean addRemoveOn, int buttonsOrientation) {
		this(columns, editOn, addRemoveOn, buttonsOrientation, null, true);
	}

	public TablePanel(String[] columns, boolean editOn, boolean addRemoveOn, int buttonsOrientation, Class[] columnClasses, boolean upDownEnabled) {
		
		edit.setToolTipText("Edit selected entry");
		add.setToolTipText("Add new entry");
		remove.setToolTipText("Remove selected entry");
		
		edit.setEnabled(false);
		remove.setEnabled(false);
		
		setColumnClasses(columnClasses);
		tableModel = new DefaultTableModel(columns, 0) {
			public Class getColumnClass(int columnIndex) {
				if (TablePanel.this.columnClasses == null || TablePanel.this.columnClasses.length <= columnIndex) {
					return super.getColumnClass(columnIndex);
				} else {
					return TablePanel.this.columnClasses[columnIndex];
				}
			}
			
		};
		
		table = new JTable() {
			 public boolean isCellEditable(int row, int column) {
				 if (editableColumns != null) {
					 for (int i = 0; i < editableColumns.length; i++) {
						if (editableColumns[i] == column) {
							return true;
						}
					}
				 }
				return false; 
			}
			public TableCellRenderer getCellRenderer(int row, int column) {
				TableCellRenderer renderer = super.getCellRenderer(row, column);
				Object indicator = tableModel.getValueAt(row, column);
				if (indicator instanceof MarkedItem) {
					MarkedItem it = (MarkedItem)indicator;
					if (!it.movable || !it.removable) {
						((DefaultTableCellRenderer)renderer).setForeground(Color.GRAY);
					}
				}
				return renderer;
			}
		};
		
		table.getTableHeader().setReorderingAllowed(false);
		table.setModel(tableModel);
		
		table.getSelectionModel().addListSelectionListener(new RemoveButtonEnablerListener(edit));
		table.getSelectionModel().addListSelectionListener(new RemoveButtonEnablerListener(remove));
		
		GridBagConstraints c = null;
		JPanel upDownPanel = new JPanel();
		if (upDownEnabled) {
			JButton up = new JButton(Configs.upArrow);
			JButton down = new JButton(Configs.downArrow);
			up.setPreferredSize(new Dimension(20, 20));
			down.setPreferredSize(new Dimension(20, 20));
			up.setEnabled(false);
			down.setEnabled(false);
			upDownPanel.setLayout(new GridBagLayout());
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 0;
			c.anchor = GridBagConstraints.PAGE_START;
			c.weightx = 0;
			c.weighty = 1;
			upDownPanel.add(up, c);
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 1;
			c.anchor = GridBagConstraints.PAGE_END;
			c.weightx = 0;
			c.weighty = 1;
			upDownPanel.add(down, c);
			table.getSelectionModel().addListSelectionListener(new UpDownButtonEnablerListener(up));
			table.getSelectionModel().addListSelectionListener(new UpDownButtonEnablerListener(down));
			up.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int[] row = table.getSelectedRows();
					table.clearSelection();
					for (int i = 0; i < row.length; i++) {
						if (row[i] > 0) {
							Vector data = (Vector) tableModel.getDataVector();
							Vector v = (Vector)data.get(row[i]);
							tableModel.removeRow(row[i]);
							tableModel.insertRow(row[i]-1, (Vector)v);
							table.addRowSelectionInterval(row[i]-1, row[i]-1);
							table.scrollRectToVisible(table.getCellRect(row[i]-1, 0, true));
						}
					}
					if (row.length != 0 && row[0] > 1) {
						table.scrollRectToVisible(table.getCellRect(row[0] - 2, 0, true));
					}
				}
			});
			down.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int[] row = table.getSelectedRows();
					table.clearSelection();
					for (int i = 0; i < row.length; i++) {
						if (row[i] < tableModel.getRowCount()-1) {
							Vector data = (Vector) tableModel.getDataVector();
							Vector v = (Vector)data.get(row[i]);
							tableModel.removeRow(row[i]);
							tableModel.insertRow(row[i]+1, (Vector)v);
							table.addRowSelectionInterval(row[i]+1, row[i]+1);
							table.scrollRectToVisible(table.getCellRect(row[i]+1, 0, true));
						}
					}
					if (row.length != 0) {
						table.scrollRectToVisible(table.getCellRect(row[0] + 2, 0, true));
					}
				}
			});
			
			upDownPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));
		}
		
		JScrollPane scroll = new JScrollPane(table);
		
		//TODO Thing below looked ugly on MAC. Why was it added?
		//scroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10), scroll.getBorder()));
		
		this.setLayout(new GridBagLayout());
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.insets = new Insets(0, 5, 0, 0);
		this.add(scroll, c);
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 1;
		c.weighty = 1;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.BOTH;
		if (upDownEnabled) {
			this.add(upDownPanel, c);
		}
		if (addRemoveOn) {
			if (buttonsOrientation == BUTTONS_LEFT) {
				add.setPreferredSize(Configs.PREFERRED_SIZE);
				edit.setPreferredSize(Configs.PREFERRED_SIZE);
				remove.setPreferredSize(Configs.PREFERRED_SIZE);
				add.setMaximumSize(Configs.PREFERRED_SIZE);
				edit.setMaximumSize(Configs.PREFERRED_SIZE);
				remove.setMaximumSize(Configs.PREFERRED_SIZE);
				JPanel buttons = new JPanel();
				buttons.setLayout(new GridBagLayout());
				buttons.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));
				buttons.setPreferredSize(new Dimension(30, 80));
				c = new GridBagConstraints();
				c.gridx = 0;
				c.gridy = 0;
				c.weighty = 0;
				c.anchor = GridBagConstraints.FIRST_LINE_START;
				buttons.add(add, c);
				if (editOn) {
					c = new GridBagConstraints();
					c.gridx = 0;
					c.gridy = 1;
					c.weighty = 0;
					c.anchor = GridBagConstraints.FIRST_LINE_START;
					buttons.add(edit, c);
				}
				c = new GridBagConstraints();
				c.gridx = 0;
				c.gridy = 2;
				c.weighty = 1;
				c.anchor = GridBagConstraints.FIRST_LINE_START;
				buttons.add(remove,c);
				c = new GridBagConstraints();
				c.gridx = 0;
				c.gridy = 1;
				c.fill = GridBagConstraints.BOTH;
				c.weighty = 1;
				c.anchor = GridBagConstraints.FIRST_LINE_START;
				this.add(buttons, c);
			} else {
				JPanel buttons = new JPanel(new GridBagLayout());
				c = new GridBagConstraints();
				c.gridx = 0;
				c.gridy = 0;
				buttons.add(add, c);
				c = new GridBagConstraints();
				c.gridx = 1;
				c.gridy = 0;
				buttons.add(edit, c);
				c = new GridBagConstraints();
				c.gridx = 2;
				c.gridy = 0;
				buttons.add(remove, c);
				add.setPreferredSize(Configs.PREFERRED_SIZE);
				edit.setPreferredSize(Configs.PREFERRED_SIZE);
				remove.setPreferredSize(Configs.PREFERRED_SIZE);
				
				c = new GridBagConstraints();
				c.gridx = 1;
				c.gridy = 0;
				c.anchor = GridBagConstraints.FIRST_LINE_START;
				c.fill = GridBagConstraints.HORIZONTAL;
				c.weightx = 1;
				//c.weightx = 1;
				this.add(buttons, c);
			}
			
			remove.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					int[] selected = table.getSelectedRows();
					table.clearSelection();
					for (int i = 0; i < selected.length; i++) {
						tableModel.removeRow(selected[i] - i);
					}
					((JButton)arg0.getSource()).setEnabled(false);
				}
			});
		} else {
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = 0;
			add(Box.createRigidArea(new Dimension(10, 10)), c);
		}
	}

	public Object[] getRows() {
		Vector data = tableModel.getDataVector();
		Object[] dataOut = new Object[data.size()];
		for (int i = 0; i < dataOut.length; i++) {
			dataOut[i] = ((Vector)data.get(i)).toArray();
			if (((Object[])dataOut[i])[0] instanceof MarkedItem) {
				((Object[])dataOut[i])[0] = ((MarkedItem)((Object[])dataOut[i])[0]).item; 
			}
		}
		return dataOut;
	}
	
	public void addAddButtonListener(ActionListener listener) {
		add.addActionListener(listener);
	}
	
	public void addRemoveButtonListener(ActionListener listener) {
		remove.addActionListener(listener);
	}
	
	public void addEditButtonListener(ActionListener listener) {
		edit.addActionListener(listener);
	}
	
	public void addRow(Object[] row) {
		tableModel.addRow(row);
	}
	
	public void addRow(Object[] row, boolean removable, boolean movable) {
		row[0] = new MarkedItem(row[0], movable, removable);
		tableModel.addRow(row);
	}
	
	public void removeRow(int id) {
		tableModel.removeRow(id);
	}
	
	public void replaceRow(int id, Object[] newRow) {
		//Vector data = tableModel.getDataVector();
		//data.remove(id);
		//data.add(id, new Vector(java.util.Arrays.asList(newRow)));
		//tableModel.setDataVector(data, );
		for (int i = 0; i < newRow.length; i++) {
			tableModel.setValueAt(newRow[i], id, i);
		}
	}
	
	public void removeAllRows() {
		tableModel.setRowCount(0);
	}

	public Object[] getSelectedRows() {
		int[] rows = table.getSelectedRows();
		Object[] rowsSel = new Object[rows.length];
		for (int i = 0; i < rowsSel.length; i++) {
			rowsSel[i] = ((Vector)tableModel.getDataVector().get(rows[i])).toArray();
			if (((Object[])rowsSel[i])[0] instanceof MarkedItem) {
				((Object[])rowsSel[i])[0] = ((MarkedItem)((Object[])rowsSel[i])[0]).item; 
			}
		}
		return rowsSel;
	}

	public void multiselectionAllowed(boolean multi) {
		table.setSelectionMode(multi ? ListSelectionModel.SINGLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
	}
	
	public int[] getSelectedRowId() {
		return table.getSelectedRows();
	}

	public void setValueAt(int row, int col, String string) {
		table.setValueAt(string, row, col);
	}

	public void clearSelection() {
		table.clearSelection();
	}
	
	public void setEnabled(boolean arg0) {
		table.setEnabled(arg0);
		table.clearSelection();
		add.setEnabled(arg0);
		edit.setEnabled(false);
		remove.setEnabled(false);
	}

	public void addTablePropertyChangeListener(ChangedConfigurationListener propertyListener) {
		table.getModel().addTableModelListener(new TableModelProxy(propertyListener));
	}
	
	public void setColumnClasses(Class[] classes) {
		this.columnClasses = classes;
	}
	
	public void setEditableColumns(int[] editableColumns) {
		this.editableColumns = editableColumns;
	}

	public JTable getTable() {
		return table;
	}
	
	private class MarkedItem {
		
		private Object item;
		private boolean movable = true;
		private boolean removable = true;
		
		public MarkedItem(Object it, boolean movable, boolean removable) {
			item = it;
			this.movable = movable;
			this.removable = removable;
		}
		
		public String toString() {
			return item.toString();
		}
	}
	
	public static void main(String[] args) {
		JFrame frame = new JFrame("tets");
		frame.setSize(new Dimension(300, 200));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		TablePanel tablePanel = new TablePanel(new String[] {"a", "b", "c"}, true);
		tablePanel.addRow(new Object[] {"1", "2", "3"}, true, true);
		tablePanel.addRow(new Object[] {"4", "5", "6"}, true, false);
		tablePanel.addRow(new Object[] {"7", "8", "9"}, false, true);
		frame.getContentPane().add(tablePanel);
		frame.setVisible(true);
	}
	
}
