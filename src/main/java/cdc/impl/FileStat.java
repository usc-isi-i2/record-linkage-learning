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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import au.com.bytecode.opencsv.CSVReader;

public class FileStat {
	
	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.out.println("Usage: FileSorter [file1] [file2] ... [filen]");
			System.exit(0);
		}
		for (int i = 0; i < args.length; i++) {
			System.out.println("Attempting to sort: " + args[i]);
			File f = new File(args[i]);
			if (!f.exists()) {
				System.out.println("File not found: " + args[i]);
				continue;
			}
			File bak = new File(args[i] + ".bak");
			if (bak.exists()) {
				bak.delete();
			}
			f.renameTo(bak);
			
			CSVReader parser = new CSVReader(new FileReader(bak));
			String[][] data = (String[][]) parser.readAll().toArray(new String[][] {});
			
			System.out.println("Sorter read " + data.length + " rows");
			
			parser.close();
			Arrays.sort(data, new Comparator() {
				public int compare(Object arg0, Object arg1) {
					String[] a1 = (String[])arg0;
					String[] a2 = (String[])arg1;
					for (int j = 0; j < a1.length; j++) {
						int cmp = a1[j].compareTo(a2[j]);
						if (cmp != 0) return cmp;
					}
					return 0;
				}
			});
			
			int dups = 0;
			int vals = 0;
			for (int j = 0; j < data.length; j++) {
				vals++;
				if (j + 1 >= data.length) {
					break;
				}
				if (equal(data[j], data[j + 1])) {
					dups++;
					int tmp = j + 1;
					j += 2;
					while (equal(data[tmp], data[j])) {
						j++;
					}
					j--;
				}
				
			}
			System.out.println("Distinct values: " + vals);
			System.out.println("Duplicates: " + dups);
			
		}
		
	}
	
	private static boolean equal(Object arg0, Object arg1) {
		String[] a1 = (String[])arg0;
		String[] a2 = (String[])arg1;
		for (int j = 0; j < a1.length; j++) {
			int cmp = a1[j].compareTo(a2[j]);
			if (cmp != 0) return false;
		}
		return true;
	}
}
