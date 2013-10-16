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


package cdc.impl.em;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import cdc.components.AbstractJoinCondition;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.gui.StoppableThread;
import cdc.gui.external.JXErrorDialog;
import cdc.gui.wizards.WizardAction;
import cdc.impl.datasource.sampling.DataSampleInterface;
import cdc.impl.datasource.sampling.FullSampler;
import cdc.impl.em.actions.ConfigureBlockingMethod;
import cdc.impl.em.actions.ConfigureSearchMethodAction;
import cdc.impl.em.actions.ConfigureSourcesAction;
import cdc.impl.join.blocking.BucketManager;
import cdc.impl.join.blocking.BlockingFunction;
import cdc.utils.RJException;

public class EMThread extends StoppableThread {

	private volatile boolean stopScheduled = false;
	private EMResultsReporter wizard;
	private ConfigureSourcesAction sources;
	private ConfigureSearchMethodAction search;
	private ConfigureBlockingMethod blocking;
	//private EMRunnerAction runnerGUI;
	private AbstractJoinCondition condition;
	private int[] weights;
	private EMEstimator estimator;
	
	public EMThread(EMResultsReporter wizard, WizardAction[] actions, AbstractJoinCondition condition) {
		this.wizard = wizard;
		sources = (ConfigureSourcesAction) actions[0];
		search = (ConfigureSearchMethodAction) actions[1];
		blocking = (ConfigureBlockingMethod) actions[2];
		//runnerGUI = (EMRunnerAction) actions[3];
		this.condition = condition;
	}

	public void run() {
		try {
			estimator = new EMEstimator();
			estimator.setLogSink(wizard.getLogSink());
			EMEstimator.cancel = false;
			DataSampleInterface leftSource = sources.getSamplingConfigurationForSourceA();
			DataSampleInterface rightSource = sources.getSamplingConfigurationForSourceB();
			BlockingFunction function = null;
			int hashingAttribute = -1; 
			DataColumnDefinition[][] columns = null;
			
			if (search.needsBlocking()) {
				hashingAttribute = blocking.getBlockingAttribute();
				columns = new DataColumnDefinition[][]{{condition.getLeftJoinColumns()[hashingAttribute], 
					  condition.getRightJoinColumns()[hashingAttribute]}};
				function = blocking.getHashingFunction(columns);
			}
			
			DataRow[][] rowsA;
			DataRow[][] rowsB;
			if (function != null) {
				BucketManager manager = new BucketManager(function);
				DataRow row;
				int n = 0;
				if (stopped()) {doStop(); return;}
				EMEstimator.sink.log("Source '" + leftSource.getInnerName() + "' is being sampled");
				while ((row = leftSource.getNextRow()) != null) {
					manager.addToBucketLeftSource(row);
					n++;
					if ((n % 1000) == 0 && n != 0) {
						EMEstimator.sink.log("Sampled next " + 1000 + " rows");
					}
				}
				EMEstimator.sink.log("Sampling finished with " + n + " records");
				if (stopped()) {doStop(); return;}
				n = 0;
				EMEstimator.sink.log("Source '" + rightSource.getInnerName() + "' is being sampled");
				while ((row = rightSource.getNextRow()) != null) {
					manager.addToBucketRightSource(row);
					n++;
					if ((n % 1000) == 0 && n != 0) {
						EMEstimator.sink.log("Sampled next " + 1000 + " rows");
					}
				}
				EMEstimator.sink.log("Sampling finished with " + n + " records");
				manager.addingCompleted();
				if (stopped()) {doStop(); return;}
				if (leftSource instanceof FullSampler) {
					DataRow[][] activeBucket;
					ArrayList list1 = new ArrayList();
					ArrayList list2 = new ArrayList();
					read: for (int i = 0; i < ((FullSampler)leftSource).getNumberOfBlocks(); i++) {
						do {
							activeBucket = manager.getBucket();
							if (activeBucket == null) {
								break read;
							}
						} while (activeBucket[0].length == 0 || activeBucket[1].length == 0);
						//System.out.println("Buckets: " + activeBucket[0].length + " <--> " + activeBucket[1].length);
						list1.add(activeBucket[0]);
						list2.add(activeBucket[1]);
						if (stopped()) {doStop(); return;}
					}
					rowsA = (DataRow[][]) list1.toArray(new DataRow[][] {});
					rowsB = (DataRow[][]) list2.toArray(new DataRow[][] {});
				} else {
					DataRow[][] activeBucket;
					ArrayList list1 = new ArrayList();
					ArrayList list2 = new ArrayList();
					if (stopped()) {doStop(); return;}
					while ((activeBucket = manager.getBucket()) != null) {
						list1.add(activeBucket[0]);
						list2.add(activeBucket[1]);
					}
					if (stopped()) {doStop(); return;}
					rowsA = (DataRow[][]) list1.toArray(new DataRow[][] {});
					rowsB = (DataRow[][]) list2.toArray(new DataRow[][] {});
				}
				if (search.isAllToAll()) {
					ArrayList list1 = new ArrayList();
					ArrayList list2 = new ArrayList();
					if (stopped()) {doStop(); return;}
					for (int i = 0; i < rowsA.length; i++) {
						list1.addAll(Arrays.asList(rowsA[i]));
					}
					if (stopped()) {doStop(); return;}
					for (int i = 0; i < rowsB.length; i++) {
						list2.addAll(Arrays.asList(rowsB[i]));
					}
					if (stopped()) {doStop(); return;}
					rowsA = new DataRow[][] {(DataRow[])list1.toArray(new DataRow[] {})};
					rowsB = new DataRow[][] {(DataRow[])list2.toArray(new DataRow[] {})};
				}
			} else {
				ArrayList list1 = new ArrayList();
				ArrayList list2 = new ArrayList();
				DataRow row;
				if (stopped()) {doStop(); return;}
				EMEstimator.sink.log("Sampling is starting (will sample first n rows from data sources)");
				while ((row = leftSource.getNextRow()) != null) {
					list1.add(row);
				}
				if (stopped()) {doStop(); return;}
				while ((row = rightSource.getNextRow()) != null) {
					list2.add(row);
				}
				if (stopped()) {doStop(); return;}
				rowsA = new DataRow[][] {(DataRow[]) list1.toArray(new DataRow[] {})};
				rowsB = new DataRow[][] {(DataRow[]) list2.toArray(new DataRow[] {})};
			}
			
			//now - rowsA and rowsB have what should be compared
			weights = estimator.weightsTo0_100(estimator.runEMMethodBlocking(rowsA, rowsB, condition, 0.02));
			//System.out.println("WEIGHTS: " + weights);\\\\\\\\['
			
			doStop();
		} catch (RJException e) {
			JXErrorDialog.showDialog(wizard, "Error running EM method", e);
		} catch (IOException e) {
			JXErrorDialog.showDialog(wizard, "Error running EM method", e);
		}
	}
	
	private void doStop() {
		EMEstimator.cancel = false;
		wizard.finished(weights != null);
	}

	public boolean stopped() {
		return stopScheduled;
	}
	
	public void scheduleStop() {
		stopScheduled = true;
		EMEstimator.cancel = true;
		System.out.println("Cancelled...");
	}

	public int[] getFinalWeights() {
		return weights;
	}

}
