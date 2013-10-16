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


package cdc.impl.datasource.jdbc;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import cdc.components.AbstractDataSource;
import cdc.datamodel.DataCell;
import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.datamodel.converters.ModelGenerator;
import cdc.gui.GUIVisibleComponent;
import cdc.utils.HTMLUtils;
import cdc.utils.Log;
import cdc.utils.RJException;
import cdc.utils.Utils;

public class JDBCDataSource extends AbstractDataSource {

	private static final int logLevel = Log.getLogLevel(JDBCDataSource.class);
	
	public static class JDBCConnectionConfig {
		private String url;
		private Properties connectionProperties;
		private String driver;
		public JDBCConnectionConfig(String driver, String url, Properties connectionProperties) {
			this.url = url;
			this.connectionProperties = connectionProperties;
			this.driver = driver;
		}
		
		public static JDBCConnectionConfig fromParams(Map params) throws RJException {
			/*
				String user = ensureField(params, PARAM_USER, false);
				String pass = ensureField(params, PARAM_PASSWORD, false);
			*/
			String url = Utils.getParam(params, PARAM_URL, true);
			String driver = Utils.getParam(params, PARAM_DRIVER, true);
			
			Properties props = new Properties();
			for (Iterator iterator = params.keySet().iterator(); iterator.hasNext();) {
				String name = (String) iterator.next();
				String value = (String) params.get(name);
				if (value != null) {
					props.setProperty(name, value);
				}
			}
			
			//if no user defined, try to load windows auth module
			if (!props.contains(PARAM_USER)) {
				synchronized (mutex) {
					if (!libLoaded) {
						File file = new File(".");
						try {
							System.load(file.getAbsolutePath() + File.separator + "ntlmauth.dll");
						} catch (UnsatisfiedLinkError e) {
							System.out.println("[WARN] It appears that ntlmauth.dll could not be loaded.");
							System.out.println("[WARN]    Either you are using non-Windows operating system, or the file is");
							System.out.println("[WARN]    not located in FRIL home directory. See below for error details:");
							System.out.println("[WARN]    " + e.getMessage());
						}
						libLoaded = true;
					}
				}
			}
			
			return new JDBCConnectionConfig(driver, url, props);
		}
		
	}

	public static final String PARAM_TEST_SELECT = "columns-select";
	public static final String PARAM_URL = "url";
	public static final String PARAM_USER = "user";
	public static final String PARAM_DRIVER = "driver";
	public static final String PARAM_TABLE = "table";
	public static final String PARAM_PASSWORD = "password";
	
	private static final Object mutex = new Object();
	private static boolean libLoaded = false;
	
	private boolean closed = false;
	private boolean connected = false;
	
	private Connection jdbcConnection = null;
	private JDBCConnectionConfig connectionConfig = null;
	
	private PreparedStatement activeStatement = null;
	private ResultSet activeResultSet = null;
	
	private String tableName;
	private String columns;
	private String orderBy = "";
	private String where = " ";
	private int size = -1;
	
	public JDBCDataSource(String name, Map params) throws RJException {
		super(name, readSchema(name, JDBCConnectionConfig.fromParams(params), Utils.getParam(params, PARAM_TEST_SELECT, true)), params);
		this.connectionConfig = JDBCConnectionConfig.fromParams(params);
		this.tableName = Utils.getParam(params, PARAM_TABLE, true);
		checkSize();
	}

	public JDBCDataSource(String name, DataColumnDefinition[] model, Map params) throws RJException {
		super(name, model, params);
		this.connectionConfig = JDBCConnectionConfig.fromParams(params);
		this.tableName = Utils.getParam(params, PARAM_TABLE, true);
		checkSize();
	}
	
	private void checkSize() throws RJException {
		ensureConnection();
		String sizeQuery = "select count(*) from " + tableName + where;
		PreparedStatement stmt;
		try {
			stmt = jdbcConnection.prepareStatement(sizeQuery);
			ResultSet cnt = stmt.executeQuery();
			if (!cnt.next()) {
				throw new RJException("Internal error. This should not happen.");
			}
			size  = cnt.getInt(1);
			Log.log(getClass(), getSourceName() + ": Number of records in data source: " + size, 1);
		} catch (SQLException e) {
			throw new RJException("Error when estimating size.", e);
		}
	}

	private static void log(String log) {
		log(log, 1);
	}
	
	private static void log(String log, int level) {
		Log.log(JDBCDataSource.class, log, level);
	}
	
	private void executeQuery() throws RJException {
		if (connected) {
			return;
		}
		if (closed) {
			throw new RJException("Cannot operate on closed data source!");
		}
		
		String[] ops = prepareColumns(getDataModel());
		this.columns = ops[0];
		//this.where = ops[1];
		
		ensureConnection();
		
		try {
			String query = "select " + columns + " from " + tableName + where  + orderBy;
			log("Query execute: " + query);
			activeStatement = jdbcConnection.prepareStatement(query);
			activeResultSet = activeStatement.executeQuery();
			log("Query was executed");
		} catch (SQLException e) {
			throw new RJException("Error operating on database", e);
		}
		connected = true;
	}
	
	private void ensureConnection() throws RJException {
		if (this.jdbcConnection == null) {
			try {
				//Utils.loadJDBCDriverClass(connectionConfig.driver);
				Class.forName(connectionConfig.driver);
			} catch (ClassNotFoundException e) {
				throw new RJException("Could not load driver class: " + connectionConfig.driver + ". Make sure the name is correct and that correct library is in classpath.");
			}
			try {
				jdbcConnection = DriverManager.getConnection(connectionConfig.url, connectionConfig.connectionProperties);
			} catch (SQLException e) {
				throw new RJException("Error connecting to database. Make sure the connection configuration is correct", e);
			}
		}
	}

	private static String[] prepareColumns(ModelGenerator generator) {
		StringBuffer columns = new StringBuffer();
		StringBuffer where = new StringBuffer(" where ");
		for (int i = 0; i < generator.getInputFormat().length; i++) {
			if (i > 0) {
				columns.append(", ");
				where.append(" and ");
			}
			columns.append(((JDBCDataColumnDefinition)generator.getInputFormat()[i]).getSqlColumnName());
			where.append(((JDBCDataColumnDefinition)generator.getInputFormat()[i]).getSqlColumnName());
			where.append(" is not null");
		}
		return new String[] {columns.toString(), where.toString()};
	}
	
	public boolean canSort() {
		return false;
	}

	protected void doClose() throws IOException, RJException {
		log("Closing data source");
		if (closed) {
			return;
		}
		if (activeResultSet != null) {
			try {
				activeStatement.cancel();
				//activeResultSet.close();
				activeStatement.close();
			} catch (SQLException e) {
				System.out.println("The error below is likely a warning only and is logged for information purposes. Ignore it.");
				e.printStackTrace();
			}
		}
		if (this.jdbcConnection != null) {
			try {
				jdbcConnection.close();
				jdbcConnection = null;
			} catch (SQLException e) {
				System.out.println("The error below is likely a warning only and is logged for information purposes. Ignore it.");
				e.printStackTrace();
			}
		}
		connected = false;
		closed = true;
	}

	public DataRow nextRow() throws IOException, RJException {
		log("Getting next row from data set", 2);
		executeQuery();
		try {
			if (activeResultSet.next()) {
				DataColumnDefinition[] model = getDataModel().getInputFormat();
				DataCell[] cells = new DataCell[model.length];
				for (int i = 0; i < model.length; i++) {
					JDBCDataColumnDefinition column = (JDBCDataColumnDefinition) model[i];
					Object d = activeResultSet.getObject(column.getSqlColumnName());
					if (d == null) {
						d = "";
					}
					cells[i] = new DataCell(column.getColumnType(), d.toString().trim());
				}
				DataRow row = new DataRow(getDataModel().getOutputFormat(), getDataModel().generateOutputRow(cells), getSourceName());
				if (logLevel >= 2) {
					log("Row retrieved: " + row, 2);
				}	
				return row;
			}
		} catch (SQLException e) {
			throw new RJException("Error reading response from database", e);
		}
		log("No more data in data source");
		return null;
	}

//	public DataRow[] getNextRows(int size) throws IOException, RJException {
//		List rows = new ArrayList();
//		DataRow row = null;
//		while (rows.size() != size && (row = getNextRow()) != null) {
//			rows.add(row);
//		}
//		return (DataRow[]) rows.toArray(new DataRow[] {});
//	}

	protected void doReset() throws IOException, RJException {
		log("Resetting data source", 2);
		//System.out.println("Reset in source inside JDBC....");
		closed = false;
		if (activeResultSet != null) {
			//System.out.println("Result set.close() in " + activeResultSet.getClass());
			try {
				activeStatement.cancel();
			} catch (SQLException e ) {
				log("Ignore this errror (" + e.getMessage() + ").");
			}
			//activeResultSet.close();
			//System.out.println("Active statement.close()");
			try {
				activeStatement.close();
			} catch (SQLException e ) {
				log("Ignore this errror (" + e.getMessage() + ")");
			}
			activeResultSet = null;
			activeStatement = null;
			connected = false;
		}
		//System.out.println("CONNECTION?....");
		executeQuery();
		//System.out.println("DONE....");
	}

	public static DataColumnDefinition[] readSchema(String name, JDBCConnectionConfig config, String testSelect) throws RJException {
		log("Reading schema...");
		try {
			//Utils.loadJDBCDriverClass(config.driver);
			Class.forName(config.driver);
		} catch (ClassNotFoundException e) {
			throw new RJException("Could not load driver class: " + config.driver + ". Make sure the name is correct and that correct library is in classpath.");
		}
		log("Driver loaded");
		try {
			Connection connection = DriverManager.getConnection(
					config.url, config.connectionProperties);
			PreparedStatement statement = connection.prepareStatement(testSelect);
			log("Statement created");
			statement.execute();
			List schema = new ArrayList();
			ResultSetMetaData metadata = statement.getMetaData();
			log("Metadata retrieved");
			int columns = metadata.getColumnCount();
			for (int i = 1; i <= columns; i++) {
				String columnName = metadata.getColumnName(i);
				int columnType = resolveColumnType(metadata.getColumnType(i));
				if (columnType < 0) {
					continue;
				}
				schema.add(new JDBCDataColumnDefinition(columnName, columnType, name, columnName));
			}
			log("Schema read completed");	
			return (DataColumnDefinition[]) schema.toArray(new DataColumnDefinition[] {});
		} catch (SQLException e) {
			throw new RJException("Error connecting to database. Make sure the connection configuration is correct", e);
		}
	}

	private static int resolveColumnType(int columnType) {
		/*switch (columnType) {
		case Types.VARCHAR:
			return DataColumnDefinition.TYPE_STRING;
		case Types.CHAR:
			return DataColumnDefinition.TYPE_STRING;
		case Types.LONGVARCHAR:
			return DataColumnDefinition.TYPE_NUMERIC;
		case Types.DATE:
			return DataColumnDefinition.TYPE_DATE;
		case Types.TIME:
			return DataColumnDefinition.TYPE_DATE;
		case Types.TIMESTAMP:
			return DataColumnDefinition.TYPE_DATE;
		case Types.BIGINT:
			return DataColumnDefinition.TYPE_NUMERIC;
		case Types.BIT:
			return DataColumnDefinition.TYPE_NUMERIC;
		case Types.BOOLEAN:
			return DataColumnDefinition.TYPE_NUMERIC;
		case Types.DECIMAL:
			return DataColumnDefinition.TYPE_NUMERIC;
		case Types.DOUBLE:
			return DataColumnDefinition.TYPE_NUMERIC;
		case Types.FLOAT:
			return DataColumnDefinition.TYPE_NUMERIC;
		case Types.INTEGER:
			return DataColumnDefinition.TYPE_NUMERIC;
		case Types.NUMERIC:
			return DataColumnDefinition.TYPE_NUMERIC;
		case Types.REAL:
			return DataColumnDefinition.TYPE_NUMERIC;
		case Types.SMALLINT:
			return DataColumnDefinition.TYPE_NUMERIC;
		case Types.TINYINT:
			return DataColumnDefinition.TYPE_NUMERIC;
		default:
			return -1; 
		}*/
		return DataColumnDefinition.TYPE_STRING;
	}

	public static GUIVisibleComponent getGUIVisibleComponent() {
		return new JDBCConfigurationPanel();
	}
	
	protected void finalize() throws Throwable {
		System.out.println(getClass() + " finalize");
		close();
	}
	
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof JDBCDataSource)) return false;
		JDBCDataSource that = (JDBCDataSource)arg0;
		return areTheSameProperties(this, that);
	}

	public AbstractDataSource copy() throws IOException, RJException {
		JDBCDataSource dataSource = new JDBCDataSource(getSourceName(), getProperties());
		dataSource.setModel(getDataModel());
		dataSource.setFilter(getFilter());
		return dataSource;
	}

	public String toHTMLString() {
		StringBuilder b = new StringBuilder();
		b.append(HTMLUtils.getHTMLHeader());
		b.append(HTMLUtils.encodeTable(new String[][] {{"Source name:", getSourceName()}, 
				{"Source type: ", "JDBC data source"},
				{"Covvention address:", connectionConfig.url},
				{"JDBC driver:", connectionConfig.driver}}));
		b.append(HTMLUtils.encodeSourceDataModel(getDataModel()));
		b.append("</html>");
		return b.toString();
	}

	public long size() {
		return size;
	}
}
