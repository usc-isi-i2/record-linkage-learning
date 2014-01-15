package usc.linkage.configuration;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

import usc.linkage.uniqueIdGenerator.UniqueIDGenerator;
import usc.linkage.webresource.FrilRunner;

import cdc.configuration.Configuration;
import cdc.impl.datasource.text.CSVDataSource;
import cdc.utils.Log;
import cdc.utils.RJException;
import cdc.configuration.ConfiguredSystem;
import cdc.datamodel.DataRow;



public class ConfigurationTest {

	@Test(description="test configuration and JSONParser")
	public void configurationTest() throws RJException, IOException, ClassNotFoundException, ParserConfigurationException, TransformerException, JSONException, InterruptedException, ExecutionException{

		BufferedReader br = null;
		StringBuilder strBuilder = new StringBuilder();
		String line;
		try {
			br = new BufferedReader(new InputStreamReader(AlgorithmBuilder.class.getClassLoader().getResourceAsStream("json.text")));
			while ((line = br.readLine()) != null) {
				strBuilder.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		String uid = UniqueIDGenerator.getUniqueID();
		String finalRes = new StringBuilder().append("test_result_").append(uid).append(FrilRunner.CSV_EXTENSION).toString();
		String leftSourceName = new StringBuilder().append("test_SourceA_").append(uid).toString();
		String rightSourceName = new StringBuilder().append("test_SourceB_").append(uid).toString();
		String leftSourceFilePath = new StringBuilder().append(FrilRunner.UTILITY_DIR).append("/").append(leftSourceName).append(FrilRunner.CSV_EXTENSION).toString();
		String rightSourceFilePath = new StringBuilder().append(FrilRunner.UTILITY_DIR).append("/").append(rightSourceName).append(FrilRunner.CSV_EXTENSION).toString();
		String configFilePath = new StringBuilder().append(FrilRunner.CONFIG_DIR).append("/test_config_").append(uid).append(FrilRunner.XML_EXTENSION).toString();
		
		JSONParser parser = new JSONParser(strBuilder.toString(), leftSourceFilePath, rightSourceFilePath);
		//JSONParser parser = new JSONParser(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(Files.readAllBytes(Paths.get("C:\\Study\\FRIL-v2.1.5-src\\FRIL-v2.1.5-src\\src\\json.txt")))).toString());
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
		ConfigBuilder configBuilder1 = new ConfigBuilder(leftSourceName, rightSourceName);
		configBuilder1.buildLeftData(leftSourceFilePath, parser.getLeftColmNames());
		configBuilder1.buildRightData(rightSourceFilePath, parser.getRightColmNames());
		configBuilder1.buildJoin(parser.getAcceptLevel(), parser.getJoinSets());
		configBuilder1.buildSaver(new StringBuilder().append(FrilRunner.RESULT_DIR).append("/").append(finalRes).toString());
		configBuilder1.build(configFilePath);
		Linkage.link(configFilePath);
		
	}
}
