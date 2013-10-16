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


package cdc.gui.components.linkagesanalysis.spantable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.TableCellRenderer;

public class SpanTalbeUI extends BasicTableUI {
	
	public void paint(Graphics g, JComponent c) {
		Rectangle r = g.getClipBounds();
		//System.out.println("Clip bounds: " + r);
		int firstRow = table.rowAtPoint(new Point(0, r.y)) - 1;
		int lastRow = table.rowAtPoint(new Point(0, r.y + r.height));
		// -1 is a flag that the ending point is outside the table
		if (lastRow < 0) {
			lastRow = table.getRowCount() - 1;
		} else {
			lastRow = (lastRow == table.getRowCount() - 1 ? lastRow : lastRow + 1);
		}
		for (int i = firstRow; i <= lastRow; i++)
			paintRow(i, g);
	}

	private void paintRow(int row, Graphics g) {
		Rectangle r = g.getClipBounds();
		for (int i = 0; i < table.getColumnCount(); i++) {
			Rectangle r1 = table.getCellRect(row, i, true);
			if (r1.intersects(r)) // at least a part is visible
			{
				int[] visCell = ((JSpanTable) table).data.visibleRowAndColumn(row, i);
				int[] span = ((JSpanTable) table).data.spannedRowsAndColumns(visCell[0], visCell[1]);
				if (visCell[0] == row && visCell[1] == i) {
					paintCell(visCell[0], visCell[1], g, r1);
				}
				// increment the column counter
				i += span[1] - 1;
			}
		}
	}

	private void paintCell(int row, int column, Graphics g, Rectangle area) {
		int verticalMargin = table.getRowMargin();
		int horizontalMargin = table.getColumnModel().getColumnMargin();

		Color c = g.getColor();
		g.setColor(table.getGridColor());
		g.drawRect(area.x, area.y, area.width - 1, area.height - 1);
		g.setColor(c);

		area.setBounds(area.x + horizontalMargin / 2, area.y + verticalMargin
				/ 2, area.width - horizontalMargin, area.height
				- verticalMargin);

		if (table.isEditing() && table.getEditingRow() == row
				&& table.getEditingColumn() == column) {
			Component component = table.getEditorComponent();
			component.setBounds(area);
			component.validate();
		} else {
			TableCellRenderer renderer = table.getCellRenderer(row, column);
			Component component = table.prepareRenderer(renderer, row, column);
			if (component.getParent() == null)
				rendererPane.add(component);
			rendererPane.paintComponent(g, component, table, area.x, area.y,
					area.width, area.height, true);
		}
	}

}
