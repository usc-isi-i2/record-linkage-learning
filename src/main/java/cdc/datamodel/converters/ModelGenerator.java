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


package cdc.datamodel.converters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.utils.Log;
import cdc.utils.PrintUtils;
import cdc.utils.RJException;

public class ModelGenerator {
	
	private static final int logLevel = Log.getLogLevel(ModelGenerator.class);
	
	private DataColumnDefinition[] inputFormat;
	private DataColumnDefinition[] outputFormat;
	private List evaluationStages = new ArrayList();

	private DataColumnDefinition[] sortedOutput;
	
	public ModelGenerator(DataColumnDefinition[] columns) {
		this.inputFormat = columns;
		this.outputFormat = new DataColumnDefinition[columns.length];
		for (int i = 0; i < columns.length; i++) {
			DummyConverter dummyConverter = new DummyConverter(columns[i].getColumnName(), columns[i], null);
			evaluationStages.add(dummyConverter);
			outputFormat[i] = dummyConverter.getOutputColumns()[0];
		}
	}
	
	public ModelGenerator(AbstractColumnConverter[] converters) {
		Log.log(ModelGenerator.class, "Creating new model generator", 1);
		List output = new ArrayList();
		List input = new ArrayList();
		Map available = new HashMap();
		for (int i = 0; i < converters.length; i++) {
			DataColumnDefinition[] in = converters[i].getExpectedColumns();
			DataColumnDefinition[] out = converters[i].getOutputColumns();
			for (int j = 0; j < in.length; j++) {
				if (!(in[j] instanceof ConverterColumnWrapper)) {
					if (!input.contains(in[j])) {
						input.add(in[j]);
					}
//					if (!output.contains(in[j])) {
//						output.add(in[j]);
//					}
					available.put(in[j], null);
				}
			}
			for (int j = 0; j < out.length; j++) {
				if (!output.contains(out[j])) {
					output.add(out[j]);
				}
			}
		}
		this.inputFormat = (DataColumnDefinition[]) input.toArray(new DataColumnDefinition[] {});
		this.outputFormat = (DataColumnDefinition[]) output.toArray(new DataColumnDefinition[] {});
		
		this.sortedOutput = new DataColumnDefinition[outputFormat.length];
		for (int i = 0; i < sortedOutput.length; i++) {
			sortedOutput[i] = outputFormat[i];
		}
		Arrays.sort(sortedOutput, new Comparator() {
			public int compare(Object arg0, Object arg1) {
				return ((DataColumnDefinition)arg0).getColumnName().toLowerCase().compareTo(((DataColumnDefinition)arg1).getColumnName().toLowerCase());
			}});
		
		List toDistribute = new ArrayList();
		for (int i = 0; i < converters.length; i++) {
			toDistribute.add(converters[i]);
		}
		
		int id = 0;
		main: while (!toDistribute.isEmpty()) {
			AbstractColumnConverter converter = (AbstractColumnConverter) toDistribute.get(id);
			for (int i = 0; i < converter.getExpectedColumns().length; i++) {
				if (!available.containsKey(converter.getExpectedColumns()[i])) {
					id++;
					continue main;
				}
			}
			toDistribute.remove(id);
			id = 0;
			for (int i = 0; i < converter.getOutputColumns().length; i++) {
				available.put(converter.getOutputColumns()[i], null);
			}
			evaluationStages.add(converter);
		}
		
		if (logLevel >= 2) {
			Log.log(ModelGenerator.class, "Model input columns: " + PrintUtils.printArray(inputFormat), 2);
			Log.log(ModelGenerator.class, "Model output columns: " + PrintUtils.printArray(outputFormat), 2);
		}
	}

	public DataColumnDefinition[] getInputFormat() {
		return inputFormat;
	}

	public DataColumnDefinition[] getOutputFormat() {
		return outputFormat;
	}
	
	public DataCell[] generateOutputRow(DataCell[] cells) throws RJException {
		Map row = new HashMap();
		for (int i = 0; i < inputFormat.length; i++) {
			row.put(inputFormat[i], cells[i]);
		}
		
		for (Iterator conv = evaluationStages.iterator(); conv.hasNext();) {
			AbstractColumnConverter converter = (AbstractColumnConverter) conv.next();
			DataColumnDefinition[] expected = converter.getExpectedColumns();
			DataColumnDefinition[] produced = converter.getOutputColumns();
			DataCell[] input = new DataCell[expected.length];
			for (int i = 0; i < input.length; i++) {
				input[i] = (DataCell) row.get(expected[i]);
			}
			DataCell[] output = converter.convert(input);
			for (int i = 0; i < output.length; i++) {
				row.put(produced[i], output[i]);
			}
		}
		
		DataCell[] out = new DataCell[outputFormat.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = (DataCell) row.get(outputFormat[i]);
		}
		
		if (logLevel >= 2) {
			Log.log(ModelGenerator.class, "Columns before convert: " + PrintUtils.printArray(cells), 2);
			Log.log(ModelGenerator.class, "Columns after convert: " + PrintUtils.printArray(out), 2);
		}
		return out;
	}

	public AbstractColumnConverter[] getConverters() {
		return (AbstractColumnConverter[]) evaluationStages.toArray(new AbstractColumnConverter[] {});
	}

	public DataColumnDefinition[] getDependantColumns(DataColumnDefinition dataColumnDefinition) {
		List deps = new ArrayList();
		if (dataColumnDefinition instanceof ConverterColumnWrapper) {
			AbstractColumnConverter converter = null;
			main: for (int i = 0; i < evaluationStages.size(); i++) {
				AbstractColumnConverter conv = (AbstractColumnConverter) evaluationStages.get(i);
				for (int j = 0; j < conv.getOutputColumns().length; j++) {
					if (conv.getOutputColumns()[j].equals(dataColumnDefinition)) {
						converter = conv;
						break main;
					}
				}
			}
			for (int i = 0; i < converter.getExpectedColumns().length; i++) {
				deps.addAll(Arrays.asList(getDependantColumns(converter.getExpectedColumns()[i])));
			}
			return (DataColumnDefinition[]) deps.toArray(new DataColumnDefinition[] {});
		} else {
			return new DataColumnDefinition[] {dataColumnDefinition};
		}
	}

	public DataColumnDefinition getColumnByName(String string) {
		for (int i = 0; i < outputFormat.length; i++) {
			if (outputFormat[i].getColumnName().equals(string)) {
				return outputFormat[i];
			}
		}
		return null;
	}

	public DataColumnDefinition[] getSortedOutputColumns() {
		return sortedOutput;
	}

}
