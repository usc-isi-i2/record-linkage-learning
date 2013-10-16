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

import cdc.components.AbstractDistance;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.utils.PrintUtils;
import cdc.utils.RJException;

public class DNMDistance implements ClusteringDistance {
	
	private double[] weights;
	private AbstractDistance[] functions;
	private DataColumnDefinition[] colsCentr;
	private DataColumnDefinition[] colsRows;
	
	public DNMDistance(AbstractDistance[] distance, DataColumnDefinition[] columnsCentroids, DataColumnDefinition[] columnsRows) throws RJException {
		this(distance, columnsCentroids, columnsRows, getDefaultWeights(columnsRows.length));
	}

	public DNMDistance(AbstractDistance[] distance, DataColumnDefinition[] columnsCentroids, DataColumnDefinition[] columnsRows, double[] weights) throws RJException {
		this.functions = new AbstractDistance[distance.length];
		for (int i = 0; i < distance.length; i++) {
			this.functions[i] = ClusteringFunctionFactory.convertDistanceFunction(distance[i]);
		}
		colsCentr = columnsCentroids;
		colsRows = columnsRows;
		this.weights = (double[]) weights.clone();
		fixWeights(this.weights);
		System.out.println("DNM distance, weights: " + PrintUtils.printArray(this.weights));
	}
	
	private static void fixWeights(double[] w) {
		for (int i = 0; i < w.length; i++) {
			w[i] = 1 - w[i];
		}
	}
 	
	private static double[] getDefaultWeights(int length) {
		double w[] = new double[length];
		for (int i = 0; i < w.length; i++) {
			w[i] = 0;
		}
		return w;
	}
	
	public Object centroid(Object[] instances) {
		//String[] items = (String[])instances;
		if (instances.length == 0) {
			return null;
		}
		int bestId = -1;
		double sum = Integer.MAX_VALUE;
		for (int i = 0; i < instances.length; i++) {
			double localSum = 0;
			for (int j = 0; j < instances.length; j++) {
				if (i != j) {
					localSum += distance(instances[i], instances[j], colsCentr, colsCentr);
				}
			}
			if (localSum < sum) {
				sum = localSum;
				bestId = i;
			}
		}
		return instances[bestId];
	}

	public double distance(Object o1, Object o2) {
		return distance(o1, o2, colsCentr, colsRows);
	}
	
	private double distance(Object a, Object b, DataColumnDefinition[] colsA, DataColumnDefinition[] colsB) {
		DataRow rowO1 = (DataRow)a;
		DataRow rowO2 = (DataRow)b;
		double sumSquares = 0;
		for (int i = 0; i < functions.length; i++) {
			sumSquares += Math.pow((100 - this.functions[i].distance(rowO1.getData(colsA[i]), rowO2.getData(colsB[i]))) * weights[i], 1);
		}
		return Math.sqrt(sumSquares);
	}

	public double getMaxDistance() {
		double sum = 0;
		for (int i = 0; i < weights.length - 2; i++) {
			sum += weights[i] * 100;
		}
		return sum;
	}
	
//	public static void main(String[] args) throws IOException {
//		System.out.println("Testing StringDistance and clustering");
//		CSVReader reader = new CSVReader(new FileReader("data-sample\\testdata.csv"), ';');
//		String[] row = reader.readNext();
//		List items = new ArrayList();
//		while ((row = reader.readNext()) != null) {
//			String[] s = row[12].split(" ");
//			items.add(new DataCell[] {new DataCell("w1", -1, s[0]), new DataCell("w1", -1, s[1])});
//		}
//		//KMeansClusterer cluster = new KMeansClusterer(20, 100, new DNMDistance(new AbstractDistance[]{new EditDistance(props), new EditDistance(props)}));
//		//cluster.buildClusterer(items.toArray());
//	}

}
