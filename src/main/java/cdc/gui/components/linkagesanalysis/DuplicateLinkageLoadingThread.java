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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import cdc.components.AbstractResultsSaver;
import cdc.components.Filter;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.gui.components.linkagesanalysis.dialog.LinkagesWindowPanel;
import cdc.gui.external.JXErrorDialog;
import cdc.impl.resultsavers.CSVFileSaver;
import cdc.utils.CompareFunctionInterface;
import cdc.utils.RJException;
import cdc.utils.RowUtils;
import cdc.utils.comparators.NumberComparator;
import cdc.utils.comparators.StringComparator;

public class DuplicateLinkageLoadingThread extends LoadingThread {
	
	private List data;
	private List toRemove = new ArrayList();
	
	private LinkagesWindowPanel dialog;
	private volatile boolean cancel;
	private volatile boolean reload;
	private Filter filter;
	private DataColumnDefinition[] sort;
	private int[] order;
	
	//private int position = 0;
	
	private int maxPage;
	private AtomicInteger move = new AtomicInteger(1);
	private int currentPage = 1;
	
	public DuplicateLinkageLoadingThread(List data, ThreadCreatorInterface interf, LinkagesWindowPanel viewLinkagesDialog, Filter filter, DataColumnDefinition[] sort, int[] order) {
		this.data = data;
		this.dialog = viewLinkagesDialog;
		this.filter = filter;
		this.sort = sort;
		this.order = order;
	}
	
	public void run() {
		
		dialog.setStatusBarMessage("Linkages deduplication...");
		dialog.setSortOn(sort != null && sort.length != 0);
		dialog.setFilterOn(filter != null && !filter.isEmpty());
		
		setPriority(MIN_PRIORITY);
		
		//give window some time to load nicely
		try {
			sleep(100);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
				
		synchronized (data) {
			maxPage = (int) Math.ceil(data.size() / (double)dialog.getRecordsPerPage());
			if (sort != null) {
				CompareFunctionInterface[] compareFunctions = new CompareFunctionInterface[sort.length];
				for (int i = 0; i < compareFunctions.length; i++) {
					if (isConfidence(sort[i])) {
						compareFunctions[i] = new NumberComparator(order[i]);
					} else {
						compareFunctions[i] = new StringComparator(order[i]);
					}
				}
				sortData(sort, compareFunctions);
			}
		}
			
		int loaded = 0;
		while (!cancel) {
			try {
				reload = false;
				dialog.clearTable();
				loaded = 0;
				
				synchronized (data) {
					//can uncomment, but need to change how current page is calculated in the window
//					if (move.get() > maxPage) {
//						move.set(maxPage);
//					}
					int position = (move.get() - 1) * dialog.getRecordsPerPage();
					int currPage = move.get();
					while (!reload && currPage == move.get()) {
						maxPage = (int) Math.ceil(data.size() / (double)dialog.getRecordsPerPage());
						// position = (currentPage - 1) * dialog.getRecordsPerPage();
						checkToRemove();
						while (!reload && loaded < dialog.getRecordsPerPage() && data.size() > position) {
							DataRow row = (DataRow) data.get(position++);
							dialog.addLinkage(row);
							loaded++;
						}
						if (reload) {
							break;
						}
						String msg = getStatusMsg(loaded, cancel);
						dialog.setStatusBarMessage(msg);
						try {
							data.wait();
						} catch (InterruptedException e) {
							if (cancel) return;
						}
					}
					maxPage = (int) Math.ceil(data.size() / (double)dialog.getRecordsPerPage());
				}
			} catch (InterruptedException e) {
				if (cancel) return;
			}
		}
	}
	
//	private boolean isInPage(int position, int page) {
//		//position here is next to take
//		int boundLower = (page - 1) * dialog.getRecordsPerPage();
//		int boundUpper = (page) * dialog.getRecordsPerPage() - 1;
//		return position >= boundLower && position < boundUpper;
//	}

	private void checkToRemove() {
		synchronized (toRemove) {
			for (Iterator iterator = toRemove.iterator(); iterator.hasNext();) {
				DataRow row = (DataRow) iterator.next();
				data.remove(row);
			}
			reload = !toRemove.isEmpty();
			toRemove.clear();
		}
	}

	private void sortData(DataColumnDefinition[] sort, CompareFunctionInterface[] compareFunctions) {
		Collections.sort(data, new SortComparator(sort, compareFunctions));
	}

	private String getStatusMsg(int loaded, boolean cancel) {
		if (cancel) {
			return "Loading linkages was canceled.";
		}
		return loaded + " linkages loaded. Page " + currentPage + " of " + maxPage + ".";
	}
	
	private boolean isConfidence(DataColumnDefinition dataColumnDefinition) {
		return dataColumnDefinition.getColumnName().equals("Confidence");
	}
	
	public void cancelReading() {
		cancel = true;
		interrupt();
	}

	public boolean moveCursorForward() {
		int sched = move.get();
		if (sched + 1 <= maxPage) {
			move.set(sched + 1);
			synchronized (data) {
				data.notifyAll();
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
			synchronized (data) {
				data.notifyAll();
			}
			return true;
		} else {
			return false;
		}
	}
	
	public boolean moveCursorToPage(int page) {
		if (page > 0 && page <= maxPage) {
			move.set(page);
			synchronized (data) {
				data.notifyAll();
			}
			return true;
		} else {
			return false;
		}
	}

	public void updateCursor() {
		synchronized (data) {
			reload = true;
			data.notifyAll();
		}
	}

	private class SortComparator implements Comparator {

		private DataColumnDefinition[] sort;
		private CompareFunctionInterface[] compareFunctions;
		
		public SortComparator(DataColumnDefinition[] sort, CompareFunctionInterface[] compareFunctions) {
			this.compareFunctions = compareFunctions;
			this.sort = sort;
		}

		public int compare(Object arg0, Object arg1) {
			DataRow r1 = (DataRow)arg0;
			DataRow r2 = (DataRow)arg1;
			
			for (int i = 0; i < sort.length; i++) {
				DataCell c1 = r1.getData(sort[i]);
				DataCell c2 = r2.getData(sort[i]);
				int cmp = compareFunctions[i].compare(c1, c2);
				if (cmp != 0) {
					return cmp;
				}
			}
			return 0;
		}
		
	}

	public void removeLinkage(DataRow linkage) {
		synchronized (toRemove) {
			toRemove.add(linkage);
		}
		interrupt();
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
				synchronized (data) {
					for (Iterator iterator = data.iterator(); iterator.hasNext();) {
						DataRow row = (DataRow) iterator.next();
						saver.saveRow(RowUtils.buildSubrow(row, model, true));
					}
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
