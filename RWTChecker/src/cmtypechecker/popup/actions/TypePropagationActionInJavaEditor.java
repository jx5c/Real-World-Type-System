package cmtypechecker.popup.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

import cmtypechecker.popup.actions.inference.TypePropagationVisitor;
import cmtypechecker.typechecker.TypeCheckingVisitor;

public class TypePropagationActionInJavaEditor implements IEditorActionDelegate {
	
	protected Shell shell;
	protected IFile ifile;
	protected IFileEditorInput thisFileEditorInput;
	
	protected CompilationUnit compilationResult;
	
	protected TypeCheckingVisitor typeCheckingVisitor;
	
	public TypePropagationActionInJavaEditor() {
		super();
	}
	
	//read the inference rules from a file; save this option for later use
	private static boolean external_file = false;
	
	public void run(IAction action) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		if(ifile.exists()){
			ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(ifile);
			parser.setSource(icompilationUnit); // set source
			parser.setResolveBindings(true); // we need bindings later on
			CompilationUnit compilationResult = (CompilationUnit) parser.createAST(null);
			if(external_file){
				TypePropagationVisitor typePropagationVisitor = new TypePropagationVisitor(compilationResult);
				compilationResult.accept(typePropagationVisitor);
			}else{
				TypePropagationVisitor typePropagationVisitor = new TypePropagationVisitor(compilationResult);
				compilationResult.accept(typePropagationVisitor);
			}
			
		}
		showMessage("Type Propagation ends here.");
	}
	
	@Override
	public void setActiveEditor(IAction arg0, IEditorPart editorPart) {
		if(editorPart!=null){
			shell = editorPart.getSite().getShell();
			IEditorInput editorInput = editorPart.getEditorInput();
			if (editorInput instanceof IFileEditorInput) {
				this.thisFileEditorInput = (IFileEditorInput) editorInput;
				this.ifile = TypePropagationActionInJavaEditor.this.thisFileEditorInput.getFile();
			}
		}
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
	}

	
	private void showMessage(String message) {
		MessageDialog.openInformation(
				this.shell,
			"Type propagation",
			message);
	}
}
