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


package cdc.impl.deduplication;

import java.lang.reflect.Constructor;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cdc.components.AbstractDataSource;
import cdc.components.AbstractDistance;
import cdc.configuration.Configuration;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.converters.ModelGenerator;
import cdc.impl.distance.EqualFieldsDistance;
import cdc.impl.join.blocking.BlockingFunction;
import cdc.impl.join.blocking.BlockingFunctionFactory;
import cdc.impl.join.blocking.EqualityBlockingFunction;
import cdc.utils.RJException;
import edu.emory.mathcs.util.xml.DOMUtils;

public class DeduplicationConfig {
	
	public static final String CONDITIONS_TAG = "deduplication-condition";
	public static final String CONDITION_TAG = "condition";
	private static final String COLUMN_TAG = "column";
	private static final String HASHING_FUNCTION_TAG = "hashing-function";
	private static final String MINUS_ELEMENT_TAG = "minus-file";
	private static final String FILE_ELEMENT_TAG = "dedupe-file";
	private static final String COLUMNS_TAG = "columns";
	private static final String HASH_TAG = "hash";
	private static final String FILE = "file";
	private static final String EMPTY_MATCH = "empty-match";
	private static final String WEIGHT = "weight";
	private static final String ACCEPTANCE_LEVEL = "acceptance-level";
	
	private String minusFile = null;
	private String dedupeFile = null;
	
	private DataColumnDefinition[] testedColumns;
	private AbstractDistance[] testCondition;
	private int[] weights;
	private double[] emptyMatchScore;
	private int acceptanceLevel = 100;
	
	private boolean hashing = false;
	private BlockingFunction hashingFunction;
	
	private boolean snm = false;
	
	public DeduplicationConfig(DataColumnDefinition[] testedColumns, AbstractDistance[] distances, int[] weights, double[] emptyMatch) {
		this.testCondition = distances;
		this.testedColumns = testedColumns;
		this.weights = weights;
		this.emptyMatchScore = emptyMatch;
		if (weights[0] == -1) {
			int sum = 0;
			for (int i = 0; i < this.weights.length; i++) {
				this.weights[i] = 100 / this.weights.length;
				sum += this.weights[i];
			}
			this.weights[this.weights.length - 1] += 100 - sum;
		}
	}
	
	public DeduplicationConfig(AbstractDataSource dataSource) {
		ModelGenerator generator = dataSource.getDataModel();
		this.testedColumns = generator.getOutputFormat();
		this.testCondition = new AbstractDistance[this.testedColumns.length];
		for (int i = 0; i < testCondition.length; i++) {
			this.testCondition[i] = new EqualFieldsDistance();
		}
		this.hashingFunction = new EqualityBlockingFunction(new DataColumnDefinition[][] {new DataColumnDefinition[] {testedColumns[0]}});
		this.emptyMatchScore = new double[this.testedColumns.length];
		this.weights = new int[this.testedColumns.length];
		for (int i = 0; i < emptyMatchScore.length; i++) {
			emptyMatchScore[i] = 0;
			weights[i] = 100 / emptyMatchScore.length;
		}
		
		//ensure correct weights...
		int sum = 0;
		for (int i = 0; i < weights.length; i++) {
			sum += weights[i];
		}
		weights[weights.length - 1] += 100 - sum;
	}

	public void setHashingConfig(BlockingFunction function) {
		this.snm = false;
		this.hashing = true;
		this.hashingFunction = function;
	}

	public DataColumnDefinition[] getTestedColumns() {
		return testedColumns;
	}

	public AbstractDistance[] getTestCondition() {
		return testCondition;
	}

	public boolean isHashing() {
		return hashing;
	}

	public BlockingFunction getHashingFunction() {
		return hashingFunction;
	}

	public boolean isSnm() {
		return snm;
	}
	
	public void setMinusFile(String minusFile) {
		this.minusFile = minusFile;
	}
	
	public String getMinusFile() {
		return minusFile;
	}
	
	public static DeduplicationConfig fromXML(AbstractDataSource source, Element dedupElement) throws RJException {
		Element cond = DOMUtils.getChildElement(dedupElement, CONDITIONS_TAG);
		Element[] children = DOMUtils.getChildElements(cond);
		DataColumnDefinition[] cols = new DataColumnDefinition[children.length];
		AbstractDistance[] dists = new AbstractDistance[children.length];
		double[] emptyMatch = new double[children.length];
		int[] weights = new int[children.length];
		for (int i = 0; i < children.length; i++) {
			Element child = children[i];
			cols[i] = source.getDataModel().getColumnByName(DOMUtils.getAttribute(child, COLUMN_TAG));
			Map params = Configuration.parseParams(DOMUtils.getChildElement(child, Configuration.PARAMS_TAG));
			String className = DOMUtils.getAttribute(child, Configuration.CLASS_ATTR);
			try {
				Class clazz = Class.forName(className);
				Constructor constr = clazz.getConstructor(new Class[] {Map.class});
				dists[i] = (AbstractDistance) constr.newInstance(new Object[] {params});
			} catch (Exception e) {
				throw new RJException("Error reading join configuration", e);
			}
			
			weights[i] = Integer.parseInt(DOMUtils.getAttribute(child, WEIGHT, "-1"));
			
			try {
				emptyMatch[i] = Double.parseDouble(DOMUtils.getAttribute(child, EMPTY_MATCH, "0"));
			} catch (NumberFormatException e) {
				String score = DOMUtils.getAttribute(child, EMPTY_MATCH, "0");
				if (score.equals("true")) {
					emptyMatch[i] = 1;
				} else {
					emptyMatch[i] = 0;
				}
			}
		}
		int acceptanceLevel = Integer.parseInt(DOMUtils.getAttribute(cond, ACCEPTANCE_LEVEL, "100"));
		Element hashingFunct = DOMUtils.getChildElement(dedupElement, HASHING_FUNCTION_TAG);
		String function = DOMUtils.getAttribute(hashingFunct, HASH_TAG);
		String columns = DOMUtils.getAttribute(hashingFunct, COLUMNS_TAG);
		BlockingFunction blockingFunction = BlockingFunctionFactory.createBlockingFunction(new DataColumnDefinition[][] {decode(columns, source), decode(columns, source)}, function);
//		if (function.startsWith(SOUNDEX_PREFIX)) {
//			String paramsStr = function.substring(function.indexOf("(") + 1, function.length()-1);
//			blockingFunction = new SoundexBlockingFunction(new DataColumnDefinition[][] {decode(columns, source), decode(columns, source)}, Integer.parseInt(paramsStr));
//		} else if (function.startsWith(EQUALITY_PREFIX)) {
//			blockingFunction = new EqualityBlockingFunction(new DataColumnDefinition[][] {decode(columns, source), decode(columns, source)});
//		} else {
//			throw new RuntimeException("Property " + HASH_TAG + " accepts only soundex or equality options.");
//		}
		DeduplicationConfig config = new DeduplicationConfig(cols, dists, weights, emptyMatch);
		config.setHashingConfig(blockingFunction);
		
		Element minusElement = DOMUtils.getChildElement(dedupElement, MINUS_ELEMENT_TAG);
		if (minusElement != null) {
			config.setMinusFile(DOMUtils.getAttribute(minusElement, FILE));
		}
		
		Element fileElement = DOMUtils.getChildElement(dedupElement, FILE_ELEMENT_TAG);
		if (fileElement != null) {
			config.setDeduplicatedFileName(DOMUtils.getAttribute(fileElement, FILE));
		}
		config.setAcceptanceLevel(acceptanceLevel);
		
		return config;
	}

	private static DataColumnDefinition[] decode(String columns, AbstractDataSource source) {
		String[] cols = columns.split(",");
		DataColumnDefinition[] colDefs = new DataColumnDefinition[cols.length];
		for (int i = 0; i < colDefs.length; i++) {
			colDefs[i] = source.getDataModel().getColumnByName(cols[i]);
		}
		return colDefs;
	}

	public void saveToXML(Document doc, Element dedupElement) {
		Element cond = DOMUtils.createChildElement(doc, dedupElement, CONDITIONS_TAG);
		for (int i = 0; i < testedColumns.length; i++) {
			Element condition = DOMUtils.createChildElement(doc, cond, CONDITION_TAG);
			DOMUtils.setAttribute(condition, Configuration.CLASS_ATTR, testCondition[i].getClass().getName());
			DOMUtils.setAttribute(condition, COLUMN_TAG, testedColumns[i].getColumnName());
			if (emptyMatchScore[i] != 0) {
				DOMUtils.setAttribute(condition, EMPTY_MATCH, String.valueOf(emptyMatchScore[i]));
			}
			DOMUtils.setAttribute(condition, WEIGHT, String.valueOf(weights[i]));
			Configuration.appendParams(doc, condition, testCondition[i].getProperties());
		}
		DOMUtils.setAttribute(cond, ACCEPTANCE_LEVEL, String.valueOf(getAcceptanceLevel()));
		
		Element hashingFunct = DOMUtils.createChildElement(doc, dedupElement, HASHING_FUNCTION_TAG);
		DOMUtils.setAttribute(hashingFunct, COLUMNS_TAG, encode(hashingFunction.getColumns()[0]));
		DOMUtils.setAttribute(hashingFunct, HASH_TAG, BlockingFunctionFactory.encodeBlockingFunction(hashingFunction));
		
//		if (hashingFunction instanceof SoundexBlockingFunction) {
//			SoundexBlockingFunction shf = (SoundexBlockingFunction)hashingFunction;
//			DOMUtils.setAttribute(hashingFunct, HASH_TAG, SOUNDEX_PREFIX + "(" + shf.getSoundexDistance().getProperty(SoundexDistance.PROP_SIZE) + ")");
//		} else {
//			DOMUtils.setAttribute(hashingFunct, HASH_TAG, EQUALITY_PREFIX);
//		}
		
		if (minusFile != null) {
			Element minusElement = DOMUtils.createChildElement(doc, dedupElement, MINUS_ELEMENT_TAG);
			DOMUtils.setAttribute(minusElement, FILE, minusFile);
		}
		
		if (dedupeFile != null) {
			Element minusElement = DOMUtils.createChildElement(doc, dedupElement, FILE_ELEMENT_TAG);
			DOMUtils.setAttribute(minusElement, FILE, dedupeFile);
		}
	}

	private String encode(DataColumnDefinition[] dataColumnDefinitions) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < dataColumnDefinitions.length; i++) {
			if (i != 0) {
				buffer.append(",");
			}
			buffer.append(dataColumnDefinitions[i].getColumnName());
		}
		return buffer.toString();
	}

	public void fixIfNeeded(AbstractDataSource originalDataSource) {
		DataColumnDefinition[] sourceColumns = originalDataSource.getDataModel().getOutputFormat();
		int nulls = 0;
		main: for (int i = 0; i < testedColumns.length; i++) {
			DataColumnDefinition col = testedColumns[i];
			for (int j = 0; j < sourceColumns.length; j++) {
				if (col != null && col.equals(sourceColumns[j])) {
					continue main;
				}
			}
			testedColumns[i] = null;
			testCondition[i] = null;
			nulls++;
		}
		
		int skipped = 0;
		if (nulls != 0) {
			DataColumnDefinition[] newTestedColumns = new DataColumnDefinition[testedColumns.length - nulls];
			AbstractDistance[] newTestCondition = new AbstractDistance[testCondition.length - nulls];
			int[] newWeights = new int[weights.length - nulls];
			double[] emptyMatched = new double[emptyMatchScore.length - nulls];
			//clean arrays
			for (int i = 0; i < testedColumns.length; i++) {
				if (testedColumns[i] == null) {
					skipped++;
				} else {
					newTestedColumns[i - skipped] = testedColumns[i];
					newTestCondition[i - skipped] = testCondition[i];
					newWeights[i - skipped] = weights[i];
					emptyMatched[i - skipped] = emptyMatchScore[i];
				}
			}
			testedColumns = newTestedColumns;
			testCondition = newTestCondition; 
			weights = newWeights;
			emptyMatchScore = emptyMatched;
		}
		
	}

	public void setDeduplicatedFileName(String fileName) {
		this.dedupeFile = fileName;
	}
	
	public String getDeduplicatedFileName() {
		return dedupeFile;
	}

	public double[] getEmptyMatchScore() {
		return emptyMatchScore;
	}

	public int[] getWeights() {
		return weights;
	}

	public void setAcceptanceLevel(int acceptanceLevel) {
		this.acceptanceLevel = acceptanceLevel;
	}

	public int getAcceptanceLevel() {
		return acceptanceLevel;
	}
	
}
