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

import java.awt.BorderLayout;
import java.awt.CheckboxMenuItem;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import cdc.components.AbstractResultsSaver;
import cdc.configuration.ConfigurationPhase;
import cdc.configuration.ConfiguredSystem;
import cdc.gui.components.dialogs.OneTimeTipDialog;
import cdc.gui.components.uicomponents.MemoryInfoComponent;
import cdc.gui.components.uicomponents.PropertiesPanel;
import cdc.gui.components.uicomponents.StatusBarLinkageInfoPanel;
import cdc.gui.external.JXErrorDialog;
import cdc.impl.FrilAppInterface;
import cdc.impl.MainApp;
import cdc.impl.resultsavers.CSVFileSaver;
import cdc.impl.resultsavers.DeduplicatingResultsSaver;
import cdc.impl.resultsavers.ResultSaversGroup;
import cdc.utils.CPUInfo;
import cdc.utils.GuiUtils;
import cdc.utils.Log;
import cdc.utils.LogSink;
import cdc.utils.Props;
import cdc.utils.RJException;
import cdc.utils.Utils;

public class MainFrame extends JFrame implements FrilAppInterface {

	private static int MAX_LOG_LINES = Props.getInteger("max-log-lines");
	
	private static final String PERSISTENT_PARAM_FIRST_TIME = "new-run";
	private static final String PERSISTENT_PROPERTIES_FILE_NAME = "properties.bin";
	
	public static final String VERSION_PROPERTY_CODENAME = "codename";
	public static final String VERSION_PROPERTY_V = "version";
	public static final String VERSION_LIST_OF_CHANGES_FILE = "changelog.txt";

	private class GUILogSink extends LogSink {

		private JTextArea log;

		public GUILogSink(JTextArea log) {
			this.log = log;
		}

		public void log(String msg) {
			synchronized (log) {
				log.append(msg + "\n");
				log.setCaretPosition(log.getText().length());
				if (log.getLineCount() > MAX_LOG_LINES) {
					Element el = log.getDocument().getRootElements()[0].getElement(0);
					try {
						log.getDocument().remove(0, el.getEndOffset());
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static MainFrame main;

	private MenuBar menuBar = new MenuBar();
	private CheckboxMenuItem autosave;
	private CheckboxMenuItem linkage;
	private CheckboxMenuItem dedupe;
	
	private SystemPanel applicationPanel;
	private JPanel logPanel;
	private JPanel applicationWrappingPanel;

	private List closingListeners = new ArrayList();

	private Map persistentParams = null;
	private Properties propertiesVersion = new Properties();
	private SavedConfigManager configManager;
	
	private StatusBarLinkageInfoPanel statusBarLinkageInfo;

	private int cpus;

	public void setPersistentParam(String paramName, String paramValue) {
		if (persistentParams == null) {
			if (!attemptLoad()) {
				persistentParams = new HashMap();
			}
		}
		persistentParams.put(paramName, paramValue);
		try {
			ObjectOutputStream os = new ObjectOutputStream(
					new FileOutputStream(PERSISTENT_PROPERTIES_FILE_NAME));
			os.writeObject(persistentParams);
			os.flush();
			os.close();
		} catch (IOException e) {
			System.out.println("[WARN] CANNOT save to file: "
					+ PERSISTENT_PROPERTIES_FILE_NAME);
			e.printStackTrace();
		}
	}

	public String getPersistentParam(String paramName) {
		if (persistentParams == null) {
			if (persistentParams == null) {
				if (!attemptLoad()) {
					persistentParams = new HashMap();
				}
			}
		}
		return (String) persistentParams.get(paramName);
	}

	private boolean attemptLoad() {
		try {
			ObjectInputStream is = new ObjectInputStream(new FileInputStream(
					PERSISTENT_PROPERTIES_FILE_NAME));
			persistentParams = (Map) is.readObject();
			return true;
		} catch (Exception e) {
			System.out.println("[INFO] [this is not an error]: CANNOT read file: " + PERSISTENT_PROPERTIES_FILE_NAME);
		}
		return false;
	}

	public MainFrame() {
		super("FRIL: A Fine-Grained Record Linkage Tool");
		super.setSize(800, 600);
		super.setIconImage(Configs.appIcon);

		main = this;
		MainApp.main = this;
		
		try {
			propertiesVersion.load(new FileInputStream("version.properties"));
			setTitle(getTitle() + " "
					+ propertiesVersion.getProperty(VERSION_PROPERTY_CODENAME));
		} catch (IOException e) {
			System.out.println("[ERROR] Cannot read version.properties....");
			e.printStackTrace();
		}

		createMenu();
		createMainWindow();

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(screenSize.width / 2 - (getSize().width / 2),
				screenSize.height / 2 - (getSize().height / 2));
		this.setVisible(true);

		doStartup();
		
	}

	private void doStartup() {

		this.cpus = CPUInfo.testNumberOfCPUs();
		Log.log(getClass(), "Number of available CPUs: " + this.cpus);
		
		if (getPersistentParam(PERSISTENT_PARAM_FIRST_TIME + propertiesVersion.getProperty(VERSION_PROPERTY_V)) == null) {
			new AboutWindow().setVisible(true);
			setPersistentParam(PERSISTENT_PARAM_FIRST_TIME + propertiesVersion.getProperty(VERSION_PROPERTY_V), "false");	
		}
		
		OneTimeTipDialog.showInfoDialogIfNeeded(OneTimeTipDialog.LINKAGE_MODE_DEFAULT, OneTimeTipDialog.LINKAGE_MODE_MESSAGE);
		
		configManager = new SavedConfigManager();
		File f = configManager.getFileToLoad();
		if (f != null) {
			loadConfiguration(f);
		}
		
	}

	private void createMainWindow() {

		applicationWrappingPanel = new JPanel(new BorderLayout());	
		createSystemPanel();

		logPanel = new JPanel(new GridBagLayout());
		JTextArea logArea = new JTextArea();
		logArea.setEditable(false);
		JScrollPane scroll = new JScrollPane(logArea);
		logPanel.add(scroll, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		Log.setSinks(new LogSink[] { new GUILogSink(logArea) });
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, applicationWrappingPanel, logPanel);
		splitPane.setDividerLocation(400);

		setLayout(new BorderLayout());
		add(splitPane, BorderLayout.CENTER);
		
		JPanel statusBar = new JPanel(new GridBagLayout());
		JPanel mm = new MemoryInfoComponent(1000);
		mm.setBorder(BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
		statusBarLinkageInfo = new StatusBarLinkageInfoPanel();
		logPanel.add(statusBarLinkageInfo.getDetailsPanel(), new GridBagConstraints(0, 0, 1, 1, 0.4, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		statusBar.add(statusBarLinkageInfo, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		statusBar.add(mm, new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		add(statusBar, BorderLayout.SOUTH);
	}

	private void createMenu() {
		super.setMenuBar(menuBar);
		super.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		super.addWindowListener(new WindowListener() {
			public void windowActivated(WindowEvent e) {
			}

			public void windowClosed(WindowEvent e) {
			}

			public void windowClosing(WindowEvent e) {
				if (closing()) {
					Log.log(getClass(), "Closing application");
					main.dispose();
					System.exit(0);
				}
			}

			public void windowDeactivated(WindowEvent e) {
			}

			public void windowDeiconified(WindowEvent e) {
			}

			public void windowIconified(WindowEvent e) {
			}

			public void windowOpened(WindowEvent e) {
			}
		});

		Menu[] menu = new Menu[4];

		menu[0] = new Menu("File");
		loadFileMenu(menu[0]);
		
		menu[1] = new Menu("Mode");
		loadModeMenu(menu[1]);

		menu[2] = new Menu("Tools");
		loadToolsMenu(menu[2]);

		menu[3] = new Menu("Help");
		loadHelpMenu(menu[3]);

		for (int i = 0; i < menu.length; i++) {
			menuBar.add(menu[i]);
		}
	}

	private void loadModeMenu(Menu menu) {
		linkage = new CheckboxMenuItem("Linkage mode", true);
		dedupe = new CheckboxMenuItem("Deduplcation mode");
		linkage.setEnabled(false);
		dedupe.setEnabled(true);
		
		linkage.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (linkage.getState()) {
					if (applicationPanel instanceof DedupeSystemPanel) {
						//need to save config...
						if (!fireClosingSystemViewListeners()) {
							linkage.setState(false);
							return;
						}
						clearClosingSystemViewListeners();
					}
					activateLinkagePanel();
				}
				configureSystemView();
			}
		});
		
		dedupe.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (dedupe.getState()) {
					if (applicationPanel instanceof LinkageSystemPanel) {
						//need to save config...
						if (!fireClosingSystemViewListeners()) {
							dedupe.setState(false);
							return;
						}
						clearClosingSystemViewListeners();
					}
					activateDeduplicationPanel();
				}
				configureSystemView();
				
			}
		});
		
		menu.add(linkage);
		menu.add(dedupe);
		
	}

	private void loadFileMenu(Menu menu) {
		MenuItem newItem = new MenuItem("New");
		newItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (fireClosingSystemViewListeners()) {
					try {
						applicationPanel.setSystem(new ConfiguredSystem(null, null, null, null));
						applicationPanel.unloadConfiguration();
						configManager = new SavedConfigManager(null);
						statusBarLinkageInfo.clearCurrentSummary();
					} catch (RJException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		MenuItem open = new MenuItem("Open");
		open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				if (!fireClosingSystemViewListeners()) {
					return;
				}
				
				File dir = configManager.getDefaultDir();
				JFileChooser chooser = new JFileChooser(dir);
				if (chooser.showOpenDialog(main) == JFileChooser.APPROVE_OPTION) {
					// will load configuration
					if (applicationPanel.getSystem() != null
							&& applicationPanel.getSystem().getJoin() != null) {
						try {
							applicationPanel.getSystem().getJoin().close();
						} catch (IOException e1) {
							e1.printStackTrace();
						} catch (RJException e1) {
							e1.printStackTrace();
						}
					}
					File f = chooser.getSelectedFile();
					//Use config manager. This will make sure we load backup config if there exists one
					configManager = new SavedConfigManager(f);
					loadConfiguration(configManager.getFileToLoad());
				}
			}
		});
		MenuItem save = new MenuItem("Save");
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				configManager.saveConfiguration(false);
			}
		});
		MenuItem saveAs = new MenuItem("Save as...");
		saveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				configManager.saveConfiguration(true);
			}
		});
		MenuItem exit = new MenuItem("Exit");
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (closing()) {
					main.dispose();
				}
			}
		});

		menu.add(newItem);
		menu.add(open);
		menu.add(save);
		menu.add(saveAs);
		menu.add(exit);

	}

	private void loadToolsMenu(Menu menu) {
		
		MenuItem gc = new MenuItem("Run garbage collection");
		gc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.gc();
			}
		});
		menu.add(gc);

		menu.addSeparator();
		
		autosave = new CheckboxMenuItem("Enable autosave");
		autosave.setState(true);
		menu.add(autosave);
		
		MenuItem prefs = new MenuItem("Preferences");
		prefs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JPanel mainPanel = new JPanel(new BorderLayout());
				JTabbedPane tabs = new JTabbedPane();
				PropertiesPanel prefs = new PropertiesPanel(Props
						.getProperties());
				PropertiesPanel logs = new PropertiesPanel(Log.getProperties());
				tabs.addTab("General preferences", prefs);
				tabs.addTab("Logging preferences", logs);
				mainPanel.add(tabs, BorderLayout.CENTER);
				mainPanel
						.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
				OptionDialog dialog = new OptionDialog(main, "Preferences");
				dialog.setPreferredSize(new Dimension(500, 450));
				dialog.setMainPanel(mainPanel);
				if (dialog.getResult() == OptionDialog.RESULT_OK) {
					if (JOptionPane
							.showConfirmDialog(
									main,
									"This operation requires restarting the application.\nDo you want to close it now?",
									"Restart required",
									JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
						try {
							if (closing()) {
								Props.saveProperties(prefs.getProperties());
								Log.saveProperties(logs.getProperties());
								main.setVisible(false);
								main.dispose();
							}
						} catch (IOException e) {
							JXErrorDialog.showDialog(main, e
									.getLocalizedMessage(), e);
						}
					}
				}
			}
		});
		menu.add(prefs);
	}

	private void loadHelpMenu(Menu menu) {
		MenuItem help = new MenuItem("Help");
		help.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(MainFrame.this, "Help not yet available");
			}
		});
		MenuItem about = new MenuItem("About...");
		about.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new AboutWindow().setVisible(true);
			}
		});
		menu.add(help);
		menu.add(about);
	}

	public void addClosingSystemViewListener(ClosingSystemViewListener listener) {
		closingListeners.add(listener);
	}

	private boolean closing() {
		System.out.println("[INFO] Application closing. Please wait for cleanup.");
		Log.log(MainFrame.this.getClass(), "Application is being closed. Please wait for cleanup...", 1);
		if (!fireClosingSystemViewListeners()) {
			return false;
		}
		if (MainFrame.this.getConfiguredSystem() != null) {
			MainFrame.this.getConfiguredSystem().close();
		}
		Log.log(MainFrame.this.getClass(), "Cleanup completed.", 1);

		return true;
	}

	private boolean fireClosingSystemViewListeners() {
		for (Iterator iterator = closingListeners.iterator(); iterator.hasNext();) {
			ClosingSystemViewListener listener = (ClosingSystemViewListener) iterator.next();
			if (!listener.closing()) {
				return false;
			}
		}
		return true;
	}
	
	private void clearClosingSystemViewListeners() {
		closingListeners.clear();
	}

	private void loadConfiguration(File f) {
		
		applicationPanel.unloadConfiguration();
		
		String[] phases = new String[ConfigurationPhase.phases.length];
		for (int i = 0; i < phases.length; i++) {
			phases[i] = ConfigurationPhase.phases[i].getPhaseName();
		}
		ConfigLoadDialog progressReporter = new ConfigLoadDialog(phases);
		ConfigLoaderThread thread = new ConfigLoaderThread(f, progressReporter);
		progressReporter.addCancelListener(new CancelThreadListener(thread));
		thread.start();
		progressReporter.setLocation(GuiUtils.getCenterLocation(this, progressReporter));
		progressReporter.started();
	}

	public ConfiguredSystem getConfiguredSystem() {
		return applicationPanel.getSystem();
	}

	public String getMinusDirectory() {
		AbstractResultsSaver savers = this.applicationPanel.getSystem().getResultSaver();
		AbstractResultsSaver[] group = null;
		if (savers instanceof CSVFileSaver) {
			return ((CSVFileSaver) savers).getActiveDirectory();
		} else if (savers instanceof DeduplicatingResultsSaver) {
			group = ((DeduplicatingResultsSaver)savers).getChildren();
		} else if (savers instanceof ResultSaversGroup) {
			group = ((ResultSaversGroup)savers).getChildren();
		}
		if (group != null) {
			for (int i = 0; i < group.length; i++) {
				if (group[i] instanceof CSVFileSaver) {
					return ((CSVFileSaver) group[i]).getActiveDirectory();
				}
			}
		}
		return "";
	}

	public void configurationReadDone() {
		try {
			configManager.configLoaded();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public SystemPanel getSystemPanel() {
		return applicationPanel;
	}
	
	public void autosaveIfNeeded() {
		configManager.configChanged();
		if (autosave.getState()) {
			Log.log(getClass(), "Saving backup configuration (autosave enabled).");
			configManager.saveBackup();
		}
	}

	public Properties getPropertiesVersion() {
		return propertiesVersion;
	}

	public void openLinkagesDialog() {
		((LinkageSystemPanel)applicationPanel).openLinkagesDialog();
	}

	public void setSystem(ConfiguredSystem system) throws RJException {
		if (system.isDeduplication() && !dedupe.getState()) {
			clearClosingSystemViewListeners();
			activateDeduplicationPanel();
			configureSystemView();
		} else if (!system.isDeduplication() && !linkage.getState()) {
			clearClosingSystemViewListeners();
			activateLinkagePanel();
			configureSystemView();
		}
		applicationPanel.setSystem(system);
	}

	private void activateDeduplicationPanel() {
		linkage.setState(false);
		dedupe.setState(true);
		dedupe.setEnabled(false);
		linkage.setEnabled(true);
	}

	private void activateLinkagePanel() {
		linkage.setState(true);
		dedupe.setState(false);
		dedupe.setEnabled(true);
		linkage.setEnabled(false);
	}
	
	private void createSystemPanel() {
		if (this.applicationPanel != null) {
			this.applicationPanel.cleanup();
		}
		this.applicationPanel = linkage.getState() ? (SystemPanel)new LinkageSystemPanel(this) : new DedupeSystemPanel(this);
		JScrollPane appScroll = new JScrollPane(applicationPanel);
		applicationWrappingPanel.removeAll();
		applicationWrappingPanel.add(appScroll, BorderLayout.CENTER);
		addClosingSystemViewListener(new ClosingSystemViewListener() {
			public boolean closing() {
				if (configManager == null) {
					return true;
				}
				return configManager.closingConfiguration();
			}
		});
	}
	
	private void configureSystemView() {
		createSystemPanel();
		applicationWrappingPanel.validate();
		applicationWrappingPanel.repaint();
	}

	public void setDeduplicationMode(boolean deduplication) {
		clearClosingSystemViewListeners();
		if (deduplication) {
			activateDeduplicationPanel();
		} else {
			activateLinkagePanel();
		}
		configureSystemView();
	}

	public void surrenderConfiguration() {
		configManager.deleteBackup();
		configManager = new SavedConfigManager(null);
	}

	public void setCompletedLinkageSummary(ConfiguredSystem system, boolean cancelled, long time, int linkages) {
		statusBarLinkageInfo.linkageCompleted(system, cancelled, time, linkages);
		JOptionPane.showMessageDialog(MainFrame.main, Utils.getSummaryMessage(system, cancelled, time, linkages));
	}

}
