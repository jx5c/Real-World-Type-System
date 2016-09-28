package cmtypechecker.popup.actions;



import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

import cmtypechecker.annotation.FileAnnotations;
import cmtypechecker.util.CMModelUtil;

public class DisplayRWTCountActionInJavaEditor implements IEditorActionDelegate {
	protected Shell shell;
	protected IFile currentFile;
	protected IFileEditorInput thisFileEditorInput;
	protected CompilationUnit compilationResult;
	
	public DisplayRWTCountActionInJavaEditor() {
		super();
	}
	
	public void run(IAction action) {
		File annotationFile = CMModelUtil.getAnnotationFile(currentFile);
		if(annotationFile!= null && annotationFile.exists()){
			FileAnnotations fileAnnotations = FileAnnotations.loadFromXMLFile(annotationFile);
			if(fileAnnotations == null){
				return;
			}
			showMessage(""+fileAnnotations.getAllAnnotationsCount());
		}
	}
	
	@Override
	public void setActiveEditor(IAction arg0, IEditorPart editorPart) {
		if(editorPart!=null){
			shell = editorPart.getSite().getShell();
			IEditorInput editorInput = editorPart.getEditorInput();
			if (editorInput instanceof IFileEditorInput) {
				this.thisFileEditorInput = (IFileEditorInput) editorInput;
				this.currentFile = thisFileEditorInput.getFile();
			}
		}
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
	}
	
	private void showMessage(String message) {
		MessageDialog.openInformation(
				this.shell,
			"The number of annotations in this file is ",
			message);
	}
}
