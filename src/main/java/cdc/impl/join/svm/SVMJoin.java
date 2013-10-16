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


package cdc.impl.join.svm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.components.AbstractJoin;
import cdc.components.AbstractJoinCondition;
import cdc.components.EvaluatedCondition;
import cdc.components.LinkageSummary;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.gui.GUIVisibleComponent;
import cdc.impl.join.blocking.BlockingFunctionFactory;
import cdc.impl.join.blocking.BlockingJoin;
import cdc.impl.join.blocking.BucketManager;
import cdc.impl.join.blocking.EqualityBlockingFunction;
import cdc.impl.join.blocking.BlockingFunction;
import cdc.impl.join.blocking.SoundexBlockingFunction;
import cdc.utils.PrintUtils;
import cdc.utils.RJException;
import cdc.utils.RowUtils;

public class SVMJoin extends AbstractJoin {

	public static final String ATTR_BLOCKING_FUNCTION = "blocking-function";
	public static final String ATTR_BLOCKING_ATTR = "blocking-attr";
	public static final String ATTR_LEARNING_ROUNDS = "learning-rounds-number";
	public static final String ATTR_YM = "ym-incremental";
	public static final String ATTR_YN = "yn-incremental";
	public static final String ATTR_WM = "wm";
	public static final String ATTR_WN = "wn";
	public static final String ATTR_MATCHING_MARGIN = "matching-margin";
	public static final String ATTR_NON_MATCHING_MARGIN = "non-matching-margin";
	public static final String ATTR_TRAINING_SELECTION_METHOD = "training-selection-method";
	
	public static final String[] SELECTION_METHODS = {"treshold", "nearest"};
	
	private double MATCHING_MARGIN = 0.85;
	private double NON_MATCHING_MARGIN = 0.3;
	private int STARTUP_MATCHING_SIZE = 100;
	private int STARTUP_NON_MATCHING_SIZE = 1000;
	private int YM_SIZE = 100;
	private int YN_SIZE = 10 * YM_SIZE;
	private int MAX_MAIN_LEARNING_ROUNDS = 3;
	private int SELECTION_METHOD = 0;
	
	private BucketManager buckets;
	private BlockingFunction function;
	private int[] blockingFactor;
	private DataColumnDefinition[][] blocks;
	
	private List list;
	private List classes;
	private List bufferedMatches;
	private int wmSize = 0;
	private int wnSize = 0;
	private int learnignRound = 1;
	private svm_model supportVectorMachine;
	
	private SortedCircularBuffer Xm;
	private SortedCircularBuffer Yn;
	
	private Map comparedRecords = new HashMap();
	private Map matchedMap = new HashMap();
	private double maxIndex = 0;
	private DataRow[][] bucket;
	private int index0;
	private int index1;
	private boolean firstStep = false;
	//private boolean fullAgreementAdded = false;
	private boolean initialized = false;
	
	private int readA = 0;
	private int readB = 0;
	
	public SVMJoin(AbstractDataSource sourceA, AbstractDataSource sourceB, DataColumnDefinition[] outColumns, AbstractJoinCondition condition, Map params) throws RJException {
		super(sourceA, sourceB, condition, outColumns, params);
		
		int paramId = Integer.parseInt(getProperty(BlockingJoin.BLOCKING_PARAM));
		String function = getProperty(BlockingJoin.BLOCKING_FUNCTION);
		
		blockingFactor = new int[] {paramId};
		blocks = new DataColumnDefinition[this.blockingFactor.length][2];
		for (int i = 0; i < blocks.length; i++) {
			blocks[i][0] = condition.getLeftJoinColumns()[this.blockingFactor[i]];
			blocks[i][1] = condition.getRightJoinColumns()[this.blockingFactor[i]];
		}
		
		if (getProperty(ATTR_LEARNING_ROUNDS) != null) {
			MAX_MAIN_LEARNING_ROUNDS = Integer.parseInt(getProperty(ATTR_LEARNING_ROUNDS));
		}
		if (getProperty(ATTR_MATCHING_MARGIN) != null) {
			MATCHING_MARGIN = Double.parseDouble(getProperty(ATTR_MATCHING_MARGIN));
		}
		if (getProperty(ATTR_NON_MATCHING_MARGIN) != null) {
			NON_MATCHING_MARGIN = Double.parseDouble(getProperty(ATTR_NON_MATCHING_MARGIN));
		}
		if (getProperty(ATTR_WM) != null) {
			STARTUP_MATCHING_SIZE = Integer.parseInt(getProperty(ATTR_WM));
		}
		if (getProperty(ATTR_WN) != null) {
			STARTUP_NON_MATCHING_SIZE = Integer.parseInt(getProperty(ATTR_WN));
		}
		if (getProperty(ATTR_YM) != null) {
			YM_SIZE = Integer.parseInt(getProperty(ATTR_YM));
		}
		if (getProperty(ATTR_YN) != null) {
			YN_SIZE = Integer.parseInt(getProperty(ATTR_YN));
		}
		if (getProperty(ATTR_TRAINING_SELECTION_METHOD) != null) {
			String selectionMethod = getProperty(ATTR_TRAINING_SELECTION_METHOD);
			if (selectionMethod.equals(SELECTION_METHODS[0])) {
				SELECTION_METHOD = 0;
			} else if (selectionMethod.equals(SELECTION_METHODS[1])) {
				SELECTION_METHOD = 1;
			} else {
				throw new RuntimeException(ATTR_TRAINING_SELECTION_METHOD + " can have only values " + PrintUtils.printArray(SELECTION_METHODS));
			}
		}
		
		if (function.startsWith(BlockingFunctionFactory.SOUNDEX)) {
			String paramsStr = function.substring(function.indexOf("(") + 1, function.length()-1);
			this.function = new SoundexBlockingFunction(blocks, Integer.parseInt(paramsStr));
		} else if (function.startsWith(BlockingFunctionFactory.EQUALITY)) {
			this.function = new EqualityBlockingFunction(blocks);
		} else {
			throw new RuntimeException("Property " + BlockingJoin.BLOCKING_FUNCTION + " accepts only soundex or equality options.");
		}
	}

	protected void doClose() throws IOException, RJException {
		if (buckets != null) {
			buckets.cleanup();
			buckets = null;
		}
		getSourceA().close();
		getSourceB().close();
	}

	protected DataRow[] doJoinNext(int size) throws IOException, RJException {
		return null;
	}

	protected DataRow doJoinNext() throws IOException, RJException {
		if (!initialized) {
			if (buckets == null) {
				initialize();
			}
			initialized = true;
			
			//This method generates first sample of the data. Although Christen suggests that values that are completely matching
			//or completely not matching should not be considered, I am adding values being complete matches. This is to improve the
			//classification results. If this generates some sort of bias (not sure about that though), this should be removed.
			//The generation of sample data makes sure that there are some not fully matching cases to generate potentially good
			//SVM classifier.
			if (SELECTION_METHOD == 0) {
				generateSampleSVMDataThreshold();
			} else {
				generateSampleSVMDataNearest();
			}
			if (wmSize == 0 || wnSize == 0) {
				throw new RJException("There was not enough data to build initial seed training examples");
			}
			buildSVM();
			buckets.reset();
			
			firstStep = true;
		}
		
		//First, output all 1s - fully matched records
		if (firstStep) {
			DataRow row = nextFullMatch();
			if (row == null) {
				firstStep = false;
				//buckets.reset();
			} else {
				return row;
			}
		}
		
		if (bufferedMatches.isEmpty()) {
			//this is the implementation of the second phase of the algorithm proposed by Christen
			//in Automatic Record Linkage using Seeded Nearest Neighbour and Support Vector Machine Classification.
			if (learnignRound < MAX_MAIN_LEARNING_ROUNDS) {
				//We can still improve the svm by adding cases to the svm.
				learnignRound++;
				System.out.println("Learnign round " + learnignRound);
				doLearningRound();
				buckets.reset();
			}
		}
		
		if (!bufferedMatches.isEmpty()) {
			//we have some buffered data from the first phase (initialization phase).
			DataRow match = (DataRow) bufferedMatches.remove(0);
			return match;
		} else {
			//We will just output everything according to the current svm decision boundry.
			//We need to start reading buckets from the beginning, eliminate cases that have already been
			//tested, and simply return all the cases that are classified to matching records.
			//This code can also be executed if learning does not provide new examples.
			System.out.println("Second phase of the algorithm begins.");
			DataRow nextMatch = nextMatch();
			return nextMatch;
		}
		
	}
	
	private DataRow nextFullMatch() throws IOException, RJException {
		
		AbstractDistance[] distances = getJoinCondition().getDistanceFunctions();
		DataColumnDefinition[] left = getJoinCondition().getLeftJoinColumns();
		DataColumnDefinition[] right = getJoinCondition().getRightJoinColumns();
		
		while (true) {
			while (bucket == null || bucket[0].length == 0 || bucket[1].length == 0) {
				index0 = index1 = 0;
				bucket = buckets.getBucket();
				if (bucket == null) {
					return null;
				}
			}
			for (; index0 < bucket[0].length; index0++) {
				loop: for (; index1 < bucket[1].length; index1++) {
					if (!checkCompared(bucket[0][index0], bucket[1][index1])) {
						for (int k = 0; k < distances.length; k++) {
							double dst = distances[k].distance(bucket[0][index0].getData(left[k]), bucket[1][index1].getData(right[k])) / (double)100;
							if (dst != 1) {
								continue loop;
							}
						}
						DataRow outRow = RowUtils.buildMergedRow(this, bucket[0][index0], bucket[1][index1], getOutColumns(), new EvaluatedCondition(true, false, 100));
						index1++;
						return outRow;
					}
				}
			}
			bucket = null;
		}
	}

	private DataRow nextMatch() throws IOException, RJException {
		
		AbstractDistance[] distances = getJoinCondition().getDistanceFunctions();
		DataColumnDefinition[] left = getJoinCondition().getLeftJoinColumns();
		DataColumnDefinition[] right = getJoinCondition().getRightJoinColumns();
		
		while (true) {
			while (bucket == null || bucket[0].length == 0 || bucket[1].length == 0) {
				index0 = index1 = 0;
				bucket = buckets.getBucket();
				if (bucket == null) {
					return null;
				}
			}
			for (; index0 < bucket[0].length; index0++) {
				for (; index1 < bucket[1].length; index1++) {
					if (!checkCompared(bucket[0][index0], bucket[1][index1])) {
						svm_node[] vector = new svm_node[distances.length];
						boolean someNot1 = false;
						for (int k = 0; k < vector.length; k++) {
							vector[k] = new svm_node();
							vector[k].index = k;
							vector[k].value = distances[k].distance(bucket[0][index0].getData(left[k]), bucket[1][index1].getData(right[k])) / (double)100;
							if (vector[k].value != 1) {
								someNot1 = true;
							}
						}
						if (!someNot1) {
							bucket[0][index0].setProperty(PROPERTY_JOINED, "true");
							bucket[1][index1].setProperty(PROPERTY_JOINED, "true");
							continue;
						}
						Boolean b = (Boolean) this.matchedMap.get(bucket[0][index0]);
						bucket[0][index0].setProperty(PROPERTY_JOINED, b == null || !b.booleanValue() ? null : "true");
						b = (Boolean) this.matchedMap.get(bucket[1][index1]);
						bucket[1][index1].setProperty(PROPERTY_JOINED, b == null || !b.booleanValue() ? null : "true");
						int[] labels = new int[2];
						svm.svm_get_labels(supportVectorMachine, labels);
						double[] probabilities = new double[2];
						int cls = (int)svm.svm_predict_probability(supportVectorMachine, vector, probabilities);
						int clsId;
						if (labels[0] == cls) {
							clsId = 0;
						} else {
							clsId = 1;
						}
						if (cls == 1) {
							//match
							//System.out.println(probabilities[0] + "  " + probabilities[1] + "  " + cls + " clsId=" + clsId);
							DataRow outRow = RowUtils.buildMergedRow(this, bucket[0][index0], bucket[1][index1], getOutColumns(), new EvaluatedCondition(true, false, (int)Math.round(probabilities[clsId] * 100)));
							//outRow.setProperty(PROPERTY_CONFIDNCE, String.valueOf(Math.round(probabilities[clsId] * 100)));
							index1++;
							return outRow;
						}
					}
				}
				if (bucket[0][index0].getProperty(PROPERTY_JOINED) != null) {
					notifyTrashingJoined(bucket[0][index0]);
				} else {
					if (RowUtils.shouldReportTrashingNotJoined(this, bucket[0][index0])) {
						notifyTrashingNotJoined(bucket[0][index0]);
					}
				}
			}
			for (int i = 0; i < bucket[1].length; i++) {
				if (bucket[1][i].getProperty(PROPERTY_JOINED) != null) {
					notifyTrashingJoined(bucket[1][i]);
				} else {
					if (RowUtils.shouldReportTrashingNotJoined(this, bucket[1][i])) {
						notifyTrashingNotJoined(bucket[1][i]);
					}
				}
			}
			bucket = null;
		}
	}

	private void doLearningRound() throws IOException, RJException {
		//The learning round starts reading buckets from the beginning, and ignores cases that have already been
		//tested. It uses svm from the previous round, and builds new one at the end.
		Xm = new SortedCircularBuffer(YM_SIZE, SortedCircularBuffer.REMOVE_POLICY_REMOVE_SMALLEST);
		Yn = new SortedCircularBuffer(YN_SIZE, SortedCircularBuffer.REMOVE_POLICY_REMOVE_SMALLEST);
		
		AbstractDistance[] distances = getJoinCondition().getDistanceFunctions();
		DataColumnDefinition[] left = getJoinCondition().getLeftJoinColumns();
		DataColumnDefinition[] right = getJoinCondition().getRightJoinColumns();
		DataRow[][] bucket;
		while ((bucket = buckets.getBucket()) != null) {
			for (int i = 0; i < bucket[0].length; i++) {
				for (int j = 0; j < bucket[1].length; j++) {
					if (!checkCompared(bucket[0][i], bucket[1][j])) {
						//Only consider cases that have not been tested - these cases are going to the svm
						svm_node[] vector = new svm_node[distances.length];
						boolean someNot1 = false;
						for (int k = 0; k < vector.length; k++) {
							vector[k] = new svm_node();
							vector[k].index = k;
							vector[k].value = distances[k].distance(bucket[0][i].getData(left[k]), bucket[1][j].getData(right[k])) / (double)100;
							if (vector[k].value != 1) {
								someNot1 = true;
							}
						}
						if (!someNot1) {
							continue;
						}
						//classify vector
						double[] probabilities = new double[2];
						int[] labels = new int[2];
						svm.svm_get_labels(supportVectorMachine, labels);
						int cls = (int)svm.svm_predict_probability(supportVectorMachine, vector, probabilities);
						int clsId;
						if (labels[0] == cls) {
							clsId = 0;
						} else {
							clsId = 1;
						}
						Double val = new Double(probabilities[clsId]);
						if (cls == 1) {
							//Record classified as match
							//System.out.println("Cls: " + cls + " clsId=" + clsId + "   " + probabilities[0] + " -- " + probabilities[1]);
							Xm.add(val, new Object[] {new DataRow[] {bucket[0][i], bucket[1][j]}, vector, val});
						} else {
							//Records classified as not match
							Yn.add(val, new Object[] {new DataRow[] {bucket[0][i], bucket[1][j]}, vector, val});
						}
					}
				}
			}
		}
		
		System.out.println("Circular buffer Xm size: " + Xm.size());
		System.out.println("Circular buffer Yn size: " + Yn.size());
		
		//now Xm and Yn have the best candidates for classification of matches and non-matches
		for (Iterator iterator = Xm.getOrderedValuesIterator(); iterator.hasNext();) {
			Object[] entry = (Object[]) iterator.next();
			DataRow[] match = (DataRow[]) entry[0];
			DataRow outRow = RowUtils.buildMergedRow(this, match[0], match[1], getOutColumns(), new EvaluatedCondition(true, false, (int)Math.round(((Double)entry[2]).doubleValue() * 100)));
			//outRow.setProperty(PROPERTY_CONFIDNCE, String.valueOf(Math.round(((Double)entry[2]).doubleValue() * 100)));
			markComparedRecords(match[0], match[1], true);
			bufferedMatches.add(outRow);
			list.add(entry[1]);
			classes.add(new Double(1));
		}
		for (Iterator iterator = Yn.getOrderedValuesIterator(); iterator.hasNext();) {
			Object[] entry = (Object[]) iterator.next();
			DataRow[] match = (DataRow[]) entry[0];
			markComparedRecords(match[0], match[1], false);
			list.add(entry[1]);
			classes.add(new Double(0));
		}
		
		//now is a time for the new svm learning
		buildSVM();
	}

	private void buildSVM() throws RJException {
		svm_problem problem = new svm_problem();
		//The problem definition should be reworked - we do not want to build everything from scratch.
		//Also there should be a limit of number of cases supported in SVM.
		svm_node[][] nodes = (svm_node[][]) list.toArray(new svm_node[][] {});
		Double[] cls = (Double[])classes.toArray(new Double[] {});
		double[] clsDbl = new double[cls.length];
		for (int i = 0; i < clsDbl.length; i++) {
			clsDbl[i] = cls[i].doubleValue();
		}
		
		problem.x = nodes;
		problem.y = clsDbl;
		problem.l = nodes.length;
		svm_parameter param = new svm_parameter();
		param.svm_type = svm_parameter.C_SVC;
		param.kernel_type = svm_parameter.RBF;
		param.degree = 3;
		param.gamma = 0;	// 1/k
		param.coef0 = 0;
		param.nu = 0.5;
		param.cache_size = 100;
		param.C = 1;
		param.eps = 1e-3;
		param.p = 0.1;
		param.shrinking = 1;
		param.probability = 1;
		param.nr_weight = 0;
		param.weight_label = new int[0];
		param.weight = new double[0];
		
		if(param.gamma == 0)
			param.gamma = 1.0 / maxIndex;
		
		String err = svm.svm_check_parameter(problem, param);

		if(err != null) {
			throw new RJException("Error using libsvm library: " + err);
		}
		
		supportVectorMachine = svm.svm_train(problem, param);
	}

	private void generateSampleSVMDataThreshold() throws IOException, RJException {
		System.out.println("Generating SVM data. Using threshold-based approach.");
		list = new ArrayList();
		classes = new ArrayList();
		bufferedMatches = new ArrayList();
		int someNot1Size = 0;
		//This should be changed. We should not consider fully matching or non-matching records
		//for the initial seed training examples.
		while (someNot1Size < STARTUP_MATCHING_SIZE || wnSize < STARTUP_NON_MATCHING_SIZE) {
			DataRow[][] bucket = buckets.getBucket();
			if (bucket == null) {
				//we have drained the source...
				break;
			}
			if (bucket[0].length == 0 || bucket[1].length == 0) {
				//this will not reult in any good wm/wn candidate
				continue;
			}
			AbstractDistance[] distances = getJoinCondition().getDistanceFunctions();
			DataColumnDefinition[] left = getJoinCondition().getLeftJoinColumns();
			DataColumnDefinition[] right = getJoinCondition().getRightJoinColumns();
			for (int i = 0; i < bucket[0].length; i++) {
				for (int j = 0; j < bucket[1].length; j++) {
					svm_node[] vector = new svm_node[distances.length];
					boolean wn = true;
					boolean wm = true;
					boolean someNot1 = false;
					boolean someNot0 = false;
					for (int k = 0; k < vector.length; k++) {
						vector[k] = new svm_node();
						vector[k].index = k;
						vector[k].value = distances[k].distance(bucket[0][i].getData(left[k]), bucket[1][j].getData(right[k])) / (double)100;
						if (maxIndex < k) {
							maxIndex = k;
						}
						if (vector[k].value < MATCHING_MARGIN) {
							wm = false;
						}
						if (vector[k].value > NON_MATCHING_MARGIN) {
							wn = false;
						}
						if (vector[k].value != 0) {
							someNot0 = true;
						}
						if (vector[k].value != 1) {
							someNot1 = true;
						}
						if (!wn && !wm) {
							break;
						}
					}
					if (wm && someNot1) {
						//Candidate for Wm
						wmSize++;
						if (someNot1) {
							someNot1Size++;
						}
						classes.add(new Double(1));
						list.add(vector);
						//this is a match - we should return this result.
						DataRow outRow = RowUtils.buildMergedRow(this, bucket[0][i], bucket[1][j], getOutColumns(), new EvaluatedCondition(true, false, 100));
						//TODO: Should this really be 100% confidence?
						//outRow.setProperty(PROPERTY_CONFIDNCE, "100*");
						bufferedMatches.add(outRow);
						markComparedRecords(bucket[0][i], bucket[1][j], true);
						//writeVector(vector, "1");
					} else if (wn && someNot0) {
						//Candidate for Wn
						wnSize++;
						classes.add(new Double(0));
						list.add(vector);
						markComparedRecords(bucket[0][i], bucket[1][j], false);
						//writeVector(vector, "0");
					}
				}
			}
		}
		System.out.println("SVM training data found.");
		System.out.println("size(Wm)=" + wmSize + " (not-fully-matched: " + someNot1Size + ")");
		System.out.println("size(Wn)=" + wnSize);
	}
	
	private void generateSampleSVMDataNearest() throws IOException, RJException {
		System.out.println("Generating SVM data. Using nearest-based approach.");
		list = new ArrayList();
		classes = new ArrayList();
		bufferedMatches = new ArrayList();
		
		SortedCircularBuffer bufferMatch = new SortedCircularBuffer(STARTUP_MATCHING_SIZE, SortedCircularBuffer.REMOVE_POLICY_REMOVE_LARGEST);
		SortedCircularBuffer bufferNonMatch = new SortedCircularBuffer(STARTUP_NON_MATCHING_SIZE, SortedCircularBuffer.REMOVE_POLICY_REMOVE_SMALLEST);
		
		AbstractDistance[] distances = getJoinCondition().getDistanceFunctions();
		DataColumnDefinition[] left = getJoinCondition().getLeftJoinColumns();
		DataColumnDefinition[] right = getJoinCondition().getRightJoinColumns();
		
		while (true) {
			DataRow[][] bucket = buckets.getBucket();
			if (bucket == null) {
				//we have drained the source...
				break;
			}
			if (bucket[0].length == 0 || bucket[1].length == 0) {
				//this will not reult in any good wm/wn candidate
				continue;
			}
			for (int i = 0; i < bucket[0].length; i++) {
				for (int j = 0; j < bucket[1].length; j++) {
					double totalDistanceMatch = 0;
					for (int k = 0; k < distances.length; k++) {
						double dst = distances[k].distance(bucket[0][i].getData(left[k]), bucket[1][j].getData(right[k])) / (double)100;
						totalDistanceMatch += Math.pow(dst - 1, 2);
					}
					if (totalDistanceMatch != 0 && totalDistanceMatch < 0.5 * distances.length) {
						bufferMatch.add(new Double(totalDistanceMatch), new DataRow[] {bucket[0][i], bucket[1][j]});
					} else if (totalDistanceMatch != distances.length && totalDistanceMatch > 0.5 * distances.length) {
						bufferNonMatch.add(new Double(totalDistanceMatch), new DataRow[] {bucket[0][i], bucket[1][j]});
					}
				}
			}
		}
		
		for (Iterator iterator = bufferMatch.getOrderedValuesIterator(); iterator.hasNext();) {
			DataRow[] row = (DataRow[]) iterator.next();
			svm_node[] vector = new svm_node[distances.length];
			for (int k = 0; k < vector.length; k++) {
				vector[k] = new svm_node();
				vector[k].index = k;
				vector[k].value = distances[k].distance(row[0].getData(left[k]), row[1].getData(right[k])) / (double)100;
				if (maxIndex < k) {
					maxIndex = k;
				}
			}
			wmSize++;
			classes.add(new Double(1));
			list.add(vector);
			//this is a match - we should return this result.
			DataRow outRow = RowUtils.buildMergedRow(this, row[0], row[1], getOutColumns(), new EvaluatedCondition(true, false, 100));
			//outRow.setProperty(PROPERTY_CONFIDNCE, "100*");
			bufferedMatches.add(outRow);
			markComparedRecords(row[0], row[1], true);
		}
		
		for (Iterator iterator = bufferNonMatch.getOrderedValuesIterator(); iterator.hasNext();) {
			DataRow[] row = (DataRow[]) iterator.next();
			svm_node[] vector = new svm_node[distances.length];
			for (int k = 0; k < vector.length; k++) {
				vector[k] = new svm_node();
				vector[k].index = k;
				vector[k].value = distances[k].distance(row[0].getData(left[k]), row[1].getData(right[k])) / (double)100;
			}
			wnSize++;
			classes.add(new Double(0));
			list.add(vector);
			markComparedRecords(row[0], row[1], false);
		}
		
		System.out.println("SVM training data found.");
		System.out.println("size(Wm)=" + wmSize);
		System.out.println("size(Wn)=" + wnSize);
	}

//	private void writeVector(svm_node[] vector, String string) {
//		System.out.print(string + " ");
//		for (int i = 0; i < vector.length; i++) {
//			System.out.print(i + ":" + vector[i].value + " ");
//		}
//		System.out.println();
//	}

	private void markComparedRecords(DataRow row1, DataRow row2, boolean matched) {
		Map level2 = (Map) comparedRecords.get(row1);
		if (level2 == null) {
			level2 = new HashMap();
			comparedRecords.put(row1, level2);
		}
		level2.put(row2, new Boolean(matched));
		if (matched) {
			matchedMap.put(row1, new Boolean(matched));
			matchedMap.put(row2, new Boolean(matched));
		}
	}
	
	private boolean checkCompared(DataRow row1, DataRow row2) {
		Map level2 = (Map) comparedRecords.get(row1);
		if (level2 == null) {
			return false;
		}
		Boolean comp = (Boolean) level2.get(row2);
		return comp != null;
	}

	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new SVMGuiVisibleComponent();
	}
	private void initialize() throws IOException, RJException {
		buckets = new BucketManager(function);
		DataRow row;
		while ((row = getSourceA().getNextRow()) != null) {
			buckets.addToBucketLeftSource(row);
			readA++;
		}
		while ((row = getSourceB().getNextRow()) != null) {
			buckets.addToBucketRightSource(row);
			readB++;
		}
		buckets.addingCompleted();
	}

	protected void doReset(boolean deep) throws IOException, RJException {
		if (deep) {
			buckets.cleanup();
			buckets = null;
			initialized = false;
		} else {
			if (buckets != null) {
				buckets.reset();
			}
		}
		this.matchedMap.clear();
		this.comparedRecords.clear();
		this.initialized = false;
		this.learnignRound = 0;
		
		readA = 0;
		readB = 0;
		
		getSourceA().reset();
		getSourceB().reset();
	}

	public LinkageSummary getLinkageSummary() {
		return new LinkageSummary(readA, readB, getLinkedCnt());
	}
	
}
