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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DateUtils {

	private static String[] YEAR = {"yy", "yyyy"};
	private static String[] MONTH = {"MM", "MMM", "MMM"};
	private static String[] DAY = {"dd"};
	
	private static String[] HOUR = {"HH", "KK", "hh", "kk"};
	private static String[] MINTE = {"mm"};
	private static String[] SECOND = {"ss"};
	
	private static String[] sepsDate = {"/", "\\", "-", ".", " ", ""};
	private static String[] sepsDateFromTime = {":", ""};
	private static String[] sepsTime = {":", ""};
	
	private static String[][][] testedFormats = {
			new String[][] {YEAR, sepsDate, MONTH, sepsDate, DAY},
			new String[][] {DAY, sepsDate, MONTH, sepsDate, YEAR},
			new String[][] {MONTH, sepsDate, DAY, sepsDate, YEAR},
			new String[][] {YEAR, sepsDate, MONTH, sepsDate, DAY, sepsDateFromTime, HOUR, sepsTime, MINTE, sepsTime, SECOND},
			new String[][] {DAY, sepsDate, MONTH, sepsDate, YEAR, sepsDateFromTime, HOUR, sepsTime, MINTE, sepsTime, SECOND},
			new String[][] {MONTH, sepsDate, DAY, sepsDate, YEAR, sepsDateFromTime, HOUR, sepsTime, MINTE, sepsTime, SECOND},
			new String[][] {HOUR, sepsTime, MINTE, sepsTime, SECOND, sepsDateFromTime, MONTH, sepsDate, DAY, sepsDate, YEAR},
			new String[][] {HOUR, sepsTime, MINTE, sepsTime, SECOND, sepsDateFromTime, DAY, sepsDate, MONTH, sepsDate, YEAR},
			new String[][] {HOUR, sepsTime, MINTE, sepsTime, SECOND, sepsDateFromTime, YEAR, sepsDate, MONTH, sepsDate, DAY},
			new String[][] {HOUR, sepsTime, MINTE, sepsTime, SECOND}
			
	};
	
	private static ThreadLocal tested = new ThreadLocal();
	private static ThreadLocal params = new ThreadLocal();
	
	private static String[] generateTestedFormats() {
		if (tested.get() == null) {
			List lTested = new ArrayList();
			for (int i = 0; i < testedFormats.length; i++) {
				params.set(new String[testedFormats[i].length]);
				lTested.addAll(Arrays.asList(generate("", testedFormats[i], 0)));
			}
			tested.set(lTested.toArray(new String[] {}));
		}
		return (String[])tested.get();
	}
	
	private static String[] generate(String seed, String[][] pattern, int level) {
		List results = new ArrayList();
		if (pattern.length == level) {
			return new String[] {seed};
		} else {
			int fixed = -1;
			for (int i = 0; i < level; i++) {
				if (pattern[i] == pattern[level]) {
					fixed = i;
					break;
				}
			}
			if (fixed != -1) {
				String newSeed = seed + ((String[])params.get())[fixed];
				results.addAll(Arrays.asList(generate(newSeed, pattern, level + 1)));
			} else {
				for (int i = 0; i < pattern[level].length; i++) {
					String newSeed = seed + pattern[level][i];
					((String[])params.get())[level] = pattern[level][i];
					results.addAll(Arrays.asList(generate(newSeed, pattern, level + 1)));
				}
			}
		}
		return (String[]) results.toArray(new String[] {});
	}

	public static String[] parse(String s) {
		String[] forms = generateTestedFormats();
		List match = new ArrayList();
		for (int i = 0; i < forms.length; i++) {
			SimpleDateFormat format = new SimpleDateFormat(forms[i]);
			format.setLenient(false);
			 
			try {
				format.parse(s);
				match.add(forms[i]);
			} catch (Exception e) {
				continue;
			}
		}
		
//		boolean fourDigitYear = false;
//		for (Iterator iterator = match.iterator(); iterator.hasNext();) {
//			String format = (String) iterator.next();
//			if (format.matches(".*yyyy.*")) {
//				fourDigitYear = true;
//			}
//		}
//		
//		if (fourDigitYear) {
//			List toRemove = new ArrayList();
//			for (Iterator iterator = match.iterator(); iterator.hasNext();) {
//				String format = (String) iterator.next();
//				if (!format.matches(".*yyyy.*")) {
//					toRemove.add(format);
//				}
//			}
//			match.removeAll(toRemove);
//		}
		
		return (String[]) match.toArray(new String[] {});
	}
	
//	private final static String wtb[] = { "am", "pm", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
//			"january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december",
//			"gmt", "ut", "utc", "est", "edt", "cst", "cdt", "mst", "mdt", "pst", "pdt" };
//
//	private final static int ttb[] = { 14, 1, 0, 0, 0, 0, 0, 0, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 10000 + 0, 10000 + 0, 10000 + 0, // GMT/UT/UTC
//			10000 + 5 * 60, 10000 + 4 * 60, // EST/EDT
//			10000 + 6 * 60, 10000 + 5 * 60, // CST/CDT
//			10000 + 7 * 60, 10000 + 6 * 60, // MST/MDT
//			10000 + 8 * 60, 10000 + 7 * 60 // PST/PDT
//	};
//
//	public static String[] parse(String s) {
//		
//		boolean hhAmPm = false;
//		
//		int year = Integer.MIN_VALUE;
//		int mon = -1;
//		int mday = -1;
//		int hour = -1;
//		int min = -1;
//		int sec = -1;
//		int c = -1;
//		int i = 0;
//		int n = -1;
//		int tzoffset = -1;
//		int prevc = 0;
//		
//		StringBuilder[] format = new StringBuilder[] {new StringBuilder()};
//		
//		syntax: {
//			if (s == null)
//				break syntax;
//			int limit = s.length();
//			while (i < limit) {
//				c = s.charAt(i);
//				i++;
//				if (c <= ' ' || c == ',') {
//					append(format, String.valueOf((char)c));
//					continue;
//				} if (c == '(') { // skip comments
//					append(format, String.valueOf((char)c));
//					int depth = 1;
//					while (i < limit) {
//						c = s.charAt(i);
//						append(format, String.valueOf((char)c));
//						i++;
//						if (c == '(')
//							depth++;
//						else if (c == ')')
//							if (--depth <= 0)
//								break;
//					}
//					continue;
//				}
//				if ('0' <= c && c <= '9') {
//					n = c - '0';
//					while (i < limit && '0' <= (c = s.charAt(i)) && c <= '9') {
//						n = n * 10 + c - '0';
//						i++;
//					}
//					if (prevc == '+' || prevc == '-' && year != Integer.MIN_VALUE) {
//						// timezone offset
//						if (n < 24) {
//							n = n * 60; // EG. "GMT-3"
//							append(format, "z");
//						} else {
//							n = n % 100 + n / 100 * 60; // eg "GMT-0430"
//							append(format, "z");
//						} if (prevc == '+') { // plus means east of GMT
//							n = -n;
//						}
//						if (tzoffset != 0 && tzoffset != -1)
//							break syntax;
//						tzoffset = n;
//					} else if (n >= 70) {
//						if (year != Integer.MIN_VALUE)
//							break syntax;
//						else if (c <= ' ' || c == ',' || c == '/' || i >= limit) {
//							// year = n < 1900 ? n : n - 1900;
//							year = n;
//							if (n < 100) {
//								append(format, "yy");
//							} else {
//								append(format, "yyyy");
//							}
//						} else
//							break syntax;
//					} else if (c == ':') {
//						if (hour < 0) {
//							hour = (byte) n;
//							if (hhAmPm) {
//								if (hour == 0) {
//									append(format, "KK");
//								} else if (hour == 12) {
//									append(format, "hh");
//								} else {
//									format = append(format, new String[] {"KK", "hh"});
//								}
//							} else {
//								if (hour == 0) {
//									append(format, "HH");
//								} else if (hour == 24) {
//									append(format, "kk");
//								} else {
//									format = append(format, new String[] {"HH", "kk"});
//								}
//							}
//						} else if (min < 0) {
//							append(format, "mm");
//							min = (byte) n;
//						} else
//							break syntax;
//					} else if (c == '/') {
//						if (mon < 0) {
//							append(format, "MM");
//							mon = (byte) (n - 1);
//						} else if (mday < 0) {
//							append(format, "DD");
//							mday = (byte) n;
//						} else
//							break syntax;
//					} else if (i < limit && c != ',' && c > ' ' && c != '-') {
//						break syntax;
//					} else if (hour >= 0 && min < 0) {
//						append(format, "mm");
//						min = (byte) n;
//					} else if (min >= 0 && sec < 0) {
//						append(format, "ss");
//						sec = (byte) n;
//					} else if (mday < 0) {
//						append(format, "dd");
//						mday = (byte) n;
//					// Handle two-digit years < 70 (70-99 handled above).
//					} else if (year == Integer.MIN_VALUE && mon >= 0 && mday >= 0) {
//						year = n;
//						if (n < 100) {
//							append(format, "yy");
//						} else {
//							append(format, "yyyy");
//						}
//					} else {
//						break syntax;
//					}
//					prevc = 0;
//				} else if (c == '/' || c == ':' || c == '+' || c == '-') {
//					prevc = c;
//					append(format, String.valueOf((char)c));
//				} else {
//					int st = i - 1;
//					while (i < limit) {
//						c = s.charAt(i);
//						if (!('A' <= c && c <= 'Z' || 'a' <= c && c <= 'z'))
//							break;
//						i++;
//					}
//					if (i <= st + 1)
//						break syntax;
//					int k;
//					for (k = wtb.length; --k >= 0;)
//						if (wtb[k].regionMatches(true, 0, s, st, i - st)) {
//							int action = ttb[k];
//							if (action != 0) {
//								if (action == 1) { // pm
//									hhAmPm = true;
//									if (hour == 0) {
//										replace(format, new String[] {"H"}, new String[] {"K"});
//									} else if (hour == 12) {
//										replace(format, new String[] {"H", "k"}, new String[] {"h", "h"});
//									} else {
//										replace(format, new String[] {"H", "k"}, new String[] {"K", "h"});
//									}
//									if (hour > 12 || hour < 1)
//										break syntax;
//									else if (hour < 12)
//										hour += 12;
//								} else if (action == 14) { // am
//									hhAmPm = true;
//									if (hour == 0) {
//										replace(format, new String[] {"H"}, new String[] {"K"});
//									} else if (hour == 12) {
//										replace(format, new String[] {"H", "k"}, new String[] {"h", "h"});
//									} else {
//										replace(format, new String[] {"H", "k"}, new String[] {"K", "h"});
//									}
//									if (hour > 12 || hour < 1)
//										break syntax;
//									else if (hour == 12)
//										hour = 0;
//								} else if (action <= 13) { // month!
//									append(format, "MMM");
//									if (mon < 0)
//										mon = (byte) (action - 2);
//									else
//										break syntax;
//								} else {
//									tzoffset = action - 10000;
//								}
//							}
//							break;
//						}
//					if (k < 0)
//						break syntax;
//					prevc = 0;
//				}
//			}
//			if (year == Integer.MIN_VALUE || mon < 0 || mday < 0)
//				break syntax;
//			
//			String[] strs = new String[format.length];
//			for (int j = 0; j < strs.length; j++) {
//				strs[j] = format[j].toString();
//			}
//			return strs;
//		}
//		// syntax error
//		throw new IllegalArgumentException();
//	}
//	
//	private static void replace(StringBuilder[] format, String string[], String string2[]) {
//		for (int i = 0; i < format.length; i++) {
//			for (int j = 0; j < string.length; j++) {
//				format[i] = new StringBuilder(format[i].toString().replaceAll(string[j], string2[j]));
//			}
//		}
//	}
//
//	private static StringBuilder[] append(StringBuilder[] formats, String[] strings) {
//		StringBuilder[] b = new StringBuilder[formats.length * strings.length];
//		for (int i = 0; i < b.length; i++) {
//			b[i] = new StringBuilder();
//			b[i].append(formats[i % formats.length]);
//			b[i].append(strings[i % strings.length]);
//		}
//		return b;
//	}
//
//	private static void append(StringBuilder[] formats, String string) {
//		for (int i = 0; i < formats.length; i++) {
//			formats[i].append(string);
//		}
//	}
//	
	public static void main(String[] args) throws ParseException {
		
		String test[] = new String[] {"10/24/2008", "10/12/2006 22:23:12", "12/10/2006 11:23:22 AM", "June 12 2006 11:23:22", "12 Jun 2006 11:23:22", "12Jun2006 11:23:22", "12 June 2006 11:23:22", "13:42:44 10/12/99", "24:43:00 99/12/01"};
		
		
		for (int i = 0; i < test.length; i++) {
			String[] parsed = parse(test[i]);
			System.out.println(test[i] + "->" + PrintUtils.printArray(parsed));
//			for (int j = 0; j < parsed.length; j++) {
//				SimpleDateFormat format = new SimpleDateFormat(parsed[j]);
//				System.out.println("Parsing " + test[i] + " using format: " + parsed[j] + "-->" + format.parse(test[i]));
//			}
			
			
		}
		
	}

}
