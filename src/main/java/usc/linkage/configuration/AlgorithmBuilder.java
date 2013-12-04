package usc.linkage.configuration;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;


/**
 * 
 * @author Chen Wang
 * 
 * AlgorithmBuilder is a builder for AlgorithmSet Class.
 *
 */

public class AlgorithmBuilder {
	private static Properties prop = new Properties();
	private String[] params;
	
	static{
		try {
			prop.load(AlgorithmBuilder.class.getClassLoader().getResourceAsStream("defaultConfig.properties"));
		} catch (IOException e) {
			throw new RuntimeException("Can not load defaultConfig.properties file!", e);
		}
	}
	
	public AlgorithmBuilder(String[] params){
		this.params = params;
	}
	
	/**
	 * 
	 * @param colums
	 * @return LinkedList<String>
	 * @throws JSONException
	 * 
	 * This method is used to create the default value for weight of each algorithm.
	 */
	public static LinkedList<String> getDefaultWeight(JSONArray colums) throws JSONException{
		int total = 0;
		int numOfProvided = 0;
		for(int i = 0; i < colums.length(); i++){
			JSONObject oneColum = colums.getJSONObject(i);
			String localWeight = oneColum.getString("weight");
			localWeight = localWeight.replaceAll("\\s", "");
			if(localWeight != null && !localWeight.equals("")){
				numOfProvided++;
				total += Integer.parseInt(localWeight);
			}
		}
		if(numOfProvided == colums.length()){
			return null;
		}
		LinkedList<String> weightList = new LinkedList<String>();
		int numOfEmpty = colums.length() - numOfProvided;
		int ave = (100 - total)/numOfEmpty;
		for(int i = 0; i < colums.length() - numOfProvided; i++){
			if(i == colums.length() - numOfProvided - 1){
				weightList.add(String.valueOf(ave + (100 - total) - ave * numOfEmpty));
			}else{
				weightList.add(String.valueOf(ave));
			}
		}
		return weightList;
	}
	
	//public abstract AlgorithmSet buildAlgorithmSet(JSONObject jsonObj) throws JSONException;
	public AlgorithmSet buildAlgorithmSet(JSONObject oneColum, LinkedList<String> weightPool) throws JSONException{
		AlgorithmSet joinSet = new AlgorithmSet(oneColum.getString("columnName"), oneColum.getString("columnName"), "cdc.impl.distance." + oneColum.getString("algorithm"));
		String weight = oneColum.getString("weight");
		weight = weight.replaceAll("\\s", "");
		if(weight == null || weight.equals("")){
			weight = weightPool.getFirst();
			weightPool.removeFirst();
		}
		joinSet.setWeight(weight);
		JSONObject paramsJson = oneColum.getJSONObject("params");
		for(int i = 0; i < params.length; i++){
			String value = paramsJson.getString(params[i]);
			value = value.replaceAll("\\s", "");
			if(value == null || value.equals("")){
				value = prop.getProperty(params[i]);
			}
			joinSet.addParam(params[i], value);
		}
		return joinSet;
	}
}
