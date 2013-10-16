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


package cdc.gui;

import java.awt.Dimension;
import java.awt.Image;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;

public class Configs {
	public static final Dimension DFAULT_WIZARD_SIZE = new Dimension(680, 490);
	
	public static final ImageIcon errorInfoIcon = new ImageIcon("icons" + File.separator + "info.png");
	public static final ImageIcon busyIcon = new ImageIcon("icons" + File.separator + "remembermilk_orange.gif");
	public static final ImageIcon playIcon = new ImageIcon("icons" + File.separator + "play1.png");
	public static final ImageIcon checkIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "finished.png").getImage().getScaledInstance(26, 26, Image.SCALE_SMOOTH));
	public static final ImageIcon warnIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "warningnew.png").getImage().getScaledInstance(26, 26, Image.SCALE_SMOOTH));;
	public static final ImageIcon addIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "add.png").getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
	public static final ImageIcon closeIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "close-transparent.png").getImage().getScaledInstance(10, 10, Image.SCALE_SMOOTH));
	public static final ImageIcon openDetailsIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "expand.png").getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
	public static final ImageIcon closeDetailsIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "collapse.png").getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
	
	public static final ImageIcon buttonVistaBlue = new ImageIcon(new ImageIcon("icons" + File.separator + "button-vista-blue.png").getImage().getScaledInstance(120, 20, Image.SCALE_SMOOTH));
	public static final ImageIcon buttonVistaRed = new ImageIcon(new ImageIcon("icons" + File.separator + "button-vista-redish.png").getImage().getScaledInstance(120, 20, Image.SCALE_SMOOTH));
	public static final ImageIcon buttonVistaWhite = new ImageIcon(new ImageIcon("icons" + File.separator + "button-vista-white.png").getImage().getScaledInstance(120, 20, Image.SCALE_SMOOTH));

	public static final ImageIcon background = new ImageIcon("icons" + File.separator + "background.png");
	
	public static final Image appIcon = new ImageIcon("icons" + File.separator + "Icon.gif").getImage();

	public static final ImageIcon upArrow = new ImageIcon(new ImageIcon("icons" + File.separator + "Arrow-Up.png").getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));
	public static final ImageIcon downArrow = new ImageIcon(new ImageIcon("icons" + File.separator + "Arrow-Down.png").getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));

	public static final ImageIcon systemComponentNotConfigured = new ImageIcon(new ImageIcon("icons" + File.separator + "system-comp-err.png").getImage().getScaledInstance(150, 100, Image.SCALE_SMOOTH));
	public static final ImageIcon systemComponentConfigured = new ImageIcon(new ImageIcon("icons" + File.separator + "system-comp.png").getImage().getScaledInstance(150, 100, Image.SCALE_SMOOTH));
	public static final ImageIcon systemControlPanel = new ImageIcon(new ImageIcon("icons" + File.separator + "system-config.png").getImage().getScaledInstance(230, 100, Image.SCALE_SMOOTH));

	public static final ImageIcon barBackground = new ImageIcon("icons" + File.separator + "bar.png");
	public static final ImageIcon statistics = new ImageIcon(new ImageIcon("icons" + File.separator + "statistics.png").getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH));
	
	public static final ImageIcon analysisButtonIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "analyze.png").getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH));
	public static final ImageIcon analysisMinusButtonIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "analyze-minus.png").getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH));
	public static final ImageIcon detailsButtonIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "details.png").getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH));
	public static final ImageIcon configurationButtonIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "wheels.png").getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH));
	public static final ImageIcon filterButtonIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "filter.png").getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH));
	public static final ImageIcon sortButtonIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "sort.png").getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH));
	
	public static final ImageIcon addButtonIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "add-new.png").getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
	public static final ImageIcon removeButtonIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "minus-new.png").getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
	public static final ImageIcon editButtonIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "edit-new.png").getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));

	public static final ImageIcon forwardButtonIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "forward.png").getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));
	public static final ImageIcon backwardButtonIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "backward.png").getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));
	
	public static final ImageIcon floppyButtonIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "floppy.png").getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH));
	
	public static final ImageIcon horizontal = new ImageIcon("icons" + File.separator + "horizontal.png");
	public static final ImageIcon vertical = new ImageIcon("icons" + File.separator + "vertical.png");
	public static final ImageIcon join_l = new ImageIcon("icons" + File.separator + "l-join.png");
	public static final ImageIcon join_t = new ImageIcon("icons" + File.separator + "t-join.png");
	public static final ImageIcon join_l1 = new ImageIcon("icons" + File.separator + "l1-join.png");
	
	public static final ImageIcon addAllButtonIcon = new ImageIcon("icons" + File.separator + "add-all.png");
	public static final ImageIcon removeAllButtonIcon = new ImageIcon("icons" + File.separator + "minus-all.png");
	public static final ImageIcon configurationButtonIconBig = new ImageIcon("icons" + File.separator + "wheels.png");
	
	public static final ImageIcon bulbOff = new ImageIcon(new ImageIcon("icons" + File.separator + "bulb-off.png").getImage().getScaledInstance(10, 10, Image.SCALE_SMOOTH));
	public static final ImageIcon bulbOn = new ImageIcon(new ImageIcon("icons" + File.separator + "bulb-on.png").getImage().getScaledInstance(10, 10, Image.SCALE_SMOOTH));
	
	public static final ImageIcon arrowUpDash = new ImageIcon(new ImageIcon("icons" + File.separator + "arrow-up-dash.png").getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
	public static final ImageIcon arrowDownDash = new ImageIcon(new ImageIcon("icons" + File.separator + "arrow-down-dash.png").getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));

	public static final ImageIcon linkModeIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "link-mode.png").getImage().getScaledInstance(225, 60, Image.SCALE_SMOOTH));
	public static final ImageIcon dedupeModeIcon = new ImageIcon(new ImageIcon("icons" + File.separator + "dedupe-mode.png").getImage().getScaledInstance(225, 60, Image.SCALE_SMOOTH));
	
	public static final JButton getAnalysisButton() {
		JButton button = new JButton(analysisButtonIcon);
		button.setPreferredSize(new Dimension(30, 30));
		return button;
	}
	
	public static final JButton getAnalysisMinusButton() {
		JButton button = new JButton(analysisMinusButtonIcon);
		button.setPreferredSize(new Dimension(30, 30));
		return button;
	}
	
	public static final JButton getDetailsButton() {
		JButton button = new JButton(detailsButtonIcon);
		button.setPreferredSize(new Dimension(30, 30));
		return button;
	}
	
	public static JButton getViewMinusButton() {
		JButton button = new JButton(analysisMinusButtonIcon);
		button.setPreferredSize(new Dimension(30, 30));
		return button;
	}
	
	public static final JButton getConfigurationButton() {
		JButton button = new JButton(configurationButtonIcon);
		button.setPreferredSize(new Dimension(30, 30));
		return button;
	}
	
	public static JButton getViewResultsButton() {
		return getAnalysisButton();
	}

	public static final Dimension PREFERRED_SIZE = new Dimension(20, 20);

	public static ImageIcon scale(ImageIcon image, int x, int y) {
		return new ImageIcon(image.getImage().getScaledInstance(y, y, Image.SCALE_SMOOTH));
	}

	public static JButton getColorChooseButton() {
		JButton button = new JButton("Change");
		button.setPreferredSize(new Dimension(100, 20));
		return button;
	}

	public static JButton getFilterButton() {
		JButton button = new JButton(filterButtonIcon);
		button.setPreferredSize(new Dimension(30, 30));
		return button;
	}
	
	public static JButton getSortButton() {
		JButton button = new JButton(sortButtonIcon);
		button.setPreferredSize(new Dimension(30, 30));
		return button;
	}

	public static JButton getForwardButton() {
		JButton button = new JButton(forwardButtonIcon);
		button.setPreferredSize(new Dimension(17, 17));
		return button;
	}
	
	public static JButton getBackwardButton() {
		JButton button = new JButton(backwardButtonIcon);
		button.setPreferredSize(new Dimension(17, 17));
		return button;
	}

	public static JButton getSaveButton() {
		JButton button = new JButton(floppyButtonIcon);
		button.setPreferredSize(new Dimension(30, 30));
		return button;
	}
}
