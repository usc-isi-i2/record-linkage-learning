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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.configuration.ConfiguredSystem;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.MainFrame;
import cdc.gui.components.dynamicanalysis.AnalysisWindowProvider;
import cdc.gui.components.paramspanel.CheckBoxParamPanelFieldCreator;
import cdc.gui.components.paramspanel.ParamPanelField;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.gui.external.JXErrorDialog;
import cdc.gui.validation.Validator;
import cdc.impl.MainApp;
import cdc.impl.conditions.WeightedJoinCondition;
import cdc.impl.distance.parampanel.DateFormatPanelField;
import cdc.impl.distance.parampanel.DateFormatPanelFieldCreator;
import cdc.impl.distance.parampanel.DateRangeParamFieldCreator;
import cdc.utils.CompareFunctionInterface;
import cdc.utils.DateUtils;
import cdc.utils.Log;
import cdc.utils.Props;
import cdc.utils.RJException;
import cdc.utils.StringUtils;
import cdc.utils.comparators.DateComparator;

public class DateDistance extends AbstractDistance {

	private static final long MAX_DATE_TEST_INTERVAL = Props.getLong("max-test-date-format-interval");
	
	public static final String PROP_FORMAT1 = "date-format-left";
	public static final String PROP_FORMAT2 = "date-format-right";
	public static final String PROP_LINERAL = "use-lineral-approximation";
	public static final String PROP_RANGE1 = "difference-before";
	public static final String PROP_RANGE2 = "difference-after";

	//private static final String PROP_SOURCE1 = "source-1";
	//private static final String PROP_SOURCE2 = "source-2";
	
	public static class DateGUIVisibleComponent extends GUIVisibleComponent {
		
		private class Creator extends DateFormatPanelFieldCreator {
			
			private class WorkThread extends Thread {
				volatile boolean goOn = true;
				DataColumnDefinition column;
				String[] formatsArray;
				public WorkThread(DataColumnDefinition column) {
					this.column = column;
				}
				public void run() {
					//System.out.println("Started...");
					ConfiguredSystem system = MainFrame.main.getConfiguredSystem();
					AbstractDataSource source = null;
					if (system.getSourceA().getSourceName().equals(column.getSourceName())) {
						source = system.getSourceA();
					} else {
						source = system.getSourceB();
					}
					try {
						source.reset();
						List formats = null;
						long start = System.currentTimeMillis();
						while (true && goOn && start + MAX_DATE_TEST_INTERVAL > System.currentTimeMillis()) {
							try {
								DataRow row = source.getNextRow();
								if (row == null) {
									break;
								}
								DataCell cell = row.getData(column);
								String strDate = String.valueOf(cell.getValue());
								formats = new ArrayList(Arrays.asList(DateUtils.parse(strDate)));
							} catch (IllegalArgumentException e) {
								continue;
							}
							break;
						}
						if (!goOn || start + MAX_DATE_TEST_INTERVAL < System.currentTimeMillis()) {
							if (goOn) {
								field.setSuggestions(null);
							}
							return;
						}
						while (start + MAX_DATE_TEST_INTERVAL > System.currentTimeMillis() && goOn) {
							DataRow row = source.getNextRow();
							if (row == null) {
								break;
							}
							try {
								DataCell cell = row.getData(column);
								String strDate = String.valueOf(cell.getValue());
								List list = Arrays.asList(DateUtils.parse(strDate));
								for (Iterator iterator = list.iterator(); iterator.hasNext();) {
									String format = (String) iterator.next();
									if (!formats.contains(format)) {
										formats.add(format);
									}
								}
							} catch (IllegalArgumentException e) {
								continue;
							}
						}
						if (!goOn || formats == null) {
							return;
						}
						formatsArray = new String[formats.size()];
						for (int i = 0; i < formatsArray.length; i++) {
							formatsArray[i] = (String) formats.get(i);
						}
						//System.out.println("Here...");
						if (goOn) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									field.setSuggestions(formatsArray);
								}
							});
						}
					} catch (IOException e) {
						e.printStackTrace();
						JXErrorDialog.showDialog(MainFrame.main, "Error", e);
					} catch (RJException e) {
						e.printStackTrace();
						JXErrorDialog.showDialog(MainFrame.main, "Error", e);
					}
				}
			}
			
			private JList component;
			private DateFormatPanelField field;
			private WorkThread thread;
			
			public Creator(JList attrComponent) {
				this.component = attrComponent;
			}

			public ParamPanelField create(JComponent parent, String param, String label, String defaultValue) {
				field = (DateFormatPanelField) super.create(parent, param, label, defaultValue);
				//System.out.println("Created...");
				if (component.getSelectedValue() != null) {
					thread = new WorkThread((DataColumnDefinition) component.getSelectedValue());
					field.setSuggestions(null);
					field.startWork();
					thread.start();
				}
				component.addListSelectionListener(new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent arg0) {
						DataColumnDefinition column = (DataColumnDefinition) component.getSelectedValue();
						System.out.println(column);
						if (column == null) {
							return;
						}
						if (thread != null) {
							thread.goOn = false;
						}
						field.setSuggestions(null);
						field.startWork();
						thread = new WorkThread(column);
						thread.start();
					}
				});
				field.addConfigurationChangeListener(DateGUIVisibleComponent.this);
				return field;
			}
		}

		private ParamsPanel panel;
		private AnalysisWindowProvider analysisListener;
		//private String[] sourceNames = null;
		
		public Object generateSystemComponent() throws RJException, IOException {
			Map params = panel.getParams();
			//params.put(PROP_SOURCE1, sourceNames[0]);
			//params.put(PROP_SOURCE2, sourceNames[1]);
			return new DateDistance(params, analysisListener);
		}

		public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
			//sourceNames = new String[2];
			//sourceNames[0] = ((DataColumnDefinition)((JList)objects[3]).getModel().getElementAt(0)).getSourceName();
			//sourceNames[1] = ((DataColumnDefinition)((JList)objects[4]).getModel().getElementAt(0)).getSourceName();
			String[] defs = new String[] {Props.getString("date-distance-default-date-format"), Props.getString("date-distance-default-date-format"), "0", "0", "true"};
			if (getRestoredParam(PROP_FORMAT1) != null) {
				defs[0] = getRestoredParam(PROP_FORMAT1);
			}
			if (getRestoredParam(PROP_FORMAT2) != null) {
				defs[1] = getRestoredParam(PROP_FORMAT2);
			}
			if (getRestoredParam(PROP_RANGE1) != null) {
				defs[2] = getRestoredParam(PROP_RANGE1);
			}
			if (getRestoredParam(PROP_RANGE2) != null) {
				defs[3] = getRestoredParam(PROP_RANGE2);
			}
			if (getRestoredParam(PROP_LINERAL) != null) {
				defs[4] = getRestoredParam(PROP_LINERAL);
			}
			
			Map creators = new HashMap();
			creators.put(PROP_RANGE1, new DateRangeParamFieldCreator(DateGUIVisibleComponent.this));
			creators.put(PROP_RANGE2, new DateRangeParamFieldCreator(DateGUIVisibleComponent.this));
			creators.put(PROP_LINERAL, new CheckBoxParamPanelFieldCreator(DateGUIVisibleComponent.this));
			creators.put(PROP_FORMAT1, new Creator((JList)WeightedJoinCondition.getLeftAttributeComponent(objects)));
			creators.put(PROP_FORMAT2, new Creator((JList)WeightedJoinCondition.getRightAttributeComponent(objects)));
			
			Map validators = new HashMap();
			validators.put(PROP_FORMAT1, new Validator() {
				public boolean validate(ParamsPanel paramsPanel, ParamPanelField paramPanelField, String parameterValue) {
					try {
						new SimpleDateFormat(paramPanelField.getValue());
						return true;
					} catch (Exception e) {
						paramPanelField.error("Format string is not correct: " + e.getMessage());
						return false;
					}
				}
				
			});
			
			validators.put(PROP_FORMAT2, new Validator() {
				public boolean validate(ParamsPanel paramsPanel, ParamPanelField paramPanelField, String parameterValue) {
					try {
						new SimpleDateFormat(paramPanelField.getValue());
						return true;
					} catch (Exception e) {
						paramPanelField.error("Format string is not correct: " + e.getMessage());
						return false;
					}
				}
				
			});
			
			panel = new ParamsPanel(new String[] {PROP_FORMAT1, PROP_FORMAT2, PROP_RANGE1, PROP_RANGE2, PROP_LINERAL}, 
					new String[] {"Date/time format (left column)", "Date/time format (right column)", "Range before", "Range after", "Use linear approximation"}, 
					defs, creators);
			panel.setValidators(validators);
			
			return panel;
		}

		public Class getProducedComponentClass() {
			return DateDistance.class;
		}

		public String toString() {
			return "Date distance";
		}

		public boolean validate(JDialog dialog) {
			return panel.doValidate();
		}
		
	}
	
	private String f1;
	private String f2;
	//private String source1;
	//private String source2;
	
	//Make these methods local to each thread!!!
	private ThreadLocal format1 = new ThreadLocal() {
		protected Object initialValue() {
			return new SimpleDateFormat(f1);
		}
	};
	private ThreadLocal format2 = new ThreadLocal() {
		protected Object initialValue() {
			return new SimpleDateFormat(f2);
		}
	};
	
	private double[] difference;
	private boolean lineralOn;
	
	private AnalysisWindowProvider distanceListener = null;
	
	public DateDistance(Map properties) {
		super(properties);
		f1 = getProperty(PROP_FORMAT1);
		f2 = getProperty(PROP_FORMAT2);
		//source1 = getProperty(PROP_SOURCE1);
		//source2 = getProperty(PROP_SOURCE2);
		//format1 = new SimpleDateFormat(f1 = getProperty(PROP_FORMAT1));
		//format2 = new SimpleDateFormat(f2 = getProperty(PROP_FORMAT2));
		lineralOn = Boolean.parseBoolean(getProperty(PROP_LINERAL));
		String r1 = getProperty(PROP_RANGE1);
		String r2 = getProperty(PROP_RANGE2);
		difference = new double[] {Long.parseLong(r1 == null ? "0" : r1), Long.parseLong(r1 == null ? "0" : r2)};
	}

	public DateDistance(Map params, AnalysisWindowProvider analysisListener) {
		this(params);
		this.distanceListener = analysisListener;
	}

	public double distance(DataCell cellA, DataCell cellB) {
		Date d1, d2;
		
		String s1 = cellA.getValue().toString().trim();
		String s2 = cellB.getValue().toString().trim();
		
		if (StringUtils.isNullOrEmpty(s1) || StringUtils.isNullOrEmpty(s2)) {
			return 0;
		}
		
		try {
			d1 = ((SimpleDateFormat)format1.get()).parse(s1);
		} catch (ParseException e) {
			Log.log(getClass(), "Error parsing date string (Used format " + f1 + "): '" + s1 + "'", 1);
			e.printStackTrace();
			if (distanceListener != null) {
				distanceListener.reportError("Some dates have not been parsed correctly. Please see logs.");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			return 0;
		} catch (NumberFormatException e) {
			Log.log(getClass(), "Error parsing date string (Used format " + f1 + "): '" + s1 + "'", 1);
			e.printStackTrace();
			if (distanceListener != null) {
				distanceListener.reportError("Some dates have not been parsed correctly. Please see logs.");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			return 0;
		}
		try {
			d2 = ((SimpleDateFormat)format2.get()).parse(s2);
		} catch (ParseException e) {
			Log.log(getClass(), "Error parsing date string (Used format " + f2 + "): '" + s2 + "'", 1);
			e.printStackTrace();
			if (distanceListener != null) {
				distanceListener.reportError("Some dates have not been parsed correctly. Please see logs.");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			return 0;
		} catch (NumberFormatException e) {
			Log.log(getClass(), "Error parsing date string (Used format " + f1 + "): '" + s1 + "'", 1);
			e.printStackTrace();
			if (distanceListener != null) {
				distanceListener.reportError("Some dates have not been parsed correctly. Please see logs.");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			return 0;
		}
		return getDistanceRange(d1.getTime(), d2.getTime(), difference);
	}
	
	private double getDistanceRange(double val1, double val2, double[] range) {
		double diff = Math.abs(val1 - val2);
		if (diff > range[0] && val1 < val2) {
			return 0;
		} else if (diff > range[1] && val1 > val2) {
			return 0;
		} else if (val1 < val2) {
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
		return new DateGUIVisibleComponent();
	}
	
	public CompareFunctionInterface getCompareFunction() {
		return new DateComparator(f1, f2);
	}
	
	public CompareFunctionInterface getCompareFunction(DataColumnDefinition colA, DataColumnDefinition colB) {
		String form1 = null;
		String form2 = null;
		
		//if (colA.getSourceName().equals(MainApp.main.getConfigured))
		
		if (colA.getSourceName().equals(MainApp.main.getConfiguredSystem().getSourceA().getSourceName())) {
			form1 = f1;
		} else {
			form1 = f2;
		}
		if (colB.getSourceName().equals(MainApp.main.getConfiguredSystem().getSourceB().getSourceName())) {
			form2 = f2;
		} else {
			form2 = f1;
		}
		return new DateComparator(form1, form2);
	}

	public String toString() {
		return "Date distance " + getProperties();
	}
	
//	public static void main(String[] args) throws IOException {
//		long time = System.currentTimeMillis();
//		Random rand = new Random();
//		DateFormat f = new SimpleDateFormat("MM-dd-yyyy");
//		BufferedWriter w = new BufferedWriter(new FileWriter("dates.csv"));
//		w.write("date\n");
//		for (int i = 0; i < 1000; i++) {
//			w.write(f.format(new Date(rand.nextLong() % time)) + "\n");
//		}
//		w.flush();
//		w.close();
//	}

}
