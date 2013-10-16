package usc.linkage.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JoinSet {
	private String leftName;
	private String rightName;
	private String algorithmName;
	private String weight;
	private HashMap<String, String> params = new HashMap<String, String>();
	
	public JoinSet(String leftName, String rightName, String algorithmName, String weight){
		this.leftName = leftName;
		this.rightName = rightName;
		this.algorithmName = algorithmName;
		this.weight = weight;
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
