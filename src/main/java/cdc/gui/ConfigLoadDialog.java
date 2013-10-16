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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

public class ConfigLoadDialog extends JDialog {

	private static final String OF = " of ";
	private static final String PROGRESS = " tasks are completed";
	private static final String TODO = "PENDING";
	private static final String DONE = "DONE";
	
	private JProgressBar progressBar;
	
	private JLabel[] phases;
	private JPanel[] phasesStatus;
	
	private JLabel mainLabel;
	private int done = 0;
	
	private JButton cancelButton;
	private AtomicBoolean bool = new AtomicBoolean(true);
	
	public ConfigLoadDialog(String[] phases) {
		super(MainFrame.main, "Loading configuration...");
		
		cancelButton = new JButton("Cancel");
		cancelButton.setPreferredSize(new Dimension(120, 20));
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				((JButton)e.getSource()).setEnabled(false);
			}
		});
		
		progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		progressBar.setPreferredSize(new Dimension(470, 15));
		
		JPanel progressBarPanel = new JPanel();
		progressBarPanel.setPreferredSize(new Dimension(500, 60));
		mainLabel = new JLabel();
		mainLabel.setHorizontalAlignment(JLabel.LEFT);
		mainLabel.setPreferredSize(new Dimension(365, 20));
		JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		labelPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
		//labelPanel.setPreferredSize(new Dimension(480, 25));
		labelPanel.add(mainLabel);
		labelPanel.add(cancelButton);
		progressBarPanel.add(labelPanel, BorderLayout.NORTH);
		progressBarPanel.add(progressBar, BorderLayout.SOUTH);
		
		JPanel log = new JPanel();
		log.setLayout(new BoxLayout(log, BoxLayout.PAGE_AXIS));
		//log.setPreferredSize(new Dimension(450, 200));
		//log.setBorder(BorderFactory.createEtchedBorder());
		log.setBackground(Color.WHITE);
		
		this.phases = new JLabel[phases.length];
		this.phasesStatus = new JPanel[phases.length];
		for (int i = 0; i < phases.length; i++) {
			this.phases[i] = new JLabel(phases[i]);
			this.phases[i].setBackground(Color.WHITE);
			this.phases[i].setOpaque(true);
			this.phases[i].setPreferredSize(new Dimension(180, 15));
			//this.phases[i].setMaximumSize(new Dimension(180, 15));
			this.phasesStatus[i] = new JPanel();
			this.phasesStatus[i].setBackground(Color.WHITE);
			JLabel todo = new JLabel(TODO);
			todo.setBackground(Color.WHITE);
			todo.setPreferredSize(new Dimension(100, 13));
			//todo.setMaximumSize(new Dimension(180, 13));
			todo.setOpaque(true);
			this.phasesStatus[i].add(todo);
			JPanel singlePhase = new JPanel(new FlowLayout(FlowLayout.LEFT));
			singlePhase.setPreferredSize(new Dimension(470, 30));
			singlePhase.setMaximumSize(new Dimension(470, 30));
			singlePhase.setBackground(Color.WHITE);
			singlePhase.add(this.phases[i]);
			singlePhase.add(this.phasesStatus[i]);
			log.add(singlePhase);
		}
		
		JScrollPane scroll = new JScrollPane(log);
		scroll.setPreferredSize(new Dimension(480, 200));
		scroll.setMaximumSize(new Dimension(480, 200));
		
		JPanel tasksPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		tasksPanel.setMaximumSize(new Dimension(480, 20));
		JLabel tasks = new JLabel("Detailed list of tasks:");
		tasks.setHorizontalAlignment(JLabel.LEFT);
		tasksPanel.add(tasks);
		
		JPanel divider = new JPanel() {
			public void paint(Graphics g) {
				g.setColor(Color.GRAY);
				g.drawLine(0, 5, 480, 5);
				g.setColor(Color.LIGHT_GRAY);
				g.drawLine(0, 6, 480, 6);
			}
		};
		divider.setMaximumSize(new Dimension(480, 10));
		divider.setPreferredSize(new Dimension(480, 10));
		
		JPanel bottomFiller = new JPanel();
		bottomFiller.setPreferredSize(new Dimension(480, 10));
		
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
		getContentPane().add(progressBarPanel, BorderLayout.CENTER);
		getContentPane().add(divider);
		getContentPane().add(tasksPanel);
		getContentPane().add(scroll, BorderLayout.SOUTH);
		getContentPane().add(bottomFiller);
		
		setMainLabelText();
		
		setLocationRelativeTo(MainFrame.main);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		pack();
		setModal(true);
		
	}
	
	public void addCancelListener(ActionListener listener) {
		cancelButton.addActionListener(listener);
	}

	public void started() {
		setVisible(bool.get());
		
	}
	
	public void finished() {
		bool.set(false);
		setVisible(bool.get());
	}

	public void startPhase(int index) {
		//System.out.println("Start phase: " + index);
		JPanel phase = phasesStatus[index];
		phase.removeAll();
		JProgressBar bar = new JProgressBar();
		bar.setIndeterminate(true);
		bar.setBackground(Color.WHITE);
		bar.setPreferredSize(new Dimension(100, 13));
		phase.add(bar);
		validate();
		repaint();
	}
	
	public void endPhase(int index) {
		done++;
		setMainLabelText();
		JPanel phase = phasesStatus[index];
		phase.removeAll();
		JLabel label = new JLabel(DONE);
		label.setPreferredSize(new Dimension(100, 13));
		phase.add(label);
		label.setBackground(Color.WHITE);
		label.setOpaque(true);
		validate();
		repaint();
	}

	private void setMainLabelText() {
		mainLabel.setText(done + OF + phases.length + PROGRESS);
	}
}
