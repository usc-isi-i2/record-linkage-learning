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


package cdc.gui.components.summary;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.components.AbstractResultsSaver;
import cdc.configuration.ConfiguredSystem;
import cdc.gui.MainFrame;
import cdc.utils.HTMLUtils;
import cdc.utils.Props;

public class SummaryWindow extends JDialog {
	
	public static final int DEFAULT_TAB = Props.getInteger("summary-window-tab");
	
	private JCheckBox clickSource;

	private JScrollPane scroll;
	
	public SummaryWindow(JCheckBox clickSource) {
		super(MainFrame.main, "Configuration summary", false);
		setSize(500, 400);
		setLayout(new BorderLayout());
		setLocation(computeInitialLocation(MainFrame.main));
		setVisible(true);
		this.clickSource = clickSource;
		this.clickSource.setEnabled(false);
		this.addWindowListener(new WindowListener() {
			public void windowActivated(WindowEvent arg0) {}
			public void windowClosing(WindowEvent arg0) {
				SummaryWindow.this.clickSource.setEnabled(true);
				SummaryWindow.this.clickSource.setSelected(false);
			}
			public void windowDeactivated(WindowEvent arg0) {}
			public void windowDeiconified(WindowEvent arg0) {}
			public void windowIconified(WindowEvent arg0) {}
			public void windowOpened(WindowEvent arg0) {}
			public void windowClosed(WindowEvent arg0) {}
		});
	}

	private Point computeInitialLocation(MainFrame main) {
		//Point parentUpperLeft = main.getLocation();
		//Dimension parentSize = main.getSize();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension size = getSize();
		return new Point(screenSize.width - size.width - 40, screenSize.height - size.height - 40);
	}
	
	public void setCurrentSystem(ConfiguredSystem system) {
		
		if (system == null) {
			return;
		}
		
		int scrollPosition = -1;
		if (scroll != null) {
			scrollPosition = scroll.getVerticalScrollBar().getValue();
		}
		
		JPanel summaryPanel = new JPanel();
		summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		summaryPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LINE_START;
		c.weightx = 1;
		
		Component sources = createLabel("Summary for data sources");
		JLabel sourceA = new JLabel("Source A");
		sourceA.setFont(sourceA.getFont().deriveFont(11F));
		JLabel sourceB = new JLabel("Source B");
		sourceB.setFont(sourceB.getFont().deriveFont(11F));
		
		c.gridy = 0;
		summaryPanel.add(sources, c);
		c.gridy = 1;
		summaryPanel.add(createSourceSummary(sourceA, system.getSourceA()), c);
		c.gridy = 2;
		summaryPanel.add(createSourceSummary(sourceB, system.getSourceB()), c);
		
		c.gridy = 3;
		summaryPanel.add(createDivider(), c);
		
		Component join = createLabel("Summary for join process");
		join.setFont(join.getFont().deriveFont(11F));
		c.gridy = 4;
		summaryPanel.add(join, c);
		c.gridy = 5;
		summaryPanel.add(createJoinSummary(system.getJoin()), c);
		
		c.gridy = 6;
		summaryPanel.add(createDivider(), c);
		
		Component resultSavers = createLabel("Summary for result savers");
		c.gridy = 7;
		summaryPanel.add(resultSavers, c);
		c.gridy = 8;
		summaryPanel.add(createResultSaversSummary(system.getResultSaver()), c);
		
		JPanel layoutPanel = new JPanel(new BorderLayout());
		layoutPanel.add(summaryPanel, BorderLayout.NORTH);
		scroll = new JScrollPane(layoutPanel);
		getContentPane().removeAll();
		getContentPane().add(scroll, BorderLayout.CENTER);
		
		validate();
		repaint();
		
		if (scrollPosition != -1) {
			System.out.println("Scroll position: " + scrollPosition);
			scroll.getVerticalScrollBar().setValue(scrollPosition);
		}
		
		//System.out.println("Done.");
	}

	private Component createLabel(String string) {
		JPanel panel = new JPanel(new BorderLayout());
		JLabel label = new JLabel(string);
		label.setFont(label.getFont().deriveFont(11F));
		panel.add(label, BorderLayout.WEST);
		return panel;
	}

	private Component createDivider() {
		JLabel div = new JLabel();
		div.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0), 
				BorderFactory.createLineBorder(Color.LIGHT_GRAY)));
		return div;
	}

	private Component createTabbedPanel(int tab, Component c) {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(Box.createRigidArea(new Dimension(tab, 20)));
		panel.add(c);
		return panel;
	}
	
	private Component createSourceSummary(JLabel sourceLabel, AbstractDataSource source) {
		JPanel inner = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LINE_START;
		c.weightx = 1;
		c.gridy = 0;
		inner.add(sourceLabel, c);
		if (source == null) {
			c.gridy = 1;
			JLabel label = new JLabel("Source not configured");
			label.setFont(label.getFont().deriveFont(11F));
			inner.add(createTabbedPanel(DEFAULT_TAB, label), c);
		} else {
			c.gridy = 1;
			JLabel label = new JLabel(source.toHTMLString());
			label.setFont(label.getFont().deriveFont(11F));
			inner.add(createTabbedPanel(DEFAULT_TAB, label), c);
//			c.gridy = 2;
//			inner.add(createTabbedPanel(20, new JLabel("<html>Line 1, line 1<br>Line 2, line 2<br>Line 3, line 3</html>")), c);
		}
		
		return createTabbedPanel(DEFAULT_TAB, inner);
	}
	
	private Component createResultSaversSummary(AbstractResultsSaver resultSavers) {
		JPanel inner = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LINE_START;
		c.weightx = 1;
		if (resultSavers == null) {
			c.gridy = 0;
			inner.add(createTabbedPanel(DEFAULT_TAB, new JLabel("Results savers not configured")), c);
		} else {
			StringBuilder b = new StringBuilder();
			b.append(HTMLUtils.getHTMLHeader());
			b.append("Registered result savers:<br>");
			b.append("To be implemented.<br>");
//			String[][] table = new String[resultSavers.length + 1][2];
//			table[0][0] = "Id";
//			table[0][1] = "Result saver";
//			for (int i = 0; i < resultSavers.length; i++) {
//				table[i + 1][0] = String.valueOf(i + 1);
//				table[i + 1][1] = resultSavers[i].toHTMLString();
//			}
//			b.append(HTMLUtils.encodeTable(table, true));
//			b.append("</html>");
//			c.gridy = 0;
//			JLabel label = new JLabel(b.toString());
//			label.setFont(label.getFont().deriveFont(11F));
//			inner.add(createTabbedPanel(DEFAULT_TAB, label), c);
		}
		return inner;
	}

	private Component createJoinSummary(AbstractJoin join) {
		JPanel inner = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LINE_START;
		c.weightx = 1;
		if (join == null) {
			c.gridy = 0;
			JLabel label = new JLabel("Join not configured");
			label.setFont(label.getFont().deriveFont(11F));
			inner.add(createTabbedPanel(DEFAULT_TAB, label), c);
		} else {
			JLabel label = new JLabel(join.toHTMLString());
			label.setFont(label.getFont().deriveFont(11F));
			inner.add(createTabbedPanel(DEFAULT_TAB, label), c);
		}
		return inner;
	}
	
}
