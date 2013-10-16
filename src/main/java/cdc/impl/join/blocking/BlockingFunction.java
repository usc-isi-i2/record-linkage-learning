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

import java.io.Serializable;

import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;

/**
 * This interface represents a blocking function.
 * The implementation of this interface can be used
 * to generate blocks of records. The most important
 * method in this interface is the hash method.
 * This method is supposed to return a string descriptor
 * of the block that should be used for given input record.
 * Two records will end up in the same block if their blocks string
 * descriptors are the same. 
 * @author Pawel Jurczyk
 *
 */
public interface BlockingFunction extends Serializable {
	
	//TODO Probably should refactor - create abstract class BlockingFunction that would hold columns used for blocking and provided getter for these.
	/**
	 * Return columns used for blocking. The returned array is two-dimensional.
	 * The array formatting is as follows:
	 * {{attr-0-first-source, attr-0-second-source},
	 *  {attr-1-first-source, attr-1-second-source},
	 *  ...
	 *  {attr-n-first-source, attr-n-second-source}}
	 *  Note that blocking functions can use any number of input attributes. 
	 * @return columns used for hashing in the format defined above
	 */
	public DataColumnDefinition[][] getColumns();
	
	/**
	 * Returns a string representing hash value of given data row.
	 * The blocking manager will build buckets from the records that have
	 * the same hash value. Note that this function returns String hash value.
	 * @param value input data row
	 * @param id sourceID (0 - left source, 1 - right source)
	 * @return hash value
	 */
	public String hash(DataRow value, int sourceId);
}
