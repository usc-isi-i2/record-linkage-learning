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


package cdc.gui.components.datasource;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import cdc.components.AbstractDataSource;
import cdc.configuration.Configuration;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.converters.AbstractColumnConverter;
import cdc.datamodel.converters.DummyConverter;
import cdc.datamodel.converters.ModelGenerator;
import cdc.gui.components.datasource.ui.BasicDataSourceUI;
import cdc.gui.components.datasource.ui.DataSourceUI;
import cdc.utils.RJException;

public class JDataSource extends JComponent {

	private static final String uiClassID = "DataSourceUI";
	
	public class Brick {
		public DataColumnDefinition col;
		public AbstractColumnConverter conv;
		public int id = 0;
		public Brick(DataColumnDefinition column) {
			col = column;
		}
		public Brick(DataColumnDefinition column, int id) {
			col = column;
			this.id = id;
		}
		public Brick(AbstractColumnConverter converter) {
			conv = converter;
		}
		
		public int hashCode() {
			if (conv != null) {
				return conv.hashCode();
			} else {
				return (col.getColumnName() + "_" + id).hashCode();
			}
		}
		
		public boolean equals(Object arg0) {
			if (!(arg0 instanceof Brick)) {
				return false;
			} else {
				Brick that = (Brick)arg0;
				if (conv != null) {
					return conv.equals(that.conv);
				} else {
					return col.equals(that.col) && id == that.id;
				}
			}
		}
		
		public String toString() {
			return "Brick: " + (col != null ? col.toString() + "(id=)" + id : conv.toString());
		}
	}
	
	public class Connection {
		public Brick from[];
		public Brick to[];
		public Brick conv;
	}
	
	private List connections = new ArrayList();
	private List columns = new ArrayList();
	private Map columnsToBricks = new HashMap();
	private AbstractDataSource dataSoruce;
	
	public String getUIClassID() {
		return uiClassID;
	}
	
	public void setUI(DataSourceUI ui) {
		super.setUI(ui);
	}
	
	public DataSourceUI getUI() {
		return (DataSourceUI)ui;
	}
	
	public void updateUI() {
		if (UIManager.get(getUIClassID()) != null) {
			setUI((DataSourceUI) UIManager.getUI(this));
		} else {
			setUI(new BasicDataSourceUI());
		}
	}
	
	public JDataSource(AbstractDataSource source, boolean restore) {
		DataColumnDefinition[] cols = source.getAvailableColumns();
		for (int i = 0; i < cols.length; i++) {
			Brick brick = new Brick(cols[i]);
			columns.add(brick);
			columnsToBricks.put(cols[i], brick);
		}
		
		if (restore && source.getDataModel() != null) {
			ModelGenerator m = source.getDataModel();
			for (int i = 0; i < m.getConverters().length; i++) {
				AbstractColumnConverter conv = m.getConverters()[i];
				addConnection(conv);
			}
		}
		this.dataSoruce = source;
		updateUI();
	}
	
	public void addConnection(AbstractColumnConverter converter) {
		addConnection(connections.size(), converter);
	}
	
	private void addConnection(int index, AbstractColumnConverter converter) {
		Connection con = new Connection();
		DataColumnDefinition cols[] = converter.getExpectedColumns();
		Brick[] b = new Brick[cols.length];
		for (int i = 0; i < b.length; i++) {
			if (isDirectColumn(cols[i])) {
				b[i] = new Brick(cols[i]);
			} else {
				b[i] = new Brick(cols[i], 1);
			}
			columnsToBricks.put(cols[i], b[i]);
		}
		con.from = b;
		cols = converter.getOutputColumns();
		b = new Brick[cols.length];
		for (int i = 0; i < b.length; i++) {
			b[i] = new Brick(cols[i], 1);
			columnsToBricks.put(cols[i], b[i]);
		}
		con.to = b;
		con.conv = new Brick(converter);
		connections.add(index, con);
		updateUI();
	}

	private boolean isDirectColumn(DataColumnDefinition dataColumnDefinition) {
		Brick brick;
		if ((brick = (Brick) columnsToBricks.get(dataColumnDefinition)) == null) {
			return true;
		}
		return brick.id == 0;
	}

	public Connection[] getConnections() {
		return (Connection[]) connections.toArray(new Connection[] {});	
	}
	
	public Brick[] getColumns() {
		return (Brick[]) columns.toArray(new Brick[] {});
	}
	
	public AbstractColumnConverter[] getConverters() {
		AbstractColumnConverter[] convs = new AbstractColumnConverter[connections.size()];
		int i = 0;
		for (Iterator iterator = connections.iterator(); iterator.hasNext();) {
			Connection connection = (Connection) iterator.next();
			if (connection.conv == null) {
				convs[i] = new DummyConverter(connection.from[0].col.getColumnName(), 
						connection.from[0].col, null);
			} else {
				convs[i] = connection.conv.conv;
			}
			i++;
		}
		return convs;
	}

	public void removeConnection(Brick item) {
		if (item.conv != null) {
			for (Iterator iterator = connections.iterator(); iterator.hasNext();) {
				Connection con = (Connection) iterator.next();
				if (item == con.conv) {
					List removal = findDepending(con);
					removal.add(con);
					connections.removeAll(removal);
					break;
				}
			}
		} else {
			i1: for (Iterator iterator = connections.iterator(); iterator.hasNext();) {
				Connection con = (Connection) iterator.next();
				for (int i = 0; i < con.to.length; i++) {
					if (item == con.to[i]) {
						List removal = findDepending(con);
						removal.add(con);
						connections.removeAll(removal);
						break i1;
					}
				}
			}
		}
		updateUI();
	}

	private List findDepending(Connection removed) {
		List deps = new ArrayList();
		for (int i = 0; i < removed.to.length; i++) {
			for (int j = 0; j < connections.size(); j++) {
				Connection tested = (Connection) connections.get(j);
				for (int k = 0; k < tested.from.length; k++) {
					if (removed.to[i].equals(tested.from[k])) {
						deps.add(tested);
						deps.addAll(findDepending(tested));
					}
				}
			}
		}
		return deps;
	}

//	public DataColumnDefinition[] getDataSourceColumns() {
//		List l = new ArrayList();
//		Brick[] allColumns = getAllPossibleColumns();
//		for (int i = 0; i < allColumns.length; i++) {
//			if (allColumns[i].col != null) {
//				l.add(allColumns[i].col);
//			}
//		}
//		return (DataColumnDefinition[]) l.toArray(new DataColumnDefinition[] {});
//	}
	
	public static void main(String[] args) throws RJException, IOException {
		
		Configuration configuration = Configuration.getConfiguration();
		
		JFrame frame = new JFrame();
		//frame.setSize(500, 500);
		frame.getContentPane().setLayout(new BorderLayout());
		JScrollPane pane = new JScrollPane(new JDataSource(configuration.getDataSources()[1], true));
		frame.getContentPane().add(pane, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		
	}

	public AbstractDataSource getDataSource() {
		return dataSoruce;
	}

	public void replaceConnection(int index, AbstractColumnConverter newConv) {
		removeConnection(((Connection)connections.get(index)).conv);
		addConnection(index, newConv);
		updateUI();
	}

	public Brick[] getAllPossibleColumns() {
		Object[] array = columnsToBricks.values().toArray();
		Brick[] copy = new Brick[array.length];
		for (int i = 0; i < copy.length; i++) {
			copy[i] = (Brick) array[i];
		}
		return copy;
	}

	public DataColumnDefinition[] getColumnsForConverter(AbstractColumnConverter createdConverter) {
		List cols = new ArrayList();
		DataColumnDefinition[] generics = dataSoruce.getAvailableColumns();
		for (int i = 0; i < generics.length; i++) {
			cols.add(generics[i]);
		}
		Connection[] matched = matchConnection(createdConverter);
		List prohibited = new ArrayList();
		for (int i = 0; i < matched.length; i++) {
			prohibited.addAll(findProhibited(matched[i]));
		}
		for (Iterator iterator = connections.iterator(); iterator.hasNext();) {
			Connection conn = (Connection) iterator.next();
			for (int i = 0; i < conn.to.length; i++) {
				if (!prohibited.contains(conn.to[i])) {
					cols.add(conn.to[i].col);
				}
			}
		}
		return (DataColumnDefinition[]) cols.toArray(new DataColumnDefinition[] {});
	}

	private Collection findProhibited(Connection connection) {
		List list = new ArrayList();
		for (int i = 0; i < connection.to.length; i++) {
			list.add(connection.to[i]);
		}
		boolean added = true;
		while (added) {
			added = false;
			for (Iterator iterator = connections.iterator(); iterator.hasNext();) {
				Connection c = (Connection) iterator.next();
				if (c != connection) {
					boolean prohibited = false;
					for (int i = 0; i < c.from.length; i++) {
						if (list.contains(c.from[i])) {
							prohibited = true;
						}
					}
					if (prohibited) {
						for (int j = 0; j < c.to.length; j++) {
							if (!list.contains(c.to[j])) {
								list.add(c.to[j]);
								added = true;
							}
						}
					}
				}
			}
		}
		return list;
	}

	private Connection[] matchConnection(AbstractColumnConverter createdConverter) {
		if (createdConverter == null) {
			return new Connection[] {};
		}
		List matched = new ArrayList();
		for (Iterator iterator = connections.iterator(); iterator.hasNext();) {
			Connection c = (Connection) iterator.next();
			if (matches(c, createdConverter)) {
				matched.add(c);
			}
		}
		return (Connection[]) matched.toArray(new Connection[] {});
	}

	private boolean matches(Connection c, AbstractColumnConverter createdConverter) {
		boolean convFound = true;
		for (int i = 0; i < createdConverter.getExpectedColumns().length; i++) {
			if (c.from.length != createdConverter.getExpectedColumns().length || 
					!c.from[i].col.equals(createdConverter.getExpectedColumns()[i])) {
				convFound = false;
				break;
			}
		}
		return convFound;
	}

	public int getIndexOf(AbstractColumnConverter createdConverter) {
		int i = 0;
		for (Iterator iterator = connections.iterator(); iterator.hasNext();) {
			Connection c = (Connection) iterator.next();
			if (c.conv.conv.equals(createdConverter)) {
				return i;
			}
			i++;
		}
		return -1;
	}

//	public void renameColumn(DataColumnDefinition col, String name) {
//		for (Iterator iterator = columnsToBricks.entrySet().iterator(); iterator.hasNext();) {
//			Map.Entry entry = (Map.Entry) iterator.next();
//			if (entry.getKey().equals(col)) {
//				((DataColumnDefinition)entry.getKey()).setName(name);
//				((Brick)entry.getValue()).col.setName(name);
//			}
//		}
//		//Brick b = (Brick)columnsToBricks.get(col);
//		//b.col.setName(name);
//		col.setName(name);
//	}

}
