package usc.linkage.configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class AlgorithmBuilderFactory {
	private static final HashMap<String, String[]> map = new HashMap<String, String[]>();
	
	static{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(AlgorithmBuilderFactory.class.getClassLoader().getResourceAsStream("algorithmParams.properties"));
			NodeList nList = doc.getElementsByTagName("algorithm");
			for(int i = 0; i < nList.getLength(); i++){
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					String algorithmName = eElement.getElementsByTagName("name").item(0).getTextContent();
					ArrayList<String> paramsArr = new ArrayList<String>();
					NodeList params = eElement.getElementsByTagName("param");
					for(int j = 0; j < params.getLength(); j++){
						Node param = params.item(j);
						if (nNode.getNodeType() == Node.ELEMENT_NODE) {
							paramsArr.add(param.getTextContent());
						}
					}
					map.put(algorithmName, paramsArr.toArray(new String[paramsArr.size()]));
				}
			}
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Fail to create DocumentBuilder!", e);
		} catch (SAXException e) {
			throw new RuntimeException("Fail to load algorithmParams.properties!", e);
		} catch (IOException e) {
			throw new RuntimeException("Fail to parse algorithmParams.properties!", e);
		}
	}
	
	public static AlgorithmBuilder getAlgorithmBuilder(String algorithmName){
		String[] params = map.get(algorithmName);
		if(params == null){
			new RuntimeException("Does not find the algorithm params configuration!");
		}
		return new AlgorithmBuilder(params);
	}
}
