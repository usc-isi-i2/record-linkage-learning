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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class Log {

	private static final String LOG_PROPERTIES = "log.properties";
	
	private static Properties levels = null;
	private static Map bufferedLevels = new HashMap();
	private static List sinks = new ArrayList();
	
	private static SimpleDateFormat format;
	static {
		String f = Props.getString("log-date-format");
		if (f == null || f.isEmpty()) {
			f = "hh:mm:ss";
		}
		format = new SimpleDateFormat(f);
	}
	
	public static final class PrintSink extends LogSink {
		public synchronized void log(String msg) {
			System.out.println(msg);
		}
	}
	
	public static final class FileSink extends LogSink {
		
		BufferedOutputStream os;
		
		public FileSink(String file) {
			try {
				os = new BufferedOutputStream(new FileOutputStream(file));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public synchronized void log(String msg) {
			try {
				if (os == null) {
					System.out.println("Log skips writing to file (no stream): " + msg);
				} else {
					os.write(msg.getBytes());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void close() {
			try {
				if (os != null) {
					os.flush();
					os.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	static {
		File f = new File(LOG_PROPERTIES);
		if (f.exists()) {
			if (!f.canRead()) {
				System.out.println("[ERROR] Log.properties cannot be read!");
			} else {
				levels = new Properties();
				try {
					levels.load(new FileInputStream(f));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		sinks.add(new PrintSink());
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("[INFO] Closing log.");
				synchronized (sinks) {
					for (Iterator iterator = sinks.iterator(); iterator.hasNext();) {
						LogSink sink = (LogSink) iterator.next();
						sink.close();
					}
				}
			}
		});
	}
	
	public static int getLogLevel(Class class1) {
		if (levels == null) {
			return 1;
		}
		int level = 0;
		synchronized (bufferedLevels) {
			if (bufferedLevels.containsKey(class1)) {
				return ((Integer)bufferedLevels.get(class1)).intValue();
			}
			int longestMatch = 0;
			for (Iterator iterator = levels.keySet().iterator(); iterator.hasNext();) {
				String pName = (String) iterator.next();
				if (longestMatch < pName.length() && class1.getName().startsWith(pName)) {
					longestMatch = pName.length();
					level = Integer.parseInt(levels.getProperty(pName));
				}
	 		}
			bufferedLevels.put(class1, new Integer(level));
		}
		return level;
	}

	public static void log(Class class1, String string, int level) {
		if (getLogLevel(class1) >= level) {
			synchronized (sinks) {
				for (Iterator iterator = sinks.iterator(); iterator.hasNext();) {
					LogSink sink = (LogSink) iterator.next();
					sink.log(getMessage(class1.getName(), string));
				}
			}
		}
	}

	public static void log(Class class1, String string) {
		log(class1, string, 1);
	}

	public static void addSink(LogSink sink) {
		synchronized (sinks) {
			sinks.add(sink);
		}
	}
	
	public static void setSinks(LogSink[] sinks) {
		synchronized (sinks) {
			Log.sinks.clear();
			Log.sinks.addAll(Arrays.asList(sinks));
		}
	}

	public static Properties getProperties() {
		return (Properties) levels.clone();
	}
	
	public static void saveProperties(Properties properties) throws FileNotFoundException, IOException {
		properties.store(new FileOutputStream(LOG_PROPERTIES), null);
	}
	
	public static void logToFile(String file) {
		sinks.clear();
		sinks.add(new FileSink(file));
	}
	
	private static String getMessage(String className, String string) {
		Date d = new Date();
		StringBuffer b = new StringBuffer();
		b.append("[").append(format.format(d)).append("] ");
		b.append(className);
		b.append(": ");
		b.append(string);
		return b.toString();
	}
	
}
