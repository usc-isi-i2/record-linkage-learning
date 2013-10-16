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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import cdc.datamodel.DataCell;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.paramspanel.CheckBoxParamPanelFieldCreator;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.utils.Log;
import cdc.utils.RJException;
import cdc.utils.StringUtils;

public class AddressDistance extends EditDistance {

	private static final String PROP_RESOLVE_SECONDARY_LOC = "resolve-secondary-location";

	private static final Index indexStreet = new Index("./scripts/addr-str-suffix.txt");
	private static final Index indexSecondary = new Index("./scripts/addr-secondary-units.txt");
	
	private boolean resolveSecondary = true;
	
	public static class AddressDstVisibleComponent extends EditDistanceVisibleComponent {
		
		private ParamsPanel editDst;
		private ParamsPanel addressPanel;
		
		public Object generateSystemComponent() throws RJException, IOException {
			Map props = editDst.getParams();
			props.putAll(addressPanel.getParams());
			return new AddressDistance(props);
		}
		
		public Class getProducedComponentClass() {
			return AddressDistance.class;
		}
		
		public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
			editDst = (ParamsPanel) super.getConfigurationPanel(objects, sizeX, sizeY);
			if (StringUtils.isNullOrEmpty(getRestoredParam(EditDistance.PROP_BEGIN_APPROVE_LEVEL))) {
				editDst.setPropertyValue(EditDistance.PROP_BEGIN_APPROVE_LEVEL, "0.0");
			}
			if (StringUtils.isNullOrEmpty(getRestoredParam(EditDistance.PROP_END_APPROVE_LEVEL))) {
				editDst.setPropertyValue(EditDistance.PROP_END_APPROVE_LEVEL, "0.3");
			}
			editDst.setBorder(BorderFactory.createTitledBorder("Underlying edit distance properties"));
			
			Map listeners = new HashMap();
			listeners.put(PROP_RESOLVE_SECONDARY_LOC, new CheckBoxParamPanelFieldCreator());
			addressPanel = new ParamsPanel(new String[] {PROP_RESOLVE_SECONDARY_LOC}, 
					new String[] {"Use second level address information (e.g., APT, BLDG)"},
					new String[] {"true"}, listeners);
			addressPanel.setBorder(BorderFactory.createTitledBorder("Distance properties"));
			JPanel mainPanel = new JPanel(new GridBagLayout());
			mainPanel.add(addressPanel, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
			mainPanel.add(editDst, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
			mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			return mainPanel;
		}
		
		public String toString() {
			return "Street address distance";
		}
		
	}
	
	public AddressDistance(Map properties) {
		super(properties);
		if (getProperty(PROP_RESOLVE_SECONDARY_LOC) != null) {
			resolveSecondary = Boolean.parseBoolean(getProperty(PROP_RESOLVE_SECONDARY_LOC));
		}
	}

	public double distance(DataCell cell1, DataCell cell2) {
		
		String a1 = cell1.getValue().toString();
		String a2 = cell2.getValue().toString();
	
		Log.log(getClass(), "Comparing addresses: '" + a1 + "' and '" + a2 + "'", 2);
		
		StreetAddress c1 = normalize(a1);
		StreetAddress c2 = normalize(a2);
		
		Log.log(getClass(), "Parsed address: " + c1, 2);
		Log.log(getClass(), "Parsed address: " + c2, 2);
		
		//Log.log(getClass(), "Normalized values: '" + a1 + "' and '" + a2 + "'", 2);
		
		if ((c1.secondLevel == null && c2.secondLevel == null) || !resolveSecondary) {
			return matchStreetName(c1, c2, 0.5, 0.5);
		} else {
			//1. Street name - 50 (name = 40, prefix = 10)
			//2. Street number - 30
			double score = matchStreetName(c1, c2, 0.3, 0.5);
			
			//3. Second level - 20
			if (score != 0) {
				score += matchSecondLevelAddr(c1, c2, 0.2);
			}
			return score;
		}
	}
	
	private double matchStreetName(StreetAddress c1, StreetAddress c2, double weightNumber, double weightStrName) {
		double score = 0;
		
		if (c1.number != null && c2.number != null) {
			score += weightNumber * super.distance(c1.number, c2.number);
		}
		
		double maxScoreStr = 0;
		if (c1.strName != null && c2.strName != null) {
			
			//Try perfect match
			if (c1.strSuffix != null && c2.strSuffix != null) {
				maxScoreStr = super.distance(c1.strName, c2.strName) * 0.8;
				if (maxScoreStr != 0) {
					maxScoreStr += super.distance(c1.strSuffix, c2.strSuffix) * 0.2;
				}
			}
			
			//Try removing either of suffixes, adding the other to the street name
			if (c1.strSuffix != null) {
				maxScoreStr = tryMatch(c1.strName + " " + c1.strSuffix, c2.strName, maxScoreStr, 1.0);
			}
			if (c2.strSuffix != null) {
				maxScoreStr = tryMatch(c2.strName + " " + c2.strSuffix, c1.strName, maxScoreStr, 1.0);
			}
			
			//Try ignoring suffixes, but dump value by 0.3
			maxScoreStr = tryMatch(c1.strName, c2.strName, maxScoreStr, (c1.strSuffix == null && c2.strSuffix == null) ? 1.0 : 0.7);
			
		} else {
			//No street in one of addresses or both, test each with prefix of the other 
			if (c1.strName != null && c2.strSuffix != null) {
				maxScoreStr = tryMatch(c1.strName + " " + c1.strSuffix, c2.strSuffix, maxScoreStr, 0.7);
				maxScoreStr = tryMatch(c1.strName, c2.strSuffix, maxScoreStr, 0.7);
			}
			if (c2.strName != null && c1.strSuffix != null) {
				maxScoreStr = tryMatch(c2.strName + " " + c2.strSuffix, c1.strSuffix, maxScoreStr, 0.7);
				maxScoreStr = tryMatch(c2.strName, c1.strSuffix, maxScoreStr, 0.7);
			}
			if (c1.strName == null && c2.strName == null && c1.strSuffix != null && c2.strSuffix != null) {
				maxScoreStr = tryMatch(c1.strSuffix, c2.strSuffix, maxScoreStr, 0.1);
			}
		}
		
		if (maxScoreStr != 0) {
			score += maxScoreStr * weightStrName;
		}
		
		//Try to ignore number, and just compare strings of number + street + suffix. This will be adjusted by 0.6
		score = Math.max(score, super.distance(c1.toStringNoL2(), c2.toStringNoL2()) * (weightNumber + weightStrName) * 0.6);
		
		return score;
	}

	private double tryMatch(String s1, String s2, double maxScore, double adjustment) {
		return Math.max(maxScore, adjustment * super.distance(s1, s2));
	}

	private double matchSecondLevelAddr(StreetAddress c1, StreetAddress c2, double weight) {
		if (c1.secondLevel != null && c2.secondLevel != null) {
			return matchSecondLevelAddr(c1.secondLevel, c2.secondLevel, weight);
		}
		return 0;
	}

	private double matchSecondLevelAddr(SecondLevelAddress s1, SecondLevelAddress s2, double weight) {
		double maxScore = 0;
		if (s1.value != null && s2.value != null) {
			if (s1.prefix != null && s2.prefix != null) {
				maxScore = super.distance(s1.value, s2.value) * 0.8;
				maxScore += super.distance(s1.prefix, s2.prefix) * 0.2;
				maxScore = adjustForChildren(s1, s2, weight, maxScore);
			}
			if (s1.prefix != null) {
				double score = tryMatch(s1.prefix + " " + s1.value, s2.value, maxScore, 1.0);
				maxScore = Math.max(maxScore, adjustForChildren(s1, s2, weight, score));
			}
			if (s2.prefix != null) {
				double score = tryMatch(s2.prefix + " " + s2.value, s1.value, maxScore, 1.0);
				maxScore = Math.max(maxScore, adjustForChildren(s1, s2, weight, score));
			}
			
			maxScore = tryMatch(s1.value, s2.value, maxScore, (s1.prefix == null && s2.prefix == null) ? 1.0 : 0.7);
			
		} else {
			
			if (s1.value != null) {
				maxScore = tryMatch(s1.prefix + s1.value, s2.prefix, maxScore, 0.6);
				maxScore = tryMatch(s1.value, s2.prefix, maxScore, 0.6);
			}
			
			if (s2.value != null) {
				maxScore = tryMatch(s2.prefix + s2.value, s1.prefix, maxScore, 0.6);
				maxScore = tryMatch(s2.value, s1.prefix, maxScore, 0.6);
			}
			
		}
		
		maxScore = tryMatch(s1.toStringWithPref(), s2.toStringWithPref(), maxScore, 0.8);
		maxScore = tryMatch(s1.toStringNoPref(), s2.toStringNoPref(), maxScore, 0.5);
		
		return maxScore * weight;
	}

	private double adjustForChildren(SecondLevelAddress s1, SecondLevelAddress s2, double weight, double score) {
		if (s1.child != null && s2.child != null) {
			score = score * 0.7 + matchSecondLevelAddr(s1.child, s2.child, weight) * 0.3;
		} else if (s1.child != null || s2.child != null) {
			//Adjust score a bit as no perfect match
			score = score * 0.8;
		}
		return score;
	}

	private StreetAddress normalize(String addr) {
		String[] addrComponents = addr.toUpperCase().split("\\s+");
		
		if (addrComponents.length == 0) {
			return null;
		}
		
		StreetAddress address = new StreetAddress();
		SecondLevelAddress activeAddrL2 = null;
		address.number = addrComponents[0];
		
		StringBuffer buffer = new StringBuffer();
		for (int i = 1; i < addrComponents.length; i++) {
			
			String test = extractString(addrComponents[i]);
						
			String strSuffix = indexStreet.lookup(test);
			String secondLevel = indexSecondary.lookup(test);
			if (strSuffix != null && !isNextSpecial(addrComponents, i + 1)) {
				address.strName = buffer.toString();
				address.strSuffix = strSuffix;
				buffer = new StringBuffer();
			} else if (secondLevel != null) {
				SecondLevelAddress newAddrL2 = new SecondLevelAddress();
				if (address.strName == null) {
					address.strName = buffer.toString().replaceAll("-", "");
				} else {
					if (activeAddrL2 == null) {
						newAddrL2.value = buffer.toString().replaceAll("-", "");
					} else {
						activeAddrL2.value = buffer.toString().replaceAll("-", "");
					}
				}
				buffer = new StringBuffer();
				newAddrL2.prefix = secondLevel;
				if (address.secondLevel == null) {
					address.secondLevel = newAddrL2;
				} else {
					activeAddrL2.child = newAddrL2;
				}
				activeAddrL2 = newAddrL2;
			} else {
				buffer.append(addrComponents[i]);
			}
		}
		
		if (!buffer.toString().isEmpty()) {
			if (address.strName == null) {
				address.strName = buffer.toString().replaceAll("-", "");
			} else { 
				if (activeAddrL2 == null) {
					activeAddrL2 = new SecondLevelAddress();
					address.secondLevel = activeAddrL2;
				}
				activeAddrL2.value = buffer.toString().replaceAll("-", "");
			}
		}
		
		return address;
	}

	private String extractString(String test) {
		int trim = test.length() - 1;
		while (trim > 0 && test.charAt(trim) == '.') trim--;
		test = test.substring(0, trim + 1);
		return test;
	}
	
	private boolean isNextSpecial(String[] addrComponents, int index) {
		if (index >= addrComponents.length) {
			return false;
		}
		String test = extractString(addrComponents[index]);
		return indexStreet.lookup(test) != null;
	}

	private class StreetAddress {
		String number;
		String strName;
		String strSuffix;
		SecondLevelAddress secondLevel;
		
		public String toString() {
			return "Street=" + strName + "; number=" + (number == null ? "none" : number) + "; StrSuffix=" + 
					(strSuffix == null ? "none" : strSuffix) + 
					"; Second level addr=" + (secondLevel == null ? "none" : ("(" + secondLevel.toString() + ")"));
		}

		public String toStringNoL2() {
			return (number == null ? "" : (number + " ")) + (strName == null ? "" : (strName + " ")) + (strSuffix == null ? "" : strSuffix);
		}
		
	}
	
	private class SecondLevelAddress {
		String value;
		String prefix;
		SecondLevelAddress child;
		
		public String toString() {
			return "prefix=" + (prefix == null ? "none" : prefix) + "; value=" + (value == null ? "none" : value) + 
					"; additional info=" + (child == null ? "none" : ("(" + child + ")"));
		}

		public String toStringNoPref() {
			return (value != null ? value : "") + " " + (child != null ? child.toStringNoPref() : ""); 
		}

		public String toStringWithPref() {
			return (prefix != null ? prefix : "") + " " + (value != null ? value : "") + " " + (child != null ? child.toStringWithPref() : "");
		}
		
	}
	
	private static class Index {

		private Map index = new HashMap();
		
		public Index(String fName) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(fName));
				String line;
				while ((line = reader.readLine()) != null) {
					//Ignore comments
					if (!line.startsWith("#")) {
						String[] arr = line.split(",");
						
						//Self-mapping (Street -> Street)
						index.put(arr[0], arr[0]);
						
						//Remaining mappings (St -> Street)
						for (int i = 1; i < arr.length; i++) {
							if (index.containsKey(arr[i]) && !index.get(arr[i]).equals(arr[0])) {
								System.out.println("ERROR IN file " + fName + ". Conflict for line " + line + " (previously " + 
										arr[i] + " -> " + index.get(arr[i]) + "). Ignoring.");
							}
							index.put(arr[i], arr[0]);
						}
					}
				}
			} catch (IOException e) {
				throw new RuntimeException("Error reading file: " + fName, e);
			}
		}
		
		public String lookup(String key) {
			return (String) index.get(key);
		}
		
	}
	
	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new AddressDstVisibleComponent();
	}
	
	public String toString() {
		return "Street address distance " + getProperties();
	}
	
//	public static void main(String[] args) {
//		
//		AddressDistance dst = new AddressDistance(new HashMap());
//		test("1129 Clairemont LN. Apt D BLDG. B", dst);
//		test("1129 Clairemont Ave Apt E", dst);
//		test("1129 Clairemont Ave.   E   ", dst);
//		test("  1129 Clairemont   Ave.. .. Apt. E", dst);
//		test("1129 Clairemont Ave Apt. E", dst);
//		test("1129 Clairemont Ave Apt. E", dst);
//		test("1129 Clairemont ST Apt. E", dst);
//		
//		
//		String t1 = "1563 sandpiper court bldg E apt G";
//		String t2 = "1563 sandpiper ct apt E";
//		System.out.println("dst= " + dst.distance(new DataCell(DataColumnDefinition.TYPE_STRING, t1), new DataCell(DataColumnDefinition.TYPE_STRING, t2)));
//	}
//
//	private static void test(String addr, AddressDistance dst) {
//		System.out.println("'" + addr + "' --> '" + dst.normalize(addr) + "'");
//	}

}
