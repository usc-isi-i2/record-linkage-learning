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


package cdc.gui.components.dynamicanalysis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import cdc.gui.Configs;
import cdc.gui.StoppableThread;
import cdc.gui.components.dynamicanalysis.DynamicAnalysis.Value;
import cdc.utils.Props;
import cdc.utils.RJException;

public class DynamicAnalysisFrame extends JDialog {
	
	private class RowAdder implements Runnable {
		
		Object[] values;
		
		public RowAdder(Object[] values) {
			this.values = values;
		}
		
		public void run() {
			matrixModel.addRow(values);
			if (matrixModel.getRowCount() >= MAX_ROWS) {
				matrixModel.removeRow(0);
			}
		}

	}
	
	private class ThreadedRestarter implements Runnable {

		private String[] columns;
		private Object[] params;

		public ThreadedRestarter(String[] columns, Object[] params) {
			this.columns = columns;
			this.params = params;
		}
		
		public void run() {
			if (backgroundThread != null) {
				backgroundThread.scheduleStop();
				try {
					backgroundThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						try {
						matrixModel = new DefaultTableModel(columns, 0);
						compMatrix.setModel(matrixModel);
						TableColumnModel colModel = compMatrix.getColumnModel();
						for (int i = 0; i < colModel.getColumnCount(); i++) {
							colModel.getColumn(i).setCellRenderer(renderer);
						}
						backgroundThread = threadCreator.createThread(DynamicAnalysisFrame.this, params);
						backgroundThread.start();
						stop.setEnabled(true);
						close.setEnabled(true);
						working.setIcon(Configs.busyIcon);
						status.setIcon(null);
						status.setToolTipText(null);
						statusWarned  = false;
						} catch (IOException e) {
							e.printStackTrace();
						} catch (RJException e) {
							e.printStackTrace();
						}
					}
				});
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		
	}

	public static final int MAX_ROWS = Props.getInteger("dynamic-analysis-max-visible-rows");
	
	//private Window parent;
	private JTable compMatrix;
	private DefaultTableModel matrixModel;
	private StoppableThread backgroundThread;
	private JButton close;
	private JButton stop;
	private ValuesCreator valuesCreator;
	private ThreadCreator threadCreator;
	private TableCellRenderer renderer;
	private JLabel working = new JLabel(Configs.busyIcon);
	private JLabel status = new JLabel();

	private volatile boolean statusWarned = false;

	public DynamicAnalysisFrame(Window parent, TableCellRenderer renderer, ValuesCreator creator, ThreadCreator threadCreator) {
		super(parent, "Analysis window");
		//this.parent = parent;
		this.valuesCreator = creator;
		this.threadCreator = threadCreator;
		this.renderer = renderer;
		//setAlwaysOnTop(true);
		setSize(400, 300);
		//setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		//Rectangle location = parent.getBounds();
		//setLocation(location.x + location.width, location.y);
		
		compMatrix = new JTable() {
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component c = super.prepareRenderer(renderer, row, column);
				if (c instanceof JComponent) {
					((JComponent) c).setToolTipText(String.valueOf(getValueAt(row, column)));
				}
				return c;
			}
		};
		compMatrix.setEnabled(false);
		compMatrix.setAutoCreateRowSorter(true);
		compMatrix.getTableHeader().setReorderingAllowed(false);
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(new JScrollPane(compMatrix), BorderLayout.CENTER);
		
		close = new JButton("Close");
		close.setPreferredSize(new Dimension(close.getPreferredSize().width, 20));
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (backgroundThread != null) {
					backgroundThread.scheduleStop();
					((JButton)arg0.getSource()).setEnabled(false);
				}
				DynamicAnalysisFrame.this.dispose();
			}
		});
		stop = new JButton("Stop");
		stop.setPreferredSize(new Dimension(close.getPreferredSize().width, 20));
		stop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				((JButton)arg0.getSource()).setEnabled(false);
				if (backgroundThread != null) {
					backgroundThread.scheduleStop();
				}
			}
		});
		
		JPanel progress = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		progress.add(working);
		progress.add(status);
		working.setPreferredSize(new Dimension(30, 30));
		working.setMinimumSize(new Dimension(30, 30));
		working.setIcon(null);
		status.setPreferredSize(new Dimension(30, 30));
		status.setMinimumSize(new Dimension(30, 30));
		status.setIcon(null);
		
		JPanel buttons = new JPanel(new FlowLayout());
		buttons.add(Box.createRigidArea(new Dimension(1, 30)));
		buttons.add(Box.createRigidArea(new Dimension(30, 30)));
		buttons.add(Box.createRigidArea(new Dimension(30, 30)));
		buttons.add(stop);
		buttons.add(close);
		mainPanel.add(buttons, BorderLayout.SOUTH);
		mainPanel.setBorder(BorderFactory.createLineBorder(Color.black, 2));
		buttons.add(progress);
		
		getContentPane().add(mainPanel);
		
//		parent.addComponentListener(new ComponentListener() {
//			public void componentHidden(ComponentEvent e) {}
//
//			public void componentMoved(ComponentEvent e) {
//				Rectangle location = DynamicAnalysisFrame.this.parent.getBounds();
//				setLocation(location.x + location.width, location.y);
//			}
//
//			public void componentResized(ComponentEvent e) {}
//
//			public void componentShown(ComponentEvent e) {}
//			
//		});
		
		addWindowListener(new WindowListener() {
			public void windowActivated(WindowEvent e) {}
			public void windowClosed(WindowEvent e) {
				if (backgroundThread != null) {
					backgroundThread.scheduleStop();
				}
			}
			public void windowClosing(WindowEvent e) {
				if (backgroundThread != null) {
					backgroundThread.scheduleStop();
				}
				
			}
			public void windowDeactivated(WindowEvent e) {}
			public void windowDeiconified(WindowEvent e) {}
			public void windowIconified(WindowEvent e) {}
			public void windowOpened(WindowEvent e) {}
		});
	}
	
	public void addRow(String[] row) {
		Value[] values = valuesCreator.create(row);
		try {
			SwingUtilities.invokeAndWait(new RowAdder(values));
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	public void stop() {
		backgroundThread.scheduleStop();
	}
	
	public void setParameters(String[] columns, Object[] params) {
		new Thread(new ThreadedRestarter(columns, params)).start();	
	}

	public void addCloseListener(ActionListener actionListener) {
		close.addActionListener(actionListener);
	}

	public void finished(boolean ok) {
		stop.setEnabled(false);
		status.setIcon(ok ? statusWarned ? Configs.warnIcon : Configs.checkIcon : Configs.warnIcon);
		working.setIcon(null);
	}

	public void setWarningMessage(String error) {
		if (statusWarned) {
			return;
		}
		status.setIcon(Configs.warnIcon);
		status.setToolTipText(error);
		statusWarned  = true;
	}
	
}
