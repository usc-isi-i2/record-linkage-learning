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


package cdc.impl.conditions;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.components.AbstractJoinCondition;
import cdc.components.EvaluatedCondition;
import cdc.configuration.Configuration;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.gui.Configs;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.OptionDialog;
import cdc.gui.components.table.TablePanel;
import cdc.gui.components.uicomponents.ManualReviewConfigDialog;
import cdc.gui.wizards.AbstractWizard;
import cdc.impl.em.EMWizard;
import cdc.utils.RJException;
import cdc.utils.StringUtils;
import edu.emory.mathcs.util.xml.DOMUtils;

public class WeightedJoinCondition extends AbstractJoinCondition {

	public static class ListListener implements ListSelectionListener {

		private PropertyChangeListener listener;
		
		public ListListener(PropertyChangeListener propertyListener) {
			listener = propertyListener;
		}

		public void valueChanged(ListSelectionEvent arg0) {
			listener.propertyChange(new PropertyChangeEvent(arg0.getSource(), "list-selection", null, null));
		}

	}
	
	public static class DocumentChangedAction implements DocumentListener {
		private PropertyChangeListener listener;
		private JTextField list;
		public DocumentChangedAction(JTextField weight, PropertyChangeListener listener) {
				this.listener = listener;
				this.list = weight;
		}
		public void changedUpdate(DocumentEvent arg0) {
			listener.propertyChange(new PropertyChangeEvent(list, "text", null, null));
		}
		public void insertUpdate(DocumentEvent arg0) {
			listener.propertyChange(new PropertyChangeEvent(list, "text", null, null));
		}
		public void removeUpdate(DocumentEvent arg0) {
			listener.propertyChange(new PropertyChangeEvent(list, "text", null, null));
		}
	}

	public static class WeightedVisibleComponent extends GUIVisibleComponent {

		
		private static final String[] cols = {"Comparison method", "Left column", "Right column", "Weight", "Empty value score"};
		private static final int HEIGHT_PANEL_BELOW = 50;
		
		private int sumWeights = 0;
		private JLabel sumLabel = new JLabel();
		private JTextField acceptLevel = new JTextField(String.valueOf(100));
		private JLabel manualReviewBulb = new JLabel(Configs.bulbOff);
		private int manualReview = -1;
		
		private AbstractDataSource sourceA;
		private AbstractDataSource sourceB;
		
		private JPanel buffer;
		private TablePanel tablePanel;
		private Window parent;
		private WeightedJoinCondition oldCondition;
		private DataColumnDefinition[] leftColumns;
		private DataColumnDefinition[] rightColumns;
		
		public WeightedVisibleComponent() {
			acceptLevel.setPreferredSize(new Dimension(40, 20));
			acceptLevel.setHorizontalAlignment(JTextField.CENTER);
			sumLabel.setPreferredSize(new Dimension(40, 20));
			sumLabel.setHorizontalAlignment(JLabel.CENTER);
			tablePanel = new TablePanel(cols);
			tablePanel.multiselectionAllowed(false);
			tablePanel.addAddButtonListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					OptionDialog dialog;
					if (parent instanceof JFrame) {
						dialog = new OptionDialog((JFrame)parent, "Choose columns");
					} else {
						dialog = new OptionDialog((JDialog)parent, "Choose columns");
					}
					NewWeightedConditionPanel panel = new NewWeightedConditionPanel(leftColumns, rightColumns, dialog);
					dialog.setMainPanel(panel);
					dialog.setLocationRelativeTo((JButton)e.getSource());
					dialog.addOptionDialogListener(panel);
					if (dialog.getResult() == OptionDialog.RESULT_OK) {
						ConditionItem item = panel.getConditionItem();
						tablePanel.addRow(new Object[] {item.getDistanceFunction(), item.getLeft(), 
								item.getRight(), String.valueOf(item.getWeight()), String.valueOf(item.getEmptyMatchScore())});
						sumWeights += item.getWeight();
						sumLabel.setText(String.valueOf(sumWeights));
					}
				}
			});
			tablePanel.addEditButtonListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Object[] selectedRow = (Object[]) (tablePanel.getSelectedRows()[0]);
					OptionDialog dialog;
					if (parent instanceof JFrame) {
						dialog = new OptionDialog((JFrame)parent, "New condition");
					} else {
						dialog = new OptionDialog((JDialog)parent, "New condition");
					}
					NewWeightedConditionPanel panel = new NewWeightedConditionPanel(leftColumns, rightColumns, dialog);
					panel.restoreValues((AbstractDistance)selectedRow[0], 
							(DataColumnDefinition)selectedRow[1], 
							(DataColumnDefinition)selectedRow[2], 
							Integer.parseInt((String)selectedRow[3]),
							Double.parseDouble((String)selectedRow[4]));
					dialog.setMainPanel(panel);
					dialog.setLocationRelativeTo((JButton)e.getSource());
					dialog.addOptionDialogListener(panel);
					if (dialog.getResult() == OptionDialog.RESULT_OK) {
						ConditionItem item = panel.getConditionItem();
						sumWeights += item.getWeight() - Integer.parseInt((String)selectedRow[3]);
						sumLabel.setText(String.valueOf(sumWeights));
						tablePanel.replaceRow(tablePanel.getSelectedRowId()[0], new Object[] {item.getDistanceFunction(), item.getLeft(), 
							item.getRight(), String.valueOf(item.getWeight()), String.valueOf(item.getEmptyMatchScore())});
					}
				}
			});
			tablePanel.addRemoveButtonListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						Object[] selected = tablePanel.getSelectedRows();
						int[] ids = tablePanel.getSelectedRowId();
						tablePanel.clearSelection();
						for (int i = 0; i < selected.length; i++) {
							sumWeights -= Integer.parseInt((String)((Object[])selected[i])[3]);
							tablePanel.removeRow(ids[i]);
						}
						((JButton)e.getSource()).setEnabled(false);
						sumLabel.setText(String.valueOf(sumWeights));
					}
				});
		}
		
		public Object generateSystemComponent() throws RJException, IOException {
			return getJoinCondition();
		}

		private AbstractJoinCondition getJoinCondition() {
			Object[] data = tablePanel.getRows();
			DataColumnDefinition[] colsLeft = new DataColumnDefinition[data.length];
			DataColumnDefinition[] colsRight = new DataColumnDefinition[data.length];
			AbstractDistance[] distances = new AbstractDistance[data.length];
			double[] weights = new double[data.length];
			double[] emptyValues = new double[data.length];
			
			for (int i = 0; i < distances.length; i++) {
				Object[] row = (Object[]) data[i];
				distances[i] = (AbstractDistance)row[0];
				colsLeft[i] = (DataColumnDefinition)row[1];
				colsRight[i] = (DataColumnDefinition)row[2];
				weights[i] = Integer.parseInt((String)row[3]) / (double)100;
				emptyValues[i] = Double.parseDouble((String)row[4]);
			}
			Map props = new HashMap();
			props.put(PROP_ACCEPTANCE_LEVEL, acceptLevel.getText());
			if (manualReview != -1) {
				props.put(PROP_MANUAL_REVIEW, String.valueOf(manualReview));
			}
			WeightedJoinCondition cond = new WeightedJoinCondition(colsLeft, colsRight, distances, weights, emptyValues, props);
			cond.creator = this;
			return cond;
		}
		
		public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
			
			sourceA = (AbstractDataSource) objects[0];
			sourceB  = (AbstractDataSource) objects[1];
			parent = (Window) objects[2];
			oldCondition = (objects[3] instanceof WeightedJoinCondition) ? (WeightedJoinCondition)objects[3] : null;
			leftColumns = sourceA.getDataModel().getSortedOutputColumns();
			rightColumns = sourceB.getDataModel().getSortedOutputColumns();
			
			JPanel weightsSumPanel = new JPanel(new GridBagLayout());
			
			JLabel label = new JLabel("Current sum of weights: ");
			weightsSumPanel.add(label, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
			weightsSumPanel.add(sumLabel, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,10,0,0), 0, 0));
			
			label = new JLabel("Acceptance level: ");
			weightsSumPanel.add(label, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
			weightsSumPanel.add(acceptLevel, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,10,0,0), 0, 0));
			
			sumLabel.addPropertyChangeListener("text", new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					int sum = Integer.parseInt((String)evt.getNewValue());
					if (sum != 100) {
						((JLabel)evt.getSource()).setForeground(Color.RED);
					} else {
						((JLabel)evt.getSource()).setForeground(Color.BLACK);
					}
				}
			});
			sumLabel.setText(String.valueOf(sumWeights));
			JButton emButton = new JButton("Run EM method");
			emButton.setToolTipText("Run Expectation-Maximization method to suggest weights");
			emButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					int[] weights = new EMWizard().emWizard((AbstractWizard)parent, sourceA, sourceB, getJoinCondition());
					Object[] rows = tablePanel.getRows();
					if (rows != null && weights != null) {
						int sum = 0;
						for (int i = 0; i < rows.length; i++) {
							Object[] row = (Object[])rows[i];
							row[3] = String.valueOf(weights[i]);
							tablePanel.replaceRow(i, row);
							sum += weights[i];
						}
						sumLabel.setText(String.valueOf(sum));
						sumWeights = sum;
					}
				}
			});
			emButton.setPreferredSize(new Dimension(emButton.getPreferredSize().width, 20));
			weightsSumPanel.add(emButton, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,10,0,25), 0, 0));
			
			JButton reviewButton = new JButton("Manual review");
			reviewButton.setToolTipText("Configure manual review process");
			reviewButton.setPreferredSize(new Dimension(emButton.getPreferredSize().width, 20));
			reviewButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ManualReviewConfigDialog dialog = new ManualReviewConfigDialog(parent, Integer.parseInt(acceptLevel.getText()), manualReview);
					if (dialog.getResult() == OptionDialog.RESULT_OK) {
						acceptLevel.setText(String.valueOf(dialog.getAcceptanceLevel()));
						manualReview = dialog.getManualReviewLevel();
						acceptLevel.setEnabled(manualReview == -1);
						manualReviewBulb.setIcon(manualReview != -1 ? Configs.bulbOn : Configs.bulbOff);
					}
				}
			});
			weightsSumPanel.add(reviewButton, new GridBagConstraints(2, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,10,0,25), 0, 0));
			
			JPanel reviewBulbPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			reviewBulbPanel.add(manualReviewBulb);
			reviewBulbPanel.add(new JLabel("Manual review"));
			weightsSumPanel.add(reviewBulbPanel, new GridBagConstraints(1, 2, 2, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,25), 0, 0));
			
			buffer = new JPanel(new GridBagLayout());			
			buffer.add(tablePanel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
			buffer.add(weightsSumPanel, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,10,0,0), 0, 0));
			
			sumWeights = 0;
			tablePanel.removeAllRows();
			if (oldCondition != null) {
				restoreCondition(oldCondition);
			}
			
			return buffer;
		}

		private void restoreCondition(WeightedJoinCondition oldCondition) {
			if (failToVerify(sourceA, oldCondition.getLeftJoinColumns()) || failToVerify(sourceB, oldCondition.getRightJoinColumns())) {
				return;
			}
			AbstractDistance[] dists = oldCondition.distances;
			DataColumnDefinition[] leftCols = oldCondition.getLeftJoinColumns();
			DataColumnDefinition[] rightCols = oldCondition.getRightJoinColumns();
			double[] weights = oldCondition.weights;
			double[] emptyValues = oldCondition.getEmptyMatchScore();
			for (int i = 0; i < rightCols.length; i++) {
				tablePanel.addRow(new Object[] {dists[i], leftCols[i], rightCols[i], String.valueOf((int)(weights[i]*100)), String.valueOf(emptyValues[i])});
				sumWeights += (int)(weights[i]*100);
			}
			acceptLevel.setText(String.valueOf(oldCondition.acceptanceThreshold));
			sumLabel.setText(String.valueOf(sumWeights));
			if (oldCondition.getProperty(PROP_MANUAL_REVIEW) != null) {
				acceptLevel.setEnabled(false);
				manualReview = Integer.parseInt(oldCondition.getProperty(PROP_MANUAL_REVIEW));
				manualReviewBulb.setIcon(Configs.bulbOn);
			}
		}

		private boolean failToVerify(AbstractDataSource source, DataColumnDefinition[] columns) {
			DataColumnDefinition[] sourceCols = source.getDataModel().getSortedOutputColumns();
			labelA: for (int i = 0; i < columns.length; i++) {
				for (int j = 0; j < sourceCols.length; j++) {
					if (columns[i].equals(sourceCols[j])) {
						continue labelA;
					}
				}
				return true;
			}
			return false;
		}

		public Class getProducedComponentClass() {
			return WeightedJoinCondition.class;
		}

		public String toString() {
			return "Weighted join condition";
		}

		public boolean validate(JDialog dialog) {
			try {
				Integer.parseInt(acceptLevel.getText());
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(dialog, "Acceptance level should be an integer value.");
				return false;
			}
			if (tablePanel.getRows().length == 0) {
				JOptionPane.showMessageDialog(dialog, "At least one condition is required.");
				return false;
			}
			if (Integer.parseInt(sumLabel.getText()) != 100) {
				JOptionPane.showMessageDialog(dialog, "Sum of weights have to be 100.");
				return false;
			}
			if (Integer.parseInt(acceptLevel.getText()) > 100) {
				JOptionPane.showMessageDialog(dialog, "Acceptance level cannot exceed 100.");
				return false;
			}
			return true;
		}
		
		public void restoreWeights(int[] weights) {
			for (int i = 0; i < weights.length; i++) {
				tablePanel.setValueAt(i, 3, weights[i] + "");
			}
		}
		
		public void setSize(int x, int y) {
			tablePanel.setPreferredSize(new Dimension(x, y - HEIGHT_PANEL_BELOW));
			buffer.setPreferredSize(new Dimension(x, y));
			buffer.setSize(x, y);
			parent.validate();
			parent.repaint();
		}

		public void windowClosing(JDialog parent) {
			// TODO Auto-generated method stub
			
		}

	}
	
	
	public static String PROP_ACCEPTANCE_LEVEL = "acceptance-level";	
	private static final String CONDITION_TAG = "condition";
	private static final String LEFT_ROW_TAG = "left-column";
	private static final String RIGHT_ROW_TAG = "right-column";
	private static final String WEIGHT_TAG = "weight";
	private static final String EMPTY_MATCH_SCORE_TAG = "empty-match-score";
	private static final String PROP_MANUAL_REVIEW = "manual-review-level";
	
	private DataColumnDefinition[] leftJoinColumns = new DataColumnDefinition[0];
	private DataColumnDefinition[] rightJoinColumns = new DataColumnDefinition[0];
	private AbstractDistance[] distances = new AbstractDistance[0];
	private double[] weights = new double[0];
	private double[] emptyMatchScore = new double[0];
	private int acceptanceThreshold = 100;
	private int manualReviewThreshold = -1;
	private WeightedVisibleComponent creator;
	
	public WeightedJoinCondition(DataColumnDefinition[] leftJoinColumns, DataColumnDefinition[] rightJoinColumns,
			AbstractDistance[] distances, double[] weights, double[] emptyValues, Map properties) {
		super(properties);
		this.leftJoinColumns = leftJoinColumns;
		this.rightJoinColumns = rightJoinColumns;
		this.distances = distances;
		this.weights = weights;
		this.emptyMatchScore = emptyValues;
		acceptanceThreshold = Integer.parseInt(getProperty(PROP_ACCEPTANCE_LEVEL));
		if (getProperty(PROP_MANUAL_REVIEW) != null) {
			manualReviewThreshold = Integer.parseInt(getProperty(PROP_MANUAL_REVIEW));
		}
	}

	public WeightedJoinCondition(Map properties) {
		super(properties);
		acceptanceThreshold = Integer.parseInt(getProperty(PROP_ACCEPTANCE_LEVEL));
		if (getProperty(PROP_MANUAL_REVIEW) != null) {
			manualReviewThreshold = Integer.parseInt(getProperty(PROP_MANUAL_REVIEW));
		}
	}
	
	public EvaluatedCondition conditionSatisfied(DataRow rowA, DataRow rowB) {
		double value = 0.0;
		double weightsToGo = 1.0;
		DataColumnDefinition[] rowACols = getRowAcols(rowA);
		DataColumnDefinition[] rowBCols = getRowAcols(rowB);
		for (int i = 0; i < rowACols.length; i++) {
			DataCell cellA = rowA.getData(rowACols[i]);
			DataCell cellB = rowB.getData(rowBCols[i]);
			if (emptyMatchScore[i] != 0 && (cellA.isEmpty(rowACols[i]) || cellB.isEmpty(rowBCols[i]))) {
				value += emptyMatchScore[i] * 100 * weights[i];
			} else {
				value += distances[i].distance(cellA, cellB) * weights[i];
			}
			weightsToGo -= weights[i];
			if (this.isCanUseOptimisticEval() && value + weightsToGo*100 < acceptanceThreshold) {
				return new EvaluatedCondition(false, false, (int)value);
			}
		}
		return new EvaluatedCondition(value >= acceptanceThreshold, manualReviewThreshold != -1 ? value <= manualReviewThreshold : false, (int)value);
	}
	
	private DataColumnDefinition[] getRowAcols(DataRow rowA) {
		if (leftJoinColumns[0].getSourceName().equals(rowA.getSourceName())) {
			return leftJoinColumns;
		} else {
			return rightJoinColumns;
		}
	}

	public void addCondition(DataColumnDefinition left, DataColumnDefinition right, AbstractDistance distance, int weight, double emptyMatchScore) {
		DataColumnDefinition[] leftJoinColumns = new DataColumnDefinition[this.leftJoinColumns.length + 1];
		System.arraycopy(this.leftJoinColumns, 0, leftJoinColumns, 0, this.leftJoinColumns.length);
		leftJoinColumns[leftJoinColumns.length - 1] = left;
		this.leftJoinColumns = leftJoinColumns;
		
		DataColumnDefinition[] rightJoinColumns = new DataColumnDefinition[this.rightJoinColumns.length + 1];
		System.arraycopy(this.rightJoinColumns, 0, rightJoinColumns, 0, this.rightJoinColumns.length);
		rightJoinColumns[rightJoinColumns.length - 1] = right;
		this.rightJoinColumns = rightJoinColumns;
		
		AbstractDistance[] distances = new AbstractDistance[this.distances.length + 1];
		System.arraycopy(this.distances, 0, distances, 0, this.distances.length);
		distances[distances.length - 1] = distance;
		this.distances = distances;
		
		double[] weights = new double[this.weights.length + 1];
		System.arraycopy(this.weights, 0, weights, 0, this.weights.length);
		weights[distances.length - 1] = weight / (double)100;
		this.weights = weights;
		
		double[] empty = new double[this.emptyMatchScore.length + 1];
		System.arraycopy(this.emptyMatchScore, 0, empty, 0, this.emptyMatchScore.length);
		empty[distances.length - 1] = emptyMatchScore;
		this.emptyMatchScore = empty;
	}

	public DataColumnDefinition[] getLeftJoinColumns() {
		return leftJoinColumns;
	}

	public DataColumnDefinition[] getRightJoinColumns() {
		return rightJoinColumns;
	}

	public void saveToXML(Document doc, Element node) {
		Configuration.appendParams(doc, node, getProperties());
		for (int i = 0; i < distances.length; i++) {
			Element condition = DOMUtils.createChildElement(doc, node, CONDITION_TAG);
			DOMUtils.setAttribute(condition, Configuration.CLASS_ATTR, distances[i].getClass().getName());
			DOMUtils.setAttribute(condition, LEFT_ROW_TAG, leftJoinColumns[i].getColumnName());
			DOMUtils.setAttribute(condition, RIGHT_ROW_TAG, rightJoinColumns[i].getColumnName());
			DOMUtils.setAttribute(condition, WEIGHT_TAG, String.valueOf((int)(weights[i] * 100)));
			if (emptyMatchScore[i] != 0) {
				DOMUtils.setAttribute(condition, EMPTY_MATCH_SCORE_TAG, String.valueOf(emptyMatchScore[i]));
			}
			Configuration.appendParams(doc, condition, distances[i].getProperties());
		}
	}

	
	public static AbstractJoinCondition fromXML(AbstractDataSource leftSource, AbstractDataSource rightSource, Element node) throws RJException {
		Element[] conds = DOMUtils.getChildElements(node);
		Element paramsNode = DOMUtils.getChildElement(node, Configuration.PARAMS_TAG);
		Map properties = null;
		if (paramsNode != null) {
			properties =  Configuration.parseParams(paramsNode);
		}
		WeightedJoinCondition condition = new WeightedJoinCondition(properties);
		for (int i = 0; i < conds.length; i++) {
			if (conds[i].getNodeName().equals(CONDITION_TAG)) {
				String className = conds[i].getAttribute(Configuration.CLASS_ATTR);
				if (Configuration.isNullOrEmpty(className)) {
					throw new RJException("Each " + CONDITION_TAG + " tag has to provide attribute " + Configuration.CLASS_ATTR);
				}
				Map props = null;
				Element paramsElement = DOMUtils.getChildElement(conds[i], Configuration.PARAMS_TAG);
				if (paramsElement != null) {
					props = Configuration.parseParams(paramsElement);
				}
				try {
					Class clazz = Class.forName(className);
					AbstractDistance distance = (AbstractDistance) clazz.getConstructor(new Class[] {Map.class}).newInstance(new Object[] {props});
					
					String leftColumnName = DOMUtils.getAttribute(conds[i], LEFT_ROW_TAG);
					String rightColumnName = DOMUtils.getAttribute(conds[i], RIGHT_ROW_TAG);
					String weightString = DOMUtils.getAttribute(conds[i], WEIGHT_TAG);
					String emptyScore = DOMUtils.getAttribute(conds[i], EMPTY_MATCH_SCORE_TAG, "0");
					
					DataColumnDefinition leftColumn = findByName(leftSource.getDataModel().getOutputFormat(), leftColumnName);
					DataColumnDefinition rightColumn = findByName(rightSource.getDataModel().getOutputFormat(), rightColumnName);
					
					if (leftColumn == null) {
						throw new RJException("Column " + leftColumnName + " was not defined in data source " + leftSource.getSourceName());
					}
					if (rightColumn == null) {
						throw new RJException("Column " + rightColumnName + " was not defined in data source " + rightSource.getSourceName());
					}
					if (StringUtils.isNullOrEmpty(weightString)) {
						throw new RJException("Each condition in weighted conditions have to have provide weight attribute");
					}
					
					condition.addCondition(leftColumn, rightColumn, distance, Integer.parseInt(weightString), Double.parseDouble(emptyScore));
					
				} catch (ClassNotFoundException e) {
					throw new RJException("Class configured in tag " + CONDITION_TAG + "(" + className
							+ ") not found. Make sure the name and classpath are correct", e);
				} catch (SecurityException e) {
					throw new RJException("Class " + className + ", error when attempting to instantiate!", e);
				} catch (NoSuchMethodException e) {
					throw new RJException("Class " + className + ", error when attempting to instantiate", e);
				} catch (IllegalArgumentException e) {
					throw new RJException("Class " + className + ", error when attempting to instantiate", e);
				} catch (IllegalAccessException e) {
					throw new RJException("Class " + className + ", error when attempting to instantiate", e);
				} catch (InvocationTargetException e) {
					throw new RJException("Class " + className + ", error when attempting to instantiate", e.getCause());
				} catch (InstantiationException e) {
					throw new RJException("Class " + className + ", error when attempting to instantiate", e.getCause());
				}
			}
		}
		return condition;
	}
	
	private static DataColumnDefinition findByName(DataColumnDefinition[] dataModel, String name) {
		for (int i = 0; i < dataModel.length; i++) {
			if (dataModel[i].getColumnName().equals(name)) {
				return dataModel[i];
			}
		}
		return null;
	}

	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new WeightedVisibleComponent();
	}

	public void setWeights(int[] weights) {
		for (int i = 0; i < weights.length; i++) {
			this.weights[i] = weights[i];
		}
		if (creator != null) {
			creator.restoreWeights(weights);
		}
	}

	public AbstractDistance[] getDistanceFunctions() {
		return this.distances;
	}

	public double[] getWeights() {
		return weights;
	}
	
	public double[] getEmptyMatchScore() {
		return emptyMatchScore;
	}
	
	public Object clone() {
		return new WeightedJoinCondition(getLeftJoinColumns(), getRightJoinColumns(), getDistanceFunctions(), getWeights(), getEmptyMatchScore(), getProperties());
	}

	public static JComponent getLeftAttributeComponent(Object[] objects) {
		return (JComponent) objects[3];
	}
	
	public static JComponent getRightAttributeComponent(Object[] objects) {
		return (JComponent) objects[4];
	}
	
}
