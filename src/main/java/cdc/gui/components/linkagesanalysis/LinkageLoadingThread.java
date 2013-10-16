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


package cdc.gui.components.linkagesanalysis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractResultsSaver;
import cdc.components.Filter;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.gui.components.linkagesanalysis.dialog.LinkagesWindowPanel;
import cdc.gui.external.JXErrorDialog;
import cdc.impl.datasource.wrappers.ExternallySortingDataSource;
import cdc.impl.resultsavers.CSVFileSaver;
import cdc.utils.CompareFunctionInterface;
import cdc.utils.RJException;
import cdc.utils.RowUtils;
import cdc.utils.comparators.NumberComparator;
import cdc.utils.comparators.StringComparator;

public class LinkageLoadingThread extends LoadingThread {

	private LinkagesWindowPanel dialog;
	private ThreadCreatorInterface tCInterface;
	private AbstractDataSource results;
	private volatile boolean cancel;
	private Filter filter;
	private DataColumnDefinition[] sort;
	private int[] order;
	
	private int totalRecords = 0;
	private int maxPage;
	private AtomicInteger move = new AtomicInteger(1);
	private int currentPage = 1;

	public LinkageLoadingThread(ThreadCreatorInterface interf, LinkagesWindowPanel viewLinkagesDialog, Filter filter, DataColumnDefinition[] sort, int[] order) {
		this.tCInterface = interf;
		this.dialog = viewLinkagesDialog;
		this.filter = filter;
		this.sort = sort;
		this.order = order;
	}
	
	private void doStartup() {
		try {
			results = tCInterface.getDataSource(filter);
			while (results.getNextRow() != null) {
				totalRecords++;
			}
			results.reset();
			maxPage = (int) Math.ceil(totalRecords / (double)dialog.getRecordsPerPage());
		} catch (IOException e) {
			JXErrorDialog.showDialog(SwingUtilities.getWindowAncestor(dialog), "Error reading linkage results", e);
		} catch (RJException e) {
			JXErrorDialog.showDialog(SwingUtilities.getWindowAncestor(dialog), "Error reading linkage results", e);
		}
	}

	public void run() {
		doStartup();
		
		if (results == null) {
			return;
		}
		
		DataRow row = null;
		dialog.setStatusBarMessage("Loading linkages...");
		dialog.setSortOn(sort != null && sort.length != 0);
		dialog.setFilterOn(filter != null && !filter.isEmpty());
		
		setPriority(MIN_PRIORITY);
		
		//give window some time to load nicely
		try {
			sleep(100);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
				
		try{	
			if (sort != null) {
				CompareFunctionInterface[] compareFunctions = new CompareFunctionInterface[sort.length];
				for (int i = 0; i < compareFunctions.length; i++) {
					if (isConfidence(sort[i])) {
						compareFunctions[i] = new NumberComparator(order[i]);
					} else {
						compareFunctions[i] = new StringComparator(order[i]);
					}
				}
				results = new ExternallySortingDataSource("results", results, sort, compareFunctions, new HashMap());
			}
			int loaded = 0;
			while (!cancel) {
				row = results.getNextRow();
				if (loaded == dialog.getRecordsPerPage() || row ==  null) {
					while (move.get() == currentPage) {
						String msg = getStatusMsg(loaded, cancel);
						dialog.setStatusBarMessage(msg);
						synchronized (this) {
							try {
								//System.out.println("Move: " + move.get() + "   curr: " + currentPage);
								wait();
							} catch (InterruptedException e) {
								return;
							}
						}
					}
					currentPage = currentPage + 1;
					int go = move.get();
					if (currentPage > go) {
						//need to reset source
						results.reset();
						currentPage = 1;
						row = results.getNextRow();
					}
					loaded = 0;
					dialog.setStatusBarMessage("Loading linkages...");
					dialog.clearTable();
				}
				try {
					//if not the page that should be shown, then skip showing, just read quickly records...
					if (currentPage == move.get() && row != null) {
						dialog.addLinkage(row);
					}
				} catch (InterruptedException e) {return;}
				if (row != null) {
					loaded++;
				}
			}
		} catch (IOException e) {
			JXErrorDialog.showDialog(SwingUtilities.getWindowAncestor(dialog), "Error reading linkage results", e);
		} catch (RJException e) {
			JXErrorDialog.showDialog(SwingUtilities.getWindowAncestor(dialog), "Error reading linkage results", e);
		} finally {
			if (results != null) {
				try {
					results.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (RJException e) {
					e.printStackTrace();
				}
				results = null;
			}
		}
	}
	
	private boolean isConfidence(DataColumnDefinition dataColumnDefinition) {
		return dataColumnDefinition.getColumnName().equals("Confidence");
	}

	private String getStatusMsg(int loaded, boolean cancel) {
		if (cancel) {
			return "Loading linkages was canceled.";
		}
		return loaded + " linkages loaded. Page " + currentPage + " of " + maxPage + ".";
	}

	public void cancelReading() {
		this.cancel = true;
		interrupt();
	}
	
	public boolean moveCursorForward() {
		int sched = move.get();
		if (sched + 1 <= maxPage) {
			move.set(sched + 1);
			synchronized (this) {
				notifyAll();
			}
			return true;
		} else {
			return false;
		}
	}
	
	public boolean moveCursorBackward() {
		int sched = move.get();
		if (sched - 1 > 0) {
			move.set(sched - 1);
			synchronized (this) {
				notifyAll();
			}
			return true;
		} else {
			return false;
		}
	}
	
	public boolean moveCursorToPage(int page) {
		if (page > 0 && page <= maxPage) {
			move.set(page);
			synchronized (this) {
				notifyAll();
			}
			return true;
		} else {
			return false;
		}
	}

	public void updateCursor() {
		synchronized (this) {
			notifyAll();
		}
	}
	
	public void saveToFile(String fileName, boolean all) {
		try {
			Map props = new HashMap();
			props.put(CSVFileSaver.OUTPUT_FILE_PROPERTY, fileName);
			props.put(CSVFileSaver.SAVE_CONFIDENCE, "true");
			props.put(CSVFileSaver.SAVE_SOURCE_NAME, "false");
			AbstractResultsSaver saver = new CSVFileSaver(props);
			DataColumnDefinition[] model = RowUtils.getModelForSave(dialog.getUsedModel());
			if (all) {
				AbstractDataSource src = results.copy();
				DataRow row;
				while ((row = src.getNextRow()) != null) {
					saver.saveRow(RowUtils.buildSubrow(row, model, true));
				}
			} else {
				DataRow[] rows = dialog.getVisibleRows();
				for (int i = 0; i < rows.length; i++) {
					saver.saveRow(RowUtils.buildSubrow(rows[i], model, true));
				}
			}
			saver.flush();
			saver.close();
		} catch (IOException e) {
			JXErrorDialog.showDialog(SwingUtilities.getWindowAncestor(dialog), "Error saving data", e);
		} catch (RJException e) {
			JXErrorDialog.showDialog(SwingUtilities.getWindowAncestor(dialog), "Error saving data", e);
		}
		
	}
	
}
