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


package cdc.gui.slope;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;

import cdc.gui.components.dynamicanalysis.ChangedConfigurationListener;
import cdc.gui.components.paramspanel.ParamPanelField;

public class SlopePanel extends JPanel {
	
	private static int RIGHT_MARGIN = 40;
	private static int LEFT_MARGIN = 10;
	private static int MARGIN_DOWN = 5;
	private static int TICK_DOWN = 2;
	private static int TICK_UP = 5;
	private static int MARGIN_UP = 15;
	
	private static int SLOPE_1_MARGIN = 25;
	private static int SLOPE_0_MARGIN = 10;
	private static int SNAP_MARGIN = 5;
	private static int SNAP_0_1 = 10;
	
	private ParamPanelField v1Listener;
	private ParamPanelField v2Listener;
	
	private class MotionListener implements MouseMotionListener {
		
		public void mouseDragged(MouseEvent e) {
				if (mouseCatched) {
					catchDrag(e.getX(), e.getY());
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
	
	private class CatchListener implements MouseListener {
		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {
			if (isClickValid(e.getX(), e.getY())) {
				mouseCatched = true;
				lockY = e.getY();
			}
		}
		public void mouseReleased(MouseEvent e) {
			mouseCatched = false;
		}
		
	}
	
	private double v1;
	private double v2;
	private boolean slopeEnabled;
	private boolean mouseCatched = false;
	private int lockY = 0;
	
	public SlopePanel(double def1, double def2) {
		slopeEnabled = true;
		v1 = def1;
		v2 = def2;
		this.setLayout(null);
		this.addMouseMotionListener(new MotionListener());
		this.addMouseListener(new CatchListener());
	}
	
	public SlopePanel(int def) {
		slopeEnabled = false;
		v1 = def;
		v2 = def;
		this.setLayout(null);
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
		g.setColor(Color.DARK_GRAY);
		g.setFont(g.getFont().deriveFont((float)10));
		g.drawString("accept", width - RIGHT_MARGIN + 4, SLOPE_0_MARGIN + 18);
		g.drawString("reject", width - RIGHT_MARGIN + 4, height - SLOPE_0_MARGIN + 3);
	}

	private void drawGraph(Graphics g) {
		g.setColor(Color.RED);
		int width = (int) getPreferredSize().getWidth();
		int height = (int) getPreferredSize().getHeight();
		int realwidth = width - LEFT_MARGIN - RIGHT_MARGIN;
		
		int x1 = (int)(v1 * realwidth);
		int x2 = (int)(v2 * realwidth);
		
		g.drawLine(LEFT_MARGIN, SLOPE_1_MARGIN, x1 + LEFT_MARGIN, SLOPE_1_MARGIN);
		g.drawLine(width - RIGHT_MARGIN, height - SLOPE_0_MARGIN, x2 + LEFT_MARGIN, height - SLOPE_0_MARGIN);
		g.drawLine(x1 + LEFT_MARGIN, SLOPE_1_MARGIN, x2 + LEFT_MARGIN, height - SLOPE_0_MARGIN);
		
	}

	private void scaleLines(Graphics g) {
		g.setColor(Color.LIGHT_GRAY);
		int width = (int) getPreferredSize().getWidth();
		int height = (int) getPreferredSize().getHeight();
		int realwidth = width - LEFT_MARGIN - RIGHT_MARGIN;
		for (int i = 0; i < 11; i++) {
			g.drawLine(LEFT_MARGIN + i*(int)(realwidth/10), height - MARGIN_DOWN - TICK_DOWN, 
					LEFT_MARGIN + i*(int)(realwidth/10), MARGIN_UP + TICK_UP);
		}
	}

	private void scaleDown(Graphics g) {
		g.setColor(Color.DARK_GRAY);
		int width = (int) getPreferredSize().getWidth();
		int realwidth = width - LEFT_MARGIN - RIGHT_MARGIN;
		int height = (int) getPreferredSize().getHeight();
		g.drawLine(LEFT_MARGIN, height - MARGIN_DOWN, width - RIGHT_MARGIN, height - MARGIN_DOWN);
		for (int i = 0; i < 11; i++) {
			g.drawLine(LEFT_MARGIN + i*(int)(realwidth/10), height - MARGIN_DOWN, 
					LEFT_MARGIN + i*(int)(realwidth/10), height - MARGIN_DOWN - TICK_DOWN);
		}
	}

	private void scaleUp(Graphics g) {
		g.setColor(Color.DARK_GRAY);
		int width = (int) getPreferredSize().getWidth();
		int realwidth = width - RIGHT_MARGIN - LEFT_MARGIN;
		g.drawLine(LEFT_MARGIN, MARGIN_UP, width - RIGHT_MARGIN, MARGIN_UP);
		g.setFont(g.getFont().deriveFont((float)10));
		for (int i = 0; i < 11; i++) {
			g.drawString(String.valueOf(i/(double)10), LEFT_MARGIN + i*(int)(realwidth/10)-7, 10);
			g.drawLine(LEFT_MARGIN + i*(int)(realwidth/10), MARGIN_UP, LEFT_MARGIN + i*(int)(realwidth/10), MARGIN_UP + TICK_UP);
		}
	}
	
	private void catchDrag(int x, int y) {
		
		int height = (int) getPreferredSize().getHeight();
		
		if (x <= LEFT_MARGIN || x >= getPreferredSize().getWidth() - LEFT_MARGIN) {
			return;
		}
		
		if (!slopeEnabled || (lockY <= height - SLOPE_0_MARGIN + SNAP_MARGIN - SNAP_0_1 && lockY >= SLOPE_1_MARGIN - SNAP_MARGIN + SNAP_0_1)) {
			double newPosition = getPosition(x);
			double oldPosition = getOldPosition(y);
			//System.out.println("Here...");
			v1 += newPosition - oldPosition;
			v2 += newPosition - oldPosition;
			if (v1 < 0) v1 = 0;
			if (v1 > 1) v1 = 1;
			if (v2 > 1) v2 = 1;
			if (v2 < 0) v2 = 0;
		} else if (lockY <= height - SLOPE_0_MARGIN + SNAP_MARGIN - SNAP_0_1) {
			v1 = getPosition(x);
			if (v1 > v2) v1 = v2;
		} else if (lockY >= SLOPE_1_MARGIN - SNAP_MARGIN + SNAP_0_1) {
			v2 = getPosition(x);
			if (v2 < v1) v2 = v1;
		}
		
		v1 = ((int)((v1 * 100))) / (double)100;
		v2 = ((int)((v2 * 100))) / (double)100;
		
		if (v1Listener != null) {
			v1Listener.setValue(String.valueOf(v1));
		}
		if (v2Listener != null) {
			v2Listener.setValue(String.valueOf(v2));
		}
		repaint();
	}
	
	private double getOldPosition(int y) {
		int width = (int) getPreferredSize().getWidth();
		int height = (int) getPreferredSize().getHeight();
		int realwidth = width - LEFT_MARGIN - RIGHT_MARGIN;
		
		int x1 = (int)(v1 * realwidth);
		int x2 = (int)(v2 * realwidth);
		
		return (getX(x1 + LEFT_MARGIN, SLOPE_1_MARGIN, x2 + LEFT_MARGIN, height - SLOPE_0_MARGIN, y) - LEFT_MARGIN)/(double)realwidth;
	}

	private int getX(int x0, int y0, int x1, int y1, int y) {
		return (int)((y-y0)*(x1-x0)/(double)(y1-y0) + x0);
	}
	
	private double getPosition(int x) {
		int width = (int) getPreferredSize().getWidth();
		int realwidth = width - LEFT_MARGIN - RIGHT_MARGIN;
		return ((int)((x - LEFT_MARGIN)*100/realwidth))/(double)100;
	}
	
	private boolean isClickValid(int x, int y) {
		int width = (int) getPreferredSize().getWidth();
		int height = (int) getPreferredSize().getHeight();
		int realwidth = width - LEFT_MARGIN - RIGHT_MARGIN;
		
		int x1 = (int)(v1 * realwidth) + LEFT_MARGIN;
		int x2 = (int)(v2 * realwidth) + LEFT_MARGIN;
		
		if (!(x >= x1 - SNAP_MARGIN && x <= x2 + SNAP_MARGIN)) {
			return false;
		}
		
		if (!(y >= SLOPE_1_MARGIN - SNAP_MARGIN && y <= height - SLOPE_0_MARGIN + SNAP_MARGIN)) {
			return false;
		}
		
		return true;
	}
	
	public void setSlope(double v1, double v2) {
		this.v1 = v1;
		this.v2 = v2;
	}
	
	public void bindV1(ParamPanelField field) {
		this.v1Listener = field;
		this.v1Listener.addConfigurationChangeListener(new ChangedConfigurationListener() {
			public void configurationChanged() {
				try {
					double v = Double.parseDouble(v1Listener.getValue());
					if (v >=0 && v <= 1) v1 = v;
					SlopePanel.this.repaint();
				} catch (NumberFormatException e) {}
			}
		});
	}
	
	public void bindV2(ParamPanelField field) {
		this.v2Listener = field;
		this.v2Listener.addConfigurationChangeListener(new ChangedConfigurationListener() {
			public void configurationChanged() {
				try {
					double v = Double.parseDouble(v2Listener.getValue());
					if (v >=0 && v <= 1) v2 = v;
					SlopePanel.this.repaint();
				} catch (NumberFormatException e) {}
			}
		});
	}
}
