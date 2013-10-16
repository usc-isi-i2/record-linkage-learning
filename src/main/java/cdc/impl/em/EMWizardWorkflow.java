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


package cdc.impl.em;

import java.io.IOException;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoinCondition;
import cdc.gui.wizards.WizardAction;
import cdc.gui.wizards.Workflow;
import cdc.impl.em.actions.ConfigureBlockingMethod;
import cdc.impl.em.actions.ConfigureSearchMethodAction;
import cdc.impl.em.actions.ConfigureSourcesAction;
import cdc.impl.em.actions.EMRunnerAction;
import cdc.utils.RJException;

public class EMWizardWorkflow implements Workflow {

	private WizardAction[] actions;
	private int currentStep = 0;
	
	public EMWizardWorkflow(AbstractDataSource sourceA, AbstractDataSource sourceB, AbstractJoinCondition cond) throws IOException, RJException {
		actions = new WizardAction[4];
		actions[0] = new ConfigureSourcesAction(sourceA, sourceB);
		actions[1] = new ConfigureSearchMethodAction();
		actions[2] = new ConfigureBlockingMethod(cond);
		actions[3] = new EMRunnerAction((ConfigureSourcesAction)actions[0], (ConfigureSearchMethodAction)actions[1], 
				(ConfigureBlockingMethod)actions[2]);
		
	}
	
	public WizardAction[] getActions() {
		return actions;
	}
	
	public int getCurrentStep() {
		return currentStep;
	}

	public boolean isFirstStep() {
		return currentStep == 0;
	}

	public boolean isLastStep() {
		return currentStep == actions.length - 1;
	}

	public int nextStep() {
		if (currentStep == 1) {
			if (((ConfigureSourcesAction)actions[0]).needsBlocking() || 
					((ConfigureSearchMethodAction)actions[1]).needsBlocking()) {
				return ++currentStep;
			} else {
				return currentStep = actions.length - 1;
			}
		} else {
			if (currentStep + 1 < actions.length) currentStep++;
			return currentStep;
		}
	}

	public int previousStep() {
		if (currentStep == actions.length - 1) {
			if (((ConfigureSourcesAction)actions[0]).needsBlocking() || 
					((ConfigureSearchMethodAction)actions[1]).needsBlocking()) {
				return --currentStep;
			} else {
				return currentStep = 1;
			}
		} else {
			if (currentStep == 0) return -1;
			return --currentStep;
		}
	}

	public String[] getLabels() {
		return new String[] {"Sample data configuration", "Search method configuration", "Blocking method configuration", "EM method results"};
		
	}

}
