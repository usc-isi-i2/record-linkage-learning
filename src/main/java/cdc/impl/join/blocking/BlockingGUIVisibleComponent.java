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


package cdc.impl.join.blocking;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JPanel;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoinCondition;
import cdc.datamodel.DataColumnDefinition;
import cdc.gui.GUIVisibleComponent;
import cdc.gui.components.uicomponents.BlockingAttributePanel;
import cdc.utils.RJException;

/**
 * This class creates GUI component for the blocking search method. 
 * The GUI generated here allows users to configure the blocking search method.
 * This class also provides a functionality of creating BlockingJoin object from
 * given GUI configuration. The class extends general class GUIVisibleComponent.
 * @author Pawel Jurczyk
 *
 */
public class BlockingGUIVisibleComponent extends GUIVisibleComponent {

	/**
	 * The two constants that are used as a names of BlockingJoin attributes.
	 * Note that the blocking join has two attributes: a blocking function and an attribute(s) used for blocking.
	 */
	protected static final String BLOCKING_FUNCTION = "blocking-function";
	protected static final String BLOCKING_ATTR = "blocking-attr";
		
	/**
	 * The reference to first data source 
	 */
	protected AbstractDataSource sourceA;
	
	/**
	 * The reference to the second data source
	 */
	protected AbstractDataSource sourceB;
	
	/**
	 * The linkage condition
	 */
	protected AbstractJoinCondition joinCondition;
	
	/**
	 * The configuration of output attributes (the attributes of records returned from linkage process)
	 */
	protected DataColumnDefinition[] outModel;
	
	/**
	 * The panel that actually allows the configuration of blocking paramters.
	 */
	private BlockingAttributePanel blockConfig;
	
	/**
	 * The implementation of abstract method from GUIVisibleComponent that creates the actual linkage method instance.
	 * This method knows how to create an instance of BlockingJoin (e.g., it is aware of the attributes that are required by
	 * the BlockingJoin).
	 */
	public Object generateSystemComponent() throws RJException, IOException {
		Map properties = new HashMap();
		properties.put(BlockingJoin.BLOCKING_PARAM, String.valueOf(blockConfig.getBlockingAttributeId()));
		properties.put(BlockingJoin.BLOCKING_FUNCTION, blockConfig.getBlockingFunction());
		return new BlockingJoin(sourceA.getPreprocessedDataSource(), sourceB.getPreprocessedDataSource(), outModel, joinCondition, properties);
	}

	/**
	 * The implementation of abstract method from GUIVisibleComponent that creates the JPanel containing
	 * the GUI for configuration of blocking search method. 
	 * The array of objects that are passed to this method are as follows:
	 * [first-source, second-source, out-model, join-condition].
	 * Note that the format above is used for all the GUIVisibleComponents that allow configuration of search method.
	 * This GUI component actually does not use the sizes provided as the input, but uses layout managers 
	 * to layout the components on nice way.
	 */
	public JPanel getConfigurationPanel(Object[] objects, int sizeX, int sizeY) {
		
		//Decode the input attributes and store them in the class
		this.sourceA = (AbstractDataSource) objects[0];
		this.sourceB = (AbstractDataSource) objects[1];
		this.outModel = (DataColumnDefinition[]) objects[2];
		this.joinCondition = (AbstractJoinCondition) objects[3];
		
		//Generate options for hash attributes
		String[] hashAttrs = new String[joinCondition.getLeftJoinColumns().length];
		for (int i = 0; i < hashAttrs.length; i++) {
			hashAttrs[i] = joinCondition.getLeftJoinColumns()[i] + 
				" and " + joinCondition.getRightJoinColumns()[i];
		}
		
		//Generate the configuration of blocking function
		blockConfig = new BlockingAttributePanel(hashAttrs, joinCondition.getDistanceFunctions());
		
		//If some configuration was loaded before, restore it!
		String restoredAttribute = getRestoredParam(BlockingJoin.BLOCKING_PARAM);
		if (restoredAttribute != null) {
			blockConfig.setBlockingAttribute(Integer.parseInt(restoredAttribute));
		}
		String restoredFunction = getRestoredParam(BlockingJoin.BLOCKING_FUNCTION);
		if (restoredFunction != null) {
			blockConfig.setBlockingFunction(restoredFunction);
		}
		
		//Return the panel with GUI configuration
		return blockConfig;
	}

	/**
	 * The implementation of abstract method from GUIVisibleComponent.
	 * Returns the Class object that is created by this class.
	 */
	public Class getProducedComponentClass() {
		return BlockingJoin.class;
	}

	/**
	 * A string representation of the visible component. Used to display the option to user,
	 * so it should be meaningful and easy to understand :)
	 */
	public String toString() {
		return "Blocking search method";
	}

	/**
	 * The implementation of abstract method from GUIVisibleComponent.
	 * This always returns true for this GUI component as nothing needs to be validated (or checked for correctness of input).
	 */
	public boolean validate(JDialog dialog) {
		return true;
	}

}
