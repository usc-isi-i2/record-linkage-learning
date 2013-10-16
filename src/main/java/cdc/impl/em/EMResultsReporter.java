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


package cdc.impl.em;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import cdc.utils.LogSink;
import cdc.utils.Props;

public class EMResultsReporter extends JDialog {

	private static final int MAX_LINES = Props.getInteger("em-max-visible-rows");
	
	private class GUILogSink extends LogSink {
		
		private JTextArea log;
		
		public GUILogSink(JTextArea log) {
			this.log = log;
		}
		
		public void log(String msg) {
			synchronized(log) {
				log.append(msg + "\n");
				log.setCaretPosition(log.getText().length());
				if (log.getLineCount() > MAX_LINES) {
					Element el = log.getDocument().getRootElements()[0].getElement(0);
					try {
						log.getDocument().remove(0, el.getEndOffset());
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
			}
		}		
	}
	
	private JTextArea log = new JTextArea();
	private JButton apply = new JButton("Apply weights");
	private JButton cancel = new JButton("Cancel");
	private JProgressBar progress = new JProgressBar(1, 100);
	private GUILogSink sink = new GUILogSink(log);
	private JLabel statusLabel = new JLabel("Please wait...");
	
	public EMResultsReporter(JDialog parent) {
		super(parent);
		setTitle("Progress report for EM method");
		apply.setEnabled(false);
		setSize(550, 300);
		setModal(true);
		progress.setIndeterminate(true);
		
		apply.setPreferredSize(new Dimension(apply.getPreferredSize().width, 20));
		cancel.setPreferredSize(new Dimension(cancel.getPreferredSize().width, 20));
		getContentPane().setLayout(new BorderLayout());
		
		JPanel panelProgress = new JPanel(new BorderLayout());
		panelProgress.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panelProgress.add(statusLabel, BorderLayout.NORTH);
		panelProgress.add(progress, BorderLayout.CENTER);
		getContentPane().add(panelProgress, BorderLayout.NORTH);
		JPanel scrollPanel = new JPanel(new BorderLayout());
		scrollPanel.add(new JLabel("Output of EM method:"), BorderLayout.NORTH);
		scrollPanel.add(new JScrollPane(log), BorderLayout.CENTER);
		scrollPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		getContentPane().add(scrollPanel, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(cancel);
		buttons.add(apply);
		buttons.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		getContentPane().add(buttons, BorderLayout.SOUTH);
	}
	
	public void appendToLog(String line) {
		sink.log(line);
	}
	
	public void finished(boolean ok) {
		if (ok) {
			apply.setEnabled(true);
		}
		progress.setIndeterminate(false);
		progress.setValue(100);
		statusLabel.setText("EM method finished.");
	}
	
	public void addCancelListener(ActionListener listener) {
		cancel.addActionListener(listener);
	}
	
	public void addApplyListener(ActionListener listener) {
		apply.addActionListener(listener);
	}

	public LogSink getLogSink() {
		return sink;
	}
}
