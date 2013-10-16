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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.Timer;

import cdc.components.AbstractDataSource;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.converters.ModelGenerator;
import cdc.gui.StoppableThread;
import cdc.gui.external.JXErrorDialog;
import cdc.utils.StringUtils;

public class SummaryThread extends StoppableThread {

	private static final String LABEL_TYPE_INTS = "Integer";
	private static final String LABEL_TYPE_REALS = "Real";
	private static final String LABEL_TYPE_STRINGS = "String";
	private static final String LABEL_TYPE_VARIOUS = "Varies";

	private class MouseListenerImpl extends MouseAdapter {
		
		private class ExitDisabler extends MouseAdapter {
			public void mouseEntered(MouseEvent e) {
				if (timer != null) {
					timer.stop();
				}
			}
			public void mouseExited(MouseEvent e) {
				newKillerTimer();
			}
		}
		
		private int id;
		private Timer timer;
		private Popup popup;
		
		public MouseListenerImpl(int id) {
			this.id = id;
		}
		
		public void mouseEntered(MouseEvent e) {
			if (timer != null) {
				timer.stop();
			}
			if (popup != null) {
				popup.hide();
			}
			JScrollPane scroll = new JScrollPane(lists[id-1]);
			scroll.setPreferredSize(new Dimension(150, 150));
			JRootPane tooltip = new JRootPane();
			tooltip.setLayout(new BorderLayout());
			tooltip.setBackground(Color.white);
			tooltip.setBorder(BorderFactory.createLineBorder(Color.black));
			tooltip.add(new JLabel("Attribute: " + columns[id-1].getColumnName()), BorderLayout.NORTH);
			tooltip.add(Box.createRigidArea(new Dimension(10, 10)), BorderLayout.CENTER);
			tooltip.add(scroll, BorderLayout.SOUTH);
			
			tooltip.addMouseListener(new ExitDisabler());
			lists[id-1].addMouseListener(new ExitDisabler());
			scroll.getVerticalScrollBar().addMouseListener(new ExitDisabler());

			
			JComponent src = (JComponent) e.getSource();
			popup = PopupFactory.getSharedInstance().getPopup(e.getComponent(), tooltip, 
					(int)src.getLocationOnScreen().getX() + src.getWidth(), (int)src.getLocationOnScreen().getY());
			popup.show();
		}
		
		public void mouseExited(MouseEvent e) {
			newKillerTimer();
		}

		private void newKillerTimer() {
			if (timer != null) {
				timer.stop();
			}
			timer = new Timer(100, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (popup != null) {
						popup.hide();
						popup = null;
					}
					timer.stop();
				}});
			timer.start();
		}
			
	}
	
	private volatile boolean stop = false;
	private SummaryWindow window;
	private AbstractDataSource source;
	private JLabel status;
	private DataColumnDefinition[] columns;
	private JList lists[];
	
	public SummaryThread(SummaryWindow window, AbstractDataSource abstractDataSource) {
		this.window = window;
		this.source = abstractDataSource;
		setPriority(Thread.MIN_PRIORITY);
	}
	
	public void run() {
		source.setModel(new ModelGenerator(source.getAvailableColumns()));
		columns = source.getDataModel().getOutputFormat();
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
		
		JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel label = new JLabel("Status: ");
		status = new JLabel("analyzing data source...");
		statusPanel.add(label);
		statusPanel.add(status);
		statusPanel.setBackground(Color.white);
		mainPanel.add(statusPanel);
		
		String[][] samples = new String[columns.length][200];
		int[] sampleLen = new int[columns.length];
		boolean[] integers = new boolean[columns.length + 1];
		boolean[] doubles = new boolean[columns.length + 1];
		boolean[] strings = new boolean[columns.length + 1];
		int[] notnulls = new int[columns.length + 1];
		JLabel[] names = new JLabel[columns.length + 1];
		JLabel[] isNumber = new JLabel[columns.length + 1];
		JLabel[] percNonEmpty = new JLabel[columns.length + 1];
		JLabel[] examples = new JLabel[columns.length + 1];
		JPanel[] panels = new JPanel[columns.length + 1];
		//ToolTipAttacher[] tooltips = new ToolTipAttacher[columns.length];
		lists = new JList[columns.length];
		names[0] = new JLabel("Attribute name");
		names[0].setPreferredSize(new Dimension(180, 20));
		names[0].setFont(names[0].getFont().deriveFont(11F));
		names[0].setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 1, 1, 0, Color.darkGray), 
				BorderFactory.createEmptyBorder(2, 5, 2, 2)));
		isNumber[0] = new JLabel("Type");
		isNumber[0].setPreferredSize(new Dimension(50, 20));
		isNumber[0].setFont(isNumber[0].getFont().deriveFont(11F));
		isNumber[0].setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 1, 1, 0, Color.darkGray), 
				BorderFactory.createEmptyBorder(2, 5, 2, 2)));
		percNonEmpty[0] = new JLabel("% empty");
		percNonEmpty[0].setPreferredSize(new Dimension(55, 20));
		percNonEmpty[0].setFont(percNonEmpty[0].getFont().deriveFont(11F));
		percNonEmpty[0].setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 1, 1, 0, Color.darkGray), 
				BorderFactory.createEmptyBorder(2, 5, 2, 2)));
		examples[0] = new JLabel("");
		examples[0].setPreferredSize(new Dimension(65, 20));
		examples[0].setFont(percNonEmpty[0].getFont().deriveFont(11F));
		examples[0].setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 1, 1, 1, Color.darkGray), 
				BorderFactory.createEmptyBorder(2, 5, 2, 2)));
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setHgap(0);
		flowLayout.setVgap(0);
		panels[0] = new JPanel(flowLayout);
		//panels[0].setBackground(Color.LIGHT_GRAY);
		panels[0].add(names[0]);
		panels[0].add(isNumber[0]);
		panels[0].add(percNonEmpty[0]);
		mainPanel.add(panels[0]);
		panels[0].add(examples[0]);
		for (int i = 1; i < examples.length; i++) {
			names[i] = new JLabel(columns[i-1].getColumnName());
			names[i].setPreferredSize(new Dimension(180, 20));
			names[i].setFont(names[i].getFont().deriveFont(11F));
			names[i].setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 1, 1, 0, Color.gray), 
					BorderFactory.createEmptyBorder(2, 5, 2, 2)));
			isNumber[i] = new JLabel("??");
			isNumber[i].setPreferredSize(new Dimension(50, 20));
			isNumber[i].setFont(isNumber[i].getFont().deriveFont(11F));
			isNumber[i].setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 1, 1, 0, Color.gray), 
					BorderFactory.createEmptyBorder(2, 5, 2, 2)));
			percNonEmpty[i] = new JLabel("??");
			percNonEmpty[i].setPreferredSize(new Dimension(55, 20));
			percNonEmpty[i].setFont(percNonEmpty[i].getFont().deriveFont(11F));
			percNonEmpty[i].setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 1, 1, 0, Color.gray), 
					BorderFactory.createEmptyBorder(2, 5, 2, 2)));
			examples[i] = new JLabel("See...", JLabel.CENTER);
			examples[i].setPreferredSize(new Dimension(65, 20));
			examples[i].setFont(examples[i].getFont().deriveFont(11F));
			examples[i].setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 1, 1, 1, Color.gray), 
					BorderFactory.createCompoundBorder(
							BorderFactory.createEmptyBorder(2, 5, 2, 2),
							BorderFactory.createEtchedBorder())));
			examples[i].addMouseListener(new MouseListenerImpl(i));
			panels[i] = new JPanel(flowLayout);
			panels[i].setBackground(Color.white);
			panels[i].add(names[i]);
			panels[i].add(isNumber[i]);
			panels[i].add(percNonEmpty[i]);
			panels[i].add(examples[i]);
			mainPanel.add(panels[i]);
			integers[i] = false;
			doubles[i] = false;
			strings[i] = false;
			
			lists[i-1] = new JList(new DefaultListModel());
		}
		
		window.setStatisticPanel(mainPanel);
		DataRow row;
		int rows = 0;
		try {
			while ((row = source.getNextRow()) != null) {
				if (stop) {
					window.finished();
					return;
				}
				rows++;
				for (int i = 0; i < columns.length; i++) {
					DataCell value = row.getData(columns[i]);
					String stringValue = value.getValue().toString();
					if (!StringUtils.isNullOrEmpty(stringValue)) {
						if (sampleLen[i] < samples[i].length && !containsSample(samples[i], sampleLen[i], stringValue)) {
							samples[i][sampleLen[i]++] = stringValue;
							((DefaultListModel)lists[i].getModel()).addElement(stringValue);
						}
						notnulls[i+1]++;
						try {
							Integer.parseInt(stringValue);
							integers[i+1] = true;
						} catch (NumberFormatException e) {
							try {
								Double.parseDouble(stringValue);
								doubles[i+1] = true;
							} catch (NumberFormatException ex) {
								strings[i+1] = true;
							}
						}
						if ((integers[i+1] || doubles[i+1]) && strings[i+1]) {
							isNumber[i+1].setText(LABEL_TYPE_VARIOUS);
						} else if (doubles[i+1]) {
							isNumber[i+1].setText(LABEL_TYPE_REALS);
						} else if (integers[i+1]) {
							isNumber[i+1].setText(LABEL_TYPE_INTS);
						} else if (strings[i+1]) {
							isNumber[i+1].setText(LABEL_TYPE_STRINGS);
						}
					}
					percNonEmpty[i+1].setText(String.valueOf((100000 - Math.round(notnulls[i+1]/(double)rows * 100000)) / (double)1000));
				}
			}
			status.setText("Analysis done. Source size: " + rows + " rows.");
		} catch (Exception e) {
			e.printStackTrace();
			JXErrorDialog.showDialog(this.window, "Error", e);
		}
		window.finished();
	}
	
	private boolean containsSample(String[] strings, int i, String newVal) {
		for (int j = 0; j < i; j++) {
			if (newVal.equals(strings[j])) {
				return true;
			}
		}
		return false;
	}

	public void scheduleStop() {
		this.stop = true;
		status.setText("Analysis stopped by user.");
	}

}
