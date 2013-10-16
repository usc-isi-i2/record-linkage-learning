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


package cdc.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import cdc.utils.GuiUtils;

public class OptionDialog extends JDialog {
	
	private static final Dimension PREFERRED_SIZE = new Dimension(100, 20);
	public static final int RESULT_OK = 1;
	public static final int RESULT_CANCEL = 2;
	
	private int result = RESULT_CANCEL;
	private boolean done = false;
	
	private List listeners = new ArrayList();
	private List internalListeners = new ArrayList();
	private JPanel mainPanel;
	
	private int n = 0;
	private JPanel visiblePanel;
	
	public OptionDialog(Window parent, String title) {
		super(parent, title);
		setModal(true);
		setMainLayout();
		addButtons();
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				internalListeners.clear();
				checkValidators(mainPanel);
				for (int i = 0; i < internalListeners.size(); i++) {
					DialogListener listener = (DialogListener) internalListeners.get(i);
					listener.windowClosing(OptionDialog.this);
				}
			}
		});
	}
	
	private GridBagConstraints getGtidBagConstraints() {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = n++;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		return c;
	}

	public OptionDialog(JFrame parent, String title) {
		super(parent, title, true);
		setMainLayout();
		addButtons();
	}

	public OptionDialog(JDialog parent, String title) {
		super(parent, title, true);
		setMainLayout();
		addButtons();
		super.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	}
	
	private void setMainLayout() {
		getContentPane().setLayout(new GridBagLayout());
		visiblePanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = getGtidBagConstraints();
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		getContentPane().add(visiblePanel, c);
	}
	
	public void addOptionDialogListener(DialogListener listener) {
		this.listeners.add(listener);
	}
	
	private void addButtons() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton ok = new JButton("OK");
		JButton cancel = new JButton("Cancel");
		ok.setPreferredSize(PREFERRED_SIZE);
		cancel.setPreferredSize(PREFERRED_SIZE);
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				internalListeners.clear();
				checkValidators(mainPanel);
				for (int i = 0; i < listeners.size(); i++) {
					DialogListener listener = (DialogListener) listeners.get(i);
					if (!listener.okPressed(OptionDialog.this)) {
						return;
					}
				}
				for (int i = 0; i < internalListeners.size(); i++) {
					DialogListener listener = (DialogListener) internalListeners.get(i);
					if (!listener.okPressed(OptionDialog.this)) {
						return;
					}
				}
				result = RESULT_OK;
				dispose();
			}
		});
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				internalListeners.clear();
				checkValidators(mainPanel);
				for (int i = 0; i < listeners.size(); i++) {
					DialogListener listener = (DialogListener) listeners.get(i);
					listener.cancelPressed(OptionDialog.this);
				}
				result = RESULT_CANCEL;
				dispose();
			}
		});
		panel.add(ok);
		panel.add(cancel);
		GridBagConstraints c = getGtidBagConstraints();
		this.getContentPane().add(panel, c);
	}

	public void setMainPanel(JPanel panel) {
		mainPanel = panel;
		visiblePanel.removeAll();
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		visiblePanel.add(panel, c);
	}
	
	private void checkValidators(JComponent panel) {
		if (panel instanceof DialogListener) {
			internalListeners.add(panel);
		}
		for (int i = 0; i < panel.getComponentCount(); i++) {
			Component comp = panel.getComponent(i);
			if (comp instanceof JComponent) {
				checkValidators((JComponent) comp);
			}
		}
	}

	public void setVisible(boolean b) {
		if (b && getOwner() != null) {
			setLocation(GuiUtils.getCenterLocation(getOwner(), this));
		}
		super.setVisible(b);
	}
	
	public int getResult() {
		if (!done) {
			pack();
			setVisible(true);
			done = true;
		}
		return result;
	}
}
