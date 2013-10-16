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

import javax.swing.SwingUtilities;

import cdc.configuration.ConfiguredSystem;
import cdc.datamodel.DataRow;
import cdc.gui.components.progress.JoinInfoPanel;
import cdc.gui.external.JXErrorDialog;
import cdc.utils.Log;
import cdc.utils.RJException;

public class LinkageThread extends StoppableThread {
	
	private class PollingThread extends Thread {
		JoinInfoPanel panel;
		private ConfiguredSystem system;
		public PollingThread(JoinInfoPanel panel, ConfiguredSystem system) {
			this.panel = panel;
			this.system = system;
		}
		public void run() {
			while (LinkageThread.this.info != null) {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							panel.getProgressBar().setValue(system.getJoin().getProgress());
						}
					});
					sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
			this.panel = null;
			this.system = null;
		}
	}
	
	//private Animation animation;
	private volatile JoinInfoPanel info;
	private ConfiguredSystem system;
	private volatile boolean stopped = false;
	private long t1;
	private long t2;
	private int n;
	
	public LinkageThread(ConfiguredSystem system, JoinInfoPanel info) {
		this.system = system;
		this.info = info;
	}
	
	public void run() {
		n = 0;
		DataRow row;
		//JFrame frame = null;
		try {
			
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					info.getProgressBar().setIndeterminate(!system.getJoin().isProgressSupported());
				}
			});
			
			System.gc();
			
			system.getJoin().reset();
			if (system.getJoin().isProgressSupported()) {
				new PollingThread(info, system).start();
			}
			system.getResultSaver().reset();
			
			t1 = System.currentTimeMillis();
			while ((row = system.getJoin().joinNext()) != null) {
				n++;
				system.getResultSaver().saveRow(row);
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						info.incrementJoined();
					}
				});
				if (stopped) {
					break;
				}
				
			}
			system.getResultSaver().flush();
			t2 = System.currentTimeMillis();
			system.getResultSaver().close();
			system.getJoin().closeListeners();
			//system.getJoin().close();
			
			Log.log(getClass(), system.getJoin() + ": Algorithm produced " + n + " joined tuples. Elapsed time: " + (t2 - t1) + "ms.", 1);
			closeProgress();
			//animation.stopAnimation();
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					if (stopped) {
						Log.log(getClass(), "Linkage was cancelled", 1);
					}
					MainFrame.main.setCompletedLinkageSummary(system, stopped, t2 - t1, n);
					//JOptionPane.showMessageDialog(MainFrame.main, Utils.getSummaryMessage(system, stopped, t2 - t1, n));
				}

			});
			
		} catch (RJException e) {
			JXErrorDialog.showDialog(MainFrame.main, "Error while joining data", e);
			closeProgress();
		} catch (IOException e) {
			JXErrorDialog.showDialog(MainFrame.main, "Error while joining data", e);
			closeProgress();
		} catch (Exception e) {
			JXErrorDialog.showDialog(MainFrame.main, "Error while joining data", e);
			closeProgress();
		}
		
		system.getJoin().setCancelled(false);
		info = null;
		system = null;
	}

	private void closeProgress() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					info.joinCompleted();
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
		this.system.getJoin().setCancelled(true);
		//this.interrupt();
	}
	
}
