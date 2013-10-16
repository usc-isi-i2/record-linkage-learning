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


package cdc.gui.components.linkagesanalysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.components.Filter;
import cdc.components.JoinListener;
import cdc.configuration.ConfiguredSystem;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.converters.ModelGenerator;
import cdc.gui.components.dialogs.OneTimeTipDialog;
import cdc.gui.components.linkagesanalysis.dialog.LinkagesWindowPanel;
import cdc.gui.components.linkagesanalysis.dialog.ViewLinkagesFrame;
import cdc.gui.components.linkagesanalysis.dialog.ViewLinkagesMultiFileFrame;
import cdc.impl.datasource.text.CSVDataSource;
import cdc.impl.join.common.DataSourceNotJoinedJoinListener;
import cdc.utils.RJException;

public class MinusAnalysisProvider {
	
	private DataColumnDefinition[][] dataModel;
	private AbstractDataSource[] source;
	private String requiredSource;
	private String minusFiles[];
	private String windowTitle;
	
	public MinusAnalysisProvider(ConfiguredSystem system, int srcId) throws IOException, RJException {
		
		//If system is deduplication and srcID == -1, then this is deduplicated file request
		//If system is deduplication and srcID == 0, then this is duplicates file request
		
		List notJoinedListeners = new ArrayList();
		
		if (system.isDeduplication()) {
			if (srcId == 0) {
				OneTimeTipDialog.showInfoDialogIfNeeded("Duplicate records data", OneTimeTipDialog.DUPLICATES_VIEWER, OneTimeTipDialog.DUPLICATES_VIEWER_MESSAGE);
				windowTitle = "Duplicates";
				minusFiles = new String[] {system.getSourceA().getDeduplicationConfig().getMinusFile()};
			} else {
				OneTimeTipDialog.showInfoDialogIfNeeded("Deduplicated data", OneTimeTipDialog.DEDUPLICATED_VIEWER, OneTimeTipDialog.DEDUPLICATED_VIEWER_MESSAGE);
				windowTitle = "Deduplicated data";
				minusFiles = new String[] {system.getSourceA().getDeduplicationConfig().getDeduplicatedFileName()};
			}
			requiredSource = system.getSourceA().getSourceName();
		} else {
			OneTimeTipDialog.showInfoDialogIfNeeded("Not joined records data", OneTimeTipDialog.MINUS_VIEWER, OneTimeTipDialog.MINUS_VIEWER_MESSAGE);
			AbstractJoin join = system.getJoin();
			requiredSource = srcId == 1 ? join.getSourceA().getSourceName() : join.getSourceB().getSourceName();
			windowTitle = "Not joined records (" + requiredSource + ")";
			List listeners = join.getListeners();
			for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
				JoinListener listener = (JoinListener) iterator.next();
				if (listener instanceof DataSourceNotJoinedJoinListener) {
					//potential listener
					DataSourceNotJoinedJoinListener notJoinedListener = (DataSourceNotJoinedJoinListener)listener;
					AbstractDataSource src = notJoinedListener.getSource();
					if (src.getSourceName().equals(requiredSource)) {
						notJoinedListeners.add(notJoinedListener);
					}
				}
			}
			
			if (notJoinedListeners.size() == 0) {
				throw new RuntimeException("Proper join listener was not found. Required minus join listener for source " + requiredSource);
			}
			
			DataSourceNotJoinedJoinListener[] arrayListeners = (DataSourceNotJoinedJoinListener[]) notJoinedListeners.toArray(new DataSourceNotJoinedJoinListener[] {});
			minusFiles = new String[arrayListeners.length];
			for (int i = 0; i < minusFiles.length; i++) {
				minusFiles[i] = arrayListeners[i].getFileName();
			}
		}
		
		source = new AbstractDataSource[minusFiles.length];
		dataModel = new DataColumnDefinition[minusFiles.length][];
		for (int i = 0; i < minusFiles.length; i++) {
			Map props = new HashMap();
			props.put(CSVDataSource.PARAM_INPUT_FILE, minusFiles[i]);
			props.put(CSVDataSource.PARAM_DELIM, ",");
			source[i] = new CSVDataSource("m-" + requiredSource, props);
			source[i].setModel(new ModelGenerator(source[i].getAvailableColumns()));
			dataModel[i] = source[i].getDataModel().getOutputFormat();
		}
		
	}

	public JFrame getFrame() {
		if (source.length == 1) {
			JFrame frame = new ViewLinkagesFrame(new DataColumnDefinition[][] {dataModel[0]}, false, null, null, null, null, new ThreadCreator(0));
			frame.setTitle(windowTitle);
			return frame;
	 	} else {
	 		DataColumnDefinition[][][] model = new DataColumnDefinition[source.length][1][];
	 		for (int i = 0; i < model.length; i++) {
				model[i][0] = dataModel[i];
			}
	 		ThreadCreatorInterface[] ths = new ThreadCreatorInterface[source.length];
	 		for (int i = 0; i < ths.length; i++) {
				ths[i] = new ThreadCreator(i);
			}
	 		DataColumnDefinition[] conf = new DataColumnDefinition[ths.length];
	 		DataColumnDefinition[] strat = new DataColumnDefinition[ths.length];
	 		DataColumnDefinition[][][] compared = new DataColumnDefinition[ths.length][][];
	 		JFrame frame = new ViewLinkagesMultiFileFrame(minusFiles, model, true, conf, strat, compared, ths);
			frame.setTitle(windowTitle);
			return frame;
	 	}
	}
	
	private class ThreadCreator implements ThreadCreatorInterface {
		
		private int id;
		
		public ThreadCreator(int id) {
			this.id = id;
		}
		
		public LoadingThread createNewThread(ThreadCreatorInterface provider, LinkagesWindowPanel parent, Filter filter, DataColumnDefinition[] sort, int[] order) {
			return new MinusLoadingThread(provider, parent, filter, sort, order);
		}

		public AbstractDataSource getDataSource(Filter filter) throws IOException, RJException {
			AbstractDataSource src = source[id].copy();
			if (filter != null) {
				src.setFilter(filter);
			}
			return src;
		}
		
	}
	
}
