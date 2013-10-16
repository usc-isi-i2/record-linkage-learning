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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import cdc.components.AbstractDataSource;
import cdc.configuration.ConfiguredSystem;
import cdc.gui.components.linkagesanalysis.LinkageResultsAnalysisProvider;
import cdc.gui.components.linkagesanalysis.MinusAnalysisProvider;
import cdc.gui.external.JXErrorDialog;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.specific.DataSourceWizard;
import cdc.gui.wizards.specific.DedupeResultSaverWizard;
import cdc.impl.deduplication.DeduplicationConfig;
import cdc.impl.deduplication.gui.DeduplicationWizard;
import cdc.utils.RJException;

public class DedupeSystemPanel extends SystemPanel {
	
	private static final String TOOLTIP_VIEW_RESULTS = "View results";
	private static final String TOOLTIP_CONFIG_SAVERS = "Configure results savers";
	private static final String TOOLTIP_CONFIG_LINK = "Configure linkage";
	private static final String TOOLTIP_CONFIG_DS = "Configure data source";
	private static final String TOOLTIP_VIEW_MINUS = "View not joined records";
	//private static final String BUTTON_LABEL_CONFIGURE = "Configure...";
	//private static final String BUTTON_LABEL_CREATE = "Create...";
	private static final String STATUS_NOT_CONFIGURED = "Not configured";
	private static final String STATUS_OK = "Status OK";
	private static final String STATUS_ERROR = "ERROR";
	
	private static final Color COLOR_ENABLED = new Color(18, 165, 8);
	private static final Color COLOR_DISABLED = new Color(245, 94, 8);

//	private class ConfigClosingListener implements ClosingSystemViewListener {
//		public boolean closing() {
//			return saveIfNeeded();
//		}
//	}
	
	private class SourceButtonListener implements ActionListener {
		private DataSourceWizard wizard = null;
		public void actionPerformed(ActionEvent e) {
			if (wizard != null) {
				wizard.bringToFront();
				return;
			}
			wizard = new DataSourceWizard(0, MainFrame.main, source, /*SystemPanel.this*/sourceButton, "sourceA", false);
			if (wizard.getResult() == AbstractWizard.RESULT_OK) {
				source = wizard.getConfiguredDataSource();
				wizard.dispose();
				configured(sourceButton, statSourceLabel);
				sourceName.setText("Name: " + source.getSourceName());
				updateSystem();
				checkSystemStatus();
				MainFrame.main.autosaveIfNeeded();
				//altered = true;
			}
			wizard = null;
		}
	}
	
	private class DedupeButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (source == null) {
				JOptionPane.showMessageDialog(MainFrame.main, "Cannot configure deduplication without configuration of data source.");
			} else {
				DeduplicationConfig old = source.getDeduplicationConfig();
				DeduplicationWizard wizard = new DeduplicationWizard(source, source.getDeduplicationConfig(), MainFrame.main, dedupeButton);
				if (wizard.getResult() == AbstractWizard.RESULT_OK) {
					DeduplicationConfig deduplicationConfig = wizard.getDeduplicationConfig();
					if (old != null) {
						deduplicationConfig.setDeduplicatedFileName(old.getDeduplicatedFileName());
						deduplicationConfig.setMinusFile(old.getMinusFile());
					}
					source.setDeduplicationConfig(deduplicationConfig);
					updateSystem();
					configured(dedupeButton, statDedupeLabel);
					//altered = true;
					MainFrame.main.autosaveIfNeeded();
				}	
			}
		}
	}
	
	private class ResultsSaversButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			DedupeResultSaverWizard wizard = new DedupeResultSaverWizard(MainFrame.main, saversButton);
			if (source != null && source.getDeduplicationConfig() != null) {
				wizard.setFileName(source.getDeduplicationConfig().getDeduplicatedFileName());
			}
			if (wizard.getResult() == AbstractWizard.RESULT_OK) {
				source.getDeduplicationConfig().setDeduplicatedFileName(wizard.getFileName());
				wizard.dispose();
				updateSystem();
				configured(saversButton, statSaversLabel);
				//altered = true;
				MainFrame.main.autosaveIfNeeded();
			}
		}
	}
	
	private class ViewResultsButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			try {
				MinusAnalysisProvider provider = new MinusAnalysisProvider(getSystem(), -1);
				viewResultsButton.setEnabled(false);
				JFrame dialog = provider.getFrame();
				dialog.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						viewResultsButton.setEnabled(true);
					}
				});
				dialog.setVisible(true);
			} catch (IOException e1) {
				JXErrorDialog.showDialog(MainFrame.main, "Error when opening the view", e1);
			} catch (RJException e1) {
				JXErrorDialog.showDialog(MainFrame.main, "Exception when opening the view", e1);
			}
		}
	}
	
	private class ViewMinusButtonListener implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			try {
				MinusAnalysisProvider provider = new MinusAnalysisProvider(getSystem(), 0);
				viewMinus.setEnabled(false);
				
				JFrame dialog = provider.getFrame();
				dialog.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						viewMinus.setEnabled(true);
					}
				});
				dialog.setVisible(true);
			} catch (IOException ex) {
				JXErrorDialog.showDialog(MainFrame.main, "Error", ex);
			} catch (RJException ex) {
				JXErrorDialog.showDialog(MainFrame.main, "Error", ex);
			}
		}
		
	}
	
	//System specification
	private JLabel sourceLabel;
	private JLabel dedupeLabel;
	private JLabel saversLabel;
	
	private JLabel sourceName;
	
	private JLabel statSourceLabel;
	private JLabel statDedupeLabel;
	private JLabel statSaversLabel;
	
	private JButton sourceButton;
	private JButton dedupeButton;
	private JButton saversButton;
	private JButton viewResultsButton;
	private JButton viewMinus;
	
	private JLabel filterSrc;
	
	private AbstractDataSource source;
	private ConfiguredSystem system;
	
	private ProcessPanel processPanel;
	
	//private boolean altered = false;

	public DedupeSystemPanel(MainFrame frame) {
		
		setLayout(null);
		
		//frame.addClosingSystemViewListener(new ConfigClosingListener());
		
		JLabel mode = new JLabel(Configs.dedupeModeIcon);
		mode.setBounds(527, 310, 225, 60);
		
		processPanel = new ProcessPanel(frame, this, new DedupeProcessStarter());
		
		sourceLabel = new JLabel("Data source");
		dedupeLabel = new JLabel("Deduplication");
		saversLabel = new JLabel("Result saver");
		sourceLabel.setHorizontalAlignment(JLabel.CENTER);
		dedupeLabel.setHorizontalAlignment(JLabel.CENTER);
		saversLabel.setHorizontalAlignment(JLabel.CENTER);
		
		sourceName = new JLabel("Name: <empty>");
		sourceName.setHorizontalAlignment(JLabel.CENTER);
		
		statSourceLabel = new JLabel(STATUS_NOT_CONFIGURED);
		statDedupeLabel = new JLabel(STATUS_NOT_CONFIGURED);
		statSaversLabel = new JLabel(STATUS_NOT_CONFIGURED);
		
		filterSrc = new JLabel(Configs.bulbOff);
		
		sourceButton = Configs.getConfigurationButton();
		dedupeButton = Configs.getConfigurationButton();
		saversButton = Configs.getConfigurationButton();
		viewResultsButton = Configs.getViewResultsButton();
		viewMinus = Configs.getViewMinusButton();
		sourceButton.setToolTipText(TOOLTIP_CONFIG_DS);
		dedupeButton.setToolTipText(TOOLTIP_CONFIG_LINK);
		saversButton.setToolTipText(TOOLTIP_CONFIG_SAVERS);
		viewResultsButton.setToolTipText(TOOLTIP_VIEW_RESULTS);
		viewMinus.setToolTipText(TOOLTIP_VIEW_MINUS);
		sourceButton.addActionListener(new SourceButtonListener());
		dedupeButton.addActionListener(new DedupeButtonListener());
		saversButton.addActionListener(new ResultsSaversButtonListener());
		viewMinus.addActionListener(new ViewMinusButtonListener());
		viewResultsButton.addActionListener(new ViewResultsButtonListener());
		viewResultsButton.setEnabled(false);
		viewMinus.setEnabled(false);
		
		sourceButton.setHorizontalAlignment(JLabel.CENTER);
		dedupeButton.setHorizontalAlignment(JLabel.CENTER);
		saversButton.setHorizontalAlignment(JLabel.CENTER);
		viewResultsButton.setHorizontalAlignment(JLabel.CENTER);
		sourceButton.setBounds(90, 205, 30, 30);
		viewMinus.setBounds(130, 205, 30, 30);
		dedupeButton.setBounds(385, 205, 30, 30);
		saversButton.setBounds(640, 205, 30, 30);
		viewResultsButton.setBounds(680, 205, 30, 30);
		
		sourceLabel.setBounds(50, 155, 150, 20);
		sourceName.setBounds(50, 170, 150, 20);
		dedupeLabel.setBounds(325, 170, 150, 20);
		saversLabel.setBounds(600, 170, 150, 20);
		
		statSourceLabel.setLocation(50, 145);
		statSourceLabel.setSize(150, 100);
		statSourceLabel.setIcon(Configs.systemComponentNotConfigured);
		statSourceLabel.setHorizontalTextPosition(JLabel.CENTER);
		statSourceLabel.setVerticalTextPosition(JLabel.CENTER);
		statSourceLabel.setForeground(COLOR_DISABLED);
		
		JLabel filterSrcALabel = new JLabel("Filter", JLabel.LEFT);
		filterSrcALabel.setBounds(110, 245, 100, 20);
		filterSrc.setBounds(90, 245, 20, 20);

		statDedupeLabel.setLocation(325, 145);
		statDedupeLabel.setSize(150, 100);
		statDedupeLabel.setIcon(Configs.systemComponentNotConfigured);
		statDedupeLabel.setHorizontalTextPosition(JLabel.CENTER);
		statDedupeLabel.setVerticalTextPosition(JLabel.CENTER);
		statDedupeLabel.setForeground(COLOR_DISABLED);
		
		statSaversLabel.setLocation(600, 145);
		statSaversLabel.setSize(150, 100);
		statSaversLabel.setIcon(Configs.systemComponentNotConfigured);
		statSaversLabel.setHorizontalTextPosition(JLabel.CENTER);
		statSaversLabel.setVerticalTextPosition(JLabel.CENTER);
		statSaversLabel.setForeground(COLOR_DISABLED);
		
		add(sourceLabel);
		add(sourceName);
		add(dedupeLabel);
		add(saversLabel);
		add(sourceButton);
		add(viewMinus);
		add(dedupeButton);
		add(saversButton);
		add(viewResultsButton);
		
		add(filterSrcALabel);
		add(filterSrc);
		
		add(statSourceLabel);
		add(statDedupeLabel);
		add(statSaversLabel);
		
		processPanel.setLocation(520, 10);
		add(processPanel);
		add(mode);
		
		setPreferredSize(new Dimension(760, 380));
	}
	
	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		super.paint(g);
		drawArrow(g, 196, 195, 325, 195);
		drawArrow(g, 496 - 25, 195, 600, 195);
		
		g.dispose();
		
	}

	private void drawArrow(Graphics g, int x1, int y1, int x2, int y2) {
		drawArrow((Graphics2D)g, x1, y1, x2, y2, (float)0.2);
	}
	
	public static void drawArrow(Graphics2D g2d, int xCenter, int yCenter, int x, int y, float stroke) {
		double aDir = Math.atan2(xCenter - x, yCenter - y);
		Polygon tmpPoly = new Polygon();
		int i1 = 10 + (int) (stroke * 2);
		int i2 = 10 + (int) stroke; // make the arrow head the same size regardless of the length length
		
		g2d.setStroke(new BasicStroke(2F));
		g2d.drawLine(x + xCor(i2, aDir), y + yCor(i2, aDir), xCenter, yCenter);
		
		g2d.setStroke(new BasicStroke(1f)); // make the arrow head solid even if dash pattern has been specified
		tmpPoly.addPoint(x, y); // arrow tip
		tmpPoly.addPoint(x + xCor(i1, aDir + .5), y + yCor(i1, aDir + .5));
		tmpPoly.addPoint(x + xCor(i2, aDir), y + yCor(i2, aDir));
		tmpPoly.addPoint(x + xCor(i1, aDir - .5), y + yCor(i1, aDir - .5));
		tmpPoly.addPoint(x, y); // arrow tip
		g2d.drawPolygon(tmpPoly);
		g2d.fillPolygon(tmpPoly); // remove this line to leave arrow head unpainted
	}

	private static int yCor(int len, double dir) {
		return (int) (len * Math.cos(dir));
	}

	private static int xCor(int len, double dir) {
		return (int) (len * Math.sin(dir));
	}
	
	private void updateSystem() {
		system = new ConfiguredSystem(source);
	}

	public ConfiguredSystem getSystem() {
		//return new ConfiguredSystem(this.sourceA, this.sourceB, this.join, this.resultSavers);
		return system;
	}
	
	public void setSystem(ConfiguredSystem system) throws RJException {
		this.system = system;
		
		AbstractDataSource dedupe = null;
		if (system.getSourceA() != null) {
			dedupe = system.getSourceA().getPreprocessedDataSource();
		}
		
		if (dedupe != system.getSourceA()) {
			configured(dedupeButton, statDedupeLabel);
		} else {
			disable(dedupeButton, statDedupeLabel);
		}
		
		this.source = system.getSourceA();
		if (this.source != null) {
			configured(sourceButton, statSourceLabel);
			this.sourceName.setText("Name: " + source.getSourceName());
		} else {
			disable(sourceButton, statSourceLabel);
		}
		
		if (source != null && source.getDeduplicationConfig() != null && source.getDeduplicationConfig().getDeduplicatedFileName() != null) {
			configured(saversButton, statSaversLabel);
		} else {
			disable(saversButton, statSaversLabel);
		}
		
		checkSystemStatus();
		
		//altered = false;
	}

//	public void systemSaved() {
//		altered = false;
//	}
	
	private void configured(JButton button, JLabel status) {
		status.setForeground(COLOR_ENABLED);
		status.setText(STATUS_OK);
		status.setIcon(Configs.systemComponentConfigured);
		//button.setText(BUTTON_LABEL_CONFIGURE);	
		checkSystemStatus();
	}
	
	private boolean checkSystemStatus() {
		processPanel.setConfiguredSystem(getSystem());
		
		if (getSystem() == null) {
			filterSrc.setIcon(Configs.bulbOff);
		} else {
		
			if (getSystem().getSourceA() != null) {
				if (getSystem().getSourceA().getFilter() != null && !getSystem().getSourceA().getFilter().isEmpty()) {
					filterSrc.setIcon(Configs.bulbOn);
				} else {
					filterSrc.setIcon(Configs.bulbOff);
				}
			}
		
		}
		
		if (source != null && source.getDeduplicationConfig() != null && source.getDeduplicationConfig().getDeduplicatedFileName() != null) {
			processPanel.setReady(true);
			return true;
		} else {
			processPanel.setReady(false);
			return false;
		}
	}

	private void disable(JButton button, JLabel statLabel) {
		statLabel.setForeground(COLOR_DISABLED);
		statLabel.setText(STATUS_NOT_CONFIGURED);
		statLabel.setIcon(Configs.systemComponentNotConfigured);
		//button.setText(BUTTON_LABEL_CREATE);
	}
	
	public void reportErrorLeftSource() {
		error(sourceButton, statSourceLabel);
	}
	
	public void reportErrorJoinSource() {
		error(dedupeButton, statDedupeLabel);
	}
	
	public void reportErrorResultSavers() {
		error(saversButton, statSaversLabel);
	}
	
	public void reportErrorRightSource() {
		throw new RuntimeException("Should not happen!");
	}
	
	private void error(JButton button, JLabel status) {
		//status.setBorder(BorderFactory.createLineBorder(Color.RED));
		status.setText(STATUS_ERROR);
		checkSystemStatus();
	}

	public void unloadConfiguration() {
		
		unload(sourceButton, statSourceLabel);
		unload(dedupeButton, statDedupeLabel);
		unload(saversButton, statSaversLabel);
		sourceName.setText("<empty>");
		
		viewResultsButton.setEnabled(false);
		viewMinus.setEnabled(false);
		
		filterSrc.setIcon(Configs.bulbOff);
	}

	private void unload(JButton button, JLabel label) {
		label.setText(STATUS_NOT_CONFIGURED);
		//button.setText(BUTTON_LABEL_CREATE);
		label.setForeground(Color.RED);
	}

	public void openLeftDataSourceConfig(Window parent) {
		sourceButton.doClick();
	}
	
	public ProcessPanel getProcessPanel() {
		return processPanel;
	}

//	public void setAltered(boolean b) {
//		altered = true;
//	}

//	public boolean saveIfNeeded() {
//		if (altered) {
//			int result = JOptionPane.showConfirmDialog(DedupeSystemPanel.this, "Current deduplication configuration was changed. Do you want to save it?", "Save new configuration", JOptionPane.YES_NO_CANCEL_OPTION);
//			if (result == JOptionPane.YES_OPTION) {
//				if (MainFrame.main.saveCurrentConfiguration(true)) {
//					systemSaved();
//				}
//			} else if (result == JOptionPane.CANCEL_OPTION) {
//				return false;
//			} else {
//				//delete tmp config
//				MainFrame.main.surrenderConfiguration();
//			}
//		}
//		return true;
//	}

	public void openLinkagesDialog() {
		try {
			LinkageResultsAnalysisProvider provider = new LinkageResultsAnalysisProvider(getSystem().getJoin());
			JFrame dialog = provider.getFrame();
			viewResultsButton.setEnabled(false);
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					viewResultsButton.setEnabled(true);
				}
				public void windowClosed(WindowEvent e) {
					viewResultsButton.setEnabled(true);
				}
			});
			dialog.setVisible(true);
		} catch (IOException e1) {
			JXErrorDialog.showDialog(MainFrame.main, "Error when opening the view", e1);
		} catch (RJException e1) {
			JXErrorDialog.showDialog(MainFrame.main, "Exception when opening the view", e1);
		}
	}

	public void setViewButtonEnabled(boolean b) {
		this.viewResultsButton.setEnabled(true);
		if (getSystem().getSourceA().getDeduplicationConfig() != null && getSystem().getSourceA().getDeduplicationConfig().getMinusFile() != null) {
			this.viewMinus.setEnabled(true);
			this.viewMinus.setToolTipText(TOOLTIP_VIEW_MINUS);
		} else {
			this.viewMinus.setEnabled(false);
			this.viewMinus.setToolTipText(TOOLTIP_VIEW_MINUS + " (requires the option of summary of not joined data to be enabled)");
		}
	}
	
	public void cleanup() {
		if (processPanel != null) {
			this.processPanel.cleanup();
		}
		this.processPanel = null;
		this.system = null;
	}
}
