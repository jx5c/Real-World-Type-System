package rwtchecker.annotation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;

import rwtchecker.rwt.RWType;
import rwtchecker.util.ActivePart;
import rwtchecker.util.RWTSystemUtil;


public class FileAnnotations {
	
	
	public static String XMLTag_annotations = "annotations";
	public static String XMLTag_annotation_body = "annotation_body";
	public static String XMLTag_bodyKey = "body_key";
	public static String XMLTag_annotation_entry = "annotation_link";
	public static String XMLTag_annotation_type = "annotation_type";
	public static String XMLTag_annotation_contents = "annotation_contents";
	
	
	private HashMap <String, ArrayList<RWTAnnotation>> annotations = new HashMap <String, ArrayList<RWTAnnotation>>();
	
	public HashMap<String, ArrayList<RWTAnnotation>> getAnnotations() {
		return annotations;
	}
	
	public int getAllAnnotationsCount(){
		int count = 0;
		for (String key: annotations.keySet()){
			count = count + annotations.get(key).size();
		}
		return count;
	}
	
	public HashSet<String> getRWTAccessed(){
		HashSet<String> rwts = new HashSet<String>();
		for(String methodKey : annotations.keySet()){
			ArrayList<RWTAnnotation> methodAnnos = annotations.get(methodKey);
			for(RWTAnnotation rwtAnno : methodAnnos){
				String annotationContents = rwtAnno.getAnnotationContents();
				String rwtString = annotationContents;
				if(rwtAnno.getAnnotationType().equals(RWTAnnotation.Define)){
					if(annotationContents.indexOf("=")!=-1){
						rwtString = annotationContents.split("=")[1];
						rwts.add(rwtString);
					}					
				}else if(rwtAnno.getAnnotationType().equals(RWTAnnotation.Return)){
					rwts.add(rwtString);
				}					
			}
		}
		return rwts;
	}
	
	public ArrayList<RWTAnnotation> retrieveAnnotations(String bodyDeclKey){
		return annotations.get(bodyDeclKey); 
	}
	
	public void addAnnotationContent(String bodyDeclKey, String annotationType, String annotationContents){
		ArrayList<RWTAnnotation> thisAnnotations = annotations.get(bodyDeclKey);
		if(thisAnnotations == null){
			thisAnnotations = new ArrayList<RWTAnnotation>();
		}
		RWTAnnotation annotation = new RWTAnnotation();
		annotation.setAnnotationType(annotationType);
		annotation.setAnnotationContents(annotationContents);

		if(!thisAnnotations.contains(annotation)){
			thisAnnotations.add(annotation);	
		}
		annotations.put(bodyDeclKey, thisAnnotations);
	}
	
	public boolean addDefineAnnotation(String bodyDeclKey, String annotationType,
			String formalElementName, String cMType){
		ArrayList<RWTAnnotation> thisAnnotations = annotations.get(bodyDeclKey);
		if(thisAnnotations == null){
			thisAnnotations = new ArrayList<RWTAnnotation>();
		}
		RWTAnnotation annotation = new RWTAnnotation();
		annotation.setAnnotationType(annotationType);
		if(annotationType.equals(RWTAnnotation.Define)){
			annotation.setAnnotationContents(RWTAnnotation.cmTypeForAnnotation + "(" + formalElementName +")"+"="+cMType);
		}else if(annotationType.equals(RWTAnnotation.Return)){
			if(cMType!= null){
				annotation.setAnnotationContents(cMType);
			}else if(formalElementName!= null){
				annotation.setAnnotationContents(RWTAnnotation.cmTypeForAnnotation + "("  + formalElementName +")");	
			} 
		}else if(annotationType.equals(RWTAnnotation.Invariant)){
			/* save for future use
			//@CM inv cmt#units(lat1)=cmt#units(lat2)
			if(formalElementName.indexOf("#")!=-1){
				String att1 = cMType.split("#")[0];
				String att2 = cMType.split("#")[1];
				String left = RWTAnnotation.cmTypeForAnnotation+"#"+att1+"("+formalElementName.split("#")[0]+")";
				String right = RWTAnnotation.cmTypeForAnnotation+"#"+att2+"("+formalElementName.split("#")[1]+")";
				String annotationContents = left+"="+right;
				annotation.setAnnotationContents(annotationContents);
			}else{
			//@CM inv cmt#units(lat1)=radians
				String att_name = cMType.split("#")[0];
				String att_value = cMType.split("#")[1];
				String annotationContents = RWTAnnotation.cmTypeForAnnotation+"#"+att_name+"("+formalElementName+")"+"="+att_value;
				annotation.setAnnotationContents(annotationContents);
			}
			*/
		}
		//if 
		for(RWTAnnotation anno : thisAnnotations){
			if(anno.getAnnotationType().equals(RWTAnnotation.Return) && anno.getAnnotationContents().equals(RWType.GenericMethod)){
				return false;
			}
		}
		/* get ready for invariant annotations
		boolean hasInvAnno = false;
		for(RWTAnnotation anno : thisAnnotations){
			if(anno.getAnnotationContents().indexOf("("+formalElementName+")")!=-1){
				return false;
			}
			if(anno.getAnnotationType().equals(RWTAnnotation.Invariant)){
				hasInvAnno = true;
			}
		}
		if(hasInvAnno && annotationType.equals(RWTAnnotation.Invariant)){
			return false;
		}
		*/
		if(thisAnnotations.contains(annotation)){
			thisAnnotations.remove(annotation);
		}
		thisAnnotations.add(annotation);
		annotations.put(bodyDeclKey, thisAnnotations);
		return true;
	}
	
	public static void saveToFile(FileAnnotations annotations, File file){
		Document document = DocumentHelper.createDocument();
        Element root = document.addElement( FileAnnotations.XMLTag_annotations );
        HashMap <String, ArrayList<RWTAnnotation>> thisAnnotations = annotations.getAnnotations();
        Iterator bodykeys = thisAnnotations.keySet().iterator();
        while(bodykeys.hasNext()){
        	String bodyKey = bodykeys.next().toString();
        	ArrayList<RWTAnnotation> annotationLinks = thisAnnotations.get(bodyKey);
        	Element annotation_section = root.addElement( FileAnnotations.XMLTag_annotation_body);
            annotation_section.addElement(FileAnnotations.XMLTag_bodyKey).addText(bodyKey);   
            for (RWTAnnotation annotationLink :annotationLinks){
            	Element annotationEntryElement = annotation_section.addElement(FileAnnotations.XMLTag_annotation_entry);
            	annotationEntryElement.addElement(FileAnnotations.XMLTag_annotation_type).addText(annotationLink.getAnnotationType());
            	annotationEntryElement.addElement(FileAnnotations.XMLTag_annotation_contents).addText(annotationLink.getAnnotationContents());
            }
        }
        
        XMLWriter writer;
		try {
			if(file.exists()){
				file.delete();	
			}
			file.createNewFile();
			writer = new XMLWriter(
			        new FileWriter(file));
            writer.write( document );
            writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static FileAnnotations loadFromXMLFile(File file){
		FileAnnotations fileAnnotations = new FileAnnotations();
		if(file.exists()){
	        SAXReader reader = new SAXReader();
	        try {
				Document document = reader.read(file);
				Element root = document.getRootElement();
		        for ( Iterator i = root.elementIterator(FileAnnotations.XMLTag_annotation_body); i.hasNext(); ) {
		            Element annotation_section = (Element) i.next();
		            String bodyKey = annotation_section.elementText(FileAnnotations.XMLTag_bodyKey);
		            ArrayList<RWTAnnotation> annotations = new ArrayList<RWTAnnotation>();
		            for ( Iterator j = annotation_section.elementIterator(FileAnnotations.XMLTag_annotation_entry); j.hasNext();){
		            	Element annotation_entry= (Element) j.next();
		            	String annotationType = annotation_entry.elementText(FileAnnotations.XMLTag_annotation_type);
		            	String annotationContents = annotation_entry.elementText(FileAnnotations.XMLTag_annotation_contents);
		            	RWTAnnotation newAnnotation = new RWTAnnotation();
		            	newAnnotation.setAnnotationContents(annotationContents);
		            	newAnnotation.setAnnotationType(annotationType);
		            	annotations.add(newAnnotation);
		            }
		            fileAnnotations.getAnnotations().put(bodyKey, annotations);
		        }
			} catch (DocumentException e) {
				e.printStackTrace();
			}
		}
		return fileAnnotations;
	}
	
	public static String getRWTypeForMethod(IMethodBinding iMethodBinding){
		if(iMethodBinding.getJavaElement()==null){
			return "";
		}
		IFile methodDeclFile = ResourcesPlugin.getWorkspace().getRoot().getFile(iMethodBinding.getJavaElement().getPath());
		if((methodDeclFile != null) && (methodDeclFile.getFileExtension().toLowerCase().endsWith("java"))){
			File annotationFile = RWTSystemUtil.getAnnotationFile(methodDeclFile);
			if(annotationFile != null){
				FileAnnotations fileAnnotations = FileAnnotations.loadFromXMLFile(annotationFile);
				if(fileAnnotations!=null && fileAnnotations.getReturnCMTypeForMethod(iMethodBinding.getKey())!=null){
					return fileAnnotations.getReturnCMTypeForMethod(iMethodBinding.getKey());
				}
			}
		}
		return "";
	}

	public static void changeAnnotationsStatus(){
		IFile currentFile = ActivePart.getFileOfActiveEditror();		
		File annotationFile = RWTSystemUtil.getAnnotationFile(currentFile);
		
		FileAnnotations fileAnnotations = new FileAnnotations ();
		if(annotationFile.exists()){
			fileAnnotations = FileAnnotations.loadFromXMLFile(annotationFile);
			if(fileAnnotations == null){
				return;
			}
		}
		CompilationUnit compilationResult = RWTSystemUtil.getCurrentJavaCompilationUnit();
		HashMap <String, ArrayList<RWTAnnotation>> annotations = fileAnnotations.getAnnotations();
		
		Iterator<String> keysets = annotations.keySet().iterator();
		
		//check for status of javadoc
		boolean javaDocOnFlag = false;
		while(keysets.hasNext()&&!javaDocOnFlag){
			String declarationKey = keysets.next();
			ASTNode declNode = compilationResult.findDeclaringNode(declarationKey);
			if(declNode instanceof BodyDeclaration){
				BodyDeclaration bodyDecl = ((BodyDeclaration)declNode);	
				Javadoc javadoc = bodyDecl.getJavadoc();
				if(javadoc == null){
					continue;
				}
				for(int i=0;i<javadoc.tags().size();i++){
					TagElement tagElement = (TagElement)(javadoc.tags().get(i));
					if(tagElement.getTagName()!= null 
							&&tagElement.getTagName().equals(RWTAnnotation.tagNameForAnnotation)
							&&tagElement.fragments().size()>0){
							javaDocOnFlag = true;
							break;
					}
				}
			}
		}
		
		keysets = annotations.keySet().iterator();
		while(keysets.hasNext()){
			String declarationKey = keysets.next();
			ArrayList<RWTAnnotation> thisAnnotations = annotations.get(declarationKey);
			ASTNode declNode = compilationResult.findDeclaringNode(declarationKey);
			
			if(declNode instanceof BodyDeclaration){
				BodyDeclaration bodyDecl = ((BodyDeclaration)declNode);	
				Javadoc javadoc = bodyDecl.getJavadoc();
				AST ast = compilationResult.getAST();
				if(javadoc == null){
					javadoc = ast.newJavadoc();
					bodyDecl.setJavadoc(javadoc);
				}
				
				if(javaDocOnFlag){
					//switch javadoc off
					for(int i=0;i<javadoc.tags().size();i++){
						TagElement tagElement = (TagElement)(javadoc.tags().get(i));
						if(tagElement.getTagName()!= null 
								&&tagElement.getTagName().equals(RWTAnnotation.tagNameForAnnotation)
								&&tagElement.fragments().size()>0){
									javadoc.tags().remove(i);
									i--;
						}
					}	
				}else{
					//switch javadoc on
					for(int j=0;j<thisAnnotations.size();j++){
						RWTAnnotation thisAnnotation = thisAnnotations.get(j);
						if(thisAnnotation.getAnnotationType().equals(RWTAnnotation.Define) || 
								thisAnnotation.getAnnotationType().equals(RWTAnnotation.Return) ||
								thisAnnotation.getAnnotationType().equals(RWTAnnotation.Invariant)){
							TagElement newTagElement= ast.newTagElement();
							newTagElement.setTagName(RWTAnnotation.tagNameForAnnotation);
							
							TextElement annotationTypeTextElement = ast.newTextElement();
							annotationTypeTextElement.setText(thisAnnotation.getAnnotationType());
							newTagElement.fragments().add(annotationTypeTextElement);
							
							TextElement annotatedTextElement =  ast.newTextElement();
							annotatedTextElement.setText(thisAnnotation.getAnnotationContents());
							newTagElement.fragments().add(annotatedTextElement);
							javadoc.tags().add(newTagElement);
						}
					}
				}
				//delete the Javadoc if no contents
				if(javadoc.tags().size()==0){
					String comments = javadoc.toString();
					Pattern p = Pattern.compile("[a-zA-Z0-9]");  // insert your pattern here
					Matcher m = p.matcher(comments);
					if (!m.find()) {
						bodyDecl.setJavadoc(null);
					}
				}				
			}
		}
		//apply changes
		ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager(); // get the buffer manager
		IPath path = compilationResult.getJavaElement().getPath(); // unit: instance of CompilationUnit
		try {
			bufferManager.connect(path, null); // (1)
			ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(path);
			IDocument document = textFileBuffer.getDocument(); 
			compilationResult.rewrite(document, null).apply(document);	
			textFileBuffer
				.commit(null /* ProgressMonitor */, false  /* Overwrite */); // (3)

		} catch (CoreException e) {
			e.printStackTrace();
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		} finally {
			try {
				bufferManager.disconnect(path, null);
			} catch (CoreException e) {
				e.printStackTrace();
			} 
		}
	}
	
	public String getCMTypeInBodyDecl(String bodyDelKey, String variableName){
		ArrayList<RWTAnnotation> methodAnnotation = retrieveAnnotations(bodyDelKey);
		if(methodAnnotation!=null){
			for(RWTAnnotation thisAnnotation: methodAnnotation){
				if(thisAnnotation.getAnnotationType().equals(RWTAnnotation.Define) ){
					String annotationContent = thisAnnotation.getAnnotationContents().replace(RWTAnnotation.cmTypeForAnnotation, "").trim();
					String annotationContentLeft = annotationContent.split("=")[0];
					String annotationContentRight = annotationContent.split("=")[1];
					String slimAnnotationPart = getRidOfParenthesis(annotationContentLeft);
					if(slimAnnotationPart.equals(variableName)){
						return annotationContentRight;
					}
				}
			}
		}
		return null;
	}
	
	public String getConstantCMTypeInBodyDecl(String bodyDelKey, String constantValue, int locationInBody){
		ArrayList<RWTAnnotation> methodAnnotation = retrieveAnnotations(bodyDelKey);
		if(methodAnnotation!=null){
			for(RWTAnnotation thisAnnotation: methodAnnotation){
				if(thisAnnotation.getAnnotationType().equals(RWTAnnotation.Constant) ){
					String annotationContent = thisAnnotation.getAnnotationContents().replace(RWTAnnotation.cmTypeForAnnotation, "").trim();
					if(annotationContent.indexOf("=")!=-1){
						String annotationContentLeft = annotationContent.split("=")[0];
						String annotationContentRight = annotationContent.split("=")[1];
						String constantAnnotation = getRidOfParenthesis(annotationContentLeft);
						
						String annotationContents =  constantValue + "(loc:"+String.valueOf(locationInBody) + ")";
						if(annotationContents.equals(constantAnnotation)){
							return annotationContentRight;
						}	
					}
				}
			}
		}
		return null;
	}
	
	public String getReturnCMTypeForMethod(String methodKey){
		ArrayList<RWTAnnotation> methodAnnotation = retrieveAnnotations(methodKey);
		if(methodAnnotation!=null){
			for(RWTAnnotation thisAnnotation: methodAnnotation){
				if(thisAnnotation.getAnnotationType().equals(RWTAnnotation.Return)){
					String annotationContent = thisAnnotation.getAnnotationContents();
					if(annotationContent.indexOf( RWTAnnotation.cmTypeForAnnotation) == -1){
						return annotationContent;
					}else{
						String returnVariableWithParenthesis = annotationContent.replace(RWTAnnotation.cmTypeForAnnotation, "").trim();
						String slimReturnVariable = getRidOfParenthesis(returnVariableWithParenthesis);
						return getCMTypeInBodyDecl(methodKey, slimReturnVariable);
					}
				}
			}
		}
		return "";
	}
	
	
	public static String getRidOfParenthesis(String originalString){
		if(originalString.indexOf("(")!= -1){
			int startPos =  originalString.indexOf("(") + 1; 
			int endPos =  originalString.length() - 1; 
			return originalString.substring(startPos, endPos);
		}
		return null;
	}
	
	public HashSet<String> getInvariantsOfBodyDecl(String bodyDelKey){
		HashSet<String> invariants = new HashSet<String>();
		ArrayList<RWTAnnotation> bodyAnnotation = retrieveAnnotations(bodyDelKey);
		if(bodyAnnotation!=null){
			for(RWTAnnotation thisAnnotation: bodyAnnotation){
				if(thisAnnotation.getAnnotationType().equals(RWTAnnotation.Invariant)){
					//load invariants
					String annotationContent = thisAnnotation.getAnnotationContents().replaceAll(RWTAnnotation.cmTypeForAnnotation+"#", "").trim();
					invariants.add(annotationContent);
				}
			}
		}
		return invariants;
	}
	
	
	public void load(Map<String, Map<String, String>> allVariableMap, 
			Map<String, String> methodReturnMap, 
			Map<String, Map<String, HashSet<String>>> allInvAttToRecordMap, 
			Map<String, ArrayList<String>> allInvariantsMap){
		Set<String> keys = this.annotations.keySet();
		for(String bodyDelKey:keys){
			ArrayList<RWTAnnotation> bodyAnnotation = retrieveAnnotations(bodyDelKey);
			Map<String , String> variableMap = new HashMap<String, String>();
			if(bodyAnnotation!=null){
				for(RWTAnnotation thisAnnotation: bodyAnnotation){
					if(thisAnnotation.getAnnotationType().equals(RWTAnnotation.Define) ){
						String annotationContent = thisAnnotation.getAnnotationContents().replace(RWTAnnotation.cmTypeForAnnotation, "").trim();
						String annotationContentLeft = annotationContent.split("=")[0];
						String annotationContentRight = annotationContent.split("=")[1];
						String slimAnnotationPart = getRidOfParenthesis(annotationContentLeft);
						variableMap.put(slimAnnotationPart, annotationContentRight);
					}
					if(thisAnnotation.getAnnotationType().equals(RWTAnnotation.Return)){
						String annotationContent = thisAnnotation.getAnnotationContents();
						if(annotationContent.indexOf( RWTAnnotation.cmTypeForAnnotation) == -1){
							methodReturnMap.put(bodyDelKey, annotationContent);
							//@CM return units=radians
						}
					}
					if(thisAnnotation.getAnnotationType().equals(RWTAnnotation.Invariant)){
						/*
						//load invariants
						String annotationContent = thisAnnotation.getAnnotationContents().replaceAll(RWTAnnotation.cmTypeForAnnotation+"#", "").trim();
						Map<String, HashSet<String>> invAttToRecordMap = allInvAttToRecordMap.get(bodyDelKey);
						if(invAttToRecordMap == null){
							invAttToRecordMap = new HashMap<String, HashSet<String>>();
							allInvAttToRecordMap.put(bodyDelKey, invAttToRecordMap);
						}
						ArrayList<String> invariants = allInvariantsMap.get(bodyDelKey);
						if(invariants==null){
							invariants = new ArrayList<String>();
							allInvariantsMap.put(bodyDelKey, invariants);
						}
						if(annotationContent.split("=")[1].indexOf("(")==-1){
							//String contents = "cmt#units(lat1)=radians";							
							String leftPart = annotationContent.split("=")[0];
							int pos = leftPart.indexOf("(");
							String varName = getRidOfParenthesis(leftPart.substring(pos));
							String attName = annotationContent.substring(0, pos);
							HashSet<String> relevantAtts = invAttToRecordMap.get(varName);
							if(relevantAtts==null){
								relevantAtts = new HashSet<String>();
								invAttToRecordMap.put(varName, relevantAtts);
							}
							//save the relevant attribute name
							relevantAtts.add(attName);
							//store this invariant, the invariant looks like "units(lat1)=radians"
							invariants.add(annotationContent);
						}else{
							//String contents = "cmt#units(lat1)=cmt#units(lat2)";
							String leftPart = annotationContent.split("=")[0];
							int pos1 = leftPart.indexOf("(");
							String var1 = getRidOfParenthesis(leftPart.substring(pos1));
							String att1 = leftPart.substring(0, pos1);
							HashSet<String> relevantAtts = invAttToRecordMap.get(var1);
							if(relevantAtts==null){
								relevantAtts = new HashSet<String>();
								invAttToRecordMap.put(var1, relevantAtts);
							}
							//save the relevant attribute name
							relevantAtts.add(att1);
							
							String rightPart = annotationContent.split("=")[1];
							int pos2 = rightPart.indexOf("(");
							String var2 = getRidOfParenthesis(rightPart.substring(pos2));
							String att2 = rightPart.substring(0, pos2);
							HashSet<String> relevantAtts2 = invAttToRecordMap.get(var2);
							if(relevantAtts2==null){
								relevantAtts2 = new HashSet<String>();
								invAttToRecordMap.put(var2, relevantAtts2);
							}
							//save the relevant attribute name
							relevantAtts2.add(att2);
							//store this invariant, the invariant looks like "units(lat1)=units(lat2)"
							invariants.add(annotationContent);
						}
						*/
					}
				}
				allVariableMap.put(bodyDelKey, variableMap);
			}
		}
	}
	
	public static RWType lookupRWTByVarName(ASTNode node, CompilationUnit compilationResult){
		String rwtypeName = "";
		if(node !=null){
			if(node instanceof SimpleName){							
				IBinding binding= ((SimpleName)node).resolveBinding();
				if(binding!=null){
					if (binding.getKind() == IBinding.METHOD) {
						ASTNode methodDeclNode = compilationResult.findDeclaringNode(binding.getKey());
						if(methodDeclNode instanceof MethodDeclaration){
							MethodDeclaration methodDecl = (MethodDeclaration)methodDeclNode;
							IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(methodDecl.resolveBinding().getJavaElement().getPath());
							FileAnnotations fileAnnotation = getFileAnnotation(ifile);
							if(fileAnnotation != null){
								rwtypeName = fileAnnotation.getReturnCMTypeForMethod(binding.getKey());
							}
						}
					}else if (binding.getKind() == IBinding.VARIABLE) {
						IVariableBinding bindingDecl= ((IVariableBinding) ((SimpleName)node).resolveBinding()).getVariableDeclaration();
						String varName = bindingDecl.getName();
						if(bindingDecl.getJavaElement()!=null){
							IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(bindingDecl.getJavaElement().getPath());
							FileAnnotations fileAnnotation = getFileAnnotation(ifile);
							if(bindingDecl.isField()){
								if(fileAnnotation != null){
									rwtypeName = fileAnnotation.getCMTypeInBodyDecl(bindingDecl.getDeclaringClass().getKey(), varName);
								}
							}else{
								if(fileAnnotation != null){
									rwtypeName = fileAnnotation.getCMTypeInBodyDecl(bindingDecl.getDeclaringMethod().getKey(), varName);
								}
							}	
						}
				 	}
				}
			}
		}
		RWType rwtype = RWTSystemUtil.getCMTypeFromTypeName(ActivePart.getFileOfActiveEditror().getProject(), rwtypeName);
		return rwtype;
	}
	
	private static FileAnnotations getFileAnnotation(IFile ifile){
		if(ifile!=null){
			File annotationFile = RWTSystemUtil.getAnnotationFile(ifile);
			if(annotationFile == null){
				//not java files
				return null;
			}
			FileAnnotations fileAnnotations = new FileAnnotations ();
			if(!annotationFile.exists()){
				try {
					annotationFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else{
				fileAnnotations = FileAnnotations.loadFromXMLFile(annotationFile);
			}
			return fileAnnotations;
		}
		return null;
	}
	
	public static void main(String args[]){
		
		FileAnnotations fileAnnotation = new FileAnnotations();
		//@CM inv cmt#units(lat1)=cmt#units(lat2)
		//String contents = "cmt#units(lat1)=cmt#units(lat2)";
		//String contents = "cmt#units(lat1)=radians";
//		fileAnnotation.addDefineAnnotation("",RWTAnnotation.Invariant,"lat1#lat2","units#units");	
//		fileAnnotation.addDefineAnnotation("",RWTAnnotation.Invariant,"lat1","units#radians");
		
		
	
		//FileAnnotations.saveToFile(fileAnnotation, new File("e:\\testfile.xml"));
		
		//FileAnnotations newFileAnnotation = FileAnnotations.loadFromFile(new File("e:\\testfile.xml"));
		//FileAnnotations.saveToFile(fileAnnotation, new File("e:\\testfile1.xml"));}
		
		//FileAnnotations test = FileAnnotations.loadFromXMLFile(new File("C:\\Users\\Admin\\Dropbox\\Develop\\EvaluationCMs\\KelpieFlightPlanner\\CM_Annotations\\6.xml"));
		//System.out.println(test.getAllAnnotationsCount());
		
		String contents = "cmt#units(lat1)=cmt#units(lat2)";
		String contents2 = "cmt#units(lat1)=radians";
		Map<String, Map<String, HashSet<String>>> allInvAttToRecordMap = new HashMap<String, Map<String, HashSet<String>>>(); 
		Map<String, ArrayList<String>> allInvariantsMap = new HashMap<String, ArrayList<String>>(); 
		fileAnnotation.addAnnotationContent("jian", RWTAnnotation.Invariant, contents);
		fileAnnotation.addAnnotationContent("jian", RWTAnnotation.Invariant, contents2);
		for(String bodyDelKey:fileAnnotation.getAnnotations().keySet()){
			ArrayList<RWTAnnotation> bodyAnnotation = fileAnnotation.getAnnotations().get(bodyDelKey);
			if(bodyAnnotation!=null){
				for(RWTAnnotation thisAnnotation: bodyAnnotation){
					if(thisAnnotation.getAnnotationType().equals(RWTAnnotation.Invariant)){
						//load invariants
						String annotationContent = thisAnnotation.getAnnotationContents().replaceAll(RWTAnnotation.cmTypeForAnnotation+"#", "").trim();
						Map<String, HashSet<String>> invAttToRecordMap = allInvAttToRecordMap.get(bodyDelKey);
						if(invAttToRecordMap == null){
							invAttToRecordMap = new HashMap<String, HashSet<String>>();
							allInvAttToRecordMap.put(bodyDelKey, invAttToRecordMap);
						}
						ArrayList<String> invariants = allInvariantsMap.get(bodyDelKey);
						if(invariants==null){
							invariants = new ArrayList<String>();
							allInvariantsMap.put(bodyDelKey, invariants);
						}
						if(annotationContent.split("=")[1].indexOf("(")==-1){
							//String contents = "cmt#units(lat1)=radians";							
							String leftPart = annotationContent.split("=")[0];
							int pos = leftPart.indexOf("(");
							String varName = getRidOfParenthesis(leftPart.substring(pos));
							String attName = annotationContent.substring(0, pos);
							HashSet<String> relevantAtts = invAttToRecordMap.get(varName);
							if(relevantAtts==null){
								relevantAtts = new HashSet<String>();
								invAttToRecordMap.put(varName, relevantAtts);
							}
							//save the relevant attribute name
							relevantAtts.add(attName);	
							//store this invariant, the invariant looks like "units(lat1)=radians"
							invariants.add(annotationContent);
						}else{
							//String contents = "cmt#units(lat1)=cmt#units(lat2)";
							String leftPart = annotationContent.split("=")[0];
							int pos1 = leftPart.indexOf("(");
							String var1 = getRidOfParenthesis(leftPart.substring(pos1));
							String att1 = leftPart.substring(0, pos1);
							HashSet<String> relevantAtts = invAttToRecordMap.get(var1);
							if(relevantAtts==null){
								relevantAtts = new HashSet<String>();
								invAttToRecordMap.put(var1, relevantAtts);
							}
							//save the relevant attribute name
							relevantAtts.add(att1);
							
							String rightPart = annotationContent.split("=")[1];
							int pos2 = rightPart.indexOf("(");
							String var2 = getRidOfParenthesis(rightPart.substring(pos2));
							String att2 = rightPart.substring(0, pos2);
							HashSet<String> relevantAtts2 = invAttToRecordMap.get(var2);
							if(relevantAtts2==null){
								relevantAtts2 = new HashSet<String>();
								invAttToRecordMap.put(var2, relevantAtts2);
							}
							//save the relevant attribute name
							relevantAtts2.add(att2);	
							//store this invariant, the invariant looks like "units(lat1)=units(lat2)"
							invariants.add(annotationContent);
						}
					}
				}
			}
		}
	}

}


