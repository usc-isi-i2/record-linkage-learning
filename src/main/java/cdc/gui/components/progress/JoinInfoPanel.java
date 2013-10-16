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


package cdc.gui.components.progress;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Hashtable;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;

import cdc.gui.ThreadPriorityChangedListener;

public class JoinInfoPanel extends JPanel {
	
	private ProgressDialog parent;
	private JSlider slider = new JSlider(Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);
	private JLabel joinedNumber = new JLabel("0");
	
	private int currX = 0;
	
	public JoinInfoPanel(ProgressDialog parent) {
		setLayout(new GridBagLayout());
		
		add(new JLabel("Join priority:"), getDefaultConstraints());
		add(Box.createRigidArea(new Dimension(10,10)), getDefaultConstraints());
		
		slider.setPreferredSize(new Dimension(100, 50));
		slider.setSnapToTicks(true);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.setMinorTickSpacing(1);
		Hashtable dict = new Hashtable();
		dict.put(new Integer(Thread.MIN_PRIORITY), new JLabel("Low"));
		dict.put(new Integer(Thread.MAX_PRIORITY), new JLabel("High"));
		slider.setLabelTable(dict);
		add(slider, getDefaultConstraints());
		
		GridBagConstraints c = getDefaultConstraints();
		c.weightx = 1.0;
		add(new JPanel(), c);
		
		add(new JLabel("Number of joined records:"), getDefaultConstraints());
		add(Box.createRigidArea(new Dimension(10,10)), getDefaultConstraints());
		
		joinedNumber.setPreferredSize(new Dimension(100, 20));
		add(joinedNumber);
		
		this.parent = parent;
	}
	
	private GridBagConstraints getDefaultConstraints() {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = currX++;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		return c;
	}
	
	public void incrementJoined() {
		int curr = Integer.parseInt(joinedNumber.getText());
		joinedNumber.setText(String.valueOf(curr + 1));
	}
	
	public void addPriorityListener(ThreadPriorityChangedListener listener) {
		slider.addChangeListener(listener);
	}

	public void joinCompleted() {
		parent.setVisible(false);
	}

	public JProgressBar getProgressBar() {
		return parent.getProgressBar();
	}
}
