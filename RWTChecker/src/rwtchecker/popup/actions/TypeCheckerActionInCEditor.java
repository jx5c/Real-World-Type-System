package rwtchecker.popup.actions;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
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

import rwtchecker.perspective.RWTCheckerPerspective;
import rwtchecker.rwtrules.RWTypeRulesManager;
import rwtchecker.typechecker.CandidateRuleVisitorForJavaDoc;
import rwtchecker.typechecker.TypeCheckingVisitor;
import rwtchecker.typechecker.C.CTypeCheckerVisitor;
import rwtchecker.util.ActivePart;
import rwtchecker.util.RWTSystemUtil;
import rwtchecker.util.DiagnosticMessage;
import rwtchecker.views.RWTRulesView;
import rwtchecker.views.RWTView;
import rwtchecker.views.provider.TreeObject;
import rwtchecker.views.DiagnoseView;

public class TypeCheckerActionInCEditor implements IEditorActionDelegate {
	
	protected Color red;
	
	protected Shell shell;
	protected IFile ifile;
	protected IFileEditorInput thisFileEditorInput;
	protected IEditorPart editorPart;
	
	protected RWTView rwtTypeView;
	protected RWTRulesView cmTypeOperationView;
	protected DiagnoseView diagnoseView = null;
	
	protected StyleRange[] defaultRange = null;
	protected StyledText textControl = null;
	protected IEditorPart currentJavaEditor;
	protected CTypeCheckerVisitor cTypeCheckerVisitor;
	
	private ArrayList<DiagnosticMessage> CMTypeCheckingResults = new ArrayList<DiagnosticMessage>();

	public TypeCheckerActionInCEditor() {
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
			rwtTypeView = (RWTView)page.findView(RWTView.ID);
			cmTypeOperationView = (RWTRulesView)page.findView(RWTRulesView.ID);
			diagnoseView = (DiagnoseView)(page.findView(DiagnoseView.ID));
		} catch (PartInitException e) {
			e.printStackTrace();
		} catch (WorkbenchException e) {
			e.printStackTrace();
		}
		
		if(this.editorPart != null){
			IASTTranslationUnit astUnit = RWTSystemUtil.getCCompilationUnit(this.editorPart);
			TreeObject treeObject = RWTSystemUtil.readInAllCMTypesToTreeObject(ifile);
			rwtTypeView.getTreeViewer().setInput(treeObject);
			rwtTypeView.setTypeChecked(false);

			typeChecking(astUnit);
			
			if(CMTypeCheckingResults.size() == 0){
				showMessage("no error has been found");
			}else{
				showMessage("Some error has been found");
			}
		}

	}
	
	protected void typeChecking(IASTTranslationUnit astUnit){
		RWTypeRulesManager manager = RWTypeRulesManager.getManagerForCurrentProject();
		cmTypeOperationView.setManager(manager);
		cmTypeOperationView.getTableViewer().setInput(manager);
		
		CTypeCheckerVisitor typeCheckingVisitor = new CTypeCheckerVisitor(manager, astUnit, false, this.ifile);
		astUnit.accept(typeCheckingVisitor);
		CMTypeCheckingResults = typeCheckingVisitor.getErrorReports();
		
		TextOperationAction fExpandAll = new TextOperationAction(FoldingMessages.getResourceBundle(), "Projection.ExpandAll.", (ITextEditor) currentJavaEditor, ProjectionViewer.EXPAND_ALL); //$NON-NLS-1$
		fExpandAll.setActionDefinitionId(IFoldingCommandIds.FOLDING_EXPAND_ALL);
		fExpandAll.run();
		
		rwtTypeView.setTypeChecked(true);
		diagnoseView.setTextControl(textControl);
		diagnoseView.getErrorTableViewer().setInput(CMTypeCheckingResults);
	}
	
	@Override
	public void setActiveEditor(IAction arg0, IEditorPart editorPart) {
		if(editorPart!=null){
			shell = editorPart.getSite().getShell();
			IEditorInput editorInput = editorPart.getEditorInput();
			if (editorInput instanceof IFileEditorInput) {
				this.editorPart = editorPart;
				this.thisFileEditorInput = (IFileEditorInput) editorInput;
				this.ifile = this.thisFileEditorInput.getFile();
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
