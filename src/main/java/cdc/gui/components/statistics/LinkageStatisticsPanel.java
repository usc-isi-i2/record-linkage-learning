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


package cdc.gui.components.statistics;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class LinkageStatisticsPanel extends JPanel implements MouseMotionListener {
	
	private static final int X_VALUES_PADDING = 18;
	private static final int X_LABEL_PADDING = 7;
	private static final String CONFIDENCE = "Confidence";
	private static final int MAX_BAR_WIDTH = 40;
	private static int AXES_PADDING = 10;
	private static int BOTTOM_AXE_PADDING = 30;
	private static int BARS_FROM_AXE = 8;
	private static int BETWEEN_BARS = 2;
	
	private class PollingThread extends Thread {
		public void run() {
			try {
				while (LinkageStatisticsPanel.this.isVisible()) {
					analyzeData();
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							linked.setText(String.valueOf(joinData.getLinkedCount()));
							notLinked.setText(String.valueOf(joinData.getNotLinkedCount() + joinData.getLinkedCount()));
							percentage.setText(String.valueOf((int)(joinData.getLinkedCount() * 100 / (double)(joinData.getNotLinkedCount() + joinData.getLinkedCount()))) + "%");
							updateUI();
						}
					});
					sleep(50);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private class Bar {
		long height;
		int position;
	}
	
	private JoinStatisticalData joinData;
	
	private Bar[] bars;
	private long min;
	private long max;
	private int minNonZero;
	private int maxNonZero;
	
	private Rectangle[] rectangles;
	private long[] values;
	private int[] confidences;
	private JPanel info;
	private JPanel infoHolder;
	private int selectedBar = -1;
	
	private JPanel chartPanel = new JPanel() {
		
		public void paint(Graphics g) {
			super.paint(g);
			
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Dimension size = getSize();
			
			if (bars == null) {
				//g2d.drawString("No data", size.width / 2, size.height / 2);
			} else {
				drawBars(g2d, size.width, size.height);
			}
		}
		
	};
	private JLabel linked = new JLabel("-");
	private JLabel notLinked = new JLabel("-");
	private JLabel percentage = new JLabel("-");
	
	private JCheckBox logarithmicScale = new JCheckBox("Enable logarithmic scale (base 2)");
	
	public LinkageStatisticsPanel(JoinStatisticalData data, JPanel glassPane) {
		
		this.joinData = data;
		
		infoHolder = glassPane;
		info = new JPanel();
		info.setBackground(new Color(255, 255, 255, 220));
		info.setSize(200, 50);
		info.setVisible(false);
		infoHolder.add(info);
		infoHolder.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		chartPanel.addMouseMotionListener(this);
		chartPanel.setBorder(BorderFactory.createEtchedBorder());
		
		//chartPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		chartPanel.setMinimumSize(new Dimension(200, 200));
		JLabel links = new JLabel("Number of linked pairs: ");
		JLabel nonLinks = new JLabel("Number of compared pairs: ");
		//JLabel perc = new JLabel("Percentage of linked pairs: ");
		Box linkedPanel = Box.createHorizontalBox();
		linkedPanel.add(Box.createRigidArea(new Dimension(5, 5)));
		linkedPanel.add(links);
		linkedPanel.add(linked);
		linkedPanel.add(Box.createGlue());
		linkedPanel.add(logarithmicScale);
		JPanel notLinkedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		notLinkedPanel.add(nonLinks);
		notLinkedPanel.add(notLinked);
		JPanel percPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		//percPanel.add(perc);
		//percPanel.add(percentage);
		
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.insets = new Insets(10, 10, 10, 10);
		c.fill = GridBagConstraints.BOTH;
		add(chartPanel, c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(linkedPanel, c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(notLinkedPanel, c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 3;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(percPanel, c);
		
		new PollingThread().start();
	}

	private void analyzeData() {
		synchronized (this) {
			long[] histogram = joinData.getHistogram();
			minNonZero = -1;
			min = Long.MAX_VALUE;
			max = 0;
			for (int i = 0; i < histogram.length; i++) {
				if (histogram[i] != 0) {
					if (minNonZero == -1) {
						minNonZero = i;
					}
					maxNonZero = i;
				}
			}
			if (minNonZero == -1) {
				bars = null;
				return;
			}
			bars = new Bar[maxNonZero - minNonZero + 1];
			for (int i = minNonZero; i <= maxNonZero; i++) {
				if (histogram[i] < min) {
					min = histogram[i];
				}
				if (histogram[i] > max) {
					max = histogram[i];
				}
				bars[i - minNonZero] = new Bar();
				bars[i - minNonZero].position = i + 1;
				bars[i - minNonZero].height = histogram[i];
			}
		}
	}
	
	private void drawBars(Graphics2D g2d, int xSize, int ySize) {
		g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
		
		synchronized (this) {
			FontMetrics metrics = g2d.getFontMetrics();
			this.rectangles = new Rectangle[100];
			this.values = new long[100];
			this.confidences = new int[100];
			
			double maxValue = 0;
			for (int i = 0; i < bars.length; i++) {
				double value = encodeValue(bars[i].height);
				if (bars[i].height > maxValue) {
					maxValue = value;
				}
			}
			int maxLabelWidth = metrics.stringWidth(String.valueOf((int)maxValue));
			
			g2d.setFont(g2d.getFont().deriveFont(9));
	 		int widthForBars = xSize - 2 * (AXES_PADDING + BARS_FROM_AXE) - maxLabelWidth;
			int heightForBars = ySize - AXES_PADDING - BOTTOM_AXE_PADDING;
			int barWidth = (widthForBars - BETWEEN_BARS * (bars.length - 1)) / bars.length;
			if (barWidth > MAX_BAR_WIDTH) {
				barWidth = MAX_BAR_WIDTH;
			}
			
			//draw values on y axe
			int axesPadding = AXES_PADDING + maxLabelWidth + 2;
			int number = 8;
			if (number > maxValue) {
				number = (int)Math.ceil(maxValue);
				if (number == 0) {
					number = 2;
				}
			}
			for (int i = 1; i < number; i++) {
				long value = 0;
				if (number - 2 != 0) {
					value = (long)(maxValue/(double) (number - 2) * i);
				}
				int positionY = AXES_PADDING + heightForBars  - (number - 2 == 0 ? 0 : i * heightForBars / (number - 2));
				g2d.setColor(Color.BLACK);
				g2d.drawLine(axesPadding - 2, positionY, axesPadding + 2, positionY);
				g2d.drawString(String.valueOf(value), AXES_PADDING, positionY + 4);
				g2d.setColor(Color.LIGHT_GRAY);
				g2d.drawLine(axesPadding + 2, positionY, xSize - AXES_PADDING, positionY);
			}
			
			//draw axes (and box)
			g2d.setColor(Color.BLACK);
			g2d.drawLine(axesPadding, ySize - BOTTOM_AXE_PADDING, axesPadding, AXES_PADDING);
			g2d.drawLine(axesPadding, ySize - BOTTOM_AXE_PADDING, xSize - AXES_PADDING, ySize - BOTTOM_AXE_PADDING);
			g2d.setColor(Color.DARK_GRAY);
			g2d.drawLine(axesPadding, AXES_PADDING, xSize - AXES_PADDING, AXES_PADDING);
			g2d.drawLine(xSize - AXES_PADDING, ySize - BOTTOM_AXE_PADDING, xSize - AXES_PADDING, AXES_PADDING);
			g2d.setColor(Color.BLACK);
			
			int barPosition = axesPadding + BARS_FROM_AXE;
			int nextFreeForLabel = 0;
			for (int i = 0; i < bars.length; i++) {
				String val = String.valueOf(bars[i].position);
				if (nextFreeForLabel <= barPosition + barWidth / 2 - metrics.stringWidth(val) / 2 && 
						(i == bars.length - 1 || barPosition + barWidth / 2 + metrics.stringWidth(val) / 2 <= barWidth * (bars.length) + (bars.length - 1) * BETWEEN_BARS - metrics.stringWidth(String.valueOf(bars[bars.length - 1].position)) / 2)) {
					g2d.setColor(Color.black);
					g2d.drawString(val, barPosition + barWidth / 2 - metrics.stringWidth(val) / 2, ySize - X_VALUES_PADDING);
					nextFreeForLabel = barPosition + barWidth / 2 + metrics.stringWidth(val) / 2 + 40;
				}
				if (selectedBar == bars[i].position) {
					g2d.setColor(new Color(84, 244, 46));
				} else {
					g2d.setColor(new Color(84, 164, 46));
				}
				int barHeight = (int)Math.round(heightForBars * encodeValue(bars[i].height) / (float)maxValue);
				int barYStart = AXES_PADDING + heightForBars - barHeight;
				g2d.fillRect(barPosition, barYStart, barWidth, barHeight);
				rectangles[i] = new Rectangle(barPosition, barYStart, barWidth, barHeight);
				barPosition += BETWEEN_BARS + barWidth;
				
				//info for mouse over
				//System.out.println("Rect: " + rectangles[i]);
				values[i] = bars[i].height;
				confidences[i] = bars[i].position;
			}
			
			//draw Confidence string
			g2d.setColor(Color.black);
			g2d.drawString(CONFIDENCE, AXES_PADDING + widthForBars / 2 - metrics.stringWidth(CONFIDENCE) / 2, ySize - X_LABEL_PADDING);
			
		}
	}
	
	public void mouseDragged(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {
		//System.out.println("Position: " + e.getX() + " " + e.getY());
		info.setVisible(false);
		info.removeAll();
		selectedBar = -1;
		if (rectangles != null) {
			for (int i = 0; i < rectangles.length; i++) {
				if (rectangles[i] == null) {
					return;
				} else if (rectangles[i].contains(e.getX(), e.getY())) {
					//System.out.println("Matched: " + rectangles[i]);
					int width = getWidth();
					if (e.getX() > width / 2) {
						info.setLocation(e.getX() - info.getWidth(), e.getY());
					} else {
						info.setLocation(e.getX(), e.getY());
					}
					selectedBar = confidences[i];
					
					String s1 = "Confidence: " + confidences[i];
					String s2 = "Number of records: " + values[i];
					
					Box box = Box.createVerticalBox();
					box.add(new JLabel(s1));
					box.add(new JLabel(s2));
					info.add(box);
					info.setVisible(true);
				}
			}
		}
		infoHolder.updateUI();
	}
	
	private double encodeValue(long height) {
		if (!logarithmicScale.isSelected()) {
			return height;
		} else {
			if (height == 0) {
				return 0;
			}
			return Math.log(height);
		}
	}

	public static void main(String[] args) {
		JoinStatisticalData data = new JoinStatisticalData();
		HistogramDialog test = new HistogramDialog(null, "test", data);
		new DataGenerator(data).start();
		test.setVisible(true);
	}
	
	private static class DataGenerator extends Thread {
		private JoinStatisticalData data;
		private Random rand = new Random();
		public DataGenerator(JoinStatisticalData data) {
			this.data = data;
		}
		public void run() {
			try {
				for (int i = 0; i < 100; i++) {
					int confidence = Math.abs(rand.nextInt()) % 100 + 1;
					data.inject(confidence);
					synchronized (this) {
						wait(1);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
}
