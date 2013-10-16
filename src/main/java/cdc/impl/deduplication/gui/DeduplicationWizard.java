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

import java.awt.Window;

import javax.swing.JButton;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.impl.conditions.ConditionItem;
import cdc.impl.deduplication.DeduplicationConfig;

public class DeduplicationWizard {

	private static final String[] LABELS = new String[] {
		"Duplicates identification condition (step 1 of 2)",
		"Duplicates search method (step 2 of 2)"
	};

	private AbstractWizard wizard;

	private WizardAction[] actions;
	
	public DeduplicationWizard(AbstractDataSource source, DeduplicationConfig configToUse, Window parent, JButton dedupeButton) {
		actions = new WizardAction[2];
		DeduplicationConfig config = configToUse;
		if (config == null) {
			config = source.getDeduplicationConfig();
		}
		if (config == null) {
			config = new DeduplicationConfig(source);
		}
		actions[0] = new DeduplicationConditionAction(source, config);
		actions[1] = new DeduplicationSearchMethod((DeduplicationConditionAction) actions[0], config != null ? config.getHashingFunction() : null);
		wizard = new AbstractWizard(parent, actions, LABELS);
		wizard.setLocationRelativeTo(dedupeButton);
	}

	public int getResult() {
		return wizard.getResult();
	}
	
	
	public DeduplicationConfig getDeduplicationConfig() {
		DeduplicationConfig config = null;
		ConditionItem[] items = ((DeduplicationConditionAction) actions[0]).getDeduplicationCondition();
		DataColumnDefinition[] cols = new DataColumnDefinition[items.length];
		AbstractDistance[] distances = new AbstractDistance[items.length];
		double[] emptyMatch = new double[items.length];
		int[] weights = new int[items.length];
		for (int i = 0; i < items.length; i++) {
			cols[i] = items[i].getLeft();
			distances[i] = items[i].getDistanceFunction();
			emptyMatch[i] = items[i].getEmptyMatchScore();
			weights[i] = items[i].getWeight();
		}
		config = new DeduplicationConfig(cols, distances, weights, emptyMatch);
		config.setAcceptanceLevel(((DeduplicationConditionAction) actions[0]).getAcceptanceLevel());
		config.setHashingConfig(((DeduplicationSearchMethod)actions[1]).getHashingFunction());
		return config;
	}

}
