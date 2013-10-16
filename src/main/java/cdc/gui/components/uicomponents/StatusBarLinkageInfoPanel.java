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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import cdc.configuration.ConfiguredSystem;
import cdc.gui.Configs;
import cdc.utils.Utils;

public class StatusBarLinkageInfoPanel extends JPanel {
	
	private JPanel detailsPanel = new JPanel();
	private JPanel informationPanel = new JPanel();
	private JLabel status;
	private JLabel button;
	
	private int nextRow = 0;
	
	public StatusBarLinkageInfoPanel() {
		status = new JLabel("No linkage/deduplication was performed", JLabel.LEFT);
		button = new JLabel(Configs.arrowUpDash);
		detailsPanel.setVisible(false);
		detailsPanel.setLayout(new BorderLayout());
		detailsPanel.add(new JScrollPane(informationPanel), BorderLayout.CENTER);
		informationPanel.setLayout(new GridBagLayout());
		informationPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		
		button.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				detailsPanel.setVisible(!detailsPanel.isVisible());
				setStatusBarButtonLabel();
				//detailsPanel.validate();
				//detailsPanel.invalidate();
			}
		});
		
		setStatusBarButtonLabel();
		button.setVisible(false);
		
		setLayout(new FlowLayout());
		add(status);
		add(button);
	}
	
	private void setStatusBarButtonLabel() {
		if (!detailsPanel.isVisible()) {
			button.setToolTipText("Open linkage summary details");
			button.setIcon(Configs.arrowUpDash);
		} else {
			button.setToolTipText("Close linkage summary details");
			button.setIcon(Configs.arrowDownDash);
		}
	}
	
	private GridBagConstraints[] getNextConst() {
		GridBagConstraints[] c = new GridBagConstraints[2];
		c[0] = new GridBagConstraints(0, nextRow, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);
		c[1] = new GridBagConstraints(1, nextRow++, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);
		return c;
	}
	
	public void linkageCompleted(ConfiguredSystem system, boolean cancelled, long time, int cnt) {
		status.setText(Utils.getStatusBarSummaryMessage(system, cancelled, time, cnt));
		button.setVisible(true);
		informationPanel.removeAll();
		nextRow = 0;
		GridBagConstraints[] c = getNextConst();
		c[0].gridwidth = 2;
		c[0].anchor = GridBagConstraints.NORTHWEST;
		c[0].insets = new Insets(0,0,5,0);
		informationPanel.add(new JLabel(system.isDeduplication() ? "Deduplication summary" : "Linkage summary"), c[0]);
		String[][] summary = Utils.getShortSummary(system, cancelled, time, cnt);
		for (int i = 0; i < summary.length; i++) {
			c = getNextConst();
			c[0].anchor = GridBagConstraints.NORTHWEST;
			c[1].anchor = GridBagConstraints.NORTHWEST;
			informationPanel.add(new JLabel(summary[i][0]), c[0]);
			informationPanel.add(new JLabel(summary[i][1]), c[1]);
		}
		c = getNextConst();
		c[0].weighty = 1;
		informationPanel.add(new JPanel(), c[0]);
		detailsPanel.repaint();
	}

	public JPanel getDetailsPanel() {
		return detailsPanel;
	}

	public void clearCurrentSummary() {
		informationPanel.removeAll();
		status.setText("No linkage/deduplication was performed.");
		detailsPanel.setVisible(false);
		button.setVisible(false);
		setStatusBarButtonLabel();
	}
	
	
}
