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

import java.awt.Dimension;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import cdc.gui.slope.SlopePanel;
import cdc.gui.validation.NumberValidator;
import cdc.utils.Log;
import cdc.utils.RJException;

public class QGramDistance extends AbstractStringDistance {
	
	private static final int logLevel = Log.getLogLevel(QGramDistance.class);
	
	public static final String PROP_Q = "q";
	public static final String PROP_APPROVE = "approve-level";
	public static final String PROP_DISAPPROVE = "disapprove-level";
	public static final double DEFAULT_APPROVE_LEVEL = 0.2;
	public static final double DEFAULT_DISAPPROVE_LEVEL = 0.4;
	public static final String DEFAULT_Q = "3";
	public static final String AUTO_Q = "auto";
	
	private static class QGramsVisibleComponent extends GUIVisibleComponent {
		
		private class CreatorV1 extends DefaultParamPanelFieldCreator {
			private SlopePanel slope;
			public CreatorV1(SlopePanel slope) {this.slope = slope;}
			public ParamPanelField create(JComponent parent, String param, String label, String defaultValue) {
				ParamPanelField field = super.create(parent, param, label, defaultValue);
				slope.bindV1(field);
				field.addConfigurationChangeListener(QGramsVisibleComponent.this);
				return field;
			}
		}

		private class CreatorV2 extends DefaultParamPanelFieldCreator {
			private SlopePanel slope;
			public CreatorV2(SlopePanel slope) {this.slope = slope;}
			public ParamPanelField create(JComponent parent, String param, String label, String defaultValue) {
				ParamPanelField field = super.create(parent, param, label, defaultValue);
				slope.bindV2(field);
				field.addConfigurationChangeListener(QGramsVisibleComponent.this);
				return field;
			}
		}
		
		private class CreatorQ extends DefaultParamPanelFieldCreator {
			public ParamPanelField create(JComponent parent, String param, String label, String defaultValue) {
				ParamPanelField field = super.create(parent, param, label, defaultValue);
				field.addConfigurationChangeListener(QGramsVisibleComponent.this);
				return field;
			}
		}
		
		
		private ParamsPanel panel;
		
		public Object generateSystemComponent() throws RJException, IOException {
			return new QGramDistance(panel.getParams());
		}

		public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
			Boolean boolCond = (Boolean)objects[0];
			String[] defs = new String[boolCond.booleanValue() ? 2:3];
			if (getRestoredParam(PROP_Q) != null) {
				defs[0] = getRestoredParam(PROP_Q);
			} else {
				defs[0] = DEFAULT_Q;
			}
			if (getRestoredParam(PROP_APPROVE) != null) {
				defs[1] = getRestoredParam(PROP_APPROVE);
			} else {
				defs[1] = String.valueOf(DEFAULT_APPROVE_LEVEL);
			}
			
			if (boolCond.booleanValue()) {
				panel = new ParamsPanel(
						new String[] {PROP_Q, PROP_APPROVE},
						new String[] {"Q value", "Acceptance level"},
						defs
				);
				Map validators = new HashMap();
				validators.put(PROP_Q, new NumberValidator(NumberValidator.INTEGER));
				validators.put(PROP_APPROVE, new NumberValidator(NumberValidator.DOUBLE));
				panel.setValidators(validators);
			} else {
				if (getRestoredParam(PROP_DISAPPROVE) != null) {
					defs[2] = getRestoredParam(PROP_DISAPPROVE);
				} else {
					defs[2] = String.valueOf(DEFAULT_DISAPPROVE_LEVEL);
				}
				
				SlopePanel slope = new SlopePanel(Double.parseDouble(defs[1]), Double.parseDouble(defs[2]));
				slope.setPreferredSize(new Dimension(240, 70));
				CreatorV1 v1 = new CreatorV1(slope);
				CreatorV2 v2 = new CreatorV2(slope);
				CreatorQ q = new CreatorQ();
				Map creators = new HashMap();
				creators.put(PROP_APPROVE, v1);
				creators.put(PROP_DISAPPROVE, v2);
				creators.put(PROP_Q, q);
				
				panel = new ParamsPanel(
						new String[] {PROP_Q, PROP_APPROVE, PROP_DISAPPROVE},
						new String[] {"Q value", "Approve level", "Disapprove level"},
						defs, creators
				);
				panel.append(slope);
				
				Map validators = new HashMap();
				validators.put(PROP_Q, new NumberValidator(NumberValidator.INTEGER));
				validators.put(PROP_APPROVE, new NumberValidator(NumberValidator.DOUBLE));
				validators.put(PROP_DISAPPROVE, new NumberValidator(NumberValidator.DOUBLE));
				panel.setValidators(validators);
			}
			
			return panel;
		}

		public Class getProducedComponentClass() {
			return QGramDistance.class;
		}

		public String toString() {
			return "Q grams distance";
		}

		public boolean validate(JDialog dialog) {
			return panel.doValidate();
		}
		
	}
	
	
	private double APPROVE = DEFAULT_APPROVE_LEVEL;
	private double DISAPPROVE = DEFAULT_DISAPPROVE_LEVEL;
	
	private int qgram;
	private ThreadLocal gramsS1 = new ThreadLocal() {
		protected Object initialValue() {
			return new HashMap();
		}
	};
	private ThreadLocal gramsS2 = new ThreadLocal() {
		protected Object initialValue() {
			return new HashMap();
		}
	};
	
	public QGramDistance(Map props) {
		super(props);
		//read q-gram
		if (getProperty(PROP_Q) != null) {
			if (!getProperty(PROP_Q).equals(AUTO_Q)) { 
				this.qgram = Integer.parseInt((String)getProperty(PROP_Q));
			}
		} else {
			this.qgram = -1;
		}
		if (getProperty(PROP_APPROVE) != null) {
			APPROVE = Double.parseDouble(getProperty(PROP_APPROVE));
		}
		if (getProperty(PROP_DISAPPROVE) != null) {
			DISAPPROVE = Double.parseDouble(getProperty(PROP_DISAPPROVE));
		}
		
		Log.log(getClass(), "Acceptance level=" + APPROVE + ", q=" + (qgram == -1 ? "auto" : qgram + ""), 1);
	}
	
	private class Counter {
		int count = 1;
	}
	
	public QGramDistance() {
		super(null);
		this.qgram = -1;
	}
	
	public QGramDistance(int qgram) {
		super(null);
		this.qgram = qgram;
	}
	
	private int measureDistance(Map gramsS1, Map gramsS2) {
		int diff = 0;
		if (logLevel >= 3) {
			Log.log(getClass(), "Grams first string: " + gramsS1, 3);
			Log.log(getClass(), "Grams second string: " + gramsS2, 3);
		}
		for (Iterator iterator = gramsS1.keySet().iterator(); iterator.hasNext();) {
			String gramS1 = (String) iterator.next();
			Counter counterS1 = (Counter) gramsS1.get(gramS1);
			Counter counterS2 = (Counter) gramsS2.get(gramS1);
			if (counterS2 == null) {
				diff += counterS1.count;
			} else {
				diff += Math.abs(counterS1.count - counterS2.count);
			}
		}
		
		for (Iterator iterator = gramsS2.keySet().iterator(); iterator.hasNext();) {
			String gramS2 = (String) iterator.next();
			if (!gramsS1.containsKey(gramS2)) {
				Counter counterS2 = (Counter) gramsS2.get(gramS2);
				diff += counterS2.count;
			}
		}
		return diff;
	}

	private void findGrams(String string, Map gramsS1, int q) {
		for (int i = 0; i < string.length(); i++) {
			String qgram = string.substring(i, (i + q) > string.length() ? string.length() : (i + q));
			Counter counter = (Counter) gramsS1.get(qgram);
			if (counter == null) {
				Counter c = new Counter();
				gramsS1.put(qgram, c);
			} else {
				counter.count++;
			}
		}
	}

	private int findGram(int size) {
		int q = 1;
		if(size > 60) {
			q = 4;
		} else if(size > 40) {
			q = 3;
		} else if (size > 20) {
			q = 2;
		}
		return q;
	}

	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new QGramsVisibleComponent();
	}

	public String toString() {
		return "Q grams distance " + getProperties();
	}

	private int distanceInt(DataCell cellA, DataCell cellB) {
		
		String s1 = cellA.getValue().toString();
		String s2 = cellB.getValue().toString();
		int qgram;
		if (this.qgram == -1) {
			qgram = findGram(s1.length() + s2.length());
			Log.log(getClass(), "Auto q found: " + qgram, 2);
		} else {
			qgram = this.qgram;
		}
		
		findGrams(s1, ((Map)gramsS1.get()), qgram);
		findGrams(s2, ((Map)gramsS2.get()), qgram);
		
		int distance = measureDistance(((Map)gramsS1.get()), ((Map)gramsS2.get()));
		
		if (logLevel >= 2) {
			Log.log(EqualFieldsDistance.class, s1 + "=?=" + s2 + ": " + distance, 2);
		}
		
		return distance;
	}

	public double distance(DataCell cellA, DataCell cellB) {
		double distance = distanceInt(cellA, cellB);
		double approve = (int)Math.round((((Map)gramsS1.get()).size() + ((Map)gramsS2.get()).size()) * APPROVE);
		double disapprove = (int)Math.round((((Map)gramsS1.get()).size() + ((Map)gramsS2.get()).size()) * DISAPPROVE);
		((Map)gramsS1.get()).clear();
		((Map)gramsS2.get()).clear();
		if (distance > disapprove) {
			return 0; 
		} else if (distance < approve) {
			return 100;
		} else {
			//System.out.println("TODO: need to check it: QGRAMDISTANCE");
			//return 0;
//			System.out.println("dis: " + disapprove);
//			System.out.println("appr: " + approve);
//			System.out.println("dst: " + distance);
			if (disapprove == approve) {
				return 0;
			} else {
				return (100 - 100/(disapprove - approve)*(distance - approve));
			}
		}
	}
	
	public static void main(String[] args) {
		Map params = new HashMap();
		params.put(PROP_Q, "3");
		params.put(PROP_APPROVE, "0.2");
		params.put(PROP_DISAPPROVE, "0.4");
		AbstractDistance d = new QGramDistance(params);
		System.out.println(d.distance(new DataCell(DataColumnDefinition.TYPE_STRING, "DONOVAN BLACK"), new DataCell(DataColumnDefinition.TYPE_STRING, "DONOVAN PACK")));
	}
	
	
}
