package rwtchecker.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;

import rwtchecker.CM.CMAttribute;
import rwtchecker.CM.CMType;

public class XMLGeneratorForTypes {
	
	public static String XMLTag_type = "real_world_type";
	public static String XMLTag_type_name = "name";
	public static String XMLTag_explication_type = "explication";
	public static String XMLTag_attributes = "semantic_attributes";
	public static String XMLTag_rwv = "real_world_value";
	public static String XMLTag_explication_attribute = "explication";
	public static String XMLTag_potential_values = "possible_values";
	
	public static void persistTypeToFile(File file, CMType type){
		if(type==null && !file.exists()){
			return;
		}
		Document document = DocumentHelper.createDocument();
        Element root = document.addElement( XMLGeneratorForTypes.XMLTag_type );
        Element typeElement = root.addElement( XMLGeneratorForTypes.XMLTag_type_name );
        typeElement.addText(type.getTypeName());
        Element explicationElement = root.addElement( XMLGeneratorForTypes.XMLTag_explication_type );
        explicationElement.addText(type.getSemanticType().getExplicationLink());
        Element attrisElement = root.addElement( XMLGeneratorForTypes.XMLTag_attributes );
        for(CMAttribute att : type.getSemanticType().getSemanticTypeAttributes()){
        	Element attElement = attrisElement.addElement( att.getAttributeName());
        	attElement.addElement(XMLTag_rwv).addText(att.getAttributeValue());
        	attElement.addElement(XMLTag_explication_attribute);
        	attElement.addElement(XMLTag_potential_values);
        }

        XMLWriter writer;
		try {
			file.delete();
			file.createNewFile();
			writer = new XMLWriter(
			        new FileWriter(file));
            writer.write( document );
            writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
