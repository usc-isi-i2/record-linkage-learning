package usc.linkage.configuration;

import java.io.IOException;

import cdc.configuration.Configuration;
import cdc.configuration.ConfiguredSystem;
import cdc.datamodel.DataRow;
import cdc.utils.RJException;
import org.apache.log4j.Logger;


public class Linkage {
	
	private static Logger log = Logger.getLogger(Linkage.class);
	
	
	public static void link(String configPath) throws ClassNotFoundException, RJException, IOException{
		if(log.isInfoEnabled()){
			log.info("Linkage starts");
		}
		Configuration config=new Configuration(configPath,false);
		ConfiguredSystem system=config.getSystem();
		System.gc();
		system.getJoin().reset();
		system.getResultSaver().reset();
		int n = 0;
		DataRow row;
		while ((row = system.getJoin().joinNext()) != null) {
			n++;
			//System.out.println(row.toString());
			system.getResultSaver().saveRow(row);
		}
		system.getResultSaver().flush();
		system.getResultSaver().close();
		system.getJoin().closeListeners();
		//system.getJoin().close();
		if(log.isInfoEnabled()){
			log.info("Linkage finishes: " + system.getJoin() + ": Algorithm produced " + n + " joined tuples.");
		}
	}
}
