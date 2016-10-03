package rwtchecker.popup.actions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import rwtchecker.annotation.FileAnnotations;
import rwtchecker.annotation.RWTAnnotation;
import rwtchecker.popup.actions.inference.TypePropagationVisitor;
import rwtchecker.typechecker.ExtractorVisitor;
import rwtchecker.util.ActivePart;
import rwtchecker.util.RWTSystemUtil;


public class TypePropagationProjectInNavigator implements IObjectActionDelegate {
	
	private ISelection selection;

	private String typesToPropagateFile = "";
	//propagation from method arguments to all calling sites of method;
	public Map<String, PropagatedMethodsSigniture> paramPropagationMap = new HashMap<String, PropagatedMethodsSigniture>();
	
	//learning annotations in assignments based on experiences; e.g., a = b+c;
	//
	public Map<String, ArrayList<PropagatedMethodsSigniture>> learningPropagationMap = new HashMap<String, ArrayList<PropagatedMethodsSigniture>>();
	
	public static String XMLTag_root = "methods";
	public static String XMLTag_method = "method";
	public static String XMLTag_methodKey = "methodkey";
	public static String XMLTag_param = "param";
	public static String XMLTag_returnType = "resulttype";
	public static String XMLTag_propagationType = "propagationType";
	
	
	public static String paramPropagation = "param_argument";
	public static String methodPropagation = "returnStatement2signature";
	public static String assignmentPropagation = "assignment_right2left";
	
	public TypePropagationProjectInNavigator(){
		super();
		//this.typesToPropagateFile = "E:\\typePropagation.xml";
		if(new File(typesToPropagateFile).exists()){
			loadMethods();
		}
	}
	
	
	public TypePropagationProjectInNavigator(File file){
		this.typesToPropagateFile = file.getAbsolutePath();
		if(file.exists()){
			loadMethods();
		}
	}
	
	public void clear(){
		this.paramPropagationMap.clear();
	}
	
	public void loadMethods(){
		File file = new File(this.typesToPropagateFile);
		if(file.exists()){
	        SAXReader reader = new SAXReader();
	        try {
				Document document = reader.read(file);
				Element root = document.getRootElement();
		        for ( Iterator i = root.elementIterator(TypePropagationProjectInNavigator.XMLTag_method); i.hasNext(); ) {
		            Element methodElement = (Element) i.next();
		            PropagatedMethodsSigniture methodToPropagate =  new PropagatedMethodsSigniture();
		            String methodKey = methodElement.element(TypePropagationProjectInNavigator.XMLTag_methodKey).getText().trim();
		            String resultType = methodElement.element(TypePropagationProjectInNavigator.XMLTag_returnType).getText().trim();
		            String propagationType = methodElement.element(TypePropagationProjectInNavigator.XMLTag_propagationType).getText().trim();
		            for ( Iterator params = methodElement.elementIterator(TypePropagationProjectInNavigator.XMLTag_param);params.hasNext(); ){
		            	Element paramElement = (Element) params.next();
		            	String paramType = paramElement.getText().trim();
		            	methodToPropagate.paramTypes.add(paramType);
		            }
		            methodToPropagate.setMethodKey(methodKey);
		            methodToPropagate.setReturnType(resultType);
		            methodToPropagate.setPropagationType(propagationType);
		            if(propagationType.equals(TypePropagationProjectInNavigator.paramPropagation)){
		            	this.paramPropagationMap.put(methodKey, methodToPropagate);
		            }else if(propagationType.equals(TypePropagationProjectInNavigator.assignmentPropagation)){
		            	if(this.learningPropagationMap.containsKey(methodKey)){
		            		this.learningPropagationMap.get(methodKey).add(methodToPropagate);
		            	}else{
		            		ArrayList<PropagatedMethodsSigniture> values = new ArrayList<PropagatedMethodsSigniture>();
		            		values.add(methodToPropagate);
		            		this.learningPropagationMap.put(methodKey, values);
		            	}
		            }		            
		        }
			} catch (DocumentException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void saveMethodsToFile(File saveFile, Map<String, PropagatedMethodsSigniture> paramPropagationMap){
		Document document = DocumentHelper.createDocument();
        Element root = document.addElement( XMLTag_root );
        for (String methodKey : paramPropagationMap.keySet()){
        	PropagatedMethodsSigniture methodToPropagate = paramPropagationMap.get(methodKey);
        	Element methodSection = root.addElement(XMLTag_method);
        	methodSection.addElement(XMLTag_methodKey).setText(methodToPropagate.getMethodKey());
        	methodSection.addElement(XMLTag_propagationType).setText(methodToPropagate.getPropagationType());
        	methodSection.addElement(XMLTag_returnType).setText(methodToPropagate.getReturnType());
        	methodSection.addElement(XMLTag_returnType).setText(methodToPropagate.getReturnType());
        	for(String paraType : methodToPropagate.paramTypes){
        		methodSection.addElement(XMLTag_param).setText(paraType);
        	}
        }
        XMLWriter writer;
		try {
			if(saveFile.exists()){
				saveFile.delete();	
			}
			saveFile.createNewFile();
			writer = new XMLWriter(
			        new FileWriter(saveFile));
            writer.write( document );
            writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static TypePropagationProjectInNavigator getManagerForCurrentProject(){
		if(ActivePart.getFileOfActiveEditror() != null){
			File file = RWTSystemUtil.getRWTypeRulesFiles(ActivePart.getFileOfActiveEditror().getProject());
			return new TypePropagationProjectInNavigator(file);
		}
		return null;
	}
	
	public static void main(String args[]){		
		TypePropagationProjectInNavigator typePropagation = new TypePropagationProjectInNavigator(new File("E:\\typePropagation.xml"));
		System.out.println(typePropagation.typesToPropagateFile);
		System.out.println(typePropagation.paramPropagationMap);
	}
	
	public class PropagatedMethodsSigniture{
		private String methodKey = "";
		public ArrayList<String> paramTypes = new ArrayList<String>();
		private String returnType = "";
		private String propagationType = "";
		public String getMethodKey() {
			return methodKey;
		}
		public void setMethodKey(String methodKey) {
			this.methodKey = methodKey;
		}
		public String getReturnType() {
			return returnType;
		}
		public void setReturnType(String returnType) {
			this.returnType = returnType;
		}
		public String getPropagationType() {
			return propagationType;
		}
		public void setPropagationType(String propagationType) {
			this.propagationType = propagationType;
		}
	}

	@Override
	public void run(IAction arg0) {
		//if(paramPropagationMap.size()>0){
			IStructuredSelection selection = (IStructuredSelection)(this.selection);
			IProject iProject = (IProject) ((IStructuredSelection) selection).getFirstElement();
			if (iProject == null) {
				return;
			}else{
				IJavaProject javaProject = JavaCore.create(iProject);
				ArrayList<IResource> javaSourceFiles = RWTSystemUtil.getAllJavaSourceFiles(javaProject);
				for(IResource javaSource:javaSourceFiles){
	        		ASTParser parser = ASTParser.newParser(AST.JLS3);
	        		parser.setKind(ASTParser.K_COMPILATION_UNIT);
	        		IWorkspace workspace= ResourcesPlugin.getWorkspace();    
	        		IFile sourceFile= workspace.getRoot().getFileForLocation(javaSource.getLocation());
	        		if(sourceFile.exists()){
	        			System.out.println("Type Propagation for file: "+ sourceFile.getName());
	        			ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(sourceFile);
	        			parser.setSource(icompilationUnit); // set source
	        			parser.setResolveBindings(true); // we need bindings later on
	        			CompilationUnit compilationResult = (CompilationUnit) parser.createAST(null);
	        			TypePropagationVisitor typePropagationVisitor = new TypePropagationVisitor(compilationResult);
	        			compilationResult.accept(typePropagationVisitor);
	        		}
				}
				System.out.println("Type Propagation ends here.");
			}
				
		//}
	}
	
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		
	}
	
}

