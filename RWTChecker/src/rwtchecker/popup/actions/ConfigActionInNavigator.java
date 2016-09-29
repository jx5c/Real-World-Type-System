package rwtchecker.popup.actions;



import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import rwtchecker.util.CMModelUtil;


public class ConfigActionInNavigator implements IObjectActionDelegate {
	private ISelection selection;
	private Shell shell;
	/**
	 * Constructor for Action1.
	 */
	public ConfigActionInNavigator() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		IProject iProject = (IProject) ((IStructuredSelection) selection).getFirstElement();
        DirectoryDialog dlg = new DirectoryDialog(shell);
        dlg.setText("Correspondence Model Location");
        // Customizable message displayed in the dialog
        dlg.setMessage("Select the Directory for existing Correspondence Model");
        String dir = dlg.open();
        System.out.println("the opened dir is: "+dir);
        if (dir != null) {
        	CMModelUtil.storePropertyToConfigFile(iProject.getName(), dir);
        	String conceptDir = dir + CMModelUtil.PathSeparator + CMModelUtil.ConceptDefinitionFolder;
        	if(!new File(conceptDir).exists()){
        		new File(conceptDir).mkdir();	
        	}
        	String cmTypeDir = dir + CMModelUtil.PathSeparator + CMModelUtil.CMTypesFolder;
        	if(!new File(cmTypeDir).exists()){
        		new File(cmTypeDir).mkdir();
        	}
        	String annotationDir = dir + CMModelUtil.PathSeparator + CMModelUtil.annotationFolder;
        	if(!new File(annotationDir).exists()){
        		new File(annotationDir).mkdir();
        	}
        }
	}
	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

}
