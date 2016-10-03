package rwtchecker.wizards;


import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

import rwtchecker.concept.ConceptAttribute;
import rwtchecker.concept.ConceptDetail;
import rwtchecker.dialogs.CMTypeApproxPropertyDialog;
import rwtchecker.dialogs.CMTypeAttributeEditingDialog;
import rwtchecker.dialogs.CMTypeRenameDialog;
import rwtchecker.dialogs.SelectConceptExplicationDialog;
import rwtchecker.dialogs.TypeDerivationDialog;
import rwtchecker.rwt.RWT_Attribute;
import rwtchecker.rwt.RWType;
import rwtchecker.rwt.RWT_Semantic;
import rwtchecker.rwt.RWT_ApproximationProperty;
import rwtchecker.util.ActivePart;
import rwtchecker.util.RWTSystemUtil;
import rwtchecker.views.provider.CMApproTableContentProvider;
import rwtchecker.views.provider.CMApproTablelLabelProvider;
import rwtchecker.views.provider.CMAttributeTableContentProvider;
import rwtchecker.views.provider.CMAttributeTablelLabelProvider;
import rwtchecker.views.provider.CMViewTreeContentProvider;
import rwtchecker.views.provider.CMViewTreeViewLabelProvider;
import rwtchecker.views.provider.TreeObject;

public class ManageRWTWizardPage extends WizardPage {
	public static final String PAGE_NAME = "ManageCMTypeWizardPage";
	
	private Text containerText;
	//for the display of correspondence types
	private TreeObject cmtypeRootTreeObject;
	private TreeViewer cmTypesTreeViewer;
	private Label CMtypeDetailLabel;
	
	//these two fields are used to represent the currently selected tree object and cm type
	private TreeObject cmtypeTreeSelectedObject;
	private RWType selectedCMType;
	
	//for the display of concept detail
	private TableViewer typeAttributeViewer;
	private Text associatedExplicationText;
	
	private Text targetAttributeText;
	
	private TableViewer approxAttributeViewer;
	
	private IProject currentProject;
	
	private Action clickActionOnTreeViewer;
	
	private Action createSubCMTypeInTreeViewer;
	private Action createDomainCMTypeInTreeViewer;
	private Action delCorrespondenceTypeInTreeViewer;
	private Action renameCorrespondenceTypeOnTreeView;
	private Action insertCorrespondenceTypeOnTreeView;
	
	public ManageRWTWizardPage() {
		super(PAGE_NAME);
		setTitle("New Correspondence Type Wizard page 1");
		setDescription("This wizard creates a new correspondence type.");
	}
	
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 6;
		Font titleOneFont = new Font(parent.getDisplay(),"courier new", 9 , SWT.BOLD );
		Font titleTwoFont = new Font(parent.getDisplay(),"arial", 10 , SWT.BOLD );
		
		//Project line
		Label label = new Label(container, SWT.NULL);
		label.setText("&Project:");
		label.setFont(titleOneFont);

		containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 4;
		containerText.setLayoutData(gd);
		containerText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button button = new Button(container, SWT.PUSH);
		GridData buttonGd = new GridData(GridData.HORIZONTAL_ALIGN_FILL); 
		button.setText("Browse...");
		button.setLayoutData(buttonGd);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});
		
		//place holder line
		label = new Label(container, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 6;
		label.setLayoutData(gd);
		label.setFont(titleTwoFont);
		label.setText("&Existing CM types");
		
		//correspondence type information
		Label cmtypesList = new Label(container, SWT.NULL);
		cmtypesList.setText("CM Types");
		cmtypesList.setFont(titleOneFont);
		GridData cmtypesListgd = new GridData(GridData.FILL_HORIZONTAL);
		cmtypesListgd.horizontalSpan = 1;
		cmtypesList.setLayoutData(cmtypesListgd);
		
		CMtypeDetailLabel = new Label(container, SWT.NULL);
		CMtypeDetailLabel.setText("Type Detail");
		CMtypeDetailLabel.setFont(titleOneFont);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 5;
		CMtypeDetailLabel.setLayoutData(gd);
		
		cmTypesTreeViewer = new TreeViewer(container, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		GridData cmtypeListGD = new GridData();
		cmtypeListGD.verticalAlignment = GridData.FILL;
		cmtypeListGD.horizontalAlignment = GridData.FILL;
		cmtypeListGD.widthHint = 300;
		cmtypeListGD.heightHint = 300;
		cmtypeListGD.horizontalSpan = 1;
		cmtypeListGD.verticalSpan = 9;
		cmTypesTreeViewer.getTree().setLayoutData(cmtypeListGD);
		
		cmTypesTreeViewer.setContentProvider(new CMViewTreeContentProvider());
		cmTypesTreeViewer.setLabelProvider(new CMViewTreeViewLabelProvider());
		cmTypesTreeViewer.setInput(null);
		cmTypesTreeViewer.setAutoExpandLevel(4);
		
		cmTypesTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				clickActionOnTreeViewer.run();
			}
		});
		
		cmTypesTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
				clickActionOnTreeViewer.run();
			}
		});
		
		/***************************/
		//Concept explication associated 
		Label associatedExplicationLabel = new Label(container, SWT.NONE);
		associatedExplicationLabel.setText("The concept explication for this CM type: ");
		GridData associatedExplicationLabelgd = new GridData();
		associatedExplicationLabelgd.verticalSpan = 1;
		associatedExplicationLabelgd.horizontalSpan = 5;
		associatedExplicationLabelgd.widthHint = 400;
		associatedExplicationLabel.setLayoutData(associatedExplicationLabelgd);
		
		associatedExplicationText = new Text(container, SWT.WRAP | SWT.BORDER );
		associatedExplicationText.setText("");
		
		GridData associatedExplicationTextgd = new GridData();
		associatedExplicationTextgd.verticalAlignment = GridData.FILL;
		associatedExplicationTextgd.horizontalAlignment = GridData.FILL;
		associatedExplicationTextgd.grabExcessHorizontalSpace = true;
		associatedExplicationTextgd.horizontalSpan = 4;
		associatedExplicationTextgd.verticalSpan = 1;
		associatedExplicationText.setLayoutData(associatedExplicationTextgd);
		
		Button viewExplicationBT = new Button(container, SWT.NULL);
		viewExplicationBT.setText("link to Concept");
		GridData viewExplicationBTgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		viewExplicationBTgd.horizontalSpan = 1;
		viewExplicationBT.setLayoutData(viewExplicationBTgd);
		viewExplicationBT.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				associateConceptWithType();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		
        typeAttributeViewer = new TableViewer(container, SWT.CHECK | SWT.MULTI | SWT.WRAP | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        GridData correspondenceTypeDetailGD = new GridData();
		correspondenceTypeDetailGD.verticalAlignment = GridData.FILL;
		correspondenceTypeDetailGD.horizontalAlignment = GridData.FILL;
		correspondenceTypeDetailGD.horizontalSpan = 5;
		correspondenceTypeDetailGD.verticalSpan = 3;
		correspondenceTypeDetailGD.heightHint = 100;
        createAttributeTableColumns(typeAttributeViewer);
        typeAttributeViewer.getTable().setLayoutData(correspondenceTypeDetailGD);
		typeAttributeViewer.setContentProvider(new CMAttributeTableContentProvider());
		typeAttributeViewer.setLabelProvider(new CMAttributeTablelLabelProvider());
		typeAttributeViewer.setInput(null);
		
		//Approximate type of the selected Correspondence type 
		Label approximateTypeLabel = new Label(container, SWT.NONE);
		approximateTypeLabel.setText("Approximation type Information: ");
		approximateTypeLabel.setFont(titleTwoFont);
		GridData approximateTypeLabelgd = new GridData();
		approximateTypeLabelgd.verticalSpan = 1;
		approximateTypeLabelgd.horizontalSpan = 5;
		approximateTypeLabel.setLayoutData(approximateTypeLabelgd);
		
        approxAttributeViewer = new TableViewer(container, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        GridData approxAttributeViewerGD = new GridData();
        approxAttributeViewerGD.verticalAlignment = GridData.FILL;
        approxAttributeViewerGD.horizontalAlignment = GridData.FILL;
        approxAttributeViewerGD.horizontalSpan = 5;
        approxAttributeViewerGD.verticalSpan = 3;
        approxAttributeViewerGD.heightHint = 100;
        createApproxTableColumns(approxAttributeViewer);
        approxAttributeViewer.getTable().setLayoutData(approxAttributeViewerGD);
        approxAttributeViewer.setContentProvider(new CMApproTableContentProvider());
        approxAttributeViewer.setLabelProvider(new CMApproTablelLabelProvider());
        approxAttributeViewer.setInput(null);
		
        /*apply changes to the cmtype*/
		Button applyChangeBT = new Button(container, SWT.NULL);
		applyChangeBT.setText("Apply Changes");
		GridData applyChangeBTgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		applyChangeBTgd.grabExcessHorizontalSpace = true;
		applyChangeBTgd.horizontalSpan = 1;
		applyChangeBT.setLayoutData(applyChangeBTgd);
		applyChangeBT.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if(currentProject != null){
					String conceptName = associatedExplicationText.getText();
					File cmtypeFile = RWTSystemUtil.getCMTypeFile(currentProject, cmtypeTreeSelectedObject);
					File newConceptFile = RWTSystemUtil.getConceptDetailFile(currentProject, conceptName);
					if(cmtypeFile.exists()){
						if(newConceptFile.exists()){
							selectedCMType.getSemanticType().setExplicationLink(newConceptFile.getAbsolutePath());
						}
						selectedCMType.setTypeName(cmtypeTreeSelectedObject.getName());
						TableItem[] attributes = typeAttributeViewer.getTable().getItems();
						for (TableItem attTI : attributes){
							if(attTI.getChecked()){
								RWT_Attribute att = (RWT_Attribute)(attTI.getData());
								att.setEnableStatus("y");
							}else{
								RWT_Attribute att = (RWT_Attribute)(attTI.getData());
								att.setEnableStatus("n");
							}
						}
					}
					RWType.writeOutCMType(selectedCMType, cmtypeFile);
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
        
		
		 /*button select all attributes */
		Button selectAllBT = new Button(container, SWT.NULL);
		selectAllBT.setText("all");
		GridData selectAllBTBTgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		selectAllBTBTgd.grabExcessHorizontalSpace = true;
		selectAllBTBTgd.horizontalSpan = 1;
		selectAllBT.setLayoutData(selectAllBTBTgd);
		selectAllBT.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if(currentProject != null){
					RWType[] cmtypes = RWType.readInAllCMTypes(currentProject);
					for(RWType cmtype : cmtypes){
						for(RWT_Attribute cmatt:cmtype.getSemanticType().getSemanticTypeAttributes()){
							if(cmatt.getAttributeName().trim().equalsIgnoreCase("tainting")){
								cmatt.setEnableStatus(RWT_Attribute.disEnableMark);
							}else{
								cmatt.setEnableStatus(RWT_Attribute.enableMark);
							}
						}
						File cmtypeFile = RWTSystemUtil.getCMTypeFile(currentProject, cmtype.getTypeName());
						RWType.writeOutCMType(cmtype, cmtypeFile);
					}
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		
		 /*button select tainting att*/
		Button selectTaintingBT = new Button(container, SWT.NULL);
		selectTaintingBT.setText("tainting");
		GridData selectTaintingBTgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		selectTaintingBTgd.grabExcessHorizontalSpace = true;
//		selectUnitsBTgd.horizontalSpan = 6;
		selectTaintingBTgd.horizontalSpan = 1;
		selectTaintingBT.setLayoutData(selectTaintingBTgd);
		selectTaintingBT.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if(currentProject != null){
					RWType[] cmtypes = RWType.readInAllCMTypes(currentProject);
					for(RWType cmtype : cmtypes){
						for(RWT_Attribute cmatt:cmtype.getSemanticType().getSemanticTypeAttributes()){
							if(cmatt.getAttributeName().trim().equalsIgnoreCase("tainting")){
								cmatt.setEnableStatus(RWT_Attribute.enableMark);
							}else{
								cmatt.setEnableStatus(RWT_Attribute.disEnableMark);
							}
						}
						File cmtypeFile = RWTSystemUtil.getCMTypeFile(currentProject, cmtype.getTypeName());
						RWType.writeOutCMType(cmtype, cmtypeFile);
					}
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		
        /*button select unit attribute only*/
		Button selectUnitsBT = new Button(container, SWT.NULL);
		selectUnitsBT.setText("Clear");
		GridData selectUnitsBTgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		selectUnitsBTgd.grabExcessHorizontalSpace = true;
//		selectUnitsBTgd.horizontalSpan = 6;
		selectUnitsBTgd.horizontalSpan = 1;
		selectUnitsBT.setLayoutData(selectUnitsBTgd);
		selectUnitsBT.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if(currentProject != null){
					RWType[] cmtypes = RWType.readInAllCMTypes(currentProject);
					for(RWType cmtype : cmtypes){
						for(RWT_Attribute cmatt:cmtype.getSemanticType().getSemanticTypeAttributes()){
//							if(cmatt.getAttributeName().trim().equalsIgnoreCase("unit")){
//								cmatt.setEnableStatus(CMAttribute.enableMark);
//							}else{
//								cmatt.setEnableStatus(CMAttribute.disEnableMark);
//							}
							cmatt.setEnableStatus(RWT_Attribute.disEnableMark);
						}
						File cmtypeFile = RWTSystemUtil.getCMTypeFile(currentProject, cmtype.getTypeName());
						RWType.writeOutCMType(cmtype, cmtypeFile);
					}
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
        
		 /*button select dimension attribute only*/
		Button selectDimensionBT = new Button(container, SWT.NULL);
		selectDimensionBT.setText("Dimension");
		GridData selectDimensionBTgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		selectDimensionBTgd.grabExcessHorizontalSpace = true;
//		selectUnitsBTgd.horizontalSpan = 6;
		selectDimensionBTgd.horizontalSpan = 1;
		selectDimensionBT.setLayoutData(selectDimensionBTgd);
		selectDimensionBT.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if(currentProject != null){
					RWType[] cmtypes = RWType.readInAllCMTypes(currentProject);
					for(RWType cmtype : cmtypes){
						for(RWT_Attribute cmatt:cmtype.getSemanticType().getSemanticTypeAttributes()){
							if(cmatt.getAttributeName().trim().equalsIgnoreCase("dimension")){
								cmatt.setEnableStatus(RWT_Attribute.enableMark);
							}else{
								cmatt.setEnableStatus(RWT_Attribute.disEnableMark);
							}
						}
						File cmtypeFile = RWTSystemUtil.getCMTypeFile(currentProject, cmtype.getTypeName());
						RWType.writeOutCMType(cmtype, cmtypeFile);
					}
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		
		 /*button select dimension/unit together*/
		Button selectDimensionUnitBT = new Button(container, SWT.NULL);
		selectDimensionUnitBT.setText("Dimension and unit");
		GridData selectDimensionUnitBTgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		selectDimensionBTgd.grabExcessHorizontalSpace = true;
//		selectUnitsBTgd.horizontalSpan = 6;
		selectDimensionBTgd.horizontalSpan = 1;
		selectDimensionUnitBT.setLayoutData(selectDimensionUnitBTgd);
		selectDimensionUnitBT.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if(currentProject != null){
					RWType[] cmtypes = RWType.readInAllCMTypes(currentProject);
					for(RWType cmtype : cmtypes){
						for(RWT_Attribute cmatt:cmtype.getSemanticType().getSemanticTypeAttributes()){
							if(cmatt.getAttributeName().trim().equalsIgnoreCase("dimension")||
									cmatt.getAttributeName().trim().equalsIgnoreCase("unit")){
								cmatt.setEnableStatus(RWT_Attribute.enableMark);
							}else{
								cmatt.setEnableStatus(RWT_Attribute.disEnableMark);
							}
						}
						File cmtypeFile = RWTSystemUtil.getCMTypeFile(currentProject, cmtype.getTypeName());
						RWType.writeOutCMType(cmtype, cmtypeFile);
					}
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		
		
		Label placeholder = new Label(container, SWT.NULL);
		GridData placeholdergd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		placeholdergd.grabExcessHorizontalSpace = true;
		placeholdergd.horizontalSpan = 1;
		placeholder.setLayoutData(placeholdergd);
		
		targetAttributeText = new Text(container, SWT.WRAP | SWT.BORDER );
		targetAttributeText.setText("");
		
		GridData targetAttributeTextgd = new GridData();
		targetAttributeTextgd.verticalAlignment = GridData.FILL;
		targetAttributeTextgd.horizontalAlignment = GridData.FILL;
		targetAttributeTextgd.grabExcessHorizontalSpace = true;
		targetAttributeTextgd.horizontalSpan = 4;
		targetAttributeTextgd.verticalSpan = 1;
		targetAttributeText.setLayoutData(targetAttributeTextgd);
		
		Button attributeCheckBT = new Button(container, SWT.NULL);
		attributeCheckBT.setText("select this att");
		GridData attributeCheckBTgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		attributeCheckBTgd.horizontalSpan = 1;
		attributeCheckBT.setLayoutData(attributeCheckBTgd);
		attributeCheckBT.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				String targetAtt = targetAttributeText.getText().trim();
				if(currentProject != null){
					RWType[] cmtypes = RWType.readInAllCMTypes(currentProject);
					for(RWType cmtype : cmtypes){
						for(RWT_Attribute cmatt:cmtype.getSemanticType().getSemanticTypeAttributes()){
							if(cmatt.getAttributeName().trim().equalsIgnoreCase(targetAtt)){
								cmatt.setEnableStatus(RWT_Attribute.enableMark);
							}else{
								cmatt.setEnableStatus(RWT_Attribute.disEnableMark);
							}
						}
						File cmtypeFile = RWTSystemUtil.getCMTypeFile(currentProject, cmtype.getTypeName());
						RWType.writeOutCMType(cmtype, cmtypeFile);
					}
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		
		
		
//        this.getShell().setSize(1200, 900);
		makeActions();
		hookContextMenu();
        hookActionsToTypeAttributeTable();
        hookActionsToApproxAttributeTable();
		dialogChanged();
		addDNDSupport();
		setControl(container);
		setPageComplete(false);
		loadCurrentProject();
	}
	
	private void loadCurrentProject(){
		IFile currentFile =  ActivePart.getFileOfActiveEditror();
		if( currentFile != null){
	        currentProject = currentFile.getProject();	
	        this.containerText.setText(currentProject.getName());
			cmtypeRootTreeObject = RWTSystemUtil.readInAllCMTypesToTreeObject(this.currentProject);
			cmTypesTreeViewer.setInput(cmtypeRootTreeObject);
		}
	}
	
	private void addDNDSupport(){
		Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
		int operations = DND.DROP_MOVE | DND.DROP_COPY ;
		
	    final DragSource source = new DragSource(cmTypesTreeViewer.getTree(), operations);
	    source.setTransfer(types);
	    source.addDragListener(new DragSourceListener() {
		public void dragStart(DragSourceEvent event) {
			ISelection selection = cmTypesTreeViewer.getSelection();
			Object obj = ((IStructuredSelection)selection).getFirstElement();
			if(obj != null){
				event.doit = true;
			}else {
			    event.doit = false;
		    }
	    };
	
	    public void dragSetData(DragSourceEvent event) {
	      if(cmtypeTreeSelectedObject != null){
		      event.data = cmtypeTreeSelectedObject.getName();  
	      }
	    }
	
	    public void dragFinished(DragSourceEvent event) {
	    }
	    });

	    DropTarget target = new DropTarget(cmTypesTreeViewer.getTree(), operations);
	    target.setTransfer(types);
	    target.addDropListener(new DropTargetAdapter() {
	      public void dragOver(DropTargetEvent event) {
	        event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
	      }

	      public void drop(DropTargetEvent event) {
	        if (event.data == null) {
	          event.detail = DND.DROP_NONE;
	          return;
	        }
			ISelection selection = cmTypesTreeViewer.getSelection();
			Object obj = ((IStructuredSelection)selection).getFirstElement();
			TreeObject dragedSourceTO = (TreeObject)obj;
			TreeItem item = (TreeItem)event.item;
			if(item!=null){
				TreeObject targetTreeObject = (TreeObject)(item.getData());
				RWType sourceCMType = RWTSystemUtil.getCMTypeFromTreeObject(currentProject, dragedSourceTO);
				if((targetTreeObject!= null)
					&&(sourceCMType!= null))
				{
					TreeItem newItem = new TreeItem(item, SWT.NONE);
					File sourceCMTypeFile = RWTSystemUtil.getCMTypeFile(currentProject, dragedSourceTO);
					dragedSourceTO.getParent().removeChild(dragedSourceTO);
					TreeObject.updateTreeObjectToFile(currentProject, dragedSourceTO.getParent());
					targetTreeObject.addChild(dragedSourceTO);
					item.setData(targetTreeObject);
					dragedSourceTO.setParent(targetTreeObject);
					newItem.setData(dragedSourceTO);
					TreeObject.updateTreeObjectToFile(currentProject, targetTreeObject);
					String targetCMTypeName = targetTreeObject.getName();
					RWType.writeOutCMType(sourceCMType, sourceCMTypeFile);
					cmTypesTreeViewer.refresh();
				}
			}
	      }
	     });
	}
	
	private void hookContextMenu() {
		MenuManager treeViewerMenuMgr = new MenuManager("#PopupMenu");
		treeViewerMenuMgr.setRemoveAllWhenShown(true);
		treeViewerMenuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				manager.add(createSubCMTypeInTreeViewer);
				manager.add(createDomainCMTypeInTreeViewer);
				manager.add(insertCorrespondenceTypeOnTreeView);
				manager.add(delCorrespondenceTypeInTreeViewer);
				manager.add(renameCorrespondenceTypeOnTreeView);
				// Other plug-ins can contribute there actions here
				manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));	
			}
		});
		Menu treeViewMenu = treeViewerMenuMgr.createContextMenu(cmTypesTreeViewer.getControl());
		cmTypesTreeViewer.getControl().setMenu(treeViewMenu);
	}

	private void makeActions() {
		clickActionOnTreeViewer = new Action(){
			public void run() {
				ISelection selection = cmTypesTreeViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if(obj != null){
					cmtypeTreeSelectedObject = (TreeObject)obj;
					selectedCMType = RWTSystemUtil.getCMTypeFromTreeObject(currentProject, cmtypeTreeSelectedObject);
					if(selectedCMType != null){
						CMtypeDetailLabel.setText("Type Detail: "+selectedCMType.getTypeName());
						RWT_Semantic thisType = selectedCMType.getSemanticType();
						ConceptDetail explication = ConceptDetail.readInByLink(thisType.getExplicationLink());
						associatedExplicationText.setText(explication.getConceptName());
						typeAttributeViewer.setInput(selectedCMType.getSemanticType());
						approxAttributeViewer.setInput(selectedCMType.getApproximationType());
						
						for (TableItem attItem : typeAttributeViewer.getTable().getItems()){
							RWT_Attribute att = (RWT_Attribute)(attItem.getData());
							if(att.getEnableStatus().equals("y")){
								attItem.setChecked(true);
							}else{
								attItem.setChecked(false);
							}
						}
						
						dialogChanged();
					}
					if(cmtypeTreeSelectedObject.getName().equals(RWTSystemUtil.CMTypesFolder)){
						createSubCMTypeInTreeViewer.setEnabled(false);
						return;
					}else{
						createSubCMTypeInTreeViewer.setEnabled(true);
					}
				}
			}
		};
		
		
		renameCorrespondenceTypeOnTreeView = new Action(){
			public void run(){
				editingCMTypeNameInTree(cmtypeTreeSelectedObject);
			}
		};
		
		renameCorrespondenceTypeOnTreeView.setText("rename this correspondence type");
		renameCorrespondenceTypeOnTreeView.setToolTipText("rename selected correspondence type to a new name");
		renameCorrespondenceTypeOnTreeView.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_TOOL_REDO));
		
		createSubCMTypeInTreeViewer = new Action(){
			public void run() {
				if(cmtypeTreeSelectedObject == null){
					setErrorMessage("no tree object has been selected");
					return;
				}
				
				String parentTypeTOName = cmtypeTreeSelectedObject.getName();
				int typeNameIndex = 0;
				String newCMTypeTOName = parentTypeTOName+"_subType_"+typeNameIndex;
				File newTypeDir = RWTSystemUtil.getCMTypeFile(currentProject, newCMTypeTOName);
				while(newTypeDir.exists()){
					typeNameIndex++;
					newCMTypeTOName = parentTypeTOName+"_subType_"+typeNameIndex;	
					newTypeDir = RWTSystemUtil.getCMTypeFile(currentProject ,newCMTypeTOName);
				}
				
				TreeObject newSubTypeTO = new TreeObject(newCMTypeTOName);
				TreeObject invisiableTop = TreeObject.getTopLevelTreeObject(cmtypeTreeSelectedObject);
				invisiableTop.getChildren()[0].addChild(newSubTypeTO);
				TreeObject.updateTreeObjectToFile(currentProject, invisiableTop);
				
				String newTypeName = newSubTypeTO.getName();
				RWType newCMType = new RWType(RWTSystemUtil.getCMTypeFromTreeObject(currentProject, cmtypeTreeSelectedObject), newTypeName);
				RWType.writeOutCMType(newCMType, newTypeDir);
				cmTypesTreeViewer.refresh();
			}
		};	
		createSubCMTypeInTreeViewer.setText("create a sub-type of this CM type");
		createSubCMTypeInTreeViewer.setToolTipText("Create a sub type of the selected type");
		createSubCMTypeInTreeViewer.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD));
		
		createDomainCMTypeInTreeViewer = new Action(){
			public void run() {
				 if(cmtypeRootTreeObject!= null){
					 for(TreeObject treeObject:cmtypeRootTreeObject.getChildren()){
						 if(treeObject.getName().equals(TreeObject.treeObjectTopName)){
							 cmtypeTreeSelectedObject = treeObject;	 	 
						 }
					 }
				 }
				
				String parentTypeTOName = cmtypeTreeSelectedObject.getName();

				int typeNameIndex = 0;
				String newCMTypeTOName = parentTypeTOName+"_subType_"+typeNameIndex;
				File newTypeDir = RWTSystemUtil.getCMTypeFile(currentProject, newCMTypeTOName);
				while(newTypeDir.exists()){
					typeNameIndex++;
					newCMTypeTOName = parentTypeTOName+"_subType_"+typeNameIndex;	
					newTypeDir = RWTSystemUtil.getCMTypeFile(currentProject, newCMTypeTOName);
				}
				
				TreeObject newSubTypeTO = new TreeObject(newCMTypeTOName);
				cmtypeTreeSelectedObject.addChild(newSubTypeTO);
				TreeObject.updateTreeObjectToFile(currentProject, cmtypeTreeSelectedObject);

				String newTypeName = newSubTypeTO.getName();
				RWType newCMType = new RWType(RWTSystemUtil.getCMTypeFromTreeObject(currentProject, cmtypeTreeSelectedObject), newTypeName);
				RWType.writeOutCMType(newCMType, newTypeDir);
				cmTypesTreeViewer.refresh();
			}
		};	
		createDomainCMTypeInTreeViewer.setText("create a Domain type of this CM type");
		createDomainCMTypeInTreeViewer.setToolTipText("Create a sub type of the selected CM type");
		createDomainCMTypeInTreeViewer.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD));
		
		
		insertCorrespondenceTypeOnTreeView = new Action(){
			public void run() {
				if((cmtypeTreeSelectedObject == null) 
					&&(cmtypeTreeSelectedObject.getParent().getName().equals("")))
				{
					return;
				}
				TreeObject parentTO = cmtypeTreeSelectedObject.getParent();
				RWType originalCMType = RWTSystemUtil.getCMTypeFromTreeObject(currentProject, cmtypeTreeSelectedObject);
				File originalCMTypeFile = RWTSystemUtil.getCMTypeFile(currentProject, cmtypeTreeSelectedObject);
				String parentTypeTOName = parentTO.getName();
				int typeNameIndex = 0;
				String newCMTypeTOName = parentTypeTOName+"_subType_"+typeNameIndex;
				File newTypeDir = RWTSystemUtil.getCMTypeFile(currentProject, newCMTypeTOName);
				while(newTypeDir.exists()){
					typeNameIndex++;
					newCMTypeTOName = parentTypeTOName+"_subType_"+typeNameIndex;
					newTypeDir = RWTSystemUtil.getCMTypeFile(currentProject, newCMTypeTOName);
				}
				
				TreeObject newSubTypeTO = new TreeObject(newCMTypeTOName);
				parentTO.addChild(newSubTypeTO);
				newSubTypeTO.addChild(cmtypeTreeSelectedObject);
				parentTO.removeChild(cmtypeTreeSelectedObject);
				cmtypeTreeSelectedObject.setParent(newSubTypeTO);
				
				String newTypeName = newSubTypeTO.getName();
				RWType newCMType = new RWType(RWTSystemUtil.getCMTypeFromTreeObject(currentProject, parentTO), newTypeName);
				RWType.writeOutCMType(newCMType, newTypeDir);
				
				RWType.writeOutCMType(originalCMType, originalCMTypeFile);
				TreeObject.updateTreeObjectToFile(currentProject, parentTO);
				cmTypesTreeViewer.refresh();
			}
		};	
		insertCorrespondenceTypeOnTreeView.setText("insert a correspondence type here");
		insertCorrespondenceTypeOnTreeView.setToolTipText("insert a correspondence type here");
		insertCorrespondenceTypeOnTreeView.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD));
		
		delCorrespondenceTypeInTreeViewer = new Action(){
			public void run() {
				File fileToDel = RWTSystemUtil.getCMTypeFile(currentProject, cmtypeTreeSelectedObject);
				if( fileToDel.exists() 
					&& fileToDel.isDirectory()){
					deleteDirectory(fileToDel);
					TreeObject parentNode =cmtypeTreeSelectedObject.getParent(); 
					parentNode.removeChild(cmtypeTreeSelectedObject);
					TreeObject.updateTreeObjectToFile(currentProject, parentNode);
					cmTypesTreeViewer.refresh();
				}
			}
		};
		delCorrespondenceTypeInTreeViewer.setText("Delete this type");
		delCorrespondenceTypeInTreeViewer.setToolTipText("Delete this cm type");
		delCorrespondenceTypeInTreeViewer.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
	}
	
	private static boolean deleteDirectory(File path) {
		    if( path.exists() ) {
		      File[] files = path.listFiles();
		      for(int i=0; i<files.length; i++) {
		         if(files[i].isDirectory()) {
		           deleteDirectory(files[i]);
		         }
		         else {
		           files[i].delete();
		         }
		      }
		    }
		    return( path.delete() );
	  }

	
	class NameSorter extends ViewerSorter {
	}
	
	private void hookActionsToTypeAttributeTable() {
	    Menu menu = new Menu(typeAttributeViewer.getTable().getShell(), SWT.POP_UP);
	    typeAttributeViewer.getTable().setMenu(menu);
	    MenuItem addAttItem = new MenuItem(menu, SWT.PUSH);
	    addAttItem.setText("Add new attribute");
	    addAttItem.addListener(SWT.Selection, new Listener() {
	      public void handleEvent(Event event) {
	    	  addTypeAttributeEvent();
	      }
	    });
	    MenuItem removeAttItem = new MenuItem(menu, SWT.PUSH);
	    removeAttItem.setText("Delete This attribute");
	    removeAttItem.addListener(SWT.Selection, new Listener() {
	      public void handleEvent(Event event) {
	    	  deleteTypeAttributeEvent();
	      }
	    });
	    MenuItem loadCLEARAttItem = new MenuItem(menu, SWT.PUSH);
	    loadCLEARAttItem.setText("reload attributes from the CLEAR explication");
	    loadCLEARAttItem.addListener(SWT.Selection, new Listener() {
	      public void handleEvent(Event event) {
	    	  loadCLEARAtt();
	      }
	    });
	    
	    typeAttributeViewer.getTable().addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent arg0) {
			}
			@Override
			public void mouseDown(MouseEvent arg0) {
			}
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {
				editingTypeAttributeEvent();
			}
		});
	}
	
	private void loadCLEARAtt(){
		if(cmtypeTreeSelectedObject!= null){
			selectedCMType = RWTSystemUtil.getCMTypeFromTreeObject(currentProject, cmtypeTreeSelectedObject);
			if(selectedCMType != null){
				CMtypeDetailLabel.setText("Type Detail: "+selectedCMType.getTypeName());
				RWT_Semantic thisType = selectedCMType.getSemanticType();
				ConceptDetail explication = ConceptDetail.readInByLink(thisType.getExplicationLink());
				associatedExplicationText.setText(explication.getConceptName());
				String conceptName = associatedExplicationText.getText();
				File newConceptFile = RWTSystemUtil.getConceptDetailFile(currentProject, conceptName);
				if(newConceptFile.exists()){
					ConceptDetail conceptDetail = ConceptDetail.readInConceptDetail(newConceptFile);
					selectedCMType.getSemanticType().setExplicationLink(newConceptFile.getAbsolutePath());	
					ArrayList<ConceptAttribute> conceptsAtts = conceptDetail.getAttributes();
					ArrayList<RWT_Attribute> semanticAtts = thisType.getSemanticTypeAttributes();
					for(ConceptAttribute conceptAttribute: conceptsAtts){
						String attName = conceptAttribute.getAttributeName();
						boolean existAtt = false;
						for(RWT_Attribute semanticAtt : semanticAtts){
							if(semanticAtt.getAttributeName().equalsIgnoreCase(attName)){
								existAtt = true;
								break;
							}
						}
						if(!existAtt){
							RWT_Attribute newSemanticTypeAtt = new RWT_Attribute(attName, "");
							thisType.addSemanticTypeAtt(newSemanticTypeAtt);
						}
					}
					selectedCMType.setSemanticType(thisType);
					typeAttributeViewer.setInput(thisType);	
				}
				dialogChanged();
			}
		}
		
	}

	private void hookActionsToApproxAttributeTable() {
	    Menu menu = new Menu(approxAttributeViewer.getTable().getShell(), SWT.POP_UP);
	    approxAttributeViewer.getTable().setMenu(menu);
	    MenuItem addAttItem = new MenuItem(menu, SWT.PUSH);
	    addAttItem.setText("Add new property");
	    addAttItem.addListener(SWT.Selection, new Listener() {
	      public void handleEvent(Event event) {
	    	  addApproxAttributeEvent();
	      }
	    });
	    MenuItem removeAttItem = new MenuItem(menu, SWT.PUSH);
	    removeAttItem.setText("Delete This property");
	    removeAttItem.addListener(SWT.Selection, new Listener() {
	      public void handleEvent(Event event) {
	    	  deleteApproxAttributeEvent();
	      }
	    });
	    
	    approxAttributeViewer.getTable().addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent arg0) {
			}
			@Override
			public void mouseDown(
					MouseEvent arg0) {
			}
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {
				System.out.println(selectedCMType.getApproximationType().getApproximateProperties().size());
				editingApproxAttributeEvent();
			}
		});
	}
	
	private void addTypeAttributeEvent(){
		if(selectedCMType != null){
			RWT_Attribute newAtt = new RWT_Attribute("new Attribute", "Attribute Type");
			  if(!selectedCMType.getSemanticType().getSemanticTypeAttributes().contains(newAtt)){
				  selectedCMType.getSemanticType().getSemanticTypeAttributes().add(newAtt);
				  typeAttributeViewer.setInput(selectedCMType.getSemanticType()); 
			 }else{
				 updateStatus("Existing concept attribute, try another name for the attribute");
			 }
		}
	}
	
	private void editingTypeAttributeEvent(){
		if(selectedCMType != null){
			if(typeAttributeViewer.getTable().getSelection()!=null){
				if(typeAttributeViewer.getTable().getSelection()[0] != null){
					IWorkbench workbench = PlatformUI.getWorkbench();
					IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
					CMTypeAttributeEditingDialog dialog = new CMTypeAttributeEditingDialog(window.getShell(), typeAttributeViewer, selectedCMType.getSemanticType(), currentProject );
					dialog.create();
					dialog.open();		
				}	
			}
		}
	}
	
	private void deleteTypeAttributeEvent(){
		if(selectedCMType != null){
		  TableItem selectedItem = typeAttributeViewer.getTable().getSelection()[0];
		  if(selectedItem != null){
			  RWT_Attribute deleteAtt = new RWT_Attribute(selectedItem.getText(0), selectedItem.getText(1));
			  if(selectedCMType.getSemanticType().getSemanticTypeAttributes().contains(deleteAtt)){
				  selectedCMType.getSemanticType().getSemanticTypeAttributes().remove(deleteAtt);
				  typeAttributeViewer.setInput(selectedCMType.getSemanticType()); 
			  }
		  }
		}
	}
	
	private void addApproxAttributeEvent(){
		if(selectedCMType != null){
			 RWT_ApproximationProperty newAtt = new RWT_ApproximationProperty("new Property", "Property Description");
			 newAtt.setPossibleValue("0");
			  if(!selectedCMType.getApproximationType().getApproximateProperties().contains(newAtt)){
				  selectedCMType.getApproximationType().getApproximateProperties().add(newAtt);
				  approxAttributeViewer.setInput(selectedCMType.getApproximationType()); 
			 }else{
				 updateStatus("Existing concept attribute, try another name for the attribute");
			 }
		}
	}
	
	private void editingApproxAttributeEvent(){
		if(selectedCMType != null){
			if(approxAttributeViewer.getTable().getSelection()[0] != null){
				IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
				CMTypeApproxPropertyDialog dialog = new CMTypeApproxPropertyDialog(window.getShell(), selectedCMType.getApproximationType(), approxAttributeViewer);
				dialog.create();
				dialog.open();		
			}
		}
	}
	
	private void deleteApproxAttributeEvent(){
		if(approxAttributeViewer.getTable().getSelection()[0] != null){
			  TableItem selectedItem = approxAttributeViewer.getTable().getSelection()[0];
			  if(selectedItem != null){
				  RWT_ApproximationProperty deleteAtt = new RWT_ApproximationProperty(selectedItem.getText(0), selectedItem.getText(1));
				  if(selectedCMType.getApproximationType().getApproximateProperties().contains(deleteAtt)){
					  selectedCMType.getApproximationType().getApproximateProperties().remove(deleteAtt);
					  approxAttributeViewer.setInput(selectedCMType.getApproximationType()); 
				  }
			  }	
		}
	}
	
	private void editingCMTypeNameInTree(final TreeObject selectedTO){
		String oldName = cmtypeTreeSelectedObject.getName();
		File oldCMTypeFile = RWTSystemUtil.getCMTypeFile(currentProject, cmtypeTreeSelectedObject);
		RWType editingCMType = RWType.readInCorrespondenceType(oldCMTypeFile);
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		CMTypeRenameDialog dialog = new CMTypeRenameDialog(window.getShell(), currentProject, cmtypeTreeSelectedObject );
		dialog.create();
		
		if(dialog.open()==Window.OK){
			String newName = cmtypeTreeSelectedObject.getName();
			if(!oldName.equals(newName)){
				File newCMTypeFile = RWTSystemUtil.getCMTypeFile(currentProject, cmtypeTreeSelectedObject);
				editingCMType.setTypeName(newName);
				oldCMTypeFile.renameTo(newCMTypeFile);
				RWType.writeOutCMType(editingCMType, newCMTypeFile);
				TreeObject.updateTreeObjectToFile(currentProject, cmtypeTreeSelectedObject);
				cmTypesTreeViewer.refresh();
			}
		}
	}
	
	private void associateConceptWithType(){
		if((currentProject != null) || (selectedCMType != null)){
			IWorkbench workbench = PlatformUI.getWorkbench();
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			if(selectedCMType==null){
				setErrorMessage("selected a cm type to view");
			}else{
				SelectConceptExplicationDialog dialog = new SelectConceptExplicationDialog(window.getShell(), currentProject, selectedCMType.getTypeName(), associatedExplicationText);
				dialog.create();
				dialog.open();		
			}
		}
	}

	private void createAttributeTableColumns(final TableViewer viewer) {
		Table table = viewer.getTable();
		String[] titles = { "Attribute", "Type", "Enable"};
		int[] bounds = { 100, 150, 150};
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
	
	private void createApproxTableColumns(final TableViewer viewer) {
		Table table = viewer.getTable();
		String[] titles = { "Approximate Property", "Value", "Description"};
		int[] bounds = { 200, 200, 200};
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
	
	private void handleBrowse() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(
				getShell(), ResourcesPlugin.getWorkspace().getRoot(), false,
				"Select project");		
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				containerText.setText(((Path) result[0]).toString());
				IResource container = ResourcesPlugin.getWorkspace().getRoot()
				.findMember(new Path(getContainerName()));
				this.currentProject =  container.getProject();
				cmtypeRootTreeObject = RWTSystemUtil.readInAllCMTypesToTreeObject(this.currentProject);
				cmTypesTreeViewer.setInput(cmtypeRootTreeObject);
			}
		}
	}
	
	private void dialogChanged() {
		IResource container = ResourcesPlugin.getWorkspace().getRoot()
				.findMember(new Path(getContainerName()));
		if (getContainerName().length() == 0) {
			updateStatus("File container must be specified");
			return;
		}
		if (container == null
				|| (container.getType() & (IResource.PROJECT )) == 0) {
			updateStatus("Project must exist");
			return;
		}
		if (!container.isAccessible()) {
			updateStatus("Project must be writable");
			return;
		}
		
		if(selectedCMType == null){
			updateStatus("A CM type must be selected for further actions");
			return;
		}
		updateStatus(null);
	}
	
	private boolean checkIfNumber(String testNumber){
        try {
            Long.parseLong(testNumber.split(",")[0]);
        } catch (NumberFormatException ex) {
            return false;
        }
        return true;
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public String getContainerName() {
		return containerText.getText();
	}
	
	public IProject getCurrentProject() {
		return currentProject;
	}

	public void setCurrentProject(IProject currentProject) {
		this.currentProject = currentProject;
	}
}