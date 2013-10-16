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


package cdc.datamodel.converters.ui;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.codehaus.janino.ScriptEvaluator;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

public class ScriptPanel extends JPanel {

	private RSyntaxTextArea scriptTextArea;
	private RTextScrollPane scriptScrollPane;
	
	private Class output;
	private Class[] paramTypes;
	private String[] paramNames;
	
	public ScriptPanel(String script, Class output, String[] attributes, Class[] attrTypes) {
		this.output = output;
		this.paramNames = attributes;
		this.paramTypes = attrTypes;
		
		setLayout(new GridBagLayout());
		
		scriptTextArea = new RSyntaxTextArea();
		scriptTextArea.restoreDefaultSyntaxHighlightingColorScheme();
		scriptTextArea.setSyntaxEditingStyle(SyntaxConstants.JAVA_SYNTAX_STYLE);
		scriptTextArea.setText(script);
		scriptScrollPane = new RTextScrollPane(0, 0, scriptTextArea, true);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		add(scriptScrollPane, c);
		
		JButton button = new JButton("Validate");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				validateScript();
			}
		});
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.add(button);
		c = new GridBagConstraints();
		c.weightx = 0;
		c.weighty = 0;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 1;
		add(buttonPanel, c);
	}
	
	public String getScript() {
		return scriptTextArea.getText();
	}
	
	public void validateScript() {
		try {
			new ScriptEvaluator(getScript(), output, paramNames, paramTypes);
			JOptionPane.showMessageDialog(this, "Script syntax is correct.");
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage());
		}
	}

	public void setScript(String script) {
		scriptTextArea.setText(script);
	}
	
}
