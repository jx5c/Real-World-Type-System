package cmtypechecker.popup.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import cmtypechecker.CMRules.CMTypeRulesManager;
import cmtypechecker.fpclose.FPTree;
import cmtypechecker.typechecker.ExtractPatternVisitor;
import cmtypechecker.util.CMModelUtil;
import cmtypechecker.views.provider.TreeObject;

public class ExtractPatternsActionInNavigator  implements IObjectActionDelegate{
	private ISelection selection;
	private Shell shell;
	
	@Override
	public void run(IAction arg0) {
		IProject iProject = (IProject) (((IStructuredSelection) selection).getFirstElement());
		
		//get source code folder
		if (iProject == null) 
			return ;
	    List<IJavaElement> ret = new ArrayList<IJavaElement>();
	    IJavaProject javaProject = JavaCore.create(iProject);
	    ArrayList<String> javaDirs = new ArrayList<String>();
	    try {
	            IPackageFragmentRoot[] packageFragmentRoot = javaProject.getAllPackageFragmentRoots();
	            for (int i = 0; i < packageFragmentRoot.length; i++){
	                if (packageFragmentRoot[i].getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT && !packageFragmentRoot[i].isArchive())
	                {
	                	ret.add(packageFragmentRoot[i]);
	                	//String relativePath = packageFragmentRoot[i].getResolvedClasspathEntry().getPath().toString();
	                	//System.out.println(relativePath);
	                	//String javadir = ResourcesPlugin.getWorkspace().getRoot().findMember(relativePath).getLocation().toString(); // gives out system file path 
	                	//javaDirs.add(javadir);

	                }
	            }
	            IPackageFragment[] packages = javaProject.getPackageFragments();
            	//reset the data source file
            	new File("e:\\testData.txt").delete();
	            for (IPackageFragment packgeFragment : packages){
	            	//traverse each java source file, get itemsets 
	            	for (final ICompilationUnit compilationUnit : packgeFragment.getCompilationUnits()) {
	            		System.out.println("------------"+compilationUnit.getElementName()+"------------");
	          	       // now check if compilationUnit.exits
	            		ASTParser parser = ASTParser.newParser(AST.JLS3);
	            		parser.setKind(ASTParser.K_COMPILATION_UNIT);
	            		CMTypeRulesManager manager = CMTypeRulesManager.getManagerForCurrentProject();
	            		
	            		if(compilationUnit.exists()){
	            			parser.setSource(compilationUnit); // set source
	            			parser.setResolveBindings(true); // we need bindings later on
	            			CompilationUnit compilationResult = (CompilationUnit) parser.createAST(null);
	            			ExtractPatternVisitor extractPatternVisitor = new ExtractPatternVisitor(manager, compilationResult);
	            			compilationResult.accept(extractPatternVisitor);
	            		}
	            		System.out.println("");
	            		System.out.println("");
	            	}
	            }
            	
         	}
	         catch (JavaModelException e) {
	            e.printStackTrace();
	        }
	        
         System.out.println(javaDirs);
                  
         ArrayList<File> resultFiles = new ArrayList<File>();
         for(String filedir : javaDirs){
        	 this.walk(filedir, resultFiles);
 
         }
         FPTree fptree = new FPTree();
         fptree.setMinSuport(3);
         List<List<String>> transRecords = fptree
                 .readDataRocords(new String[]{"e:\\develop\\testing_data\\1.txt"});
         fptree.FPGrowth(transRecords, null);	
         
	}
	

    private void walk( String path, ArrayList<File> resultFiles) {
        File root = new File( path );
        File[] list = root.listFiles();

        for ( File f : list ) {
            if ( f.isDirectory() ) {
                walk( f.getAbsolutePath(), resultFiles);
            }
            else {
            	resultFiles.add(f.getAbsoluteFile());
            }
        }
    }

    
	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
		this.selection = arg1;
	}

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		shell = arg1.getSite().getShell();
	}

}
