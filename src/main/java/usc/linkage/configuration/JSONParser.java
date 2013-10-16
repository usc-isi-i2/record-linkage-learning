package usc.linkage.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.ParserConfigurationException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

public class JSONParser {

	private ArrayList<JoinSet> joinSets = new ArrayList<JoinSet>();
	private ArrayList<String> leftColmNames = new ArrayList<String>();
	private ArrayList<String> rightColmNames = new ArrayList<String>();
	String leftSourcePath;
	String rightSourcePath;
	String acceptLevel;
	
	public JSONParser(JSONObject obj) throws ParserConfigurationException, JSONException, InterruptedException, ExecutionException, IOException{
		 leftSourcePath = obj.getString("sourcePathLeft");
		 rightSourcePath = obj.getString("sourcePathRight");
		 generateSource(leftSourcePath, "source1.csv");
		 generateSource(rightSourcePath, "source2.csv");
		 acceptLevel = obj.getString("acceptance-level");
		 JSONArray colums = obj.getJSONArray("columns");
		 for(int i = 0; i < colums.length(); i++){
			 JSONObject oneColum = colums.getJSONObject(i);
			 leftColmNames.add(oneColum.getString("columnName"));
			 rightColmNames.add(oneColum.getString("columnName"));
			 JoinSet joinSet = new JoinSet(oneColum.getString("columnName"), oneColum.getString("columnName"), "cdc.impl.distance."+oneColum.getString("algorithm"), oneColum.getString("weight"));
			 JSONObject params = oneColum.getJSONObject("params");
			 Iterator iter = params.keys();
			 while(iter.hasNext()){
				 String key = (String) iter.next();
				 joinSet.addParam(key, params.getString(key));
			 }
			 joinSets.add(joinSet);
		 }
	}
	
	public JSONParser(String stringBody) throws ParserConfigurationException, JSONException, InterruptedException, ExecutionException, IOException{
		this(new JSONObject(stringBody));
		//this(new JSONObject(new Scanner(new File(filePath)).useDelimiter("\\Z").next()));
	}
	
	public String getLeftSourcePath(){
		return this.leftSourcePath;
	}
	
	public String getRightSourcePath(){
		return this.rightSourcePath;
	}
	
	public String getAcceptLevel(){
		return this.acceptLevel;
	}
	
	public String[] getLeftColmNames(){
		return this.leftColmNames.toArray(new String[this.leftColmNames.size()]);
	}
	
	public String[] getRightColmNames(){
		return this.rightColmNames.toArray(new String[this.rightColmNames.size()]);
	}
	
	public JoinSet[] getJoinSets(){
		return this.joinSets.toArray(new JoinSet[this.joinSets.size()]);
	}
	
	private void generateSource(String url, String fileName) throws InterruptedException, ExecutionException, IOException{
		AsyncHttpClient httpClient=new AsyncHttpClient();
		Request request=httpClient.prepareGet(url).build();
		Response response=httpClient.executeRequest(request).get();
		String responseBody=response.getResponseBody();
		File file = null;
		FileOutputStream fop = null;
		try {
			file = new File(fileName);
			fop = new FileOutputStream(file);
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
			// get the content in bytes
			byte[] contentInBytes = responseBody.getBytes();
			fop.write(contentInBytes);
			fop.flush();
			fop.close();
			System.out.println(fileName + " is saved");
 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fop != null) {
					fop.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
