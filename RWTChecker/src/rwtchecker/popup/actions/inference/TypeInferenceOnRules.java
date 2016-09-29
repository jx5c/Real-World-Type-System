package rwtchecker.popup.actions.inference;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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

import rwtchecker.typechecker.ExtractorVisitor;
import rwtchecker.util.ActivePart;
import rwtchecker.util.CMModelUtil;


public class TypeInferenceOnRules implements IObjectActionDelegate {
	
	private static final String XMLTag_method = null;
	private static final String XMLTag_methodKey = null;
	private static final String XMLTag_returnType = null;
	private static final String XMLTag_propagationType = null;
	private static final String XMLTag_param = null;
	private static final String paramPropagation = null;
	private static final String assignmentPropagation = null;

	private ISelection selection;

	private String typesToPropagateFile = "";
	public HashMap<String, PropagatedMethodsSigniture> paramPropagationMap = new HashMap<String, PropagatedMethodsSigniture>();
	
	public HashMap<String, ArrayList<PropagatedMethodsSigniture>> assignmentPropagationMap = new HashMap<String, ArrayList<PropagatedMethodsSigniture>>();
		
	public TypeInferenceOnRules(){
		super();
		this.typesToPropagateFile = "E:\\typePropagation.xml";
		if(new File(typesToPropagateFile).exists()){
			loadMethods();
		}
	}
	
	
	public TypeInferenceOnRules(File file){
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
		        for ( Iterator i = root.elementIterator(TypeInferenceOnRules.XMLTag_method); i.hasNext(); ) {
		            Element methodElement = (Element) i.next();
		            PropagatedMethodsSigniture methodToPropagate =  new PropagatedMethodsSigniture();
		            String methodKey = methodElement.element(TypeInferenceOnRules.XMLTag_methodKey).getText().trim();
		            String resultType = methodElement.element(TypeInferenceOnRules.XMLTag_returnType).getText().trim();
		            String propagationType = methodElement.element(TypeInferenceOnRules.XMLTag_propagationType).getText().trim();
		            for ( Iterator params = methodElement.elementIterator(TypeInferenceOnRules.XMLTag_param);params.hasNext(); ){
		            	Element paramElement = (Element) params.next();
		            	String paramType = paramElement.getText().trim();
		            	methodToPropagate.paramTypes.add(paramType);
		            }
		            methodToPropagate.setMethodKey(methodKey);
		            methodToPropagate.setReturnType(resultType);
		            methodToPropagate.setPropagationType(propagationType);
		            if(propagationType.equals(TypeInferenceOnRules.paramPropagation)){
		            	this.paramPropagationMap.put(methodKey, methodToPropagate);
		            }else if(propagationType.equals(TypeInferenceOnRules.assignmentPropagation)){
		            	if(this.assignmentPropagationMap.containsKey(methodKey)){
		            		this.assignmentPropagationMap.get(methodKey).add(methodToPropagate);
		            	}else{
		            		ArrayList<PropagatedMethodsSigniture> values = new ArrayList<PropagatedMethodsSigniture>();
		            		values.add(methodToPropagate);
		            		this.assignmentPropagationMap.put(methodKey, values);
		            	}
		            }		            
		        }
			} catch (DocumentException e) {
				e.printStackTrace();
			}
		}
	}

	public static TypeInferenceOnRules getManagerForCurrentProject(){
		if(ActivePart.getFileOfActiveEditror() != null){
			File file = CMModelUtil.getRWTypeRulesFiles(ActivePart.getFileOfActiveEditror().getProject());
			return new TypeInferenceOnRules(file);
		}
		return null;
	}
	
	public static void main(String args[]){		
		TypeInferenceOnRules typePropagation = new TypeInferenceOnRules(new File("E:\\typePropagation.xml"));
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
		if(paramPropagationMap.size()>0){
			IStructuredSelection selection = (IStructuredSelection)(this.selection);
			IProject iProject = (IProject) ((IStructuredSelection) selection).getFirstElement();
			if (iProject == null) {
				return;
			}else{
				IJavaProject javaProject = JavaCore.create(iProject);
				ArrayList<IResource> javaSourceFiles = CMModelUtil.getAllJavaSourceFiles(javaProject);
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
				
		}
	}
	
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		
	}
	
}

