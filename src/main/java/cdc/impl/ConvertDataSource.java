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
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractResultsSaver;
import cdc.datamodel.DataRow;
import cdc.datamodel.converters.ModelGenerator;
import cdc.impl.resultsavers.CSVFileSaver;
import cdc.utils.RJException;
import edu.emory.mathcs.util.xml.DOMUtils;

public class ConvertDataSource {
	public static void main(String[] args) throws RJException, IOException, SAXException {
		
		if (args.length != 2) {
			System.out.println("Usage: ConvertDataSource xml-source-config out-csv-file");
		}
		
		String fileName = args[0];
		String outFile = args[1];
		
		DocumentBuilder builder = DOMUtils.createDocumentBuilder(false, false);
		Document doc = builder.parse(fileName);
		Node baseNode = doc.getDocumentElement();
		AbstractDataSource source = AbstractDataSource.fromXML((Element)baseNode);
		if (source.getDataModel() == null) {
			source.setModel(new ModelGenerator(source.getAvailableColumns()));
		}
		Map props = new HashMap();
		props.put(CSVFileSaver.OUTPUT_FILE_PROPERTY, outFile);
		props.put(CSVFileSaver.SAVE_SOURCE_NAME, "false");
		props.put(CSVFileSaver.SAVE_CONFIDENCE, "false");
		AbstractResultsSaver saver = new CSVFileSaver(props);
		DataRow row;
		while ((row = source.getNextRow()) != null) {
			saver.saveRow(row);
		}
		saver.close();
		source.close();
	}

	
}
