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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import cdc.components.AbstractDataSource;
import cdc.datamodel.converters.ModelGenerator;
import cdc.gui.LinkageSystemPanel;
import cdc.gui.MainFrame;
import cdc.gui.components.datasource.JDataSource;
import cdc.gui.components.datasource.ui.LegendPanel;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;

public class DSConfigureAttrsAction extends WizardAction {

	
//	public class ResizeListener implements ComponentListener {
//		
//		private int top, left, bottom, right;
//		private JPanel panel;
//		private JComponent refresh;
//		
//		public ResizeListener(JPanel panel, JComponent refresh, int top, int left, int bottom, int right) {
//			this.top = top;
//			this.left = left;
//			this.right = right;
//			this.bottom = bottom;
//			this.panel = panel;
//			this.refresh = refresh;
//		}
//
//		public void componentHidden(ComponentEvent arg0) {}
//		
//		public void componentMoved(ComponentEvent arg0) {}
//		
//		public void componentResized(ComponentEvent arg0) {
//			int x = arg0.getComponent().getSize().width;
//			int y = arg0.getComponent().getSize().height;
//			panel.setSize(x - left - right, y - top - bottom);
//			panel.setBounds(left, top, x - left - right, y - top - bottom);
//			panel.updateUI();
//			refresh.updateUI();
//		}
//		
//		public void componentShown(ComponentEvent arg0) {}
//	}

	private DSConfigureTypeAction source;
	private AbstractDataSource dataSource;
	private AbstractDataSource lastDataSource;
	private JDataSource buffer;
	private AbstractWizard parent;
	private int id;
	private boolean showOther;
	
	public DSConfigureAttrsAction(int id, DSConfigureTypeAction sourceAction) {
		this.source = sourceAction;
		this.id = id;
		this.showOther = true;
	}
	
	public DSConfigureAttrsAction(int id, DSConfigureTypeAction sourceAction, boolean showDedupeOption) {
		this.source = sourceAction;
		this.id = id;
		this.showOther = showDedupeOption;
	}

	public JPanel beginStep(AbstractWizard wizard) {
		dataSource = source.getDataSource();
		parent = wizard;
		
		buffer = new JDataSource(dataSource, lastDataSource == null || dataSource.getClass().equals( lastDataSource.getClass()));
		JScrollPane scroll = new JScrollPane(buffer);
		lastDataSource = dataSource;
		
		JPanel stering = new JPanel(new FlowLayout());
		
		JButton summary = new JButton("Show fields summary");
		summary.setPreferredSize(new Dimension(200, 20));
		summary.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					new SummaryWindow(parent, (JButton)arg0.getSource(), dataSource);
					((JButton)arg0.getSource()).setEnabled(false);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		JButton other = new JButton("Show other data source");
		if (showOther) {
			other.setPreferredSize(new Dimension(200, 20));
			other.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						if (id == 0) {
							((LinkageSystemPanel)MainFrame.main.getSystemPanel()).openRightDataSourceConfig(parent);
						} else if (id == 1) {
							((LinkageSystemPanel)MainFrame.main.getSystemPanel()).openLeftDataSourceConfig(parent);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
		
		JButton legend = new JButton("Show legend");
		legend.setPreferredSize(new Dimension(200, 20));
		legend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JDialog legend = new JDialog(parent, "Legend");
				LegendPanel panel = new LegendPanel();
				legend.getContentPane().add(panel);
				legend.pack();
				legend.setVisible(true);
			}
		});
		
		stering.add(summary);
		if (id != -1 && showOther) {
			stering.add(other);
		}
		stering.add(legend);
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(stering, BorderLayout.NORTH);
		panel.add(scroll, BorderLayout.CENTER);
		
//		transparentPanel = new JPanel() {
//			public void paint(Graphics arg0) {
//				Graphics2D g2d = (Graphics2D)arg0;
//				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
//				super.paint(arg0);
//			}
//		};
//		transparentPanel.setOpaque(false);
//		transparentPanel.setBounds(40, 20, 320, 82);
//		transparentPanel.setLayout(null);
		
		
		//JPanel testMainPanel = new JPanel();
		//testMainPanel.addComponentListener(new ResizeListener(panel, transparentPanel, 20, 5, 5, 5));
		//testMainPanel.setLayout(null);
		
		//testMainPanel.add(transparentPanel);
		//testMainPanel.add(panel);
		
		return panel;
	}

	public boolean endStep(AbstractWizard wizard) {
		
		if (buffer.getConverters().length == 0) {
			JOptionPane.showMessageDialog(wizard, "At least one column is required in output model.");
			return false;
		}
		
		dataSource.setModel(new ModelGenerator(buffer.getConverters()));
		
		return true;
	}
	
	public void setSize(int width, int height) {
		new Exception().printStackTrace();
	}

	public void dispose() {
		this.buffer = null;
		this.dataSource = null;
		this.parent = null;
		this.source = null;
	}

}
