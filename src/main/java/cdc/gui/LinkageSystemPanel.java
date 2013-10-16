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
import cdc.components.AbstractJoin;
import cdc.components.AbstractResultsSaver;
import cdc.configuration.ConfiguredSystem;
import cdc.gui.components.linkagesanalysis.LinkageResultsAnalysisProvider;
import cdc.gui.components.linkagesanalysis.MinusAnalysisProvider;
import cdc.gui.external.JXErrorDialog;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.specific.DataSourceWizard;
import cdc.gui.wizards.specific.JoinWizard;
import cdc.gui.wizards.specific.ResultsSaversWizard;
import cdc.impl.resultsavers.DeduplicatingResultsSaver;
import cdc.utils.RJException;

public class LinkageSystemPanel extends SystemPanel {
	
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
	
	private class SourceAButtonListener implements ActionListener {
		private DataSourceWizard wizard = null;
		public void actionPerformed(ActionEvent e) {
			if (wizard != null) {
				wizard.bringToFront();
				return;
			}
			wizard = new DataSourceWizard(0, MainFrame.main, sourceA, /*SystemPanel.this*/sourceAButton, "sourceA");
			if (wizard.getResult() == AbstractWizard.RESULT_OK) {
				sourceA = wizard.getConfiguredDataSource();
				wizard.dispose();
				configured(sourceAButton, statSourceALabel);
				sourceAName.setText("Name: " + sourceA.getSourceName());
				if (join != null) {
					try {
						if (!join.newSourceA(sourceA.getPreprocessedDataSource())) {
							disable(joinButton, statJoinLabel);
							join.close();
							join = null;
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (RJException e1) {
						e1.printStackTrace();
					}
				}
				updateSystem();
				checkSystemStatus();
				MainFrame.main.autosaveIfNeeded();
				//altered = true;
				viewResultsButton.setEnabled(false);
			}
			wizard = null;
		}
	}
	
	private class SourceBButtonListener implements ActionListener {
		private DataSourceWizard wizard = null;
		public void actionPerformed(ActionEvent e) {
			if (wizard != null) {
				wizard.bringToFront();
				return;
			}
			wizard = new DataSourceWizard(1, MainFrame.main, sourceB, /*SystemPanel.this*/sourceBButton, "sourceB");
			if (wizard.getResult() == AbstractWizard.RESULT_OK) {
				sourceB = wizard.getConfiguredDataSource();
				wizard.dispose();
				configured(sourceBButton, statSourceBLabel);
				sourceBName.setText("Name: " + sourceB.getSourceName());
				if (join != null) {
					try {
						if (!join.newSourceB(sourceB.getPreprocessedDataSource())) {
							disable(joinButton, statJoinLabel);
							join.close();
							join = null;
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (RJException e1) {
						e1.printStackTrace();
					}
				}
				updateSystem();
				checkSystemStatus();
				MainFrame.main.autosaveIfNeeded();
				//altered = true;
				viewResultsButton.setEnabled(false);
			}
			wizard = null;
		}
	}
	
	private class JoinButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (sourceA == null || sourceB == null) {
				JOptionPane.showMessageDialog(MainFrame.main, "Cannot configure join without configuration of both datasources.");
			} else {
				try {
					JoinWizard wizard = new JoinWizard(MainFrame.main, sourceA, sourceB, join, LinkageSystemPanel.this);
					if (wizard.getResult() == AbstractWizard.RESULT_OK) {
						join = wizard.getConfiguredJoin();
						join.enableJoinStatistics();
						wizard.dispose();
						updateSystem();
						configured(joinButton, statJoinLabel);
						//altered = true;
						MainFrame.main.autosaveIfNeeded();
						viewResultsButton.setEnabled(false);
					}
				} catch (RJException ex) {
					JXErrorDialog.showDialog(MainFrame.main, "Error", ex);
				}
			}
		}
	}
	
	private class ResultsSaversButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (getSystem() == null || getSystem().getJoin() == null) {
				JOptionPane.showMessageDialog(MainFrame.main, "Please configure linkage before result savers.", "Linkage configuration required", JOptionPane.INFORMATION_MESSAGE);
			} else {
				ResultsSaversWizard wizard = new ResultsSaversWizard(MainFrame.main, LinkageSystemPanel.this);
				wizard.loadConfig(resultSavers);
				if (wizard.getResult() == AbstractWizard.RESULT_OK) {
					resultSavers = wizard.getConfiguredResultSaver();
					wizard.dispose();
					updateSystem();
					configured(saversButton, statSaversLabel);
					//altered = true;
					MainFrame.main.autosaveIfNeeded();
				}
			}
		}
	}
	
	private class ViewResultsButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			LinkageResultsAnalysisProvider provider;
			try {
				provider = new LinkageResultsAnalysisProvider(getSystem().getJoin());
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

		private int srcId;
		
		public ViewMinusButtonListener(int srcId) {
			this.srcId = srcId;
		}
		
		public void actionPerformed(ActionEvent e) {
			try {
				MinusAnalysisProvider provider = new MinusAnalysisProvider(getSystem(), srcId);
				if (srcId == 1) {
					viewMinusA.setEnabled(false);
				} else {
					viewMinusB.setEnabled(false);
				}
				JFrame dialog = provider.getFrame();
				dialog.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						if (srcId == 1) {
							viewMinusA.setEnabled(true);
						} else {
							viewMinusB.setEnabled(true);
						}
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
	private JLabel sourceALabel;
	private JLabel sourceBLabel;
	private JLabel joinLabel;
	private JLabel saversLabel;
	
	private JLabel sourceAName;
	private JLabel sourceBName;
	
	private JLabel statSourceALabel;
	private JLabel statSourceBLabel;
	private JLabel statJoinLabel;
	private JLabel statSaversLabel;
	
	private JButton sourceAButton;
	private JButton sourceBButton;
	private JButton joinButton;
	private JButton saversButton;
	private JButton viewResultsButton;
	private JButton viewMinusA;
	private JButton viewMinusB;
	
	private JLabel filterSrcA;
	private JLabel filterSrcB;
	private JLabel dedupeSrcA;
	private JLabel dedupeSrcB;
	private JLabel dedupeSaver;
	
	private AbstractDataSource sourceA;
	private AbstractDataSource sourceB;
	private AbstractJoin join;
	private AbstractResultsSaver resultSavers;
	private ConfiguredSystem system;
	
	private ProcessPanel processPanel;
	//private boolean altered = false;

	public LinkageSystemPanel(MainFrame frame) {
		
		setLayout(null);
		
		//frame.addClosingSystemViewListener(new ConfigClosingListener());
		
		JLabel mode = new JLabel(Configs.linkModeIcon);
		mode.setBounds(527, 310, 225, 60);
		
		processPanel = new ProcessPanel(frame, this, new LinkageProcessStarter());
		
		sourceALabel = new JLabel("Data source");
		sourceBLabel = new JLabel("Date source");
		joinLabel = new JLabel("Linkage");
		saversLabel = new JLabel("Result savers");
		sourceALabel.setHorizontalAlignment(JLabel.CENTER);
		sourceBLabel.setHorizontalAlignment(JLabel.CENTER);
		joinLabel.setHorizontalAlignment(JLabel.CENTER);
		saversLabel.setHorizontalAlignment(JLabel.CENTER);
		
		sourceAName = new JLabel("Name: <empty>");
		sourceBName = new JLabel("Name: <empty>");
		sourceAName.setHorizontalAlignment(JLabel.CENTER);
		sourceBName.setHorizontalAlignment(JLabel.CENTER);
		
		statSourceALabel = new JLabel(STATUS_NOT_CONFIGURED);
		statSourceBLabel = new JLabel(STATUS_NOT_CONFIGURED);
		statJoinLabel = new JLabel(STATUS_NOT_CONFIGURED);
		statSaversLabel = new JLabel(STATUS_NOT_CONFIGURED);
		
		filterSrcA = new JLabel(Configs.bulbOff);
		filterSrcB = new JLabel(Configs.bulbOff);
		dedupeSrcA = new JLabel(Configs.bulbOff);
		dedupeSrcB = new JLabel(Configs.bulbOff);
		dedupeSaver = new JLabel(Configs.bulbOff);
		
		sourceAButton = Configs.getConfigurationButton();
		sourceBButton = Configs.getConfigurationButton();
		joinButton = Configs.getConfigurationButton();
		saversButton = Configs.getConfigurationButton();
		viewResultsButton = Configs.getViewResultsButton();
		viewMinusA = Configs.getViewMinusButton();
		viewMinusB = Configs.getViewMinusButton();
		sourceAButton.setToolTipText(TOOLTIP_CONFIG_DS);
		sourceBButton.setToolTipText(TOOLTIP_CONFIG_DS);
		joinButton.setToolTipText(TOOLTIP_CONFIG_LINK);
		saversButton.setToolTipText(TOOLTIP_CONFIG_SAVERS);
		viewResultsButton.setToolTipText(TOOLTIP_VIEW_RESULTS);
		viewMinusA.setToolTipText(TOOLTIP_VIEW_MINUS);
		viewMinusB.setToolTipText(TOOLTIP_VIEW_MINUS);
		sourceAButton.addActionListener(new SourceAButtonListener());
		sourceBButton.addActionListener(new SourceBButtonListener());
		joinButton.addActionListener(new JoinButtonListener());
		saversButton.addActionListener(new ResultsSaversButtonListener());
		viewResultsButton.addActionListener(new ViewResultsButtonListener());
		viewMinusA.addActionListener(new ViewMinusButtonListener(1));
		viewMinusB.addActionListener(new ViewMinusButtonListener(2));
		viewResultsButton.setEnabled(false);
		viewMinusA.setEnabled(false);
		viewMinusB.setEnabled(false);
		
		sourceAButton.setHorizontalAlignment(JLabel.CENTER);
		sourceBButton.setHorizontalAlignment(JLabel.CENTER);
		joinButton.setHorizontalAlignment(JLabel.CENTER);
		saversButton.setHorizontalAlignment(JLabel.CENTER);
		viewResultsButton.setHorizontalAlignment(JLabel.CENTER);
		sourceAButton.setBounds(90, 105, 30, 30);
		viewMinusA.setBounds(130, 105, 30, 30);
		sourceBButton.setBounds(90, 305, 30, 30);
		viewMinusB.setBounds(130, 305, 30, 30);
		joinButton.setBounds(410, 205, 30, 30);
		saversButton.setBounds(640, 205, 30, 30);
		viewResultsButton.setBounds(680, 205, 30, 30);
		
		sourceALabel.setBounds(50, 50, 150, 20);
		sourceBLabel.setBounds(50, 250, 150, 20);
		sourceAName.setBounds(50, 70, 150, 20);
		sourceBName.setBounds(50, 270, 150, 20);
		joinLabel.setBounds(350, 170, 150, 20);
		saversLabel.setBounds(600, 170, 150, 20);
		
		statSourceALabel.setLocation(50, 45);
		statSourceALabel.setSize(150, 100);
		statSourceALabel.setIcon(Configs.systemComponentNotConfigured);
		statSourceALabel.setHorizontalTextPosition(JLabel.CENTER);
		statSourceALabel.setVerticalTextPosition(JLabel.CENTER);
		statSourceALabel.setForeground(COLOR_DISABLED);
		
		statSourceBLabel.setLocation(50, 245);
		statSourceBLabel.setSize(150, 100);
		statSourceBLabel.setIcon(Configs.systemComponentNotConfigured);
		statSourceBLabel.setHorizontalTextPosition(JLabel.CENTER);
		statSourceBLabel.setVerticalTextPosition(JLabel.CENTER);
		statSourceBLabel.setForeground(COLOR_DISABLED);
		
		JLabel filterSrcALabel = new JLabel("Filter", JLabel.LEFT);
		filterSrcALabel.setBounds(80, 145, 100, 20);
		filterSrcA.setBounds(60, 145, 20, 20);
		JLabel dedupeSrcALabel = new JLabel("Deduplication", JLabel.LEFT);
		dedupeSrcALabel.setBounds(80, 165, 100, 20);
		dedupeSrcA.setBounds(60, 165, 20, 20);
		
		JLabel filterSrcBLabel = new JLabel("Filter", JLabel.LEFT);
		filterSrcBLabel.setBounds(80, 345, 100, 20);
		filterSrcB.setBounds(60, 345, 20, 20);
		JLabel dedupeSrcBLabel = new JLabel("Deduplication", JLabel.LEFT);
		dedupeSrcBLabel.setBounds(80, 365, 100, 20);
		dedupeSrcB.setBounds(60, 365, 20, 20);
		
		statJoinLabel.setLocation(350, 145);
		statJoinLabel.setSize(150, 100);
		statJoinLabel.setIcon(Configs.systemComponentNotConfigured);
		statJoinLabel.setHorizontalTextPosition(JLabel.CENTER);
		statJoinLabel.setVerticalTextPosition(JLabel.CENTER);
		statJoinLabel.setForeground(COLOR_DISABLED);
		
		statSaversLabel.setLocation(600, 145);
		statSaversLabel.setSize(150, 100);
		statSaversLabel.setIcon(Configs.systemComponentNotConfigured);
		statSaversLabel.setHorizontalTextPosition(JLabel.CENTER);
		statSaversLabel.setVerticalTextPosition(JLabel.CENTER);
		statSaversLabel.setForeground(COLOR_DISABLED);
		
		JLabel dedupeSaverLabel = new JLabel("Deduplication", JLabel.LEFT);
		dedupeSaverLabel.setBounds(630, 245, 140, 20);
		dedupeSaver.setBounds(610, 245, 20, 20);
		
		add(sourceALabel);
		add(sourceBLabel);
		add(sourceAName);
		add(sourceBName);
		add(joinLabel);
		add(saversLabel);
		add(sourceAButton);
		add(viewMinusA);
		add(sourceBButton);
		add(viewMinusB);
		add(joinButton);
		add(saversButton);
		add(viewResultsButton);
		
		add(filterSrcALabel);
		add(filterSrcA);
		add(dedupeSrcALabel);
		add(dedupeSrcA);
		
		add(filterSrcBLabel);
		add(filterSrcB);
		add(dedupeSrcBLabel);
		add(dedupeSrcB);
		
		add(dedupeSaverLabel);
		add(dedupeSaver);
		
		add(statSourceALabel);
		add(statSourceBLabel);
		add(statJoinLabel);
		add(statSaversLabel);
		
		add(mode);
		
		processPanel.setLocation(520, 10);
		add(processPanel);
		
		setPreferredSize(new Dimension(760, 380));
	}
	
	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		
		super.paint(g);
		drawArrow(g, 196, 95, 350, 180);
		drawArrow(g, 196, 295, 350, 210);
		drawArrow(g, 496, 195, 600, 195);
		
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
		system = new ConfiguredSystem(sourceA, sourceB, join, resultSavers);
	}

	/* (non-Javadoc)
	 * @see cdc.gui.SystemPanel#getSystem()
	 */
	public ConfiguredSystem getSystem() {
		//return new ConfiguredSystem(this.sourceA, this.sourceB, this.join, this.resultSavers);
		return system;
	}
	
	/* (non-Javadoc)
	 * @see cdc.gui.SystemPanel#setSystem(cdc.configuration.ConfiguredSystem)
	 */
	public void setSystem(ConfiguredSystem system) throws RJException {
		this.system = system;
		this.join = system.getJoin();
		if (this.join != null) {
			this.join.enableJoinStatistics();
			configured(joinButton, statJoinLabel);
		} else {
			disable(joinButton, statJoinLabel);
		}
		
		this.sourceA = system.getSourceA();
		if (this.sourceA != null) {
			configured(sourceAButton, statSourceALabel);
			this.sourceAName.setText("Name: " + sourceA.getSourceName());
		} else {
			disable(sourceAButton, statSourceALabel);
		}
		
		this.sourceB = system.getSourceB();
		if (this.sourceB != null) {
			configured(sourceBButton, statSourceBLabel);
			this.sourceBName.setText("Name: " + sourceB.getSourceName());
		} else {
			disable(sourceBButton, statSourceBLabel);
		}
		
		this.resultSavers = system.getResultSaver();
		if (this.resultSavers != null) {
			configured(saversButton, statSaversLabel);
		} else {
			disable(saversButton, statSaversLabel);
		}
		
		checkSystemStatus();
		
		//altered = false;
	}

//	/* (non-Javadoc)
//	 * @see cdc.gui.SystemPanel#systemSaved()
//	 */
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
			dedupeSrcA.setIcon(Configs.bulbOff);
			dedupeSrcB.setIcon(Configs.bulbOff);
			filterSrcA.setIcon(Configs.bulbOff);
			filterSrcB.setIcon(Configs.bulbOff);
			dedupeSaver.setIcon(Configs.bulbOff);
		} else {
		
			if (getSystem().getSourceA() != null) {
				if (getSystem().getSourceA().getDeduplicationConfig() != null) {
					dedupeSrcA.setIcon(Configs.bulbOn);
				} else {
					dedupeSrcA.setIcon(Configs.bulbOff);
				}
				if (getSystem().getSourceA().getFilter() != null && !getSystem().getSourceA().getFilter().isEmpty()) {
					filterSrcA.setIcon(Configs.bulbOn);
				} else {
					filterSrcA.setIcon(Configs.bulbOff);
				}
			}
			
			if (getSystem().getSourceB() != null) {
				if (getSystem().getSourceB().getDeduplicationConfig() != null) {
					dedupeSrcB.setIcon(Configs.bulbOn);
				} else {
					dedupeSrcB.setIcon(Configs.bulbOff);
				}
				if (getSystem().getSourceB().getFilter() != null && !getSystem().getSourceB().getFilter().isEmpty()) {
					filterSrcB.setIcon(Configs.bulbOn);
				} else {
					filterSrcB.setIcon(Configs.bulbOff);
				}
			}
		
		}
		
		if (getSystem() != null && getSystem().getResultSaver() != null) {
			if (getSystem().getResultSaver() instanceof DeduplicatingResultsSaver) {
				dedupeSaver.setIcon(Configs.bulbOn);
			} else {
				dedupeSaver.setIcon(Configs.bulbOff);
			}
		}
		
		if (join != null && sourceA != null && sourceB != null && resultSavers != null) {
			processPanel.setReady(true);
			return true;
		} else {
			//processPanel.setReady(false);
			//processPanel.setConfiguredSystem(null);
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
		error(sourceAButton, statSourceALabel);
	}
	
	public void reportErrorRightSource() {
		error(sourceBButton, statSourceBLabel);
	}
	
	public void reportErrorJoinSource() {
		error(joinButton, statJoinLabel);
	}
	
	public void reportErrorResultSavers() {
		error(saversButton, statSaversLabel);
	}
	
	private void error(JButton button, JLabel status) {
		//status.setBorder(BorderFactory.createLineBorder(Color.RED));
		status.setText(STATUS_ERROR);
		checkSystemStatus();
	}

	/* (non-Javadoc)
	 * @see cdc.gui.SystemPanel#unloadConfiguration()
	 */
	public void unloadConfiguration() {
		
		unload(sourceAButton, statSourceALabel);
		unload(sourceBButton, statSourceBLabel);
		unload(joinButton, statJoinLabel);
		unload(saversButton, statSaversLabel);
		sourceAName.setText("<empty>");
		sourceBName.setText("<empty>");
		
		viewResultsButton.setEnabled(false);
		viewMinusA.setEnabled(false);
		viewMinusB.setEnabled(false);
		
		filterSrcA.setIcon(Configs.bulbOff);
		filterSrcB.setIcon(Configs.bulbOff);
		dedupeSrcA.setIcon(Configs.bulbOff);
		dedupeSrcB.setIcon(Configs.bulbOff);
		dedupeSaver.setIcon(Configs.bulbOff);
	}

	private void unload(JButton button, JLabel label) {
		label.setText(STATUS_NOT_CONFIGURED);
		//button.setText(BUTTON_LABEL_CREATE);
		label.setForeground(Color.RED);
	}

	public void openRightDataSourceConfig(Window parent) {
		sourceBButton.doClick();
	}

	public void openLeftDataSourceConfig(Window parent) {
		sourceAButton.doClick();
	}
	
	/* (non-Javadoc)
	 * @see cdc.gui.SystemPanel#getProcessPanel()
	 */
	public ProcessPanel getProcessPanel() {
		return processPanel;
	}

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
		if (getSystem().getJoin().isSummaryForLeftSourceEnabled()) {
			this.viewMinusA.setEnabled(true);
			this.viewMinusA.setToolTipText(TOOLTIP_VIEW_MINUS);
		} else {
			this.viewMinusA.setEnabled(false);
			this.viewMinusA.setToolTipText(TOOLTIP_VIEW_MINUS + " (requires the option of summary of not joined data to be enabled)");
		}
		if (getSystem().getJoin().isSummaryForRightSourceEnabled()) {
			this.viewMinusB.setEnabled(true);
			this.viewMinusB.setToolTipText(TOOLTIP_VIEW_MINUS);
		} else {
			this.viewMinusB.setEnabled(false);
			this.viewMinusB.setToolTipText(TOOLTIP_VIEW_MINUS + " (requires the option of summary of not joined data to be enabled)");
		}
	}

	public void cleanup() {
		this.join = null;
		if (processPanel != null) {
			this.processPanel.cleanup();
		}
		this.processPanel = null;
		this.sourceA = null;
		this.sourceB = null;
		this.system = null;
	}
}
