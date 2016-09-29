package rwtchecker.popup.actions;



import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

import rwtchecker.annotation.FileAnnotations;

public class DisplayAnnotationActionInJavaEditor implements IEditorActionDelegate {
	
	public DisplayAnnotationActionInJavaEditor() {
		super();
	}
	
	public void run(IAction action) {
		FileAnnotations.changeAnnotationsStatus();
	}
	

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
	}

	@Override
	public void setActiveEditor(IAction arg0, IEditorPart arg1) {
	}

}
