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


package cdc.impl.deduplication.gui;

import javax.swing.JPanel;

import cdc.components.AbstractDistance;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.components.uicomponents.BlockingAttributePanel;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.impl.conditions.ConditionItem;
import cdc.impl.join.blocking.BlockingFunction;
import cdc.impl.join.blocking.BlockingFunctionFactory;

public class DeduplicationSearchMethod extends WizardAction {

	public static final String WARNING_DEDUPE = "The available options depend on distance metric used for selected blocking attribute.";
	
	private DeduplicationConditionAction action;
	private BlockingAttributePanel blockPanel;
	
	private DataColumnDefinition[][] originalHashAttr;
	private String originalHashFunction;
	
	public DeduplicationSearchMethod(DeduplicationConditionAction action, BlockingFunction fnct) {
		this.action = action;
		this.originalHashFunction = BlockingFunctionFactory.encodeBlockingFunction(fnct);
		this.originalHashAttr = fnct.getColumns();
	}

	public JPanel beginStep(AbstractWizard wizard) {
		ConditionItem[] dedupCond = action.getDeduplicationCondition();
		String[] attrLabels = new String[dedupCond.length];
		AbstractDistance[] distances = new AbstractDistance[dedupCond.length];
		for (int i = 0; i < dedupCond.length; i++) {
			attrLabels[i] = dedupCond[i].getLeft().getColumnName();
			distances[i] = dedupCond[i].getDistanceFunction();
		}
		
		blockPanel = new BlockingAttributePanel(attrLabels, distances, WARNING_DEDUPE);
		if (originalHashAttr != null) {
			int id = -1;
			for (int i = 0; i < dedupCond.length; i++) {
				if (dedupCond[i].getLeft().equals(originalHashAttr[0][0])) {
					id = i;
					break;
				}
			}
			
			if (id != -1) {
				blockPanel.setBlockingAttribute(id);
				blockPanel.setBlockingFunction(originalHashFunction);
			}
		}
		
		return blockPanel;
	}

	public void dispose() {
		this.action = null;
	}

	public boolean endStep(AbstractWizard wizard) {
		return true;
	}

	public void setSize(int width, int height) {
	}
	
	public BlockingFunction getHashingFunction() {
		DataColumnDefinition col = action.getDeduplicationCondition()[blockPanel.getBlockingAttributeId()].getLeft();
		return BlockingFunctionFactory.createBlockingFunction(new DataColumnDefinition[][] {new DataColumnDefinition[] {col, col}}, blockPanel.getBlockingFunction());
	}

}
