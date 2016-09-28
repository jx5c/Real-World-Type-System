package cmtypechecker.CM;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import cmtypechecker.util.CMModelUtil;
import cmtypechecker.util.interval.RealInterval;

public class CMType {

	private String typeName = "";
	
	private static String feasiable_range ="feasiable_range"; 
	private static String units = "unit";
		
	public static String XMLTag_type_name = "type_name";
	public static String XMLTag_root = "cmtype";
	public static String XMLTag_explication = "explication";
	public static String XMLTag_semantic_type = "semantic_type";
	public static String XMLTag_machine_type = "machine_type";
	public static String XMLTag_approx_type = "approx_type";
	
	public static String XMLTag_semantic_att = "semantic_att";
	public static String XMLTag_name = "name";
	public static String XMLTag_enable_status = "enable";
	
	public static String NonType = "notype";
	
	public static String TypeLess = "";
	public static String UnknownType = "";
	
	public static String GenericMethod = "generic";
	
	public static String annotation_const = "const#";
	
	public static String genericTypeInRules = "*";
	
	public static String errorType = "error";
	
	public static String error_propogate = "error_propogate";
	public static String error_source = "error_source";

	
	
	private CM_SemanticType semanticType = new CM_SemanticType();
	private CM_ApproxType approximationType = new CM_ApproxType();
	
	public CMType() {
		super();
	}
	
	public CMType(CMType cmTypeFromTreeObject, String newTypeName) {
		super();
		if(cmTypeFromTreeObject!=null){
			semanticType = cmTypeFromTreeObject.getSemanticType();	
		}
		this.typeName = newTypeName;
	}

	public CM_SemanticType getSemanticType() {
		return semanticType;
	}

	public void setSemanticType(CM_SemanticType semanticType) {
		this.semanticType = semanticType;
	}

	public CM_ApproxType getApproximationType() {
		return approximationType;
	}

	public void setApproximationType(CM_ApproxType approximationType) {
		this.approximationType = approximationType;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
	
	@Override
	public String toString(){
		return this.typeName;
	}
	
	@Override
	public boolean equals(Object object){
		if(object instanceof CMType){
			if(((CMType)object).getTypeName().equals(this.getTypeName())){
				return true;
			}
		}
		return false;
	}
	
	public static CMType readInCMType(String correspondenceTypeFile){
		return readInCorrespondenceType(new File(correspondenceTypeFile));
	}
	
	public static CMType readInCorrespondenceType(File correspondenceTypeDir){
		CMType cmType = new CMType();
		File correspondenceTypeFile  = new File(correspondenceTypeDir+CMModelUtil.PathSeparator+CMModelUtil.typeDefinition);
		if(correspondenceTypeFile.exists()){
	        SAXReader reader = new SAXReader();
	        try {
				Document document = reader.read(correspondenceTypeFile);
				Element root = document.getRootElement();
				String typename = root.element(CMType.XMLTag_type_name).getText();
				Element semantic_type_element = root.element(CMType.XMLTag_semantic_type);
				String explicationLink = semantic_type_element.element(CMType.XMLTag_explication).getText();
		        
		        cmType.setTypeName(typename);
		        CM_SemanticType cm_semantic_type = new CM_SemanticType();
		        cm_semantic_type.setExplicationLink(explicationLink);

		        for ( Iterator i = semantic_type_element.elementIterator(CMType.XMLTag_semantic_att); i.hasNext(); ) {
		            Element element = (Element) i.next();
		            String attributeName = element.attribute(CMType.XMLTag_name).getValue();
//		            if(attributeName.equals(feasiable_range)){
//		            	continue;
//		            }
		            String attributeValue = element.getText();
		            String attributeStatus = element.attribute(CMType.XMLTag_enable_status).getValue();
		            CMAttribute newatt = new CMAttribute(attributeName, attributeValue);
		            newatt.setEnableStatus(attributeStatus);
		            cm_semantic_type.addSemanticTypeAtt(newatt);
		        }
		        cmType.setSemanticType(cm_semantic_type);
		        /* to do here
		         * read machine type 
		         */
		        /* to do here
		         * read approximation type 
		         */
			} catch (DocumentException e) {
				e.printStackTrace();
			}
		}
		return cmType; 
	}

	public static void writeOutCMType(CMType correspondenceType, String correspondenceTypeFileDir){
		writeOutCMType(correspondenceType, new File(correspondenceTypeFileDir));
	}
	
	public static void writeOutCMType(CMType correspondenceType, File correspondenceTypeFileDir){
		if(!correspondenceTypeFileDir.exists()){
			correspondenceTypeFileDir.mkdir();
		}
		File correspondenceTypeFile  = new File(correspondenceTypeFileDir+CMModelUtil.PathSeparator+CMModelUtil.typeDefinition);
		if(!correspondenceTypeFile.exists()){
			try {
				correspondenceTypeFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		Document document = DocumentHelper.createDocument();
        Element root = document.addElement( CMType.XMLTag_root );
        root.addElement( CMType.XMLTag_type_name )
            .addText(correspondenceType.typeName);
        
        Element semanticTypeElement = root.addElement( CMType.XMLTag_semantic_type );
        semanticTypeElement.addElement(CMType.XMLTag_explication).addText(correspondenceType.semanticType.getExplicationLink());
        for (CMAttribute semanticAtt :  correspondenceType.semanticType.getSemanticTypeAttributes()){
        	semanticTypeElement.addElement(CMType.XMLTag_semantic_att)
        					.addAttribute(CMType.XMLTag_name, semanticAtt.getAttributeName())
        					.addAttribute(CMType.XMLTag_enable_status, semanticAtt.getEnableStatus())
        					.addText(semanticAtt.getAttributeValue());
        }
        Element machineTypeElement = root.addElement( CMType.XMLTag_machine_type );
        /* 
         * do something about machine type 
         */
        Element approxTypeElement = root.addElement( CMType.XMLTag_approx_type );
        /* 
         * do something about approximation type 
         */
        
        XMLWriter writer;
		try {
			writer = new XMLWriter(
			        new FileWriter(correspondenceTypeFile));
            writer.write( document );
            writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static CMType[] readInAllCMTypes(IFile fileInput){
		return readInAllCMTypes(fileInput.getProject());
	}
	
	public static CMType[] readInAllCMTypes(IProject iproject){
		Object location = CMModelUtil.readPropertyFromConfigFile(iproject.getName());
		ArrayList<CMType> results = new ArrayList<CMType>();
		if(location !=null){
			File dir = new File(location.toString()+CMModelUtil.PathSeparator+CMModelUtil.CMTypesFolder);
			if((dir.exists())&& (dir.isDirectory())){
				File[] cmtypeFiles = dir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return dir.isDirectory();
					}
				});
				for(int i=0;i<cmtypeFiles.length;i++){
					results.add(CMType.readInCorrespondenceType(cmtypeFiles[i]));
				}
			}	
		}
		return results.toArray(new CMType[results.size()]);
	}
	
	public String getAttributeValue(String attName){
		return this.semanticType.findAttValue(attName);
	}
	
	public String getUnitsAttribute(){
		for(CMAttribute att : this.semanticType.getSemanticTypeAttributes()){
			if(att.getAttributeName().equals(CMType.units)){
				return att.getAttributeName() + "=" + att.getAttributeValue();
			}
		}
		return "";
	}
	
	public String getEnabledAttributeSet(){
		String result = "";
		ArrayList<String> enabledAtts = new ArrayList<String>();
		for(CMAttribute att : this.semanticType.getSemanticTypeAttributes()){
			//include only enabled attributes
			if(att.getEnableStatus().equals("y") && !att.getAttributeName().equals(CMType.feasiable_range)){
				enabledAtts.add(att.getAttributeName() + "=" + att.getAttributeValue());
			}
		}
		Collections.sort(enabledAtts);
		if(enabledAtts.size()==0){
			return result;
		}else{
			result = enabledAtts.get(0);
			for(int i=1;i<enabledAtts.size();i++){
				result = result+";"+enabledAtts.get(i);
			}
		}
		return result;
	}
	
	public RealInterval getInterval(){
		for(CMAttribute att : semanticType.getSemanticTypeAttributes()){
			if(att.getAttributeName().equals(CMType.feasiable_range)){
				String range = att.getAttributeValue();
				if(range.indexOf(",")==-1 || range.split(",").length!=2){
					return null;
				}else{
					double lower = Double.valueOf(range.split(",")[0]);
					double upper = Double.valueOf(range.split(",")[1]);
					return new RealInterval(lower,upper); 
				}
			}
		}
		return null;
	}
	
	public static void addUniversalAttribute(String location, String attName, String attVal){
		if(location !=null){
			File dir = new File(location);
			if((dir.exists())&& (dir.isDirectory())){
				for(File cmFolder : dir.listFiles()){
					if(cmFolder.isDirectory()){
						CMType cmtype = CMType.readInCorrespondenceType(cmFolder);
						CM_SemanticType st = cmtype.getSemanticType();
						st.addSemanticTypeAtt(new CMAttribute(attName,attVal));
						CMType.writeOutCMType(cmtype, cmFolder);
					}
				}
			}	
		}
	}
	
	public static void enableOneAttribute(String location, String attName){
		if(location !=null){
			File dir = new File(location);
			if((dir.exists())&& (dir.isDirectory())){
				for(File cmFolder : dir.listFiles()){
					if(cmFolder.isDirectory()){
						CMType cmtype = CMType.readInCorrespondenceType(cmFolder);
						CM_SemanticType st = cmtype.getSemanticType();
						for(CMAttribute cmAtt : st.getSemanticTypeAttributes()){
							if(cmAtt.getAttributeName().equals(attName)){
								cmAtt.setEnableStatus(CMAttribute.enableMark);
							}else{
								cmAtt.setEnableStatus(CMAttribute.disEnableMark);
							}
						}
						CMType.writeOutCMType(cmtype, cmFolder);
					}
				}
			}	
		}
	}
	
	public static void enableAllAttribute(String location){
		if(location !=null){
			File dir = new File(location);
			if((dir.exists())&& (dir.isDirectory())){
				for(File cmFolder : dir.listFiles()){
					if(cmFolder.isDirectory()){
						CMType cmtype = CMType.readInCorrespondenceType(cmFolder);
						CM_SemanticType st = cmtype.getSemanticType();
						for(CMAttribute cmAtt : st.getSemanticTypeAttributes()){
							cmAtt.setEnableStatus(CMAttribute.enableMark);
						}
						CMType.writeOutCMType(cmtype, cmFolder);
					}
				}
			}	
		}
	}
	
	public static void main(String args[]){
//		CMType cmtype = new CMType();
//		cmtype.setTypeName("jianjian");
//		CM_SemanticType st = new CM_SemanticType();
//		st.setExplicationLink("jianjian_explication");
//		st.addSemanticTypeAtt(new CMAttribute("gender", "Male"));
//		st.addSemanticTypeAtt(new CMAttribute("name", "jianjian"));
//		st.addSemanticTypeAtt(new CMAttribute("wife", "xixi"));
//		cmtype.setSemanticType(st);
//		
//		CMType.writeOutCMType(cmtype, "e://jianjian.xml");
//		
//		CMType readCMtype = CMType.readInCMType("e://jianjian.xml");
//		System.out.println(readCMtype);
		
//		CMType cmtype = CMType.readInCMType("E:\\Develop\\EvaluationCMs\\KelpieFlightPlanner\\CMTYPES\\chBord_unit_circle");
		
		/*
		String fileLoc = "E:\\Develop\\EvaluationCMs\\KelpieFlightPlanner\\CMTYPES\\cruise_altitude";
		CMType cmtype = CMType.readInCMType(fileLoc);
		CM_SemanticType st = cmtype.getSemanticType();
		st.addSemanticTypeAtt(new CMAttribute(CMType.feasiable_range,"3000,30000"));
		CMType.writeOutCMType(cmtype, fileLoc);
		System.out.println(cmtype.getInterval());
		CMType cmtypenew = CMType.readInCMType(fileLoc);
		System.out.println(cmtypenew.getEnabledAttributeSet());
		*/
		String cmfolder = "C:\\develop\\projects\\case-studies\\case-study\\EvaluationCMs\\KelpieFlightPlanner\\CMTYPES";
		CMType.addUniversalAttribute(cmfolder, CMType.feasiable_range, "");
		
		
		//String cmfolder = "E:\\Develop\\EvaluationCMs\\KelpieFlightPlanner\\CMTYPES";
		//String cmFolder = "C:\\develop\\projects\\case-studies\\case-study\\EvaluationCMs\\openmap\\CMTYPES";
		//CMType.enableOneAttribute(cmFolder, "earth_model");
		//CMType.enableAllAttribute(cmFolder);
	}

}
