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


package cdc.gui;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractResultsSaver;
import cdc.datamodel.DataRow;
import cdc.gui.components.progress.DedupeInfoPanel;
import cdc.gui.external.JXErrorDialog;
import cdc.impl.deduplication.DeduplicationDataSource;
import cdc.impl.resultsavers.CSVFileSaver;
import cdc.utils.Log;
import cdc.utils.RJException;

public class DeduplicationThread extends StoppableThread {
	
	//private Animation animation;
	private volatile DedupeInfoPanel info;
	private AbstractDataSource source;
	private String fileLocation;
	private volatile boolean stopped = false;
	private long t1;
	private long t2;
	private int n;
	int nDup;
	
	public DeduplicationThread(AbstractDataSource source, DedupeInfoPanel info) {
		this.source = source.getPreprocessedDataSource();
		this.info = info;
		this.fileLocation = source.getDeduplicationConfig().getDeduplicatedFileName();
	}
	
	public void run() {
		n = 0;
		DataRow row;
		//JFrame frame = null;
		try {
			
			sleep(1000);
			
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					info.getProgressBar().setIndeterminate(false);
				}
			});
			
			source.reset();
			
			new PollingThread(info, (DeduplicationDataSource)source).start();
			
			System.gc();
			
			Map props = new HashMap();
			props.put(CSVFileSaver.SAVE_SOURCE_NAME, "false");
			props.put(CSVFileSaver.SAVE_CONFIDENCE, "false");
			props.put(CSVFileSaver.OUTPUT_FILE_PROPERTY, fileLocation);
			AbstractResultsSaver saver = new CSVFileSaver(props);
			
			t1 = System.currentTimeMillis();
			while ((row = source.getNextRow()) != null) {
				n++;
				saver.saveRow(row);
				if (stopped) {
					break;
				}
				
			}
			t2 = System.currentTimeMillis();
			saver.flush();
			saver.close();
			
			nDup = ((DeduplicationDataSource)source).getDuplicatesCount();
			Log.log(getClass(), "Deduplication completed. Identified " + nDup + " duplicates. Elapsed time: " + (t2 - t1) + "ms.", 1);
			closeProgress();
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					if (stopped) {
						Log.log(getClass(), "Deduplication was cancelled", 1);
					}
					MainFrame.main.setCompletedLinkageSummary(MainFrame.main.getConfiguredSystem(), stopped, t2-t1, n);
				}
			});
			
		} catch (RJException e) {
			JXErrorDialog.showDialog(MainFrame.main, "Error while deduplicating data", e);
			closeProgress();
		} catch (IOException e) {
			JXErrorDialog.showDialog(MainFrame.main, "Error while deduplicating data", e);
			closeProgress();
		} catch (Exception e) {
			JXErrorDialog.showDialog(MainFrame.main, "Error while deduplicating data", e);
			closeProgress();
		} finally {
			try {
				source.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (RJException e) {
				e.printStackTrace();
			}
		}
		
		info = null;
		source = null;
	}


	private void closeProgress() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					info.dedupeCompleted();
				}
			});
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public void scheduleStop() {
		this.stopped = true;
		((DeduplicationDataSource)source).cancel();
	}
	
	private class PollingThread extends Thread {
		
		private DedupeInfoPanel panel;
		private DeduplicationDataSource source;
		
		public PollingThread(DedupeInfoPanel panel, DeduplicationDataSource system) {
			this.panel = panel;
			this.source = system;
		}
		
		public void run() {
			while (DeduplicationThread.this.info != null) {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							panel.getProgressBar().setValue(source.getProgress());
							panel.setDuplicatesCount(source.getDuplicatesCount());
						}
					});
					sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
			this.panel = null;
			this.source = null;
		}
		
	}
	
}
