package cmtypechecker.popup.actions;


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
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import cmtypechecker.util.ActivePart;
import cmtypechecker.util.CMModelUtil;
import cmtypechecker.util.DiagnosticMessage;
import cmtypechecker.util.interval.IntervalAnalysisVisitor;
import cmtypechecker.views.provider.TreeObject;
import rwtchecker.perspective.RWTCheckerPerspective;
import rwtchecker.views.RWTRulesView;
import rwtchecker.views.RWTView;
import rwtchecker.views.DiagnoseView;

public class InspectionModeActionInJavaEditor implements IEditorActionDelegate {
	
	protected Color red;
	
	protected Shell shell;
	protected IFile ifile;
	protected IFileEditorInput thisFileEditorInput;
	
	protected CompilationUnit compilationResult;
	
	protected RWTView cmTypeView;
	protected DiagnoseView diagnoseView = null;
	
	protected StyleRange[] defaultRange = null;
	protected StyledText textControl = null;
	protected IEditorPart currentJavaEditor;
	
	private ArrayList<DiagnosticMessage> analysisResults = new ArrayList<DiagnosticMessage>();

	public InspectionModeActionInJavaEditor() {
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
			diagnoseView = (DiagnoseView)(page.findView(DiagnoseView.ID));
		} catch (PartInitException e) {
			e.printStackTrace();
		} catch (WorkbenchException e) {
			e.printStackTrace();
		}
		
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		
		if(ifile != null){
			ICompilationUnit icompilationUnit = JavaCore.createCompilationUnitFrom(ifile);
			parser.setSource(icompilationUnit); // set source
			parser.setResolveBindings(true); // we need bindings later on
			compilationResult = (CompilationUnit) parser.createAST(null);
			
			TreeObject treeObject = CMModelUtil.readInAllCMTypesToTreeObject(ifile);
			cmTypeView.getTreeViewer().setInput(treeObject);
			cmTypeView.setTypeChecked(false);
			intervalAnalysis(compilationResult);
			showMessage("Interval Analysis Finished");
		}
	}
	
	protected void intervalAnalysis(CompilationUnit compilationResult){		
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
	
		IntervalAnalysisVisitor IAVisitor = new IntervalAnalysisVisitor(compilationResult);
		compilationResult.accept(IAVisitor);
		analysisResults = IAVisitor.getAnalysisReports();
		
		TextOperationAction fExpandAll = new TextOperationAction(FoldingMessages.getResourceBundle(), "Projection.ExpandAll.", (ITextEditor) currentJavaEditor, ProjectionViewer.EXPAND_ALL); //$NON-NLS-1$
		fExpandAll.setActionDefinitionId(IFoldingCommandIds.FOLDING_EXPAND_ALL);
		fExpandAll.run();
		
		diagnoseView.setTextControl(textControl);
		diagnoseView.getErrorTableViewer().setInput(analysisResults);
	}
	
	
	@Override
	public void setActiveEditor(IAction arg0, IEditorPart editorPart) {
		if(editorPart!=null){
			shell = editorPart.getSite().getShell();
			IEditorInput editorInput = editorPart.getEditorInput();
			if (editorInput instanceof IFileEditorInput) {
				this.thisFileEditorInput = (IFileEditorInput) editorInput;
				this.ifile = InspectionModeActionInJavaEditor.this.thisFileEditorInput.getFile();
			}
		}
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
	}

	
	private void showMessage(String message) {
		MessageDialog.openInformation(
				this.shell,
			"Interval Analysis",
			message);
	}
}
