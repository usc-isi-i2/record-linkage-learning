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


package cdc.impl.join.strata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.components.AbstractJoinCondition;
import cdc.components.AtomicCondition;
import cdc.components.EvaluatedCondition;
import cdc.configuration.Configuration;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.utils.RJException;
import edu.emory.mathcs.util.xml.DOMUtils;

public class StrataJoinCondition extends AbstractJoinCondition {

	private DataStratum[] strata = new DataStratum[] {};
	private AbstractJoinCondition[] strataConditions = new AbstractJoinCondition[] {};
	
	private DataColumnDefinition[] leftCols;
	private DataColumnDefinition[] rightCols;
	private AbstractDistance[] distances;
	
	public StrataJoinCondition(Map properties) {
		super(properties);
	}
	
	public void addStrata(DataStratum stratum, AbstractJoinCondition condition) {
		DataStratum[] newStrata = new DataStratum[strata.length + 1];
		AbstractJoinCondition[] newConds = new AbstractJoinCondition[strataConditions.length + 1];
		System.arraycopy(strata, 0, newStrata, 0, strata.length);
		newStrata[newStrata.length - 1] = stratum;
		System.arraycopy(strataConditions, 0, newConds, 0, strataConditions.length);
		newConds[newConds.length - 1] = condition;
		strata = newStrata;
		strataConditions = newConds;
		calculateDistanceAndColums();
	}

	private void calculateDistanceAndColums() {
		List distances = new ArrayList();
		List left = new ArrayList();
		List right = new ArrayList();
		for (int i = 0; i < strataConditions.length; i++) {
			for (int j = 0; j < strataConditions[i].getDistanceFunctions().length; j++) {
				if (left.indexOf(strataConditions[i].getLeftJoinColumns()[j]) == -1 || 
						right.indexOf(strataConditions[i].getRightJoinColumns()[j]) == -1) {
					left.add(strataConditions[i].getLeftJoinColumns()[j]);
					right.add(strataConditions[i].getRightJoinColumns()[j]);
					distances.add(strataConditions[i].getDistanceFunctions()[j]);
				}
			}
		}
		this.distances = (AbstractDistance[])distances.toArray(new AbstractDistance[] {});
		this.leftCols = (DataColumnDefinition[]) left.toArray(new DataColumnDefinition[] {});
		this.rightCols = (DataColumnDefinition[]) right.toArray(new DataColumnDefinition[] {});
	}
	
	public void setCanUseOptimisticEval(boolean canUseOptimisticEval) {
		for (int i = 0; i < strataConditions.length; i++) {
			strataConditions[i].setCanUseOptimisticEval(canUseOptimisticEval);
		}
		super.setCanUseOptimisticEval(canUseOptimisticEval);
	}
	
	public EvaluatedCondition conditionSatisfied(DataRow rowA, DataRow rowB) {
		EvaluatedCondition bestEval = new EvaluatedCondition(false, false, 0);
		for (int i = 0; i < strata.length; i++) {
			if (strata[i].rowsInStratum(rowA, rowB)) {
				EvaluatedCondition eval = strataConditions[i].conditionSatisfied(rowA, rowB);
				if (bestEval == null || bestEval.getConfidence() < eval.getConfidence()) {
					bestEval = eval;
				}
			}
		}
		return bestEval;
	}

	public AbstractDistance[] getDistanceFunctions() {
		return distances;
	}

	public DataColumnDefinition[] getLeftJoinColumns() {
		return leftCols;
	}

	public DataColumnDefinition[] getRightJoinColumns() {
		return rightCols;
	}

	public DataStratum[] getStrata() {
		return strata;
	}

	public AbstractJoinCondition[] getJoinConditions() {
		return strataConditions;
	}
	
	public static AbstractJoinCondition fromXML(AbstractDataSource leftSource, AbstractDataSource rightSource, Element node) throws RJException {
		Element conditions = DOMUtils.getChildElement(node, "strata");
		Element[] conds = DOMUtils.getChildElements(conditions);
		List condList = new ArrayList();
		List strataList = new ArrayList();
		for (int i = 0; i < conds.length; i++) {
			if (conds[i].getNodeName().equals("stratum")) {
				String name = DOMUtils.getAttribute(conds[i], Configuration.NAME_ATTR);
				Element stratumConds = DOMUtils.getChildElement(conds[i], "conditions");
				Element left = DOMUtils.getChildElement(stratumConds, "left-source");
				Element right = DOMUtils.getChildElement(stratumConds, "right-source");
				strataList.add(new DataStratum(name, readConditions(left, leftSource, name), readConditions(right, rightSource, name)));
				Element joinCond = DOMUtils.getChildElement(conds[i], "join-condition");
				condList.add(Configuration.readConditionConfiguration(leftSource, rightSource, joinCond));
			}
		}
		StrataJoinCondition cond = new StrataJoinCondition(null);
		for (int i = 0; i < condList.size(); i++) {
			cond.addStrata((DataStratum)strataList.get(i), (AbstractJoinCondition)condList.get(i));
		}
		return cond;
	}

	public void saveToXML(Document doc, Element node) {
		Configuration.appendParams(doc, node, getProperties());
		Element conditions = DOMUtils.createChildElement(doc, node, "strata");
		for (int i = 0; i < strata.length; i++) {
			Element stratum = DOMUtils.createChildElement(doc, conditions, "stratum");
			DOMUtils.setAttribute(stratum, "name", strata[i].getName());
			Element stratumConfig = DOMUtils.createChildElement(doc, stratum, "conditions");
			Element stratumCSA = DOMUtils.createChildElement(doc, stratumConfig, "left-source");
			encodeCondition(doc, stratumCSA, strata[i].getSourceA());
			Element stratumCSB = DOMUtils.createChildElement(doc, stratumConfig, "right-source");
			encodeCondition(doc, stratumCSB, strata[i].getSourceB());
			Element cond = DOMUtils.createChildElement(doc, stratum, "join-condition");
			DOMUtils.setAttribute(cond, Configuration.CLASS_ATTR, strataConditions[i].getClass().getName());
			strataConditions[i].saveToXML(doc, cond);
		}
	}

	private void encodeCondition(Document doc, Element stratumEl, AtomicCondition[] source) {
		for (int j = 0; j < source.length; j++) {
			Element child = DOMUtils.createChildElement(doc, stratumEl, "stratum-condition");
			DOMUtils.setAttribute(child, "column", source[j].getColumn().getColumnName());
			DOMUtils.setAttribute(child, "condition", source[j].getCondition());
			DOMUtils.setAttribute(child, "value", source[j].getValue());
		}
	}
	
	private static AtomicCondition[] readConditions(Element node, AbstractDataSource source, String stratumName) {
		Element[] children = DOMUtils.getChildElements(node);
		List conds = new ArrayList();
		for (int i = 0; i < children.length; i++) {
			if (children[i].getNodeName().equals("stratum-condition")) {
				String colName = DOMUtils.getAttribute(children[i], "column");
				String cond = DOMUtils.getAttribute(children[i], "condition");
				String value = DOMUtils.getAttribute(children[i], "value");
				conds.add(new AtomicCondition(source.getDataModel().getColumnByName(colName), cond, value, stratumName));
			}
		}
		return (AtomicCondition[]) conds.toArray(new AtomicCondition[] {});
	}

	public Object clone() {
		StrataJoinCondition cond = new StrataJoinCondition(getProperties());
		cond.distances = distances;
		cond.leftCols = leftCols;
		cond.rightCols = rightCols;
		cond.strata = strata;
		cond.strataConditions = strataConditions;
		return cond;
	}

}
