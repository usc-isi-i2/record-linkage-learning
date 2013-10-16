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


package cdc.gui.wizards.specific.actions;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import cdc.components.AbstractDataSource;
import cdc.gui.Configs;
import cdc.utils.RJException;

public class SummaryWindow extends JDialog {

	private JPanel summaryPanel;
	private SummaryThread thread;
	private JButton source;
	private JLabel working = new JLabel(Configs.busyIcon);
	private JButton stop;
	
	public SummaryWindow(Window parent, JButton sourceB, AbstractDataSource dataSource) throws IOException, RJException {
		super(parent, "Data source summary");
		
		this.source = sourceB;
		//Rectangle location = parent.getBounds();
		//setLocation(location.x + location.width, location.y);
		
		JPanel main = new JPanel(new BorderLayout());
		main.setPreferredSize(new Dimension(400, 400));
		
		JPanel buttons = new JPanel(new FlowLayout());
		stop = new JButton("Stop");
		stop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				thread.scheduleStop();
				((JButton)e.getSource()).setEnabled(false);
			}
		});
		stop.setPreferredSize(new Dimension(stop.getPreferredSize().width, 20));
		JButton close = new JButton("Close");
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				thread.scheduleStop();
				try {
					thread.join();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				dispose();
				source.setEnabled(true);
			}
		});
		close.setPreferredSize(new Dimension(close.getPreferredSize().width, 20));
		
		working.setIcon(Configs.busyIcon);
		working.setPreferredSize(new Dimension(30, 30));
		working.setMinimumSize(new Dimension(30, 30));
		
		buttons.add(Box.createRigidArea(new Dimension(1, 30)));
		buttons.add(stop);
		buttons.add(close);
		buttons.add(working);
		
		JLabel label = new JLabel("Summary for data source", JLabel.CENTER);
		label.setFont(label.getFont().deriveFont(12.0F));
		main.add(label, BorderLayout.NORTH);
		main.add(new JScrollPane(summaryPanel = new JPanel()), BorderLayout.CENTER);
		main.add(buttons, BorderLayout.SOUTH);
		main.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Color.black, 2), 
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		
		summaryPanel.setBackground(Color.white);
		
//		parent.addComponentListener(new ComponentListener() {
//			public void componentHidden(ComponentEvent e) {}
//
//			public void componentMoved(ComponentEvent e) {
//				Rectangle location = SummaryWindow.this.parent.getBounds();
//				setLocation(location.x + location.width, location.y);
//			}
//
//			public void componentResized(ComponentEvent e) {}
//
//			public void componentShown(ComponentEvent e) {}
//			
//		});
		
		addWindowListener(new WindowListener() {
			public void windowActivated(WindowEvent arg0) {}
			public void windowClosed(WindowEvent arg0) {}
			public void windowClosing(WindowEvent arg0) {
				SummaryWindow.this.source.setEnabled(true);
			}
			public void windowDeactivated(WindowEvent arg0) {}
			public void windowDeiconified(WindowEvent arg0) {}
			public void windowIconified(WindowEvent arg0) {}
			public void windowOpened(WindowEvent arg0) {}
		});
		
		getContentPane().add(main);
		pack();
		setVisible(true);
		
		thread = new SummaryThread(this, dataSource.copy());
		thread.start();
	}
	
	public void finished() {
		working.setIcon(null);
		stop.setEnabled(false);
	}

	public void setStatisticPanel(JPanel mainPanel) {
		summaryPanel.add(mainPanel);
		validate();
		repaint();
	}
	
}
