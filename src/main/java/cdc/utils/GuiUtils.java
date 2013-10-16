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


package cdc.utils;

import java.awt.Point;
import java.awt.Window;
import java.lang.reflect.Method;

import cdc.gui.GUIVisibleComponent;

public class GuiUtils {
	
	public static final String EMPTY = Props.getString("gui-empty-string");
	
	private static GUIVisibleComponent[] getGuiVisibleComponents(GuiComponentsConfigParser.Element element) {
		GUIVisibleComponent[] array = new GUIVisibleComponent[element.children.length];
		try {
			for (int i = 0; i < array.length; i++) {
				Class clazz = Class.forName(element.children[i].value);
				try {
					Method m = clazz.getMethod("getGUIVisibleComponent", new Class[] {});
					array[i] = (GUIVisibleComponent) m.invoke(null, new Object[] {});
				} catch (NoSuchMethodException ex) {
					System.out.println("ERROR: Class " + clazz.getName() + " does not implement mandatory static method: public static GUIVisibleComponent getGUIVisibleComponent()");
					System.exit(0);
				}
			}
		} catch (Exception e) {
			System.out.println("Error reading configuration");
			e.printStackTrace();
		}
		return array;
	}
	
	public static GUIVisibleComponent[] getAvailableSources() {
		return getGuiVisibleComponents(GuiComponentsConfigParser.getElementByName(GuiComponentsConfigParser.sources));
	}

	public static GUIVisibleComponent[] getAvailableJoins() {
		return getGuiVisibleComponents(GuiComponentsConfigParser.getElementByName(GuiComponentsConfigParser.joins));
	}
	
	public static GUIVisibleComponent[] getAvailableSavers() {
		return getGuiVisibleComponents(GuiComponentsConfigParser.getElementByName(GuiComponentsConfigParser.savers));
	}

	public static GUIVisibleComponent[] getAvailableConverters() {
		return getGuiVisibleComponents(GuiComponentsConfigParser.getElementByName(GuiComponentsConfigParser.converters));
	}

	public static GUIVisibleComponent[] getAvailableDistanceMetrics() {
		return getGuiVisibleComponents(GuiComponentsConfigParser.getElementByName(GuiComponentsConfigParser.distances));
	}

	public static GUIVisibleComponent[] getJoinConditions() {
		return getGuiVisibleComponents(GuiComponentsConfigParser.getElementByName(GuiComponentsConfigParser.joinCondition));
	}

	public static Point getCenterLocation(Window parent, Window d) {
		int sizeX = d.getPreferredSize().width / 2;
		int sizeY = d.getPreferredSize().height / 2;
		int parentX = parent.getLocation().x + parent.getSize().width / 2;
		int parentY = parent.getLocation().y + parent.getSize().height / 2;
		return new Point(parentX - sizeX, parentY - sizeY);
	}

}
