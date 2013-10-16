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


package cdc.utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.Timer;

public class ToolTipAttacher {
	
	private JComponent toolTipOwner;
	private JWindow toolTipWindow;
	private JPanel innerToolTipPanel;
	//private JComponent toolTip;
	private Timer timer;
	private boolean alreadyListening = false;
	
	public ToolTipAttacher(Window parent, JComponent toolTipOwner) {
		this.toolTipOwner = toolTipOwner;
		toolTipWindow = new JWindow(parent);
		toolTipWindow.setAlwaysOnTop(true);
		toolTipWindow.setFocusable(true);
		innerToolTipPanel = new JPanel(new BorderLayout());
		innerToolTipPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		innerToolTipPanel.setBackground(Color.white);
		toolTipWindow.getContentPane().add(innerToolTipPanel);
	}
	
	public void setToolTip(JComponent toolTip) {
		//this.toolTip = toolTip;
		innerToolTipPanel.add(toolTip, BorderLayout.CENTER);
		//toolTipWindow.setSize(100, 100);
		Dimension preferred = toolTip.getPreferredSize();
		toolTipWindow.setSize(preferred.width + 10, preferred.height + 10);
		registerListeners();
	}

	private void registerListeners() {
		if (alreadyListening) {
			return;
		}
		alreadyListening = true;
		
		toolTipOwner.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {
			}
			public void mouseEntered(MouseEvent e) {
				System.out.println("Entered...");
				if (!toolTipWindow.isVisible()) {
					JComponent source = (JComponent) e.getSource();
					toolTipWindow.setLocation(new Point(source.getLocationOnScreen().x + e.getX() + 2, 
							source.getLocationOnScreen().y + e.getY()));
					toolTipWindow.setVisible(true);
				}
				if (timer != null) {
					timer.stop();
					timer = null;
				}
			}

			public void mouseExited(MouseEvent e) {
				System.out.println("Exited in main");
				timer = new Timer(500, new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						System.out.println("Event ...");
						Point p = MouseInfo.getPointerInfo().getLocation();
						System.out.println("p is: " + p);
						System.out.println("bounds: " + toolTipWindow.getBounds());
						if (!mouseIn(p, toolTipWindow)) {
							toolTipWindow.setVisible(false);
							timer.stop();
						}
					}
				});
				timer.start();
			}

			public void mousePressed(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
			}
			
		});
		
		toolTipWindow.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
			
			public void mouseEntered(MouseEvent e) {
				System.out.println("Mouse listener here....");
				if (timer != null) {
					timer.stop();
					timer = null;
				}
			}

			public void mouseExited(MouseEvent e) {
				timer = new Timer(500, new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						System.out.println("Here...");
						Point p = MouseInfo.getPointerInfo().getLocation();
						if (!mouseIn(p, toolTipWindow)) {
							toolTipWindow.setVisible(false);
						}
						timer.stop();
					}});
				timer.start();
			}
		});
		
	}
	
	private boolean mouseIn(Point p, JWindow source) {
		if (!source.isVisible()) {
			return false;
		}
		Rectangle bounds = source.getBounds();
		int x = p.x;
		int y = p.y;
		return bounds.x < x && bounds.x + bounds.width > x && bounds.y < y && bounds.y + bounds.height > y;
	}
	
}
