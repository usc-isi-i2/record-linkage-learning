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
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;

import cdc.components.AbstractDistance;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.paramspanel.CheckBoxParamPanelFieldCreator;
import cdc.gui.components.paramspanel.ComponentFactory;
import cdc.gui.components.paramspanel.ComponentFactoryInterface;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.components.paramspanel.RadioParamPanelFieldCreator;
import cdc.utils.CompareFunctionInterface;
import cdc.utils.RJException;
import cdc.utils.comparators.NumberComparator;

public class NumericDistance extends AbstractDistance {

	public static final String PROP_PERCENT = "percent-difference";
	public static final String PROP_NUMBER_RANGE = "numeric-difference";
	public static final String PROP_RANGE_TYPE = "range-type";
	public static final String PROP_LINERAL = "use-lineral-approximation";
	//private static final String[] RANGE_LABELS = {"[value - epsilon, value + epsilon]", "[value - epsilon, value]", "[value, value + epsilon]"};
	//private static final String[] RANGE_VALUES = {"0", "1", "2"};
	
	public static class NumericDistanceComponent extends GUIVisibleComponent {

		private ParamsPanel panel;
		
		public Object generateSystemComponent() throws RJException, IOException {
			return new NumericDistance(panel.getParams());
		}

		public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
			ButtonGroup group = new ButtonGroup();
			String[] defs = new String[] {"0", "0", "true"};
			if (getRestoredParam(PROP_NUMBER_RANGE) != null) {
				defs[0] = getRestoredParam(PROP_NUMBER_RANGE);
			} else if (getRestoredParam(PROP_PERCENT) != null) {
				defs[1] = getRestoredParam(PROP_PERCENT);
			}
			if (getRestoredParam(PROP_LINERAL) != null) {
				defs[2] = getRestoredParam(PROP_LINERAL);
			}
			
			ComponentFactoryInterface factoryRange = ComponentFactory.compoundFactory(new ComponentFactoryInterface[] {
					ComponentFactory.labelFactory("Between (value - "),
					ComponentFactory.jSpinnerFactory(new SpinnerNumberModel(0.0, 0.0, 10000000.0, 0.01), new Dimension(60, 20)),
					ComponentFactory.labelFactory(") and (value + "),
					ComponentFactory.jSpinnerFactory(new SpinnerNumberModel(0.0, 0.0, 10000000.0, 0.01), new Dimension(60, 20)),
					ComponentFactory.labelFactory(")")
			});
			RadioParamPanelFieldCreator creatorRange = new RadioParamPanelFieldCreator(group, factoryRange, this);
			
			ComponentFactoryInterface factoryPercentage = ComponentFactory.compoundFactory(new ComponentFactoryInterface[] {
					ComponentFactory.labelFactory("Between (value - "),
					ComponentFactory.jSpinnerFactory(new SpinnerNumberModel(0.0, 0.0, 10000000.0, 0.01), new Dimension(60, 20)),
					ComponentFactory.labelFactory("%) and (value + "),
					ComponentFactory.jSpinnerFactory(new SpinnerNumberModel(0.0, 0.0, 10000000.0, 0.01), new Dimension(60, 20)),
					ComponentFactory.labelFactory("%)")
			});
			RadioParamPanelFieldCreator creatorPercent = new RadioParamPanelFieldCreator(group, factoryPercentage, this);
			
			Map creators = new HashMap();
			creators.put(PROP_NUMBER_RANGE, creatorRange);
			creators.put(PROP_PERCENT, creatorPercent);
			creators.put(PROP_LINERAL, new CheckBoxParamPanelFieldCreator(this));
			
			return panel = new ParamsPanel(new String[] {PROP_NUMBER_RANGE, PROP_PERCENT, PROP_LINERAL}, 
					new String[] {"Range (fixed value)", "Range (percentage)", "Use linear approximation"}, 
					defs, creators);
			
		}

		public Class getProducedComponentClass() {
			return NumericDistance.class;
		}

		public String toString() {
			return "Numeric distance";
		}

		public boolean validate(JDialog dialog) {
			if (panel.getParameterValue(PROP_NUMBER_RANGE) == null &&
					panel.getParameterValue(PROP_PERCENT) == null) {
				JOptionPane.showMessageDialog(dialog, "Please select either max percentage difference or max range for numeric distance");
				return false;
			}
			return true;
		}
		
	}
	
	private double[] percent;
	private double[] numberRange;
	private boolean lineralOn;
	
	public NumericDistance(Map properties) {
		super(properties);
		if (getProperty(PROP_NUMBER_RANGE) != null) {
			percent = null;
			String[] numbers = getProperty(PROP_NUMBER_RANGE).split(",");
			if (numbers.length == 1) {
				numberRange = new double[] {Double.parseDouble(numbers[0]), Double.parseDouble(numbers[0])};
			} else {
				numberRange = new double[] {Double.parseDouble(numbers[0]), Double.parseDouble(numbers[1])};
			}
		} else {
			numberRange = null;
			String[] numbers = getProperty(PROP_PERCENT).split(",");
			if (numbers.length == 1) {
				percent = new double[] {Double.parseDouble(numbers[0]), Double.parseDouble(numbers[0])};
			} else {
				percent = new double[] {Double.parseDouble(numbers[0]), Double.parseDouble(numbers[1])};
			}
		}
		lineralOn = Boolean.parseBoolean(getProperty(PROP_LINERAL));
	}

	public double distance(DataCell cellA, DataCell cellB) {
		try {
			double val1 = Double.parseDouble(String.valueOf(cellA.getValue()));
			double val2 = Double.parseDouble(String.valueOf(cellB.getValue()));
			if (percent == null) {
				return getDistanceRange(val1, val2, numberRange);
			} else {
				return getDistancePercent(val1, val2);
			}
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private double getDistancePercent(double val1, double val2) {
		double n1 = val1 * percent[0] / 100;
		double n2 = val1 * percent[1] / 100;
		return getDistanceRange(val1, val2, new double[] {n1, n2});
	}

	private double getDistanceRange(double val1, double val2, double[] range) {
		double diff = Math.abs(val1 - val2);
		if (diff > range[0] && val1 > val2) {
			return 0;
		} else if (diff > range[1] && val1 < val2) {
			return 0;
		} else if (val1 > val2) {
			if (range[0] == 0) {
				return val1 == val2 ? 100: 0;
			}
			if (lineralOn) {
				return (range[0] - diff) / range[0] * 100;
			} else {
				return 100;
			}
		} else {
			if (range[1] == 0) {
				return val1 == val2 ? 100: 0;
			}
			if (lineralOn) {
				return (range[1] - diff) / range[1] * 100;
			} else {
				return 100;
			}
		}
	}
	
	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new NumericDistanceComponent();
	}
	
	public CompareFunctionInterface getCompareFunction(DataColumnDefinition colA, DataColumnDefinition colB) {
		return new NumberComparator();
	}
	
	public String toString() {
		return "Numeric distance " + getProperties();
	}

}
