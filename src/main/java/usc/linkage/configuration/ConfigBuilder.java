package usc.linkage.configuration;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
 
import org.apache.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 
 * @author Chen Wang
 * 
 * ConfigBuilder Class is used to create the configuration file. Usually, we firstly use JSONParser to
 * parse the JSON text, then use ConfigBuilder and data stored in the JSONParser object to create the
 * configuration file.
 */

public class ConfigBuilder {
	
	private Document doc;
	private Element rootElement;
	private static Logger log = Logger.getLogger(usc.linkage.configuration.ConfigBuilder.class);
	private String leftSourceName;
	private String rightSourceName;
	
	
	public ConfigBuilder(String leftSourceName, String rightSourceName) throws ParserConfigurationException{
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		// root elements
		doc = docBuilder.newDocument();
		rootElement = doc.createElement("configuration");
		doc.appendChild(rootElement);
		this.leftSourceName = leftSourceName;
		this.rightSourceName = rightSourceName;
	}
	
	
	public void buildLeftData(String sourcePath, String[] colmNames){
		Element leftData = doc.createElement("left-data-source");
		Attr attrClass = doc.createAttribute("class");
		attrClass.setValue("cdc.impl.datasource.text.CSVDataSource");
		leftData.setAttributeNode(attrClass);
		Attr sourceName = doc.createAttribute("name");
		sourceName.setValue(leftSourceName);
		leftData.setAttributeNode(sourceName);
		rootElement.appendChild(leftData);
		leftData.appendChild(createParams(new String[]{"column-separator", "source-name", "input-file"}, new String[]{",", leftSourceName, sourcePath}));
		Element rowModel = doc.createElement("row-model");
		leftData.appendChild(rowModel);
		for(String name : colmNames){
			rowModel.appendChild(createSingleColm(name));
		}
	}
	
	public void buildRightData(String sourcePath, String[] colmNames){
		Element leftData = doc.createElement("right-data-source");
		Attr attrClass = doc.createAttribute("class");
		attrClass.setValue("cdc.impl.datasource.text.CSVDataSource");
		leftData.setAttributeNode(attrClass);
		Attr sourceName = doc.createAttribute("name");
		sourceName.setValue(rightSourceName);
		leftData.setAttributeNode(sourceName);
		rootElement.appendChild(leftData);
		leftData.appendChild(createParams(new String[]{"column-separator", "source-name", "input-file"}, new String[]{",", rightSourceName, sourcePath}));
		Element rowModel = doc.createElement("row-model");
		leftData.appendChild(rowModel);
		for(String name : colmNames){
			rowModel.appendChild(createSingleColm(name));
		}
	}
	
	
	private Element createSingleColm(String colmName){
		Element colm = doc.createElement("column");
		Attr attrColm = doc.createAttribute("column");
		attrColm.setValue(colmName);
		colm.setAttributeNode(attrColm);
		Attr attrConverter = doc.createAttribute("converter");
		attrConverter.setValue("cdc.datamodel.converters.DummyConverter");
		colm.setAttributeNode(attrConverter);
		Attr attrName = doc.createAttribute("name");
		attrName.setValue(colmName);
		colm.setAttributeNode(attrName);
		Element emptyVal = doc.createElement("empty-values");
		colm.appendChild(emptyVal);
		return colm;
	}
	
	private Element createParams(String[] names, String[] values){
		Element params = doc.createElement("params");
		for(int i = 0; i < names.length; i++){
			Element param = doc.createElement("param");
			Attr attrName = doc.createAttribute("name");
			attrName.setValue(names[i]);
			param.setAttributeNode(attrName);
			Attr attrValue = doc.createAttribute("value");
			attrValue.setValue(values[i]);
			param.setAttributeNode(attrValue);
			params.appendChild(param);
		}
		return params;
	}
	
	private Element createRowModel(AlgorithmSet[] sets){
		Element rowModel = doc.createElement("row-model");
		for(AlgorithmSet set : sets){
			Element column = doc.createElement("column");
			rowModel.appendChild(column);
			Attr attrName = doc.createAttribute("name");
			attrName.setValue(set.getLeftName());
			column.setAttributeNode(attrName);
			Attr attrSource = doc.createAttribute("source");
			attrSource.setValue(leftSourceName);
			column.setAttributeNode(attrSource);
		}
		for(AlgorithmSet set : sets){
			Element column = doc.createElement("column");
			rowModel.appendChild(column);
			Attr attrName = doc.createAttribute("name");
			attrName.setValue(set.getRightName());
			column.setAttributeNode(attrName);
			Attr attrSource = doc.createAttribute("source");
			attrSource.setValue(rightSourceName);
			column.setAttributeNode(attrSource);
		}
		return rowModel;
	}
	
	public void build(String path) throws TransformerException{
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(path));
		transformer.transform(source, result);
		if(log.isInfoEnabled()){
			log.info("Configuration file is saved!");
		}
	}
	
	public void buildJoin(String acceptanceLevel, AlgorithmSet... joinSets){
		Element join = doc.createElement("join");
		rootElement.appendChild(join);
		Attr attrClass = doc.createAttribute("class");
		attrClass.setValue("cdc.impl.join.snm.SNMJoin_v1");
		join.setAttributeNode(attrClass);
		Attr attrLeft = doc.createAttribute("summary-left");
		attrLeft.setValue("true");
		join.setAttributeNode(attrLeft);
		Attr attrRight = doc.createAttribute("summary-right");
		attrRight.setValue("true");
		join.setAttributeNode(attrRight);
		join.appendChild(createParams(new String[]{"window", "sort-order-right", "sort-order-left"}, new String[]{"8", getLeftNames(joinSets), getRightNames(joinSets)}));
		Element joinCondition = doc.createElement("join-condition");
		Attr joinClass = doc.createAttribute("class");
		joinClass.setValue("cdc.impl.conditions.WeightedJoinCondition");
		joinCondition.setAttributeNode(joinClass);
		join.appendChild(joinCondition);
		Element params = doc.createElement("params");
		joinCondition.appendChild(params);
		Element param = doc.createElement("param");
		params.appendChild(param);
		Attr attrParaName = doc.createAttribute("name");
		attrParaName.setValue("acceptance-level");
		param.setAttributeNode(attrParaName);
		Attr attrParaValue = doc.createAttribute("value");
		attrParaValue.setValue(acceptanceLevel);
		param.setAttributeNode(attrParaValue);
		for(AlgorithmSet set : joinSets){
			joinCondition.appendChild(conditionGenerator(set));
		}
		join.appendChild(createRowModel(joinSets));
	}
	
	private String getLeftNames(AlgorithmSet[] joinSets){
		StringBuilder strBuilder = new StringBuilder();
		for(AlgorithmSet set: joinSets){
			strBuilder.append(set.getLeftName());
			strBuilder.append(',');
		}
		if(strBuilder.length() > 0){
			strBuilder.deleteCharAt(strBuilder.length() - 1);
		}
		return strBuilder.toString();
	}
	
	private String getRightNames(AlgorithmSet[] joinSets){
		StringBuilder strBuilder = new StringBuilder();
		for(AlgorithmSet set: joinSets){
			strBuilder.append(set.getRightName());
			strBuilder.append(',');
		}
		if(strBuilder.length() > 0){
			strBuilder.deleteCharAt(strBuilder.length() - 1);
		}
		return strBuilder.toString();
	}
	
	public void buildSaver(String path){
		Element savers = doc.createElement("results-savers");
		rootElement.appendChild(savers);
		Element saver = doc.createElement("results-saver");
		savers.appendChild(saver);
		Attr saverClass = doc.createAttribute("class");
		saverClass.setValue("cdc.impl.resultsavers.CSVFileSaver");
		saver.setAttributeNode(saverClass);
		saver.appendChild(createParams(new String[]{"encoding", "output-file"}, new String[]{"UTF-8", path}));
	}
	
	private Element conditionGenerator(AlgorithmSet set){
		Element condition = doc.createElement("condition");
		Attr attrAlgo = doc.createAttribute("class");
		attrAlgo.setValue(set.getAlgorithmName());
		condition.setAttributeNode(attrAlgo);
		Attr attrLeft = doc.createAttribute("left-column");
		attrLeft.setValue(set.getLeftName());
		condition.setAttributeNode(attrLeft);
		Attr attrRight = doc.createAttribute("right-column");
		attrRight.setValue(set.getRightName());
		condition.setAttributeNode(attrRight);
		Attr attrWeight = doc.createAttribute("weight");
		attrWeight.setValue(set.getWeight());
		condition.setAttributeNode(attrWeight);
		condition.appendChild(createParams(set.getParamNames(), set.getParamValues()));
		return condition;
	}
	
	
	public void test(){
		try {
			 
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	 
			// root elements
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("company");
			doc.appendChild(rootElement);
	 
			// staff elements
			Element staff = doc.createElement("Staff");
			rootElement.appendChild(staff);
	 
			// set attribute to staff element
			Attr attr = doc.createAttribute("id");
			attr.setValue("1");
			staff.setAttributeNode(attr);
	 
			// shorten way
			// staff.setAttribute("id", "1");
	 
			// firstname elements
			Element firstname = doc.createElement("firstname");
			firstname.appendChild(doc.createTextNode("yong"));
			staff.appendChild(firstname);
	 
			// lastname elements
			Element lastname = doc.createElement("lastname");
			lastname.appendChild(doc.createTextNode("mook kim"));
			staff.appendChild(lastname);
	 
			// nickname elements
			Element nickname = doc.createElement("nickname");
			nickname.appendChild(doc.createTextNode("mkyong"));
			staff.appendChild(nickname);
	 
			// salary elements
			Element salary = doc.createElement("salary");
			salary.appendChild(doc.createTextNode("100000"));
			staff.appendChild(salary);
	 
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File("C:\\Study\\FRIL-v2.1.5\\FRIL-v2.1.5\\config\\a.xml"));
	 
			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);
	 
			transformer.transform(source, result);
	 
			System.out.println("File saved!");
	 
		  } catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		  } catch (TransformerException tfe) {
			tfe.printStackTrace();
		  }
	}
	
}
