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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.components.AbstractResultsSaver;
import cdc.components.LinkageSummary;
import cdc.configuration.ConfiguredSystem;
import cdc.gui.MainFrame;
import cdc.gui.SavedConfigManager;
import cdc.impl.MainApp;
import cdc.impl.deduplication.DeduplicationDataSource;
import cdc.impl.resultsavers.DeduplicatingResultsSaver;

public class Utils {

	public static Properties mapToProperties(Map params) {
		Properties p = new Properties();
		for (Iterator iterator = params.keySet().iterator(); iterator.hasNext();) {
			String name = (String) iterator.next();
			String value = (String) params.get(name);
			if (value != null) {
				p.setProperty(name, value);
			}
		}
		return p;
	}
	
	public static String getParam(Map params, String paramName, boolean b) throws RJException {
		if (params.containsKey(paramName)) {
			return (String)params.get(paramName);
		}
		if (b) {
			throw new RJException("Mandatory attribute not set: " + paramName);
		}
		return null;
	}
	
	public static boolean isWindowMode() {
		return MainFrame.main != null;
	}
	
	public static boolean isTextMode() {
		return !isWindowMode();
	}

	public synchronized static File createBufferFile(Object ref) {
		File fout = new File(System.currentTimeMillis()  + "_" + ref.hashCode() + ".bin");
		while (fout.exists()) {
			fout = new File(System.currentTimeMillis()  + "_" + ref.hashCode() + ".bin");
		}
		return fout;
	}

	public static String getSummaryMessage(ConfiguredSystem system, boolean cancelled, long elapsedTime, int nn) {
		if (system.isDeduplication()) {
			return prepareDeduplicationMessage(system, cancelled, elapsedTime, nn);
		} else {
			return prepareLinkageMessage(system, cancelled, elapsedTime, nn);
		}
	}

	private static String prepareLinkageMessage(ConfiguredSystem system, boolean cancelled, long elapsedTime, int nn) {
		String msg = cancelled ? "Linkage cancelled by user.\n\n" : "Linkage successfully completed :)\n\n";
		
		//Before this was wrapped in if that checked whether system used strata join wrapper...
		msg += getSourceSummary(system.getJoin().getLinkageSummary().getCntReadSrcA(), system.getSourceA());
		msg += "\n\n";
		msg += getSourceSummary(system.getJoin().getLinkageSummary().getCntReadSrcB(), system.getSourceB());
		msg += "\n\n";
		
		msg += getLinkageSummary(system.getJoin(), system.getResultSaver());
		msg += "\n\n";
		msg += (cancelled ? "The linkage process interrupted after " : "Overall the linkage process took ") + elapsedTime + "ms.";
		
		return msg;
	}

	private static String getLinkageSummary(AbstractJoin join, AbstractResultsSaver resultSaver) {
		String msg = "";
		if (resultSaver instanceof DeduplicatingResultsSaver) {
			DeduplicatingResultsSaver dedupe = (DeduplicatingResultsSaver)resultSaver;
			msg += "Linkage process initially identified " + join.getLinkageSummary().getCntLinked() + " linkages.";
			msg += "\nThe results deduplication identified " + dedupe.getDuplicatesCnt() + " duplicates.";
			msg += "\n" + dedupe.getSavedCnt() + " final linkages were saved.";
		} else {
			msg += "Linkage process identified " + join.getLinkageSummary().getCntLinked() + " linkages.";
		}
		if (join.isSummaryForLeftSourceEnabled() && join.isSummaryForRightSourceEnabled()) {
			msg += "\nSummary information for not joined data for both \nsources was generated.";
		} else if (join.isSummaryForLeftSourceEnabled()) {
			msg += "\nSummary information for not joined data for source \n" + join.getSourceA().getSourceName() + " was generated.";
		} else if (join.isSummaryForRightSourceEnabled()) {
			msg += "\nSummary information for not joined data for source \n" + join.getSourceB().getSourceName() + " was generated.";
		} else {
			msg += "\nNo summary information for not joined data was generated.";
		}
		return msg;
	}

	private static String getSourceSummary(int srcRecordsCnt, AbstractDataSource src) {
		if (!(src.getPreprocessedDataSource() instanceof DeduplicationDataSource)) {
			return "Source " + src.getSourceName() + " provided " + srcRecordsCnt + " records.\nNo deduplication of data source was performed.";
		} else {
			DeduplicationDataSource dedupe = (DeduplicationDataSource)src.getPreprocessedDataSource();
			String msg = "Source " + src.getSourceName() + " provided " + dedupe.getInputRecordsCount() + " records.\n" + 
					"The deduplication process identified " + dedupe.getDuplicatesCount() + " duplicates";
			if (src.getDeduplicationConfig().getMinusFile() != null) {
				msg += "\nThe report containing duplicates was saved into file.";
			}
			return msg;
		}
	}
	
	private static String[] getShortSourceSummary(int srcRecordsCnt, AbstractDataSource src) {
		if (!(src.getPreprocessedDataSource() instanceof DeduplicationDataSource)) {
			return new String[] {String.valueOf(srcRecordsCnt)};
		} else {
			DeduplicationDataSource dedupe = (DeduplicationDataSource)src.getPreprocessedDataSource();
			return new String[] {String.valueOf(dedupe.getInputRecordsCount()), String.valueOf(dedupe.getDuplicatesCount())};
		}
	}

	private static String prepareDeduplicationMessage(ConfiguredSystem system, boolean cancelled, long elapsedTime, int nn) {
		AbstractDataSource src = system.getSourceA();
		
		String msg = cancelled ? "Deduplication cancelled by user.\n\n" : "Deduplication successfully completed :)\n\n";
		msg += getSourceSummary(-1, src);
		msg += "\n\n";
		msg += (cancelled ? "The deduplication process interrupted after " : "Overall the deduplication process took ") + elapsedTime + "ms.";
		
		return msg;
	}

	public static String getStatusBarSummaryMessage(ConfiguredSystem system, boolean cancelled, long time, int cnt) {
		if (system.isDeduplication()) {
			if (cancelled) {
				return "Deduplication was cancelled by user.";
			} else {
				return "Deduplication completed.";
			}
		} else {
			if (cancelled) {
				return "Linkage cancelled by user.";
			} else {
				if (system.getResultSaver() instanceof DeduplicatingResultsSaver) {
					return "Linkage completed (saved " + ((DeduplicatingResultsSaver)system.getResultSaver()).getSavedCnt() + " linkages).";
				} else {
					return "Linkage completed (saved " + system.getJoin().getLinkageSummary().getCntLinked() + " linkages).";
				}
			}
		}
	}
	
	public static String[][] getShortSummary(ConfiguredSystem system, boolean cancelled, long time, int cnt) {
		if (system.isDeduplication()) {
			DeduplicationDataSource dedupe = (DeduplicationDataSource)system.getSourceA().getPreprocessedDataSource();
			String[][] summary = new String[3][];
			summary[0] = new String[] {"size (" + system.getSourceA().getSourceName() + "): ", String.valueOf(dedupe.getInputRecordsCount())};
			summary[1] = new String[] {"#duplicates: ", String.valueOf(dedupe.getDuplicatesCount())};
			summary[2] = new String[] {"time: ", time + "ms"};
			return summary;
		} else {
			LinkageSummary s = system.getJoin().getLinkageSummary();
			List summary = new ArrayList();
			String[] sum1 = getShortSourceSummary(s.getCntReadSrcA(), system.getSourceA());
			summary.add(new String[] {"size (" + system.getSourceA().getSourceName() + "): ", sum1[0]});
			if (sum1.length > 1) {
				summary.add(new String[] {"#duplicates (" + system.getSourceA().getSourceName() + "): ", sum1[1]});
			}
			String[] sum2 = getShortSourceSummary(s.getCntReadSrcB(), system.getSourceB());
			summary.add(new String[] {"size (" + system.getSourceB().getSourceName() + "): ", sum2[0]});
			if (sum2.length > 1) {
				summary.add(new String[] {"#duplicates (" + system.getSourceB().getSourceName() + "): ", sum2[1]});
			}
			if (system.getResultSaver() instanceof DeduplicatingResultsSaver) {
				DeduplicatingResultsSaver saver = (DeduplicatingResultsSaver)system.getResultSaver();
				summary.add(new String[] {"#linkages: ", String.valueOf(saver.getSavedCnt())});
			} else {
				summary.add(new String[] {"#linkages: ", String.valueOf(s.getCntLinked())});
			}
			summary.add(new String[] {"time: ", time + "ms"});
			return (String[][])summary.toArray(new String[][] {});
		}
	}
	
	public static Encoding recognizeEncoding(File inputFile) throws IOException {
		
		InputStream str = null;
		try {
			byte[] magic = new byte[4];
			str = new FileInputStream(inputFile);
			int read = readGuaranteed(magic, str);
			
			//now the recognition can happen
			
			if (read < 2) {
				return getEncodingForName("US-ASCII");
			}
			
			//UTF-16(BE)
			if (magic[0] == (byte)0xFE && magic[1] == (byte)0xFF) {
				return getEncodingForName("UTF-16BE");
			}
			
			//UTF-16(LE)
			if (magic[0] == (byte)0xFF && magic[1] == (byte)0xFE) {
				return getEncodingForName("UTF-16LE");
			}
			
			if (read < 3) {
				return getEncodingForName("US-ASCII");
			}
			
			//UTF-8
			if (magic[0] == (byte)0xEF && magic[1] == (byte)0xBB && magic[2] == (byte)0xBF) {
				return getEncodingForName("UTF-8");
			}
			
			if (read < 4) {
				return getEncodingForName("US-ASCII");
			}
			
			//UTF-32(BE)
			if (magic[0] == (byte)0x00 && magic[1] == (byte)0x00 && magic[2] == (byte)0xFE && magic[3] == (byte)0xFF) {
				return getEncodingForName("UTF-32BE");
			}
			
			//UTF-32(LE)
			if (magic[0] == (byte)0xFF && magic[1] == (byte)0xFE && magic[2] == (byte)0x00 && magic[3] == (byte)0x00) {
				return getEncodingForName("UTF-32LE");
			}
			
			//Try to recognize UTF-8 without magic code
			str.close();
			str = new FileInputStream(inputFile);
			
			int utf8Compilant = 0;
			int asciiCompilant = 0;
			for (int i = 0; i < 10000; i++) {
				int b = str.read();
				if (b == -1) {
					break;
				}
				if (b < 128) {
					asciiCompilant++;
				} else {
					//This potentially is n-byte code in utf-8
					//1. Determine length
					//2. Check next codes - if they are correct
					int len = getUtf8Len(b);
					if (len == -1) {
						return getEncodingForName("US-ASCII");
					}
					for (int j = 0; j < len - 1; j++) {
						b = str.read();
						if (b == -1 || !isValidUTF(b, j + 1)) {
							return getEncodingForName("US-ASCII");
						}
					}
					utf8Compilant++;
				}
				
			}
			str.close();
			if (utf8Compilant == 0) {
				return getEncodingForName("US-ASCII");
			} else {
				return getEncodingForName("UTF-8");
			}
		} finally {
			if (str != null) {
				str.close();
			}
		}
	}
	
	private static int readGuaranteed(byte[] magic, InputStream str) throws IOException {
		int read = 0;
		int tmp = 0;
		while ((tmp = str.read(magic, read, magic.length - read)) != -1) {
			read += tmp;
			if (read == magic.length) {
				break;
			}
		}
		return read;
	}

	private static boolean isValidUTF(int b, int i) {
		return (b & (1<<7)) == (1<<7) && (b & (1<<6)) == 0;
	}

	private static int getUtf8Len(int b) {
		for (int i = 0; i < 8; i++) {
			int mask = 1<<(8-i-1);
			if ((mask & b) == 0) {
				return i;
			}
		}
		return -1;
	}
	
	public static Reader openTextFileForReading(String filePath) throws IOException {
		return openTextFileForReading(filePath, false);
	}

	public static File resolvePath(String filePath, boolean relativePaths) throws IOException {
		File f = new File(filePath);
		if (relativePaths) {
			//Try to look for file in few places
			if (MainApp.main instanceof MainFrame) {
				String recentPath = MainFrame.main.getPersistentParam(SavedConfigManager.PERSISTENT_PARAM_RECENT_PATH);
				if (!f.exists() && recentPath != null) {
					f = new File(recentPath + File.separator + filePath);
				}
			}
		}
		return f;
	}
	
	public static Reader openTextFileForReading(String filePath, boolean relativePaths) throws IOException {
		String[] parsedFile = parseFilePath(filePath);
		File f = resolvePath(parsedFile[0], relativePaths);
		
		Charset encoding = DEFAULT_ENCODING.getCharset();
		if (parsedFile.length > 1) {
			encoding = getEncodingForName(parsedFile[1]).getCharset();
		}
		
		//Some encoding is used, will read that format
		Log.log(Utils.class, "File " + f.getAbsolutePath() + " encoding identifed: " + encoding, 2);
		FileInputStream in = new FileInputStream(f);
		long header = getHeaderLen(in, encoding);
		in.close();
		in = new FileInputStream(f);
		Log.log(Utils.class, "Header length was determined to be " + header + " byte(s).", 3);
		in.skip(header);
		InputStreamReader inputStreamReader = new InputStreamReader(in, encoding);
		return inputStreamReader;
	}
	
	public static Writer openTextFileForWriting(File f, Charset encoding) throws IOException {
		FileOutputStream fos = new FileOutputStream(f);
		byte[] header = getHeader(encoding);
		if (!encoding.toString().equals("UTF-8")) {
			fos.write(header);
		}
		return new OutputStreamWriter(fos, encoding);
	}
	
	private static byte[] getHeader(Charset encoding) {
		byte[] header = null;
		if (encoding.toString().equals("UTF-32LE")) {
			header = new byte[] {(byte)0xFF, (byte)0xFE, (byte)0x00, (byte)0x00};
		} else if (encoding.toString().equals("UTF-32BE")) {
			header = new byte[] {(byte)0x00, (byte)0x00, (byte)0xFE, (byte)0xFF};
		} else if (encoding.toString().equals("UTF-16LE")) {
			header = new byte[] {(byte)0xFF, (byte)0xFE};
		} else if (encoding.toString().equals("UTF-16BE")) {
			header = new byte[] {(byte)0xFE, (byte)0xFF};
		} else if (encoding.toString().equals("UTF-8")) {
			header = new byte[] {(byte)0xEF, (byte)0xBB, (byte)0xBF};
		} else {
			header = new byte[] {};
		}
		return header;
	}

	private static long getHeaderLen(FileInputStream in, Charset encoding) throws IOException {
		byte[] expectedHeader = getHeader(encoding);
		byte[] magic = new byte[expectedHeader.length];
		int read = readGuaranteed(magic, in);
		if (read != expectedHeader.length) {
			return 0;
		}
		for (int i = 0; i < magic.length; i++) {
			if (magic[i] != expectedHeader[i]) {
				return 0;
			}
		}
		return magic.length;
	}

	public static final Encoding[] SUPPORTED_ENCODINGS = new Encoding[] {
		new Encoding("ASCII", "US-ASCII"),
		new Encoding("UTF-8", "UTF-8"),
		new Encoding("UTF-16", "UTF-16LE"),
		new Encoding("UTF-16 Big Endian", "UTF-16BE"),
		new Encoding("UTF-32", "UTF-32LE"),
		new Encoding("UTF-32 Big Endian", "UTF-32BE")
	};
	
	public static final Encoding DEFAULT_ENCODING = SUPPORTED_ENCODINGS[0];
	
	public static Encoding getEncodingForName(String name) {
		for (int i = 0; i < SUPPORTED_ENCODINGS.length; i++) {
			if (SUPPORTED_ENCODINGS[i].getCharset().toString().equals(name)) {
				return SUPPORTED_ENCODINGS[i];
			}
		}
		Log.log(Utils.class, "ERROR: Coding \"" + name + "\" was not found in FRIL. Using default (" + DEFAULT_ENCODING.getCharset().toString() + ")");
		return DEFAULT_ENCODING;
	}
	
	public static Encoding getEncodingForUserLabel(String name) {
		for (int i = 0; i < SUPPORTED_ENCODINGS.length; i++) {
			if (SUPPORTED_ENCODINGS[i].toString().equals(name)) {
				return SUPPORTED_ENCODINGS[i];
			}
		}
		Log.log(Utils.class, "ERROR: Coding \"" + name + "\" was not found in FRIL. Using default (" + DEFAULT_ENCODING.toString() + ")");
		return DEFAULT_ENCODING;
	}
	
	public static String[] parseFilePath(String val) {
		if (val.lastIndexOf("#ENC=") != -1) {
			String fileName = val.substring(0, val.lastIndexOf("#ENC="));
			String enc = val.substring(val.lastIndexOf("#ENC=") + "#ENC=".length(), val.length() - 1);
			return new String[] {fileName, enc};
		} else {
			return new String[] {val};
		}
	}
	
	public static class Encoding {
		
		private String label;
		private Charset charset;
		
		private Encoding(String label, String charsetCode) {
			this.label = label;
			this.charset = Charset.forName(charsetCode);
		}
		
		public Charset getCharset() {
			return charset;
		}
		
		public String toString() {
			return label;
		}
		
	}
	
}
