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


package cdc.impl.join.blocking;

/**
 * A simple class that represents bucket. The class stores information
 * about hash code of the bucket (integer) and count of records in this
 * bucket. The two counts are stored, one for each data source.
 * @author Pawel Jurczyk
 *
 */
public class Bucket {
	
	/**
	 * The hash code of bucket
	 */
	private int hash;
	
	/**
	 * The count of records in this bucket coming from left data source.
	 */
	private int leftRecords;
	
	/**
	 * The count of records in this bucket coming from right data source.
	 */
	private int rightRecords;
	
	/**
	 * The constructor. It accept an array of strings that represent String hash codes,
	 * and generates an integer hash code from that. 
	 * @param bucket the hash codes represented as strings
	 */
	public Bucket(String[] bucket) {
		hash = bucket[0].hashCode();
		for (int i = 1; i < bucket.length; i++) {
			hash = hash ^ bucket[i].hashCode();
		}
	}
	
	/**
	 * Creates bucket for given hash code.
	 * @param hash the hash code for this bucket
	 */
	public Bucket(int hash) {
		this.hash = hash;
	}
	
	/**
	 * Returns the hash code of this bucket
	 */
	public int hashCode() {
		return hash;
	}
	
	/**
	 * Compares two buckets. Two buckets are equal if they have the same hash codes.
	 */
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof Bucket)) {
			return false;
		}
		return hash == ((Bucket)arg0).hash;
	}
	
	/**
	 * Increases count of records from left source in thus bucket.
	 */
	public void leftRecordAdded() {
		leftRecords++;
	}

	/**
	 * Returns the count of records from left source in this bucket
	 * @return
	 */
	public int getLeftRowsCount() {
		return leftRecords;
	}
	
	/**
	 * Increases count of records from right source in this bucket.
	 */
	public void rightRecordAdded() {
		rightRecords++;
	}
	
	/**
	 * Returns the count of records from left source in this bucket
	 * @return
	 */
	public int getRightRowsCount() {
		return rightRecords;
	}
	
}
