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


package cdc.gui.components.dynamicanalysis;

import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.io.IOException;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import cdc.datamodel.converters.AbstractColumnConverter;
import cdc.gui.StoppableThread;
import cdc.utils.RJException;

public class DynamicAnalysis {
	
	public static class Value {
		public double match;
		public String value;
		public String toString() {
			return value;
		}
	}
	
	private static class ColorCellRenderer extends DefaultTableCellRenderer {
		public Component getTableCellRendererComponent(JTable arg0, Object arg1, boolean arg2, boolean arg3, int arg4, int arg5) {
			Value val = (Value)arg1;
			Color c = null;
			//System.out.println("Draw: " + val.match);
			if (val.match == 100) {
				c = Color.green;
				//System.out.println("Color: "  + c);
			} else if (val.match == 0) {
				c = Color.red;
			} else {
				c = new Color(255 - (int)(val.match * 2.55), (int)(val.match * 2.55), 10);
			}
			this.setBackground(c);
			//this.setBorder(val.border);
			setText(val.value);
			return this;
		}
	}
	
	public static class DistValuesCreator implements ValuesCreator {
		public Value[] create(String[] row) {
			double dst = Double.parseDouble(row[2]);
			Value v1 = new Value();
			v1.value = row[0];
			v1.match = dst;
			Value v2 = new Value();
			v2.value = row[1];
			v2.match = dst;
			Value v3 = new Value();
			v3.value = String.valueOf(dst/(double)100);
			v3.match = dst;
			return new Value[] {v1, v2, v3};
		}
		public String[] getColumns() {
			return new String[] {"Left source", "Right source", "Match"};
		}
	}
	
	public static class DistThreadCreator implements ThreadCreator {
		public StoppableThread createThread(DynamicAnalysisFrame frame, Object[] params) throws IOException, RJException {
			return new AnalysisThread(frame, params);
		}
	}
	
	public static DynamicAnalysisFrame getDistanceAnalysisFrame(Window parent) {
		return new DynamicAnalysisFrame(parent, new ColorCellRenderer(), new DistValuesCreator(), new DistThreadCreator());
	}
	
	
	public static class ConvValuesCreator implements ValuesCreator {
		public Value[] create(String[] row) {
			Value[] vals = new Value[row.length];
			for (int i = 0; i < vals.length; i++) {
				vals[i] = new Value();
				vals[i].value = row[i];
			}
			return vals;
		}
	}
	
	public static class ConvsThreadCreator implements ThreadCreator {
		public StoppableThread createThread(DynamicAnalysisFrame frame, Object[] params) {
			return new ConvsThread(frame, params);
		}
	}
	
	public static DynamicAnalysisFrame getConverterAnalysisFrame(Window parent, AbstractColumnConverter conv) {
		return new DynamicAnalysisFrame(parent, new DefaultTableCellRenderer(), new ConvValuesCreator(), new ConvsThreadCreator());
	}
}
