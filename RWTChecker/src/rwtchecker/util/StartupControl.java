package rwtchecker.util;

import java.io.File;
import java.io.IOException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import rwtchecker.annotation.FileAnnotations;
import rwtchecker.rwt.RWType;

public class StartupControl implements IStartup{

	@Override
	public void earlyStartup() {
		// TODO Auto-generated method stub
	    final IWorkbench workbench = PlatformUI.getWorkbench();
	    workbench.getDisplay().asyncExec(new Runnable() {
	    	   public void run() {
	    	     IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
	    	     if (window != null) {
	    	       // do something
	    	    	IWorkbenchPage activePage = window.getActivePage(); 
	 	            activePage.addPartListener(generateIPartListener2());
	    	     }
	    	   }
	    	 });
	    workbench.addWindowListener(generateWindowListener());
	}

	private IWindowListener generateWindowListener() {
	    return new IWindowListener() {
	        @Override
	        public void windowOpened(IWorkbenchWindow window) {
	            IWorkbenchPage activePage = window.getActivePage(); 
	            activePage.addPartListener(generateIPartListener2());
	        }

	        @Override
	        public void windowDeactivated(IWorkbenchWindow window) {}

	        @Override
	        public void windowClosed(IWorkbenchWindow window) {}

	        @Override
	        public void windowActivated(IWorkbenchWindow window) {}

	    };
	}
	
	private IPartListener2 generateIPartListener2() 
	{
	    return new IPartListener2() {

	        private void checkPart(IWorkbenchPartReference partRef) {
	        IWorkbenchPart part = partRef.getPart(false);
	            if (part instanceof IEditorPart)
	            {
	                IEditorPart editor = (IEditorPart) part;
	                IEditorInput input = editor.getEditorInput();
	                if (editor instanceof ITextEditor && input instanceof FileEditorInput){
	                    editor.getSite().getSelectionProvider().addSelectionChangedListener(new ISelectionChangedListener() {							
							@Override
							public void selectionChanged(SelectionChangedEvent event) {
								getRWTOfSelection(event.getSelection());
							}
						});
	                }
	            }
	        }

	        @Override
	        public void partOpened(IWorkbenchPartReference partRef){
	            checkPart(partRef);
	        }

	        @Override
	        public void partInputChanged(IWorkbenchPartReference partRef){
	            checkPart(partRef);
	        }           

	        @Override
	        public void partVisible(IWorkbenchPartReference partRef){
	        }

	        @Override
	        public void partHidden(IWorkbenchPartReference partRef) {}

	        @Override
	        public void partDeactivated(IWorkbenchPartReference partRef)  {}

	        @Override
	        public void partClosed(IWorkbenchPartReference partRef) {}

	        @Override
	        public void partBroughtToTop(IWorkbenchPartReference partRef) {}

	        @Override
	        public void partActivated(IWorkbenchPartReference partRef) {
	        	checkPart(partRef);
	        }
	    };
	}
	
	private void getRWTOfSelection(ISelection iSelection){
		if(iSelection != null){
			if(iSelection instanceof ITextSelection){
				ITextSelection textSelection = (ITextSelection)iSelection;
				CompilationUnit compilationResult = RWTSystemUtil.getCurrentCompliationUnit();
				if(compilationResult!=null){
					ASTNode node = NodeFinder.perform(compilationResult.getRoot(),textSelection.getOffset(), textSelection.getLength());
					if(node != null){
						RWType rwtype = FileAnnotations.lookupRWTByVarName(node, compilationResult);
						if(rwtype!=null){
							RWTHover.rwtTypeInfo = rwtype.getTypeName();
							RWTHover.currentSelection = node.toString();
						}
					}
				}
			}
		}
		RWTHover.rwtTypeInfo = "";
	}
}
