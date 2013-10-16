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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractResultsSaver;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.converters.ModelGenerator;
import cdc.impl.datasource.text.CSVDataSource;
import cdc.impl.datasource.wrappers.ExternallySortingDataSource;
import cdc.impl.resultsavers.CSVFileSaver;
import cdc.utils.CompareFunctionInterface;
import cdc.utils.RJException;
import cdc.utils.comparators.StringComparator;

public class DeduplicateLinkageResults {
	
	private AbstractDataSource source;
	private int removed;
	
	public DeduplicateLinkageResults(AbstractDataSource source) {
		this.source = source;
	}

	public static void main(String[] args) throws IOException, RJException {
		if (args.length != 3) {
			printInfo();
			return;
		}
		
		Map props = new HashMap();
		props.put(CSVDataSource.PARAM_INPUT_FILE, args[0]);
		props.put(CSVDataSource.PARAM_DELIM, args[2]);
		AbstractDataSource source = new CSVDataSource("source", props);
		source.setModel(new ModelGenerator(source.getAvailableColumns()));
		props = new HashMap();
		props.put(CSVFileSaver.OUTPUT_FILE_PROPERTY, args[1]);
		props.put(CSVFileSaver.SAVE_CONFIDENCE, "false");
		props.put(CSVFileSaver.SAVE_SOURCE_NAME, "false");
		AbstractResultsSaver saver = new CSVFileSaver(props);
		DeduplicateLinkageResults dedup = new DeduplicateLinkageResults(source);
		
		DataColumnDefinition[] keyA = readKey("Key for first data source (use commas to separate multiple fields)", source.getDataModel().getOutputFormat());
		DataColumnDefinition[] keyB = readKey("Key for second data source (use commas to separate multiple fields)", source.getDataModel().getOutputFormat());
		
		dedup.doWork(keyA, keyB, saver);
		saver.flush();
		saver.close();
		source.close();
	}

	private static DataColumnDefinition[] readKey(String string, DataColumnDefinition[] availableColumns) {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Available columns:");
		for (int i = 0; i < availableColumns.length; i++) {
			System.out.println("   " + (i + 1) + ": " + availableColumns[i].getColumnName());
		}
		System.out.print(string + ": ");
		String input = scanner.next();
		String[] ids = input.replaceAll(" ", "").split(",");
		DataColumnDefinition[] result = new DataColumnDefinition[ids.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = availableColumns[Integer.parseInt(ids[i]) - 1];
		}
		return result;
	}

	public void doWork(DataColumnDefinition[] keyA, DataColumnDefinition[] keyB, AbstractResultsSaver saver) throws IOException, RJException {
		
		removed = 0;
		
		AbstractDataSource sort1 = new ExternallySortingDataSource("sorted-source", source, keyA, getCompareFunctions(keyA), new HashMap());
		
		Map props = new HashMap();
		String tmpFile1 = "tmp_" + this.hashCode() + "_1.csv";
		props.put(CSVFileSaver.OUTPUT_FILE_PROPERTY, tmpFile1);
		props.put(CSVFileSaver.SAVE_CONFIDENCE, "false");
		props.put(CSVFileSaver.SAVE_SOURCE_NAME, "false");
		
		AbstractResultsSaver tmpSaver = new CSVFileSaver(props);
		identifyDuplicates(sort1, tmpSaver, keyA, keyB);
		tmpSaver.flush();
		tmpSaver.close();
		
		System.out.println("First round gone.");
		
		props = new HashMap();
		props.put(CSVDataSource.PARAM_INPUT_FILE, tmpFile1);
		props.put(CSVDataSource.PARAM_DELIM, ",");
		AbstractDataSource source = new CSVDataSource("source", props);
		source.setModel(new ModelGenerator(source.getAvailableColumns()));
		AbstractDataSource sort2 = new ExternallySortingDataSource("source", source, keyB, getCompareFunctions(keyB), new HashMap());
		identifyDuplicates(sort2, saver, keyB, keyA);
		
		System.out.println("Second round gone.");
		
		sort1.close();
		sort2.close();
		
		source.close();
		this.source.close();
		saver.flush();
		saver.close();
		
		System.out.println("Identified " + removed + " duplicate(s)");
		
	}

	private CompareFunctionInterface[] getCompareFunctions(DataColumnDefinition[] keyB) {
		CompareFunctionInterface[] functions = new CompareFunctionInterface[keyB.length];
		for (int i = 0; i < functions.length; i++) {
			functions[i] = new StringComparator();
		}
		return functions;
	}

	private void identifyDuplicates(AbstractDataSource source, AbstractResultsSaver saver, DataColumnDefinition[] sortedKey, DataColumnDefinition[] otherKey) throws IOException, RJException {
		DataRow example = null;
		List buffer = new ArrayList();
		example = source.getNextRow();
		buffer.add(example);
		while (true) {
			if (example == null) {
				break;
			}
			buffer.add(example);
			DataRow row;
			while (isTheSameKey(row = source.getNextRow(), example, sortedKey) && row != null) {
				buffer.add(row);
			}
			solveGroup(saver, buffer, sortedKey, otherKey);
			buffer.clear();
			example = row;
		}
	}

	private void solveGroup(AbstractResultsSaver saver, List buffer, DataColumnDefinition[] sortedKey, DataColumnDefinition[] otherKey) throws RJException, IOException {
		int maxConfidence = 0;
		int cnt = 0;
		for (Iterator iterator = buffer.iterator(); iterator.hasNext();) {
			DataRow row = (DataRow) iterator.next();
			int conf = Integer.parseInt(row.getData("Confidence").getValue().toString());
			if (maxConfidence < conf) {
				maxConfidence = conf;
				cnt = 0;
			} else if (maxConfidence == conf) {
				cnt++;
			}
		}
		if (cnt > 1) {
			System.out.println("WARNING: " + cnt + " records with confidence " + maxConfidence);
		}
		for (Iterator iterator = buffer.iterator(); iterator.hasNext();) {
			DataRow row = (DataRow) iterator.next();
			if (maxConfidence == Integer.parseInt(row.getData("Confidence").getValue().toString())) {
				if (buffer.size() > 1) {
					System.out.println("                Row: " + row);
				}
				saver.saveRow(row);
			} else {
				System.out.println("    Skipping record: " + row);
				removed++;
			}
		}
	}

	private boolean isTheSameKey(DataRow r1, DataRow r2, DataColumnDefinition[] sortedKey) {
		if (r1 == null || r2 == null) {
			return false;
		}
		for (int i = 0; i < sortedKey.length; i++) {
			if (!r1.getData(sortedKey[i]).getValue().equals(r2.getData(sortedKey[i]).getValue())) {
				return false;
			}
		}
		return true;
	}

	private static void printInfo() {
		System.out.println("Usage: DeduplicateLinkageResults csv-in-file csv-out-file separator");
	}
	
}
