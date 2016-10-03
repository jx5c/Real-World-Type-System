package rwtchecker.concept;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import rwtchecker.util.RWTSystemUtil;

public class ConceptDetail implements Serializable {
	
	private static final long serialVersionUID = -54311402356540889L;
	private String conceptName = "";
	private String definition = "";
	private ArrayList<ConceptAttribute> attributes = new ArrayList<ConceptAttribute>(); 
	
	public ConceptDetail(){
		super();
	}
	public ConceptDetail(String newConceptName){
		super();
		this.conceptName = newConceptName;
	}
	public String getDefinition() {
		return definition;
	}
	public void setDefinition(String definition) {
		this.definition = definition;
	}
	public String toString(){
		return "Concept Name:"+conceptName+";"+"definition:"+definition +";"+ attributes.toString();
	}
	public String getConceptName() {
		return conceptName;
	}
	public void setConceptName(String conceptName) {
		this.conceptName = conceptName;
	}

	public void addAttribute(ConceptAttribute conceptAttribute){
		attributes.add(conceptAttribute);
	}
	public ArrayList<ConceptAttribute> getAttributes() {
		return attributes;
	}
	public static ConceptDetail readInConceptDetails(String conceptFile){
		ConceptDetail conceptDetail = new ConceptDetail();
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(new FileInputStream(new File(conceptFile)));
			conceptDetail = (ConceptDetail)in.readObject();
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return conceptDetail; 
	}

	public static void writeOutConceptDetails(ConceptDetail conceptDetail, String conceptFile){
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream
			(new FileOutputStream(new File(conceptFile)));
			out.writeObject(conceptDetail);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	public static void writeOutConceptDetails(ConceptDetail conceptDetail, File conceptFile){
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream
			(new FileOutputStream(conceptFile));
			out.writeObject(conceptDetail);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static ConceptDetail readInConceptDetail(String conceptFile){
		ConceptDetail conceptDetail = new ConceptDetail();
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(new FileInputStream(new File(conceptFile)));
			conceptDetail = (ConceptDetail)in.readObject();
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return conceptDetail; 
	}
	
	public static ConceptDetail readInConceptDetail(File conceptFile){
		ConceptDetail conceptDetail = new ConceptDetail();
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(new FileInputStream(conceptFile));
			conceptDetail = (ConceptDetail)in.readObject();
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return conceptDetail; 
	}
		
	public static ConceptDetail[] readInAllConceptDetails(IProject iproject){
		Object location = RWTSystemUtil.readPropertyFromConfigFile(iproject.getName());
		ArrayList<ConceptDetail> results = new ArrayList<ConceptDetail>();
		if(location !=null){
			File dir = new File(location.toString() + RWTSystemUtil.PathSeparator + RWTSystemUtil.ConceptDefinitionFolder);
			if((dir.exists())&& (dir.isDirectory())){
				File[] conceptFiles = dir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						 if(new File(dir, name).isFile()){
				                return name.endsWith(RWTSystemUtil.RealWorldConcept_FileExtension);
				         }
						 return false;
					}
				});
				for(int i=0;i<conceptFiles.length;i++){
					results.add(ConceptDetail.readInConceptDetail(conceptFiles[i]));
				}
			}	
		}
		return results.toArray(new ConceptDetail[results.size()]);
	}
	/**
	 * 
	 * @param explicationLink explication Name with file extension e.g., latitude.concept
	 * @param iproject
	 * @return
	 */
	public static ConceptDetail readInByLink(String explicationLink){
		ConceptDetail result = new ConceptDetail();
		File targetFile = new File(explicationLink); 
		if(targetFile.exists()){
			result = ConceptDetail.readInConceptDetail(targetFile);
		}	
		return result;
	}
	
	/**
	 * how to compare two concepts in real world
	 */
	public boolean equals(Object conceptDetail){
		boolean result = false;
		if(conceptDetail instanceof ConceptDetail){
			ConceptDetail comparedConceptDetail = (ConceptDetail)conceptDetail;
			if(((ConceptDetail) conceptDetail).getConceptName().equals(comparedConceptDetail.getConceptName())
				&& ((ConceptDetail) conceptDetail).getDefinition().equals(comparedConceptDetail.getDefinition())
				&& ((ConceptDetail) conceptDetail).getAttributes().size() == comparedConceptDetail.getAttributes().size()){
				result = true;
			}
		}
		return result;
	}

	public static void main(String[] args){
		ConceptDetail newConcept = new ConceptDetail();
		newConcept.setConceptName("Latitude");
		newConcept.setDefinition("Latitude gives the location of a place on Earth (or other planetary body) north or south of the equator. ");
		ConceptAttribute attribute1 = new ConceptAttribute();
		attribute1.setAttributeName("origin place");
		attribute1.setAttributeExplanation("The latitude of a place is a calculated according its angle to the origin place; the origin place is usually equator");
		newConcept.addAttribute(attribute1);
		
		ConceptAttribute attribute2 = new ConceptAttribute();
		attribute2.setAttributeName("origin value");
		attribute2.setAttributeExplanation("The latitude value for origin place; normally, it¡¯s 0;");
		newConcept.addAttribute(attribute2);
		
		ConceptAttribute attribute3 = new ConceptAttribute();
		attribute3.setAttributeName("units");
		attribute3.setAttributeExplanation("¡ã(Degrees) ");
		newConcept.addAttribute(attribute3);
		
		
		ConceptAttribute attribute4 =  new ConceptAttribute();
		attribute4.setAttributeName("value");
		attribute4.setAttributeExplanation("The value of the latitude in degree; value ranging from 0¡ã to +90¡ã for location north of the equator, while 0¡ã to -90¡ã for south of equator.");
		newConcept.addAttribute(attribute4);
		
		writeOutConceptDetails(newConcept, "E:\\develop\\SituatedFormalismSample\\Latitude.concept");
		ConceptDetail readinConcept = readInConceptDetails("E:\\develop\\SituatedFormalismSample\\Latitude.concept");
//		ConceptDetail readinConcept  = newConcept;
		System.out.println(readinConcept.getConceptName());
		System.out.println(readinConcept.getDefinition());
		System.out.println(readinConcept.getAttributes());
		
		
		ConceptDetail longitudeConcept = new ConceptDetail();
		longitudeConcept.setConceptName("Longitude");
		longitudeConcept.setDefinition("Longitude Definition");
		attribute1 = new ConceptAttribute();
		attribute1.setAttributeName("origin place");
		attribute1.setAttributeExplanation("The latitude of a place is a calculated according its angle to the origin place; the origin place is usually equator");
		longitudeConcept.addAttribute(attribute1);
		
		attribute2 =  new ConceptAttribute();
		attribute2.setAttributeName("origin value");
		attribute2.setAttributeExplanation("The latitude value for origin place; normally, it¡¯s 0;");
		longitudeConcept.addAttribute(attribute2);
		
		attribute3 = new ConceptAttribute();
		attribute3.setAttributeName("units");
		attribute3.setAttributeExplanation("¡ã(Degrees) ");
		longitudeConcept.addAttribute(attribute3);
		
		
		attribute4 = new ConceptAttribute();
		attribute4.setAttributeName("value");
		attribute4.setAttributeExplanation("The value of the latitude in degree; value ranging from 0¡ã to +90¡ã for location north of the equator, while 0¡ã to -90¡ã for south of equator.");
		longitudeConcept.addAttribute(attribute4);
		writeOutConceptDetails(longitudeConcept, "E:\\develop\\SituatedFormalismSample\\Longitude.concept");
		readinConcept = readInConceptDetails("E:\\develop\\SituatedFormalismSample\\Longitude.concept");
//		ConceptDetail readinConcept  = newConcept;
		System.out.println(readinConcept.getConceptName());
		System.out.println(readinConcept.getDefinition());
		System.out.println(readinConcept.getAttributes());
	}
}
