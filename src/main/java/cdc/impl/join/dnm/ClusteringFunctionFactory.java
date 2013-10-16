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


package cdc.impl.join.dnm;

import java.util.HashMap;
import java.util.Map;

import cdc.components.AbstractDistance;
import cdc.impl.distance.EditDistance;
import cdc.impl.distance.EqualFieldsDistance;
import cdc.impl.distance.QGramDistance;
import cdc.impl.distance.SoundexDistance;
import cdc.utils.RJException;

public class ClusteringFunctionFactory {
	
	public static AbstractDistance getEditDistance() {
		Map props = new HashMap();
		props.put(EditDistance.PROP_BEGIN_APPROVE_LEVEL, "0");
		props.put(EditDistance.PROP_END_APPROVE_LEVEL, "1");
		return new EditDistance(props);
	}
	
	private static AbstractDistance getQGrams() {
		Map propMap = new HashMap();
		propMap.put(QGramDistance.PROP_APPROVE, "0");
		propMap.put(QGramDistance.PROP_DISAPPROVE, "1");
		propMap.put(QGramDistance.PROP_Q, QGramDistance.DEFAULT_Q);
		return new QGramDistance(propMap);
	}
	
	private static AbstractDistance getSoundex() {
		Map propMap = new HashMap();
		propMap.put(SoundexDistance.PROP_SIZE, String.valueOf(SoundexDistance.DFAULT_SIZE));
		propMap.put(EditDistance.PROP_BEGIN_APPROVE_LEVEL, "0");
		propMap.put(EditDistance.PROP_END_APPROVE_LEVEL, "1");
		return new SoundexDistance(propMap);
	}
	
	public static AbstractDistance convertDistanceFunction(AbstractDistance initial) throws RJException {
		if (initial instanceof EditDistance) {
			return getEditDistance();
		} else if (initial instanceof QGramDistance) {
			return getQGrams();
		} else if (initial instanceof EqualFieldsDistance) {
			return new EqualFieldsDistance();
		} else if (initial instanceof SoundexDistance) {
			return getSoundex();
		}
		throw new RJException("Function " + initial.toString() + " cannot be currently used for clustering");
	}

	public static boolean canBeUsed(AbstractDistance abstractDistance) {
		try {
			convertDistanceFunction(abstractDistance);
			return true;
		} catch (RJException e) {
			return false;
		}
	}
	
}
