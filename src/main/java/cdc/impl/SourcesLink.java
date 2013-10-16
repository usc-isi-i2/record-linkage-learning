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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractResultsSaver;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataRow;
import cdc.datamodel.converters.ModelGenerator;
import cdc.impl.datasource.office.ExcelDataSource;
import cdc.impl.datasource.text.CSVDataSource;
import cdc.impl.datasource.text.FixedColumnWidthFileDataSource;
import cdc.impl.resultsavers.CSVFileSaver;
import cdc.utils.RJException;

public class SourcesLink {
	public static void main(String[] args) throws IOException, RJException {
		
		InputStream stream = System.in;
		
		if (args.length != 0) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-f")) {
					stream = new FileInputStream(args[i + 1]);
					i++;
				}
			}
		}
		
		System.out.println("This program fills in data in the first data source with data from other file based on join condition.");
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		AbstractDataSource source1 = getSource("s1", reader);
		AbstractDataSource source2 = getSource("s2", reader);
		source1.setModel(new ModelGenerator(source1.getAvailableColumns()));
		source2.setModel(new ModelGenerator(source2.getAvailableColumns()));
		
		String cond = readParam(reader, "Join rule (column_name=column_name): ");
		String[] condArray = cond.split("=");
		
		List mappings = new ArrayList();
		while (true) {
			String mapping = readParam(reader, "Mapping rule (column_name=column_name, type done when finished): ", "done");
			if (mapping.equals("done")) {
				break;
			}
			String[] cols = mapping.split("=");
			mappings.add(cols);
		}
		
		//read second data source
		Map index = new HashMap();
		DataRow row;
		while ((row = source2.getNextRow()) != null) {
			index.put(row.getData(condArray[1]).getValue(), row);
		}
		
		Map props = new HashMap();
		props.put(CSVFileSaver.OUTPUT_FILE_PROPERTY, readParam(reader, "Output file name: ", "join-result"));
		props.put(CSVFileSaver.SAVE_SOURCE_NAME, "false");
		AbstractResultsSaver saver = new CSVFileSaver(props);
		while ((row = source1.getNextRow()) != null) {
			Object key = row.getData(condArray[0]).getValue();
			DataRow complement = (DataRow)index.get(key);
			if (complement != null) {
				System.out.println("Complement of record " + row.getData(condArray[0]).getValue());
				for (int i = 0; i < mappings.size(); i++) {
					String[] mapping = (String[])mappings.get(i);
					DataCell data = row.getData(mapping[0]);
					System.out.println("   " + data.getValue() + " -< " + complement.getData(mapping[1]).getValue());
					data.setValue(complement.getData(mapping[1]).getValue());
				}
			}
			saver.saveRow(row);
		}
		saver.close();
		source1.close();
		source2.close();
	}
	
	private static AbstractDataSource getSource(String name, BufferedReader reader) throws IOException, RJException {
		while (true) {
			String line = readParam(reader, "Data source type (csv/excel/text)", "csv");
			if (line.equals("csv")) {
				Map props = new HashMap();
				props.put(CSVDataSource.PARAM_INPUT_FILE, readParam(reader, "Input file name: "));
				props.put(CSVDataSource.PARAM_DELIM, readParam(reader, "Field delimeter: ", ","));
				return new CSVDataSource(name, props);
			} else if (line.equals("excel")) {
				Map props = new HashMap();
				props.put(ExcelDataSource.PARAM_FILE, readParam(reader, "Input file name: "));
				props.put(ExcelDataSource.PARAM_SHEET, readParam(reader, "Sheet name: "));
				return new ExcelDataSource(name, props);
			} else if (line.equals("text")) {
				Map props = new HashMap();
				props.put(FixedColumnWidthFileDataSource.PARAM_FILE_NAME, readParam(reader, "Input file name: "));
				String file = (String)props.get(FixedColumnWidthFileDataSource.PARAM_FILE_NAME);
				String format = file.substring(0, file.lastIndexOf('.'));
				props.put(FixedColumnWidthFileDataSource.PARAM_SCHEMA_FILE, readParam(reader, "Format file: ", format));
				return new FixedColumnWidthFileDataSource(name, props);
			} else {
				System.out.println("Error: Unkwnon source type.");
			}
		}
	}

	private static String readParam(BufferedReader reader, String string) throws IOException {
		return readParam(reader, string, null);
	}

	private static String readParam(BufferedReader reader, String outText, String string) throws IOException {
		while (true) {
			System.out.print(outText + (string != null ? " [" + string + "]: " : ""));
			String line = reader.readLine().trim();
			if (line.isEmpty() && string != null) {
				line = string;
			}
			if (line.isEmpty() && string == null) {
				System.out.println("Invalid value.");
			} else {
				return line;
			}
		}
	}
}
