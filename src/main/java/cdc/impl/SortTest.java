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
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class SortTest {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		//List list = new ArrayList();
		
		BufferedReader reader = new BufferedReader(new java.io.FileReader("data/last.csv"));
		BufferedWriter writer = new BufferedWriter(new FileWriter("data/last1.csv"));
		//CSVPrinter printer = new CSVPrinter(writer);
		String line = null;
		while ((line = reader.readLine()) != null) {
//			String[] row = new String[3];
//			row[0] = line.substring(12, 37).trim() + " " + line.substring(38, 63).trim();
//			row[1] = line.substring(93, 95);
//			row[2] = line.substring(96, 100);

			writer.write(line);
			writer.write("\n");
			//printer.println(row);
		}
//		System.out.println("Read done...");
//		Collections.sort(list, new Comparator() {
//			public int compare(Object arg0, Object arg1) {
//				return ((String)arg0).toLowerCase().replace((char)126, (char)64).replace((char)58, (char)47).replace((char)93, (char)47).replace((char)45, (char)45).
//						compareTo(((String)arg1).toLowerCase().replace((char)126, (char)64).replace((char)58, (char)47).replace((char)93, (char)47).replace((char)45, (char)45));
//			}
//		});
//		
//		System.out.println("Sort done...");
//		
//		
//		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
//			String name = (String) iterator.next();
//			writer.write(name);
//			writer.write("\n");
//		}
//		printer.flush();
//		printer.close();
		writer.flush();
		writer.close();
		reader.close();
	}

}
