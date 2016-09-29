package rwtchecker.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import rwtchecker.CM.CMType;
import rwtchecker.annotation.FileAnnotations;
import rwtchecker.views.provider.TreeObject;

public class CMModelUtil {
	public static String PathSeparator = System.getProperty("file.separator");
	public static String RealWorldConcept_FileExtension = "concept";
	public static String ProjectConfigFile = "CMReferenceLocation.prop";
	public static String CMTypeRulesFile = "CMTypeRulesFile.prop";
	
//	public static String CMTypeOperationRuleFile = "CMTypeRuleFile.xml";
	public static String RWTypeRulesFolder = "RWType_rules";
	
	
//	public static String NewCMTypeRuleFile = "CMTypeRules.xml";
	public static String typeDefinition = "Type_Definition";
	public static String ConceptDefinitionFolder = "CONCEPTS";
	public static String CMTypesFolder = "CMTYPES";
	
	public static String RWTypeCandidateRules_Folder = "RWTypeRules_candidate";
	
	public static String CMTypePatternFolder = "CMTYPE_Patterns";
	public static String CMTypeRule_ext = ".xml";
	public static String CMTypePattern_ext = ".cmpr";
	
	private static String getConfigFile(){
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IPath path = root.getLocation();
		String stringPath = path.toString();
		return stringPath+ PathSeparator + ProjectConfigFile;	
	}
	
	public static void storePropertyToConfigFile(String key, String contents){
		String configFileLoc = CMModelUtil.getConfigFile();
		File configFile = new File(configFileLoc);
		try {
			if(!configFile.exists()){
				configFile.createNewFile();
			}
			Properties prop = new Properties();
			prop.load(new FileInputStream(configFile));
			prop.put(key, contents);
			prop.store(new FileOutputStream(configFile), null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Object readPropertyFromConfigFile(String key){
		String configFileLoc = CMModelUtil.getConfigFile();
		File configFile = new File(configFileLoc);
		try {
			if(!configFile.exists()){
				configFile.createNewFile();
			}
			Properties prop = new Properties();
			prop.load(new FileInputStream(configFile));
			return prop.get(key);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static File getCMTypeRulesFile(IProject iproject){
		Object location = CMModelUtil.readPropertyFromConfigFile(iproject.getName());
		File cmtypeOperationRuleFile = new File(location + CMModelUtil.PathSeparator + CMModelUtil.CMTypeRulesFile);
		return cmtypeOperationRuleFile;
	}
	
	public static File getRWTypeRulesFiles(IProject iproject){
		Object location = CMModelUtil.readPropertyFromConfigFile(iproject.getName());
		File cmtypeOperationRuleFile = new File(location + CMModelUtil.PathSeparator + CMModelUtil.RWTypeRulesFolder);
		return cmtypeOperationRuleFile;
	}
	
	public static File getCandidateCMTypeRuleFile(IProject iproject, String fileName){
		Object location = CMModelUtil.readPropertyFromConfigFile(iproject.getName());
		File cmtypeRuleFolder = new File(location + CMModelUtil.PathSeparator + CMModelUtil.RWTypeCandidateRules_Folder );
		if(!cmtypeRuleFolder.exists()){
			cmtypeRuleFolder.mkdir();
		}
		File cmtypeOperationRuleFile = new File(location + CMModelUtil.PathSeparator + CMModelUtil.RWTypeCandidateRules_Folder + CMModelUtil.PathSeparator + fileName + CMModelUtil.CMTypeRule_ext);
		return cmtypeOperationRuleFile;
	}
	
	public static File getCMTypePatternFile(IProject iproject, String fileName){
		Object location = CMModelUtil.readPropertyFromConfigFile(iproject.getName());
		File cmTypePatternFolder = new File(location + CMModelUtil.PathSeparator + CMModelUtil.CMTypePatternFolder );
		if(!cmTypePatternFolder.exists()){
			cmTypePatternFolder.mkdir();
		}
		String cmtypePatternFileName = String.valueOf(fileName.hashCode());
		File cmtypePatternFile = new File(location + CMModelUtil.PathSeparator + CMModelUtil.CMTypePatternFolder + CMModelUtil.PathSeparator + cmtypePatternFileName + CMModelUtil.CMTypePattern_ext);
		return cmtypePatternFile;
	}
	
	public static File getConceptDetailFile(IProject selectedProject, String conceptName){
		Object location = CMModelUtil.readPropertyFromConfigFile(selectedProject.getName());
		String filePath = location.toString() + CMModelUtil.PathSeparator + ConceptDefinitionFolder + CMModelUtil.PathSeparator + conceptName+"."+CMModelUtil.RealWorldConcept_FileExtension;
		return new File(filePath);
	}
	
	public static File getTreeIndexFile(IProject selectedProject){
		Object location = CMModelUtil.readPropertyFromConfigFile(selectedProject.getName());
		File cMTypeDir = new File(location.toString() + CMModelUtil.PathSeparator + CMModelUtil.CMTypesFolder);
		String treeIndexPath = cMTypeDir.getAbsolutePath() + CMModelUtil.PathSeparator + TreeObject.treeIndexFileName;
		return new File(treeIndexPath);
	}
	
	public static File getCMTypeFile(IProject selectedProject, TreeObject selectedTO){
		Object location = CMModelUtil.readPropertyFromConfigFile(selectedProject.getName());
		File cMTypeDir = new File(location.toString() + CMModelUtil.PathSeparator + CMModelUtil.CMTypesFolder);
		String CMTypeFile = cMTypeDir.getAbsolutePath() + CMModelUtil.PathSeparator + selectedTO.getName().trim();
		return new File(CMTypeFile);
	}
	
	public static File getCMTypeFile(IProject selectedProject, String typeName){
		Object location = CMModelUtil.readPropertyFromConfigFile(selectedProject.getName());
		File cMTypeDir = new File(location.toString() + CMModelUtil.PathSeparator + CMModelUtil.CMTypesFolder);
		String CMTypeFile = cMTypeDir.getAbsolutePath() + CMModelUtil.PathSeparator + typeName.trim();
		return new File(CMTypeFile);
	}
	
	public static File generateXMLFile(IProject selectedProject, String typeName){
		Object location = CMModelUtil.readPropertyFromConfigFile(selectedProject.getName());
		File cMTypeDir = new File(location.toString() + CMModelUtil.PathSeparator + CMModelUtil.CMTypesFolder);
		String xml = cMTypeDir.getAbsolutePath() + CMModelUtil.PathSeparator + typeName.trim()+".xml";
		return new File(xml);
	}
	
	public static ArrayList<String> getBaseTypes(IProject selectedProject){
		ArrayList<String> results = new ArrayList<String>();
		if(selectedProject!=null){
			Object location = CMModelUtil.readPropertyFromConfigFile(selectedProject.getName());
			if(location !=null){
				File baseTypeDir = new File(location.toString() + CMModelUtil.PathSeparator + CMModelUtil.CMTypesFolder);
				File dir = new File(baseTypeDir.toString());
				if((dir.exists())&& (dir.isDirectory())){
					File[] baseTypesFiles = dir.listFiles(new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							return dir.isDirectory();
						}
					});
					for(int i=0;i<baseTypesFiles.length;i++){
						results.add(baseTypesFiles[i].getName());
					}
				}	
			}
		}
		return results;
	}
	
	public static String annotationFolder = "CM_Annotations";
	public static String annotationExtension = ".xml";
	public static String annotationPropFile = "annotations.prop";
	
	public static File getAnnotationFile(IFile ifile){
		if(ifile.getFileExtension().equals("java")){
			Object location = CMModelUtil.readPropertyFromConfigFile(ifile.getProject().getName());
			File annotationFolder = new File(location + CMModelUtil.PathSeparator + CMModelUtil.annotationFolder);
			if(!annotationFolder.exists()){
				annotationFolder.mkdir();
			}
			File annotationPropFile = new File(location + CMModelUtil.PathSeparator + CMModelUtil.annotationFolder+ CMModelUtil.PathSeparator + CMModelUtil.annotationPropFile);
			if(!annotationPropFile.exists()){
				try {
					annotationPropFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			Properties annotationProp = new Properties();
			try {
				annotationProp.load(new FileInputStream(annotationPropFile));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			String fileKeyName = ifile.getProjectRelativePath().toString();
			if(annotationProp.get(fileKeyName) != null){
				String annotationFileName = annotationProp.get(fileKeyName).toString(); 
				String annotationFilePath = annotationFolder + CMModelUtil.PathSeparator + annotationFileName;
				return new File(annotationFilePath);
			}else{
				int fileCourt = annotationFolder.list().length;
				String annotationFileName = String.valueOf(fileCourt) + annotationExtension;
				String annotationFileAbsPath = annotationFolder + CMModelUtil.PathSeparator + annotationFileName;
				FileAnnotations fileAnnotations = new FileAnnotations();
				FileAnnotations.saveToFile(fileAnnotations, new File(annotationFileAbsPath));
				annotationProp.put(ifile.getProjectRelativePath().toString(), annotationFileName);
				try {
					annotationProp.store(new FileOutputStream(annotationPropFile), null);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	public static ArrayList<FileAnnotations> getAllFileAnntationsForProject(IProject curProject){
		ArrayList<FileAnnotations> results = new ArrayList<FileAnnotations>();
		Object location = CMModelUtil.readPropertyFromConfigFile(curProject.getName());
		File annotationFolder = new File(location + CMModelUtil.PathSeparator + CMModelUtil.annotationFolder);
		if(!annotationFolder.exists()){
			annotationFolder.mkdir();
		}
		File annotationPropFile = new File(location + CMModelUtil.PathSeparator + CMModelUtil.annotationFolder+ CMModelUtil.PathSeparator + CMModelUtil.annotationPropFile);
		if(!annotationPropFile.exists()){
			try {
				annotationPropFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Properties annotationProp = new Properties();
		try {
			annotationProp.load(new FileInputStream(annotationPropFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(Object fileName : annotationProp.values()){			
			File annotationFile = new File(location + CMModelUtil.PathSeparator + CMModelUtil.annotationFolder+ CMModelUtil.PathSeparator + fileName);
			if(annotationFile.isFile() && annotationFile.getName().endsWith(CMModelUtil.annotationExtension)){
				FileAnnotations thisFileAnno = FileAnnotations.loadFromXMLFile(annotationFile); 
				results.add(thisFileAnno);
			}
		}
		return results;
	}
	
	public static String errorReportsFolder = "CM_erreports";
	public static String errorReportExtension = ".erreport";
	public static String errorReportPropFile = "errorReports.prop";
	
	public static File getErrorReportFile(IFile ifile){
		Object location = CMModelUtil.readPropertyFromConfigFile(ifile.getProject().getName());
		File errorReportsFolder = new File(location + CMModelUtil.PathSeparator + CMModelUtil.errorReportsFolder);
		if(!errorReportsFolder.exists()){
			errorReportsFolder.mkdir();
		}
		
		File errorReportPropFile = new File(location + CMModelUtil.PathSeparator + CMModelUtil.errorReportsFolder+ CMModelUtil.PathSeparator + CMModelUtil.errorReportPropFile);
		if(!errorReportPropFile.exists()){
			try {
				errorReportPropFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		Properties errorReportProp = new Properties();
		try {
			errorReportProp.load(new FileInputStream(errorReportPropFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String fileKeyName = ifile.getProjectRelativePath().toString();
		if(errorReportProp.get(fileKeyName) != null){
			return new File(errorReportProp.get(fileKeyName).toString());
		}else{
			int fileCourt = errorReportsFolder.list().length;
			String errorReportFileName = String.valueOf(fileCourt) + errorReportExtension;
			errorReportFileName = errorReportsFolder + CMModelUtil.PathSeparator + errorReportFileName;
			errorReportProp.put(ifile.getProjectRelativePath().toString(), errorReportFileName);
			try {
				errorReportProp.store(new FileOutputStream(errorReportPropFile), null);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return new File(errorReportFileName);
		}
	}
	
	public static CompilationUnit getCurrentCompliationUnit(){
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		IFile activeFile = ActivePart.getFileOfActiveEditror();
		ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(activeFile);
		parser.setSource(icompilationUnit); // set source
		parser.setResolveBindings(true); // we need bindings later on
		CompilationUnit compilationResult = (CompilationUnit) parser.createAST(null);
		compilationResult.recordModifications();
		return compilationResult;
	}
	
	public static String complexTypeSeparator = "*";
	
	public static TreeObject readInAllCMTypesToTreeObject(IFile fileInput){
		return readInAllCMTypesToTreeObject(fileInput.getProject());
	}
	
	public static TreeObject readInAllCMTypesToTreeObject(IProject iproject){
		TreeObject invisibleRootTreeObject = new TreeObject("");
		File treeIndexFile = CMModelUtil.getTreeIndexFile(iproject);
		if(!treeIndexFile.exists()){
			try {
				TreeObject cmtypeListTO = new TreeObject(TreeObject.treeObjectTopName);
				//create a error type which is needed in type rule definition
				//TreeObject errorType = new TreeObject(CMType.errorType);
				//cmtypeListTO.addChild(errorType);
				invisibleRootTreeObject.addChild(cmtypeListTO);				
				treeIndexFile.createNewFile();
				TreeObject.writeOutTreeObject(invisibleRootTreeObject, treeIndexFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			invisibleRootTreeObject = TreeObject.readInTreeObject(iproject, treeIndexFile);	
		}
		
		
//		if(location !=null){
//			File cMTypeDir = new File(location.toString() + CMModelUtil.PathSeparator + CMModelUtil.CMTypesFolder);
//			File[] topLevelFolders = cMTypeDir.listFiles();
//			for(File topLevelFolder:topLevelFolders ){
//				TreeObject topLevelTO = new TreeObject(topLevelFolder.getName());
//				File treeIndexFile = new File(topLevelFolder.getAbsolutePath() + CMModelUtil.PathSeparator + TreeObject.treeIndexFileName);
//				if(!treeIndexFile.exists()){
//					try {
//						treeIndexFile.createNewFile();
//						TreeObject.writeOutTreeObject(topLevelTO, treeIndexFile);
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}else{
//					topLevelTO = TreeObject.readInTreeObject(treeIndexFile);	
//				}
//				cmtypeListTO.addChild(topLevelTO);
//				constructTreeObject(topLevelFolder, topLevelTO);
//			}
//		}
		return invisibleRootTreeObject;
	}
	
//	private static void constructTreeObject(File projectDir, TreeObject treeObjectParent){
//		if((projectDir.exists())&& (projectDir.isDirectory())){
//			File[] cmtypeFiles = projectDir.listFiles(new FilenameFilter() {
//				@Override
//				public boolean accept(File dir, String name) {
//					return true;
//				}
//			});
//			for(int i=0;i<cmtypeFiles.length;i++){
//				if(cmtypeFiles[i].isDirectory()){
//					CMType thisNewCMType = CMType.readInCorrespondenceType(cmtypeFiles[i]);
//					TreeObject newTreeObject = new TreeObject(thisNewCMType.getTypeName());
////					newTreeObject.setCmType(thisNewCMType);
//					treeObjectParent.addChild(newTreeObject);
//					constructTreeObject(cmtypeFiles[i], newTreeObject);
//				}
//			}
//		}	
//	}	
	
	public static CMType getCMTypeFromTypeName(IProject currentProject, String annotatedTypeName){
		if(annotatedTypeName!=null){
			File cmTypeFile = CMModelUtil.getCMTypeFile(currentProject, annotatedTypeName);
			if((cmTypeFile!= null) && (cmTypeFile.exists())){
				return CMType.readInCorrespondenceType(cmTypeFile);
			}
		}
		return null;
	}
	
	public static CMType getCMTypeFromTreeObject(IProject currentProject, TreeObject selectedTO){
		File cmTypeFile = CMModelUtil.getCMTypeFile(currentProject, selectedTO);
		if((cmTypeFile!= null) && (cmTypeFile.exists())){
			return CMType.readInCorrespondenceType(cmTypeFile);
		}
		return null;
	}
	
	public static ArrayList<IResource> getAllJavaSourceFiles(IJavaProject javaProject){
		IPackageFragmentRoot[] packageFragmentRoot;
		 ArrayList<IResource> javaSourceFiles = new ArrayList<IResource>();
		try {
			packageFragmentRoot = javaProject.getAllPackageFragmentRoots();
	        for (int i = 0; i < packageFragmentRoot.length; i++){
	            if (packageFragmentRoot[i].getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT && !packageFragmentRoot[i].isArchive())
	            {
	            	IResource folder = packageFragmentRoot[i].getResource();
	            	if(folder instanceof IContainer){
	                	findAllJavaFiles((IContainer)folder, javaSourceFiles);                		
	            	}
	            }
	        }
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
        
        System.out.println("we have in total "+javaSourceFiles.size()+" files");
        return javaSourceFiles;
	}
	
	private static void findAllJavaFiles(IContainer folder, ArrayList<IResource> allJavaSourcesFiles){
		IResource[] resources;
		try {
			resources = folder.members();
			for(IResource resource: resources){
				if(resource.getName().endsWith(".java")){
					allJavaSourcesFiles.add(resource);
				}
	        	if(resource instanceof IContainer){
	        		findAllJavaFiles((IContainer)resource, allJavaSourcesFiles);
	        	}
	        }
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
}