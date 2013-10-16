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


package cdc.gui.wizards.specific;

import java.io.IOException;

import javax.swing.JFrame;

import cdc.configuration.ConfiguredSystem;
import cdc.gui.Configs;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.gui.wizards.specific.actions.ResultsConfigureSaversAction;
import cdc.gui.wizards.specific.actions.DSConfigureTypeAction;
import cdc.gui.wizards.specific.actions.DSConfigureAttrsAction;
import cdc.gui.wizards.specific.actions.LinkageConfigureConditionsAction;
import cdc.gui.wizards.specific.actions.LinkageConfigureSearchAction;
import cdc.gui.wizards.specific.actions.LinkageConfigureStrataAction;

public class SystemWizard {
	
	private static String[] steps = new String[] {
		"Left data source configuration (step 1 of 7)",
		"Right data source configuration (step 2 of 7)",
		"Right data source configuration (step 3 of 7)",
		"Right data source configuration (step 4 of 7)",
		"Join conditions and output columns (step 5 of 7)",
		"Join method configuration (step 6 of 7)",
		"Results saving configuration (step 7 of 7)"
	};
	
	private AbstractWizard wizard;
	
	private DSConfigureTypeAction leftSourceAction;
	private DSConfigureAttrsAction leftSourceFieldsAction;
	private DSConfigureTypeAction rightSourceAction;
	private DSConfigureAttrsAction rightSourceFieldsAction;
	private LinkageConfigureSearchAction joinConfiguration;
	private LinkageConfigureConditionsAction joinFieldsConfiguration;
	private ResultsConfigureSaversAction resultSaversActions;
	private LinkageConfigureStrataAction joinStratificationConfiguration;
	
	public SystemWizard(JFrame parent) {
		
		leftSourceAction = new DSConfigureTypeAction("sourceA");
		leftSourceFieldsAction = new DSConfigureAttrsAction(-1, leftSourceAction);
		rightSourceAction = new DSConfigureTypeAction("sourceB");
		rightSourceFieldsAction = new DSConfigureAttrsAction(-1, rightSourceAction);
		joinStratificationConfiguration = new LinkageConfigureStrataAction(leftSourceAction, rightSourceAction);
		joinFieldsConfiguration = new LinkageConfigureConditionsAction(leftSourceAction, rightSourceAction, joinStratificationConfiguration);
		joinConfiguration = new LinkageConfigureSearchAction(leftSourceAction, rightSourceAction, joinStratificationConfiguration, joinFieldsConfiguration);
		resultSaversActions = new ResultsConfigureSaversAction();
		
		WizardAction[] actions = new WizardAction[] {
				leftSourceAction,
				leftSourceFieldsAction,
				rightSourceAction,
				rightSourceFieldsAction,
				joinFieldsConfiguration,
				joinConfiguration,
				resultSaversActions,
		};
		
		wizard = new AbstractWizard(parent, actions, steps);
		wizard.setLocationRelativeTo(parent);
		wizard.setMinimum(Configs.DFAULT_WIZARD_SIZE);
	}
	
	public int getResult() {
		return wizard.getResult();
	}
	
	public ConfiguredSystem getConfiguredSystem() throws IOException {
		return new ConfiguredSystem(leftSourceAction.getDataSource(), rightSourceAction.getDataSource(), joinConfiguration.getJoin(), resultSaversActions.getResultsSaver());
	}
	
	public void dispose() {
		leftSourceAction.dispose();
		leftSourceFieldsAction.dispose();
		rightSourceAction.dispose();
		rightSourceFieldsAction.dispose();
		joinConfiguration.dispose();
		joinFieldsConfiguration.dispose();
		resultSaversActions.dispose();
		joinStratificationConfiguration.dispose();
		
		leftSourceAction = null;
		leftSourceFieldsAction = null;
		rightSourceAction = null;
		rightSourceFieldsAction = null;
		joinConfiguration = null;
		joinFieldsConfiguration = null;
		resultSaversActions = null;
		joinStratificationConfiguration = null;
	}
	
}
