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


package cdc.impl.datastream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import cdc.datamodel.DataRow;
import cdc.impl.datasource.wrappers.propertiescache.CacheInterface;
import cdc.utils.RJException;
import cdc.utils.RowUtils;

public class DataRowInputStream {
	
	private InputStream stream;
	private DataFileHeader header;
	private CacheInterface cache;
	
	public DataRowInputStream(/*CacheInterface cache, */InputStream stream) throws IOException, RJException {
		this.stream = stream;
		//this.cache = cache;
		readDataFileHeader();
	}
	
	public DataFileHeader getHeader() {
		return header;
	}
	
	public DataRow readDataRow() throws IOException, RJException {
		byte[] b = readGuaranteed(4);
		if (b == null) {
			return null;
		}
		b = readGuaranteed(fromBytes(b));
		return RowUtils.byteArrayToDataRow(cache, b, header.getMetadataAsColumnsArray("columns"), header.getSourceName());
	}
	
	private void readDataFileHeader() throws IOException, RJException {
		byte[] b = readGuaranteed(4);
		b = readGuaranteed(fromBytes(b));
		ByteArrayInputStream array = new ByteArrayInputStream(b);
		ObjectInputStream os = new ObjectInputStream(array);
		header = new DataFileHeader(null);
		try {
			header.read(os);
		} catch (ClassNotFoundException e) {
			throw new RJException("Error reading header of input file");
		}
	}
	
	private int fromBytes(byte[] buf) {
		return (int)((((int)buf[0]&0xff)<<24)+(((int)buf[1]&0xff)<<16)+(((int)buf[2]&0xff)<<8)+((int)buf[3]&0xff));
	}
	
	public void close() throws IOException {
		stream.close();
	}
	
	public byte[] readGuaranteed(int size) throws IOException, RJException {
		int total = 0;
		byte[] bytes = new byte[size];
		while (total != size) {
			int r = stream.read(bytes, total, size - total);
			if (r == -1) {
				if (total == 0) return null;
				throw new RJException("Unexpected end of file");
			}
			total += r;
		}
		return bytes;
	}
	
	public InputStream getUnderlyingStream() {
		return stream;
	}
	
}
