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

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import cdc.configuration.Configuration;
import cdc.configuration.ConfiguredSystem;
import cdc.utils.Log;


public class SavedConfigManager {
	
	public static final String UNNAMED_BACKUP_SUFFIX = ".xml~~";
	public static final String PERSISTENT_PARAM_RECENT_CONFIG = "recent-config";
	public static final String PERSISTENT_PARAM_RECENT_BACKUP_CONFIG = "recent-backup-config";
	public static final String CONFIG_DIR = "./config";
	public static final String PERSISTENT_PARAM_RECENT_PATH = "recent-path";
	
	//Indication of read configuration
	private boolean configurationRead = false;
	
	//Indication of saved configuration
	private boolean configurationSaved = false;
	
	//Indication of changed configuration
	private boolean configurationChanged = false;
	
	private File file;
	private File fileUnnamedBackup;
	
	/**
	 * Creates a completely new configuration that has not yet been saved.
	 */
	public SavedConfigManager() {
		
		//First check for unnamed backup file
		//If present, attempt to load
		String backupNoName = null;
		if ((backupNoName = unnamedBackupExists()) != null) {
			//this is an unnamed backup file...
			if (JOptionPane.showConfirmDialog(MainFrame.main,
					"There exists an unsaved backup copy of FRIL configuration.\nWould you like to revocer it?",
					"Recover configuration", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				fileUnnamedBackup = new File(backupNoName);
				configurationChanged = true;
				return;
			} else {
				deleteBackupFile(backupNoName);
			}
		}
		
		//Now check for a regular configuration that was recently used
		String recentConfig = MainFrame.main.getPersistentParam(PERSISTENT_PARAM_RECENT_CONFIG);
		if (recentConfig != null) {
			if (JOptionPane.showConfirmDialog(MainFrame.main,
							"The system was closed using the following configuration file:\n"
									+ recentConfig + "\nWould you like to load it?",
							"Load last active configuration?",
							JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				file = new File(recentConfig);
			}
		}
		
	}

	/**
	 * Creates pre-saved configuration. Will do restoring if needed.
	 * @param fileName the existing configuration
	 */
	public SavedConfigManager(File fileName) {
		//System.out.println("Constructor! " + fileName);
		//new Exception().printStackTrace();
		this.file = fileName;
		if (fileName != null) {
			MainFrame.main.setPersistentParam(PERSISTENT_PARAM_RECENT_PATH, fileName.getParent());
		}
	}
	
	private String unnamedBackupExists() {
		String backupIfAny = MainFrame.main.getPersistentParam(PERSISTENT_PARAM_RECENT_BACKUP_CONFIG);
		if (backupIfAny != null && backupIfAny.endsWith(UNNAMED_BACKUP_SUFFIX)) {
			return backupIfAny;
		} else {
			return null;
		}
	}
	
	/**
	 * Returns file that should be loaded.
	 * @return configuration file that should be loaded (takes into account backups). null if no configuration to load.
	 */
	public File getFileToLoad() {
		//check backup
		if (file == null) {
			if (fileUnnamedBackup != null) {
				return fileUnnamedBackup;
			} else {
				return null;
			}
		}
		File f = file;
		String backup = file.getAbsolutePath() + "~";
		File fBackup = new File(backup);
		if (fBackup.exists()) {
			if (JOptionPane.showConfirmDialog(MainFrame.main, "Backup copy of the configuration file exists.\nDo you want to recover?",
					"Backup copy exists", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				//Need to use the backup file.
				f = fBackup;
				configurationChanged = true;
			} else {
				//User said no to loading previously unsaved configuration. Safe to delete.
				deleteBackupFile(backup);
			}
		}
		return f;
	}
	
	/**
	 * Notifies configuration manager about successfully loaded configuration.
	 * @throws IOException 
	 */
	public void configLoaded() throws IOException {
		if (fileUnnamedBackup == null) {
			MainFrame.main.setPersistentParam(PERSISTENT_PARAM_RECENT_CONFIG, file.getCanonicalPath());
			configurationRead = true;
		}
	}
	
	/**
	 * Saves current configuration.
	 * @param newName true if user should be asked about the name of out file
	 * @return true if configuration successfully saved
	 */
	public boolean saveConfiguration(boolean newName, String defaultDirectory) {
		if (MainFrame.main.getConfiguredSystem() == null) {
			return true;
		}

		File dir = null;
		while (true) {
			File f = null;
			
			//Let's first check whether we need to ask for a new file name
			if (newName || !configurationRead || file == null) {
				//This is a configuration that will be saved into a new file
				//Need to ask for that file
				dir = new File(defaultDirectory);
				if (!dir.exists() || !dir.isDirectory()) {
					dir = new File(".");
				}
				JFileChooser chooser = new JFileChooser(dir);
				if (chooser.showSaveDialog(MainFrame.main) == JFileChooser.APPROVE_OPTION) {
					// Will save configuration
					f = chooser.getSelectedFile();
					if (!f.getName().endsWith(".xml")) {
						f = new File(f.getAbsolutePath() + ".xml");
					}
					if (f.exists()) {
						if (JOptionPane.showConfirmDialog(MainFrame.main, "File "
								+ f.getPath() + " already exists.\nOverwrite?",
								"File exists", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
							continue;
						}
					}
				} else {
					return false;
				}
			} else {
				//We will just reuse the file that was saved before
				f = file;
			}

			configurationSaved = saveConfiguration(f);
			if (configurationSaved) {
				//Save OK, can delete backup and update file pointer
				deleteBackup();
				configurationChanged = false;
				file = f;
				try {
					MainFrame.main.setPersistentParam(PERSISTENT_PARAM_RECENT_CONFIG, file.getCanonicalPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return configurationSaved;
		}
	}
	
	/**
	 * Saves backup configuration. Should be invoked every time a backup should be saved.
	 * @return true if saving backup was a success
	 */
	public boolean saveBackup() {
		
		String tmpFile = null;
		String prevBackup = MainFrame.main.getPersistentParam(PERSISTENT_PARAM_RECENT_BACKUP_CONFIG);
		
		//Check for a name of backup file.
		if (configurationSaved || configurationRead) {
			//I can use the tmp file as filename.xml~
			tmpFile = MainFrame.main.getPersistentParam(PERSISTENT_PARAM_RECENT_CONFIG) + "~";
		} else {
			//This is truly temporary configuration (will use x.xml~~, where x is a time stamp)
			String dir = CONFIG_DIR;
			tmpFile = dir + File.separator + System.currentTimeMillis() + UNNAMED_BACKUP_SUFFIX;
			fileUnnamedBackup = new File(tmpFile);
		}
		
		//Save the temporary file, and remember it in the persistent params
		File tmp = new File(tmpFile);
		saveConfiguration(tmp);
		if (prevBackup != null && prevBackup.endsWith(UNNAMED_BACKUP_SUFFIX)) {
			//Delete only if it is a time-stamped backup file. Otherwise, no need to delete.
			deleteBackupFile(prevBackup);
		}
		if (tmpFile.endsWith(UNNAMED_BACKUP_SUFFIX)) {
			MainFrame.main.setPersistentParam(PERSISTENT_PARAM_RECENT_BACKUP_CONFIG, tmpFile);
		}
		//MainFrame.main.setPersistentParam(PERSISTENT_PARAM_RECENT_BACKUP_CONFIG, tmpFile);
		return true;
	}
	
	private void deleteBackupFile(String fileName) {
		File f = new File(fileName);
		if (!f.delete()) {
			Log.log(getClass(), "Backup configuration could not be deleted: " + fileName);
			//new Exception().printStackTrace();
		} else {
			Log.log(getClass(), "Backup configuration deleted: " + fileName);
		}
		MainFrame.main.setPersistentParam(PERSISTENT_PARAM_RECENT_BACKUP_CONFIG, null);
	}
	
	private boolean saveConfiguration(File file) {
		ConfiguredSystem system = MainFrame.main.getConfiguredSystem();
		Configuration.saveToXML(system, file);
		Log.log(getClass(), "Configuration was saved to file: " + file);
		return true;
	}

	public void deleteBackup() {
		String backup;
		if (fileUnnamedBackup != null) {
			backup = fileUnnamedBackup.getAbsolutePath();
			fileUnnamedBackup = null;
		} else {
			backup = file.getAbsolutePath() + "~";
		}
		deleteBackupFile(backup);
	}

	public boolean closingConfiguration() {
		if (configurationChanged) {
			int result = JOptionPane.showConfirmDialog(MainFrame.main, "Current linkage configuration was changed. Do you want to save it?", "Save new configuration", JOptionPane.YES_NO_CANCEL_OPTION);
			if (result == JOptionPane.YES_OPTION) {
				return saveConfiguration(true);
			} else if (result == JOptionPane.CANCEL_OPTION) {
				return false;
			} else {
				//delete tmp config
				MainFrame.main.surrenderConfiguration();
				return true;
			}
		} else {
			return true;
		}
	}
	
	public boolean saveConfiguration(boolean newName) {
		String dir = MainFrame.main.getPersistentParam(PERSISTENT_PARAM_RECENT_PATH);
		if (dir == null) {
			dir = CONFIG_DIR;
		}
		
		if (saveConfiguration(newName, dir)) {
			MainFrame.main.configurationReadDone();
			String recentPath = getFileToLoad().getParent();
			if (recentPath != null) {
				MainFrame.main.setPersistentParam(PERSISTENT_PARAM_RECENT_PATH, recentPath);
			}
			return true;
		} else {
			return false;
		}
	}

	public void configChanged() {
		this.configurationChanged = true;
	}

	public File getDefaultDir() {
		File dir = null;
		if (MainFrame.main.getPersistentParam(PERSISTENT_PARAM_RECENT_PATH) != null) {
			dir = new File(MainFrame.main.getPersistentParam(PERSISTENT_PARAM_RECENT_PATH));
		} else {
			dir = new File(CONFIG_DIR);
		}
		if (!dir.exists() || !dir.isDirectory()) {
			dir = new File(".");
		}
		return dir;
	}
	
	
}
