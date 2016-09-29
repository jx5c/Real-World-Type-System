package rwtchecker.popup.actions;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import rwtchecker.annotation.FileAnnotations;
import rwtchecker.util.CMModelUtil;

public class GetStatisticActionInNavigator  implements IObjectActionDelegate{
	private ISelection selection;
	private Shell shell;
	
	@Override
	public void run(IAction arg0) {
		IProject iProject = (IProject) (((IStructuredSelection) selection).getFirstElement());
		Object location = CMModelUtil.readPropertyFromConfigFile(iProject.getName());
		if(location==null){
			MessageDialog.openInformation(
					shell,
					"Correspondence Model References Location",
					"Error! Please setup the real-world type folders for the project first");
			return ;
		}
		ArrayList<FileAnnotations> allAnnotations = CMModelUtil.getAllFileAnntationsForProject(iProject);
		int totalAnnoCount = 0;
		HashSet<String> rwtsUsed = new HashSet<String>();
		for(FileAnnotations anno : allAnnotations ){
			totalAnnoCount += anno.getAllAnnotationsCount();
			rwtsUsed.addAll(anno.getRWTAccessed());
		}
		for(String rwtUsed : rwtsUsed){
			System.out.println(rwtUsed);
		}
		MessageDialog.openInformation(shell, "Statistic of real-world type system", "The number of real-world entities:"+rwtsUsed.size()+"; The number of interpretations: "+totalAnnoCount);
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
