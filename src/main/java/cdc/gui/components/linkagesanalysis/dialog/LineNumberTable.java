package cdc.gui.components.linkagesanalysis.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import cdc.gui.components.linkagesanalysis.spantable.JSpanTable;
import cdc.gui.components.linkagesanalysis.spantable.SpanTableModel;

public class LineNumberTable extends JTable implements ChangeListener,
		PropertyChangeListener {

	private JTable main;

	public LineNumberTable(JSpanTable spanTable) {
		this.main = spanTable;

		main.addPropertyChangeListener(this);
		
		setShowGrid(true);
		setGridColor(Color.LIGHT_GRAY);
		setFocusable(false);
		setAutoCreateColumnsFromModel(false);
		setModel(main.getModel());
		setSelectionModel(main.getSelectionModel());

		TableColumn column = new TableColumn();
		column.setHeaderValue(" ");
		addColumn(column);
		column.setCellRenderer(new RowNumberRenderer());

		getColumnModel().getColumn(0).setPreferredWidth(45);
		setPreferredScrollableViewportSize(getPreferredSize());
	}
	
	public Rectangle getCellRect(int row, int column, boolean includeSpacing) {
		
		SpanTableModel data = (SpanTableModel) main.getModel();

		// add widths of all spanned logical cells
		int[] visCell = data.visibleRowAndColumn(row, column);
		int[] spanSize = data.spannedRowsAndColumns(row, column);
		
		Rectangle r1 = super.getCellRect(visCell[0], visCell[1], includeSpacing);
		
		for (int i = 1; i < spanSize[0]; i++) {
			r1.height += getRowHeight(visCell[0] + i);
		}
		
		for (int i = 1; i < spanSize[1]; i++) {
			r1.width += getColumnModel().getColumn(visCell[1] + i).getWidth();
		}
		
		return r1;
	}

	public void addNotify() {
		super.addNotify();
		Component c = getParent();
		if (c instanceof JViewport) {
			JViewport viewport = (JViewport) c;
			viewport.addChangeListener(this);
		}
	}

	public int getRowCount() {
		return main.getRowCount();
	}

	public int getRowHeight(int row) {
		return main.getRowHeight(row);
	}

	public Object getValueAt(int row, int column) {
		SpanTableModel data = (SpanTableModel) main.getModel();
		
		return Integer.toString(data.getRowNumber(row));
	}

	public boolean isCellEditable(int row, int column) {
		return false;
	}

	public void stateChanged(ChangeEvent e) {
		JViewport viewport = (JViewport) e.getSource();
		JScrollPane scrollPane = (JScrollPane) viewport.getParent();
		scrollPane.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
	}
	

	public void propertyChange(PropertyChangeEvent e) {
		if ("selectionModel".equals(e.getPropertyName())) {
			setSelectionModel(main.getSelectionModel());
		}

		if ("model".equals(e.getPropertyName())) {
			setModel(main.getModel());
		}
	}

	private static class RowNumberRenderer extends DefaultTableCellRenderer {
		public RowNumberRenderer() {
			setHorizontalAlignment(JLabel.CENTER);
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			
			setBackground(new Color(249, 249, 249));
			setForeground(Color.GRAY);
			if (isSelected) {
				setFont(getFont().deriveFont(Font.BOLD));
			}

			setText((value == null) ? "" : value.toString());
			
			return this;
		}
	}
}
