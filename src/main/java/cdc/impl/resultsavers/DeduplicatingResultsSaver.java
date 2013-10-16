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


package cdc.impl.resultsavers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cdc.components.AbstractJoin;
import cdc.components.AbstractResultsSaver;
import cdc.configuration.Configuration;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.PropertyBasedColumn;
import cdc.gui.MainFrame;
import cdc.gui.components.linkagesanalysis.DuplicateLinkageDecisionProvider;
import cdc.gui.components.linkagesanalysis.dialog.DecisionListener;
import cdc.impl.MainApp;
import cdc.impl.datasource.wrappers.ExternallySortingDataSource;
import cdc.impl.datasource.wrappers.SortedData;
import cdc.utils.CPUInfo;
import cdc.utils.CompareFunctionInterface;
import cdc.utils.Log;
import cdc.utils.Props;
import cdc.utils.RJException;
import cdc.utils.RowUtils;
import cdc.utils.comparators.StringComparator;
import edu.emory.mathcs.util.xml.DOMUtils;

public class DeduplicatingResultsSaver extends AbstractResultsSaver {

	private static int RESULTS_DEDUPE_BUFFER = Props.getInteger("results-dedupe-sort-buffer", ExternallySortingDataSource.BUFFER_SIZE / CPUInfo.testNumberOfCPUs());
	private static final String DEDUPE_SRC = "dedupe-results";
	private static CompareFunctionInterface[] comps = new CompareFunctionInterface[] {new StringComparator()};
	
	private class SortedDataDataWriter implements DataWriter {
		private SortedData data;
		public SortedDataDataWriter(SortedData data) {
			this.data = data;
		}
		public void finish() throws IOException, RJException {
			data.complete();
		}
		public synchronized void writeRow(DataRow row) throws IOException {
			data.addRow(row);
		}
	}
	
	private class ResultsSaverDataWriter implements DataWriter {
		public void finish() throws IOException, RJException {
		}
		public synchronized void writeRow(DataRow row) throws IOException, RJException {
			savedCnt++;
			for (int i = 0; i < savers.length; i++) {
				savers[i].saveRow(row);
			}
		}
	}
	
	private int activePhase = 0;
	private DataColumnDefinition[] sortPhases;
	private boolean deleteDuplicates = false;
	private boolean askBeforeDeleting = false;
	private volatile RJException rjExc = null;
	private volatile IOException ioExc = null;
	
	private int duplicatesCnt = 0;
	private int savedCnt = 0;
	
	private int addedCnt = 0;
	
	private AbstractResultsSaver[] savers;
	
	//decision maker when deleting linkages with the same probability
	private DuplicateLinkageDecisionProvider decisionProvider;
	
	//internal writer
	private SortedData data;
	
	public DeduplicatingResultsSaver(AbstractResultsSaver[] savers, Map props) throws IOException {
		super(props);
		this.savers = savers;
		
		String dedupeConfig = null;
		if ((dedupeConfig = getProperty("deduplication")) != null) {
			//possible options: left, right, both
			if (dedupeConfig.equals("left")) {
				sortPhases = new DataColumnDefinition[1];
				sortPhases[0] = new PropertyBasedColumn(AbstractJoin.PROPERTY_SRCA_ID, DEDUPE_SRC);
			} else if (dedupeConfig.equals("right")) {
				sortPhases = new DataColumnDefinition[1];
				sortPhases[0] = new PropertyBasedColumn(AbstractJoin.PROPERTY_SRCB_ID, DEDUPE_SRC);
			} else {
				//assume both
				sortPhases = new DataColumnDefinition[2];
				sortPhases[0] = new PropertyBasedColumn(AbstractJoin.PROPERTY_SRCA_ID, DEDUPE_SRC);
				sortPhases[1] = new PropertyBasedColumn(AbstractJoin.PROPERTY_SRCB_ID, DEDUPE_SRC);
			}
		} else {
			throw new RuntimeException("DeduplicationResultsSaver requires attrubute 'deduplication'");
		}
		deleteDuplicates = !"false".equals(getProperty("delete-duplicates"));
		askBeforeDeleting = !"true".equals(getProperty("delete-duplicates"));
	}

	public void close() throws IOException, RJException {
		if (data != null) {
			data.cleanup();
		}
		data = null;
		for (int i = 0; i < savers.length; i++) {
			savers[i].close();
		}
		//getCache().trash();
	}

	private void doDeduplication() throws IOException, RJException {
		//System.out.println("Deduplication starts. Added: " + addedCnt);
		DataWriter writer;
		SortedData nextPhaseData = null;
		if (data == null) {
			return;
		}
		while (activePhase <= sortPhases.length) {
			Log.log(getClass(), "Results deduplication, phase " + activePhase + " out of " + sortPhases.length);
			if (activePhase < sortPhases.length) {
				nextPhaseData = new SortedData(RESULTS_DEDUPE_BUFFER, DEDUPE_SRC, new DataColumnDefinition[] {sortPhases[activePhase]}, comps);
				writer = new SortedDataDataWriter(nextPhaseData);
			} else {
				writer = new ResultsSaverDataWriter();
			}
			identifyDuplicates(data, writer, sortPhases[activePhase - 1]);
			data.cleanup();
			data = nextPhaseData;
			activePhase++;
			
			//just wait for the finish
			try {
				while (decisionProvider != null && !decisionProvider.isDone()) {
					synchronized (this) {
						wait(100);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (decisionProvider != null) {
				decisionProvider.closeDecisionWindow();
				decisionProvider = null;
			}
		}
		
		if (decisionProvider != null) {
			decisionProvider.closeDecisionWindow();
		}
		if (ioExc != null) {
			throw ioExc;
		}
		if (rjExc != null) {
			throw rjExc;
		}
		
	}

	public void flush() throws IOException, RJException {
		Log.log(getClass(), "Results deduplication starts.");
		
		doDeduplication();
		
		Log.log(getClass(), "Removed " + duplicatesCnt + " duplicate(s) from results.");
		//MainApp.main.appendLinkageSummary("\nResults deduplication identified " + duplicatesCnt + " duplicate(s).\n");
		//MainApp.main.appendLinkageSummary("Saved " + savedCnt + " linkage(s) to result files.\n");
		
		if (data != null) {
			data.cleanup();
		}
		
		for (int i = 0; i < savers.length; i++) {
			savers[i].flush();
		}
	}

	public void reset() throws IOException, RJException {
		if (data != null) {
			data.cleanup();
		}
		activePhase = 0;
		data = null;
		duplicatesCnt = 0;
		addedCnt = 0;
		savedCnt = 0;
		for (int i = 0; i < savers.length; i++) {
			savers[i].reset();
		}
		decisionProvider = null;
		ioExc = null;
		rjExc = null;
	}

	public void saveRow(DataRow row) throws RJException, IOException {
		if (data == null) {
			data = new SortedData(RESULTS_DEDUPE_BUFFER, DEDUPE_SRC, new DataColumnDefinition[] {sortPhases[activePhase++]}, comps);
		}
		addedCnt++;
		data.addRow(row);
	}

	public void saveToXML(Document doc, Element saver) {
		Configuration.appendParams(doc, saver, getProperties());
		Element saversXML = DOMUtils.createChildElement(doc, saver, "savers");
		for (int i = 0; i < savers.length; i++) {
			Element s = DOMUtils.createChildElement(doc, saversXML, Configuration.RESULTS_SAVER_TAG);
			DOMUtils.setAttribute(s, Configuration.CLASS_ATTR, savers[i].getClass().getName());
			savers[i].saveToXML(doc, s);
		}
		
	}
	
	public static AbstractResultsSaver fromXML(Element element) throws RJException, IOException {
		Element paramsElement = DOMUtils.getChildElement(element, Configuration.PARAMS_TAG);
		Map params = Configuration.parseParams(paramsElement);
		
		Element saversXML = DOMUtils.getChildElement(element, "savers");
		Element[] children = DOMUtils.getChildElements(saversXML);
		AbstractResultsSaver[] savers = new AbstractResultsSaver[children.length];
		for (int i = 0; i < savers.length; i++) {
			savers[i] = AbstractResultsSaver.fromXML(children[i]);
		}
		
		return new DeduplicatingResultsSaver(savers, params);
	}
	
	public AbstractResultsSaver[] getChildren() {
		return savers;
	}
	
	private void identifyDuplicates(SortedData inputData, DataWriter outputData, DataColumnDefinition sortedKey) throws IOException, RJException {
		DataRow example = null;
		List buffer = new ArrayList();
		example = inputData.getNextSortedRow();
//		int n = 0;
//		int s = 0;
//		int n1 = 0;
		while (true) {
			if (example == null) {
				break;
			}
			//n++;
			buffer.add(example);
			DataRow row;
			while (isTheSameKey(row = inputData.getNextSortedRow(), example, sortedKey) && row != null) {
				buffer.add(row);
				//n++;
			}
			//n1 += buffer.size();
			/*s += */
			solveGroup(outputData, buffer, sortedKey);
			buffer.clear();
			example = row;
		}
		//System.out.println("Read: n=" + n + "  s=" + s + " duplicates: " + duplicatesCnt + "  buffer tot = " + n1);
		
	}

	private int solveGroup(DataWriter saver, List buffer, DataColumnDefinition sortedKey) throws RJException, IOException {
		int maxConfidence = 0;
		if (buffer.isEmpty()) {
			return 0;
		}
		List toSolve = new ArrayList();
		for (Iterator iterator = buffer.iterator(); iterator.hasNext();) {
			DataRow row = (DataRow) iterator.next();
			int conf = Integer.parseInt(row.getProperty(AbstractJoin.PROPERTY_CONFIDNCE));
			if (maxConfidence < conf) {
				maxConfidence = conf;
				synchronized (this) {
					duplicatesCnt += toSolve.size();
				}
				//now - save the records to minus (if needed)...
				for (Iterator iterator2 = toSolve.iterator(); iterator2.hasNext();) {
					DataRow rejected = (DataRow) iterator2.next();
					saveToMinusIfNeeded(rejected);
				}
				toSolve.clear();
				toSolve.add(row);
			} else if (maxConfidence == conf) {
				toSolve.add(row);
			} else {
				synchronized (this) {
					duplicatesCnt++;
				}
				saveToMinusIfNeeded(row);
			}
		}
		
		if (!deleteDuplicates) {
			//just save all the records
			for (Iterator iterator = toSolve.iterator(); iterator.hasNext();) {
				DataRow row = (DataRow) iterator.next();
				saver.writeRow(row);
			}
			return toSolve.size();
		} else {
			if (toSolve.size() > 1 && askBeforeDeleting && MainFrame.main != null) {
				if (decisionProvider == null) {
					decisionProvider = new DuplicateLinkageDecisionProvider("Results deduplication - manual decision", new DuplicateDecisionListener(saver));
				}
				decisionProvider.addUndecidedRecords((DataRow[]) toSolve.toArray(new DataRow[] {}));
				return 0;
			} else {
				//just save the first one...
				saver.writeRow((DataRow) toSolve.remove(0));
				synchronized (this) {
					duplicatesCnt += toSolve.size();
				}
				for (Iterator iterator2 = toSolve.iterator(); iterator2.hasNext();) {
					DataRow rejected = (DataRow) iterator2.next();
					saveToMinusIfNeeded(rejected);
				}
				return 1;
			}
		}
		//toSolve.clear();
		
	}

	private synchronized void saveToMinusIfNeeded(DataRow rejected) {
		DataRow rowA = (DataRow) rejected.getObjectProperty(AbstractJoin.PROPERTY_RECORD_SRCA);
		DataRow rowB = (DataRow) rejected.getObjectProperty(AbstractJoin.PROPERTY_RECORD_SRCB);
		AbstractJoin join = MainApp.main.getConfiguredSystem().getJoin();
		if (!decrementAndCheck(rowA)) {
			//System.out.println("Saving to minus: " + rowA);
			try {
				join.notifyTrashingNotJoined(rowA);
			} catch (RJException e) {
				e.printStackTrace();
			}
		}
		if (!decrementAndCheck(rowB)) {
			//System.out.println("Saving to minus: " + rowB);
			try {
				join.notifyTrashingNotJoined(rowB);
			} catch (RJException e) {
				e.printStackTrace();
			}
		}
	}

	private boolean decrementAndCheck(DataRow row) {
		AbstractJoin join = MainApp.main.getConfiguredSystem().getJoin();
		return RowUtils.decrement(join, row, AbstractJoin.PROPERTY_JOIN_MULTIPLICITY) != 0;
		
//		Integer cnt = (Integer) row.getObjectProperty(AbstractJoin.PROPERTY_JOIN_MULTIPLICITY);
//		if (cnt.intValue() == 1) {
//			return false;
//		}
//		row.setProperty(AbstractJoin.PROPERTY_JOIN_MULTIPLICITY, new Integer(cnt.intValue() - 1));
//		return true;
	}

	private boolean isTheSameKey(DataRow r1, DataRow r2, DataColumnDefinition sortedKey) {
		if (r1 == null || r2 == null) {
			return false;
		}
		return r1.getData(sortedKey).getValue().equals(r2.getData(sortedKey).getValue());
	}
	
	private class DuplicateDecisionListener implements DecisionListener {
		
		private DataWriter saver;
		
		public DuplicateDecisionListener(DataWriter saver) {
			this.saver = saver;
		}
		public void linkageAccepted(DataRow linkage) {
			try {
				saver.writeRow(linkage);
			} catch (IOException e) {
				ioExc = e;
			} catch (RJException e) {
				rjExc = e;
			}
		}
		public void linkageRejected(DataRow linkage) {
			//System.out.println("Rejecting: " + linkage);
			synchronized (DeduplicatingResultsSaver.this) {
				duplicatesCnt++;
			}
			saveToMinusIfNeeded(linkage);
		}
	}
	
	public int getDuplicatesCnt() {
		return duplicatesCnt;
	}

	public int getSavedCnt() {
		return savedCnt;
	}
	
}
