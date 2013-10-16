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


package cdc.impl.resultsavers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JPanel;

import au.com.bytecode.opencsv.CSVWriter;
import cdc.components.AbstractJoin;
import cdc.components.AbstractResultsSaver;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataRow;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.paramspanel.FileChoosingPanelFieldCreator;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.impl.join.strata.StrataJoinWrapper;
import cdc.utils.Log;
import cdc.utils.RJException;
import cdc.utils.Utils;

public class CSVFileSaver extends AbstractResultsSaver {

	public static final String DEFAULT_FILE = "results.csv";
	public static final String DEFAULT_ENCODING = "UTF-8";
	public static final String OUTPUT_FILE_PROPERTY = "output-file";
	public static final String OUTPUT_FILE_ENCODING = "encoding";
	public static final String SAVE_SOURCE_NAME = "save-source-name";
	public static final String SAVE_CONFIDENCE = "save-confidence";
	
	private static class CSVFileSaverVisibleComponent extends GUIVisibleComponent {
		
		private ParamsPanel panel;
		
		public Object generateSystemComponent() throws RJException, IOException {
			Map params = panel.getParams();
			String fileName = (String) params.get(OUTPUT_FILE_PROPERTY);
			String[] name = Utils.parseFilePath(fileName);
			if (!name[0].endsWith(".csv")) {
				name[0] = name[0] + ".csv";
			}
			params.put(OUTPUT_FILE_PROPERTY, name[0]);
			if (name.length == 2) {
				params.put(OUTPUT_FILE_ENCODING, name[1]);
			}
			
			return new CSVFileSaver(params);
		}
		public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
			String file = DEFAULT_FILE;
			String enc = getRestoredParam(OUTPUT_FILE_PROPERTY) == null ? DEFAULT_ENCODING : "US-ASCII";
			if (getRestoredParam(OUTPUT_FILE_PROPERTY) != null) {
				file = getRestoredParam(OUTPUT_FILE_PROPERTY);
			}
			if (getRestoredParam(OUTPUT_FILE_ENCODING) != null) {
				enc = getRestoredParam(OUTPUT_FILE_ENCODING);
			}
			String[] defs = new String[] {file + "#ENC=" + enc + "#"};
			Map map = new HashMap();
			map.put(OUTPUT_FILE_PROPERTY, new FileChoosingPanelFieldCreator(FileChoosingPanelFieldCreator.SAVE));
			panel = new ParamsPanel(
					new String[] {OUTPUT_FILE_PROPERTY},
					new String[] {"Output file"},
					defs,
					map
			);
			
			return panel;
		}
		public Class getProducedComponentClass() {
			return CSVFileSaver.class;
		}
		public String toString() {
			return "CSV file data saver";
		}
		public boolean validate(JDialog dialog) {
			return true;
		}
	}
	
	private File file;
	private Charset encoding = Utils.DEFAULT_ENCODING.getCharset();
	private CSVWriter printer;
	private boolean saveConfidence = true;
	private boolean closed = false;
	private boolean saveSourceName = true;
	
	public CSVFileSaver(Map properties) throws RJException {
		super(properties);
		if (!properties.containsKey(OUTPUT_FILE_PROPERTY)) {
			file = new File(DEFAULT_FILE);
		} else {
			file = new File((String) properties.get(OUTPUT_FILE_PROPERTY));
		}
		if (properties.containsKey(SAVE_SOURCE_NAME)) {
			saveSourceName = Boolean.parseBoolean((String)properties.get(SAVE_SOURCE_NAME));
		}
		if (properties.containsKey(OUTPUT_FILE_ENCODING)) {
			encoding = Utils.getEncodingForName((String)properties.get(OUTPUT_FILE_ENCODING)).getCharset();
		}
		Log.log(getClass(), "Saver created. Encoding=" + encoding, 2);
		if (file.exists() && !file.isFile()) {
			throw new RJException("Output file cannot be directory or other special file");
		}
		if (properties.containsKey(SAVE_CONFIDENCE)) {
			saveConfidence = properties.get(SAVE_CONFIDENCE).equals("true");
		}
	}
	
	public void saveRow(DataRow row) throws RJException, IOException {	
		String stratum = row.getProperty(StrataJoinWrapper.PROPERTY_STRATUM_NAME);
		if (printer == null) {
			
			printer = new CSVWriter(new BufferedWriter(Utils.openTextFileForWriting(file, encoding)));
			String[] header = new String[row.getData().length + (saveConfidence ? 1 : 0) + (stratum != null?1:0)];
			for (int i = 0; i < header.length - (stratum != null?1:0) - (saveConfidence ? 1 : 0); i++) {
				if (saveSourceName) {
					header[i] = row.getRowModel()[i].toString();
				} else {
					header[i] = row.getRowModel()[i].getColumnName();
				}
			}
			if (stratum != null) {
				header[header.length - (saveConfidence ? 2 : 1)] = "Stratum name";
			}
			if (saveConfidence) {
				header[header.length - 1] = "Confidence";
			}
			printer.writeNext(header);
		}
		DataCell[] cells = row.getData();
		String[] strRow = new String[cells.length + (saveConfidence ? 1 : 0) + (stratum != null ? 1 : 0)];
		for (int i = 0; i < strRow.length - (stratum != null ? 1 : 0) - (saveConfidence ? 1 : 0); i++) {
			strRow[i] = cells[i].getValue().toString();
		}
		if (stratum != null) {
			strRow[strRow.length - (saveConfidence ? 2 : 1)] = stratum;
		}
		if (saveConfidence) {
			strRow[strRow.length - 1] = row.getProperty(AbstractJoin.PROPERTY_CONFIDNCE);
		}
		printer.writeNext(strRow);

	}
	
	public void flush() throws IOException {
		if (printer != null) {
			printer.flush();
		}
	}

	public void close() throws IOException {
		Log.log(getClass(), "Close in CSV saver for file " + file);
		if (closed) {
			return;
		}
		closed = true;
		if (printer != null) {
			printer.flush();
			printer.close();
			printer = null;
		}
	}
	
	public void reset() throws IOException {
		if (printer != null) {
			printer.close();
			printer = null;
		}
		closed = false;
	}
	
	//Method moved to AbstractResultsSaver
//	public static AbstractResultsSaver fromXML(Element element) throws RJException {
//		Element params = DOMUtils.getChildElement(element, Configuration.PARAMS_TAG);
//		Map parameters = null;
//		if (params != null) {
//			parameters = Configuration.parseParams(params);
//		}
//		return new CSVFileSaver(parameters);
//	}

	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new CSVFileSaverVisibleComponent();
	}
	
	public String toString() {
		return "CSV file saver";
	}
	
	public String toHTMLString() {
		return "CSV result saver (file=" + file.getName() + ")";
	}

	public String getActiveDirectory() {
		return new File(file.getAbsolutePath()).getParent();
	}

	public boolean isClosed() {
		return closed;
	}
}
