package usc.linkage.configuration;


import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Chen Wang
 *
 *	AlgorithmSet is used to describe one basic Join Algorithm, including the left/right column name,
 *	algorithm name, weight, and params for the algorithm.
 */


public class AlgorithmSet {
	private String leftName;
	private String rightName;
	private String algorithmName;
	private String weight;
	private HashMap<String, String> params = new HashMap<String, String>();
	
	public AlgorithmSet(String leftName, String rightName, String algorithmName){
		this.leftName = leftName;
		this.rightName = rightName;
		this.algorithmName = algorithmName;
	}
	
	public void addParam(String name, String value){
		params.put(name, value);
	}
	
	public String getLeftName(){
		return leftName;
	}
	
	public String getRightName(){
		return rightName;
	}
	
	public void setWeight(String weight){
		this.weight = weight;
	}
	
	public String getAlgorithmName(){
		return algorithmName;
	}
	
	public String getWeight(){
		return weight;
	}
	
	public String[] getParamNames(){
		String[] res = new String[params.size()];
		int index = 0;
		for(Map.Entry<String,String> entry : params.entrySet()){
			res[index++] = entry.getKey();
		}
		return res;
	}
	
	public String[] getParamValues(){
		String[] res = new String[params.size()];
		int index = 0;
		for(Map.Entry<String,String> entry : params.entrySet()){
			res[index++] = entry.getValue();
		}
		return res;
	}
	
}
