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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cdc.configuration.ConfiguredSystem;

public class ProcessPanel extends JPanel {

	private static final String STATUS_IDLE = "idle";
	private static final String STATUS_BUSY = "busy";
	
	private JLabel working = new JLabel(Configs.busyIcon);
	private JButton start = new JButton(Configs.playIcon);
	private JLabel status = new JLabel(STATUS_IDLE);
	
	//private SummaryWindow activeSummaryWindow = null;	
	private ConfiguredSystem system = new ConfiguredSystem(null, null, null, null);
	private SystemPanel systemPanel;
	private ProcessStarterInterface processStarter;
	
	public ProcessPanel(MainFrame frame, SystemPanel panel, ProcessStarterInterface processStarter) {
		setLayout(null);
		setSize(240, 100);
		
		this.processStarter = processStarter;
		this.systemPanel = panel;
		
		start.setEnabled(false);
		start.setBorder(BorderFactory.createEmptyBorder());
		start.setBounds(30, 28, 42, 42);
		start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				status.setText(STATUS_BUSY);
				working.setVisible(true);
				start.setEnabled(false);
				ProcessPanel.this.processStarter.startProcessAndWait(system);
				working.setVisible(false);
				status.setText(STATUS_IDLE);
				start.setEnabled(true);
				systemPanel.setViewButtonEnabled(true);
				systemPanel.invalidate();
				systemPanel.repaint();
			}
		});
		
		status.setBounds(150, 40, 50, 25);
		JLabel label = new JLabel("Status: ");
		label.setBounds(105, 40, 50, 25);
		
		working.setVisible(false);
		working.setBounds(65, 30, 40, 40);
		
//		JCheckBox summary = new JCheckBox("Enable system summary");
//		summary.setOpaque(false);
//		summary.setBounds(30, 60, 190, 20);
//		summary.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent arg0) {
//				activeSummaryWindow = new SummaryWindow((JCheckBox)arg0.getSource());
//				activeSummaryWindow.setCurrentSystem(system);
//			}
//		});
		
		add(start);
		add(status);
		add(working);
		add(label);
//		add(summary);
		
		JLabel background = new JLabel(Configs.systemControlPanel);
		background.setLocation(0, 0);
		background.setSize(getSize());
		add(background);
	}

	public void setReady(boolean b) {
		start.setEnabled(b);
	}
	
	public void cleanup() {
		this.processStarter = null;
		this.system = null;
		this.systemPanel = null;
	}
	
	public void setConfiguredSystem(ConfiguredSystem system) {
		//System.out.println("System new");
		this.system = system;
//		if (activeSummaryWindow != null && activeSummaryWindow.isVisible()) {
//			activeSummaryWindow.setCurrentSystem(system);
//		}
	}
	
//	public void appendSummaryMessage(String msg) {
//		processStarter.appendSummaryMessage(msg);
//	}
	
}
