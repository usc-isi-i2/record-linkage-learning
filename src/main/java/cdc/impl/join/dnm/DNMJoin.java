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


package cdc.impl.join.dnm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.components.AbstractJoin;
import cdc.components.AbstractJoinCondition;
import cdc.components.EvaluatedCondition;
import cdc.components.LinkageSummary;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.gui.GUIVisibleComponent;
import cdc.impl.conditions.WeightedJoinCondition;
import cdc.impl.datasource.sampling.DataSampleInterface;
import cdc.impl.datasource.sampling.RandomSampler;
import cdc.impl.join.blocking.BucketManager;
import cdc.impl.join.blocking.BlockingFunction;
import cdc.utils.HTMLUtils;
import cdc.utils.Props;
import cdc.utils.RJException;
import cdc.utils.RowUtils;

//Assume: distance is 0..100 (100 is the closest!!)

public class DNMJoin extends AbstractJoin {
	
	public static final String PROP_CLUSTER_PARAMS = "clustering-params";
	private static final int MAX_COMPARISONS = 	Props.getInteger("dnm-max-comparisons-factor");
	private static final int MAX_SL_COMPARISONS = Props.getInteger("dnm-max-second-level-comparisons-factor");
	
	private boolean closed = false;
	
	private KMeansClusterer clusterer;
	private BucketManager buckets;
	private DataRow[][] activeBucket;
	
	private int index1;
	private int index2;
	private boolean secondLevelActive = false;
	private BucketManager secondLevel;
	private boolean sampleLeft = false;
	private DataColumnDefinition[] sampledSourceColumns;
	private ClusteringDistance[] clusteringDistances;
	private BlockingFunction hash;
	private DataColumnDefinition[][] clusteringColumns;
	
	public DNMJoin(AbstractDataSource sourceA, AbstractDataSource sourceB, AbstractJoinCondition condition,
			DataColumnDefinition[] outColumns, Map params) throws IOException, RJException {
		super(fixSource(sourceA, condition.getLeftJoinColumns()), 
				fixSource(sourceB, condition.getRightJoinColumns()), condition, outColumns, params);
		
		String ids = (String) params.get(PROP_CLUSTER_PARAMS);
		if (ids == null) {
			throw new RJException("Configuration of ClusteringJoin requires parameter " + PROP_CLUSTER_PARAMS + ". Please verify the configuration.");
		}
		String[] idsT = ids.split(",");
		int[] idsInt = new int[idsT.length];
		for (int i = 0; i < idsInt.length; i++) {
			idsInt[i] = Integer.parseInt(idsT[i]);
		}
		
		AbstractDistance[] distances = new AbstractDistance[idsInt.length];
		DataColumnDefinition[] leftCols = new DataColumnDefinition[idsInt.length];
		DataColumnDefinition[] rightCols = new DataColumnDefinition[idsInt.length];
		for (int i = 0; i < distances.length; i++) {
			distances[i] = condition.getDistanceFunctions()[idsInt[i]];
		}
		for (int i = 0; i < leftCols.length; i++) {
			leftCols[i] = condition.getLeftJoinColumns()[idsInt[i]];
		}
		for (int i = 0; i < rightCols.length; i++) {
			rightCols[i] = condition.getRightJoinColumns()[idsInt[i]];
		}
		clusteringColumns = new DataColumnDefinition[][] {leftCols, rightCols};
		
		sampledSourceColumns = sampleLeft ? leftCols : rightCols;
		DataSampleInterface sampler = new RandomSampler(sourceB, 10);
		
		List data = new ArrayList();
		DataRow row;
		while ((row = sampler.getNextRow()) != null) {
			data.add(row);
		}
		System.out.println("Sampled " + data.size() + " records.");
		int k = data.size() / 50;
		if (k < 2) k = 2;
		if (k > 20) k = 20;
		if (condition instanceof WeightedJoinCondition) {
			double[] weights = ((WeightedJoinCondition)condition).getWeights();
			clusterer = new KMeansClusterer(k, 4, new DNMDistance(distances, sampledSourceColumns, sampledSourceColumns, weights));
		} else {
			clusterer = new KMeansClusterer(k, 4, new DNMDistance(distances, sampledSourceColumns, sampledSourceColumns));
		}
		
		clusterer.learn(data.toArray());
		
		sourceA.reset();
		sourceB.reset();
		
		clusteringDistances = new ClusteringDistance[2];
		if (condition instanceof WeightedJoinCondition) {
			double[] weights = ((WeightedJoinCondition)condition).getWeights();
			clusteringDistances[0] = new DNMDistance(distances, sampledSourceColumns, leftCols, weights);
			clusteringDistances[1] = new DNMDistance(distances, sampledSourceColumns, rightCols, weights);
		} else {
			clusteringDistances[0] = new DNMDistance(distances, sampledSourceColumns, leftCols);
			clusteringDistances[1] = new DNMDistance(distances, sampledSourceColumns, rightCols);
		}
		hash = new ClusterHashingFunction(clusterer.getCentroids(), clusteringDistances);
		buckets = new BucketManager(hash);
		
		while ((row = getSourceA().getNextRow()) != null) {
			buckets.addToBucketLeftSource(row);
		}
		while ((row = getSourceB().getNextRow()) != null) {
			buckets.addToBucketRightSource(row);
		}
	}
	
	private static AbstractDataSource fixSource(AbstractDataSource source, DataColumnDefinition[] order) throws IOException, RJException {
		return source;
//		if (source.canSort()) {
//			source.setOrderBy(order);
//			return source;
//		} else {
//			ExternallySortingDataSource sorter = new ExternallySortingDataSource(source.getSourceName(), source, order, new HashMap());
//			return sorter;
//		}
	}

	protected DataRow doJoinNext() throws IOException, RJException {
		while (true) {
			
			DataRow[][] activeBucket = getActiveOrNextBucket();
			if (activeBucket == null) {
				return null;
			}
			
			for (; index1 < activeBucket[0].length; index1++) {
				for (; index2 < activeBucket[1].length; index2++) {
					DataRow rowA = activeBucket[0][index1];
					DataRow rowB = activeBucket[1][index2];
					EvaluatedCondition eval;
					if ((eval = getJoinCondition().conditionSatisfied(rowA, rowB)).isSatisfied()) {
						DataRow joined = RowUtils.buildMergedRow(this, rowA, rowB, getOutColumns(), eval);
						if (isAnyJoinListenerRegistered()) {
							notifyJoined(rowA, rowB, joined);
						}
						index2++;
						return joined;
					} else {
						if (isAnyJoinListenerRegistered()) {
							notifyNotJoined(rowA, rowB, eval.getConfidence());
						}
					}
					
					if (isCancelled()) {
						return null;
					}
				}
				index2 = 0;
				if (activeBucket[0][index1].getProperty(PROPERTY_JOINED) != null) {
					notifyTrashingJoined(activeBucket[0][index1]);
				} else if (RowUtils.shouldReportTrashingNotJoined(this, activeBucket[0][index1])) {
					notifyTrashingNotJoined(activeBucket[0][index1]);
				}
			}
			
			for (index2=0; index2 < activeBucket[1].length; index2++) {
				if (activeBucket[1][index2].getProperty(PROPERTY_JOINED) != null) {
					notifyTrashingJoined(activeBucket[1][index2]);
				} else if (RowUtils.shouldReportTrashingNotJoined(this, activeBucket[1][index2])) {
					notifyTrashingNotJoined(activeBucket[1][index2]);
				}
			}
		}
	}
	
	public DataRow[][] getActiveOrNextBucket() throws IOException, RJException {
		if (activeBucket == null || (index1 == activeBucket[0].length)) {
			index1 = 0;
			if (secondLevelActive ) {
				DataRow[][] bucket = readFromManager(secondLevel);
				if (bucket == null) {
					secondLevelActive = false;
				} else {
					activeBucket = bucket;
					return activeBucket;
				}
			}
			DataRow[][] bucket = readFromManager(buckets);
			if (bucket == null) {
				return null;
			}
			System.out.println("Bucket: " + bucket[0].length + " <---> " + bucket[1].length);
			if (bucket[0].length * bucket[1].length > MAX_COMPARISONS) {
				secondLevelActive = true;
				
				//new cluster
				int sourceId = sampleLeft ? 0 : 1;
				//double percent = MAX_SL_COMPARISONS / (double)(bucket[0].length * bucket[1].length);
				//if (percent > 1) percent = 1;
				int k = (int) Math.sqrt((bucket[0].length * bucket[1].length / MAX_SL_COMPARISONS));
				if (k < 2) {
					k = 2;
				}
				if (k > 50) {
					k = 50;
				}
				System.out.println("Second level k=" + k);
				KMeansClusterer clustererSecondLevel  = new KMeansClusterer(k, 4, clusterer.getDistance());
				clustererSecondLevel.learn(bucket[sourceId]);
				
				//second level blocks
				secondLevel = new BucketManager(new ClusterHashingFunction(clustererSecondLevel.getCentroids(), clusteringDistances), false);
				for (int i = 0; i < bucket[0].length; i++) {
					secondLevel.addToBucketLeftSource(bucket[0][i]);
				}
				for (int i = 0; i < bucket[1].length; i++) {
					secondLevel.addToBucketRightSource(bucket[1][i]);
				}
				activeBucket = secondLevel.getBucket();
				System.out.println("Bucket: " + activeBucket[0].length + " <---> " + activeBucket[1].length);
			} else {
				activeBucket = bucket;
			}
			
		}
		return activeBucket;
	}
	
	private DataRow[][] readFromManager(BucketManager manager) throws IOException, RJException {
		DataRow[][] potentialBucket = null;
		do {
			potentialBucket = manager.getBucket();
			if (potentialBucket == null) {
				return null;
			}
		} while (potentialBucket[0].length == 0 || potentialBucket[1].length == 0);
		return potentialBucket;
	}

	protected DataRow[] doJoinNext(int size) throws IOException, RJException {
		List joinResult = new ArrayList();
		DataRow result;
		while (((result = joinNext()) != null) && joinResult.size() < size) {
			joinResult.add(result);
		}
		return (DataRow[]) joinResult.toArray(new DataRow[] {});
	}

	protected void doReset(boolean deep) throws IOException, RJException {
		getSourceA().reset();
		getSourceB().reset();
		closed = false;
	}
	
	protected void doClose() throws IOException, RJException {
		if (!closed) {
			getSourceA().close();
			getSourceB().close();
			closed = true;
		}
	}
	
	public String toString() {
		return "Distance neighbourhood method (under development)";
	}

	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new DNMGUIVisibleComponent();
	}
	
	public String toHTMLString() {
		StringBuilder builder = new StringBuilder();
		builder.append(HTMLUtils.getHTMLHeader());
		builder.append(HTMLUtils.encodeTable(new String[][] {
				{"Search method:", "Clustering search method"}, 
			}));
		builder.append("<br>Clustering configuration:</br>");
		String[][] table = new String[clusteringDistances.length + 1][2];
		table[0][0] = "Attribute (" + getSourceA().getSourceName() + ")";
		table[0][1] = "Attribute (" + getSourceB().getSourceName() + ")";
		for (int i = 1; i < table.length; i++) {
			table[i][0] = clusteringColumns[0][i - 1].getColumnName();
			table[i][1] = clusteringColumns[1][i - 1].getColumnName();
		}
		builder.append(HTMLUtils.encodeTable(table, true));
		builder.append("<br>Attributes mapping and distance function selection:<br>");
		builder.append(HTMLUtils.encodeJoinCondition(getJoinCondition()));
		builder.append("</html>");
		return builder.toString();
	}
	
	public LinkageSummary getLinkageSummary() {
		return null;
	}
}
