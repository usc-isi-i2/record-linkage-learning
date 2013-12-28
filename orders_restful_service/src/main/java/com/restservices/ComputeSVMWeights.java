package com.restservices;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.core.Instances;

public class ComputeSVMWeights {
	String sourcePathLeft = "";
	String sourcePathRight = "";
	String acceptance_level = "";
	static int noofAttributes = 0;
	static int defaultWeight = 0;
	static int AttributeScalabilityFactor = 3;
	ArrayList<Double> weightList;
	Map<String, String> svm_weights = new HashMap<String, String>();
	Map<String, String> old_weights = new HashMap<String, String>();
	Map<String, String> new_weights = new HashMap<String, String>();
	String json_phase1_path = "";

	String constructJSON() throws FileNotFoundException {
		String JSON_STR = "";
		JSON_STR = JSON_STR + "{ \"sourcePathLeft\":" + "\"" + sourcePathLeft
				+ "\",\n";
		JSON_STR = JSON_STR + "\"sourcePathRight\":" + "\"" + sourcePathRight
				+ "\",\n";
		JSON_STR = JSON_STR + "\"acceptance-level\":" + "\"" + acceptance_level
				+ "\",\n";
		JSON_STR = JSON_STR + "\"columns\": [";

		JSONParser parser = new JSONParser();
		try {

			Object obj = null;
			try {
				obj = parser.parse(new FileReader(json_phase1_path));
			} catch (org.json.simple.parser.ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			JSONObject jsonObject = (JSONObject) obj;
			// loop array

			JSONArray msg_1 = (JSONArray) jsonObject.get("columns");
			Iterator<JSONObject> iterator = msg_1.iterator();
			while (iterator.hasNext()) {
				JSONObject obj1 = iterator.next();
				JSON_STR = JSON_STR + "{\n";
				JSON_STR = JSON_STR + "\"columnName\":" + "\""
						+ (String) obj1.get("columnName") + "\",\n";
				JSON_STR = JSON_STR + "\"algorithm\":" + "\""
						+ (String) obj1.get("algorithm") + "\",\n";
				JSON_STR = JSON_STR + "\"weight\":" + "\""
						+ new_weights.get((String) obj1.get("columnName"))
						+ "\",\n";
				if (!((String) obj1.get("algorithm")).equals("NumericDistance")) {
					JSON_STR = JSON_STR
							+ "\"params\": {\n \"match-level-start\": \"0.1\",\n\"math-level-end\": \"0.9\"\n}";
					JSON_STR = JSON_STR + "\n},";
				} else {
					JSON_STR = JSON_STR
							+ "\"params\": {\n \"use-lineral-approximation\": \"true\",\n\"percent-difference\": \"5.0,5.0\",\n\"numeric-difference\": \"\"\n}";
					JSON_STR = JSON_STR + "\n},";
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		JSON_STR = JSON_STR.substring(0, JSON_STR.length() - 2);
		JSON_STR = JSON_STR + "}]}";
//		System.out.println("JSON Written to " + PATH_SVM_JSON_OP);
//		FileOutputStream f = new FileOutputStream(PATH_SVM_JSON_OP);
//		System.setOut(new PrintStream(f));
		return JSON_STR;
	}

	String scaleWeights() throws FileNotFoundException {
		int newWeight = 0;
		int totalWeight = 0;
		Collections.sort(weightList);
		for (int i = 0; i < weightList.size(); i++) {
			System.out.println(weightList.get(i));
			String columnName = svm_weights.get(String.valueOf(weightList
					.get(i)));
			String oldWeight = svm_weights.get(columnName);
			if (noofAttributes != (i + 1)) {
				if (oldWeight != null) {
					newWeight = (int) (Double.parseDouble(oldWeight) - (weightList
							.get(i)) * AttributeScalabilityFactor);
					totalWeight = totalWeight + newWeight;
				} else {
					newWeight = (int) (defaultWeight - (weightList.get(i))
							* AttributeScalabilityFactor);
					totalWeight = totalWeight + newWeight;
				}
			} else {
				newWeight = 100 - totalWeight;
			}

			new_weights.put(columnName, String.valueOf(newWeight));
			// System.out.println(columnName + "-- "+
			// new_weights.get(columnName) );
		}
		return constructJSON();
	}

	void getOldWeights() {

		JSONParser parser = new JSONParser();
		try {

			Object obj = null;
			try {
				obj = parser.parse(new FileReader(json_phase1_path));
			} catch (org.json.simple.parser.ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			JSONObject jsonObject = (JSONObject) obj;
			// loop array
			sourcePathLeft = (String) jsonObject.get("sourcePathLeft");
			sourcePathRight = (String) jsonObject.get("sourcePathRight");
			acceptance_level = (String) jsonObject.get("acceptance-level");

			JSONArray msg_1 = (JSONArray) jsonObject.get("columns");
			Iterator<JSONObject> iterator = msg_1.iterator();
			while (iterator.hasNext()) {
				JSONObject obj1 = iterator.next();
				old_weights.put((String) obj1.get("weight"),
						(String) obj1.get("columnName"));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	String computeWeights(String path) throws Exception {
		weightList = new ArrayList<Double>();
		defaultWeight = 100 / noofAttributes;

		BufferedReader br = null;
		StringReader sr = new StringReader(path); // wrap your String
		br = new BufferedReader(sr);

		// br = new BufferedReader(new FileReader(path));
		Instances data = new Instances(br);

		if (data.classIndex() == -1)
			data.setClassIndex(data.numAttributes() - 1);

		// Initialize the SMO classifier
		SMO smo = new SMO();
		smo.buildClassifier(data);
		Evaluation eval = new Evaluation(data);
		// Get the weights in sparse representation
		double[][][] sparseWeights = smo.sparseWeights();
		int[][][] sparseIndices = smo.sparseIndices();

		// Transform the weights into non-sparse representation
		double[] weights = new double[10];
		for (int i = 0; i < sparseIndices[0][1].length; i++) {
			weights[sparseIndices[0][1][i]] = sparseWeights[0][1][i];
		}

		// Output the weights
		for (int i = 0; i < data.numAttributes(); i++) {

			// Skip the class
			if (i != data.classIndex()) {
				weightList.add((Double) weights[i]);
				svm_weights.put(String.valueOf(weights[i]), (String) data
						.attribute(i).name());
			}
		}
		getOldWeights();
		return scaleWeights();
	}

	public double edits(String s1, String s2) {
		return distanceInt(s1, s2);
	}

	public double distance(String s1, String s2) {
		double dist = distanceInt(s1, s2);
		return dist;
	}

	boolean lineralOn = true;
	private double[] percent;

	private double getDistancePercent(double val1, double val2) {
		double n1 = val1 * 1;
		double n2 = val1 * 10;
		return getDistanceRange(val1, val2, new double[] { n1, n2 });
	}

	private double getDistanceRange(double val1, double val2, double[] range) {
		double diff = Math.abs(val1 - val2);
		if (diff > range[0] && val1 > val2) {
			return 0;
		} else if (diff > range[1] && val1 < val2) {
			return 0;
		} else if (val1 > val2) {
			if (range[0] == 0) {
				return val1 == val2 ? 100 : 0;
			}
			if (lineralOn) {
				return (range[0] - diff) / range[0] * 100;
			} else {
				return 100;
			}
		} else {
			if (range[1] == 0) {
				return val1 == val2 ? 100 : 0;
			}
			if (lineralOn) {
				return (range[1] - diff) / range[1] * 100;
			} else {
				return 100;
			}
		}
	}

	private double distanceInt(String str1, String str2) {
		int m = str1.length();
		int n = str2.length();
		str1 = str1.toUpperCase();
		str2 = str2.toUpperCase();
		int mat[][] = new int[m + 1][n + 1];

		if (m == 0 || n == 0) {
			return Math.max(m, n);
		} else {
			for (int k = 0; k < m + 1; k++) {
				mat[k][0] = k;
			}
			for (int k = 0; k < n + 1; k++) {
				mat[0][k] = k;
			}

			for (int k = 1; k < m + 1; k++) {
				for (int l = 1; l < n + 1; l++) {
					int cost = 0;
					if (str1.charAt(k - 1) == str2.charAt(l - 1)) {
						cost = 0;
					} else {
						cost = 1;
					}
					mat[k][l] = minimum(mat[k - 1][l] + 1, mat[k][l - 1] + 1,
							mat[k - 1][l - 1] + cost);
					if (k > 1 && l > 1
							&& str1.charAt(k - 1) == str2.charAt(l - 2)
							&& str1.charAt(k - 2) == str2.charAt(l - 1)) {
						mat[k][l] = Math.min(mat[k][l], mat[k - 2][l - 2]
								+ cost);
					}
				}

			}
			return mat[m][n];
		}
	}

	private int minimum(int i, int j, int k) {
		return Math.min(i, Math.min(j, k));
	}

	public String parseJSON_1(String jsonStr) throws Exception {
		// TODO Auto-generated method stub
		String dataforSVM = "@relation fulldataset\n";
		JSONObject json = (JSONObject) new JSONParser().parse(jsonStr);

		Map<String, String> algo_map = new HashMap<String, String>();

		// loop array
		JSONArray msg_1 = (JSONArray) json.get("columns");
		json_phase1_path = (String) json.get("json_phase1_path");
		Iterator<JSONObject> iterator = msg_1.iterator();
		while (iterator.hasNext()) {
			noofAttributes++;
			JSONObject obj1 = iterator.next();
			algo_map.put((String) obj1.get("columnName"),
					(String) obj1.get("algorithm"));
			dataforSVM = dataforSVM + "@attribute "
					+ (String) obj1.get("columnName") + "  " + "NUMERIC\n";
		}

		dataforSVM = dataforSVM + "@attribute feedback{no, yes}\n@data\n";

		JSONArray msg_2 = (JSONArray) json.get("data");
		Iterator<JSONObject> iterator_2 = msg_2.iterator();
		while (iterator_2.hasNext()) {
			JSONObject obj_2 = iterator_2.next();
			JSONArray msg_3 = (JSONArray) obj_2.get("pair");
			Iterator<JSONObject> iterator_3 = msg_3.iterator();
			while (iterator_3.hasNext()) {
				JSONObject obj_3 = iterator_3.next();
				if (algo_map.get((String) obj_3.get("columnName")).equals(
						"EditDistance")) {
					dataforSVM = dataforSVM
							+ distance((String) obj_3.get("src1"),
									(String) obj_3.get("src2")) + ",";
				} else if (algo_map.get((String) obj_3.get("columnName"))
						.equals("NumericDistance")) {
					dataforSVM = dataforSVM
							+ Math.floor(getDistancePercent(Double
									.parseDouble((String) obj_3.get("src1")),
									Double.parseDouble((String) obj_3
											.get("src2"))) / 100 * 100) + ",";
				}
			}
			dataforSVM = dataforSVM + ((String) obj_2.get("feedback"));
			dataforSVM = dataforSVM + "\n";
		}
		System.out.println(dataforSVM);
		return computeWeights(dataforSVM);
	}
}
















