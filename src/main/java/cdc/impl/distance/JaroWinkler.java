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


package cdc.impl.distance;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;

import cdc.components.AbstractStringDistance;
import cdc.datamodel.DataCell;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.paramspanel.DefaultParamPanelFieldCreator;
import cdc.gui.components.paramspanel.ParamPanelField;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.validation.NumberValidator;
import cdc.utils.RJException;

public class JaroWinkler extends AbstractStringDistance {

	private static class JaroWinklerGUIComponent extends GUIVisibleComponent {

		private static final String LENGTH = "Common prefix length";
		private static final String WEIGHT = "Scaling factor";
		
		private class FieldCreator extends DefaultParamPanelFieldCreator {
			public ParamPanelField create(JComponent parent, String param, String label, String defaultValue) {
				ParamPanelField field = super.create(parent, param, label, defaultValue);
				field.addConfigurationChangeListener(JaroWinklerGUIComponent.this);
				return field;
			}
		}
		
		private ParamsPanel panel;
		
		public Object generateSystemComponent() throws RJException, IOException {
			return new JaroWinkler(panel.getParams());
		}

		public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
			
			String[] params = new String[] {PROP_PREFIX_LENGTH, PROP_PREFIX_WEIGHT};
			String[] defaults = new String[] {"4", "0.1"};
			String[] labels = new String[] {LENGTH, WEIGHT};
			
			Map listeners = new HashMap();
			listeners.put(PROP_PREFIX_LENGTH, new FieldCreator());
			listeners.put(PROP_PREFIX_WEIGHT, new FieldCreator());
			
			Map validators = new HashMap();
			validators.put(PROP_PREFIX_LENGTH, new NumberValidator(NumberValidator.INTEGER));
			validators.put(PROP_PREFIX_LENGTH, new NumberValidator(NumberValidator.DOUBLE));
			panel = new ParamsPanel(params, labels, defaults, listeners);
			panel.setValidators(validators);
			
			return panel;
			
		}

		public Class getProducedComponentClass() {
			return JaroWinkler.class;
		}

		public String toString() {
			return "Jaro-Winkler";
		}

		public boolean validate(JDialog dialog) {
			return panel.doValidate();
		}
		
	}
	
	
	public static final String PROP_PREFIX_LENGTH = "pref-length";
	public static final String PROP_PREFIX_WEIGHT = "pref-weight";
	
	private int maxPrefLength = 4;
	private double weight = 0.1;
	
	public JaroWinkler(Map properties) {
		super(properties);
		if (getProperty(PROP_PREFIX_LENGTH) != null) {
			maxPrefLength = Integer.parseInt(getProperty(PROP_PREFIX_LENGTH));
		}
		if (getProperty(PROP_PREFIX_WEIGHT) != null) {
			weight = Double.parseDouble(getProperty(PROP_PREFIX_WEIGHT));
		}
	}

	public double distance(DataCell cellA, DataCell cellB) {
		String s1 = cellA.getValue().toString();
		String s2 = cellB.getValue().toString();
		
		return distance(s1, s2);
	}
	
	private double distance(String s1, String s2) {
		s1 = s1.toLowerCase();
		s2 = s2.toLowerCase();
		double dist = score(s1, s2);
		dist = dist + commonPrefix(s1, s2, maxPrefLength) * weight * (100 - dist);
		if (dist < 0) dist = 0;
		if (dist > 100) dist = 100;
		return dist;
	}

	public double score(String s1,String s2) {

		/**
		 * TODO: Maybe the commented one is better to solve switched first/last names 
		 * Is there any paper to acknowledge that?
		 */
		int limit = (s1.length() > s2.length()) ? s2.length()/2 + 1 : s1.length()/2 + 1;
		//int limit = (s1.length() > s2.length()) ? s2.length() : s1.length();
		
		String c1 = commonChars(s1, s2, limit);
		String c2 = commonChars(s2, s1, limit);

		if ((c1.length() != c2.length()) || c1.length() == 0 || c2.length() == 0) {
			return 0;
		}
		int transpositions = transpositions(c1, c2);
		return (c1.length() / ((double)s1.length()) + c2.length() / ((double)s2.length()) + (c1.length() - transpositions) / ((double)c1.length())) / 3.0 * 100;
	}



	private String commonChars(String s1, String s2, int limit) {

		StringBuilder common = new StringBuilder(); 
		StringBuilder copy = new StringBuilder(s2);

		for (int i = 0; i < s1.length(); i++) {
			char ch = s1.charAt(i);
			boolean foundIt = false;
			for (int j = Math.max(0, i - limit); !foundIt && j < Math.min(i + limit, s2.length()); j++) {
				if (copy.charAt(j)==ch) {
					foundIt = true;
					common.append(ch);
					copy.setCharAt(j, '*');
				}
			}
		}
		return common.toString();
	}

	private int transpositions(String c1, String c2) {
		int transpositions = 0;
		for (int i = 0; i < c1.length(); i++) {
			if (c1.charAt(i) != c2.charAt(i)) { 
				transpositions++;
			}
		}
		return transpositions / 2;
	}
	
	private static int commonPrefix(String c1,String c2, int maxPref) {
		int n = Math.min(maxPref, Math.min(c1.length(), c2.length()) );
		for (int i = 0; i < n; i++) {
			if (c1.charAt(i) != c2.charAt(i)) {
				return i;
			}
		}
		return n;
	}
	
	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new JaroWinklerGUIComponent();
	}
	
	public String toString() {
		return "Jaro-Winkler " + getProperties();
	}
	
	public static void main(String[] args) {
		JaroWinkler dst = new JaroWinkler(new HashMap());
		System.out.println(dst.distance("William YOUNG", "William YOUNG"));
		System.out.println(dst.distance("John", "Jeremy"));
		System.out.println(dst.distance("YVETTE", "YEVETT"));
		System.out.println(dst.distance("MASSEY", "MASSIE"));
		System.out.println(dst.distance("DWAYNE", "DUANE"));
		
		com.wcohen.ss.JaroWinkler jw = new com.wcohen.ss.JaroWinkler();
		System.out.println(jw.explainScore("DWAYNE", "DUANE"));
		
	}

}
