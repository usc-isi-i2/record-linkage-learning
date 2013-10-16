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


package cdc.components;

import java.util.ArrayList;
import java.util.List;

import cdc.datamodel.DataRow;
import cdc.gui.components.linkagesanalysis.DuplicateLinkageDecisionProvider;
import cdc.gui.components.linkagesanalysis.dialog.DecisionListener;

public class ManualDecisionModule implements DecisionListener {
	
	private DuplicateLinkageDecisionProvider decisionWindowProvider;
	
	private List toBeDecided = new ArrayList(); 
	private List decided = new ArrayList();
	
	public ManualDecisionModule() {
	}
	
	public void addRow(DataRow row) {
		//System.out.println("Adding to manual decision: " + row);
		synchronized (toBeDecided) {
			if (decisionWindowProvider == null) {
				decisionWindowProvider = new DuplicateLinkageDecisionProvider("Linkages - manual decision", this);
			}
			toBeDecided.add(row);
		}
		decisionWindowProvider.addUndecidedRecords(new DataRow[] {row});
	}
	
	public ManualDecision getNextDecidedRow() {
		try {
			synchronized (toBeDecided) {
				while (true) {
					if (decided.size() != 0) {
						ManualDecision row = (ManualDecision) decided.remove(0);
						toBeDecided.remove(row.row);
						//System.out.println("size=" + toBeDecided.size());
						return row;
					} else if (toBeDecided.size() == 0) {
						if (decisionWindowProvider != null) {
							decisionWindowProvider.closeDecisionWindow();
							decisionWindowProvider = null;
						}
						return null;
					} else {
						toBeDecided.wait();
					}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public class ManualDecision {

		private DataRow row;
		private boolean accepted;
		
		public ManualDecision(DataRow linkage, boolean b) {
			row = linkage;
			accepted = b;
		}

		public boolean isAccepted() {
			return accepted;
		}

		public DataRow getRow() {
			return row;
		}
		
	}

	public void linkageAccepted(DataRow linkage) {
		//RowUtils.linkageManuallyAccepted(linkage);
		synchronized (toBeDecided) {
			decided.add(new ManualDecision(linkage, true));
			toBeDecided.notifyAll();
		}
	}

	public void linkageRejected(DataRow linkage) {
		//RowUtils.linkageManuallyRejected(linkage);
		synchronized (toBeDecided) {
			decided.add(new ManualDecision(linkage, false));
			toBeDecided.notifyAll();
		}
	}
	
}
