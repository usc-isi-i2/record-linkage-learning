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


package cdc.impl;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cdc.utils.PrintUtils;

import au.com.bytecode.opencsv.CSVReader;

public class MatchSummary {
	
	//public static String[] files = new String[] {"g:/cdcapp/Match-Deterministic.csv", "g:/cdcapp/E3.csv"};
	
	public static Map index = new HashMap();
	
	//summary data
	static int matchesGold = 0;
	static int correctMatches = 0;
	static int incorrectMatches = 0;
	static int notFoundInGold = 0;
	
	public static void main(String[] args) throws IOException {
		String[] files = args;
		if (files.length != 2) {
			System.out.println("Usage: MatchSummary [file1] [file2]");
			return;
		}
		buildIndex(files[0]);
		System.out.println("Index OK");
		searchIndexTakingNBestMatch(files[1], 1);
		System.out.println("Matches produced by gold standard: " + matchesGold);
		System.out.println("Correct matches: " + correctMatches);
		System.out.println("Incorrect matches: " + incorrectMatches);
		System.out.println("Matches not found in gold standard: " + notFoundInGold);
	}

	public static void buildIndex(String fName) throws IOException {
		CSVReader parser = new CSVReader(new FileReader(fName));
		String[] row = parser.readNext();
		while ((row = parser.readNext()) != null) {
			matchesGold++;
			Integer cdcid = new Integer(row[0]);
			Integer certid = new Integer(row[1]);
			Integer year = new Integer(row[2]);
			if (index.containsKey(cdcid)) {
				throw new RuntimeException("Multiple matches for CDCID" + cdcid);
			} else {
				index.put(cdcid, new Integer[] {certid, year});
			}
		}
	}
	
	public static void searchIndex(String fName) throws IOException {
		CSVReader parser = new CSVReader(new FileReader(fName));
		String[] row = parser.readNext();
		while ((row = parser.readNext()) != null) {
			Integer cdcid = new Integer(row[0]);
			Integer certid = new Integer(row[1]);
			Integer year = new Integer(row[2]);
			if (index.containsKey(cdcid)) {
				Integer[] thatRecord = (Integer[]) index.get(cdcid);
				if (thatRecord[0].equals(certid) && thatRecord[1].equals(year)) {
					correctMatches++;
				} else {
					incorrectMatches++;
				}
			} else {
				notFoundInGold++;
			}
		}
	}
	
	public static void searchIndexTakingBestMatch(String fName) throws IOException {
		Map records = new HashMap();
		CSVReader parser = new CSVReader(new FileReader(fName));
		String[] row = parser.readNext();
		while ((row = parser.readNext()) != null) {
			//System.out.println("Record: " + PrintUtils.printArray(row));
			Integer cdcid = new Integer(row[0]);
			Integer certid = new Integer(row[1]);
			Integer year = new Integer(row[2]);
			Integer conf = new Integer(row[3]);
			if (records.containsKey(cdcid)) {
				Integer[] bestMatch = (Integer[])records.get(cdcid);
				if (bestMatch[2].intValue() < conf.intValue()) {
					records.put(cdcid, new Integer[] {certid, year, conf});
				}
			} else {
				records.put(cdcid, new Integer[] {certid, year, conf});
			}
		}
		
		for (Iterator iterator = records.keySet().iterator(); iterator.hasNext();) {
			Integer cdcid = (Integer) iterator.next();
			Integer[] rest = (Integer[])records.get(cdcid);
			if (index.containsKey(cdcid)) {
				Integer[] thatRecord = (Integer[]) index.get(cdcid);
				if (thatRecord[0].equals(rest[0]) && thatRecord[1].equals(rest[1])) {
					correctMatches++;
				} else {
					incorrectMatches++;
				}
			} else {
				notFoundInGold++;
			}
		}
	}
	
	private static void searchIndexTakingNBestMatch(String fName, int nBest) throws IOException {
		Map records = new HashMap();
		CSVReader parser = new CSVReader(new FileReader(fName));
		String[] row = parser.readNext();
		while ((row = parser.readNext()) != null) {
			//System.out.println("Record: " + PrintUtils.printArray(row));
			try {
					Integer cdcid = new Integer(row[0]);
					Integer certid = new Integer(row[1]);
					Integer year = new Integer(row[2]);
					Integer conf = new Integer(row[3]);
					if (records.containsKey(cdcid)) {
						List matches = (List)records.get(cdcid);
						if (matches.size() < nBest) {
							matches.add(new Integer[] {certid, year, conf});
						} else {
							int best = 0;
							int lowestConf = ((Integer[])matches.get(0))[2].intValue();
							for (int i = 1; i < matches.size(); i++) {
								Integer[] candidate = (Integer[])matches.get(i);
								if (lowestConf > candidate[2].intValue()) {
									best = i;
									lowestConf = candidate[2].intValue();
								}
							}
							if (best != -1 && lowestConf < conf.intValue()) {
								matches.remove(best);
								matches.add(new Integer[] {certid, year, conf});
							} else if (lowestConf == conf.intValue()) {
								matches.add(new Integer[] {certid, year, conf});
							}
						}
					} else {
						List matches = new ArrayList();
						matches.add(new Integer[] {certid, year, conf});
						records.put(cdcid, matches);
					}
			} catch (NumberFormatException e) {
				System.out.println("Error in row: " + PrintUtils.printArray(row));
				return;
			}
		}
		
		for (Iterator iterator = records.keySet().iterator(); iterator.hasNext();) {
			Integer cdcid = (Integer) iterator.next();
			List matches = (List)records.get(cdcid);
			for (Iterator iterator2 = matches.iterator(); iterator2.hasNext();) {
				Integer[] rest = (Integer[]) iterator2.next();
				if (index.containsKey(cdcid)) {
					Integer[] thatRecord = (Integer[]) index.get(cdcid);
					if (thatRecord[0].equals(rest[0]) && thatRecord[1].equals(rest[1])) {
						correctMatches++;
					} else {
						incorrectMatches++;
					}
				} else {
					notFoundInGold++;
					System.out.print(cdcid + "  ");
					System.out.println(rest[0] + "  " + rest[1] + "  " + rest[2]);
				}
			}
		}
	}
	
}
