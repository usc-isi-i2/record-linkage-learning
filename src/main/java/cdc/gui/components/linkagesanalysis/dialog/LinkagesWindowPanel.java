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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import cdc.components.AbstractDistance;
import cdc.components.AbstractJoin;
import cdc.components.Filter;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.gui.Configs;
import cdc.gui.OptionDialog;
import cdc.gui.components.linkagesanalysis.LoadingThread;
import cdc.gui.components.linkagesanalysis.ThreadCreatorInterface;
import cdc.gui.components.linkagesanalysis.spantable.SpanInfo;
import cdc.gui.components.linkagesanalysis.spantable.SpanTableModel;
import cdc.gui.components.uicomponents.FilterExpressionEditor;
import cdc.gui.external.JXErrorDialog;
import cdc.utils.Props;
import cdc.utils.RJException;

public class LinkagesWindowPanel extends JPanel {


	private static final String STRATUM_NAME = "Stratum name";
	private static final String CONFIDENCE = "Confidence";
	private static final String VIEW_PREFERENCES = "View preferences";
	private static final String REJECT_ALL_LINKAGES = "Reject all linkages";
	private static final String ACCEPT_ALL_LINKAGES = "Accept all linkages";
	private static final String LINKAGE_DETAILS = "Show linkage details";
	private static final String SORT_LINKAGES = "Sort linkages";
	private static final String SAVE_LINKAGES = "Export current view to a file";
	private static final String FILTER_LINKAGES = "Filter linkages";
	private static final String CONFIGURE_SORTING = "Configure sorting";
	private static final String CONFIGURE_FITER = "Configure fiter";
	
	private static int RECORDS_PER_PAGE = Props.getInteger("records-per-page", 200);
	
	private LinkagesPanel linkages;
	private SpanTableModel tableModel;
	
	private DataColumnDefinition[][] comparedColumns;
	private AbstractDistance[] compareDistances;
	private DataColumnDefinition[][] dataModel;
	private DataColumnDefinition confidence;
	private DataColumnDefinition stratum;
	
	private DataColumnDefinition[][] usedModel;
	
	private boolean acceptRejectOption;
	private boolean showSourceName;
	private ThreadCreatorInterface threadCreator;
	private LoadingThread loadingThread;
	
	private Filter filter;
	private DataColumnDefinition[] sortColumns;
	private int[] sortOrder;
	
	private JLabel statusMsg = new JLabel(" ", JLabel.LEFT);
	
	private Object mutex = new Object();
	
	private JLabel sortStatus;
	private JLabel filterStatus;
	
	private JTextField positionField;
	
	private List details = new ArrayList();
	private AbstractDetailsDialog detailsWindow;
	
	private List decisionListeners = new ArrayList();
	
	private Window parentWindow;
	private JToolBar toolBar;
	private JPanel statusBar;
	
	public LinkagesWindowPanel(Window parent, DataColumnDefinition[][] columns, boolean showDataSourceName, DataColumnDefinition confidenceColumn, DataColumnDefinition stratum, DataColumnDefinition[][] comparedColumns, AbstractDistance[] distances, ThreadCreatorInterface threadCreator, boolean acceptRejectOption) {
		this.parentWindow = parent;
		this.threadCreator = threadCreator;
		this.showSourceName = showDataSourceName;
		this.dataModel = columns;
		this.usedModel = comparedColumns;
		this.comparedColumns = comparedColumns;
		this.confidence = confidenceColumn;
		this.stratum = stratum;
		this.acceptRejectOption = acceptRejectOption;
		this.compareDistances = distances;
		
		toolBar = createToolBar();
		statusBar = createStatusBar();
		
		if (this.usedModel == null) {
			this.usedModel = dataModel;
		}
		
		tableModel = new SpanTableModel(getDefaultColumns());
		
		if (acceptRejectOption) {
			linkages = new LinkagesPanel(
				new DecisionCellCreator(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						notifyListeners(true);
					}
				}, 
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						notifyListeners(false);
					}
				}), tableModel, dataModel.length == 2, stratum != null);
		} else {
			linkages = new LinkagesPanel(tableModel, dataModel.length > 1, stratum != null);
			linkages.setTableModel(tableModel, decodeUsedDistance(usedModel), decodeUsedCompColumns(usedModel));
		}
		
		setLayout(new BorderLayout());
		add(toolBar, BorderLayout.PAGE_START);
		add(linkages, BorderLayout.CENTER);
		add(statusBar, BorderLayout.SOUTH);
		
		linkages.addMouseOverRowListener(new MouseOverRowListener() {
			public void mouseOverRow(int rowId) {
				if (detailsWindow != null) {
					detailsWindow.setDetail((DataRow) details.get(rowId));
				}
			}
		});
			
		loadingThread = threadCreator.createNewThread(threadCreator, this, null, null, null);
		loadingThread.start();
	}

	private JPanel createStatusBar() {
		JPanel panel = new JPanel(new GridBagLayout());
		JPanel msgPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		statusMsg.setVerticalTextPosition(JLabel.CENTER);
		msgPanel.add(statusMsg);
		panel.add(msgPanel, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0, 0));
		JPanel sortStatusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		sortStatusPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		sortStatusPanel.add(new JLabel("Sort:"));
		sortStatus = new JLabel(Configs.bulbOff);
		sortStatusPanel.add(sortStatus);
		JPanel filterStatusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		filterStatusPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		filterStatusPanel.add(new JLabel("Filter:"));
		filterStatus = new JLabel(Configs.bulbOff);
		filterStatusPanel.add(filterStatus);
		panel.add(sortStatusPanel, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
		panel.add(filterStatusPanel, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
		msgPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		
		JPanel navigation = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		JButton forward = Configs.getForwardButton();
		JButton backward = Configs.getBackwardButton();
		navigation.add(backward);
		navigation.add(positionField = new JTextField("1", 3));
		positionField.setEditable(false);
		positionField.setHorizontalAlignment(JTextField.CENTER);
		positionField.setBorder(BorderFactory.createEmptyBorder());
		navigation.add(forward);
		panel.add(navigation, new GridBagConstraints(3, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
		backward.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (loadingThread.moveCursorBackward()) {
					positionField.setText(String.valueOf(Integer.parseInt(positionField.getText()) - 1));
				}
			}
		});
		forward.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (loadingThread.moveCursorForward()) {
					positionField.setText(String.valueOf(Integer.parseInt(positionField.getText()) + 1));
				}
			}
		});
		
		panel.add(new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)), new GridBagConstraints(4, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
		
		return panel;
	}

	private JToolBar createToolBar() {
		JToolBar tb = new JToolBar();
		
		JButton save = Configs.getSaveButton();
		save.setMaximumSize(new Dimension(30, 30));
		save.setBorder(BorderFactory.createEmptyBorder());
		save.setToolTipText(SAVE_LINKAGES);
		tb.add(save);
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//int decision = JOptionPane.showOptionDialog(parentWindow, SAVE_MESSAGE, "Save data", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, SAVE_OPTIONS, SAVE_OPTIONS[0]);
				RecordSavingPanel panel = new RecordSavingPanel();
				OptionDialog dialog = new OptionDialog(parentWindow, "Save data");
				dialog.setMainPanel(panel);
				if (dialog.getResult() == OptionDialog.RESULT_OK) {
					JDialog d = new JDialog(parentWindow, "Saving data...");
					d.setSize(180, 80);
					d.setLocationRelativeTo(parentWindow);
					d.setLayout(new BorderLayout());
					d.add(new JLabel(Configs.busyIcon, JLabel.CENTER), BorderLayout.CENTER);
					d.setModal(true);
					d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
					Worker w = new Worker();
					w.fileName = panel.getFileName();
					w.all = panel.isSaveAll();
					w.dialog = d;
					w.start();
					d.setVisible(true);
				}
			}
		});
		
		tb.addSeparator();
		
		JButton filter = Configs.getFilterButton();
		filter.setMaximumSize(new Dimension(30, 30));
		filter.setBorder(BorderFactory.createEmptyBorder());
		filter.setToolTipText(FILTER_LINKAGES);
		if (acceptRejectOption) {
			filter.setEnabled(false);
		}
		tb.add(filter);
		filter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				OptionDialog dialog = new OptionDialog(parentWindow, CONFIGURE_FITER);
				DataColumnDefinition[] cols;
				if (dataModel.length == 1) {
					cols = new DataColumnDefinition[dataModel[0].length];
				} else {
					cols = new DataColumnDefinition[dataModel[0].length + dataModel[1].length];
				}
				System.arraycopy(dataModel[0], 0, cols, 0, dataModel[0].length);
				if (dataModel.length != 1) {
					System.arraycopy(dataModel[1], 0, cols, dataModel[0].length, dataModel[1].length);
				}
				FilterExpressionEditor expr = new FilterExpressionEditor(dialog, cols, LinkagesWindowPanel.this.filter);
				expr.setPreferredSize(new Dimension(500, 300));
				expr.setEnabled(true);
				expr.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
				dialog.setMainPanel(expr);
				dialog.setSize(400, 300);
				while (true) {
					if (dialog.getResult() == OptionDialog.RESULT_OK) {
						stopLoadingThread();
						synchronized (mutex) {
							resetTableSettings();
						}
						Filter f = null;
						try {
							loadingThread = threadCreator.createNewThread(threadCreator, LinkagesWindowPanel.this, f = expr.getFilter(), sortColumns, sortOrder);
							positionField.setText("1");
							loadingThread.start();
						} catch (RJException ex) {
							JXErrorDialog.showDialog(parentWindow, "Filter expression error", ex);
						}
						LinkagesWindowPanel.this.filter = f;
					}
					break;
				}
			}
		});
		
		JButton sort = Configs.getSortButton();
		sort.setMaximumSize(new Dimension(30, 30));
		sort.setBorder(BorderFactory.createEmptyBorder());
		sort.setToolTipText(SORT_LINKAGES);
		tb.add(sort);
		sort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				OptionDialog dialog = new OptionDialog(parentWindow, CONFIGURE_SORTING);
				AbstractSortingEditor sortPanel = null;
				if (dataModel.length != 1) {
					sortPanel = new SortingEditor(dataModel, confidence, comparedColumns, sortColumns, sortOrder, showSourceName);
				} else {
					sortPanel = new SortingEditorSingleSource(dataModel, sortColumns, sortOrder, showSourceName);
				}
				dialog.setMainPanel(sortPanel);
				dialog.setSize(400, 300);
				if (dialog.getResult() == OptionDialog.RESULT_OK) {
					
					stopLoadingThread();
					synchronized (mutex) {
						resetTableSettings();
					}
					loadingThread = threadCreator.createNewThread(threadCreator, LinkagesWindowPanel.this, LinkagesWindowPanel.this.filter, sortColumns = sortPanel.getSortColumns(), sortOrder = sortPanel.getSortOrder());
					positionField.setText("1");
					loadingThread.start();
					
				}
			}
		});
		
		tb.addSeparator();
		
		JButton details = Configs.getDetailsButton();
		details.setMaximumSize(new Dimension(30, 30));
		details.setBorder(BorderFactory.createEmptyBorder());
		details.setToolTipText(LINKAGE_DETAILS);
		tb.add(details);
		details.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (detailsWindow != null) {
					detailsWindow.toFront();
				} else {
					if (usedModel.length != 1) {
						detailsWindow = new DetailsDialog(parentWindow, comparedColumns, dataModel);
					} else {
						detailsWindow = new DetailsDialogSingleSource(parentWindow, dataModel);
					}
					detailsWindow.addWindowListener(new WindowAdapter() {
						public void windowClosed(WindowEvent e) {
							detailsWindow = null;
						}
						public void windowClosing(WindowEvent e) {
							detailsWindow = null;
						}
					});
					detailsWindow.setVisible(true);
				}
			}
		});
		
		tb.addSeparator();
		
		if (!acceptRejectOption) {
//			JButton minus = Configs.getAnalysisMinusButton();
//			minus.setMaximumSize(new Dimension(30, 30));
//			minus.setToolTipText(ANALYZE_MINUS);
//			tb.add(minus);
//			minus.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent e) {
//					JOptionPane.showMessageDialog(ViewLinkagesDialog.this, "FRIL-integrated viewer for not joined records is not yet implemented.\n\nTo see the not joined data, navigate to the disk location\nwhere the results are saved and locate the minus files.");
//				}
//			});
		} else {
			JButton addAll = new JButton(Configs.scale(Configs.addAllButtonIcon, 30, 30));
			addAll.setPreferredSize(new Dimension(30, 30));
			addAll.setMaximumSize(new Dimension(30, 30));
			addAll.setBorder(BorderFactory.createEmptyBorder());
			addAll.setToolTipText(ACCEPT_ALL_LINKAGES);
			tb.add(addAll);
			addAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					//int option = JOptionPane.showOptionDialog(ViewLinkagesDialog.this, ACCEPT_ALL_QUESTION, ACCEPT_ALL_LINKAGES, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, ACCEPT_ALL_BUTTONS, ACCEPT_ALL_BUTTONS[0]);
					doLinkages(true);
				}
			});
			
			JButton removeAll = new JButton(Configs.scale(Configs.removeAllButtonIcon, 30, 30));
			removeAll.setPreferredSize(new Dimension(30, 30));
			removeAll.setMaximumSize(new Dimension(30, 30));
			removeAll.setBorder(BorderFactory.createEmptyBorder());
			removeAll.setToolTipText(REJECT_ALL_LINKAGES);
			tb.add(removeAll);
			removeAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					//int option = JOptionPane.showOptionDialog(ViewLinkagesDialog.this, REJECT_ALL_QUESTION, REJECT_ALL_LINKAGES, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, REJECT_ALL_BUTTONS, REJECT_ALL_BUTTONS[0]);
					doLinkages(false);
				}
			});
		}
		
		tb.addSeparator();
		
		JButton prefs = new JButton(Configs.scale(Configs.configurationButtonIconBig, 30, 30));
		prefs.setPreferredSize(new Dimension(30, 30));
		prefs.setMaximumSize(new Dimension(30, 30));
		prefs.setBorder(BorderFactory.createEmptyBorder());
		prefs.setToolTipText(VIEW_PREFERENCES);
		tb.add(prefs);
		prefs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AbstractColumnConfigDialog dialog;
				if (dataModel.length != 1) {
					dialog = new ColumnConfigDialog(parentWindow, linkages.getColors(), comparedColumns, dataModel, usedModel);
				} else {
					dialog = new ColumnConfigDialogSingleSource(parentWindow, linkages.getColors(), dataModel, usedModel);
				}
				dialog.setLocationRelativeTo(parentWindow);
				if (dialog.getResult() == ColumnConfigDialog.RESULT_OK) {
					stopLoadingThread();
					synchronized (mutex) {
						usedModel = dialog.getConfiguredColumns();
						tableModel = new SpanTableModel(getDefaultColumns());
						linkages.setTableModel(tableModel, decodeUsedDistance(usedModel), decodeUsedCompColumns(usedModel));
						linkages.setColors(dialog.getColorConfig());
						LinkagesWindowPanel.this.details.clear();
					}
					loadingThread = threadCreator.createNewThread(threadCreator, LinkagesWindowPanel.this, LinkagesWindowPanel.this.filter, sortColumns, sortOrder);
					positionField.setText("1");
					loadingThread.start();
				}
			}
		});
		
		
		return tb;
	}
	
	private Object[] getDefaultColumns() {
		int extraCols = (acceptRejectOption ? 1 : 0) + (stratum != null ? 1 : 0);
		String[] names = new String[usedModel[0].length + extraCols + (usedModel.length == 1 ? 0 : 1)];
		if (acceptRejectOption) {
			names[0] = "Decision";
		}
		if (usedModel.length > 1) {
			names[acceptRejectOption ? 1 : 0] = CONFIDENCE;
		}
		if (stratum != null) {
			names[extraCols] = STRATUM_NAME;
		}
		
		if (usedModel.length == 1) {
			for (int i = 0; i < names.length - extraCols; i++) {
				names[i + extraCols] = (showSourceName ? usedModel[0][i].toString() : usedModel[0][i].getColumnName());
			}
		} else {
			for (int i = 1; i < names.length - extraCols; i++) {
				if (usedModel[0][i-1] == null) {
					names[i + extraCols] = " --- / " + (showSourceName ? usedModel[1][i-1].toString() : usedModel[1][i-1].getColumnName());
				} else if (usedModel[1][i-1] == null) {
					names[i + extraCols] = (showSourceName ? usedModel[0][i-1].toString() : usedModel[0][i-1].getColumnName()) + " / --- ";
				} else {
					names[i + extraCols] = (showSourceName ? usedModel[0][i-1].toString() : usedModel[0][i-1].getColumnName()) + " / " + 
									(showSourceName ? usedModel[1][i-1].toString() : usedModel[1][i-1].getColumnName());
				}
			}
		}
		return names;
	}
	
	public void addLinkage(DataRow linkage) throws InterruptedException {
		synchronized (mutex) {
			int extraColumns = (acceptRejectOption ? 1 : 0) + (stratum != null ? 1 : 0); 
			String[][] data = new String[2][usedModel[0].length + 1 + extraColumns];
			String conf = linkage.getProperty(AbstractJoin.PROPERTY_CONFIDNCE);
			if (conf == null) {
				conf = linkage.getData(CONFIDENCE).getValue().toString();
			}
			data[0][acceptRejectOption ? 1 : 0] = conf;
			for (int i = 1; i < data[0].length - extraColumns; i++) {
				if (usedModel[0][i - 1] != null) {
					data[0][i + extraColumns] = linkage.getData(usedModel[0][i - 1]).getValue().toString();
				} else {
					data[0][i + extraColumns] = null;
				}
				if (usedModel[1][i - 1] != null) {
					data[1][i + extraColumns] = linkage.getData(usedModel[1][i - 1]).getValue().toString();
				} else {
					data[1][i + extraColumns] = null;
				}
			}
			if (stratum != null) {
				data[0][acceptRejectOption ? 2 : 1] = linkage.getData(stratum).getValue().toString();
			}
			
			details.add(linkage);
			
			Adder doRun;
			if (acceptRejectOption) {
				if (stratum != null) {
					doRun = new Adder(data, new SpanInfo[] {new SpanInfo(0, 0, 1, 2), new SpanInfo(0, 1, 1, 2), new SpanInfo(0, 2, 1, 2)});
				} else {
					doRun = new Adder(data, new SpanInfo[] {new SpanInfo(0, 0, 1, 2), new SpanInfo(0, 1, 1, 2)});
				}
			} else {
				if (stratum != null) {
					doRun = new Adder(data, new SpanInfo[] {new SpanInfo(0, 0, 1, 2), new SpanInfo(0, 1, 1, 2)});
				} else {
					doRun = new Adder(data, new SpanInfo[] {new SpanInfo(0, 0, 1, 2)});
				}
			}
//			try {
//				SwingUtilities.invokeAndWait(doRun);
//			} catch (InvocationTargetException e) {
//				e.printStackTrace();
//			} catch (InterruptedException e) {
//				doRun.ignore();
//				throw e;
//			}
			SwingUtilities.invokeLater(doRun);
		}
		
	}
	
	public void addRecord(DataRow record) throws InterruptedException {
		synchronized (mutex) {
			int extraColumns = (acceptRejectOption ? 1 : 0) + (stratum != null ? 1 : 0); 
			String[][] data = new String[1][usedModel[0].length + extraColumns];
			for (int i = 0; i < data[0].length - extraColumns; i++) {
				data[0][i + extraColumns] = record.getData(usedModel[0][i]).getValue().toString();
			}
			if (stratum != null) {
				data[0][acceptRejectOption ? 2 : 1] = record.getData(stratum).getValue().toString();
			}
			
			details.add(record);
			
			Adder doRun = new Adder(data, new SpanInfo[] {});
			try {
				SwingUtilities.invokeAndWait(doRun);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				doRun.ignore();
				throw e;
			}
			
		}
		
	}

	public void setStatusBarMessage(String string) {
		statusMsg.setText(string);
	}
	
	public void setFilterOn(boolean filter) {
		filterStatus.setIcon(filter ? Configs.bulbOn : Configs.bulbOff);
		filterStatus.repaint();
	}
	
	public void setSortOn(boolean sort) {
		sortStatus.setIcon(sort ? Configs.bulbOn : Configs.bulbOff);
		sortStatus.repaint();
	}
	
	private class Adder implements Runnable {
		private String[][] data;
		private SpanInfo[] spanInfos;
		private volatile boolean ignore = false;
		public Adder(String[][] data, SpanInfo[] spanInfos) {
			this.data = data;
			this.spanInfos = spanInfos;
		}
		public void run() {
			if (!ignore) {
				tableModel.addSpannedRows(data, spanInfos);
			}
		}
		public void ignore() {
			ignore = true;
		}
	}

	public int getRecordsPerPage() {
		return RECORDS_PER_PAGE;
	}

	public void clearTable() {
		synchronized (mutex) {
			Clearer c = new Clearer();
			try {
				SwingUtilities.invokeAndWait(c);
				details.clear();
			} catch (InterruptedException e) {
				c.ignore = true;
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				c.ignore = true;
			}
		}
	}
	
	private class Clearer implements Runnable {

		boolean ignore = false;
		
		public void run() {
			if (!ignore) {
				tableModel = new SpanTableModel(getDefaultColumns());
				linkages.setTableModel(tableModel, true, decodeUsedDistance(usedModel), decodeUsedCompColumns(usedModel));
			}
		}
		
	}
	
	class Worker extends Thread {
		protected JDialog dialog;
		private boolean all;
		private String fileName;

		public void run() {
			try {
				sleep(500);
			} catch (InterruptedException e) {}
			loadingThread.saveToFile(fileName, all);
			dialog.setVisible(false);
		}
	}
	
	private void doLinkages(boolean acceptReject) {
		//stopLoadingThread();
		for (Iterator iterator = details.iterator(); iterator.hasNext();) {
			DataRow row = (DataRow) iterator.next();
			notifyAllListeners(acceptReject, row);
		}
		loadingThread.updateCursor();
	}
	
	private void notifyListeners(boolean accepted) {
		int rId = linkages.getSelectedLinkage();
		DataRow linkage = (DataRow) details.remove(rId);
		tableModel.removeRow(rId * 2);
		notifyAllListeners(accepted, linkage);
		loadingThread.updateCursor();
	}
	
	public void setVisible(boolean aFlag) {
		if (!aFlag) {
			stopLoadingThread();
		}
	}

	private void notifyAllListeners(boolean accepted, DataRow linkage) {
		for (Iterator iterator = decisionListeners.iterator(); iterator.hasNext();) {
			DecisionListener l = (DecisionListener) iterator.next();
			if (accepted) {
				l.linkageAccepted(linkage);
			} else {
				l.linkageRejected(linkage);
			}
		}
	}
	
	private void stopLoadingThread() {
		loadingThread.cancelReading();
		try {
			loadingThread.join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	
	public DataRow[] getVisibleRows() {
		return (DataRow[]) details.toArray(new DataRow[] {});
	}
	
	public void addDecisionListener(DecisionListener l) {
		decisionListeners.add(l);
	}
	
	public void removeDecisionListener(DecisionListener l) {
		decisionListeners.add(l);
	}
	
	public void removeAllDecisionListeners() {
		decisionListeners.clear();
	}

	public void cancelThread() {
		if (loadingThread != null) {
			loadingThread.cancelReading();
		}
	}
	
	public JPanel getStatusBar() {
		return statusBar;
	}
	
	public JToolBar getToolBar() {
		return toolBar;
	}

	private void resetTableSettings() {
		tableModel = new SpanTableModel(getDefaultColumns());
		//usedModel = dataModel;
		linkages.setTableModel(tableModel, true, decodeUsedDistance(usedModel), decodeUsedCompColumns(usedModel));
		details.clear();
	}
	
	private AbstractDistance[] decodeUsedDistance(DataColumnDefinition[][] usedModel) {
		if (usedModel.length == 1) {
			return null;
		}
		List dst = new ArrayList();
		for (int i = 0; i < usedModel[0].length; i++) {
			if (usedModel[0][i] != null && usedModel[1][i] != null) {
				int id = getComparedId(i, usedModel);
				dst.add(compareDistances[id]);
			}
		}
		return (AbstractDistance[]) dst.toArray(new AbstractDistance[] {});
	}

	private DataColumnDefinition[][] decodeUsedCompColumns(DataColumnDefinition[][] usedModel) {
		if (usedModel.length == 1) {
			return null;
		}
		List usedCols = new ArrayList();
		for (int i = 0; i < usedModel[0].length; i++) {
			if (usedModel[0][i] != null && usedModel[1][i] != null) {
				int id = getComparedId(i, usedModel);
				usedCols.add(new DataColumnDefinition[] {comparedColumns[0][id], comparedColumns[1][id]});
			}
		}
		return (DataColumnDefinition[][]) usedCols.toArray(new DataColumnDefinition[][] {});
	}
	
	private int getComparedId(int i, DataColumnDefinition[][] usedModel) {
		for (int j = 0; j < comparedColumns[0].length; j++) {
			if (comparedColumns[0][j].equals(usedModel[0][i]) && comparedColumns[1][j].equals(usedModel[1][i])) {
				return j;
			}
		}
		return -1;
	}
	
	public DataColumnDefinition[][] getUsedModel() {
		return usedModel;
	}
}
