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


package cdc.gui.components.datasource.ui;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.plaf.ComponentUI;

import cdc.datamodel.converters.DummyConverter;
import cdc.gui.Configs;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.MainFrame;
import cdc.gui.components.datasource.JDataSource;
import cdc.gui.components.datasource.JDataSource.Brick;
import cdc.gui.components.datasource.JDataSource.Connection;
import cdc.utils.GuiUtils;

public class BasicDataSourceUI extends DataSourceUI {
	
	private static final int SPACE = 180;
	private static final int MARGIN_TOP = 20;
	private static final int CENTER_X = 50;
	private static final int CENTER_Y = 00;
	private static final int HEIGHT_LABELS = 20;
	private static final int WIDTH_LABELS = 120;
	private static final int SPACING_LABELS = 3;
	
	private class UIConnection {
		JLabel[] in;
		JLabel conv;
		JLabel[] out;
		int level;
		int[] levels;
		int index;
	}
	
	private class MenuMouseListener implements MouseListener {
		private JPopupMenu menu;
		public MenuMouseListener(JPopupMenu menu) {
			this.menu = menu;
		}
		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {
			menu.show(e.getComponent(), e.getX(), e.getY());
		}
		public void mouseReleased(MouseEvent e) {
			menu.show(e.getComponent(), e.getX(), e.getY());
		}
	}
	
	protected JDataSource model;
	private UIConnection[] uiConnections;
	private Map brickToColumn = new HashMap();
	private Map usedColumns = new HashMap();
	
	public static ComponentUI createUI(JComponent c) {
		return new BasicDataSourceUI();
	}
	
	public void installUI(JComponent c) {
		c.setLayout(createLayoutManager());
		this.model = (JDataSource)c;
	}

	public void paint(Graphics g, JComponent c) {
		super.paint(g, c);
		Graphics2D g2d = (Graphics2D)g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		for (int i = 0; i < uiConnections.length; i++) {
			if (uiConnections[i].conv != null) {
				for (int j = 0; j < uiConnections[i].in.length; j++) {
					Rectangle rB = uiConnections[i].in[j].getBounds();
					Rectangle rE = uiConnections[i].conv.getBounds();
					int xB = rB.x + rB.width;
					int yB = rB.y + rB.height / 2;
					int xE = rE.x;
					int yE = rE.y + rB.height / 2;
					drawArrow(g2d, xB, yB, xE, yE, 0.1F);
				}
				for (int j = 0; j < uiConnections[i].out.length; j++) {
					Rectangle rB = uiConnections[i].conv.getBounds();
					Rectangle rE = uiConnections[i].out[j].getBounds();
					int xB = rB.x + rB.width;
					int yB = rB.y + rB.height / 2;
					int xE = rE.x;
					int yE = rE.y + rB.height / 2;
					drawArrow(g2d, xB, yB, xE, yE, 0.1F);
				}
			} else {
				Rectangle rB = uiConnections[i].in[0].getBounds();
				Rectangle rE = uiConnections[i].out[0].getBounds();
				int xB = rB.x + rB.width;
				int yB = rB.y + rB.height / 2;
				int xE = rE.x;
				int yE = rE.y + rB.height / 2;
				drawArrow(g2d, xB, yB, xE, yE, 0.1F);
			}
		}
	}

	public void uninstallUI(JComponent c) {
		this.model = null;
	}
	
	protected LayoutManager createLayoutManager() {
		return new DataSourceLayoutMgr();
	}
	
	private static void drawArrow(Graphics2D g2d, int xCenter, int yCenter, int x, int y, float stroke) {
		double aDir = Math.atan2(xCenter - x, yCenter - y);
		g2d.drawLine(x, y, xCenter, yCenter);
		g2d.setStroke(new BasicStroke(1f)); // make the arrow head solid even if dash pattern has been specified
		Polygon tmpPoly = new Polygon();
		int i1 = 7 + (int) (stroke * 2);
		int i2 = 6 + (int) stroke; // make the arrow head the same size regardless of the length length
		tmpPoly.addPoint(x, y); // arrow tip
		tmpPoly.addPoint(x + xCor(i1, aDir + .5), y + yCor(i1, aDir + .5));
		tmpPoly.addPoint(x + xCor(i2, aDir), y + yCor(i2, aDir));
		tmpPoly.addPoint(x + xCor(i1, aDir - .5), y + yCor(i1, aDir - .5));
		tmpPoly.addPoint(x, y); // arrow tip
		g2d.drawPolygon(tmpPoly);
		g2d.fillPolygon(tmpPoly); // remove this line to leave arrow head unpainted
	}
	
	private static int yCor(int len, double dir) {
		return (int) (len * Math.cos(dir));
	}

	private static int xCor(int len, double dir) {
		return (int) (len * Math.sin(dir));
	}

	protected class DataSourceLayoutMgr implements LayoutManager {

		private int lowest;
		
		public void addLayoutComponent(String name, Component comp) {}

		public void layoutContainer(Container c) {
			//System.out.println("Layout");
			usedColumns = new HashMap();
			lowest = 0;
			c.removeAll();
			brickToColumn.clear();
			JDataSource.Brick[] columns = BasicDataSourceUI.this.model.getColumns();
			JDataSource.Brick[] allColumns = BasicDataSourceUI.this.model.getAllPossibleColumns();
			JLabel[] labels = new JLabel[allColumns.length];
			for (int i = 0; i < columns.length; i++) {
				JLabel label = labels[getPosition(allColumns, columns[i])] = new JLabel(columns[i].col.getColumnName());
				label.setIcon(Configs.buttonVistaWhite);
				label.setHorizontalAlignment(JLabel.CENTER);
				label.setVerticalAlignment(JLabel.CENTER);
				label.setHorizontalTextPosition(JLabel.CENTER);
				label.setVerticalTextPosition(JLabel.CENTER);
				//label.setBorder(BorderFactory.createEtchedBorder());
				label.setBounds(CENTER_X, MARGIN_TOP + (HEIGHT_LABELS + SPACING_LABELS)*i + CENTER_Y, WIDTH_LABELS, HEIGHT_LABELS);
				c.add(label);
				JPopupMenu menu = new JPopupMenu();
				JMenuItem item = new JMenuItem("Add to out model");
				JMenuItem itemAll = new JMenuItem("Add all columns to out model");
				item.addActionListener(new AddOutModelListener(model, columns[i]));
				itemAll.addActionListener(new AddAllListener(model));
				menu.add(item);
				menu.add(itemAll);
				JMenu submenu = new JMenu("Use converter");
				GUIVisibleComponent[] converters = GuiUtils.getAvailableConverters();
				for (int j = 0; j < converters.length; j++) {
					JMenuItem subItem = new JMenuItem(converters[j].toString());
					submenu.add(subItem);
					subItem.addActionListener(new AddOutModelListener(model, columns[i], converters[j]));
				}
				menu.add(submenu);
				label.addMouseListener(new MenuMouseListener(menu));
				label.setFont(label.getFont().deriveFont(11F));
				brickToColumn.put(columns[i], new Integer(0));
				registerUsedCell(0, i, label);
			}
			
			Connection[] cs = model.getConnections();
			List connections = new ArrayList();
			for (int i = 0; i < cs.length; i++) {
				connections.add(cs[i]);
			}
			uiConnections = new UIConnection[connections.size()];
			int next = 0;
			int id = 0;
			while (!connections.isEmpty()) {
				UIConnection connectionCandidate = new UIConnection();
				Connection connection = (Connection) connections.get(id);
				try {
					if (connection.conv.conv instanceof DummyConverter) {
						JLabel label = labels[getPosition(allColumns, connection.to[0])] = new JLabel(connection.to[0].col.getColumnName());
						label.setIcon(Configs.buttonVistaBlue);
						label.setHorizontalAlignment(JLabel.CENTER);
						label.setVerticalAlignment(JLabel.CENTER);
						label.setHorizontalTextPosition(JLabel.CENTER);
						label.setVerticalTextPosition(JLabel.CENTER);
						label.setHorizontalAlignment(JLabel.CENTER);
						label.setVerticalAlignment(JLabel.CENTER);
						//label.setBorder(BorderFactory.createEtchedBorder());
						c.add(label);
						connectionCandidate.out = new JLabel[] {label};
						int[] inds = getInColumns(allColumns, connection.from, labels);
						connectionCandidate.in = new JLabel[] {labels[inds[0]]};
						//connectionCandidate.index = inds[0];
						JPopupMenu menu = new JPopupMenu();
						JMenuItem item = new JMenuItem("Delete");
						JMenuItem rename = new JMenuItem("Rename");
						JMenuItem emptyVals = new JMenuItem("Set empty values");
						JMenu submenu = new JMenu("Use converter");
						GUIVisibleComponent[] converters = GuiUtils.getAvailableConverters();
						for (int j = 0; j < converters.length; j++) {
							JMenuItem subItem = new JMenuItem(converters[j].toString());
							submenu.add(subItem);
							subItem.addActionListener(new AddOutModelListener(model, connection.to[0], converters[j]));
						}
						menu.add(submenu);
						item.addActionListener(new DeleteOutModelListener(connection.to[0], model));
						rename.addActionListener(new RenameListener(MainFrame.main, connection.to[0], model, connection.conv.conv));
						emptyVals.addActionListener(new AddEmptyValuesListener(MainFrame.main, connection.to[0]));
						menu.add(rename);
						menu.add(emptyVals);
						menu.add(item);
						label.addMouseListener(new MenuMouseListener(menu));
						brickToColumn.put(connection.to[0], new Integer(getColumnId(connection.from[0]) + 2));
						connectionCandidate.level = getColumnId(connection.from[0]) + 1;
						connectionCandidate.levels = new int[1];
						connectionCandidate.levels[0] = getColumnId(connection.from[0]) + 2;
					} else {
						JDataSource.Brick[] from = connection.from;
						JDataSource.Brick[] to = connection.to;
						JLabel[] toLabels = new JLabel[to.length];
						connectionCandidate.levels = new int[to.length];
						for (int j = 0; j < to.length; j++) {
							JLabel label = labels[getPosition(allColumns, to[j])] = toLabels[j] = new JLabel(to[j].col.getColumnName());
							label.setIcon(Configs.buttonVistaBlue);
							label.setHorizontalAlignment(JLabel.CENTER);
							label.setVerticalAlignment(JLabel.CENTER);
							label.setHorizontalTextPosition(JLabel.CENTER);
							label.setVerticalTextPosition(JLabel.CENTER);
							label.setHorizontalAlignment(JLabel.CENTER);
							label.setVerticalAlignment(JLabel.CENTER);
							//label.setBorder(BorderFactory.createEtchedBorder());
							c.add(label);
							JPopupMenu menu = new JPopupMenu();
							JMenuItem item = new JMenuItem("Delete");
							JMenuItem rename = new JMenuItem("Rename");
							JMenuItem emptyVals = new JMenuItem("Set empty values");
							JMenu submenu = new JMenu("Use converter");
							GUIVisibleComponent[] converters = GuiUtils.getAvailableConverters();
							for (int k = 0; k < converters.length; k++) {
								JMenuItem subItem = new JMenuItem(converters[k].toString());
								submenu.add(subItem);
								subItem.addActionListener(new AddOutModelListener(model, to[j], converters[k]));
							}
							menu.add(submenu);
							item.addActionListener(new DeleteOutModelListener(connection.to[j], model));
							rename.addActionListener(new RenameListener(MainFrame.main, connection.to[j], model, connection.conv.conv));
							emptyVals.addActionListener(new AddEmptyValuesListener(MainFrame.main, connection.to[j]));
							menu.add(rename);
							menu.add(emptyVals);
							menu.add(item);
							label.addMouseListener(new MenuMouseListener(menu));
							brickToColumn.put(to[j], new Integer(getMaxColumnId(from) + 2));
							connectionCandidate.levels[j] = getMaxColumnId(from) + 2;
						}
						connectionCandidate.out = toLabels;
						int[] inds = getInColumns(allColumns, from, labels);
						connectionCandidate.in = new JLabel[inds.length];
						//int avg = 0;
						for (int j = 0; j < inds.length; j++) {
							connectionCandidate.in[j] = labels[inds[j]];
							//avg += inds[j];
						}
						//connectionCandidate.index = avg / inds.length;
						JLabel convLabel = new JLabel(connection.conv.conv.toString());
						convLabel.setIcon(Configs.buttonVistaRed);
						convLabel.setHorizontalAlignment(JLabel.CENTER);
						convLabel.setVerticalAlignment(JLabel.CENTER);
						convLabel.setHorizontalTextPosition(JLabel.CENTER);
						convLabel.setVerticalTextPosition(JLabel.CENTER);
						convLabel.setHorizontalAlignment(JLabel.CENTER);
						convLabel.setVerticalAlignment(JLabel.CENTER);
						//convLabel.setBorder(BorderFactory.createEtchedBorder());
						connectionCandidate.conv = convLabel;
						c.add(convLabel);
						JPopupMenu menu = new JPopupMenu();
						JMenuItem item = new JMenuItem("Delete");
						JMenuItem edit = new JMenuItem("Edit");
						edit.addActionListener(new EditConverterListener(connection.from[0], model, connection.conv.conv));
						item.addActionListener(new DeleteOutModelListener(connection.conv, model));
						menu.add(item);
						menu.add(edit);
						convLabel.addMouseListener(new MenuMouseListener(menu));
						brickToColumn.put(connection.conv, new Integer(getMaxColumnId(from) + 1));
						connectionCandidate.level = getMaxColumnId(from) + 1;
					}
					connections.remove(id);
					id = 0;
					uiConnections[next++] = connectionCandidate;
				} catch (IllegalArgumentException e) {
					id++;
				}
			}			
			
			layoutConnections();
			
		}
		
		private void registerUsedCell(int columnId, int rowId, JLabel brick) {
			List items = (List) usedColumns.get(new Integer(columnId));
			if (items == null) {
				items = new ArrayList();
				usedColumns.put(new Integer(columnId), items);
			}
			while (items.size() <= rowId) {
				items.add(items.size(), null);
			}
			items.set(rowId, brick);
		}
		
		private boolean isCellUsed(int columnId, int rowId) {
			List items = (List) usedColumns.get(new Integer(columnId));
			return !(items == null || items.size() <= rowId || items.get(rowId) == null);
		}
		
		private int getCellPosition(int column, JLabel brick) {
			List items = (List) usedColumns.get(new Integer(column));
			if (items == null) {
				return -1;
			}
			return items.indexOf(brick);
		}
		
//		private Brick getCell(int columnId, int rowId) {
//			List items = (List) usedColumns.get(new Integer(columnId));
//			if (items == null) return null;
//			return (Brick) items.get(rowId);
//		}

		private int getPosition(Brick[] allColumns, Brick brick) {
			for (int i = 0; i < allColumns.length; i++) {
				if (allColumns[i].equals(brick)) {
					return i;
				}
			}
			return -1;
		}

		private int getMaxColumnId(Brick[] from) {
			int max = 0;
			for (int i = 0; i < from.length; i++) {
				int id = getColumnId(from[i]);
				if (max < id) {
					max = id;
				}
			}
			return max;
		}

		private int getColumnId(Brick brick) {
			Integer n = (Integer) brickToColumn.get(brick);
			if (n == null) {
				throw new IllegalArgumentException("Brick not found in map");
			} else {
				return n.intValue();
			}
		}

		private void layoutConnections() {
			
			for (int i = 0; i < uiConnections.length; i++) {
				uiConnections[i].index = findStartPosition(uiConnections, i);
			}
			
			//TEMPORARY FIX - IF WRONG - REMOVE
			for (int j = 0; j < uiConnections.length; j++) {
				for (int j2 = 0; j2 < j; j2++) {
					if (uiConnections[j2].index == uiConnections[j].index) {
						uiConnections[j].index++;
					}
				}
			}
			
			for (int i = 0; i < uiConnections.length; i++) {
				int position = uiConnections[i].index;
				if (uiConnections[i].conv != null) {
					position = getClosestFree(uiConnections[i].level, position, uiConnections[i].conv);
					uiConnections[i].conv.setBounds(SPACE * uiConnections[i].level + CENTER_X, MARGIN_TOP + (HEIGHT_LABELS + SPACING_LABELS)*position + CENTER_Y, WIDTH_LABELS, HEIGHT_LABELS);
					if (position > lowest) {lowest = position;}
				}
				for (int j = 0; j < uiConnections[i].out.length; j++) {
					int newFree = getClosestFree(uiConnections[i].levels[j], position, uiConnections[i].out[j]);
					uiConnections[i].out[j].setBounds(SPACE * uiConnections[i].levels[j] + CENTER_X, MARGIN_TOP + (HEIGHT_LABELS + SPACING_LABELS)*newFree + CENTER_Y, WIDTH_LABELS, HEIGHT_LABELS);
					if (newFree > lowest) {lowest = newFree;}
				}
			}
		}
		
		private int findStartPosition(UIConnection[] connections, int id) {
			int sum = 0;
			if (connections[id].level == 1) {
				for (int i = 0; i < connections[id].in.length; i++) {
					sum += getCellPosition(connections[id].level - 1, connections[id].in[i]);
				}
				return (int) (sum / (double)connections[id].in.length);
			} else {
				for (int i = 0; i < connections[id].in.length; i++) {
					UIConnection conn = getOwnerConnection(connections, connections[id].in[i]);
					if (conn != null) {
						sum += conn.index;
					} else {
						//must be an input
						sum += getCellPosition(0, connections[id].in[i]);
					}
				}
				return (int) (sum / (double)connections[id].in.length);
			}

		}

		private UIConnection getOwnerConnection(UIConnection[] connections, JLabel label) {
			for (int i = 0; i < connections.length; i++) {
				for (int j = 0; j < connections[i].out.length; j++) {
					if (label.equals(connections[i].out[j])) {
						return connections[i];
					}
				}
//				for (int j = 0; j < connections[i].in.length; j++) {
//					if (label.equals(connections[i].in[j])) {
//						return connections[i];
//					}
//				}
			}
			return null;
		}

		private int getClosestFree(int column, int row, JLabel brick) {
			int bound = 0;
			while (true) {
				if (!isCellUsed(column, row + bound)) {
					registerUsedCell(column, row + bound, brick);
					return row + bound;
				} else if (row - bound >= 0 && !isCellUsed(column, row - bound)) {
					registerUsedCell(column, row - bound, brick);
					return row - bound;
				} else {
					bound++;
				}
			}
		}

		private int[] getInColumns(Brick[] columns, Brick[] brick, JLabel[] colLabels) {
			int[] labels = new int[brick.length];
			for (int i = 0; i < labels.length; i++) {
				int j = 0;
				while (j < columns.length && !columns[j].equals(brick[i])) {
					j++;
				}
				if (j == columns.length) {
					throw new RuntimeException("Should not happen");
				} else {
					labels[i] = j;
				}
			}
			return labels;
		}

		public Dimension minimumLayoutSize(Container parent) {
			layoutContainer(parent);
			int n = Math.max(lowest, model.getColumns().length);
			return new Dimension(180 * usedColumns.size() + 200, n * (HEIGHT_LABELS + SPACING_LABELS) + 120);
		}

		public Dimension preferredLayoutSize(Container parent) {
			layoutContainer(parent);
			int n = Math.max(lowest, model.getColumns().length);
			return new Dimension(180 * usedColumns.size() + 200, n * (HEIGHT_LABELS + SPACING_LABELS) + 120);
		}

		public void removeLayoutComponent(Component comp) {}
		
	}

}
