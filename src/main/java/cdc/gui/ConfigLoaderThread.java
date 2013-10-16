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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import cdc.components.AbstractDataSource;
import cdc.configuration.Configuration;
import cdc.configuration.ConfigurationListener;
import cdc.configuration.ConfigurationPhase;
import cdc.configuration.ConfiguredSystem;
import cdc.gui.external.JXErrorDialog;
import cdc.utils.RJException;

public class ConfigLoaderThread extends StoppableThread {
	
	private class ConfigListener implements ConfigurationListener {
		private ConfigurationPhase phase;
		private int state;
		public void configurationEvent(Configuration configuration, ConfigurationPhase phase, int state) {
			this.phase = phase;
			this.state = state;
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						int phaseId = ConfigurationPhase.getPhaseIndex(ConfigListener.this.phase);
						if (ConfigListener.this.state == ConfigurationPhase.START) {
							dialog.startPhase(phaseId);
						} else if (ConfigListener.this.state == ConfigurationPhase.END) {
							dialog.endPhase(phaseId);
						} else {
							//error
							if (ConfigListener.this.phase.equals(ConfigurationPhase.loadingLeftSourcePhase)) {
								MainFrame.main.getSystemPanel().reportErrorLeftSource();
							} else if (ConfigListener.this.phase.equals(ConfigurationPhase.loadingRightSourcePhase)) {
								MainFrame.main.getSystemPanel().reportErrorRightSource();
							} else if (ConfigListener.this.phase.equals(ConfigurationPhase.loadingJoinProcessPhase)) {
								MainFrame.main.getSystemPanel().reportErrorJoinSource();
							} else if (ConfigListener.this.phase.equals(ConfigurationPhase.loadingResultSaversPhase)) {
								MainFrame.main.getSystemPanel().reportErrorResultSavers();
							} else {
								//phase unknown??
							}
						}
					}
				});
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		public void configurationModeDetermined(boolean deduplication) {
			MainFrame.main.setDeduplicationMode(deduplication);
		}
		public void systemUpdated(ConfiguredSystem system) {
			try {
				MainFrame.main.setSystem(system);
			} catch (RJException e) {
				JXErrorDialog.showDialog(MainFrame.main, "Error reading configuration", e);
			}
			
		}
	}
	
	private ConfigLoadDialog dialog;
	private File f;
	
	private volatile boolean stopScheduled = false;
	
	public ConfigLoaderThread(File f, ConfigLoadDialog dialog) {
		this.f = f;
		this.dialog = dialog;
		setPriority(Thread.MIN_PRIORITY);
	}
	
	public void run() {
		try {
			new Configuration(f.getAbsolutePath(), false, new ConfigListener());
			//ConfiguredSystem system = config.getSystem();
			if (!stopScheduled) {
				MainFrame.main.configurationReadDone();
			}
			Configuration.stopForced = false;
			AbstractDataSource.requestStop(false);
		} catch (RJException e) {
			JXErrorDialog.showDialog(dialog, "Error reading configuration file", e);
		} catch (IOException e) {
			JXErrorDialog.showDialog(dialog, "Error reading configuration file", e);
		} catch (Exception e) {
			JXErrorDialog.showDialog(dialog, "Error reading configuration file", e);
		}
		try {
			Thread.sleep(500);
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					dialog.dispose();
				}
			});
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public void scheduleStop() {
		AbstractDataSource.requestStop(true);
		Configuration.stopForced = true;
		stopScheduled = true;
		interrupt();
	}
	
}
