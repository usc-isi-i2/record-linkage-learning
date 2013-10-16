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


package cdc.components;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.utils.CompareFunctionInterface;

public abstract class AbstractJoinCondition extends SystemComponent  {
	
	private boolean canUseOptimisticEval = false;
	
	public AbstractJoinCondition(Map properties) {
		super(properties);
	}
	
	public boolean isCanUseOptimisticEval() {
		return canUseOptimisticEval;
	}

	public void setCanUseOptimisticEval(boolean canUseOptimisticEval) {
		this.canUseOptimisticEval = canUseOptimisticEval;
	}
	
	public abstract Object clone();
	public abstract EvaluatedCondition conditionSatisfied(DataRow rowA, DataRow rowB);
	public abstract DataColumnDefinition[] getLeftJoinColumns();
	public abstract DataColumnDefinition[] getRightJoinColumns();
	public abstract AbstractDistance[] getDistanceFunctions();
	public abstract void saveToXML(Document doc, Element node);

	public CompareFunctionInterface[] getCompareFunctions(DataColumnDefinition[] columnsLeft, DataColumnDefinition[] columnsRight) {
		DataColumnDefinition[] left = getLeftJoinColumns();
		DataColumnDefinition[] right = getRightJoinColumns();
		AbstractDistance[] dists = getDistanceFunctions();
		DataColumnDefinition[] localLeft = null;
		if (left[0].getSourceName().equals(columnsLeft[0].getSourceName())) {
			localLeft = left;
		} else {
			localLeft = right;
		}
		return analyze(localLeft, columnsLeft, columnsRight, dists);
	}

	private CompareFunctionInterface[] analyze(DataColumnDefinition[] localLeft, DataColumnDefinition[] columnsLeft, DataColumnDefinition[] columnsRight, AbstractDistance[] distance) {
		CompareFunctionInterface[] functions = new CompareFunctionInterface[columnsLeft.length];
		for (int i = 0; i < functions.length; i++) {
			for (int j = 0; j < localLeft.length; j++) {
				if (localLeft[j].equals(columnsLeft[i])) {
					functions[i] = distance[j].getCompareFunction(columnsLeft[i], columnsRight[i]);
				}
			}
			if (functions[i] == null) {
				return null;
				//throw new RuntimeException("Requested columns " + columnsLeft[i] + " <--> " + columnsRight[i] + " was not added to join condition!");
			}
			
		}
		return functions;
	}

}
