package rwtchecker.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.cdt.ui.CUIPlugin;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import rwtchecker.annotation.FileAnnotations;
import rwtchecker.dialogs.NoRWTFoundErrorDialog;
import rwtchecker.rwt.RWType;
import rwtchecker.views.provider.TreeObject;

public class RWTSystemUtil {
	
	public static String defaultRWTSystemFolder = "rwt";
	public static String PathSeparator = System.getProperty("file.separator");
	public static String RealWorldConcept_FileExtension = "concept";
	public static String ProjectConfigFile = "RWTSystemLocation.prop";
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
		String configFileLoc = RWTSystemUtil.getConfigFile();
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
		String configFileLoc = RWTSystemUtil.getConfigFile();
		File configFile = new File(configFileLoc);
		try {
			if(!configFile.exists()){
				configFile.createNewFile();
			}
			Properties prop = new Properties();
			prop.load(new FileInputStream(configFile));
			if(!prop.containsKey(key)){
				final IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
				while(true){
					NoRWTFoundErrorDialog errordialog = new NoRWTFoundErrorDialog(window.getShell(),key);
					errordialog.open();
					if(errordialog.getReturnCode()==Window.OK){
						prop.put(key, errordialog.getRwtLocation());
				        break;
					}
					//System.out.println(errordialog.getReturnCode());
				}
				MessageBox messageBox = new MessageBox(window.getShell(), SWT.OK | SWT.ICON_INFORMATION);
		        messageBox.setText("Real-world type system has been set");
		        messageBox.setMessage("The location of real-world type system is set to: "+prop.get(key) +";\n" +
		        	"the location can be modified by accessing the right-click pop menu of the project");
		        messageBox.open();
			}
			return prop.get(key);
		} catch (IOException e){
			e.printStackTrace();
		}
		return null;
	}
	
	private static void enforceConfig(){
		final IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		MessageDialog dialog = new MessageDialog(window.getShell(), "My Title", null,
			    "My message", MessageDialog.ERROR, new String[] { "First",
			        "Second", "Third" }, 0);
			int result = dialog.open();
			System.out.println(result);
//			workbench.restart();
	}
	
	public static File getCMTypeRulesFile(IProject iproject){
		Object location = RWTSystemUtil.readPropertyFromConfigFile(iproject.getName());
		File cmtypeOperationRuleFile = new File(location + RWTSystemUtil.PathSeparator + RWTSystemUtil.CMTypeRulesFile);
		return cmtypeOperationRuleFile;
	}
	
	public static File getRWTypeRulesFiles(IProject iproject){
		Object location = RWTSystemUtil.readPropertyFromConfigFile(iproject.getName());
		File cmtypeOperationRuleFile = new File(location + RWTSystemUtil.PathSeparator + RWTSystemUtil.RWTypeRulesFolder);
		return cmtypeOperationRuleFile;
	}
	
	public static File getCandidateCMTypeRuleFile(IProject iproject, String fileName){
		Object location = RWTSystemUtil.readPropertyFromConfigFile(iproject.getName());
		File cmtypeRuleFolder = new File(location + RWTSystemUtil.PathSeparator + RWTSystemUtil.RWTypeCandidateRules_Folder );
		if(!cmtypeRuleFolder.exists()){
			cmtypeRuleFolder.mkdir();
		}
		File cmtypeOperationRuleFile = new File(location + RWTSystemUtil.PathSeparator + RWTSystemUtil.RWTypeCandidateRules_Folder + RWTSystemUtil.PathSeparator + fileName + RWTSystemUtil.CMTypeRule_ext);
		return cmtypeOperationRuleFile;
	}
	
	public static File getCMTypePatternFile(IProject iproject, String fileName){
		Object location = RWTSystemUtil.readPropertyFromConfigFile(iproject.getName());
		File cmTypePatternFolder = new File(location + RWTSystemUtil.PathSeparator + RWTSystemUtil.CMTypePatternFolder );
		if(!cmTypePatternFolder.exists()){
			cmTypePatternFolder.mkdir();
		}
		String cmtypePatternFileName = String.valueOf(fileName.hashCode());
		File cmtypePatternFile = new File(location + RWTSystemUtil.PathSeparator + RWTSystemUtil.CMTypePatternFolder + RWTSystemUtil.PathSeparator + cmtypePatternFileName + RWTSystemUtil.CMTypePattern_ext);
		return cmtypePatternFile;
	}
	
	public static File getConceptDetailFile(IProject selectedProject, String conceptName){
		Object location = RWTSystemUtil.readPropertyFromConfigFile(selectedProject.getName());
		String filePath = location.toString() + RWTSystemUtil.PathSeparator + ConceptDefinitionFolder + RWTSystemUtil.PathSeparator + conceptName+"."+RWTSystemUtil.RealWorldConcept_FileExtension;
		return new File(filePath);
	}
	
	public static File getTreeIndexFile(IProject selectedProject){
		Object location = RWTSystemUtil.readPropertyFromConfigFile(selectedProject.getName());
		File cMTypeDir = new File(location.toString() + RWTSystemUtil.PathSeparator + RWTSystemUtil.CMTypesFolder);
		String treeIndexPath = cMTypeDir.getAbsolutePath() + RWTSystemUtil.PathSeparator + TreeObject.treeIndexFileName;
		return new File(treeIndexPath);
	}
	
	public static File getCMTypeFile(IProject selectedProject, TreeObject selectedTO){
		Object location = RWTSystemUtil.readPropertyFromConfigFile(selectedProject.getName());
		File cMTypeDir = new File(location.toString() + RWTSystemUtil.PathSeparator + RWTSystemUtil.CMTypesFolder);
		String CMTypeFile = cMTypeDir.getAbsolutePath() + RWTSystemUtil.PathSeparator + selectedTO.getName().trim();
		return new File(CMTypeFile);
	}
	
	public static File getCMTypeFile(IProject selectedProject, String typeName){
		Object location = RWTSystemUtil.readPropertyFromConfigFile(selectedProject.getName());
		File cMTypeDir = new File(location.toString() + RWTSystemUtil.PathSeparator + RWTSystemUtil.CMTypesFolder);
		String CMTypeFile = cMTypeDir.getAbsolutePath() + RWTSystemUtil.PathSeparator + typeName.trim();
		return new File(CMTypeFile);
	}
	
	public static File generateXMLFile(IProject selectedProject, String typeName){
		Object location = RWTSystemUtil.readPropertyFromConfigFile(selectedProject.getName());
		File cMTypeDir = new File(location.toString() + RWTSystemUtil.PathSeparator + RWTSystemUtil.CMTypesFolder);
		String xml = cMTypeDir.getAbsolutePath() + RWTSystemUtil.PathSeparator + typeName.trim()+".xml";
		return new File(xml);
	}
	
	public static ArrayList<String> getBaseTypes(IProject selectedProject){
		ArrayList<String> results = new ArrayList<String>();
		if(selectedProject!=null){
			Object location = RWTSystemUtil.readPropertyFromConfigFile(selectedProject.getName());
			if(location !=null){
				File baseTypeDir = new File(location.toString() + RWTSystemUtil.PathSeparator + RWTSystemUtil.CMTypesFolder);
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
		if(ifile.getFileExtension().equals("java") || ifile.getFileExtension().equals("c")){
			Object location = RWTSystemUtil.readPropertyFromConfigFile(ifile.getProject().getName());
			File annotationFolder = new File(location + RWTSystemUtil.PathSeparator + RWTSystemUtil.annotationFolder);
			if(!annotationFolder.exists()){
				annotationFolder.mkdir();
			}
			File annotationPropFile = new File(location + RWTSystemUtil.PathSeparator + RWTSystemUtil.annotationFolder+ RWTSystemUtil.PathSeparator + RWTSystemUtil.annotationPropFile);
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
				String annotationFilePath = annotationFolder + RWTSystemUtil.PathSeparator + annotationFileName;
				return new File(annotationFilePath);
			}else{
				int fileCourt = annotationFolder.list().length;
				String annotationFileName = String.valueOf(fileCourt) + annotationExtension;
				String annotationFileAbsPath = annotationFolder + RWTSystemUtil.PathSeparator + annotationFileName;
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
		Object location = RWTSystemUtil.readPropertyFromConfigFile(curProject.getName());
		File annotationFolder = new File(location + RWTSystemUtil.PathSeparator + RWTSystemUtil.annotationFolder);
		if(!annotationFolder.exists()){
			annotationFolder.mkdir();
		}
		File annotationPropFile = new File(location + RWTSystemUtil.PathSeparator + RWTSystemUtil.annotationFolder+ RWTSystemUtil.PathSeparator + RWTSystemUtil.annotationPropFile);
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
			File annotationFile = new File(location + RWTSystemUtil.PathSeparator + RWTSystemUtil.annotationFolder+ RWTSystemUtil.PathSeparator + fileName);
			if(annotationFile.isFile() && annotationFile.getName().endsWith(RWTSystemUtil.annotationExtension)){
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
		Object location = RWTSystemUtil.readPropertyFromConfigFile(ifile.getProject().getName());
		File errorReportsFolder = new File(location + RWTSystemUtil.PathSeparator + RWTSystemUtil.errorReportsFolder);
		if(!errorReportsFolder.exists()){
			errorReportsFolder.mkdir();
		}
		
		File errorReportPropFile = new File(location + RWTSystemUtil.PathSeparator + RWTSystemUtil.errorReportsFolder+ RWTSystemUtil.PathSeparator + RWTSystemUtil.errorReportPropFile);
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
			errorReportFileName = errorReportsFolder + RWTSystemUtil.PathSeparator + errorReportFileName;
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
	
	/**
	 * retrieve the current Java compilation unit
	 * @return
	 */
	public static CompilationUnit getCurrentJavaCompilationUnit(){
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		IFile activeFile = ActivePart.getFileOfActiveEditror();
		if(activeFile!=null && activeFile.getFullPath().getFileExtension().equals("java")){
				ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(activeFile);
				parser.setSource(icompilationUnit); // set source
				parser.setResolveBindings(true); // we need bindings later on
				CompilationUnit compilationResult = (CompilationUnit) parser.createAST(null);
				compilationResult.recordModifications();
				return compilationResult;	
		}
		return null;
	}
	/**
	 * retrieve the C compilation Unit
	 * @return
	 */
	public static IASTTranslationUnit getCCompilationUnit(IEditorPart IEditorPart){
		ITranslationUnit translationUnit = CUIPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(IEditorPart.getEditorInput());
		try {
//			IASTTranslationUnit astUnit = translationUnit.getAST(null,ITranslationUnit.AST_SKIP_ALL_HEADERS);
			IASTTranslationUnit astUnit = translationUnit.getAST();
			return astUnit;
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String complexTypeSeparator = "*";
	
	public static TreeObject readInAllCMTypesToTreeObject(IFile fileInput){
		return readInAllCMTypesToTreeObject(fileInput.getProject());
	}
	
	public static TreeObject readInAllCMTypesToTreeObject(IProject iproject){
		TreeObject invisibleRootTreeObject = new TreeObject("invisible");
		File treeIndexFile = RWTSystemUtil.getTreeIndexFile(iproject);
		invisibleRootTreeObject = TreeObject.readInTreeObject(iproject, treeIndexFile);
//		if(!treeIndexFile.exists()){
//			try {
//				TreeObject cmtypeListTO = new TreeObject(TreeObject.treeObjectTopName);
//				invisibleRootTreeObject.addChild(cmtypeListTO);				
//				treeIndexFile.createNewFile();
//				TreeObject.writeOutTreeObject(invisibleRootTreeObject, treeIndexFile);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}else{
//			invisibleRootTreeObject = TreeObject.readInTreeObject(iproject, treeIndexFile);	
//		}
		
		
//		if(location !=null){
//			File cMTypeDir = new File(location.toString() + RWTSystemUtil.PathSeparator + RWTSystemUtil.CMTypesFolder);
//			File[] topLevelFolders = cMTypeDir.listFiles();
//			for(File topLevelFolder:topLevelFolders ){
//				TreeObject topLevelTO = new TreeObject(topLevelFolder.getName());
//				File treeIndexFile = new File(topLevelFolder.getAbsolutePath() + RWTSystemUtil.PathSeparator + TreeObject.treeIndexFileName);
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
	
	public static RWType getCMTypeFromTypeName(IProject currentProject, String annotatedTypeName){
		if(annotatedTypeName!=null && annotatedTypeName.length()>0){
			File cmTypeFile = RWTSystemUtil.getCMTypeFile(currentProject, annotatedTypeName);
			if((cmTypeFile!= null) && (cmTypeFile.exists())){
				return RWType.readInCorrespondenceType(cmTypeFile);
			}
		}
		return null;
	}
	
	public static RWType getCMTypeFromTreeObject(IProject currentProject, TreeObject selectedTO){
		File cmTypeFile = RWTSystemUtil.getCMTypeFile(currentProject, selectedTO);
		if((cmTypeFile!= null) && (cmTypeFile.exists())){
			return RWType.readInCorrespondenceType(cmTypeFile);
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

class NoRWTSystemFoundException extends Exception{
	
}