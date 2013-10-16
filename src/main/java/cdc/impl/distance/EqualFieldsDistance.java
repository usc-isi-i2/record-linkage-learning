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
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JPanel;

import cdc.components.AbstractDistance;
import cdc.datamodel.DataCell;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.paramspanel.ParamsPanel;
import cdc.utils.Log;
import cdc.utils.RJException;

public class EqualFieldsDistance extends AbstractDistance {

	private static final int logLevel = Log.getLogLevel(EqualFieldsDistance.class);
	
	private static class EgualDistanceVisibleComponent extends GUIVisibleComponent {

		public Object generateSystemComponent() throws RJException, IOException {
			return new EqualFieldsDistance();
		}

		public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
			return new ParamsPanel();
		}

		public Class getProducedComponentClass() {
			return EqualFieldsDistance.class;
		}

		public String toString() {
			return "Equal fields boolean distance";
		}

		public boolean validate(JDialog dialog) {
			return true;
		}
	}
	
	public EqualFieldsDistance() {
		super(new HashMap());
	}
	
	public EqualFieldsDistance(Map props) {
		super(props);
	}
	
	public boolean distanceSatisfied(DataCell cell1, DataCell cell2) {
		if (logLevel >= 2) {
			Log.log(EqualFieldsDistance.class, cell1 + "=?=" + cell2 + ": " + cell1.equals(cell2), 2);
		}
		return cell1.equals(cell2);
	}

	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new EgualDistanceVisibleComponent();
	}
	
	public String toString() {
		return "Equal fields boolean distance";
	}

	public double distance(DataCell cellA, DataCell cellB) {
		if (distanceSatisfied(cellA, cellB)) {
			return 100;
		}
		return 0;
	}
}
