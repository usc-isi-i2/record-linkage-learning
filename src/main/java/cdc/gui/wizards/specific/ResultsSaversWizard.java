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

import javax.swing.JComponent;
import javax.swing.JFrame;

import cdc.components.AbstractResultsSaver;
import cdc.gui.Configs;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.gui.wizards.specific.actions.ResultsConfigureSaversAction;

public class ResultsSaversWizard {
	
	private static String[] steps = new String[] {
		"Results saving configuration (step 1 of 1)"
	};
	
	private AbstractWizard wizard;
	
	private ResultsConfigureSaversAction resultSaversActions;
	
	public ResultsSaversWizard(JFrame parent, JComponent source) {
		
		resultSaversActions = new ResultsConfigureSaversAction();
		
		WizardAction[] actions = new WizardAction[] {
				resultSaversActions
		};
		
		wizard = new AbstractWizard(parent, actions, steps);
		wizard.setLocationRelativeTo(source);
		wizard.setMinimum(Configs.DFAULT_WIZARD_SIZE);
	}
	
	public int getResult() {
		return wizard.getResult();
	}
	
	public AbstractResultsSaver getConfiguredResultSaver() {
		return resultSaversActions.getResultsSaver();
	}
	
	public void loadConfig(AbstractResultsSaver savers) {
		resultSaversActions.setResultSavers(savers);
	}
	
	public void dispose() {
		resultSaversActions.dispose();
		resultSaversActions = null;
	}
}
