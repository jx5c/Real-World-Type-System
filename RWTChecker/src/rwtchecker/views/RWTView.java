package rwtchecker.views;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import rwtchecker.annotation.AnnotationVisitor;
import rwtchecker.annotation.FileAnnotations;
import rwtchecker.annotation.RWTAnnotation;
import rwtchecker.concept.ConceptDetail;
import rwtchecker.rwt.RWType;
import rwtchecker.rwtrules.RWTypeRulesManager;
import rwtchecker.typechecker.NewTypeCheckerVisitor;
import rwtchecker.typechecker.RevisionVisitor;
import rwtchecker.util.ActivePart;
import rwtchecker.util.RWTSystemUtil;
import rwtchecker.util.DiagnosticMessage;
import rwtchecker.util.XMLGeneratorForTypes;
import rwtchecker.views.provider.CMAttributeTableContentProvider;
import rwtchecker.views.provider.CMAttributeTablelLabelProvider;
import rwtchecker.views.provider.CMViewTreeContentProvider;
import rwtchecker.views.provider.CMViewTreeViewLabelProvider;
import rwtchecker.views.provider.TreeObject;
import rwtchecker.wizards.ManageRWTypeWizard;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.editors.text.IFoldingCommandIds;
import org.eclipse.ui.part.*;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.ui.actions.FoldingMessages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.text.folding.DefaultJavaFoldingStructureProvider;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class RWTView extends ViewPart {

	public static final String ID = "rwtchecker.views.rwtView";

	private TableViewer typeAttributeViewer;

	private TreeViewer existingTypesTreeViewer;
	
	//there two are used accompany with method declaration, and the correspondence types are not determined; (generic types)
	private Action refreshTreeActionOnTreeViewer;
	private Action bindingCMTypeActionInTreeViewer;
	private Action persistentAnnotationsInTreeViewer;
	private Action showAnnotationsActionInTreeViewer;
	private Action markAllVariablesActionInTreeViewer;
	private Action generateAssertionsActionInTreeViewer;
//	private Action markPrimaryActionInTreeViewer;
	private Action manageCorrespondenceTypeInTreeViewer;
	
	private Action saveTypesToXMLFileInTreeViewer;
	
	private Action clickActionOnTreeViewer;

	
	private SashForm sashFormMain;
	private SashForm sashFormBottom;
	private SashForm sashFormTop;
	
	private CMViewTreeContentProvider treeContentProvider = new CMViewTreeContentProvider();

	private ConceptDetailView conceptDetailView = null;
	private DiagnoseView diagnoseView = null;
	
	private ArrayList<DiagnosticMessage> CMTypeCheckingResults = new ArrayList<DiagnosticMessage>();
	
	private boolean typeChecked = false;
	
	private TreeObject thisSelectedTreeObject;
	private TreeObject cmtypeTreeRootObject;
	
	private RWType selectedNewCMType;
	
	private IProject currentProject;
	
	private IEditorPart currentJavaEditor;
	private StyledText currentTextControl;
	private Color redColor;
	private Color greenColor;
	
	private boolean inspectionMode = false;
	
	public RWTView() {
	}

	public TreeViewer getTreeViewer() {
		return existingTypesTreeViewer;
	}

	public void createPartControl(Composite parent) {	
        sashFormMain = new SashForm(parent, SWT.VERTICAL | SWT.NULL);
        sashFormTop = new SashForm(sashFormMain, SWT.HORIZONTAL | SWT.NULL);

		existingTypesTreeViewer = new TreeViewer(sashFormTop, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		existingTypesTreeViewer.setContentProvider(treeContentProvider);
		existingTypesTreeViewer.setLabelProvider(new CMViewTreeViewLabelProvider());
		existingTypesTreeViewer.setInput(null);
		existingTypesTreeViewer.setAutoExpandLevel(4);
		
		sashFormBottom = new SashForm(sashFormMain, SWT.HORIZONTAL | SWT.NULL);
		//correspondence type display here;
		typeAttributeViewer = new TableViewer(sashFormBottom, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        createAttributeTableColumns(typeAttributeViewer);
		typeAttributeViewer.setContentProvider(new CMAttributeTableContentProvider());
		typeAttributeViewer.setLabelProvider(new CMAttributeTablelLabelProvider());
		typeAttributeViewer.setInput(null);
        
        // Set the size of the split pane
        sashFormMain.setWeights(new int [] {40, 60});
        
        
        IFile currentFile = ActivePart.getFileOfActiveEditror();
        if(currentFile != null){
        	this.currentProject = currentFile.getProject();	
        }
        currentJavaEditor = ActivePart.getActiveEditor();
        if(currentJavaEditor!=null){
        	currentTextControl = (StyledText) currentJavaEditor.getAdapter(Control.class);	
        }
        
        redColor = this.getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_RED);
        greenColor = this.getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_GREEN);
        
		makeActions();
		hookContextMenu();
		hookClickAction();
		contributeToActionBars();
		
		try {
			conceptDetailView = (ConceptDetailView)(RWTView.this.getSite().getPage().showView(ConceptDetailView.ID));
			diagnoseView = (DiagnoseView)(RWTView.this.getSite().getPage().showView(DiagnoseView.ID));
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		
	}
	
	private void createAttributeTableColumns(final TableViewer viewer) {
		Table table = viewer.getTable();
		String[] titles = { "Attribute", "Type"};
		int[] bounds = { 100, 200};
		for (int i = 0; i < titles.length; i++) {
			final TableViewerColumn viewerColumn = new TableViewerColumn(
					viewer, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL
					| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
			final TableColumn column = viewerColumn.getColumn();
			column.setText(titles[i]);
			column.setWidth(bounds[i]);
			column.setResizable(true);
			column.setMoveable(true);
		}
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}
	
	private void hookContextMenu() {
		MenuManager treeViewerMenuMgr = new MenuManager("#PopupMenu");
		treeViewerMenuMgr.setRemoveAllWhenShown(true);
		treeViewerMenuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				manager.add(refreshTreeActionOnTreeViewer);
				manager.add(bindingCMTypeActionInTreeViewer);
				manager.add(saveTypesToXMLFileInTreeViewer);
				manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
				manager.add(persistentAnnotationsInTreeViewer);
				manager.add(showAnnotationsActionInTreeViewer);
				
				manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
				manager.add(markAllVariablesActionInTreeViewer);
				manager.add(generateAssertionsActionInTreeViewer);
				// Other plug-ins can contribute there actions here
			}
		});
		Menu treeViewMenu = treeViewerMenuMgr.createContextMenu(existingTypesTreeViewer.getControl());
		existingTypesTreeViewer.getControl().setMenu(treeViewMenu);
		getSite().registerContextMenu(treeViewerMenuMgr, existingTypesTreeViewer);
		
		MenuManager tableViewerMenuMgr = new MenuManager("#PopupMenu");
		tableViewerMenuMgr.setRemoveAllWhenShown(true);
		tableViewerMenuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
//				manager.add(typeCheckingActionInTableViewer);
				// Other plug-ins can contribute there actions here
				manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));				
			}
		});
		Menu tableViewMenu = tableViewerMenuMgr.createContextMenu(typeAttributeViewer.getControl());
		typeAttributeViewer.getControl().setMenu(tableViewMenu);
		getSite().registerContextMenu(tableViewerMenuMgr, typeAttributeViewer);
	}
	
	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}
	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(manageCorrespondenceTypeInTreeViewer);
	}
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(manageCorrespondenceTypeInTreeViewer);
	}

	private void makeActions() {
		clickActionOnTreeViewer = new Action(){
			public void run() {
				ISelection selection = existingTypesTreeViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if(obj != null){
					thisSelectedTreeObject = (TreeObject)obj;
					selectedNewCMType = RWTSystemUtil.getCMTypeFromTreeObject(currentProject, thisSelectedTreeObject);
					if(selectedNewCMType!=null){
						typeAttributeViewer.setInput(selectedNewCMType.getSemanticType());
						if(conceptDetailView!= null){
							conceptDetailView.showConceptDetail(ConceptDetail.readInByLink(selectedNewCMType.getSemanticType().getExplicationLink()));
						}
					}else{
						conceptDetailView.clearAllContents();
					}
				}
			}
		};
		
		refreshTreeActionOnTreeViewer = new Action() {
			public void run() {
				IFile currentFile =  ActivePart.getFileOfActiveEditror();
				if( currentFile != null){
					cmtypeTreeRootObject = RWTSystemUtil.readInAllCMTypesToTreeObject(currentFile);
					if(cmtypeTreeRootObject!= null){
						existingTypesTreeViewer.setInput(cmtypeTreeRootObject);		
				        currentProject = currentFile.getProject();	
					}
				}
			}
		};
		refreshTreeActionOnTreeViewer.setText("reload CM types");
		refreshTreeActionOnTreeViewer.setToolTipText("reload CM type in the tree");
		refreshTreeActionOnTreeViewer.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER));
				
		manageCorrespondenceTypeInTreeViewer = new Action(){
			public void run() {
				ManageRWTypeWizard wizard = new ManageRWTypeWizard();
				wizard.init(RWTView.this.getSite().getWorkbenchWindow().getWorkbench(),
			            null);
			    WizardDialog dialog = new WizardDialog(RWTView.this.getSite().getShell(), wizard);
			    dialog.create();
			    dialog.open();
			    
			}
		};
		manageCorrespondenceTypeInTreeViewer.setText("Manage Correspondence Types");
		manageCorrespondenceTypeInTreeViewer.setToolTipText("Manage Correspondence Types");
		manageCorrespondenceTypeInTreeViewer.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJ_ADD));
		
		bindingCMTypeActionInTreeViewer = new Action() {
			public void run() {
				String typeName = thisSelectedTreeObject.getName();
				bindingType(typeName);
			}
		};
		bindingCMTypeActionInTreeViewer.setText("Bind to this type");
		bindingCMTypeActionInTreeViewer.setToolTipText("Bind this Type to the selected formal element");
		bindingCMTypeActionInTreeViewer.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));
		
		
		persistentAnnotationsInTreeViewer = new Action() {
			public void run() {
				persistAnnotationsToFile();
			}
		};
		persistentAnnotationsInTreeViewer.setText("Persistent annotations in source code to file");
		persistentAnnotationsInTreeViewer.setToolTipText("Persistent annotations in source code to file");
		persistentAnnotationsInTreeViewer.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));
		
		showAnnotationsActionInTreeViewer = new Action() {
			public void run() {
				FileAnnotations.changeAnnotationsStatus();
			}
		};
		showAnnotationsActionInTreeViewer.setText("turn on/off annotations");
		showAnnotationsActionInTreeViewer.setToolTipText("turn on / off the annotations for the current active file");
		showAnnotationsActionInTreeViewer.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));
		
		saveTypesToXMLFileInTreeViewer = new Action() {
			public void run() {
				saveTypesInXMLFiles();
			}
		};
		saveTypesToXMLFileInTreeViewer.setText("Persist the type in xml file");
		saveTypesToXMLFileInTreeViewer.setToolTipText("Persist the type in xml file");
		saveTypesToXMLFileInTreeViewer.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));
		
		
		markAllVariablesActionInTreeViewer = new Action() {
			public void run() {
				markAllRelevantVariabls();
			}
		};
		markAllVariablesActionInTreeViewer.setText("show all variables binded to the CM Type");
		markAllVariablesActionInTreeViewer.setToolTipText("show all variables binded to the CM Type");
		markAllVariablesActionInTreeViewer.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));
		
		generateAssertionsActionInTreeViewer = new Action() {
			public void run() {
				generateAssertions();
			}

			private void generateAssertions() {
			}
		};
		generateAssertionsActionInTreeViewer.setText("Generate all assertions in annotation");
		generateAssertionsActionInTreeViewer.setToolTipText("Generate all assertions in annotation");
		generateAssertionsActionInTreeViewer.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));
		
	}


	private void hookClickAction() {
		typeAttributeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
			}
		});
		
		existingTypesTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				clickActionOnTreeViewer.run();
			}
		});
		
		existingTypesTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
				clickActionOnTreeViewer.run();
			}
		});
	}
	
	private void showMessage(String message) {
		MessageDialog.openInformation(
				typeAttributeViewer.getControl().getShell(),
			"CM Type Annotation View",
			message);
	}

	public void setFocus() {
		typeAttributeViewer.getControl().setFocus();
	}

	public void clearAllContents() {
		this.typeAttributeViewer.setInput(null);
		this.existingTypesTreeViewer.setInput(null);
		this.diagnoseView.getErrorTableViewer().setInput(null);
		this.conceptDetailView.clearAllContents();
	}
	
	public static boolean getAnnotationStatus(){
		CompilationUnit compilationResult = RWTSystemUtil.getCurrentCompliationUnit();
		List comments = compilationResult.getCommentList();
		if(comments.size() == 0){
			return false;
		}
		ListIterator iterator = comments.listIterator();
		while(iterator.hasNext()) {
			Object object =  iterator.next();
			if(object instanceof Javadoc){
				Javadoc javadoc = (Javadoc)object;
				for(int i=0;i<javadoc.tags().size();i++){
					TagElement tagElement = (TagElement)(javadoc.tags().get(i));
					if(tagElement.getTagName()!= null 
						&& tagElement.getTagName().equals(RWTAnnotation.tagNameForAnnotation)
						&& tagElement.fragments().size()>0){
								return true;
					}
				}
			}
		} 
		return false;
	}
	
	private void persistAnnotationsToFile(){	
		CompilationUnit compilationResult = RWTSystemUtil.getCurrentCompliationUnit();
		AnnotationVisitor annotationVisitor = new AnnotationVisitor();
		compilationResult.accept(annotationVisitor);
		FileAnnotations fileAnnotations = annotationVisitor.getFileAnnotations();
		IFile currentFile = ActivePart.getFileOfActiveEditror();		
		File annotationFile = RWTSystemUtil.getAnnotationFile(currentFile);
		FileAnnotations.saveToFile(fileAnnotations, annotationFile);
		
		BufferedReader infile = null;
		ArrayList<String> contents = new ArrayList<String>();
		String line = "";
		try {
			infile = new BufferedReader(new FileReader(currentFile.getRawLocation().toFile()));
			while((line = infile.readLine())!= null){
				contents.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*
		String[] sourceContents = contents.toArray(new String[contents.size()]);
		RevisionVisitor revisionVisitor = new RevisionVisitor(compilationResult, fileAnnotations, sourceContents);
		compilationResult.accept(revisionVisitor);
		StringBuffer readContents = revisionVisitor.getRevisedSource();
		int lastUnreadLine = revisionVisitor.getLastUnReadLine();
		for(int i=lastUnreadLine;i<sourceContents.length;i++){
			readContents.append(sourceContents[i]+"\n");
		}
		*/
	}
	
	private void saveTypesInXMLFiles(){	
		if(selectedNewCMType!=null){
			File xmlFile = RWTSystemUtil.generateXMLFile(currentProject, selectedNewCMType.getTypeName());
			XMLGeneratorForTypes.persistTypeToFile(xmlFile, selectedNewCMType);
		}
	}
	
	private void markAllRelevantVariabls(){
		String typeName = thisSelectedTreeObject.getName();
		currentJavaEditor = ActivePart.getActiveEditor();
		if(currentJavaEditor!= null){
			/*
			CMTypeRulesManager operationManager = CMTypeRulesManager.getManagerForCurrentProject(); 
			CompilationUnit compilationResult = RWTSystemUtil.getCurrentCompliationUnit();
			NewTypeCheckerVisitor visitor = new NewTypeCheckerVisitor(operationManager, compilationResult, typeName);
			compilationResult.accept(visitor);
			CMTypeCheckingResults = visitor.getErrorReports();
			typeChecked = true;
			if(!CMTypeView.getAnnotationStatus()){
				FileAnnotations.changeAnnotationsStatus();
			}
			TextOperationAction fExpandAll = new TextOperationAction(FoldingMessages.getResourceBundle(), "Projection.ExpandAll.", (ITextEditor) currentJavaEditor, ProjectionViewer.EXPAND_ALL); //$NON-NLS-1$
			fExpandAll.setActionDefinitionId(IFoldingCommandIds.FOLDING_EXPAND_ALL);
			fExpandAll.run();
			
			currentTextControl = (StyledText) currentJavaEditor.getAdapter(Control.class);
			diagnoseView = (DiagnoseView)(ActivePart.getSpecificView(DiagnoseView.ID));
			diagnoseView.getErrorTableViewer().setInput(CMTypeCheckingResults);
			diagnoseView.setTextControl(currentTextControl);
			ArrayList<Expression> markingExps = visitor.getMarkingNodes();
			for(Expression exp : markingExps){
				StyleRange errorRange = createRange(compilationResult.getExtendedStartPosition(exp),
						compilationResult.getExtendedLength(exp),greenColor);
						currentTextControl.setStyleRange(errorRange);
			}
			*/
		}
	}
	
	private void bindingType(String typeName){
		if(selectedNewCMType!=null){
			typeAttributeViewer.setInput(selectedNewCMType.getSemanticType());
			if(conceptDetailView!= null){
				conceptDetailView.showConceptDetail(ConceptDetail.readInByLink(selectedNewCMType.getSemanticType().getExplicationLink()));
			}
		}
		else{
			conceptDetailView.clearAllContents();
		}
		
		boolean annotationStatus = getAnnotationStatus();
		
		IEditorPart javaEditor = ActivePart.getActiveEditor();		
		ISelection iselection = javaEditor.getEditorSite().getSelectionProvider().getSelection();
		
		if(iselection != null){
			if(iselection instanceof ITextSelection){
				ITextSelection textSelection = (ITextSelection)iselection;
				CompilationUnit compilationResult = RWTSystemUtil.getCurrentCompliationUnit();
				if(compilationResult!=null){
					ASTNode node = NodeFinder.perform(compilationResult.getRoot(),textSelection.getOffset(), textSelection.getLength());
					if(node !=null){
						
						if(node instanceof NumberLiteral){	
							NumberLiteral numberLiteral = (NumberLiteral)node;
							ASTNode parentNode = node.getParent();
				            while(!((parentNode instanceof MethodDeclaration) || (parentNode instanceof TypeDeclaration))){
				            	parentNode = parentNode.getParent();
				            }
				            String newAddedVariableName = addNewVariableForNumberLiteral((BodyDeclaration)parentNode, compilationResult,RWTAnnotation.Define, numberLiteral, typeName);
				            if(annotationStatus){
				            	addJAVADocElement((BodyDeclaration)parentNode, compilationResult, RWTAnnotation.Define, newAddedVariableName, typeName);	
				            }
			    			saveJAVADocElementToFile((BodyDeclaration)parentNode, RWTAnnotation.Define, newAddedVariableName, typeName, false);
						}
						if(node instanceof SimpleName){							
							IBinding binding= ((SimpleName)node).resolveBinding();
						 	if (binding.getKind() == IBinding.METHOD) {
								ASTNode methodDeclNode = compilationResult.findDeclaringNode(binding.getKey());
								if(methodDeclNode instanceof MethodDeclaration){
									MethodDeclaration methodDecl = (MethodDeclaration)methodDeclNode;
									if(annotationStatus){
						            	addJAVADocElement(methodDecl, compilationResult, RWTAnnotation.Return, null, typeName);	
						            }
					    			saveJAVADocElementToFile(methodDecl, RWTAnnotation.Return, null, typeName, false);
								}
						 		typeAttributeViewer.refresh();
						 	} else if (binding.getKind() == IBinding.VARIABLE) {
								IVariableBinding bindingDecl= ((IVariableBinding) ((SimpleName)node).resolveBinding()).getVariableDeclaration();
								String formalElementName = bindingDecl.getName();
				    			if(typeName.length() == 0){
				    				return;
				    			}
								if(bindingDecl.isField()){
									/*
									problems here right now
									*/
									
									ASTNode declaringClassNode = compilationResult.findDeclaringNode(bindingDecl.getDeclaringClass());
									if(declaringClassNode!= null && declaringClassNode instanceof TypeDeclaration){
						    			TypeDeclaration parentTD = (TypeDeclaration)declaringClassNode;						    			
						    			if(annotationStatus){
						    				addJAVADocElement(parentTD, compilationResult, RWTAnnotation.Define, formalElementName, typeName);	
						    			}
						    			saveJAVADocElementToFile(parentTD, RWTAnnotation.Define, formalElementName, typeName, false);
									}else{
										String declarationBodykey = bindingDecl.getDeclaringClass().getKey();
										IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(bindingDecl.getJavaElement().getPath());
										File otherSourceFileAnnotationFile = RWTSystemUtil.getAnnotationFile(ifile);
										FileAnnotations fileAnnotations = new FileAnnotations ();
										if(!otherSourceFileAnnotationFile.exists()){
											try {
												otherSourceFileAnnotationFile.createNewFile();
											} catch (IOException e) {
												e.printStackTrace();
											}
										}else{
											fileAnnotations = FileAnnotations.loadFromXMLFile(otherSourceFileAnnotationFile);
											if(fileAnnotations == null){
												fileAnnotations = new FileAnnotations();
											}
										}
										if((declarationBodykey!= null) && declarationBodykey.length() > 0){
											fileAnnotations.addDefineAnnotation(declarationBodykey, RWTAnnotation.Define, formalElementName, typeName);
											FileAnnotations.saveToFile(fileAnnotations, otherSourceFileAnnotationFile);	
										}
									}
								}else{
									ASTNode declaringMethodNode = compilationResult.findDeclaringNode(bindingDecl.getDeclaringMethod());
									MethodDeclaration methodDeclaration = (MethodDeclaration)declaringMethodNode;
					                if(annotationStatus){
					                	addJAVADocElement(methodDeclaration, compilationResult, RWTAnnotation.Define, formalElementName, typeName);	
					                }
					                saveJAVADocElementToFile(methodDeclaration, RWTAnnotation.Define, formalElementName, typeName, false);
								}
					    		typeAttributeViewer.refresh();
							}
						 	else {
								throw new IllegalArgumentException("Unexpected binding"); //$NON-NLS-1$
							}
						}if(node instanceof ThisExpression){
							ThisExpression thisExp = ((ThisExpression)node);
							ITypeBinding typeBinding = thisExp.resolveTypeBinding();
							String formalElementName = "this";
							ASTNode parentNode = node.getParent();
				            while(!((parentNode instanceof MethodDeclaration) || (parentNode instanceof TypeDeclaration))){
				            	parentNode = parentNode.getParent();
				            }
//							ASTNode declaringClassNode = compilationResult.findDeclaringNode(typeBinding);
//							if(declaringClassNode!= null && declaringClassNode instanceof TypeDeclaration){
//				    			TypeDeclaration parentTD = (TypeDeclaration)declaringClassNode;						    			if(annotationStatus){
//				    				addJAVADocElement(parentTD, compilationResult, Annotation.Define, formalElementName, typeName);	
//				    			}
//				    			saveJAVADocElementToFile(parentTD, Annotation.Define, formalElementName, typeName);
//							}
				            if(annotationStatus){
				            	addJAVADocElement((BodyDeclaration)parentNode, compilationResult, RWTAnnotation.Define, "this", typeName);	
				            }
			    			saveJAVADocElementToFile((BodyDeclaration)parentNode, RWTAnnotation.Define, "this", typeName, false);
				            
						}
					}
				}
			}
		}
	}
	
	public static void saveJAVADocElementToFile(BodyDeclaration parentTD, String annotationType, 
			String formalElementName, String cmtype_name, boolean propagation){
		String declarationBodykey = "";
		IFile ifile = null;
		if(parentTD instanceof TypeDeclaration){
			declarationBodykey = ((TypeDeclaration)parentTD).resolveBinding().getKey();	
			ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(((TypeDeclaration)parentTD).resolveBinding().getJavaElement().getPath());
		}
		if(parentTD instanceof MethodDeclaration){
			declarationBodykey = ((MethodDeclaration)parentTD).resolveBinding().getKey();	
			ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(((MethodDeclaration)parentTD).resolveBinding().getJavaElement().getPath());
		}
		if(ifile!=null){
			File annotationFile = RWTSystemUtil.getAnnotationFile(ifile);
			if(annotationFile == null){
				//not java files
				return ;
			}
			FileAnnotations fileAnnotations = new FileAnnotations ();
			if(!annotationFile.exists()){
				try {
					annotationFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else{
				fileAnnotations = FileAnnotations.loadFromXMLFile(annotationFile);
			}
			if(fileAnnotations == null){
				fileAnnotations = new FileAnnotations();
			}
			if((declarationBodykey!= null) && declarationBodykey.length() > 0){
				if(propagation){
					if(cmtype_name.equals(RWType.GenericMethod)){
						return;
					}
					ArrayList<RWTAnnotation> thisAnnotations =fileAnnotations.getAnnotations().get(declarationBodykey);
					if(thisAnnotations!=null){
						for(RWTAnnotation anno : thisAnnotations){
							if(annotationType.equals(RWTAnnotation.Define)){
								if(anno.getAnnotationContents().indexOf("("+formalElementName+")")!=-1){
									System.out.println("RWT annotation for "+formalElementName+" already exists");
									return;
								}	
							}else if(annotationType.equals(RWTAnnotation.Return)){
								System.out.println("RWT annotation for return value already exists");
								return;
							}
						}
					}
				}
				fileAnnotations.addDefineAnnotation(declarationBodykey, annotationType, formalElementName, cmtype_name);
				FileAnnotations.saveToFile(fileAnnotations, annotationFile);	
			}
		}
	}
	
	private String addNewVariableForNumberLiteral(BodyDeclaration parentTD, CompilationUnit compilationResult, String annotationType, NumberLiteral numberLiteral, String CMType){
		AST ast = compilationResult.getAST();
		String variableName = "cst_"+numberLiteral.getStartPosition();
		PrimitiveType vdsType = null;
        ITypeBinding itypebinding = numberLiteral.resolveTypeBinding();
        System.out.println(itypebinding.getBinaryName());
        String typeName = itypebinding.getBinaryName();
        if(typeName.equals("D")){
        	vdsType = ast.newPrimitiveType(PrimitiveType.DOUBLE);
        } else if(typeName.equals("I")) {
        	vdsType = ast.newPrimitiveType(PrimitiveType.INT);
        }else if(typeName.equals("F")) {
        	vdsType = ast.newPrimitiveType(PrimitiveType.FLOAT);
        }
        ASTRewrite rewrite= ASTRewrite.create(ast);
        SimpleName newVariableName = ast.newSimpleName(variableName);
        rewrite.replace(numberLiteral, newVariableName, null);
      
		ASTNode thisStatementNode = numberLiteral.getParent();
        while(!(thisStatementNode instanceof org.eclipse.jdt.core.dom.Statement) && 
        	  !(thisStatementNode instanceof org.eclipse.jdt.core.dom.FieldDeclaration) ){
        	thisStatementNode = thisStatementNode.getParent();
        }
        ASTNode newNode = null;
        ListRewrite listRewriter  = null;
        if(parentTD instanceof MethodDeclaration){
			VariableDeclarationFragment fragment= ast.newVariableDeclarationFragment();
			fragment.setName(newVariableName);
			Expression numberLiteralCopy= (Expression) rewrite.createCopyTarget(numberLiteral);
			fragment.setInitializer(numberLiteralCopy);
			VariableDeclarationStatement newDecl= ast.newVariableDeclarationStatement(fragment);
			newDecl.setType(vdsType);
			newNode = newDecl;
			listRewriter = rewrite.getListRewrite(((MethodDeclaration)parentTD).getBody(), Block.STATEMENTS_PROPERTY);
        }else if(parentTD instanceof TypeDeclaration){
        	VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
        	fragment.setName(newVariableName);
        	Expression numberLiteralCopy= (Expression) rewrite.createCopyTarget(numberLiteral);
        	fragment.setInitializer(numberLiteralCopy);
        	FieldDeclaration fd = ast.newFieldDeclaration(fragment);
        	
        	fd.modifiers().add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
        	if(thisStatementNode instanceof FieldDeclaration){
        		for(Object modifier: ((FieldDeclaration)thisStatementNode).modifiers()){
        			if(((Modifier)modifier).getKeyword().equals(ModifierKeyword.STATIC_KEYWORD)){
        				fd.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));			
        			}
        		}
        	}
        	fd.setType(vdsType);
        	newNode = fd;
        	listRewriter = rewrite.getListRewrite(((TypeDeclaration)parentTD), TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        }
        
		listRewriter.insertBefore(newNode, thisStatementNode, null);
		
		
		//apply changes
//		ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager(); // get the buffer manager
//		IPath path = compilationResult.getJavaElement().getPath(); // unit: instance of CompilationUnit
//		try {
//			bufferManager.connect(path, null); // (1)
//			ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(path);
//			IDocument document = textFileBuffer.getDocument(); 
//			TextEdit edit = rewrite.rewriteAST(document, null);
//			edit.apply(document);
//		} catch (CoreException e) {
//			e.printStackTrace();
//		} catch (MalformedTreeException e) {
//			e.printStackTrace();
//		} catch (BadLocationException e) {
//			e.printStackTrace();
//		} finally {
//			try {
//				bufferManager.disconnect(path, null);
//			} catch (CoreException e) {
//				e.printStackTrace();
//			} 
//		}
		currentJavaEditor = ActivePart.getActiveEditor();
		if(currentJavaEditor!=null){
	        IDocumentProvider dp = ((ITextEditor) currentJavaEditor).getDocumentProvider();
	        IDocument document = dp.getDocument(this.currentJavaEditor.getEditorInput());
	        TextEdit edit = rewrite.rewriteAST(document, null);
			try {
				edit.apply(document);
			} catch (MalformedTreeException e) {
				e.printStackTrace();
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
			currentJavaEditor.doSave(null);
		}
		return variableName;
	}
	
	public static void addJAVADocElement(BodyDeclaration parentTD, CompilationUnit compilationResult, String annotationType, String formalElementName, String CMType){
		Javadoc javadoc= parentTD.getJavadoc();
		AST ast = compilationResult.getAST();
		TagElement newTagElement= ast.newTagElement();
		newTagElement.setTagName(RWTAnnotation.tagNameForAnnotation);
		
		String formalElementNameWithParenthesis = RWTAnnotation.cmTypeForAnnotation + "(" + formalElementName +")";
		if(annotationType.equals(RWTAnnotation.Define) ||
				(annotationType.equals(RWTAnnotation.Constant))){
			TextElement annotationTypeTextElement = ast.newTextElement();
			annotationTypeTextElement.setText(annotationType);
			newTagElement.fragments().add(annotationTypeTextElement);
			
			TextElement annotatedTextElement =  ast.newTextElement();
			annotatedTextElement.setText(formalElementNameWithParenthesis + "="+CMType);
			newTagElement.fragments().add(annotatedTextElement);
		}
		else if(annotationType.equals(RWTAnnotation.Return)){
			TextElement annotationTypeTextElement = ast.newTextElement();
			annotationTypeTextElement.setText(annotationType);
			newTagElement.fragments().add(annotationTypeTextElement);
			
			TextElement annotatedTextElement =  ast.newTextElement();
			if(formalElementName!= null){
				annotatedTextElement.setText(RWTAnnotation.cmTypeForAnnotation + "("  + formalElementName +")");
			}else if(CMType!= null){
				annotatedTextElement.setText(CMType);
			}
			newTagElement.fragments().add(annotatedTextElement);
		}
		else{
			return;
		}
		
		if(javadoc != null){
			for(int i=0;i<javadoc.tags().size();i++){
				TagElement tagElement = (TagElement)(javadoc.tags().get(i));
				if(tagElement.getTagName() == null){
						continue;
				}
				else if(tagElement.getTagName().equals(RWTAnnotation.tagNameForAnnotation)
						&& tagElement.fragments().size()>0){
					String thisformalElementName = tagElement.fragments().get(0).toString().trim().split(" ")[1];
					String thisAnnotationType = tagElement.fragments().get(0).toString().trim().split(" ")[0];
					if(annotationType.equals(RWTAnnotation.Define)){
						String variableName = thisformalElementName.split("=")[0];
						if(formalElementNameWithParenthesis.equals(variableName)){
							javadoc.tags().remove(i);
							continue;
						}
					}
					if(thisAnnotationType.equals(RWTAnnotation.Return) &&
							annotationType.equals(RWTAnnotation.Return)){
						javadoc.tags().remove(i);
						 break;
					}
				}
			}
			javadoc.tags().add(newTagElement);
		}else{
			Javadoc jc = ast.newJavadoc();
			jc.tags().add(newTagElement);
			parentTD.setJavadoc(jc);
		}
		
		//apply changes
		ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager(); // get the buffer manager
		IPath path = compilationResult.getJavaElement().getPath(); // unit: instance of CompilationUnit
		try {
			bufferManager.connect(path, null); // (1)
			ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(path);
			IDocument document = textFileBuffer.getDocument(); 
			compilationResult.rewrite(document, null).apply(document);	
			textFileBuffer
				.commit(null /* ProgressMonitor */, false  /* Overwrite */); // (3)

		} catch (CoreException e) {
			e.printStackTrace();
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		} finally {
			try {
				bufferManager.disconnect(path, null);
			} catch (CoreException e) {
				e.printStackTrace();
			} 
		}
	}
	
	public static void removeJAVAElement(BodyDeclaration parentTD, CompilationUnit compilationResult, String annotationType){
		Javadoc javadoc= parentTD.getJavadoc();
		if(javadoc != null){
			for(int i=0;i<javadoc.tags().size();i++){
				TagElement tagElement = (TagElement)(javadoc.tags().get(i));
				if(tagElement.getTagName()!=null){
					if(tagElement.getTagName().equals(RWTAnnotation.tagNameForAnnotation)
							&& tagElement.fragments().size()>0){
						String thisAnnotationType = tagElement.fragments().get(0).toString().trim().split(" ")[0];
						if(annotationType.equals(thisAnnotationType)){
							javadoc.tags().remove(i);
						}
					}
				}
			}
		}
		//apply changes
		ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager(); // get the buffer manager
		IPath path = compilationResult.getJavaElement().getPath(); // unit: instance of CompilationUnit
		try {
			bufferManager.connect(path, null); // (1)
			ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(path);
			IDocument document = textFileBuffer.getDocument(); 
			compilationResult.rewrite(document, null).apply(document);	
			textFileBuffer
				.commit(null /* ProgressMonitor */, false  /* Overwrite */); // (3)

		} catch (CoreException e) {
			e.printStackTrace();
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		} finally {
			try {
				bufferManager.disconnect(path, null);
			} catch (CoreException e) {
				e.printStackTrace();
			} 
		}
	}
	
	public void refreshTheStyleRangesForErrors(){
		CompilationUnit compilationResult = RWTSystemUtil.getCurrentCompliationUnit();
		if(compilationResult!=null){
				expandAllChildElements(currentJavaEditor,compilationResult);
				for (int i=0; i < CMTypeCheckingResults.size(); ++i) {
					ASTNode node = CMTypeCheckingResults.get(i).getErrorNode();
										
					StyleRange errorRange = createRange(compilationResult.getExtendedStartPosition(node),
						compilationResult.getExtendedLength(node),redColor);
					currentTextControl.setStyleRange(errorRange);
				}
		}
	}
	
	private void expandAllChildElements(IEditorPart currentJavaEditor, CompilationUnit compilationResult){
		DefaultJavaFoldingStructureProvider foldingProvider = new DefaultJavaFoldingStructureProvider();
		JavaEditor javaeditor = null;
		if(currentJavaEditor instanceof JavaEditor) {
			javaeditor = (JavaEditor) currentJavaEditor;
		}
		else {
			javaeditor = (JavaEditor) currentJavaEditor.getAdapter(JavaEditor.class);
		}
		foldingProvider.install(javaeditor, (ProjectionViewer)(javaeditor.getViewer()));
		IJavaElement topJavaElement = compilationResult.getJavaElement();
		if(topJavaElement instanceof IParent){
			List<IJavaElement> allChildrenElements = getAllChildren((IParent)topJavaElement);
			foldingProvider.expandElements(allChildrenElements.toArray(new IJavaElement[allChildrenElements.size()]));

		}
		foldingProvider.uninstall();
	}
	
	private List<IJavaElement> getAllChildren(IParent parentElement) {
		List<IJavaElement> allChildren = new ArrayList<IJavaElement>();
		try {
			for (IJavaElement child : parentElement.getChildren()) {
				allChildren.add(child);
				if (child instanceof IParent) {
					allChildren.addAll(getAllChildren((IParent) child));
				}
			}
		} catch (JavaModelException e) {
		}
		return allChildren;
	}

	
	static private StyleRange createRange(int start,int length,Color color) {
		StyleRange styleRange = new StyleRange();
		styleRange.start = start;
		styleRange.length = length; 
		styleRange.fontStyle = SWT.BOLD;
		styleRange.foreground = color;
		return styleRange;
	}
	
	public void refreshCMTypeTree(){
		this.refreshTreeActionOnTreeViewer.run();
	}
	
	public void resetTableContents(){
		this.typeAttributeViewer.refresh();
	}

	public boolean isTypeChecked() {
		return typeChecked;
	}

	public void setTypeChecked(boolean typeChecked) {
		this.typeChecked = typeChecked;
	}	
	

	public TableViewer getTypeAttributeViewer() {
		return typeAttributeViewer;
	}

	public boolean isInspectionMode() {
		return inspectionMode;
	}

	public void setInspectionMode(boolean inspectionMode) {
		this.inspectionMode = inspectionMode;
	}
	
	
}