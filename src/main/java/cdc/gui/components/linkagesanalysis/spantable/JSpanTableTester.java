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


package cdc.gui.components.linkagesanalysis.spantable;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

public class JSpanTableTester {
	
	
	public static void main(String[] args) {
		SpanTableModel model = new SpanTableModel(new String[] {"Col 1", "Col 2", "Col 3"});
		
		addData(model);
		
		JSpanTable spanTable = new JSpanTable(model);
		
		JScrollPane scroll = new JScrollPane(spanTable);
		JFrame frame = new JFrame();
		frame.setSize(500, 500);
		frame.setLocationRelativeTo(null);
		frame.add(scroll);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

	private static void addData(SpanTableModel model) {
		Object[] r1 = new String[] {"10", "11", "12", "13"};
		Object[] r2 = new String[] {"20", "21", "22", "23"};
		Object[] r3 = new String[] {"30", "31", "32", "33"};
		
		SpanInfo[] span = new SpanInfo[3];
		span[0] = new SpanInfo(0, 0, 1, 3);
		span[1] = new SpanInfo(1, 1, 2, 2);
		span[2] = new SpanInfo(0, 1, 2, 1);
		
		model.addSpannedRows(new Object[][] {r1, r2, r3}, span);
		
	}
	
}
