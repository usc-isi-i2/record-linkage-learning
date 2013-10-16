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

import java.awt.Window;

import javax.swing.JComponent;

import cdc.components.AbstractDataSource;
import cdc.components.Filter;
import cdc.gui.Configs;
import cdc.gui.wizards.AbstractWizard;
import cdc.gui.wizards.WizardAction;
import cdc.gui.wizards.specific.actions.DSConfigureTypeAction;
import cdc.gui.wizards.specific.actions.DSConfigureAttrsAction;
import cdc.gui.wizards.specific.actions.DSConfigurePreprocessingAction;
import cdc.impl.deduplication.DeduplicationConfig;

public class DataSourceWizard {
	
	private static String[] steps = new String[] {
		"General data source configuration (step 1 of 3)",
		"Data source fields (step 2 of 3)",
		"Preprocessing configuration (step 3 of 3)"
	};
	
	private AbstractWizard wizard;
	
	private DSConfigureTypeAction sourceAction;
	private DSConfigureAttrsAction sourceFieldsAction;
	private DSConfigurePreprocessingAction sourceDeduplication;
	
	public DataSourceWizard(int id, Window parent, AbstractDataSource source, JComponent component, String defaultName) {
		this(id, parent, source, component, defaultName, true);
	}
	
	public DataSourceWizard(int id, Window parent, AbstractDataSource source, JComponent component, String defaultName, boolean showDedupeOption) {
		
		sourceAction = new DSConfigureTypeAction(defaultName);
		sourceFieldsAction = new DSConfigureAttrsAction(id, sourceAction, showDedupeOption);
		sourceDeduplication = new DSConfigurePreprocessingAction(sourceAction, source, showDedupeOption);
		sourceAction.setDataSource(source);
		
		WizardAction[] actions = new WizardAction[] {
				sourceAction,
				sourceFieldsAction,
				sourceDeduplication
		};
		
		wizard = new AbstractWizard(parent, actions, steps);
		wizard.setLocationRelativeTo(component);
		wizard.setMinimum(Configs.DFAULT_WIZARD_SIZE);
	}
	
	public int getResult() {
		return wizard.getResult();
	}
	
	public AbstractDataSource getConfiguredDataSource() {
		DeduplicationConfig deduplication = sourceDeduplication.getDeduplicationConfig();
		Filter filter = sourceDeduplication.getFilter();
		AbstractDataSource source = sourceAction.getDataSource();
		source.setDeduplicationConfig(deduplication);
		source.setFilter(filter);
		return source;
	}
	
	public void dispose() {
		sourceAction.dispose();
		sourceFieldsAction.dispose();
		sourceAction = null;
		sourceFieldsAction = null;
	}

	public void bringToFront() {
		wizard.toFront();
	}
}
