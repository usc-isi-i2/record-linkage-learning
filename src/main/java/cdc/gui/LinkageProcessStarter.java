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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;

import cdc.components.AbstractJoin;
import cdc.configuration.ConfiguredSystem;
import cdc.gui.components.progress.JoinDetailsPanel;
import cdc.gui.components.progress.JoinInfoPanel;
import cdc.gui.components.progress.ProgressDialog;
import cdc.gui.external.JXErrorDialog;
import cdc.utils.GuiUtils;
import cdc.utils.RJException;

public class LinkageProcessStarter implements ProcessStarterInterface {

	private ProgressDialog progressReporter;
	private LinkageThread thread;
	private DetailsActionListener detailsListener;
	
	public void startProcessAndWait(ConfiguredSystem system) {
		
		progressReporter = new ProgressDialog(MainFrame.main, "Joining records...");
		JoinInfoPanel infoPanel = new JoinInfoPanel(progressReporter);
		thread = new LinkageThread(system, infoPanel);
		progressReporter.setInfoPanel(infoPanel);
		progressReporter.addCancelListener(new CancelThreadListener(thread));
		if (detailsListener != null) {
			detailsListener.detailsPanel = null;
			detailsListener.dialog = null;
			detailsListener.join = null;
		}
		progressReporter.addDetailsActionListener(detailsListener = new DetailsActionListener(progressReporter, system.getJoin()));
		infoPanel.addPriorityListener(new ThreadPriorityChangedListener(thread));
		progressReporter.setLocation(GuiUtils.getCenterLocation(MainFrame.main, progressReporter));
		thread.start();
		progressReporter.setVisible(true);
		if (detailsListener.detailsPanel != null) {
			detailsListener.detailsPanel.windowClosed();
		}
		
	}
	
	public class DetailsActionListener implements ActionListener {

		private AbstractJoin join;
		private ProgressDialog dialog;
		private JoinDetailsPanel detailsPanel;
		
		public DetailsActionListener(ProgressDialog dialog, AbstractJoin join) {
			this.join = join;
			this.dialog = dialog;
		}

		public void actionPerformed(ActionEvent e) {
			if (!dialog.isDetailsOn()) {
				join.getJoinCondition().setCanUseOptimisticEval(false);
				try {
					if (detailsPanel != null) {
						detailsPanel.windowClosed();
					}
					detailsPanel = new JoinDetailsPanel(join);
					detailsPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
					dialog.setDetailsPanel(detailsPanel);
				} catch (RJException ex) {
					JXErrorDialog.showDialog(MainFrame.main, "Error registering join listener", ex);
				}
			} else {
				if (detailsPanel != null) {
					try {
						detailsPanel.close();
					} catch (RJException e1) {
						e1.printStackTrace();
						JXErrorDialog.showDialog(MainFrame.main, "Error", e1);
					}
					join.getJoinCondition().setCanUseOptimisticEval(true);
				}
			}
		}

	}

//	public void appendSummaryMessage(String msg) {
//		thread.appendSummaryMessage(msg);
//	}

}
