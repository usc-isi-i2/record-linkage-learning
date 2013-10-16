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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiComponentsConfigParser {
	
	public static String CONFIG_FILE_NAME = "gui-components.config";
	
	public static String sources = "sources";
	public static String joins = "joins";
	public static String savers = "savers";
	public static String converters = "converters";
	public static String distances = "distances";
	public static String joinCondition = "join-conditions";
	
	private static String comment = "#";
	private static String endToken = "end";
	private static String itemizeToken = ":";
	private static String spaces = " \n\t\r";
	private static String endCommand = ";";
	
	private static Map elements = new HashMap();
	
	private static BufferedReader configReader = null;
	private static String nextToken = null;
	private static char bufferedChar = 255;
	static {
		try {
			configReader = new BufferedReader(new FileReader(CONFIG_FILE_NAME));
			Element e = null;
			while ((e = getNextElement()) != null) {
				elements.put(e.value, e);
			}
		} catch (FileNotFoundException e) {
			System.out.println("ERROR: Mandatory config file missing: " + CONFIG_FILE_NAME);
			System.exit(-1);
		} catch (IOException e) {
			System.out.println("ERROR reading config file: " + CONFIG_FILE_NAME);
			System.exit(-1);
		}
	}
	
	public static class Element {
		public String value;
		public Element[] children;
		
		public String toString() {
			return "Element: " + value + " [" + PrintUtils.printArray(children) + "]";
		}
	}
	
	private static Element getNextElement() throws IOException {
		boolean ended = false;
		boolean started = false;
		Element newEl = new Element();
		while (!ended) {
			String token = getNextToken();
			if (token == null && started) {
				System.out.println("ERROR in file " + CONFIG_FILE_NAME + ": Unexpected end of file.");
				System.exit(-1);
			} else if (token == null && !started) {
				ended = true;
				return null;
			} else {
				if (token.equals(endCommand)) {
					ended = true;
					consumeNextToken();
					break;
				} else if (token.equals(itemizeToken)) {
					consumeNextToken();
					List children = new ArrayList();
					while (!endToken.equals(getNextToken())) {
						children.add(getNextElement());
					}
					newEl.children = (Element[])children.toArray(new Element[] {});
					consumeNextToken();
				} else {
					started = true;
					newEl.value = token;
					consumeNextToken();
				}
			}
		}
		return newEl;
	}
	
	private static String getNextToken() throws IOException {
		if (nextToken != null) {
			return nextToken;
		}
		StringBuffer buffer = new StringBuffer();
		char buf;
		while ((buf = getNextChar()) != 255) {
			if (comment.charAt(0) == buf) {
				while ((buf = getNextChar()) != '\n');
			}
			if (spaces.indexOf(buf) != -1 || endCommand.charAt(0) == buf || itemizeToken.charAt(0) == buf) {
				if (spaces.indexOf(buf) == -1) {
					if (buffer.length() != 0) {
						bufferedChar = buf;
					} else {
						buffer.append(buf);
					}
					break;
				}
			} else {
				buffer.append(buf);
			}
		}
		if (buffer.length() == 0) return null;
		return nextToken = buffer.toString();
	}

	private static char getNextChar() throws IOException {
		if (bufferedChar != 255) {
			char tmp = bufferedChar;
			bufferedChar = 255;
			return tmp;
		}
		char[] buf = new char[1];
		if (configReader.read(buf) == 0) {
			return 255;
		}
		if (buf[0] == 0) return 255;
		return buf[0];
	}

	private static void consumeNextToken() {
		nextToken = null;
	}

	public static Element getElementByName(String name) {
		return (Element) elements.get(name);
	}
	
}
