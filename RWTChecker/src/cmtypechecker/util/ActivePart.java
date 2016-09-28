package cmtypechecker.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class ActivePart {
		
		private ActivePart(){}
		public static IEditorPart getActiveEditor(){
			IWorkbenchWindow currentWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if(currentWindow!=null){
				IWorkbenchPage activePage = currentWindow.getActivePage();
				if(activePage!= null){
					return activePage.getActiveEditor();
				}
			}
			return null;
		}
		
		public static IWorkbenchWindow getActiveWindow(){
			IWorkbenchWindow currentWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			return currentWindow;
		}
		
		public static Shell getWorkbenchShell() {
			return PlatformUI.getWorkbench().getDisplay().getActiveShell();
		}
		
		public static IFile getFileOfActiveEditror(){
			if(getActiveEditor()!= null){
				IEditorInput editorInput = getActiveEditor().getEditorInput();
				if (editorInput instanceof IFileEditorInput) {
					return ((IFileEditorInput) editorInput).getFile();
				}	
			}
			return null;
		}
		
		public static IPerspectiveDescriptor getActivePerspective(){
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if(page!=null){
				return page.getPerspective();
			}
			return null;
		}
		
		public static IViewPart getSpecificView(String viewID){
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if(page!=null){
				try {
					return page.showView(viewID);
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}
			return null;
			
		}

}
