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


package cdc.gui.components.dynamicanalysis;

import java.io.IOException;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.gui.StoppableThread;
import cdc.utils.Props;
import cdc.utils.RJException;

public class AnalysisThread extends StoppableThread  {
	
	private static final int ZERO_OUT_INTERVAL = Props.getInteger("analysis-thread-non-matching-report-interval");
	private static final int STEP = Props.getInteger("analysis-thread-step");
	
	private DynamicAnalysisFrame frame;
	private AbstractDataSource sourceA;
	private AbstractDataSource sourceB;
	private DataColumnDefinition colA;
	private DataColumnDefinition colB;
	private AbstractDistance distane;
	private double emptyScore = 0;
	private volatile boolean stop = false;
	
	public AnalysisThread(DynamicAnalysisFrame frame, Object[] params) throws IOException, RJException {
		setPriority(Thread.MIN_PRIORITY);
		this.frame = frame;
		this.sourceA = (AbstractDataSource) params[0];
		this.sourceB = (AbstractDataSource) params[1];
		this.colA = (DataColumnDefinition) params[2];
		this.colB = (DataColumnDefinition) params[3];
		this.distane = (AbstractDistance) params[4];
		this.emptyScore = ((Double)params[5]).doubleValue();
		
		//This is an ugly hack!! Should probably change that.
		if (colB.getSourceName().equals(sourceA.getSourceName())) {
			sourceB = sourceA.copy();
		}
		if (colA.getSourceName().equals(sourceB.getSourceName())) {
			sourceA = sourceB.copy();
		}
		
	}
	
	public void run() {
		
		try {
			Thread.sleep(100);
			long tested = 0;
			for (int i = 0; i < STEP; i++) {
				sourceA.reset();
				sourceA.getNextRows(i);
				//System.out.println("i = " + i);
				DataRow[] tmp = sourceA.getNextRows(1);
				mainLoop: while (true) {
					this.sourceB.reset();
					DataRow rowA;
					if (tmp == null || tmp.length == 0) {
						break mainLoop;
					}
					rowA = tmp[0];
					
					while (rowA != null && !stop) {
						DataRow rowB;
						sourceB.reset();
						while ((rowB = sourceB.getNextRow()) != null) {
							if (stop) {
								frame.finished(true);
								return;
							}
							DataCell cellA = rowA.getData(colA);
							DataCell cellB = rowB.getData(colB);
							double distance;
							if (cellA.isEmpty(colA) || cellB.isEmpty(colB)) {
								distance = emptyScore * 100;
							} else {
								distance = distane.distance(cellA, cellB);
							}
							
							tested++;
							distance = Math.round(distance);
							if (distance == 0 && !(tested % ZERO_OUT_INTERVAL == 0)) {
								continue;
							}
							tested = 0;
							frame.addRow(new String[] {String.valueOf(cellA.getValue()), 
									String.valueOf(cellB.getValue()), String.valueOf(distance)});
							Thread.sleep(1);
						}
						rowA = sourceA.getNextRow();
					}
					tmp = sourceA.getNextRows(STEP);
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			frame.finished(false);
			frame.setWarningMessage(e.toString());
			return;
		} finally {
			System.out.println("[INFO] Analysis thread done. Closing data sources.");
			try {
				sourceA.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (RJException e) {
				e.printStackTrace();
			}
			try {
				sourceB.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (RJException e) {
				e.printStackTrace();
			}
		}
		frame.finished(true);
		
	}

	public void scheduleStop() {
		stop = true;
	}
	
}
