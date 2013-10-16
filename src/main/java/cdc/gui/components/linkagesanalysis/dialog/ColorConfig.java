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


package cdc.gui.components.linkagesanalysis.dialog;

import java.awt.Color;

public class ColorConfig {
	
	private static final Color ODD_COLOR = new Color(240, 255, 240);
	private static final Color EVEN_COLOR = new Color(255, 255, 240);
	private static final Color DIFF_COLOR = new Color(255, 220, 220);
	private static final Color EDITOR_COLOR = new Color(187, 207, 255);
	private static final Color MOUSE_OVER_COLOR = new Color(199, 207, 255);
	
	private Color oddRowColor;
	private Color evenRowColor;
	private Color diffColor;
	private Color editorColor;
	private Color mouseOverColor;
	
	public ColorConfig(Color oddColor, Color evenColor, Color diffColor2, Color editorColor, Color mouse_over_color2) {
		this.oddRowColor = oddColor;
		this.evenRowColor = evenColor;
		this.diffColor = diffColor2;
		this.editorColor = editorColor;
		this.mouseOverColor = mouse_over_color2;
	}

	public static ColorConfig getDefault() {
		return new ColorConfig(ODD_COLOR, EVEN_COLOR, DIFF_COLOR, EDITOR_COLOR, MOUSE_OVER_COLOR);	
	}
	
	public Color getOddRowColor() {
		return oddRowColor;
	}
	
	public void setOddRowColor(Color oddRowColor) {
		this.oddRowColor = oddRowColor;
	}
	
	public Color getEvenRowColor() {
		return evenRowColor;
	}
	
	public void setEvenRowColor(Color evenRowColor) {
		this.evenRowColor = evenRowColor;
	}
	
	public Color getDiffColor() {
		return diffColor;
	}
	
	public void setDiffColor(Color diffColor) {
		this.diffColor = diffColor;
	}
	
	public Color getEditorColor() {
		return editorColor;
	}
	
	public void setEditorColor(Color editorColor) {
		this.editorColor = editorColor;
	}
	
	public Color getMouseOverColor() {
		return mouseOverColor;
	}
	
	public void setMouseOverColor(Color mouseOverColor) {
		this.mouseOverColor = mouseOverColor;
	}
}
