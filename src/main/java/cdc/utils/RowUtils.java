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


package cdc.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cdc.components.AbstractJoin;
import cdc.components.EvaluatedCondition;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.converters.AbstractColumnConverter;
import cdc.gui.components.datasource.JDataSource;
import cdc.impl.datasource.wrappers.propertiescache.CacheInterface;
import cdc.impl.datasource.wrappers.propertiescache.CachedObjectInterface;
import cdc.impl.join.strata.StrataJoinWrapper;

public class RowUtils {
	
	public static DataRow buildMergedRow(AbstractJoin join, DataRow rowA, DataRow rowB, DataColumnDefinition[] outModel, EvaluatedCondition eval) {
		DataCell[] data = new DataCell[outModel.length];
		for (int i = 0; i < data.length; i++) {
			if (rowA.getSourceName().equals(outModel[i].getSourceName())) {
				data[i] = rowA.getData(outModel[i]);
			} else if (rowB.getSourceName().equals(outModel[i].getSourceName())) {
				data[i] = rowB.getData(outModel[i]);
			} else {
				throw new RuntimeException("Undefined datasource: " + outModel[i].getSourceName());
			}
		}
		
		//Create the actual record
		DataRow row = new DataRow(outModel, data);
		Map props = new HashMap(8);
		row.setProperies(props);
		
		//Now we will set some properties of the newly created joined record
		
		//Set linkage confidence and joined property for the joined records
		row.setProperty(AbstractJoin.PROPERTY_CONFIDNCE, String.valueOf(eval.getConfidence()));
		if (eval.isManualReview()) {
			row.setProperty(AbstractJoin.PROPERTY_MANUAL_REVIEW, "true");
			synchronized (rowA) {
				increment(join, rowA, AbstractJoin.PROPERTY_MANUAL_REVIEW_CNT);
			}
			synchronized (rowB) {
				increment(join, rowB, AbstractJoin.PROPERTY_MANUAL_REVIEW_CNT);
			}
		} else {
			synchronized (rowA) {
				rowA.setProperty(AbstractJoin.PROPERTY_JOINED, "true");
			}
			synchronized (rowB) {
				rowB.setProperty(AbstractJoin.PROPERTY_JOINED, "true");
			}
		}
		
		//Set the stratum name
		synchronized (rowA) {
			if (rowA.getProperty(StrataJoinWrapper.PROPERTY_STRATUM_NAME) != null) {
				row.setProperty(StrataJoinWrapper.PROPERTY_STRATUM_NAME, rowA.getProperty(StrataJoinWrapper.PROPERTY_STRATUM_NAME));
			}
		}
		
		synchronized (rowB) {
			if (rowB.getProperty(StrataJoinWrapper.PROPERTY_STRATUM_NAME) != null) {
				row.setProperty(StrataJoinWrapper.PROPERTY_STRATUM_NAME, rowB.getProperty(StrataJoinWrapper.PROPERTY_STRATUM_NAME));
			}
		}
		
		//Set record ids to be able to do results deduplication
		row.setProperty(AbstractJoin.PROPERTY_SRCA_ID, String.valueOf(rowA.getRecordId()));
		row.setProperty(AbstractJoin.PROPERTY_SRCB_ID, String.valueOf(rowB.getRecordId()));
		
		//Below is to be able to save minus after deduplication
		synchronized (rowA) {
			increment(join, rowA, AbstractJoin.PROPERTY_JOIN_MULTIPLICITY);
		}
		synchronized (rowB) {
			increment(join, rowB, AbstractJoin.PROPERTY_JOIN_MULTIPLICITY);
		}
		row.setProperty(AbstractJoin.PROPERTY_RECORD_SRCA, rowA);
		row.setProperty(AbstractJoin.PROPERTY_RECORD_SRCB, rowB);
		
		return row;
	}
	
	public static void linkageManuallyRejected(AbstractJoin join, DataRow row) {
		DataRow rowA = (DataRow) row.getObjectProperty(AbstractJoin.PROPERTY_RECORD_SRCA);
		DataRow rowB = (DataRow) row.getObjectProperty(AbstractJoin.PROPERTY_RECORD_SRCB);
		synchronized (rowA) {
			decrement(join, rowA, AbstractJoin.PROPERTY_JOIN_MULTIPLICITY);
			decrement(join, rowA, AbstractJoin.PROPERTY_MANUAL_REVIEW_CNT);
		}
		synchronized (rowB) {
			decrement(join, rowB, AbstractJoin.PROPERTY_JOIN_MULTIPLICITY);
			decrement(join, rowB, AbstractJoin.PROPERTY_MANUAL_REVIEW_CNT);
		}
	}
	
	public static void linkageManuallyAccepted(AbstractJoin join, DataRow row) {
		DataRow rowA = (DataRow) row.getObjectProperty(AbstractJoin.PROPERTY_RECORD_SRCA);
		DataRow rowB = (DataRow) row.getObjectProperty(AbstractJoin.PROPERTY_RECORD_SRCB);
		synchronized (rowA) {
			decrement(join, rowA, AbstractJoin.PROPERTY_MANUAL_REVIEW_CNT);
			rowA.setProperty(AbstractJoin.PROPERTY_JOINED, "true");
		}
		synchronized (rowB) {
			decrement(join, rowB, AbstractJoin.PROPERTY_MANUAL_REVIEW_CNT);
			rowB.setProperty(AbstractJoin.PROPERTY_JOINED, "true");
		}
	}
	
	public static boolean shouldReportTrashingNotJoined(AbstractJoin join, DataRow dataRow) {
		synchronized (dataRow) {
			if (dataRow.getProperty(AbstractJoin.PROPERTY_JOINED) == null && getValue(join, dataRow, AbstractJoin.PROPERTY_MANUAL_REVIEW_CNT) == 0) {
				return true;
			} else {
				return false;
			}
		}
	}
	
	public static boolean shouldReportTrashingNotJoinedAfterManualReview(AbstractJoin join, DataRow row) {
		synchronized (row) {
			return getValue(join, row, AbstractJoin.PROPERTY_MANUAL_REVIEW_CNT) == 0 && getValue(join, row, AbstractJoin.PROPERTY_JOIN_MULTIPLICITY) == 0;
		}
	}

	
	private static void increment(AbstractJoin join, DataRow row, int cacheId) {
		join.getCache(cacheId).increment(row.getRecordId());
	}
	
	public static int decrement(AbstractJoin join, DataRow row, int cacheId) {
		return join.getCache(cacheId).decrement(row.getRecordId());
	}
	
	public static int getValue(AbstractJoin join, DataRow row, int cacheId) {
		return join.getCache(cacheId).get(row.getRecordId());
	}
	
	public static void resetRow(DataRow row) {
		if (row == null) {
			return;
		}
		synchronized (row) {
			//row.setProperty(AbstractJoin.PROPERTY_JOIN_MULTIPLICITY, null);
			row.setProperty(AbstractJoin.PROPERTY_JOINED, null);
			//row.setProperty(AbstractJoin.PROPERTY_MANUAL_REVIEW_CNT, null);
			row.setProperty(AbstractJoin.PROPERTY_MANUAL_REVIEW, null);
			row.setProperty(AbstractJoin.PROPERTY_CONFIDNCE, null);
		}
	}

	public static int compareRows(DataRow rowA, DataRow rowB, DataColumnDefinition[] sourceAJoinCols, DataColumnDefinition[] sourceBJoinCols) {
		return compareRows(rowA, rowB, sourceAJoinCols, sourceBJoinCols, null);
	}
	
	public static int compareRows(DataRow rowA, DataRow rowB, DataColumnDefinition[] sourceAJoinCols, DataColumnDefinition[] sourceBJoinCols, CompareFunctionInterface[] function) {
		if (function == null) {
			for (int i = 0; i < sourceAJoinCols.length; i++) {
				DataCell cellA = rowA.getData(sourceAJoinCols[i]);
				DataCell cellB = rowB.getData(sourceBJoinCols[i]);
				int cmp = cellA.compareTo(cellB);
				if (cmp != 0) {
					return cmp;
				}
			}
		} else {
			for (int i = 0; i < sourceAJoinCols.length; i++) {
				DataCell cellA = rowA.getData(sourceAJoinCols[i]);
				DataCell cellB = rowB.getData(sourceBJoinCols[i]);
				int cmp = function[i].compare(cellA, cellB);
				if (cmp != 0) {
					return cmp;
				}
			}
		}
		return 0;
	}

	public static DataRow buildSubrow(DataRow row, DataColumnDefinition[] model) {
		return buildSubrow(row, model, false);
	}
	
	public static DataRow buildSubrow(DataRow row, DataColumnDefinition[] model, boolean copyParams) {
		DataCell[] cells = new DataCell[model.length];
		for (int i = 0; i < cells.length; i++) {
			cells[i] = row.getData(model[i]);
		}
		DataRow newR = new DataRow(model, cells, row.getSourceName());
		if (copyParams) {
			newR.setProperies(row.getProperties());
		}
		return newR;
	}

	public static void fixConverter(AbstractColumnConverter conv, JDataSource model, int convId) {
		DataColumnDefinition[] columns = conv.getOutputColumns();
		boolean switched = true;
		while (switched) {
			switched = false;
			for (int i = columns.length - 1; i >= 0; i--) {
				for (int j = columns.length - 1 ; j >= 0; j--) {
					if (i != j && columns[i].getColumnName().equals(columns[j].getColumnName())) {
						switched = true;
						columns[i].setName(columns[i].getColumnName() + "_1");
					}
				}
			}
		}
		
		for (int i = 0; i < columns.length; i++) {
			int n = 1;
			String name = columns[i].getColumnName();
			while (isConflict(conv, name, model, convId)) {
				name = columns[i].getColumnName() + "_" + n;
				n++;
			}
			columns[i].setName(name);
		}
	}
	
	public static void fixConverter(AbstractColumnConverter conv, JDataSource model) {
		fixConverter(conv, model, -1);
	}

	private static boolean isConflict(AbstractColumnConverter tested, String name, JDataSource model, int convId) {
		JDataSource.Connection[] cons = model.getConnections();
		for (int i = 0; i < cons.length; i++) {
			if (i == convId) {
				continue;
			}
			JDataSource.Brick[] alreadyIn = cons[i].to;
			for (int j = 0; j < alreadyIn.length; j++) {
				if (alreadyIn[j].col.getColumnName().equals(name)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static byte[] rowToByteArray(CacheInterface propsCache, DataRow row, DataColumnDefinition[] rowModel) throws IOException {
		ByteArrayOutputStream array = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(array);
		for (int i = 0; i < rowModel.length; i++) {
			DataCell cell = row.getData(rowModel[i]);
			oos.writeObject(cell.getValue());
			oos.writeInt(cell.getValueType());
		}
		if (row.getProperties() != null) {
			oos.writeBoolean(true);
			Map props = row.getProperties();
			oos.writeInt(props.size());
			for (Iterator iterator = props.keySet().iterator(); iterator.hasNext();) {
				String key = (String) iterator.next();
				Object val = props.get(key);
				if (propsCache != null && val instanceof DataRow) {
					oos.writeObject(key);
					oos.writeObject(propsCache.cacheObject((DataRow) val));
				} else {
					oos.writeObject(key);
					oos.writeObject(val);
				}
			}
			//oos.writeObject(row.getProperties());
		} else {
			oos.writeBoolean(false);
		}
		oos.flush();
		byte[] bytes = array.toByteArray();
		return bytes;
	}
		
	public static DataRow byteArrayToDataRow(CacheInterface propsCache, byte[] b, DataColumnDefinition[] columns, String sourceName) throws IOException, RJException {
		try {
			ByteArrayInputStream array = new ByteArrayInputStream(b);
			ObjectInputStream ois = new ObjectInputStream(array);
			try {
				DataCell cells[] = new DataCell[columns.length];
				for (int i = 0; i < cells.length; i++) {
					Object val = ois.readObject();
					int type = ois.readInt();
					cells[i] = new DataCell(type, val);
				}
				DataRow row = new DataRow(columns, cells, sourceName);
				if (ois.readBoolean()) {
					int size = ois.readInt();
					Map props = new HashMap(8);
					for (int i = 0; i < size; i++) {
						 Object key = ois.readObject();
						 Object val = ois.readObject();
						 if (propsCache != null && val instanceof CachedObjectInterface) {
							 props.put(key, propsCache.getObject((CachedObjectInterface) val));
						 } else {
							 props.put(key, val);
						 }
					}
					row.setProperies(props);
					//row.setProperies((Map) ois.readObject());
				}
				return row;
			} catch (ClassNotFoundException e) {
				throw new RJException("Error reading input file", e);
			}
		} catch (EOFException e) {
			return null;
		}
	}

	
	public static DataColumnDefinition[] getModelForSave(DataColumnDefinition[][] usedModel) {
		List l = new ArrayList();
		for (int i = 0; i < usedModel[0].length; i++) {
			for (int j = 0; j < usedModel.length; j++) {
				if (usedModel[j][i] != null) {
					l.add(usedModel[j][i]);
				}
			}
		}
		return (DataColumnDefinition[]) l.toArray(new DataColumnDefinition[] {});
	}

	public static DataRow copyRow(DataRow dataRow) {
		DataColumnDefinition[] cols = dataRow.getRowModel();
		DataCell[] columns = new DataCell[cols.length];
		for (int i = 0; i < columns.length; i++) {
			DataCell original = dataRow.getData(cols[i]);
			columns[i] = new DataCell(original.getValueType(), original.getValue());
		}
		DataRow copy = new DataRow(cols, columns, dataRow.getSourceName());
		for (Iterator iterator = dataRow.getProperties().keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			copy.setProperty(key, dataRow.getProperty(key));
		}
		return copy;
	}

}
