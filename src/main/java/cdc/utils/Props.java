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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Props {
	
	public static final String PROPERTIES_FILE = "misc.properties";
	
	private static Properties properties;
	private static boolean print = true;
	
	static {
		properties = new Properties();
		try {
			properties.load(new FileInputStream(PROPERTIES_FILE));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error reading misc.properties");
		}
	}
	
	private static String readPropertyGuaranteed(String name) {
		String prop = readProperty(name);
		if (prop == null) {
			throw new RuntimeException("Please check file misc.properties. It appears that the property " + name + " is not set.");
		}
		return prop;
	}
	
	private static String readProperty(String name) {
		String prop = (String)properties.get(name);
		if (print) {
			System.out.println("[INFO] Retrieved property: " + name + "=" + prop);
		}
		return prop;
	}
	
	public static int getInteger(String name, int defVal) {
		String val = readProperty(name);
		return val == null ? defVal : Integer.parseInt(val);
	}
	
	public static double getDouble(String name, double defVal) {
		String val = readProperty(name);
		return val == null ? defVal : Double.parseDouble(val);
	}
	
	public static long getLong(String name, long defVal) {
		String val = readProperty(name);
		return val == null ? defVal : Long.parseLong(val);
	}
	
	public static String getString(String name, String defVal) {
		String val = readProperty(name);
		return val == null ? defVal : val;
	}
	
	public static int getInteger(String name) {
		return Integer.parseInt(readPropertyGuaranteed(name));
	}
	
	public static double getDouble(String name) {
		return Double.parseDouble(readPropertyGuaranteed(name));
	}
	
	public static long getLong(String name) {
		return Long.parseLong(readPropertyGuaranteed(name));
	}
	
	public static String getString(String name) {
		return readProperty(name);
	}
	
	public static Properties getProperties() {
		return (Properties) properties.clone();
	}
	
	public static void saveProperties(Properties newProperties) throws FileNotFoundException, IOException {
		newProperties.store(new FileOutputStream(PROPERTIES_FILE), null);
	}

	public static boolean getBoolean(String name) {
		return Boolean.parseBoolean(readPropertyGuaranteed(name));
	}
	
	public static boolean getBoolean(String string, boolean defaultValue) {
		String value = readProperty(string);
		if (value == null) {
			return defaultValue;
		} else {
			return Boolean.parseBoolean(value);
		}
	}
	
	public static void disablePrinting() {
		print = false;
	}
}
