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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import cdc.datamodel.DataColumnDefinition;

public class DataFileHeader {
	
	public Map meta = new HashMap();
	
	public String dataSourceName;
	public long timestamp;
	
	public DataFileHeader(String name) {
		dataSourceName = name;
		timestamp = System.currentTimeMillis();
	}
	
	public void addMetadata(String metadata, Object value) {
		meta.put(metadata, value);
	}
	
	public Object getMetadata(String metadata) {
		return meta.get(metadata);
	}

	public DataColumnDefinition[] getMetadataAsColumnsArray(String metadata) {
		return (DataColumnDefinition[]) meta.get(metadata);
	}
	
	public String getMetadataAsString(String metadata) {
		return (meta.containsKey(metadata) ? meta.get(metadata).toString() : null);
	}
	
	public void write(ObjectOutputStream oos) throws IOException {
		oos.writeObject(dataSourceName);
		oos.writeLong(timestamp);
		oos.writeObject(meta);
	}
	
	public void read(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		dataSourceName = (String) ois.readObject();
		timestamp = ois.readLong();
		meta = (Map) ois.readObject();
	}

	public String getSourceName() {
		return dataSourceName;
	}

	public long getTimestamp() {
		return timestamp;
	}
	
}
