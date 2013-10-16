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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.impl.datasource.wrappers.propertiescache.CacheInterface;
import cdc.utils.RowUtils;

public class DataRowOutputStream {
	
	private OutputStream stream;
	private DataFileHeader header;
	private CacheInterface cache;
	private boolean headerWritten = false;
	
	public DataRowOutputStream(/*CacheInterface cache, */String name, DataColumnDefinition[] rowModel, OutputStream stream) {
		header = new DataFileHeader(name);
		header.addMetadata("columns", rowModel);
		this.stream = stream;
		//this.cache = cache;
	}
	
	public void addHeaderMetadata(String metadata, Object value) {
		if (headerWritten) {
			throw new RuntimeException("Cannot add metadata to file header after it was written");
		}
		header.addMetadata(metadata, value);
	}
	
	public void writeDataRow(DataRow row) throws IOException {
		if (!headerWritten) {
			headerWritten = true;
			writeDataFileHeader();
		}
		DataColumnDefinition[] rowModel = header.getMetadataAsColumnsArray("columns");
		byte[] bytes = RowUtils.rowToByteArray(cache, row, rowModel);
		stream.write(toBytes(bytes.length));
		stream.write(bytes);
	}
	
	private void writeDataFileHeader() throws IOException {
		//System.out.println("Writting header");
		ByteArrayOutputStream array = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(array);
		header.write(oos);
		oos.flush();
		byte[] bytes = array.toByteArray();
		stream.write(toBytes(bytes.length));
		stream.write(bytes);
	}
	
	private byte[] toBytes(int x) {
		byte[] buf = new byte[4];
		buf[0]=(byte)((x & 0xff000000)>>>24);
		buf[1]=(byte)((x & 0x00ff0000)>>>16);
		buf[2]=(byte)((x & 0x0000ff00)>>>8);
		buf[3]=(byte)((x & 0x000000ff));
		return buf;
	}

	public void close() throws IOException {
		if (!headerWritten) {
			headerWritten = true;
			writeDataFileHeader();
		}
		stream.flush();
		stream.close();
	}
	
	public OutputStream getUnderlyingStream() throws IOException {
		if (!headerWritten) {
			headerWritten = true;
			writeDataFileHeader();
		}
		return stream;
	}
}
