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


package cdc.gui.components.linkagesanalysis;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.components.AbstractJoin;
import cdc.components.AbstractJoinCondition;
import cdc.components.Filter;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.PropertyBasedColumn;
import cdc.gui.MainFrame;
import cdc.gui.components.linkagesanalysis.dialog.DecisionListener;
import cdc.gui.components.linkagesanalysis.dialog.LinkagesWindowPanel;
import cdc.gui.components.linkagesanalysis.dialog.ViewLinkagesDialog;
import cdc.impl.join.strata.StrataJoinWrapper;
import cdc.utils.Log;
import cdc.utils.RJException;

public class DuplicateLinkageDecisionProvider implements ThreadCreatorInterface, DecisionListener {

	protected static final Object[] OPTIONS_CLOSE = new String[] {"Yes (accept all linkages)", "Yes (reject all linkages)", "Cancel"};
	private DataColumnDefinition[][] dataModel;
	private DataColumnDefinition confidence;
	private DataColumnDefinition stratum;
	private DataColumnDefinition[][] comparedColumns;
	
	private DecisionListener listener;
	
	private boolean ultimateDecisionAcceptAll;
	private volatile boolean allAdded = false;
	
	private AbstractDistance[] distances;
	
	private ViewLinkagesDialog dialog;
	private List internalData = new ArrayList();
	private Thread decisionThread;
	
	//private DuplicateLinkageLoadingThread activeThread;
	
	public DuplicateLinkageDecisionProvider(String windowTitle, DecisionListener decisionListener) {
		
		AbstractJoin join = MainFrame.main.getConfiguredSystem().getJoin();
		dataModel = readModel(join.getOutColumns(), join.getJoinCondition());
		
		AbstractJoinCondition cond = join.getJoinCondition();
		comparedColumns = new DataColumnDefinition[2][cond.getDistanceFunctions().length];
		for (int i = 0; i < comparedColumns[0].length; i++) {
			comparedColumns[0][i] = cond.getLeftJoinColumns()[i];
			comparedColumns[1][i] = cond.getRightJoinColumns()[i];
		}
		
		comparedColumns = removeNotAvailableColumns(comparedColumns, dataModel);
		listener = decisionListener;
		
		//do all the transformations of data column definitions...
		confidence = new PropertyBasedColumn(AbstractJoin.PROPERTY_CONFIDNCE, "src", "Confidence");
		if (join instanceof StrataJoinWrapper) {
			stratum = new PropertyBasedColumn(StrataJoinWrapper.PROPERTY_STRATUM_NAME, "src", "Stratum name");
		}
		
		distances = matchDistances(join.getJoinCondition().getDistanceFunctions(), new DataColumnDefinition[][] {join.getJoinCondition().getLeftJoinColumns(), join.getJoinCondition().getRightJoinColumns()}, comparedColumns);
		
		dialog = new ViewLinkagesDialog(dataModel, true, confidence, stratum, comparedColumns, distances, this, true);
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				int response = JOptionPane.showOptionDialog(dialog, "You are about to close manual decision module. Are you sure?", "Close manual linkage", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
						OPTIONS_CLOSE, OPTIONS_CLOSE[0]);
				if (response == 0) {
					ultimateDecisionAcceptAll = true;
					dialog.setVisible(false);
					startDecisionThread();
				} else if (response == 1) {
					ultimateDecisionAcceptAll = false;
					dialog.setVisible(false);
					startDecisionThread();
				}
			}

		});
		dialog.getLinkageWindowPanel().addDecisionListener(decisionListener);
		dialog.getLinkageWindowPanel().addDecisionListener(this);
//		dialog.getLinkageWindowPanel().addDecisionListener(new DecisionListener() {
//			public void linkageAccepted(DataRow linkage) {
//				activeThread.removeLinkage(linkage);
//			}
//			public void linkageRejected(DataRow linkage) {
//				activeThread.removeLinkage(linkage);
//			}
//		});
		dialog.setTitle(windowTitle);
		new Thread() {
		 public void run() {
			 dialog.setVisible(true);
		 }
		}.start();
	}
	
	private AbstractDistance[] matchDistances(AbstractDistance[] oldDst, DataColumnDefinition[][] oldCols, DataColumnDefinition[][] compared) {
		List dsts = new ArrayList();
		for (int i = 0; i < compared[0].length; i++) {
			for (int j = 0; j < oldCols[0].length; j++) {
				if (compared[0][i].equals(oldCols[0][j]) && compared[1][i].equals(oldCols[1][j])) {
					dsts.add(oldDst[j]);
					break;
				}
			}
		}
		return (AbstractDistance[]) dsts.toArray(new AbstractDistance[] {});
	}

	private DataColumnDefinition[][] removeNotAvailableColumns(DataColumnDefinition[][] compared, DataColumnDefinition[][] model) {
		List lList = new ArrayList();
		List rList = new ArrayList();
		for (int i = 0; i < compared[0].length; i++) {
			if (isAvailable(compared[0][i], model[0]) && isAvailable(compared[1][i], model[1])) {
				lList.add(compared[0][i]);
				rList.add(compared[1][i]);
			}
		}
		return new DataColumnDefinition[][] {(DataColumnDefinition[]) lList.toArray(new DataColumnDefinition[] {}), (DataColumnDefinition[]) rList.toArray(new DataColumnDefinition[] {})};
	}

	private boolean isAvailable(DataColumnDefinition dataColumnDefinition, DataColumnDefinition[] dataColumnDefinitions) {
		for (int i = 0; i < dataColumnDefinitions.length; i++) {
			if (dataColumnDefinition.equals(dataColumnDefinitions[i])) {
				return true;
			}
		}
		return false;
	}

	private DataColumnDefinition[][] readModel(DataColumnDefinition[] outColumns, AbstractJoinCondition cond) {
		String src1 = cond.getLeftJoinColumns()[0].getSourceName();
		List l1 = new ArrayList();
		List l2 = new ArrayList();
		for (int i = 0; i < outColumns.length; i++) {
			if (outColumns[i].getSourceName().equals(src1)) {
				l1.add(outColumns[i]);
			} else {
				l2.add(outColumns[i]);
			}
		}
		return new DataColumnDefinition[][] {(DataColumnDefinition[])l1.toArray(new DataColumnDefinition[] {}), (DataColumnDefinition[])l2.toArray(new DataColumnDefinition[] {})};
	}
	
	public LoadingThread createNewThread(ThreadCreatorInterface provider, LinkagesWindowPanel parent, Filter filter, DataColumnDefinition[] sort, int[] order) {
		return new DuplicateLinkageLoadingThread(internalData, provider, parent, filter, sort, order);
	}

	public AbstractDataSource getDataSource(Filter filter) throws IOException, RJException {
		return null;
	}
	
	public void addUndecidedRecords(DataRow[] linkages) {
		synchronized (internalData) {
			internalData.addAll(Arrays.asList(linkages));
			internalData.notifyAll();
		}
	}

	public boolean isDone() {
		synchronized (internalData) {
			allAdded = true;
			return internalData.isEmpty();
		}
	}
	
	public void closeDecisionWindow() {
		dialog.setVisible(false);
		dialog.getLinkageWindowPanel().removeAllDecisionListeners();
		listener = null;
	}

	public void linkageAccepted(DataRow linkage) {
		synchronized (internalData) {
			internalData.remove(linkage);
		}
	}

	public void linkageRejected(DataRow linkage) {
		synchronized (internalData) {
			internalData.remove(linkage);
		}
	}
	
	private void startDecisionThread() {
		decisionThread = new Thread() {
			public void run() {
				Log.log(getClass(), "Automatic decision thread started.", 1);
				try {
					synchronized (internalData) {
						while (true) {
							if (allAdded && internalData.isEmpty()) {
								return;
							} else if (internalData.isEmpty()) {
								internalData.wait(100);
							} else {
								while (!internalData.isEmpty()) {
									DataRow data = (DataRow) internalData.remove(0);
									if (ultimateDecisionAcceptAll) {
										listener.linkageAccepted(data);
									} else {
										listener.linkageRejected(data);
									}
								}
							}
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Log.log(getClass(), "Automatic decision thread done.", 1);
			}
		};
		decisionThread.start();
	}

}
