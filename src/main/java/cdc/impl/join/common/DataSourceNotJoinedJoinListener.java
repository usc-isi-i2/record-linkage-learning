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


package cdc.impl.join.common;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoinCondition;
import cdc.components.JoinListener;
import cdc.datamodel.DataRow;
import cdc.impl.MainApp;
import cdc.impl.resultsavers.CSVFileSaver;
import cdc.utils.RJException;
import cdc.utils.StringUtils;

public class DataSourceNotJoinedJoinListener implements JoinListener {

	private AbstractDataSource source;
	private CSVFileSaver saver;
	private String filePrefix;
	private String fileName;
	
	public DataSourceNotJoinedJoinListener(String filePrefix, AbstractDataSource source) throws RJException {
		this.source = source;
		this.filePrefix = filePrefix;
	}
	
	private void createSaverIfNeeded() throws RJException {
		if (saver != null) {
			return;
		}
		Map props = new HashMap();
		if (!StringUtils.isNullOrEmpty(filePrefix)) {
			props.put(CSVFileSaver.OUTPUT_FILE_PROPERTY, fileName = MainApp.main.getMinusDirectory() + File.separator + filePrefix + "-minus-" + source.getSourceName() + ".csv");
		} else {
			//System.out.println(MainApp.main.getMinusDirectory());
			//System.out.println(source.getSourceName());
			//System.out.println(File.separator);//MainApp.main.getMinusDirectory()
			props.put(CSVFileSaver.OUTPUT_FILE_PROPERTY, fileName = "minus-" + source.getSourceName() + ".csv");
		}
		props.put(CSVFileSaver.SAVE_SOURCE_NAME, "false");
		props.put(CSVFileSaver.SAVE_CONFIDENCE, "false");
		props.put(CSVFileSaver.OUTPUT_FILE_ENCODING, "UTF-8");
		saver = new CSVFileSaver(props);
		System.out.println("[INFO] Saver created: " + props);
	}

	public void rowsJoined(DataRow rowA, DataRow rowB, DataRow row, AbstractJoinCondition condition) {
	}

	public void rowsNotJoined(DataRow rowA, DataRow rowB, int confidence, AbstractJoinCondition condition) {
	}

	public void trashingJoinedTuple(DataRow row) {
	}

	public void trashingNotJoinedTuple(DataRow row) throws RJException {
		createSaverIfNeeded();
		if (row.getSourceName().equals(source.getSourceName())) {
			try {
				createSaverIfNeeded();
				saver.saveRow(row);
			} catch (IOException e) {
				throw new RJException("I/O Error saving minus file", e);
			}
		}
	}

	public void close() throws RJException {
		System.out.println("[INFO] Minus saver closed.");
		if (saver != null) {
			try {
				//saver.flush();
				saver.close();
			} catch (IOException e) {
				throw new RJException("I/O Error closing minus file", e);
			}
			saver = null;
		}
	}

	public void reset() throws RJException {
		if (saver != null) {
			try {
				saver.reset();
			} catch (IOException e) {
				throw new RJException("I/O Error closing minus file", e);
			}
		}
	}
	
	public AbstractDataSource getSource() {
		return source;
	}

	public void joinConfigured() throws RJException {
	}

	public boolean equals(Object arg0) {
		if (!(arg0 instanceof DataSourceNotJoinedJoinListener)) {
			return false;
		}
		DataSourceNotJoinedJoinListener that = (DataSourceNotJoinedJoinListener) arg0;
//		if (that == null || this.filePrefix == null || this.source == null) {
//			return false;
//		}
		return this.filePrefix.equals(that.filePrefix) && this.source.getSourceName().equals(that.source.getSourceName());
	}

	public String getFileName() {
		return fileName;
	}
	
	public String toString() {
		return "DataSourceNotJoinedJoinListener: " + filePrefix + fileName;
	}
	
}
