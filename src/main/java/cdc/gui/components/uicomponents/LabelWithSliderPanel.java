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


package cdc.gui.components.uicomponents;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cdc.gui.components.dynamicanalysis.ChangedConfigurationListener;

public class LabelWithSliderPanel extends JPanel {
	
	private JLabel label;
	private JSlider slider;
	private JTextField value;
	
	private double min = 0;
	private double max = 0;
	
	private ChangedConfigurationListener listener;
	
	public LabelWithSliderPanel(String label, double initVal) {
		this(label, 0, 100, initVal);
	}
	
	public LabelWithSliderPanel(String label, double minVal, double maxVal) {
		this(label, minVal, maxVal, 0);
	}
	
	public LabelWithSliderPanel(String label, int minVal, int maxVal, int initVal) {
		this(label, (double)minVal, maxVal, initVal);
	}
	
	public LabelWithSliderPanel(String label, int initVal) {
		this(label, 0, 100, initVal);
	}
	
	public LabelWithSliderPanel(String label, int minVal, int maxVal) {
		this(label, minVal, maxVal, (double)0);
	}
	
	public LabelWithSliderPanel(String label, double minVal, double maxVal, double initVal) {
		this.label = new JLabel(label, JLabel.LEFT);
		this.slider = new JSlider(0, 100, (int) (initVal * 100));
		this.value = new JTextField(String.valueOf(initVal), 3);
		this.value.setHorizontalAlignment(JTextField.CENTER);
		this.value.setEnabled(false);
		this.label.setMinimumSize(new Dimension(100, this.label.getHeight()));
		this.min = minVal;
		this.max = maxVal;
		//setBackground(Color.RED);
		
		this.slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				value.setText(String.valueOf(decodeSliderValue()));
			}
		});
		this.setLayout(new GridBagLayout());
		this.add(this.label, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		this.add(this.slider, new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 10, 0, 0), 0, 0));
		this.add(this.value, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
	}
	
	public double getValueDouble() {
		return decodeSliderValue();
	}
	
	public int getValueInt() {
		return (int)decodeSliderValue();
	}
	
	public void setValue(double val) {
		slider.setValue((int) (val / (max - min) * 100));
		value.setText(String.valueOf(val));
	}
	
	private double decodeSliderValue() {
		int sliderVal = slider.getValue();
		return min + (max - min) * sliderVal / (double)100;
	}

	public void addSliderListener(ChangedConfigurationListener listener1) {
		this.listener = listener1;
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (listener != null) {
					listener.configurationChanged();
				}
			}
		});
	}
	
}
