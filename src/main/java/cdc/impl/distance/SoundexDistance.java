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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;

import cdc.components.AbstractDistance;
import cdc.components.AbstractStringDistance;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.paramspanel.DefaultParamPanelFieldCreator;
import cdc.gui.components.paramspanel.ParamPanelField;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.validation.NumberValidator;
import cdc.impl.distance.EditDistance.EditDistanceVisibleComponent;
import cdc.utils.Log;
import cdc.utils.RJException;
import cdc.utils.StringUtils;

public class SoundexDistance extends AbstractStringDistance {
	
	private static final int logLevel = Log.getLogLevel(SoundexDistance.class);
	
	public static final String PROP_SIZE = "soundex-length";
	
	public static final int DFAULT_SIZE = 5;
	
	private static class SoundexVisibleComponent extends EditDistanceVisibleComponent {
		
		private class CreatorQ extends DefaultParamPanelFieldCreator {
			public ParamPanelField create(JComponent parent, String param, String label, String defaultValue) {
				ParamPanelField field = super.create(parent, param, label, defaultValue);
				field.addConfigurationChangeListener(SoundexVisibleComponent.this);
				return field;
			}
		}
		
		private ParamsPanel editDst;
		private ParamsPanel soundexDst;
		
		public Object generateSystemComponent() throws RJException, IOException {
			Map props = new HashMap(editDst.getParams());
			props.putAll(soundexDst.getParams());
			return new SoundexDistance(props);
		}

		public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
			
			editDst = (ParamsPanel) super.getConfigurationPanel(objects, sizeX, sizeY);
			editDst.setBorder(BorderFactory.createTitledBorder("Underlying edit distance properties"));
			if (getRestoredParam(EditDistance.PROP_BEGIN_APPROVE_LEVEL) == null) {
				editDst.setPropertyValue(EditDistance.PROP_BEGIN_APPROVE_LEVEL, "0");
			}
			if (getRestoredParam(EditDistance.PROP_END_APPROVE_LEVEL) == null) {
				editDst.setPropertyValue(EditDistance.PROP_END_APPROVE_LEVEL, "0");
			}
			
			String[] defs = new String[1];
			if (getRestoredParam(PROP_SIZE) != null) {
				defs[0] = getRestoredParam(PROP_SIZE);
			} else {
				defs[0] = String.valueOf(DFAULT_SIZE);
			}
			CreatorQ l = new CreatorQ();
			Map creators = new HashMap();
			creators.put(PROP_SIZE, l);
			soundexDst = new ParamsPanel(
					new String[] {PROP_SIZE},
					new String[] {"Soundex length"},
					defs, creators
			);
			soundexDst.setBorder(BorderFactory.createTitledBorder("Soundex properties"));
			Map validators = new HashMap();
			validators.put(PROP_SIZE, new NumberValidator(NumberValidator.INTEGER));
			soundexDst.setValidators(validators);
			
			JPanel mainPanel = new JPanel(new GridBagLayout());
			mainPanel.add(soundexDst, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
			mainPanel.add(editDst, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
			mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			return mainPanel;

		}

		public Class getProducedComponentClass() {
			return SoundexDistance.class;
		}

		public String toString() {
			return "Soundex";
		}

		public boolean validate(JDialog dialog) {
			return soundexDst.doValidate() && editDst.doValidate();
		}
		
	}
	
	private EditDistance distance;
	private int size;
	
	public SoundexDistance() {
		super(null);
		this.size = DFAULT_SIZE;
		distance = new EditDistance();
		Log.log(getClass(), "Soundex size=" + this.size, 1);
	}
	
	public SoundexDistance(Map props) {
		super(props);
		distance = new EditDistance(props);
		if (getProperty(PROP_SIZE) != null) {
			this.size = Integer.parseInt((String)getProperty(PROP_SIZE));
		}
		
		Log.log(getClass(), "Soundex size=" + this.size, 1);
	}
	
	public String encodeToSoundex(String string) {
		if (logLevel >= 2) {
			Log.log(getClass(), "Encoding to soundex: '" + string + "'", 2);
		}
		
		if (StringUtils.isNullOrEmptyNoTrim(string)) {
			return "";
		}
		
		string = string.toLowerCase();
		char first = string.charAt(0);
		String reminder = string.substring(1);
		
		reminder = reminder.replaceAll("[aehiouwy]", "");
		String soundexString = first + reminder;
		
		char[] table = (soundexString + "0000000000").toLowerCase().toCharArray();
        
		for (int i = 1; i < table.length; i++) {
            switch (table[i]) {
                case 'b':
                case 'f':
                case 'p':
                case 'v':
                	table[i] = '1'; break;
                case 'c':
                case 'g':
                case 'j':
                case 'k':
                case 'q':
                case 's':
                case 'x':
                case 'z': 
                	table[i] = '2'; break;
                case 'd':
                case 't': 
                	table[i] = '3'; break;
                case 'l': 
                	table[i] = '4'; break;
                case 'm':
                case 'n': 
                	table[i] = '5'; break;
                case 'r': 
                	table[i] = '6'; break;
                default: 
                	table[i] = '0'; break;
            }
        }
        return new String(table, 0, size);
	}

	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new SoundexVisibleComponent();
	}

	public String toString() {
		return "Soundex distance " + getProperties();
	}

	public double distance(DataCell cellA, DataCell cellB) {
		String s1 = cellA.getValue().toString();
		String s2 = cellB.getValue().toString();
		
		String soundexS1 = encodeToSoundex(s1);
		String soundexS2 = encodeToSoundex(s2);
		
		
		if (logLevel >= 2) {
			Log.log(EqualFieldsDistance.class, s1 + "=?=" + s2 + ": s1=" + soundexS1 + ", s2=" + soundexS2 + ", distance:" + distance.distance(soundexS1, soundexS2), 2);
		}
		
		return distance.distance(soundexS1, soundexS2);
	}
	
	
	public static void main(String[] args) {
		AbstractDistance dst = new SoundexDistance();
		String[][] c1 = new String[][] {{"PAGE", "LEWIS"}};
		for (int i = 0; i < c1.length; i++) {
			System.out.println("Soundex between " + c1[i][0] + " and " + c1[i][1] + ": " + 
					dst.distance(new DataCell(DataColumnDefinition.TYPE_STRING, c1[i][0]), 
							new DataCell(DataColumnDefinition.TYPE_STRING, c1[i][1])));
		}
	}
}
