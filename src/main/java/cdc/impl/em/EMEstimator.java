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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cdc.components.AbstractJoinCondition;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataRow;
import cdc.impl.distance.EqualFieldsDistance;
import cdc.utils.LogSink;
import cdc.utils.PrintUtils;
import cdc.utils.Props;

public class EMEstimator {
	
	public static final double DEFAULT_EPSILON = Props.getDouble("em-epsilon");
	public static final double DEFAULT_M_i = Props.getDouble("em-mi");
	public static final double DEFAULT_U_i = Props.getDouble("em-ui");
	
	public static class EMIteration {
		public double[] m;
		public double[] u;
		public double p;
	}
	
	public static boolean cancel = false;
	
	static LogSink sink = null;
	
	private AbstractJoinCondition condition;
	
	public void setLogSink(LogSink sink) {
		EMEstimator.sink = sink;
	}
	
	public void log(String line) {
		if (sink != null) {
			sink.log(line);
		}
	} 
	
	public double[] runEMMethodBlocking(DataRow[][] rowsA, DataRow[][] rowsB, AbstractJoinCondition cond, double p) {
		System.out.println("Size: " + rowsA.length + " and " + rowsB.length);
		this.condition = cond;
		double[] m = new double[cond.getLeftJoinColumns().length];
		double[] u = new double[cond.getLeftJoinColumns().length];
		for (int i = 0; i < u.length; i++) {
			m[i] = DEFAULT_M_i;
			u[i] = DEFAULT_U_i;
		}
		EqualFieldsDistance dst = new EqualFieldsDistance();
		List comparisons = new ArrayList();
		for (int n = 0; n < rowsA.length; n++) {
			DataRow[] r1 = rowsA[n];
			DataRow[] r2 = rowsB[n];
			for (int i = 0; i < r1.length; i++) {
				if (cancel) return null;
				for (int j = 0; j < r2.length; j++) {
					boolean b[] = new boolean[cond.getLeftJoinColumns().length];
					for (int k = 0; k < cond.getLeftJoinColumns().length; k++) {
						DataCell cellA = r1[i].getData(cond.getLeftJoinColumns()[k]);
						DataCell cellB = r2[j].getData(cond.getRightJoinColumns()[k]);
						b[k] = dst.distanceSatisfied(cellA, cellB);
						comparisons.add(b);
					}
				}
			}
		}
		boolean[][] input = (boolean[][]) comparisons.toArray(new boolean[][] {});
		if (!checkData(input)) {
			return null;
		}
		sink.log("EM method configured, number of cases: " + input.length);
		return runEMMethod(input, m, u, p, DEFAULT_EPSILON);
	}
	
	private boolean checkData(boolean[][] input) {
		System.out.println("Comps: " + input.length);
		if (input.length < 10) {
			sink.log("Not sufficient data provided to EM method.");
			sink.log("Please use different sampling configuration.");
			sink.log("To do so, please hit cancel and restart the process.");
			return false;
		}
		return true;
	}

	public double[] runEMMethodAllToAll(DataRow[] rowsA, DataRow[] rowsB, AbstractJoinCondition cond, double p) {
		this.condition = cond;
		double[] m = new double[cond.getLeftJoinColumns().length];
		double[] u = new double[cond.getLeftJoinColumns().length];
		for (int i = 0; i < u.length; i++) {
			m[i] = DEFAULT_M_i;
			u[i] = DEFAULT_U_i;
		}
		boolean[][] input = new boolean[rowsA.length * rowsB.length][cond.getLeftJoinColumns().length];
		EqualFieldsDistance dst = new EqualFieldsDistance();
		for (int i = 0; i < rowsA.length; i++) {
			for (int j = 0; j < rowsB.length; j++) {
				if (cancel) return null;
				for (int k = 0; k < cond.getLeftJoinColumns().length; k++) {
					DataCell cellA = rowsA[i].getData(cond.getLeftJoinColumns()[k]);
					DataCell cellB = rowsB[j].getData(cond.getRightJoinColumns()[k]);
					input[rowsB.length * i + j][k] = dst.distanceSatisfied(cellA, cellB);
				}
			}
		}
		if (!checkData(input)) {
			return null;
		}
		return runEMMethod(input, m, u, p, DEFAULT_EPSILON);
	}
	
	public double[] runEMMethod(boolean[][] input, double[] m, double[] u, double p, double epsilon) {
		double error = Double.MAX_VALUE;
		double oldError = 0;
		int step = 0;
		EMIteration iter = new EMIteration();
		iter.m = m;
		iter.u = u;
		iter.p = p;
		DateFormat format = new SimpleDateFormat("HH:MM:ss");
		do {
			if (cancel) return null;
			step++;
			log(format.format(new Date()) + ": Iteration " + step + " starts.");
			EMIteration old = iter;
			iter = iterate(input, iter.m, iter.u, iter.p);
			if (iter == null) {
				return null;
			}
			oldError = error;
			error = calculateError(old, iter);
			System.out.println("Current error: " + error);
			log(format.format(new Date()) + ": Iteration " + step + " finished.");
			logWeights(iter);
		} while (error > epsilon || error > oldError);
		log(format.format(new Date()) + ": EM finished after " + step + " iterations.");
		log(format.format(new Date()) + ": Final weights:");
		logWeights(iter);
		return weights(iter.m, iter.u);
	}
	
	private void logWeights(EMIteration iter) {
		int[] weights = weightsTo0_100(weights(iter.m, iter.u));
		for (int i = 0; i < condition.getLeftJoinColumns().length; i++) {
			log("Attributes: [" + condition.getLeftJoinColumns()[i] + "] and [" + 
					condition.getRightJoinColumns()[i] + "] -> " + weights[i]);
		}
	}

	public double calculateError(EMIteration old, EMIteration iter) {
		double error = 0;
		for (int i = 0; i < old.m.length; i++) {
			error += Math.abs(old.m[i] - iter.m[i]) + Math.abs(old.u[i] - iter.u[i]);
		}
		return error;
	}

	public EMIteration iterate(boolean[][] input, double[] m, double[] u, double p) {
		
		//E phase for probabilities m_i
		double[] g_m = computeGM(input, m, u, p);
		if (cancel) return null;
		
		//M phase for probabilities m_i
		double[] new_m = computeNewProbabilities(input, g_m, m.length);
		if (cancel) return null;
		
		//E phase for probabilities u_i
		double[] g_u = computeGU(input, m, u, p);
		if (cancel) return null;
		
		//M phase for probabilities u_i
		double[] new_u = computeNewProbabilities(input, g_u, m.length);
		
		double new_p = computeP(g_m);
		
		EMIteration em = new EMIteration();
		em.m = new_m;
		em.u = new_u;
		em.p = new_p;	
		return em;
	}
	
	private double[] computeGM(boolean[][] input, double[] m, double[] u, double p) {
		double[] g = new double[input.length];
		for (int i = 0; i < g.length; i++) {
			double numerator = p;
			double denominator1 = p;
			double denominator2 = 1-p;
			for (int j = 0; j < m.length; j++) {
				numerator *= quickPow(m[j], input[i][j]) * quickPow(1-m[j], !input[i][j]);
				denominator1 *= quickPow(m[j], input[i][j]) * quickPow(1-m[j], !input[i][j]);
				denominator2 *= quickPow(u[j], input[i][j]) * quickPow(1-u[j], !input[i][j]);
			}
			g[i] = numerator / (denominator1 + denominator2);
		}
		return g;
	}
	
	private double[] computeGU(boolean[][] input, double[] m, double[] u, double p) {
		double[] g = new double[input.length];
		for (int i = 0; i < g.length; i++) {
			double numerator = 1-p;
			double denominator1 = p;
			double denominator2 = 1-p;
			for (int j = 0; j < m.length; j++) {
				numerator *= quickPow(u[j], input[i][j]) * quickPow(1-u[j], !input[i][j]);
				denominator1 *= quickPow(m[j], input[i][j]) * quickPow(1-m[j], !input[i][j]);
				denominator2 *= quickPow(u[j], input[i][j]) * quickPow(1-u[j], !input[i][j]);
			}
			g[i] = numerator / (denominator1 + denominator2);
		}
		return g;
	}
	
	private double quickPow(double d, boolean b) {
		return b ? d : 1;
	}

	private double[] computeNewProbabilities(boolean[][] input, double[] g, int k) {
		double[] p = new double[k];
		for (int i = 0; i < p.length; i++) {
			double numerator = 0;
			double denominator = 0;
			for (int j = 0; j < g.length; j++) {
				numerator += g[j] * asInt(input[j][i]);
				denominator += g[j];
			}
			p[i] = numerator / denominator;
		}
		return p;
	}

	private double asInt(boolean b) {
		return b ? 1 : 0;
	}
	
	private double computeP(double[] g) {
		double sum = 0;
		for (int i = 0; i < g.length; i++) {
			sum += g[i];
		}
		return sum / (double)g.length;
	}
	
	public double[] weights(double[] m, double[] u) {
		double[] w = new double[m.length];
		for (int i = 0; i < w.length; i++) {
			if (u[i] == 0) {
				w[i] = 0;
			} else {
				w[i] = Math.log(m[i]/u[i]) / Math.log(2);
			}
		}
		System.out.println("Weights: " + PrintUtils.printArray(w));
		return w;
	}
	
	public int[] weightsTo0_100(double[] weights) {
		if (weights == null) return null;
		double adjusted[] = new double[weights.length];
		double sum = 0;
		for (int i = 0; i < adjusted.length; i++) {
			if (weights[i] < 0) {
				adjusted[i] = 0;
			} else {
				adjusted[i] = weights[i];
			}
			sum += adjusted[i];
		}
		int[] w0100 = new int[adjusted.length];
		for (int i = 0; i < w0100.length; i++) {
			w0100[i] = (int)Math.round((adjusted[i] / sum) * 100);
		}
		int mySum = 0;
		int max = -1;
		for (int i = 0; i < w0100.length; i++) {
			mySum += w0100[i];
			if (max == -1 || w0100[max] < w0100[i]) {
				max = i;
			}
		}
		if (mySum < 100) {
			w0100[max] += 100 - mySum;
		} else if (mySum > 100) {
			w0100[max] -= mySum - 100;
		}
		return w0100;
	}
	
	public static void main(String[] args) {
		double[] m = new double[] {0.9, 0.9, 0.9, 0.9};
		double[] u = new double[] {0.1, 0.1, 0.1, 0.1};
		boolean[][] input = new boolean[][] 
		  {
				{true, true, true, true},
				{false, true, true, true},
				{false, false, true, false},
				{true, true, false, true},
				{true, false, true, false},
				{false, true, false, false},
				{false, false, false, false},
				{false, false, true, true},
				{true, true, true, true},
				{false, true, false, false},
				{true, true, false, true},
				{true, true, false, true},
				{false, false, false, false},
		  };
		double p = 0.02;
		
		EMEstimator estimator = new EMEstimator();
		EMIteration iter = new EMIteration();
		iter.m = m;
		iter.u = u;
		iter.p = p;
//		summary(iter);
//		for (int i = 0; i < 100; i++) {
//			iter = estimator.iterate(input, iter.m, iter.u, iter.p);
//			System.out.println("Iteration number " + i);
//			summary(iter);
//		}
//		
//		double[] weights = estimator.weights(iter.m, iter.u);
		
		double weights[] = estimator.runEMMethod(input, m, u, p, 0);
		
		System.out.println("Weights: " + PrintUtils.printArray(weights));
	}

	
//	private static void summary(EMIteration iter) {
//		System.out.println("M: " + PrintUtils.printArray(iter.m));
//		System.out.println("U: " + PrintUtils.printArray(iter.u));
//		System.out.println("P: " + iter.p);
//	}
}
