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


package cdc.impl;

import java.io.IOException;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractJoin;
import cdc.components.AbstractResultsSaver;
import cdc.configuration.Configuration;
import cdc.configuration.ConfiguredSystem;
import cdc.datamodel.DataRow;
import cdc.impl.resultsavers.CSVFileSaver;
import cdc.impl.resultsavers.DeduplicatingResultsSaver;
import cdc.impl.resultsavers.ResultSaversGroup;
import cdc.utils.RJException;

/**
 * This class is a main entry point to run FRIL as batch process or to invoke linkage from java code.
 * The class enables one to create FRIL linkage instance and start it.
 * @author Pawel Jurczyk
 *
 */
public class Main implements FrilAppInterface {
	
	private Configuration join;
	private ConfiguredSystem system;
	private StringBuilder linkageSummary;
	
	/**
	 * This constructor reads the FRIL configuration from specified configuration file. 
	 * The default configuration file can be obtained from Configuration.DEFAULT_CONFIGURATION_FILE. 
	 * @param configFile path to configuration file
	 * @throws RJException
	 * @throws IOException
	 */
	public Main(String configFile) throws RJException, IOException {
		join = Configuration.getConfiguration(configFile);
		system = new ConfiguredSystem(join.getDataSources()[0], join.getDataSources()[1], join.getJoin(), join.getResultsSaver());
	}
	
	/**
	 * Starts linkage process. The method is blocking (e.g., it blocks until the linkage finishes).
	 * @return number of linked pairs
	 * @throws RJException
	 * @throws IOException
	 */
	public int runJoin() throws RJException, IOException {
		return startJoin(join.getJoin(), join.getResultsSaver());
	}
	
	/**
	 * The main method that starts FRIL batch processing. 
	 * @param args
	 * @throws IOException
	 * @throws RJException
	 */
	public static void main(String[] args) throws IOException, RJException {
		
		String configFile = Configuration.DEFAULT_CONFIGURATION_FILE;
		if (System.getProperty("config") != null) {
			configFile = System.getProperty("config");
		}
		
		System.out.println("Using configuration file: " + configFile);
		
		Main main = new Main(configFile);
		main.runJoin(true);
	}

	private int runJoin(boolean printSummary) throws RJException, IOException {
		
		long t1 = System.currentTimeMillis();
		int n = runJoin();
		long t2 = System.currentTimeMillis();
		
		System.out.println("\n" + join + ": Algorithm produced " + n + " joined tuples. Elapsed time: " + (t2 - t1) + "ms.");
		if (!linkageSummary.toString().isEmpty()) {
			System.out.println(linkageSummary.toString());
		}
		return n;
	}

	private int startJoin(AbstractJoin join, AbstractResultsSaver abstractResultsSaver) throws IOException, RJException {
		int n = 0;
		DataRow row;
		while ((row = join.joinNext()) != null) {
			n++;
			if (n % 1000 == 0) System.out.print(".");
			
			abstractResultsSaver.saveRow(row);
		}
		
		abstractResultsSaver.flush();
		abstractResultsSaver.close();
		
		return n;
	}

//	public int rerun() throws IOException, RJException {
//		join.getJoin().reset();
//		return startJoin(join.getJoin(), join.getResultsSaver());
//	}
	
	/**
	 * This method closes FRIL linkage. Should be called when linkage is complete.
	 */
	public void close() throws IOException, RJException {
		join.getJoin().close();
	}
	
	/**
	 * Advanced. Method returns FRIL linkage. The AbstractJoin provides method joinNext() that returns next linkage.
	 * When end of the process is reached, the joinNext() returns null.
	 * @return
	 */
	public AbstractJoin getLinkage() {
		return join.getJoin();
	}
	
	/**
	 * Advanced. Method returns AbstractResultSaver that is used to save results of the linkage.
	 * @return
	 */
	public AbstractResultsSaver getResultSaver() {
		return join.getResultsSaver();
	}
	
	/**
	 * Advanced. Method returns two datasources being used in the linkage.
	 * @return
	 */
	public AbstractDataSource[] getSources() {
		return join.getDataSources();
	}

	/**
	 * This method returns location of 'minus' files (files containing not joined records).
	 */
	public String getMinusDirectory() {
		AbstractResultsSaver savers = join.getResultsSaver();
		AbstractResultsSaver[] group = null;
		if (savers instanceof CSVFileSaver) {
			return ((CSVFileSaver) savers).getActiveDirectory();
		} else if (savers instanceof DeduplicatingResultsSaver) {
			group = ((DeduplicatingResultsSaver)savers).getChildren();
		} else if (savers instanceof ResultSaversGroup) {
			group = ((ResultSaversGroup)savers).getChildren();
		}
		if (group != null) {
			for (int i = 0; i < group.length; i++) {
				if (group[i] instanceof CSVFileSaver) {
					return ((CSVFileSaver) group[i]).getActiveDirectory();
				}
			}
		}
		return "";
	}

	public void appendLinkageSummary(String text) {
		linkageSummary.append(text);
	}

	public int rerun() throws IOException, RJException {
		join.getJoin().reset();
		return runJoin();
	}

	public ConfiguredSystem getConfiguredSystem() {
		return system;
	}
	
}
