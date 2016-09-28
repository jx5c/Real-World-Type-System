package cmtypechecker.popup.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import cmtypechecker.util.CMModelUtil;

public class ShowConfigActionInNavigator  implements IObjectActionDelegate{
	private ISelection selection;
	private Shell shell;
	
	@Override
	public void run(IAction arg0) {
		IProject iProject = (IProject) (((IStructuredSelection) selection).getFirstElement());
//		IProject iProject = ((IFile) ((IStructuredSelection) selection).getFirstElement()).getProject();
		Object location = CMModelUtil.readPropertyFromConfigFile(iProject.getName());
		if(location!=null){
			MessageDialog.openInformation(
					shell,
					"Correspondence Model References Location",
					"The location is at: "+location.toString());
			return ;
		}
		MessageDialog.openInformation(
				shell,
				"Correspondence Model References Location",
				"Error! Please setup the location for Correspondence Model first");
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
