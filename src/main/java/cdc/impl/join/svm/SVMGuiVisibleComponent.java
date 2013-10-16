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


package cdc.impl.join.svm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JPanel;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoinCondition;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.paramspanel.ComboBoxPanelFieldCreator;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.components.paramspanel.SeparatorPanelFieldCreator;
import cdc.gui.validation.NumberValidator;
import cdc.impl.join.blocking.BlockingJoin;
import cdc.utils.RJException;

public class SVMGuiVisibleComponent extends GUIVisibleComponent {

	private static final String LABEL_BLOCKING_FUNCTION = "Blocking function";
	private static final String LABEL_BLOCKING_ATTRIBUTE = "Blocking attribute";
	private static final String LABEL_LEARNING_ROUNDS = "Number of learning rounds";
	private static final String LABEL_YM = "Size of training set incremental (matches)";
	private static final String LABEL_YN = "Size of training set incremental (non-matches)";
	private static final String LABEL_MATCHING_MARGIN = "Margin for matching records (treshold approach)";
	private static final String LABEL_NONMATCHING_MARGIN = "Margin for non-matching record (treshold approach)";
	private static final String LABEL_WM = "Size of seeded training set (matches)";
	private static final String LABEL_WN = "Size of seeded training set (non-matches)";
	private static final String LABEL_TRAINING_SELECTION = "Selection of training examples";
	
	private static final String[] LABELS_SELECTION_METHOD = {"Threshold approach (speed)", "Nearest approach (accuracy)"};
	
	private static final String DEFAULT_LEARNING_ROUNDS = "4";
	private static final String DEFAULT_YM = "200";
	private static final String DEFAULT_YN = "400";
	private static final String DEFAULT_WM = "200";
	private static final String DEFAULT_WN = "1000";
	private static final String DEFAULT_MATCHING_MARGIN = "0.85";
	private static final String DEFAULT_NON_MATCHING_MARGIN = "0.3";
	private static final String DEFAULT_TRAINING_SELECTION = "nearest";
	
	public static final String functions[] = new String[] {
			"Equality metric",
			"Soundex, length = 4",
			"Soundex, length = 5",
			"Soundex, length = 6",
			"Soundex, length = 7"
		};
	public static final String functionsEncoded[] = new String[] {
		"equality",
		"soundex(4)",
		"soundex(5)",
		"soundex(6)",
		"soundex(7)"
	};
	
	protected AbstractDataSource sourceA;
	protected AbstractDataSource sourceB;
	protected AbstractJoinCondition joinCondition;
	protected DataColumnDefinition[] outModel;
	protected String[] hashAttrs;
	
	protected ParamsPanel buffer;
	
	public Object generateSystemComponent() throws RJException, IOException {
		
		Map params = buffer.getParams();
		String attribute = (String) params.remove(SVMJoin.ATTR_BLOCKING_ATTR);
		String function = (String) params.remove(SVMJoin.ATTR_BLOCKING_FUNCTION);
		Map properties = new HashMap();
		properties.putAll(params);
		for (int i = 0; i < hashAttrs.length; i++) {
			if (hashAttrs[i].equals(attribute)) {
				properties.put(BlockingJoin.BLOCKING_PARAM, String.valueOf(i));
				break;
			}
		}
		for (int i = 0; i < functions.length; i++) {
			if (functions[i].equals(function)) {
				properties.put(BlockingJoin.BLOCKING_FUNCTION, functionsEncoded[i]);
				break;
			}
		}
		
		return new SVMJoin(sourceA.getPreprocessedDataSource(), sourceB.getPreprocessedDataSource(), outModel, joinCondition, properties);
	}

	public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
		
		this.sourceA = (AbstractDataSource) objects[0];
		this.sourceB = (AbstractDataSource) objects[1];
		this.outModel = (DataColumnDefinition[]) objects[2];
		this.joinCondition = (AbstractJoinCondition) objects[3];
		
		hashAttrs = new String[joinCondition.getLeftJoinColumns().length];
		for (int i = 0; i < hashAttrs.length; i++) {
			hashAttrs[i] = joinCondition.getLeftJoinColumns()[i] + 
				" and " + joinCondition.getRightJoinColumns()[i];
		}
		
		
		Map creators = new HashMap();
		creators.put(SVMJoin.ATTR_BLOCKING_ATTR, new ComboBoxPanelFieldCreator(hashAttrs));
		creators.put(SVMJoin.ATTR_BLOCKING_FUNCTION, new ComboBoxPanelFieldCreator(functions));
		creators.put(SVMJoin.ATTR_TRAINING_SELECTION_METHOD, new SeparatorPanelFieldCreator(SVMJoin.SELECTION_METHODS, LABELS_SELECTION_METHOD, 1, 2, false));
		
		String[] defaults = new String[] {hashAttrs[0], "Soundex, length = 5", DEFAULT_LEARNING_ROUNDS, DEFAULT_TRAINING_SELECTION, DEFAULT_MATCHING_MARGIN, DEFAULT_NON_MATCHING_MARGIN, DEFAULT_WM, DEFAULT_WN, DEFAULT_YM, DEFAULT_YN};
		String restoredAttribute = getRestoredParam(BlockingJoin.BLOCKING_PARAM);
		if (restoredAttribute != null) {
			defaults[0] = hashAttrs[Integer.parseInt(restoredAttribute)];
		}
		String restoredFunction = getRestoredParam(BlockingJoin.BLOCKING_FUNCTION);
		String properFunction = null;
		for (int i = 0; i < functions.length; i++) {
			if (functionsEncoded[i].equals(restoredFunction)) {
				properFunction = functions[i];
				break;
			}
		}
		if (properFunction != null) {
			defaults[1] = properFunction;
		}
		
		String rounds = getRestoredParam(SVMJoin.ATTR_LEARNING_ROUNDS);
		if (rounds != null) {
			defaults[2] = rounds;
		}
		String selection = getRestoredParam(SVMJoin.ATTR_TRAINING_SELECTION_METHOD);
		if (selection != null) {
			defaults[3] = selection;
		}
		String marginM = getRestoredParam(SVMJoin.ATTR_MATCHING_MARGIN);
		if (marginM != null) {
			defaults[4] = marginM;
		}
		String marginN = getRestoredParam(SVMJoin.ATTR_NON_MATCHING_MARGIN);
		if (marginN != null) {
			defaults[5] = marginN;
		}
		String wm = getRestoredParam(SVMJoin.ATTR_WM);
		if (wm != null) {
			defaults[6] = wm;
		}
		String wn = getRestoredParam(SVMJoin.ATTR_WN);
		if (wn != null) {
			defaults[7] = wn;
		}
		String ym = getRestoredParam(SVMJoin.ATTR_YM);
		if (ym != null) {
			defaults[8] = ym;
		}
		String yn = getRestoredParam(SVMJoin.ATTR_YN);
		if (yn != null) {
			defaults[9] = yn;
		}
		
		Map validators = new HashMap();
		validators.put(SVMJoin.ATTR_LEARNING_ROUNDS, new NumberValidator(NumberValidator.INTEGER));
		validators.put(SVMJoin.ATTR_MATCHING_MARGIN, new NumberValidator(NumberValidator.DOUBLE));
		validators.put(SVMJoin.ATTR_NON_MATCHING_MARGIN, new NumberValidator(NumberValidator.DOUBLE));
		validators.put(SVMJoin.ATTR_WM, new NumberValidator(NumberValidator.INTEGER));
		validators.put(SVMJoin.ATTR_WN, new NumberValidator(NumberValidator.INTEGER));
		validators.put(SVMJoin.ATTR_YM, new NumberValidator(NumberValidator.INTEGER));
		validators.put(SVMJoin.ATTR_YN, new NumberValidator(NumberValidator.INTEGER));
		
		buffer = new ParamsPanel(new String[] {SVMJoin.ATTR_BLOCKING_ATTR, SVMJoin.ATTR_BLOCKING_FUNCTION, SVMJoin.ATTR_LEARNING_ROUNDS, SVMJoin.ATTR_TRAINING_SELECTION_METHOD, SVMJoin.ATTR_MATCHING_MARGIN, SVMJoin.ATTR_NON_MATCHING_MARGIN, SVMJoin.ATTR_WM, SVMJoin.ATTR_WN, SVMJoin.ATTR_YM, SVMJoin.ATTR_YN}, 
				new String[] {LABEL_BLOCKING_ATTRIBUTE, LABEL_BLOCKING_FUNCTION, LABEL_LEARNING_ROUNDS, LABEL_TRAINING_SELECTION, LABEL_MATCHING_MARGIN, LABEL_NONMATCHING_MARGIN, LABEL_WM, LABEL_WN, LABEL_YM, LABEL_YN},
				defaults,
				creators);
		buffer.setValidators(validators);
		
		return buffer;
	}

	public String toString() {
		return "SVM join (experimental)";
	}
	public Class getProducedComponentClass() {
		return SVMJoin.class;
	}

	public boolean validate(JDialog dialog) {
		return buffer.doValidate();
	}
}
