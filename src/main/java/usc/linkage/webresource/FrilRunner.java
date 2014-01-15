package usc.linkage.webresource;


import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import usc.linkage.configuration.ConfigBuilder;
import usc.linkage.configuration.JSONParser;
import usc.linkage.configuration.Linkage;
import usc.linkage.uniqueIdGenerator.UniqueIDGenerator;

import cdc.utils.RJException;


@Path("/")
public class FrilRunner {
	
//"https://dl.dropboxusercontent.com/s/g4c31s8vpvthjm4/source2.csv?token_hash=AAEbXVY9_UUk14vpjBdf9jObZnT5Dm6Gtclkl3SlyhQLHA&amp;dl=1
//https://dl.dropboxusercontent.com/s/t9b3nnlgftb9xo3/source1.csv?token_hash=AAFgnAO0pBYoUHP2icjVBm39fuxwKFn6_gvHdJAr50p2cA&amp;dl=1

	public static final String CONFIG_DIR = "FrilConfig";
	public static final String RESULT_DIR = "FrilResults";
	public static final String UTILITY_DIR = "FrilUtility";
	public static final String CSV_EXTENSION = ".csv";
	public static final String XML_EXTENSION = ".xml";
	private static Logger log = Logger.getLogger(FrilRunner.class);
	
	static{
		if(!new File(RESULT_DIR).exists()){
			new File(RESULT_DIR).mkdirs();
		}
		if(!new File(UTILITY_DIR).exists()){
			new File(UTILITY_DIR).mkdirs();
		}
		if(!new File(CONFIG_DIR).exists()){
			new File(CONFIG_DIR).mkdirs();
		}
	}

	@POST
	@Path("link")
	@Consumes("text/plain")
	@Produces("text/plain")
	public javax.ws.rs.core.Response link(String jsonStr) throws ParserConfigurationException, JSONException, InterruptedException, ExecutionException, IOException, TransformerException, ClassNotFoundException, RJException{
		String uid = UniqueIDGenerator.getUniqueID();
		String finalRes = new StringBuilder().append("result_").append(uid).append(CSV_EXTENSION).toString();
		String leftSourceName = new StringBuilder().append("SourceA_").append(uid).toString();
		String rightSourceName = new StringBuilder().append("SourceB_").append(uid).toString();
		String leftSouceFilePath = new StringBuilder().append(UTILITY_DIR).append("/").append(leftSourceName).append(CSV_EXTENSION).toString();
		String rightSourceFilePath = new StringBuilder().append(UTILITY_DIR).append("/").append(rightSourceName).append(CSV_EXTENSION).toString();
		String configFilePath = new StringBuilder().append(CONFIG_DIR).append("/config_").append(uid).append(XML_EXTENSION).toString();
		JSONParser parser = new JSONParser(jsonStr, leftSouceFilePath, rightSourceFilePath);
		ConfigBuilder configBuilder1 = new ConfigBuilder(leftSourceName, rightSourceName);
		configBuilder1.buildLeftData(leftSouceFilePath, parser.getLeftColmNames());
		configBuilder1.buildRightData(rightSourceFilePath, parser.getRightColmNames());
		configBuilder1.buildJoin(parser.getAcceptLevel(), parser.getJoinSets());
		configBuilder1.buildSaver(new StringBuilder().append(RESULT_DIR).append("/").append(finalRes).toString());
		configBuilder1.build(configFilePath);
		Linkage.link(configFilePath);
		JSONObject retBody = new JSONObject();
		retBody.put("resultFile", finalRes);
		removeMinusFiles(uid);//This method is not efficient. Later we can figure out how to configure the paths of the minus files, then we can remove this method.
		return javax.ws.rs.core.Response.ok().entity(retBody.toString()).build();
	}
	
	@GET
	@Path("data/{fileName}")
	@Produces("text/plain")
	public javax.ws.rs.core.Response getLinkageResult(@PathParam("fileName") String fileName) throws IOException{
		String data = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(Files.readAllBytes(Paths.get(new StringBuilder().append(RESULT_DIR).append("/").append(fileName).toString())))).toString();
		return javax.ws.rs.core.Response.ok().entity(data).build();
	}
	
	/**
	 * 
	 * @param uid
	 * 
	 * This method is not efficient. Later we can figure out how to configure the paths of the minus files, then we can remove this method.
	 * 
	 */
	private void removeMinusFiles(String uid){
		String leftMinusFileName = new StringBuilder().append("minus-SourceA_").append(uid).append(CSV_EXTENSION).toString();
		String rightMinusFileName = new StringBuilder().append("minus-SourceB_").append(uid).append(CSV_EXTENSION).toString();
		try{
    		File leftMinusFile = new File(leftMinusFileName);
    		if(leftMinusFile.delete()){
    			if(log.isInfoEnabled()){
    				log.info("Minus file for Source A is deleted");
    			}
    		}else{
    			if(log.isInfoEnabled()){
    				log.warn("Minus file for Source A can not be deleted");
    			}
    		}
    	}catch(Exception e){
    		if(log.isInfoEnabled()){
				log.warn(leftMinusFileName + " does not exist!");
			}
    	}
		try{
    		File rightMinusFile = new File(rightMinusFileName);
    		if(rightMinusFile.delete()){
    			if(log.isInfoEnabled()){
    				log.info("Minus file for Source B is deleted");
    			}
    		}else{
    			if(log.isInfoEnabled()){
    				log.warn("Minus file for Source B can not be deleted");
    			}
    		}
    	}catch(Exception e){
    		if(log.isInfoEnabled()){
				log.warn(leftMinusFileName + " does not exist!");
			}
    	}
	}
	
}
