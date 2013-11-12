package usc.linkage.configuration;


import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.codehaus.jettison.json.JSONException;
import org.testng.annotations.Test;

import cdc.configuration.Configuration;
import cdc.impl.datasource.text.CSVDataSource;
import cdc.utils.Log;
import cdc.utils.RJException;
import cdc.configuration.ConfiguredSystem;
import cdc.datamodel.DataRow;



public class ConfigurationTest {

	@Test(description="test configuration and JSONParser")
	public void configurationTest() throws RJException, IOException, ClassNotFoundException, ParserConfigurationException, TransformerException, JSONException, InterruptedException, ExecutionException{
		String file="C:\\Study\\FRIL-v2.1.5\\FRIL-v2.1.5\\config\\a.xml";
		//Linkage.link(file);
		/*ConfigBuilder configBuilder = new ConfigBuilder();
		configBuilder.buildLeftData("C:\\Study\\FRIL-v2.1.5\\FRIL-v2.1.5\\data-sample\\generated-data - Copy.csv", new String[]{"patient_name", "DOB", "height", "weight"});
		configBuilder.buildRightData("C:\\Study\\FRIL-v2.1.5\\FRIL-v2.1.5\\data-sample\\generated-data-error - Copy.csv", new String[]{"name", "DOB", "height", "weight"});
		JoinSet joinSet1 = new JoinSet("patient_name", "name", "cdc.impl.distance.EditDistance", "40");
		joinSet1.addParam("match-level-start", "0.2");
		joinSet1.addParam("math-level-end", "0.4");

		JoinSet joinSet2 = new JoinSet("DOB", "DOB", "cdc.impl.distance.EditDistance", "30");
		joinSet2.addParam("match-level-start", "0.1");
		joinSet2.addParam("math-level-end", "0.3");
		
		JoinSet joinSet3 = new JoinSet("height", "height", "cdc.impl.distance.NumericDistance", "15");
		joinSet3.addParam("use-lineral-approximation", "true");
		joinSet3.addParam("percent-difference", "10.0,10.0");
		joinSet3.addParam("numeric-difference", "");
		
		JoinSet joinSet4 = new JoinSet("weight", "weight", "cdc.impl.distance.NumericDistance", "15");
		joinSet4.addParam("use-lineral-approximation", "true");
		joinSet4.addParam("percent-difference", "10.0,10.0");
		joinSet4.addParam("numeric-difference", "");
		
		
		configBuilder.buildJoin("71", joinSet1, joinSet2, joinSet3, joinSet4);
		configBuilder.buildSaver("results2.csv");
		configBuilder.build("C:\\Study\\FRIL-v2.1.5\\FRIL-v2.1.5\\config\\a.xml");
		//Linkage.link(file);*/
		
		
				
		JSONParser parser = new JSONParser(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(Files.readAllBytes(Paths.get("C:\\Study\\FRIL-v2.1.5-src\\FRIL-v2.1.5-src\\src\\json.txt")))).toString());
		System.out.println(parser.getLeftSourcePath());
		System.out.println(parser.getRightSourcePath());
		System.out.println(parser.getAcceptLevel());
		String[] leftNames = parser.getLeftColmNames();
		String[] rightNames = parser.getRightColmNames();
		for(String name : leftNames){
			System.out.println(name);
		}
		for(String name: rightNames){
			System.out.println(name);
		}
		AlgorithmSet[] joinSets = parser.getJoinSets();
		for(AlgorithmSet joinSet : joinSets){
			System.out.println("One Set");
			System.out.println(joinSet.getAlgorithmName());
			System.out.println(joinSet.getLeftName());
			System.out.println(joinSet.getRightName());
			System.out.println(joinSet.getWeight());
			for(String paramNames : joinSet.getParamNames()){
				System.out.println(paramNames);
			}
			for(String paramValue : joinSet.getParamValues()){
				System.out.println(paramValue);
			}
		}
		ConfigBuilder configBuilder1 = new ConfigBuilder();
		configBuilder1.buildLeftData("source1.csv", parser.getLeftColmNames());
		configBuilder1.buildRightData("source2.csv", parser.getRightColmNames());
		configBuilder1.buildJoin(parser.getAcceptLevel(), parser.getJoinSets());
		configBuilder1.buildSaver("result5.csv");
		configBuilder1.build("b.xml");
		Linkage.link("b.xml");
		
		/*System.out.println(file);
		Class claz=Class.forName("cdc.impl.datasource.text.CSVDataSource");//cdc.impl.datasource.text.CSVDataSource
		Configuration config=new Configuration(file,false);
		ConfiguredSystem system=config.getSystem();
		
		System.gc();
		
		system.getJoin().reset();

		system.getResultSaver().reset();
		int n = 0;
		DataRow row;
		while ((row = system.getJoin().joinNext()) != null) {
			n++;
			System.out.println(row.toString());
			system.getResultSaver().saveRow(row);
		}
		system.getResultSaver().flush();
		system.getResultSaver().close();
		system.getJoin().closeListeners();
		//system.getJoin().close();
		
		System.out.println(system.getJoin() + ": Algorithm produced " + n + " joined tuples. Elapsed time: " +  "ms.");
		
		*/
		
	}
}
