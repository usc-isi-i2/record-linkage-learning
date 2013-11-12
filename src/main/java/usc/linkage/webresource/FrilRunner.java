package usc.linkage.webresource;


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
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import usc.linkage.configuration.ConfigBuilder;
import usc.linkage.configuration.JSONParser;
import usc.linkage.configuration.Linkage;

import cdc.utils.RJException;


@Path("/")
public class FrilRunner {
	
//"https://dl.dropboxusercontent.com/s/g4c31s8vpvthjm4/source2.csv?token_hash=AAEbXVY9_UUk14vpjBdf9jObZnT5Dm6Gtclkl3SlyhQLHA&amp;dl=1
//https://dl.dropboxusercontent.com/s/t9b3nnlgftb9xo3/source1.csv?token_hash=AAFgnAO0pBYoUHP2icjVBm39fuxwKFn6_gvHdJAr50p2cA&amp;dl=1

	private static final String configFileName = "config.xml";
	private static final String finalRes = "result.csv";

	@POST
	@Path("link")
	@Consumes("text/plain")
	@Produces("text/plain")
	public javax.ws.rs.core.Response link(String jsonStr) throws ParserConfigurationException, JSONException, InterruptedException, ExecutionException, IOException, TransformerException, ClassNotFoundException, RJException{
		JSONParser parser = new JSONParser(jsonStr);
		//JSONParser parser = new JSONParser(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(Files.readAllBytes(Paths.get("C:\\Study\\FRIL-v2.1.5-src\\FRIL-v2.1.5-src\\src\\json.txt")))).toString());
		/*System.out.println(parser.getLeftSourcePath());
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
		JoinSet[] joinSets = parser.getJoinSets();
		for(JoinSet joinSet : joinSets){
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
		}*/
		ConfigBuilder configBuilder1 = new ConfigBuilder();
		configBuilder1.buildLeftData("source1.csv", parser.getLeftColmNames());
		configBuilder1.buildRightData("source2.csv", parser.getRightColmNames());
		configBuilder1.buildJoin(parser.getAcceptLevel(), parser.getJoinSets());
		configBuilder1.buildSaver(finalRes);
		configBuilder1.build(configFileName);
		Linkage.link(configFileName);
		JSONObject retBody = new JSONObject();
		retBody.put("resultFile", finalRes);
		return javax.ws.rs.core.Response.ok().entity(retBody.toString()).build();
	}
	
	@GET
	@Path("data/{fileName}")
	@Produces("text/plain")
	public javax.ws.rs.core.Response getLinkageResult(@PathParam("fileName") String fileName) throws IOException{
		String data = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(Files.readAllBytes(Paths.get(fileName)))).toString();
		return javax.ws.rs.core.Response.ok().entity(data).build();
	}
	
	
}
