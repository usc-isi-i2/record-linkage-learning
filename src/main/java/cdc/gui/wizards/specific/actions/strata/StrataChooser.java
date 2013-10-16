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


package cdc.gui.wizards.specific.actions.strata;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import cdc.components.AbstractDataSource;
import cdc.gui.Configs;
import cdc.impl.join.strata.DataStratum;

public class StrataChooser extends JPanel {
	
	private static final String NEW_STRATA_LABEL = "New stratum";

	public class TabHeader extends JPanel implements ActionListener {
		private JLabel label;
		private JButton button;
		public TabHeader(String label, ImageIcon icon) {
			super(new FlowLayout());
			JLabel jLabel = new JLabel(label);
			add(jLabel);
			button = new JButton(icon);
			button.addActionListener(this);
			button.setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
			add(button);
			setOpaque(false);
			this.label = jLabel;
		}

		public void actionPerformed(ActionEvent arg0) {
			tabs.removeTabAt(tabs.indexOfTabComponent(this));
		}

		public String getTitle() {
			return label.getText();
		}

		public void setTitle(String text) {
			label.setText(text);
		}
		
		public void setEnabled(boolean arg0) {
			button.setEnabled(arg0);
			label.setEnabled(arg0);
		}
		
	}
	
	private AbstractDataSource sourceA;
	private AbstractDataSource sourceB;
	
	private JTabbedPane tabs = new JTabbedPane();
	private JButton addButton;
	
	public StrataChooser(AbstractDataSource sourceA, AbstractDataSource sourceB) {
		super(new GridBagLayout());
		
		this.sourceA = sourceA;
		this.sourceB = sourceB;
		
		tabs.addTab(null, new StratumInnerPanel(tabs, NEW_STRATA_LABEL, sourceA, sourceB));
		tabs.setTabComponentAt(0, new TabHeader(NEW_STRATA_LABEL, Configs.closeIcon));
		
		JPanel buttonsPanel = new JPanel(new FlowLayout());
		addButton = new JButton("Add strata");
		addButton.setPreferredSize(new Dimension(addButton.getPreferredSize().width, 20));
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String strataName = NEW_STRATA_LABEL;
				boolean exists = true;
				int n = 1;
				while (exists) {
					exists = false;
					for (int i = 0; i < tabs.getTabCount(); i++) {
						String currName = ((TabHeader)tabs.getTabComponentAt(i)).getTitle();
						if (currName.equals(strataName)) {
							exists = true;
							strataName = NEW_STRATA_LABEL + " " + n++;
						}
					}
				}
				tabs.addTab(null, new StratumInnerPanel(tabs, strataName, StrataChooser.this.sourceA, StrataChooser.this.sourceB));
				tabs.setTabComponentAt(tabs.getTabCount() - 1, new TabHeader(strataName, Configs.closeIcon));
				tabs.setSelectedIndex(tabs.getTabCount() - 1);
			}
		});
		buttonsPanel.add(addButton);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		add(buttonsPanel, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		add(tabs, c);
	}
	
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		tabs.setEnabled(enabled);
		for (int i = 0; i < tabs.getTabCount(); i++) {
			tabs.setEnabledAt(i, enabled);
			((TabHeader)tabs.getTabComponentAt(i)).setEnabled(enabled);
			tabs.getComponentAt(i).setEnabled(enabled);
		}
		addButton.setEnabled(enabled);
	}
	
	public DataStratum[] getStratumConfiguration() {
		DataStratum[] stratum = new DataStratum[tabs.getTabCount()];
		for (int i = 0; i < stratum.length; i++) {
			StratumInnerPanel panel = (StratumInnerPanel)tabs.getComponentAt(i);
			stratum[i] = new DataStratum(panel.getStratumName(), panel.getLeftStratum(), panel.getRightStratum());
		}
		return stratum;
	}

	public void restoreStrata(DataStratum[] strata) {
		tabs.removeAll();
		for (int i = 0; i < strata.length; i++) {
			tabs.addTab(null, new StratumInnerPanel(tabs, strata[i].getName(), sourceA, sourceB, strata[i]));
			tabs.setTabComponentAt(i, new TabHeader(strata[i].getName(), Configs.closeIcon));
		}
	}
	
	public void dispose() {
		this.sourceA = null;
		this.sourceB = null;
		this.tabs = null;
		this.addButton = null;
	}
	
}
