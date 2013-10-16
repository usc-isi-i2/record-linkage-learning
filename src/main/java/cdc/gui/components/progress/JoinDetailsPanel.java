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


package cdc.gui.components.progress;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import cdc.components.AbstractJoin;
import cdc.components.AbstractJoinCondition;
import cdc.components.JoinListener;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.utils.RJException;

public class JoinDetailsPanel extends JPanel {
	
	private static final int LENGTH = 40;
	private static final int MAX_FOUND_ITER = 9;
	private static final int DFAULT_DELAY = 0;
	
	private static final String LABEL_PAUSE = "Pause";
	private static final String LABEL_RESUME = "Resume"; 
	
	private class Runner implements Runnable {

		private DataRow rowA, rowB;
		private DataColumnDefinition[] colsA, colsB; 
		private boolean join;
		private int conf;
		
		public Runner(DataRow rowA, DataRow rowB, int confidence, DataColumnDefinition[] colsA, DataColumnDefinition[] colsB, boolean join) {
			this.rowA = rowA;
			this.rowB = rowB;
			this.conf = confidence;
			this.colsA = colsA;
			this.colsB = colsB;
			this.join = join;
		}
		
		public void run() {
			addRow(rowA, colsA, conf, join, new MatteBorder(1,0,0,0, Color.BLACK));
			addRow(rowB, colsB, conf, join, new MatteBorder(0,0,1,0, Color.BLACK));
		}
		
	}
	
	private class TableCreator implements Runnable {

		private AbstractJoinCondition cond;
		
		public TableCreator(AbstractJoinCondition cond) {
			this.cond = cond;
		}
		
		public void run() {
			createTable(cond);
		}
		
	}
	
	private class ColorCellRenderer extends DefaultTableCellRenderer {
		public Component getTableCellRendererComponent(JTable arg0, Object arg1, boolean arg2, boolean arg3, int arg4, int arg5) {
			Value val = (Value)arg1;
			this.setBackground(val.color);
			this.setBorder(val.border);
			setText(val.values);
			return this;
		}
	}
	
	private class Value {
		public String values;
		public Color color;
		public Border border;
	}
	
	private class ReporterJoinListener implements JoinListener {
		public void rowsJoined(DataRow rowA, DataRow rowB, DataRow row, AbstractJoinCondition condition) {
			try {
				if (table == null) {
					SwingUtilities.invokeAndWait(new TableCreator(condition));
				}
				SwingUtilities.invokeLater(new Runner(rowA, rowB, Integer.parseInt(row.getProperty(AbstractJoin.PROPERTY_CONFIDNCE)), condition.getLeftJoinColumns(), condition.getRightJoinColumns(), true));
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			foundIter = MAX_FOUND_ITER;
			waitIfShould();
			
		}
		public void rowsNotJoined(DataRow rowA, DataRow rowB, int confidence, AbstractJoinCondition condition) {
			try {
				if (table == null) {
					SwingUtilities.invokeAndWait(new TableCreator(condition));
				}
				SwingUtilities.invokeLater(new Runner(rowA, rowB, confidence, condition.getLeftJoinColumns(), condition.getRightJoinColumns(), false));
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			
			waitIfShould();
			
		}
		
		public void trashingJoinedTuple(DataRow row) {
		}
		
		public void trashingNotJoinedTuple(DataRow row) {
		}
		
		public void close() throws RJException {
		}
		public void reset() throws RJException {
		}
		public void joinConfigured() throws RJException {
			JoinDetailsPanel.this.setPreparationStage();
		}
	}
	
	private class PollingThread extends Thread {
		private AbstractJoin join;
		public PollingThread(AbstractJoin join) {
			this.join = join;
		}
		public void run() {
			while (preparation) {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							awaitPanel.setProgressValue(join.getConfigurationProgress());
						}
					});
					sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
					break;
				}
			}
			this.join = null;
		}
	}

	
	private DefaultTableModel tableModel;
	private JTable table;
	private JPanel controlPanel;
	private JScrollPane scroll;
	
	private boolean paused = false;
	private JButton pause = new JButton(paused ? LABEL_RESUME : LABEL_PAUSE);
	
	private AbstractJoin join;
	private ReporterJoinListener listener;
	private JSlider slider;
	private JLabel timeLabel;
	
	private int foundIter;
	private int waitTime;
	private Object mutex = new Object();
	
	private volatile boolean preparation = true;
	private AwaitPanel awaitPanel;
	
	public JoinDetailsPanel(AbstractJoin join) throws RJException {
		setLayout(new BorderLayout());
		this.join = join;
		
		createUI();
		update();
		
		waitTime = slider.getValue() * 10;
		join.addJoinListener(listener = new ReporterJoinListener());
		
	}

	private void createUI() {
		controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		slider = new JSlider(0, 200);
		slider.setPreferredSize(new Dimension(100, 20));
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent ev) {
				synchronized (mutex) {
					waitTime = slider.getValue() * 10;
				}
			}
		});
		slider.setValue(DFAULT_DELAY);
		
		pause.setPreferredSize(new Dimension(100, 20));
		pause.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				synchronized (mutex) {
					paused = !paused;
					pause.setText(paused ? LABEL_RESUME : LABEL_PAUSE);
					if (!paused) {
						//unpause
						mutex.notifyAll();
					}
				}
			}
		});
		
		controlPanel.add(new JLabel("Delay"));
		controlPanel.add(slider);
		controlPanel.add(new JLabel("Current delay:"));
		controlPanel.add(timeLabel = new JLabel());
		controlPanel.add(pause);
		
		timeLabel.setPreferredSize(new Dimension(50, 20));
	}
	
	private void update() {
		if (preparation) {
			removeAll();
			awaitPanel = new AwaitPanel(!join.isConfigurationProgressSupported());
			add(awaitPanel, BorderLayout.CENTER);
		} else {
			removeAll();
			add(controlPanel, BorderLayout.NORTH);
		}
		updateUI();
		controlPanel.updateUI();
	}
	
	public void setPreparationStage() {
		synchronized(this) {
			preparation = true;
			table = null;
			update();
			if (join.isConfigurationProgressSupported()) {
				new PollingThread(join).start();
			}
		}
	}

	private void addRow(DataRow row, DataColumnDefinition[] columns, int conf, boolean join, Border border) {
		synchronized(this) {
			Value[] cols = new Value[columns.length+1];
			for (int i = 0; i < cols.length-1; i++) {
				cols[i] = new Value();
				cols[i].values = row.getData(columns[i]).getValue().toString();
				cols[i].color = join ? Color.GREEN : Color.RED;
				cols[i].border = border;
			}
			cols[cols.length-1] = new Value();
			cols[cols.length-1].values = String.valueOf(conf);
			cols[cols.length-1].color = join ? Color.GREEN : Color.RED;
			cols[cols.length-1].border = border;
			tableModel.addRow(cols);
			
			while (tableModel.getRowCount() > LENGTH) {
				tableModel.removeRow(0);
			}
			
			scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum() + 1);
		}
		
	}
	
	private void waitIfShould() {
		int wait;
		synchronized (mutex) {
			if (foundIter != 0) {
				wait = (int) Math.pow(2, foundIter);
				if (wait < waitTime) {
					wait = waitTime;
				}
				foundIter--;
			} else {
				wait = waitTime;
			}
			while (paused) {
				try {
					mutex.wait();
				} catch (InterruptedException e) {}
				if (!paused) {
					return;
				}
			}
		}
		if (timeLabel != null) {
			timeLabel.setText(wait + " ms");
		}
		try {
			Thread.sleep(wait);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void createTable(AbstractJoinCondition condition) {
		synchronized (this) {
			preparation = false;
			update();
			String model[] = new String[condition.getLeftJoinColumns().length+1];
			for (int i = 0; i < model.length-1; i++) {
				model[i] = condition.getLeftJoinColumns()[i].getColumnName();
			}
			model[model.length-1] = "Confidence";
			tableModel = new DefaultTableModel(model, 0);
			table = new JTable(tableModel) {
				public boolean isCellEditable(int row, int column) {
					return false;
				}
			};
			//table.setPreferredSize(new Dimension(400, 200));
			TableColumnModel colModel = table.getColumnModel();
			for (int i = 0; i < colModel.getColumnCount(); i++) {
				colModel.getColumn(i).setCellRenderer(new ColorCellRenderer());
			}
			
			scroll = new JScrollPane(table);
			scroll.setPreferredSize(getPreferredSize());
			JPanel panel = new JPanel(new BorderLayout());
			panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			panel.add(scroll, BorderLayout.CENTER);
			add(panel, BorderLayout.CENTER);
		}
	}
	
	public void close() throws RJException {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setPaused(false);
				try {
					join.removeJoinListener(listener);
				} catch (RJException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void windowClosed() {
		this.join = null;
	}
	
	public void setPaused(boolean pause) {
		synchronized (mutex) {
			paused = pause;
			this.pause.setText(paused ? LABEL_RESUME : LABEL_PAUSE);
			if (!paused) {
				//unpause
				mutex.notifyAll();
			}
		}
	}
	
}
