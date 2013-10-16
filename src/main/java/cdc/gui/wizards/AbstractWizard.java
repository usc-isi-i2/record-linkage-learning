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


package cdc.gui.wizards;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.InvocationTargetException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import cdc.gui.Configs;
import cdc.gui.external.JXErrorDialog;

public class AbstractWizard extends JDialog {

	private static final String LABEL_CANCEL = "Cancel";
	private static final String LABEL_BACK = "< Back";
	private static final String LABEL_NEXT = "Next >";
	private static final String LABEL_FINISH = "Finish";
	
	private static Dimension BUTTON_DIMENSION = new Dimension(100, 20);
	
	public static final int RESULT_OK = 1;
	public static final int RESULT_CANCEL = 2;
	
	//private int step = 0;
	
	private int result = RESULT_CANCEL;
	private boolean done = false;
	private boolean disposingAfterFinish = true;
	
	private JPanel mainWizardPanel = new JPanel();
	
	private String[] steps;
	
	private WizardAction[] stepActions;
	private Workflow workflow;
	
	private JButton nextButton;
	private JButton backButton;
	private JButton cancelButton;
	private MinimumSizeListener sizeListener;
	private MouseListener mouseBlocker = new MouseAdapter() {
		public void mouseClicked(MouseEvent ev) {
		      Toolkit.getDefaultToolkit().beep();
		}
	};
	
//	private KeyEventDispatcher keyBlocker = new KeyEventDispatcher() {
//		public boolean dispatchKeyEvent(KeyEvent ev) {
//		      Component source = ev.getComponent();
//		      if (source != null && SwingUtilities.isDescendingFrom(source, getParent())) {
//		        Toolkit.getDefaultToolkit().beep();
//		        ev.consume();
//		        return true;
//		      }
//		      return false;
//
//		    }
//	};
	
	public AbstractWizard(Window mainFrame, WizardAction[] stepActions, String[] labels) {
		this(mainFrame, Configs.DFAULT_WIZARD_SIZE, stepActions, labels);
	}
	
	public AbstractWizard(Window mainFrame, Dimension size, WizardAction[] stepActions, String[] steps) {
		this(mainFrame, size, stepActions, new DefaultWorkflow(stepActions.length), steps);
	}
	
	public AbstractWizard(Window mainFrame, Dimension size, WizardAction[] stepActions, Workflow workflow, String[] steps) {
		super(mainFrame);
		super.setModal(true);
		super.getContentPane().setLayout(new BorderLayout());
		super.setSize(size);
		addComponentListener(sizeListener = new MinimumSizeListener());
		
		this.steps = steps;
		this.stepActions = stepActions;
		this.workflow = workflow;
		
		//prepare glass pane
		((JPanel)AbstractWizard.this.getGlassPane()).setLayout(new BorderLayout());
		JLabel busyLabel = new JLabel(Configs.busyIcon, JLabel.CENTER);
		((JPanel)AbstractWizard.this.getGlassPane()).add(busyLabel, BorderLayout.CENTER);
		((JPanel)AbstractWizard.this.getGlassPane()).setOpaque(true);
		((JPanel)AbstractWizard.this.getGlassPane()).setBackground(new Color(157, 255, 246, 2));
		((JPanel)AbstractWizard.this.getGlassPane()).addMouseListener(mouseBlocker );
		
		mainWizardPanel.setPreferredSize(new Dimension((int)size.getWidth() - 10, (int)size.getHeight() - 10));
		mainWizardPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		mainWizardPanel.setLayout(new BorderLayout());
		getContentPane().add(mainWizardPanel);
		
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		backButton = new JButton(LABEL_BACK);
		nextButton = new JButton(LABEL_NEXT);
		cancelButton = new JButton(LABEL_CANCEL);
		buttons.add(backButton);
		buttons.add(nextButton);
		buttons.add(cancelButton);
		
		super.getContentPane().add(buttons, BorderLayout.SOUTH);
		
		nextButton.setPreferredSize(BUTTON_DIMENSION);
		backButton.setPreferredSize(BUTTON_DIMENSION);
		cancelButton.setPreferredSize(BUTTON_DIMENSION);
		
		backButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				backPressed();
			}
		});
		
		nextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				nextPressed();
			}
		});
		
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancelPressed();
			}
		});
		
		beginStep();
		backButton.setEnabled(false);
		if (steps.length == 1) {
			nextButton.setText(LABEL_FINISH);
		}
		//super.setVisible(true);
//		this.addComponentListener(new ComponentListener() {
//			public void componentHidden(ComponentEvent e) {}
//			public void componentMoved(ComponentEvent e) {}
//			public void componentResized(ComponentEvent e) {
//				sizeChanged(mainWizardPanel.getSize().width, mainWizardPanel.getSize().height);
//			}
//			public void componentShown(ComponentEvent e) {}
//			
//		});
	}

	private void nextPressed() {
		nextButton.setEnabled(false);
		backButton.setEnabled(false);
		cancelButton.setEnabled(false);
		Thread worker = new Thread() {
			private boolean disposed = false;

			public void run() {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
							((JPanel)AbstractWizard.this.getGlassPane()).setVisible(true);
							setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						    //FocusManager.getCurrentManager().addKeyEventDispatcher(keyBlocker);
						}
					});
					
					if (!endStep()) {
						SwingUtilities.invokeAndWait(new Runnable() {
							public void run() {
								nextButton.setEnabled(true);
								backButton.setEnabled(true);
								cancelButton.setEnabled(true);
								((JPanel)AbstractWizard.this.getGlassPane()).setVisible(false);
								setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
							}
						});
						return;
					}
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							if (workflow.isLastStep()) {
								result = RESULT_OK;
								if (disposingAfterFinish) {
									disposed = true;
									dispose();
								} else {
									nextButton.setEnabled(false);
									backButton.setEnabled(false);
								}
								return;
							}
							workflow.nextStep();
							//backButton.setEnabled(true);
							if (workflow.isLastStep()) {
								nextButton.setText(LABEL_FINISH);
							}
						}
					});
					if (!disposed ) {
						beginStep();
						SwingUtilities.invokeAndWait(new Runnable() {
							public void run() {
								nextButton.setEnabled(true);
								backButton.setEnabled(true);
								cancelButton.setEnabled(true);
								((JPanel)AbstractWizard.this.getGlassPane()).setVisible(false);
								setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
							    //FocusManager.getCurrentManager().removeKeyEventDispatcher(keyBlocker);
							}
						});
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
					JXErrorDialog.showDialog(AbstractWizard.this, "Error", e);
				}
			}
		};
		worker.start();
		
	}

	private void backPressed() {
		if (!workflow.isFirstStep()) {
			//if (!endStep()) return;
			workflow.previousStep();
			nextButton.setText(LABEL_NEXT);
			beginStep();
		}
		if (workflow.isFirstStep()) backButton.setEnabled(false);
	}
	
	private void beginStep() {
		JPanel panel = stepActions[workflow.getCurrentStep()].beginStep(this);
		this.mainWizardPanel.removeAll();
		this.mainWizardPanel.add(panel, BorderLayout.CENTER);
		this.setTitle(steps[workflow.getCurrentStep()]);
		
		validate();
		repaint();
	}

	private boolean endStep() {
		return stepActions[workflow.getCurrentStep()].endStep(this);
	}

	private void cancelPressed() {
		result = RESULT_CANCEL;
		dispose();
	}

	public JPanel getMainPanel() {
		return this.mainWizardPanel;
	}

	public int getResult(boolean disposing) {
		this.disposingAfterFinish = disposing;
		if (!done) {
			//pack();
			setVisible(true);
			done = true;
		}
		return result;
	}
	
	public int getResult() {
		return getResult(true);
	}

	public void setMinimum(int x, int y) {
		sizeListener.setMinXY(x, y);
	}

	public void setMinimum(Dimension d) {
		setMinimum((int)d.getWidth(), (int)d.getHeight());
	}
	
	public void dispose() {
//		for (int i = 0; i < stepActions.length; i++) {
//			stepActions[i].dispose();
//		}
		workflow = null;
		stepActions = null;
		nextButton = null;
		backButton = null;
		sizeListener = null;
		mainWizardPanel = null;
		super.dispose();
	}
}
