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
import cdc.utils.comparators.StringComparator;

public class MinusLoadingThread extends LoadingThread {

	private LinkagesWindowPanel dialog;
	private ThreadCreatorInterface tCInterface;
	private volatile boolean cancel = false;
	private Filter filter;
	private DataColumnDefinition[] sort;
	private int[] order;
	
	private int maxPages;
	private int currentPage = 1;
	private AtomicInteger page = new AtomicInteger(1);
	
	public MinusLoadingThread(ThreadCreatorInterface interf, LinkagesWindowPanel viewLinkagesDialog, Filter filter, DataColumnDefinition[] sort, int[] order) {
		this.dialog = viewLinkagesDialog;
		this.tCInterface = interf;
		this.filter = filter;
		this.sort = sort;
		this.order = order;
	}
	
	public void run() {
		AbstractDataSource src = null;
		try {
			src = createSource();
			
			dialog.setStatusBarMessage("Loading records...");
			dialog.setSortOn(sort != null && sort.length != 0);
			dialog.setFilterOn(filter != null && !filter.isEmpty());
			
			while (!cancel) {
				if (currentPage > page.get()) {
					src.reset();
					currentPage = 1;
				}
				try {
					while (!cancel && currentPage < page.get()) {
						src.getNextRows(dialog.getRecordsPerPage());
						currentPage++;
					}
					int loaded = 0;
					DataRow row;
					dialog.clearTable();
					while (!cancel && loaded != dialog.getRecordsPerPage() && (row = src.getNextRow()) != null) {
						//add only relevant records...
						if (currentPage == page.get()) {
							dialog.addRecord(row);
						}
						loaded++;
					}
					dialog.setStatusBarMessage(getMessage());
					currentPage++;
					synchronized (this) {
						if (currentPage == page.get() + 1) {
							wait();
						}
					}
				} catch (InterruptedException e) {
					
				}
			}
		} catch (RJException e) {
			JXErrorDialog.showDialog(SwingUtilities.getWindowAncestor(dialog), "Error reading file", e);
		} catch (IOException e) {
			JXErrorDialog.showDialog(SwingUtilities.getWindowAncestor(dialog), "Error reading file", e);
		} finally {
			if (src != null) {
				try {
					src.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (RJException e) {
					e.printStackTrace();
				}
				src = null;
			}
		}
	}

	private AbstractDataSource createSource() throws IOException, RJException {
		AbstractDataSource src;
		src = tCInterface.getDataSource(filter);
		testNumberOfRecords(src);
		if (sort != null) {
			CompareFunctionInterface[] compareFunctions = new CompareFunctionInterface[sort.length];
			for (int i = 0; i < compareFunctions.length; i++) {
				compareFunctions[i] = new StringComparator(order[i]);
			}
			src = new ExternallySortingDataSource("m-", src, sort, compareFunctions, new HashMap());
		}
		return src;
	}
	
	private String getMessage() {
		return "Linkages loaded. Page " + currentPage + " of " + maxPages;
	}

	private void testNumberOfRecords(AbstractDataSource src) throws IOException, RJException {
		int n = 0;
		while (src.getNextRow() != null) {
			n++;
		}
		src.reset();
		maxPages = (int) Math.ceil(n / (double)dialog.getRecordsPerPage());
	}

	public void cancelReading() {
		cancel = true;
		interrupt();
	}

	public boolean moveCursorBackward() {
		synchronized (this) {
			int c = page.get();
			if (c - 1 > 0) {
				page.set(c - 1);
				notifyAll();
				return true;
			} else {
				return false;
			}
		}
	}

	public boolean moveCursorForward() {
		synchronized (this) {
			int c = page.get();
			if (c + 1 <= maxPages) {
				page.set(c + 1);
				notifyAll();
				return true;
			} else {
				return false;
			}
		}
	}

	public boolean moveCursorToPage(int page) {
		return false;
	}

	public void updateCursor() {
		synchronized (this) {
			notifyAll();
		}
		interrupt();
	}

	public void saveToFile(String fileName, boolean all) {
		try {
			Map props = new HashMap();
			props.put(CSVFileSaver.OUTPUT_FILE_PROPERTY, fileName);
			props.put(CSVFileSaver.SAVE_CONFIDENCE, "false");
			props.put(CSVFileSaver.SAVE_SOURCE_NAME, "false");
			AbstractResultsSaver saver = new CSVFileSaver(props);
			DataColumnDefinition[] model = RowUtils.getModelForSave(dialog.getUsedModel());
			if (all) {
				AbstractDataSource src = createSource();
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
