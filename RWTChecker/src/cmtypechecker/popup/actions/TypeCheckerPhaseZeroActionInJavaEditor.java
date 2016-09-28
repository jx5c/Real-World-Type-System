package cmtypechecker.popup.actions;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.ui.actions.FoldingMessages;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.editors.text.IFoldingCommandIds;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextOperationAction;

import cmtypechecker.CM.CMType;
import cmtypechecker.CMRules.CMTypeRulesManager;
import cmtypechecker.annotation.FileAnnotations;
import cmtypechecker.typechecker.CandidateRuleVisitor;
import cmtypechecker.typechecker.CandidateRuleVisitor2;
import cmtypechecker.typechecker.CommentVisitor;
import cmtypechecker.typechecker.NewTypeCheckerVisitor;
import cmtypechecker.util.ActivePart;
import cmtypechecker.util.CMModelUtil;
import cmtypechecker.util.DiagnosticMessage;
import cmtypechecker.views.provider.TreeObject;
import rwtchecker.perspective.RWTCheckerPerspective;
import rwtchecker.views.RWTRulesView;
import rwtchecker.views.RWTView;
import rwtchecker.views.DiagnoseView;

public class TypeCheckerPhaseZeroActionInJavaEditor implements IEditorActionDelegate {
	
	protected Color red;
	
	protected Shell shell;
	protected IFile ifile;
	protected IFileEditorInput thisFileEditorInput;
	
	protected CompilationUnit compilationResult;
	
	protected RWTView cmTypeView;
	protected RWTRulesView cmTypeOperationView;
	protected DiagnoseView diagnoseView = null;
	
	protected StyleRange[] defaultRange = null;
	protected StyledText textControl = null;
	protected IEditorPart currentJavaEditor;
	protected NewTypeCheckerVisitor typeCheckingVisitor;
	
	private ArrayList<DiagnosticMessage> CMTypeCheckingResults = new ArrayList<DiagnosticMessage>();

	public TypeCheckerPhaseZeroActionInJavaEditor() {
		super();
	}
	
	public void run(IAction action) {
		currentJavaEditor = ActivePart.getActiveEditor();
		textControl = (StyledText) currentJavaEditor.getAdapter(Control.class);
		this.defaultRange = textControl.getStyleRanges();
		
		Display display = shell.getDisplay();
	    this.red = display.getSystemColor(SWT.COLOR_RED);
	    
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			PlatformUI.getWorkbench().showPerspective(RWTCheckerPerspective.ID, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
			cmTypeView = (RWTView)page.findView(RWTView.ID);
			cmTypeOperationView = (RWTRulesView)page.findView(RWTRulesView.ID);
			diagnoseView = (DiagnoseView)(page.findView(DiagnoseView.ID));
		} catch (PartInitException e) {
			e.printStackTrace();
		} catch (WorkbenchException e) {
			e.printStackTrace();
		}
		
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
//		IFile activeFile = ActivePart.getFileOfActiveEditror();
		
		if(ifile != null){
			ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(ifile);
			parser.setSource(icompilationUnit); // set source
			parser.setResolveBindings(true); // we need bindings later on
			compilationResult = (CompilationUnit) parser.createAST(null);
			
			TreeObject treeObject = CMModelUtil.readInAllCMTypesToTreeObject(ifile);
			cmTypeView.getTreeViewer().setInput(treeObject);
			cmTypeView.setTypeChecked(false);
			typeChecking(compilationResult);
			if(CMTypeCheckingResults.size() == 0){
				showMessage("Good News: no error has been found");
			}else{
				showMessage("Some error has been found");
			}
		}

	}
	
	protected void typeChecking(CompilationUnit compilationResult){
		CMTypeRulesManager manager = CMTypeRulesManager.getManagerForCurrentProject();
		cmTypeOperationView.setManager(manager);
		cmTypeOperationView.getTableViewer().setInput(manager);
		
		BufferedReader infile = null;
		ArrayList<String> contents = new ArrayList<String>();
		String line = "";
		try {
			infile = new BufferedReader(new FileReader(ifile.getRawLocation().toFile()));
			while((line = infile.readLine())!= null){
				contents.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] sourceList = contents.toArray(new String[contents.size()]);
	
		Map<Integer, ArrayList<String>> varsCommentsMap = new HashMap<Integer, ArrayList<String>>();
		Map<Integer, ArrayList<String>> funcCommentsMap = new HashMap<Integer, ArrayList<String>>();
		
		for (Comment comment : (List<Comment>) compilationResult.getCommentList()) {
			CommentVisitor thisCommentVisitor = new CommentVisitor(compilationResult, sourceList);
            comment.accept(thisCommentVisitor);
            if(thisCommentVisitor.isDefComment()){
            	varsCommentsMap.put(thisCommentVisitor.getLineCourt(), thisCommentVisitor.getCommentConents());
            }
            if(thisCommentVisitor.isFuncComment()){
            	funcCommentsMap.put(thisCommentVisitor.getLineCourt(), thisCommentVisitor.getCommentConents());
            }
        }
	
	    typeCheckingVisitor = new NewTypeCheckerVisitor(manager, compilationResult, varsCommentsMap, funcCommentsMap);
		compilationResult.accept(typeCheckingVisitor);
		CMTypeCheckingResults = typeCheckingVisitor.getErrorReports();
		
		//generate candidate rules
		CandidateRuleVisitor2 candidateRuleVisitor = new CandidateRuleVisitor2(manager, compilationResult, varsCommentsMap, funcCommentsMap);
		compilationResult.accept(candidateRuleVisitor);
		
		TextOperationAction fExpandAll = new TextOperationAction(FoldingMessages.getResourceBundle(), "Projection.ExpandAll.", (ITextEditor) currentJavaEditor, ProjectionViewer.EXPAND_ALL); //$NON-NLS-1$
		fExpandAll.setActionDefinitionId(IFoldingCommandIds.FOLDING_EXPAND_ALL);
		fExpandAll.run();
		
		cmTypeView.setTypeChecked(true);
		diagnoseView.setTextControl(textControl);
		diagnoseView.getErrorTableViewer().setInput(CMTypeCheckingResults);
		
	}
	
	@Override
	public void setActiveEditor(IAction arg0, IEditorPart editorPart) {
		if(editorPart!=null){
			shell = editorPart.getSite().getShell();
			IEditorInput editorInput = editorPart.getEditorInput();
			if (editorInput instanceof IFileEditorInput) {
				this.thisFileEditorInput = (IFileEditorInput) editorInput;
				this.ifile = TypeCheckerPhaseZeroActionInJavaEditor.this.thisFileEditorInput.getFile();
			}
		}
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
	}

	
	private void showMessage(String message) {
		MessageDialog.openInformation(
				this.shell,
			"New Type Checking Schema",
			message);
	}
}
