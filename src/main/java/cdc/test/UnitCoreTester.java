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


package cdc.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import junit.framework.ComparisonFailure;
import cdc.components.AbstractDataSource;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.converters.AbstractColumnConverter;
import cdc.datamodel.converters.JoinConverter;
import cdc.datamodel.converters.ModelGenerator;
import cdc.impl.Main;
import cdc.impl.datasource.wrappers.ExternallySortingDataSource;
import cdc.utils.CompareFunctionInterface;
import cdc.utils.Log;
import cdc.utils.Props;
import cdc.utils.RJException;
import cdc.utils.comparators.StringComparator;

public class UnitCoreTester {
	
	public static void main(String[] args) throws SecurityException, IllegalArgumentException, IOException, NoSuchMethodException, ClassNotFoundException, RJException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Log.logToFile("tests/log.txt");
		Props.disablePrinting();
		try {
			System.out.println("Unit tester of core FRIL functionality.");
			System.out.println("Testing converters and data sources");
			testConvertersAndSources();
			System.out.println("Testing sorting data source");
			testSortingDS();
			System.out.println("Testing deduplication data source");
			testDeduplication();
			System.out.println("Testing joins...");
			testJoins();
		} catch (ComparisonFailure f) {
			System.out.println("...FAILED");
			System.out.println(f.getMessage());
			System.out.println("TESTING FAILED!");
			System.exit(0);
		} catch (AssertionFailedError f) {
			System.out.println("...FAILED");
			System.out.println(f.getMessage());
			System.out.println("TESTING FAILED!");
			System.exit(0);
		}
		System.out.println("All tests completed.");
	}

	private static void testDeduplication() {
		// TODO Auto-generated method stub
		
	}

	private static void testSortingDS() throws IOException, SecurityException, NoSuchMethodException, ClassNotFoundException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, RJException {
		ItemDescriptor[] sourceDescriptors = readDescriptors("tests/sources.txt");
		
		for (int i = 0; i < sourceDescriptors.length; i++) {
			System.out.print("   Testing " + sourceDescriptors[i].message);
			Object[] constrParams = new Object[] {(String)sourceDescriptors[i].properties.get("source-name"), sourceDescriptors[i].properties};
			Constructor c = Class.forName(sourceDescriptors[i].className).getConstructor(new Class[] {String.class, Map.class});	
			AbstractDataSource source = (AbstractDataSource) c.newInstance(constrParams);
			CompareFunctionInterface[] compares = new CompareFunctionInterface[source.getAvailableColumns().length];
			for (int j = 0; j < compares.length; j++) {
				compares[j] = new StringComparator();
			}
			
			ItemDescriptor[] converterDescriptors = readDescriptors("tests/converters.txt");
			ModelGenerator model = createModel(converterDescriptors, source.getAvailableColumns());
			source.setModel(model);
			
			source = new ExternallySortingDataSource(source.getSourceName(), source, model.getOutputFormat(), compares, new HashMap());
			
			Assert.assertEquals(sourceDescriptors[i].message + ": ERROR in size()", 3L, source.size());
			DataRow row;
			int n = 0;
			while ((row = source.getNextRow()) != null) {
				n++;
			}
			
			Assert.assertEquals(sourceDescriptors[i].message + ": ERROR in size() (source does not return number of rows it promised)", 3, n);
			source.reset();
			
			n = 0;
			while ((row = source.getNextRow()) != null) {
				n++;
			}
			Assert.assertEquals(sourceDescriptors[i].message + ": ERROR in reset() (source does not return number of rows it promised after reset)", 3, n);
			source.close();
			row = null;
			Exception e = null;
			try {
				n = 0;
				while ((row = source.getNextRow()) != null) {
					n++;
				}
				Assert.assertEquals(sourceDescriptors[i].message + ": ERROR in close() (source does not return size() records after closed and reopened)", 3, n);
			} catch (RJException ec) {
				e = ec;
			}
			Assert.assertTrue(sourceDescriptors[i].message + ": ERROR in close() (source should reopen after close and getNextRow were called)", row == null && e == null);
			System.out.println("...OK");
		}
	}

	private static void testJoins() throws IOException, RJException {
		ItemDescriptor[] joins = readDescriptors("tests/joins.txt");
		for (int i = 0; i < joins.length; i++) {
			System.out.print("   Testing " + joins[i].message);
			Main main = new Main((String) joins[i].properties.get("configuration"));
			int results = main.runJoin();
			Assert.assertEquals("ERROR in initial join", 3, results);
			results = main.rerun();
			Assert.assertEquals("ERROR in join after reset", 3, results);
			System.out.println(" OK");
		}
	}

	private static void testConvertersAndSources() throws IOException, SecurityException, NoSuchMethodException, ClassNotFoundException, RJException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		
		ItemDescriptor[] sourceDescriptors = readDescriptors("tests/sources.txt");
		
		for (int i = 0; i < sourceDescriptors.length; i++) {
			System.out.print("   Testing " + sourceDescriptors[i].message);
			Object[] constrParams = new Object[] {(String)sourceDescriptors[i].properties.get("source-name"), sourceDescriptors[i].properties};
			Constructor c = Class.forName(sourceDescriptors[i].className).getConstructor(new Class[] {String.class, Map.class});	
			AbstractDataSource source = (AbstractDataSource) c.newInstance(constrParams);
			
			ItemDescriptor[] converterDescriptors = readDescriptors("tests/converters.txt");
			ModelGenerator model = createModel(converterDescriptors, source.getAvailableColumns());
			source.setModel(model);
			
			Assert.assertEquals(sourceDescriptors[i].message + ": ERROR in size()", 3L, source.size());
			DataRow row;
			int n = 0;
			while ((row = source.getNextRow()) != null) {
				n++;
			}
			
			Assert.assertEquals(sourceDescriptors[i].message + ": ERROR in size() (source does not return number of rows it promised)", 3, n);
			source.reset();
			
			n = 0;
			while ((row = source.getNextRow()) != null) {
				for (int j = 0; j < converterDescriptors.length; j++) {
					Assert.assertEquals("ERROR in converter " + converterDescriptors[j].message, converterDescriptors[j].result[n], getResponse(row, model.getConverters()[j]));
				}
				n++;
			}
			Assert.assertEquals(sourceDescriptors[i].message + ": ERROR in reset() (source does not return number of rows it promised after reset)", 3, n);
			source.close();
			row = null;
			Exception e = null;
			try {
				n = 0;
				while ((row = source.getNextRow()) != null) {
					n++;
				}
				Assert.assertEquals(sourceDescriptors[i].message + ": ERROR in close() (source does not return size() records after closed and reopened)", 3, n);
			} catch (RJException ec) {
				e = ec;
			}
			Assert.assertTrue(sourceDescriptors[i].message + ": ERROR in close() (source should reopen after close and getNextRow were called)", row == null && e == null);
			System.out.println("...OK");
		}
	}
	
	private static ModelGenerator createModel(ItemDescriptor[] converterDescriptors, DataColumnDefinition[] cols) throws SecurityException, NoSuchMethodException, ClassNotFoundException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, RJException {
		AbstractColumnConverter[] convs = new AbstractColumnConverter[converterDescriptors.length];
		System.out.println("\n      Tested converters:");
		for (int i = 0; i < convs.length; i++) {
			System.out.println("         " + converterDescriptors[i].message);
		}
		System.out.print("   Test starts");
		for (int i = 0; i < convs.length; i++) {
			if (converterDescriptors[i].className.endsWith("JoinConverter")) {
				String name = (String) converterDescriptors[i].basicProperties.get("name");
				DataColumnDefinition columns[] = readColumns((String)converterDescriptors[i].basicProperties.get("column"), cols);
				convs[i] = new JoinConverter(name, columns, converterDescriptors[i].properties);
			} else {
				Constructor c = Class.forName(converterDescriptors[i].className).getConstructor(new Class[] {String.class, Map.class, DataColumnDefinition.class});
				String name = (String) converterDescriptors[i].basicProperties.get("name");
				DataColumnDefinition col = search((String) converterDescriptors[i].basicProperties.get("column"), cols);
				convs[i] = (AbstractColumnConverter) c.newInstance(new Object[] {name, converterDescriptors[i].properties, col});
			}
		}
		return new ModelGenerator(convs);
	}

	private static DataColumnDefinition[] readColumns(String columnNames, DataColumnDefinition[] cols) {
		String[] columnNamesArray = columnNames.split(",");
		DataColumnDefinition[] columns = new DataColumnDefinition[columnNamesArray.length];
		for (int i = 0; i < columns.length; i++) {
			columns[i] = search(columnNamesArray[i], cols);		
		}
		return columns;
	}

	private static DataColumnDefinition search(String string, DataColumnDefinition[] cols) {
		for (int i = 0; i < cols.length; i++) {
			if (cols[i].getColumnName().equals(string)) {
				return cols[i];
			}
		}
		return null;
	}

	private static String getResponse(DataRow row, AbstractColumnConverter conv) {
		StringBuffer response = new StringBuffer();
		for (int i = 0; i < conv.getOutputColumns().length; i++) {
			if (i > 0) {
				response.append(".");
			}
			response.append(row.getData(conv.getOutputColumns()[i]).getValue().toString());
		}
		return response.toString();
	}

	private static ItemDescriptor[] readDescriptors(String string) throws IOException {
		BufferedReader sources = new BufferedReader(new FileReader(string));
		String srcStr;
		List desc = new ArrayList();
		while ((srcStr = sources.readLine()) != null && !srcStr.trim().equals("")) {
			//System.out.println("Line: " + srcStr);
			ItemDescriptor descriptor = new ItemDescriptor(srcStr);
			desc.add(descriptor);
		}
		return (ItemDescriptor[])desc.toArray(new ItemDescriptor[] {});
	}

	private static class ItemDescriptor {
		
		String className;
		String message;
		Map properties;
		Map basicProperties;
		String[] result;
		
		public ItemDescriptor(String line) {
			Map props = parse(line);
			className = (String) props.get("class");
			message = (String) props.get("message");
			properties = parse((String)props.get("properties"));
			basicProperties = parse((String)props.get("basicProperties"));
			if (props.get("result") != null) {
				result = ((String)props.get("result")).split(",");
			}
		}
		
		private Map parse(String line) {
			if (line == null) {
				return null;
			}
			Map props = new HashMap();
			Pattern p = Pattern.compile("[a-zA-Z_0-9\\-]*=(\\[[^\\]]*\\]|\"[^\"]*\")");
			Matcher m = p.matcher(line);
			while (m.find()) {
				String match = m.group();
				int equals = match.indexOf('=');
				String name = match.substring(0, equals);
				props.put(name, match.substring(equals + 2, match.length() - 1));
			}
			return props;
		}
		
	}
	
}
