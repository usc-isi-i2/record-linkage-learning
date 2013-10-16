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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class KMeansClusterer {
	
	private Object[] centroids;
	private List[] clustersData;
	private int iterations;
	private ClusteringDistance distance;
	
	//private Random random = new Random(System.currentTimeMillis());
	
	public KMeansClusterer(int k, int maxIters, ClusteringDistance distance) {
		iterations = maxIters;
		centroids = new Object[k];
		clustersData = new List[k];
		this.distance = distance;
	}
	
	public void learn(Object[] data) {
		
		int usedCentroids = 0;
		Random r = new Random();
		for (int i = 0; i < data.length; i++) {
			if (usedCentroids < centroids.length) {
				centroids[usedCentroids++] = data[i];
			} else {
				if (r.nextDouble() < 0.01) {
					System.out.println("Swithing random field");
					centroids[r.nextInt() % centroids.length] = data[i];
				}
			}
		}
//		for (int i = 0; i < centroids.length && i < data.length; i++) {
//			if (usedCentroids == 0) {
//				centroids[usedCentroids++] = data[i]; 
//			} else {
//				for (int j = 0; j < usedCentroids; j++) {
//					if (distance.distance(centroids[j], data[i]) >= distance.getMaxDistance()) {
//						centroids[usedCentroids++] = data[i]; 
//					}
//				}
//			}
//		}
//		System.out.println("Used centroids: " + usedCentroids);
//		if (usedCentroids < centroids.length) {
//			System.out.println("Will have to choose some random centroids.");
//			//just add some random cases
//			for (int i = 0; i < data.length; i++) {
//				boolean used = false;
//				for (int j = 0; j < usedCentroids; j++) {
//					if (distance.distance(centroids[j], data[i]) == 0) {
//						used = true;
//						break;
//					}
//				}
//				if (!used) {
//					centroids[usedCentroids++] = data[i];
//				}
//			}
//		}
		
		System.out.println("Centroids (initial)");
		printCentroidsSummary();
		
		for (int i = 0; i < iterations; i++) {
			for (int j = 0; j < clustersData.length; j++) {
				clustersData[j] = new ArrayList();
			}
			doIteration(data);
		}
		
		//printCentroidsSummary();
		
		//reclaim memory
		this.clustersData = null;
	}

	private void doIteration(Object[] data) {
		
		//cluster data to new centroids
		for (int i = 0; i < data.length; i++) {
			double bestDist = Double.MAX_VALUE;
			int bestCluster = 0;
			for (int j = 0; j < centroids.length; j++) {
				double distance = this.distance.distance(centroids[j], data[i]);
				if (bestDist > distance) {
					bestDist = distance;
					bestCluster = j;
				}
			}
			clustersData[bestCluster].add(data[i]);
		}
		
		///////////Only stats
		System.out.print("Size of clusters:");
		int sum = 0;
		for (int i = 0; i < centroids.length; i++) {
			System.out.print(clustersData[i].size() + "  ");
			sum += clustersData[i].size();
		}
		System.out.println();
		///////////End Only stats
		
		
		//update centroids
		for (int i = 0; i < centroids.length; i++) {
			centroids[i] = distance.centroid(clustersData[i].toArray());
		}
	}
	
//	public void addLeftDataSourceRow(DataRow row) {
//		
//	}
//	
//	public void addRightDataSourceRow(DataRow row) {
//		
//	}

	public Object[] getCentroids() {
		return centroids;
	}

	public ClusteringDistance getDistance() {
		return distance;
	}
	
	private void printCentroidsSummary() {
		System.out.println("Cluster centroids summary:");
		for (int i = 0; i < centroids.length; i++) {
			System.out.println("Centroid " + i + ": " + centroids[i]);
		}
		System.out.println();
	}
	
//	public static void main(String[] args) {
//	System.out.println("Test for KMeans begins");
//	
////	Double[][] data = {
////			{new Double(1), new Double(1)},
////			{new Double(1), new Double(2)},
////			{new Double(3), new Double(4)},
////			{new Double(4), new Double(4)}
////	};
//	
//	Double[][] data = {
//			{new Double(1), new Double(1)},
//			{new Double(1), new Double(2)},
//			{new Double(3), new Double(3)},
//			{new Double(2), new Double(1)},
//			{new Double(1), new Double(8)},
//			{new Double(10), new Double(1)},
//			{new Double(11), new Double(11)},
//			{new Double(4), new Double(5)},
//			{new Double(5), new Double(4)},
//			{new Double(4), new Double(7)},
//			{new Double(10), new Double(12)},
//			{new Double(5), new Double(6)},
//			{new Double(6), new Double(6)},
//			{new Double(6), new Double(7)},
//			{new Double(4), new Double(1)},
//			{new Double(4), new Double(3)},
//			{new Double(3), new Double(3)},
//			{new Double(4), new Double(4)}
//	};
//	
//	KMeansClusterer kmeans = new KMeansClusterer(4, 100, new EuclidDistance());
//	kmeans.learn(data);
//	
//}
	
}
