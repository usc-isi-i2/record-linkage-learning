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


/**
 * 
 */
package cdc.gui.components.dynamicanalysis;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.Timer;

import cdc.components.AbstractDataSource;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.converters.AbstractColumnConverter;
import cdc.datamodel.converters.ModelGenerator;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.datasource.JDataSource;

public class ConvAnalysisActionListener implements ActionListener, ChangedConfigurationListener {
	
	private GUIVisibleComponent convCreator;
	private AbstractDataSource source;
	private Window parent;
	private boolean on = false;
	private DynamicAnalysisFrame frame;
	private JButton button;
	private JDataSource dataSource;
	private Timer timer;
	
	public ConvAnalysisActionListener(Window parent, AbstractDataSource source, GUIVisibleComponent convCreator, JDataSource dataSource) {
		this.convCreator = convCreator;
		this.source = source;
		this.parent = parent;
		this.dataSource = dataSource;
		parent.addWindowListener(new WindowListener() {
			public void windowActivated(WindowEvent arg0) {}
			public void windowClosed(WindowEvent arg0) {
				if (on) {
					on = !on;
					button.setEnabled(true);
				}
			}

			public void windowClosing(WindowEvent arg0) {}
			public void windowDeactivated(WindowEvent arg0) {}
			public void windowDeiconified(WindowEvent arg0) {}
			public void windowIconified(WindowEvent arg0) {}
			public void windowOpened(WindowEvent arg0) {}
			
		});
	}
	
	public void actionPerformed(ActionEvent arg0) {
		on = !on;
		
		if (on) {
			button = (JButton)arg0.getSource();
			button.setEnabled(false);
			try {
				AbstractColumnConverter conv = (AbstractColumnConverter) convCreator.generateSystemComponent();
				frame = DynamicAnalysis.getConverterAnalysisFrame(parent, conv);
				if (convCreator.validate((JDialog)parent)) {
					frame.setParameters(getColumns(conv), new Object[] {source, conv, new ModelGenerator(dataSource.getConverters())});
				} else {
					frame.finished(false);
					frame.setWarningMessage("Converter not created.");
				}
				frame.addCloseListener(this);
			} catch (Exception e) {
				e.printStackTrace();
				frame.finished(false);
				frame.setWarningMessage(e.toString());
			}
			//frame.addCloseListener(this);
			frame.setVisible(true);
		} else {
			button.setEnabled(true);
		}
	}
	
	public void configurationChanged() {
		if (!on) return;
		if (timer != null && timer.isRunning()) timer.stop();
		timer = new Timer(700, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (convCreator.validate(null)) {
						AbstractColumnConverter conv = (AbstractColumnConverter) convCreator.generateSystemComponent();
						frame.setParameters(ConvAnalysisActionListener.getColumns(conv), new Object[] {source, conv, new ModelGenerator(dataSource.getConverters())});
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				timer.stop();
			}
		});
		timer.start();
	}
	
//	public void closeWindow() {
//		if (frame != null) {
//			frame.dispose();
//		}
//		if (button != null) {
//			button.setEnabled(true);
//		}
//		on = false;
//	}
	
	public static String[] getColumns(AbstractColumnConverter conv) {
		DataColumnDefinition[] in = conv.getExpectedColumns();
		DataColumnDefinition[] out = conv.getOutputColumns();
		String[] columns = new String[in.length + out.length];
		for (int i = 0; i < in.length; i++) {
			columns[i] = in[i].getColumnName() + " (Input)";
		}
		for (int i = 0; i < out.length; i++) {
			columns[i + in.length] = out[i].getColumnName() + " (Output)";
		}
		return columns;
	}
}
