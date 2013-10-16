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


package cdc.gui.components.dynamicanalysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cdc.components.AbstractDataSource;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.converters.AbstractColumnConverter;
import cdc.datamodel.converters.DummyConverter;
import cdc.datamodel.converters.ModelGenerator;
import cdc.gui.StoppableThread;
import cdc.utils.RJException;

public class ConvsThread extends StoppableThread {

	private volatile boolean stop;
	private DynamicAnalysisFrame frame;
	private AbstractDataSource source;
	private AbstractColumnConverter conv;
	private ModelGenerator model;
	
	public ConvsThread(DynamicAnalysisFrame frame, Object[] params) {
		this.frame = frame;
		this.source = (AbstractDataSource) params[0];
		this.conv = (AbstractColumnConverter) params[1];
		this.model = (ModelGenerator) params[2];
	}

	public void run() {
		try {
			DataColumnDefinition[] in = conv.getExpectedColumns();
			DataColumnDefinition[] cols = source.getAvailableColumns();
			
			//This is to fix the same name of output column in converter as input column name...
			//If causes problems, change it.
			for (int i = 0; i < conv.getOutputColumns().length; i++) {
				for (int j = 0; j < cols.length; j++) {
					if (conv.getOutputColumns()[i].getColumnName().equals(cols[j].getColumnName())) {
						conv.getOutputColumns()[i].setName(conv.getOutputColumns()[i].getColumnName() + "_" + System.currentTimeMillis());
					}
				}
			}
			
			List columns = new ArrayList();
			List columns1 = new ArrayList();
			for (int i = 0; i < cols.length; i++) {
				columns.add(cols[i]);
				columns1.add(cols[i]);
			}
			
			List convs = ensureColumns(in, columns, model);
			convs.add(conv);
			List simpleColumns = new ArrayList();
			for (Iterator iterator = convs.iterator(); iterator.hasNext();) {
				AbstractColumnConverter converter = (AbstractColumnConverter) iterator.next();
				DataColumnDefinition[] input = converter.getExpectedColumns();
				for (int i = 0; i < input.length; i++) {
					if (columns1.contains(input[i]) && !simpleColumns.contains(input[i])) {
						simpleColumns.add(input[i]);
					}
				}
			}
			int n = 0;
			for (Iterator iterator = simpleColumns.iterator(); iterator.hasNext();) {
				DataColumnDefinition object = (DataColumnDefinition) iterator.next();
				DummyConverter dummyConverter = new DummyConverter("Dummy_converter_" + n++, object, null);
//				DataColumnDefinition[] out = dummyConverter.getOutputColumns();
//				out[0].setName(out[0].getColumnName() + "_" + System.currentTimeMillis());
				convs.add(dummyConverter);
			}
			
			DataColumnDefinition[] inTransformed = new DataColumnDefinition[in.length];
			for (int i = 0; i < inTransformed.length; i++) {
				for (Iterator iterator = convs.iterator(); iterator.hasNext();) {
					AbstractColumnConverter converter = (AbstractColumnConverter) iterator.next();
					for (int j = 0; j < converter.getOutputColumns().length; j++) {
						if (in[i].getColumnName().equals(converter.getOutputColumns()[j].getColumnName())) {
							inTransformed[i] = converter.getOutputColumns()[j];
						}
					}
				}
			}
			
//			for (Iterator iterator = convs.iterator(); iterator.hasNext();) {
//				AbstractColumnConverter converter = (AbstractColumnConverter) iterator.next();
//				if (converter instanceof AbstractColumnConverter) {
//					converter.getOutputColumns()[0].setName(converter.getOutputColumns()[0].getColumnName() + "_" + System.currentTimeMillis());
//				}
//			}
			
			ModelGenerator newModelGenerator = 
						new ModelGenerator((AbstractColumnConverter[])convs.toArray(new AbstractColumnConverter[] {}));
			//newModelGenerator.removeConverter(conv);
			source.setModel(newModelGenerator);
			source.reset();
			DataRow row;
			n = 0;
			while ((row = source.getNextRow()) != null) {
				if (n == DynamicAnalysisFrame.MAX_ROWS || stop) {
					break;
				}
				n++;
				String[] rowStr = new String[row.getData().length];
				for (int i = 0; i < inTransformed.length; i++) {
					rowStr[i] = row.getData(inTransformed[i]).getValue().toString();
				}
				DataColumnDefinition[] out = conv.getOutputColumns();
				for (int i = 0; i < out.length; i++) {
					rowStr[i + inTransformed.length] = row.getData(out[i]).getValue().toString();
				}
				frame.addRow(rowStr);
			}
		} catch (IOException e) {
			e.printStackTrace();
			frame.finished(false);
			frame.setWarningMessage(e.getMessage());
			return;
		} catch (RJException e) {
			e.printStackTrace();
			frame.finished(false);
			frame.setWarningMessage(e.getMessage());
			return;
		} catch (Exception e) {
			e.printStackTrace();
			frame.finished(false);
			frame.setWarningMessage(e.getMessage());
			return;
		} finally {
			try {
				source.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (RJException e) {
				e.printStackTrace();
			}
		}
		frame.finished(true);
	}
	
	private List ensureColumns(DataColumnDefinition[] in, List columns, ModelGenerator model) {
		List list = new ArrayList();
		List required = new ArrayList();
		for (int i = 0; i < in.length; i++) {
			required.add(in[i]);
		}
		main: while (!allAvailable(required, columns)) {
			for (Iterator iterator = required.iterator(); iterator.hasNext();) {
				DataColumnDefinition input = (DataColumnDefinition) iterator.next();
				if (list.indexOf(input) == -1) {
					//need to find this item
					AbstractColumnConverter[] convs = model.getConverters();
					for (int i = 0; i < convs.length; i++) {
						for (int j = 0; j < convs[i].getOutputColumns().length; j++) {
							if (convs[i].getOutputColumns()[j].equals(input) && !list.contains(convs[i])) {
								list.add(convs[i]);
								for (int j2 = 0; j2 < convs[i].getOutputColumns().length; j2++) {
									if (!columns.contains(convs[i].getOutputColumns()[j2])) {
										columns.add(convs[i].getOutputColumns()[j2]);
									}
								}
								for (int j2 = 0; j2 < convs[i].getExpectedColumns().length; j2++) {
									if (!required.contains(convs[i].getExpectedColumns()[j2])) {
										required.add(convs[i].getExpectedColumns()[j2]);
									}
								}
								continue main;
							}
						}
					}
				}
			}
		}
		return list;
	}

	private boolean allAvailable(List required, List columns) {
		for (int i = 0; i < required.size(); i++) {
			if (columns.indexOf(required.get(i)) == -1) {
				return false;
			}
		}
		return true;
	}

	public void scheduleStop() {
		this.stop = true;
	}

}
