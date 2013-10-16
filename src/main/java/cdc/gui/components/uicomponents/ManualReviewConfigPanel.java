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


package cdc.gui.components.uicomponents;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ManualReviewConfigPanel extends JPanel {
	
	private JCheckBox enabled = new JCheckBox("Enable manual review process");
	private JTextField acceptanceLevel = new JTextField(3);
	private JTextField manualRevLevel = new JTextField(3);
	private JPanel infoPanel = new JPanel(new BorderLayout());
	
	private int acceptance;
	private int manual;
	private Plot plot;
	

	public ManualReviewConfigPanel(int acceptance) {
		this(acceptance, -1);
	}
	
	public ManualReviewConfigPanel(int acceptance, int manual) {
		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		enabled.setSelected(manual != -1);
		enabled.addActionListener(new ActionListener() {			
			public void actionPerformed(ActionEvent e) {
				repaintAll();
			}
		});
		
		acceptanceLevel.setText(String.valueOf(this.acceptance = acceptance));
		if (manual == -1) {
			manualRevLevel.setText(String.valueOf(this.manual = (100 - acceptance) / 2 + acceptance));
		} else {
			manualRevLevel.setText(String.valueOf(this.manual = manual));
		}
		acceptanceLevel.setEnabled(false);
		acceptanceLevel.setHorizontalAlignment(JTextField.CENTER);
		manualRevLevel.setEnabled(false);
		manualRevLevel.setHorizontalAlignment(JTextField.CENTER);
		
		add(enabled, new GridBagConstraints(0, 0, 2, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
		plot = new Plot();
		add(plot, new GridBagConstraints(0, 1, 2, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(20,10,10,0), 0, 0));
		
		add(new JLabel("Linkage acceptance level: ", JLabel.LEFT), new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(20,5,0,0), 0, 0));
		add(acceptanceLevel, new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
		
		add(new JLabel("Manual review level: ", JLabel.LEFT), new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,0), 0, 0));
		add(manualRevLevel, new GridBagConstraints(1, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
		
		infoPanel.setPreferredSize(new Dimension(500, 60));
		add(infoPanel, new GridBagConstraints(0, 4, 2, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(20,0,0,0), 0, 0));
		infoPanel.add(new InformationPanel(getMessage()), BorderLayout.WEST);
	}
	
	private String getMessage() {
		if (!enabled.isSelected()) {
			return "Manual review process is disabled.";
		} else {
			return "Linkages with score between " + acceptance + " and " + manual + " will be manually revieved.\n" + 
				"Linkages with score above " + manual + " will be accepted automatically.\n" +
				"Drag the red line on the plot above to change these settings.";
		}
	}
	
	private void repaintAll() {
		infoPanel.removeAll();
		infoPanel.add(new InformationPanel(getMessage()), BorderLayout.WEST);
		infoPanel.validate();
		infoPanel.repaint();
		plot.repaint();
		this.validate();
		this.repaint();
	}
	
	private class Plot extends JPanel {
		
		private Dimension SIZE = new Dimension(400, 100);
		private int RIGHT_MARGIN = 40;
		private int LEFT_MARGIN = 10;
		private int MARGIN_DOWN = 5;
		private int TICK_DOWN = 2;
		private int TICK_UP = 5;
		private int MARGIN_UP = 15;
		
		private int SLOPE_1_MARGIN = 25;
		private int SLOPE_0_MARGIN = 10;
		
		private int GRID_X = 10;
		private int THICKNESS = 3;
		
		private boolean mouseCatched = false;
		
		public Plot() {
			setPreferredSize(SIZE);
			addMouseListener(new CatchListener());
			addMouseMotionListener(new MotionListener());
		}
		
		public void paint(Graphics g) {
			//draw scales
			super.paint(g);
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			scaleUp(g);
			scaleDown(g);
			scaleLines(g);
			scale01(g);
			drawGraph(g);
			g.dispose();
		}

		private void scale01(Graphics g) {
			int width = (int) getPreferredSize().getWidth();
			int height = (int) getPreferredSize().getHeight();
			g.setColor(Color.LIGHT_GRAY);
			g.drawLine(LEFT_MARGIN, SLOPE_1_MARGIN, width - RIGHT_MARGIN, SLOPE_1_MARGIN);
			g.drawLine(LEFT_MARGIN, height - SLOPE_0_MARGIN, width - RIGHT_MARGIN, height - SLOPE_0_MARGIN);
			g.drawLine(LEFT_MARGIN, ((height - SLOPE_0_MARGIN - SLOPE_1_MARGIN)/2) + SLOPE_1_MARGIN, width - RIGHT_MARGIN, ((height - SLOPE_0_MARGIN - SLOPE_1_MARGIN)/2) + SLOPE_1_MARGIN);
			g.setColor(Color.DARK_GRAY);
			g.setFont(g.getFont().deriveFont((float)10));
			g.drawString("accept", width - RIGHT_MARGIN + 4, SLOPE_0_MARGIN + 18);
			g.drawString("reject", width - RIGHT_MARGIN + 4, height - SLOPE_0_MARGIN + 3);
			g.drawString("manual", width - RIGHT_MARGIN + 4, ((height - SLOPE_0_MARGIN - SLOPE_1_MARGIN)/2) + SLOPE_1_MARGIN);
			g.drawString("review", width - RIGHT_MARGIN + 4, ((height - SLOPE_0_MARGIN - SLOPE_1_MARGIN)/2) + SLOPE_1_MARGIN + 10);
		}

		private void drawGraph(Graphics g) {
			g.setColor(enabled.isSelected() ? Color.RED : Color.DARK_GRAY);
			int width = (int) getPreferredSize().getWidth();
			int height = (int) getPreferredSize().getHeight();
			int realwidth = width - LEFT_MARGIN - RIGHT_MARGIN;
			
			int x1 = (int)(acceptance / (double)100.0 * realwidth);
			int x2 = (int)(manual / 100.0 * realwidth);
			
			g.fillRect(LEFT_MARGIN, height - SLOPE_0_MARGIN - THICKNESS / 2, x1, THICKNESS);
			g.fillRect(x1 + LEFT_MARGIN, ((height - SLOPE_0_MARGIN - SLOPE_1_MARGIN)/2) + SLOPE_1_MARGIN - THICKNESS/2, x2 - x1, THICKNESS);
			g.fillRect(x2 + LEFT_MARGIN, SLOPE_1_MARGIN - THICKNESS / 2, realwidth - x2, THICKNESS);
			g.fillRect(x2 + LEFT_MARGIN - THICKNESS / 2, SLOPE_0_MARGIN + MARGIN_UP - THICKNESS / 2, THICKNESS, (height - SLOPE_0_MARGIN - SLOPE_1_MARGIN) / 2 + THICKNESS);
			g.fillRect(x1 + LEFT_MARGIN - THICKNESS / 2, height - (height - SLOPE_0_MARGIN - SLOPE_1_MARGIN) / 2 - THICKNESS / 2 - SLOPE_0_MARGIN - 1, THICKNESS, (height - SLOPE_0_MARGIN - SLOPE_1_MARGIN) / 2 + THICKNESS + 1);
		}

		private void scaleLines(Graphics g) {
			g.setColor(Color.LIGHT_GRAY);
			int width = (int) getPreferredSize().getWidth();
			int height = (int) getPreferredSize().getHeight();
			int realwidth = width - LEFT_MARGIN - RIGHT_MARGIN;
			for (int i = 0; i <= GRID_X; i++) {
				g.drawLine(LEFT_MARGIN + i*(int)(realwidth/GRID_X), height - MARGIN_DOWN - TICK_DOWN, 
						LEFT_MARGIN + i*(int)(realwidth/GRID_X), MARGIN_UP + TICK_UP);
			}
		}

		private void scaleDown(Graphics g) {
			g.setColor(Color.DARK_GRAY);
			int width = (int) getPreferredSize().getWidth();
			int realwidth = width - LEFT_MARGIN - RIGHT_MARGIN;
			int height = (int) getPreferredSize().getHeight();
			g.drawLine(LEFT_MARGIN, height - MARGIN_DOWN, width - RIGHT_MARGIN, height - MARGIN_DOWN);
			for (int i = 0; i <= GRID_X; i++) {
				g.drawLine(LEFT_MARGIN + i*(int)(realwidth/GRID_X), height - MARGIN_DOWN, 
						LEFT_MARGIN + i*(int)(realwidth/GRID_X), height - MARGIN_DOWN - TICK_DOWN);
			}
		}

		private void scaleUp(Graphics g) {
			g.setColor(Color.DARK_GRAY);
			int width = (int) getPreferredSize().getWidth();
			int realwidth = width - RIGHT_MARGIN - LEFT_MARGIN;
			g.drawLine(LEFT_MARGIN, MARGIN_UP, width - RIGHT_MARGIN, MARGIN_UP);
			g.setFont(g.getFont().deriveFont((float)10));
			for (int i = 0; i <= GRID_X; i++) {
				g.drawString(String.valueOf((int)(i/(double)GRID_X * 100)), LEFT_MARGIN + i*(int)(realwidth/GRID_X)-7, 10);
				g.drawLine(LEFT_MARGIN + i*(int)(realwidth/GRID_X), MARGIN_UP, LEFT_MARGIN + i*(int)(realwidth/GRID_X), MARGIN_UP + TICK_UP);
			}
		}
		
		private boolean isClickValid(int x, int y) {
			if (!enabled.isSelected()) {
				return false;
			}
			int width = (int) getPreferredSize().getWidth();
			int height = (int) getPreferredSize().getHeight();
			if (x < LEFT_MARGIN || x > width - RIGHT_MARGIN || y < SLOPE_1_MARGIN || y > height - SLOPE_0_MARGIN) {
				return false;
			} else {
				return true;
			}
		}
		
		private void updateLines(int x, int y) {
			int height = (int) getPreferredSize().getHeight();
			if (y < ((height - SLOPE_0_MARGIN - SLOPE_1_MARGIN)/2) + SLOPE_1_MARGIN) {
				int newValue = getValue(x);
				manual = newValue;
				manualRevLevel.setText(String.valueOf(newValue));
				if (newValue < acceptance) {
					acceptance = newValue;
					acceptanceLevel.setText(String.valueOf(acceptance));
				}
			} else {
				int newValue = getValue(x);
				acceptance = newValue;
				acceptanceLevel.setText(String.valueOf(acceptance));
				if (manual < acceptance) {
					manual = newValue;
					manualRevLevel.setText(String.valueOf(newValue));
				}
			}
			repaintAll();
		}
		
		private int getValue(int x) {
			int width = (int) getPreferredSize().getWidth();
			int realwidth = width - LEFT_MARGIN - RIGHT_MARGIN;
			return (int) (Math.round(100 * ((x - LEFT_MARGIN) / (double)realwidth)));
		}

		private class MotionListener implements MouseMotionListener {
			
			public void mouseDragged(MouseEvent e) {
					if (mouseCatched && isClickValid(e.getX(), e.getY())) {
						updateLines(e.getX(), e.getY());
					}
			}
			public void mouseMoved(MouseEvent e) {
				if (isClickValid(e.getX(), e.getY())) {
					setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
				} else {
					setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
			}
		}
		
		private class CatchListener extends MouseAdapter {
			public void mousePressed(MouseEvent e) {
				if (isClickValid(e.getX(), e.getY())) {
					mouseCatched = true;
				}
			}
			public void mouseReleased(MouseEvent e) {
				mouseCatched = false;
			}
			public void mouseClicked(MouseEvent e) {
				if (isClickValid(e.getX(), e.getY())) {
					updateLines(e.getX(), e.getY());
				}
			}
		}
		
	}

	public int getManulaReview() {
		return enabled.isSelected() ? manual : -1;
	}

	public int getAcceptance() {
		return acceptance;
	}
	

	
}
