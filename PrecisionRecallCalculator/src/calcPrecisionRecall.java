import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import au.com.bytecode.opencsv.CSVReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class calcPrecisionRecall {
	static String JSON_STR = "";
	static int attributeCount = 0;
	static int TOTAL_CORRECT_ANS_COUNT = 0;
	static String PATH_JSON_PHASE1 = "";
	static String PATH_SRC1_LOCAL = "";
	static String PATH_SRC2_LOCAL = "";
	static String PATH_PERFECT_MAPPING_LOCAL = "";
	static String PATH_RESULT_PHASE1 = "";
	static String PATH_JSON_PHASE2 = "";

	private static void computeAttributes() {
		JSON_STR = "{ \"json_phase1_path\": " + "\"" +PATH_JSON_PHASE1.replace("\\", "\\\\")+ "\"," + "\n" + " \"columns\": \n[\n";
		JSONParser parser = new JSONParser();
		try {

			Object obj = null;
			try {
				obj = parser.parse(new FileReader(PATH_JSON_PHASE1));
			} catch (org.json.simple.parser.ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			JSONObject jsonObject = (JSONObject) obj;
			// loop array
			JSONArray msg_1 = (JSONArray) jsonObject.get("columns");
			Iterator<JSONObject> iterator = msg_1.iterator();
			// System.out.println(JSON_STR);
			while (iterator.hasNext()) {
				attributeCount++;
				JSONObject obj1 = iterator.next();
				JSON_STR = JSON_STR + "{ \"columnName\": \""
						+ obj1.get("columnName") + "\",\n";
				JSON_STR = JSON_STR + " \"algorithm\": \""
						+ obj1.get("algorithm") + "\" \n},\n";
			}

			// To remove extra coma at the end
			JSON_STR = JSON_STR.substring(0, JSON_STR.length() - 2);
			JSON_STR = JSON_STR + "\n],\n\"data\": [ \n";
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws FileNotFoundException,
			UnsupportedEncodingException {
		// TODO Auto-generated method stub
		Properties prop = new Properties();

		try {
			// load a properties file
			prop.load(new FileInputStream("RecordLinkage.Properties"));

			// get the property value and print it out
			PATH_JSON_PHASE1 = prop.getProperty("PATH_JSON_PHASE1");
			PATH_SRC1_LOCAL = prop.getProperty("PATH_SRC1_LOCAL");
			PATH_SRC2_LOCAL = prop.getProperty("PATH_SRC2_LOCAL");
			PATH_PERFECT_MAPPING_LOCAL = prop
					.getProperty("PATH_PERFECT_MAPPING_LOCAL");
			PATH_RESULT_PHASE1 = prop.getProperty("PATH_RESULT_PHASE1");
			PATH_JSON_PHASE2 = prop.getProperty("PATH_JSON_PHASE2");
			TOTAL_CORRECT_ANS_COUNT=Integer.parseInt((prop.getProperty("TOTAL_CORRECT_ANS_COUNT")));
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		computeAttributes();

		CSVReader reader;
		String csvFile = PATH_SRC1_LOCAL;
		reader = new CSVReader(new InputStreamReader(new FileInputStream(
				csvFile), "UTF-8"), ',', '\"', 1);
		// reader = new CSVReader(new FileReader(csvFile));
		BufferedReader br = null;
		String hash = null;
		String value = null;
		String[] col;
		Map<String, String> hash_map_src_1 = new HashMap<String, String>();
		try {
			while ((col = reader.readNext()) != null) {
				value = "[" + col[1] + "][" + col[2] + "][" + col[3] + "]["
						+ col[4] + "]";
				hash = col[0];
				// System.out.println(hash);
				// System.out.println(value);
				hash_map_src_1.put(hash, value);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		csvFile = PATH_SRC2_LOCAL;
		reader = new CSVReader(new InputStreamReader(new FileInputStream(
				csvFile), "UTF-8"), ',', '\"', 1);
		Map<String, String> hash_map_src_2 = new HashMap<String, String>();
		try {

			while ((col = reader.readNext()) != null) {
				value = "[" + col[1] + "][" + col[2] + "][" + col[3] + "]["
						+ col[4] + "]";
				hash = col[0];
				// System.out.println(value);
				hash_map_src_2.put(hash, value);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		csvFile = PATH_PERFECT_MAPPING_LOCAL;
		reader = new CSVReader(new InputStreamReader(new FileInputStream(
				csvFile), "UTF-8"), ',', '\"', 1);
		Map<String, String> mapping_map = new HashMap<String, String>();

		String hash1 = null;
		String hash2 = null;
		try {
			while ((col = reader.readNext()) != null) {
				hash1 = col[0];
				hash2 = col[1];
				hash = hash_map_src_1.get(hash1) + "-"
						+ hash_map_src_2.get(hash2);
				mapping_map.put(hash, "true");
				// System.out.println(hash);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		csvFile = PATH_RESULT_PHASE1;
		double truecount = 0;
		double falsecount = 0;
		reader = new CSVReader(new InputStreamReader(new FileInputStream(
				csvFile), "UTF-8"), ',', '\"', 1);
		try {
			while ((col = reader.readNext()) != null) {
				hash1 = "[" + col[0] + "][" + col[1] + "][" + col[2] + "]["
						+ col[3] + "]";

				hash2 = "[" + col[4] + "][" + col[5] + "][" + col[6] + "]["
						+ col[7] + "]";
				hash = hash1 + "-" + hash2;
				// System.out.println(hash);
				if (mapping_map.get(hash) != null) {
					truecount++;
					if (truecount < 10) {
						JSON_STR = JSON_STR + "{ \"pair\": [\n";
						JSONParser parser = new JSONParser();
						try {

							Object obj = null;
							try {
								obj = parser.parse(new FileReader(
										PATH_JSON_PHASE1));
							} catch (org.json.simple.parser.ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							JSONObject jsonObject = (JSONObject) obj;
							// loop array
							JSONArray msg_1 = (JSONArray) jsonObject
									.get("columns");
							Iterator<JSONObject> iterator = msg_1.iterator();
							// System.out.println(JSON_STR);
							attributeCount = 0;
							while (iterator.hasNext()) {
								JSONObject obj1 = iterator.next();
								JSON_STR = JSON_STR
										+ "\n{\n \"columnName\": \""
										+ obj1.get("columnName") + "\",\n";

								JSON_STR = JSON_STR + "\"src1\":" + "\""
										+ col[attributeCount] + "\",\n"
										+ "\"src2\":" + "\""
										+ col[attributeCount + 4] + "\"\n},";
								attributeCount++;
							}
							JSON_STR = JSON_STR.substring(0,
									JSON_STR.length() - 1);
							JSON_STR = JSON_STR + "],\n"
									+ "\"feedback\": \"yes\"\n},\n";

						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}

						// System.out.println("\"" + col[0] + "\",\"" + col[1]
						// + "\",\"" + col[2] + "\",\"" + col[3] + "\",\""
						// + col[4] + "\",\"" + col[5] + "\",\"" + col[6]
						// + "\",\"" + col[7] + "\"," + "yes");
					}
				} else {
					// System.out.println(hash+",no");
					falsecount++;
					if (falsecount < 10) {
						JSON_STR = JSON_STR + "{ \"pair\": [\n";
						JSONParser parser = new JSONParser();
						try {

							Object obj = null;
							try {
								obj = parser.parse(new FileReader(
										PATH_JSON_PHASE1));
							} catch (org.json.simple.parser.ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							JSONObject jsonObject = (JSONObject) obj;
							// loop array
							JSONArray msg_1 = (JSONArray) jsonObject
									.get("columns");
							Iterator<JSONObject> iterator = msg_1.iterator();
							// System.out.println(JSON_STR);
							attributeCount = 0;
							while (iterator.hasNext()) {
								JSONObject obj1 = iterator.next();
								JSON_STR = JSON_STR
										+ "\n{\n \"columnName\": \""
										+ obj1.get("columnName") + "\",\n";

								JSON_STR = JSON_STR + "\"src1\":" + "\""
										+ col[attributeCount] + "\",\n"
										+ "\"src2\":" + "\""
										+ col[attributeCount + 4] + "\"\n},";
								attributeCount++;
							}
							JSON_STR = JSON_STR.substring(0,
									JSON_STR.length() - 1);
							JSON_STR = JSON_STR + "],\n"
									+ "\"feedback\": \"no\"\n},\n";

						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}

						// System.out.println("\"" + col[0] + "\",\"" + col[1]
						// + "\",\"" + col[2] + "\",\"" + col[3] + "\",\""
						// + col[4] + "\",\"" + col[5] + "\",\"" + col[6]
						// + "\",\"" + col[7] + "\"," + "no");
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		JSON_STR = JSON_STR.substring(0, JSON_STR.length() - 2) + "]\n}";

		System.out.println("Precision = "
				+ (truecount / (truecount + falsecount)) * 100);
		System.out.println("Recall = " + truecount * 100 / TOTAL_CORRECT_ANS_COUNT);
		System.out.println("JSON Written to path:  " + PATH_JSON_PHASE2);
		FileOutputStream f = new FileOutputStream(PATH_JSON_PHASE2);
		System.setOut(new PrintStream(f));
		System.out.println(JSON_STR);

	}
}
