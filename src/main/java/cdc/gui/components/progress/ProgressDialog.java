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


package cdc.gui.components.progress;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import cdc.gui.Configs;
import cdc.gui.MainFrame;
import cdc.gui.components.statistics.HistogramDialog;

public class ProgressDialog extends JDialog {
	
	private JButton cancel = new JButton("Cancel");
	private JButton details = new JButton(Configs.openDetailsIcon);
	private JProgressBar progressBar;
	
	private JPanel infoPanel;
	private JPanel detailsPanel;
	
	private JPanel mainPanel;
	private JPanel firstLevelPanel;
	
	private JDialog statistics;
	
	private Dimension sizeDefaultDetailsOn = new Dimension(600, 400);
	private Dimension sizeDefaultDetailsOff = new Dimension(600, 200);
	
	private boolean detailsOn = false;
	private boolean detailsActiolListenerSet = false;
	private boolean showStatsButton = true;
	
	private int currY = 0;
	
	public ProgressDialog(Window parent, String title, boolean resizable) {
		this(parent, title, resizable, true);
	}
	
	public ProgressDialog(Window parent, String title, boolean resizable, boolean showStatsButton) {
		super(parent);
		setModal(true);
		setTitle(title);
		
		this.showStatsButton = showStatsButton;
		
		mainPanel = new JPanel();
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		mainPanel.setLayout(new GridBagLayout());
		getContentPane().add(mainPanel);
		
		createUI();
		updateUI();
	}

	public ProgressDialog(Window parent, String title) {
		this(parent, title, true);
	}
	
	
	private void createUI() {
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		cancel.setPreferredSize(new Dimension(100, 20));
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				cancel.setEnabled(false);
			}
		});
		details.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				detailsOn = !detailsOn;
				if (detailsOn) {
					details.setIcon(Configs.closeDetailsIcon);
				} else {
					details.setIcon(Configs.openDetailsIcon);
				}
				updateUI();
			}
		});
		details.setPreferredSize(new Dimension(20, 20));
		details.setBorder(BorderFactory.createEmptyBorder());
		
		firstLevelPanel = new JPanel(new BorderLayout());
		firstLevelPanel.add(new JLabel("Please wait..."), BorderLayout.WEST);
		firstLevelPanel.add(cancel, BorderLayout.EAST);
		
		progressBar = new JProgressBar();
		progressBar.setPreferredSize(new Dimension(100, 15));
		progressBar.setIndeterminate(true);
		
	}
	
	
	
	private GridBagConstraints getDefaultConstraints() {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = currY ++;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.BOTH;
		return c;
	}

	public void updateUI() {
		mainPanel.removeAll();
		mainPanel.add(firstLevelPanel, getDefaultConstraints());
		mainPanel.add(Box.createRigidArea(new Dimension(5,5)), getDefaultConstraints());
		mainPanel.add(progressBar, getDefaultConstraints());
		
		if (infoPanel != null) {
			mainPanel.add(Box.createRigidArea(new Dimension(5,5)), getDefaultConstraints());
			mainPanel.add(infoPanel, getDefaultConstraints());
		}
		
		Box buttons = Box.createHorizontalBox();
		if (detailsPanel != null || detailsActiolListenerSet) {
			mainPanel.add(Box.createRigidArea(new Dimension(5,5)), getDefaultConstraints());
			//JPanel detailsButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			buttons.add(new JLabel("Details"));
			buttons.add(Box.createRigidArea(new Dimension(5,5)));
			buttons.add(details);
			//mainPanel.add(buttons, getDefaultConstraints());
		}
		buttons.add(Box.createGlue());
		JButton stats = new JButton(Configs.statistics);
		stats.setPreferredSize(new Dimension(30, 30));
		stats.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (statistics != null) {
					statistics.toFront();
				} else {
					statistics = new HistogramDialog(ProgressDialog.this, "Linkage statistics", MainFrame.main.getConfiguredSystem().getJoin().getJoinStatisticsListener());
					statistics.addWindowListener(new WindowAdapter() {
						public void windowClosing(WindowEvent e) {
							statistics = null;
						}
					});
					statistics.setVisible(true);
				}
			}
		});
		
//		JButton linkages = new JButton(Configs.analysisButtonIcon);
//		linkages.setPreferredSize(new Dimension(30, 30));
//		linkages.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				MainFrame.main.openLinkagesDialog();
//			}
//		});
		
		//stats.setPreferredSize(new Dimension(160, 20));
		buttons.add(Box.createRigidArea(new Dimension(30, 30)));
		//buttons.add(linkages);
		//buttons.add(Box.createRigidArea(new Dimension(10, 10)));
		if (showStatsButton) {
			buttons.add(stats);
		}
		mainPanel.add(buttons, getDefaultConstraints());
		
		if (detailsOn) {
			mainPanel.add(Box.createRigidArea(new Dimension(5,5)), getDefaultConstraints());
			GridBagConstraints c = getDefaultConstraints();
			c.weighty = 1.0;
			if (detailsPanel != null) {
				mainPanel.add(detailsPanel, c);
			} else {
				mainPanel.add(new JPanel(), c);
			}
			setSize(new Dimension(sizeDefaultDetailsOn));
		} else {
			mainPanel.add(Box.createRigidArea(new Dimension(5,5)), getDefaultConstraints());
			JPanel filler = new JPanel();
			GridBagConstraints c = getDefaultConstraints();
			c.weighty = 1.0;
			mainPanel.add(filler, c);
			setSize(new Dimension(sizeDefaultDetailsOff));
		}
		mainPanel.updateUI();
		

	}
	
	public boolean isDetailsOn() {
		return detailsOn;
	}
	
	public void setInfoPanel(JPanel panel) {
		this.infoPanel = panel;
		updateUI();
	}
	
	public void setDetailsPanel(JPanel panel) {
		this.detailsPanel = panel;
		updateUI();
	}
	
	public JProgressBar getProgressBar() {
		return progressBar;
	}
	
	public void addCancelListener(ActionListener listener) {
		cancel.addActionListener(listener);
	}
	
	public void addDetailsActionListener(ActionListener listener) {
		detailsActiolListenerSet = true;
		details.addActionListener(listener);
		updateUI();
	}
	
}
