package rwtchecker.popup.actions;

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

import rwtchecker.popup.actions.inference.TypePropagationVisitor;
import rwtchecker.typechecker.AnnotationPatternMinerVisitor;
import rwtchecker.typechecker.TypeCheckingVisitor;

public class AnnotationPatternActionInJavaEditor implements IEditorActionDelegate {
	
	protected Shell shell;
	protected IFile ifile;
	protected IFileEditorInput thisFileEditorInput;
	
	protected CompilationUnit compilationResult;
	
	public AnnotationPatternActionInJavaEditor() {
		super();
	}
	
	public void run(IAction action) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		if(ifile.exists()){
			ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(ifile);
			parser.setSource(icompilationUnit); // set source
			parser.setResolveBindings(true); // we need bindings later on
			CompilationUnit compilationResult = (CompilationUnit) parser.createAST(null);
			AnnotationPatternMinerVisitor annotationPatternMinerVisitor = new AnnotationPatternMinerVisitor(compilationResult);
			compilationResult.accept(annotationPatternMinerVisitor);
		}
		showMessage("Annotation pattern mining ends here.");
	}
	
	@Override
	public void setActiveEditor(IAction arg0, IEditorPart editorPart) {
		if(editorPart!=null){
			shell = editorPart.getSite().getShell();
			IEditorInput editorInput = editorPart.getEditorInput();
			if (editorInput instanceof IFileEditorInput) {
				this.thisFileEditorInput = (IFileEditorInput) editorInput;
				this.ifile = AnnotationPatternActionInJavaEditor.this.thisFileEditorInput.getFile();
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
