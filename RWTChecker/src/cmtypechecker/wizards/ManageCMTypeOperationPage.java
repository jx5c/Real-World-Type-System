package cmtypechecker.wizards;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

import cmtypechecker.CM.CM_SemanticType;
import cmtypechecker.CM.CMType;
import cmtypechecker.dialogs.OperationSelectDialog;
import cmtypechecker.dialogs.SelectConceptExplicationDialog;
import cmtypechecker.realworldmodel.ConceptDetail;
import cmtypechecker.util.ActivePart;
import cmtypechecker.util.CMModelUtil;
import cmtypechecker.util.CMModelUtil;
import cmtypechecker.views.provider.CMApproTableContentProvider;
import cmtypechecker.views.provider.CMApproTablelLabelProvider;
import cmtypechecker.views.provider.CMAttributeTableContentProvider;
import cmtypechecker.views.provider.CMAttributeTablelLabelProvider;
import cmtypechecker.views.provider.CMViewTreeContentProvider;
import cmtypechecker.views.provider.CMViewTreeViewLabelProvider;
import cmtypechecker.views.provider.TreeObject;



public class ManageCMTypeOperationPage extends WizardPage {

	public static final String PAGE_NAME = "ManageCMTypeOperationPage";
	private Text containerText;

	private IProject currentProject;
	
	//operands 
	private Text operandOneTypeText;
	private Text operandTwoTypeText;
	private Text returnTypeText;
	private Text operationText;

	private String ruleContentsFilePath;

	private Label CMtypeDetailLabel;
	private CMType selectedNewCMType;
	private Text associatedExplicationText;
	private TableViewer typeAttributeViewer;
	private TableViewer approxAttributeViewer;
	
	private TreeViewer cmTypesTreeViewer;
	private Action clickActionOnTreeViewer;
	protected TreeObject cmtypeTreeSelectedObject;
	
	
	public ManageCMTypeOperationPage() {
		super(PAGE_NAME);
		setTitle("New operation for Correspondence Type");
		setDescription("This wizard creates a new operation based on correspondence types.");
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		
		
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 6;
		layout.verticalSpacing = 9;
		Font titleFont = new Font(parent.getDisplay(),"courier new", 9 , SWT.ITALIC );
		Font boldTitleFont = new Font(parent.getDisplay(),"courier new", 10 , SWT.BOLD );
		
		Font titleOneFont = new Font(parent.getDisplay(),"courier new", 9 , SWT.BOLD );
		Font titleTwoFont = new Font(parent.getDisplay(),"arial", 10 , SWT.BOLD );
		
		//Project line
		Label projectLabel = new Label(container, SWT.NULL);
		projectLabel.setText("&Project:");
		projectLabel.setFont(titleFont);
		GridData projectLabelgridData = new GridData();
		projectLabelgridData.horizontalSpan = 1;
		projectLabel.setLayoutData(projectLabelgridData);

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

		Button browserButton = new Button(container, SWT.PUSH);
		browserButton.setText("Browse...");
		browserButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});
		GridData browserButtongridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		browserButtongridData.horizontalSpan = 1;
		browserButton.setLayoutData(browserButtongridData);
		
		Label placeHolderLabel = new Label(container, SWT.NULL);
		placeHolderLabel.setText("Select the relevant type in this permitted operation");
		placeHolderLabel.setFont(titleTwoFont);
		GridData placeHolderLabelgridData = new GridData();
		placeHolderLabelgridData.horizontalAlignment = GridData.FILL;
		placeHolderLabelgridData.horizontalSpan = 6;
		placeHolderLabel.setLayoutData(placeHolderLabelgridData);
		
        //Operation part
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.horizontalSpan = 1;
		
		Label operationChoiceLabel = new Label(container, SWT.NULL);
		operationChoiceLabel.setText("Operation type:");
		operationChoiceLabel.setFont(titleFont);
		operationChoiceLabel.setLayoutData(gridData);
		
		operationText = new Text(container, SWT.BORDER | SWT.SINGLE);
		operationText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
	    GridData operationChoiceGD = new GridData();
	    operationChoiceGD.horizontalAlignment = GridData.FILL;
	    operationChoiceGD.grabExcessHorizontalSpace = true;
	    operationChoiceGD.horizontalSpan = 4;
	    operationText.setLayoutData(operationChoiceGD);
	    
		Button operationSelectButton = new Button(container, SWT.PUSH);
		operationSelectButton.setText("Select one");
		operationSelectButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		operationSelectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				//operationSelectDialog
				IWorkbench workbench = PlatformUI.getWorkbench();
	    		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
	    		OperationSelectDialog dialog = new OperationSelectDialog(window.getShell(), operationText, operandTwoTypeText);
				dialog.create();
				dialog.open();	
			}
		});
		
        //operand one part
		Label operandOnelabel = new Label(container, SWT.NULL);
		operandOnelabel.setText("Operand One:");
		operandOnelabel.setFont(titleFont);
		operandOnelabel.setLayoutData(gridData);

		operandOneTypeText = new Text(container, SWT.BORDER | SWT.SINGLE);
		operandOneTypeText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
	    GridData operandOneTypeTextGD = new GridData();
	    operandOneTypeTextGD.horizontalAlignment = GridData.FILL;
	    operandOneTypeTextGD.grabExcessHorizontalSpace = true;
	    operandOneTypeTextGD.horizontalSpan = 5;
	    operandOneTypeText.setLayoutData(operandOneTypeTextGD);
		
        //operand two part
		Label operandTwoLabel = new Label(container, SWT.NULL);
		operandTwoLabel.setText("Operand Two:");
		operandTwoLabel.setFont(titleFont);
		operandTwoLabel.setLayoutData(gridData);

		operandTwoTypeText = new Text(container, SWT.BORDER | SWT.SINGLE);
		operandTwoTypeText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		GridData operandTwoTypeTextGD = new GridData();
		operandTwoTypeTextGD.horizontalAlignment = GridData.FILL;
		operandTwoTypeTextGD.grabExcessHorizontalSpace = true;
		operandTwoTypeTextGD.horizontalSpan = 5;
		operandTwoTypeText.setLayoutData(operandTwoTypeTextGD);
		
        //return type part
		Label returnTypeTextLabel = new Label(container, SWT.NULL);
		returnTypeTextLabel.setText("Return type:");
		returnTypeTextLabel.setFont(titleFont);
		returnTypeTextLabel.setLayoutData(gridData);

		returnTypeText = new Text(container, SWT.BORDER | SWT.SINGLE);
		returnTypeText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		GridData returnTypeTextGD = new GridData();
		returnTypeTextGD.horizontalAlignment = GridData.FILL;
		returnTypeTextGD.grabExcessHorizontalSpace = true;
		returnTypeTextGD.horizontalSpan = 5;
		returnTypeText.setLayoutData(returnTypeTextGD);

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
		
		clickActionOnTreeViewer = new Action(){
			public void run() {
				ISelection selection = cmTypesTreeViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if(obj != null){
					cmtypeTreeSelectedObject = (TreeObject)obj;
					selectedNewCMType = CMModelUtil.getCMTypeFromTreeObject(currentProject, cmtypeTreeSelectedObject);
					if(selectedNewCMType != null){
						CMtypeDetailLabel.setText("Type Detail: "+selectedNewCMType.getTypeName());
						CM_SemanticType thisType = selectedNewCMType.getSemanticType();
						ConceptDetail explication = ConceptDetail.readInByLink(thisType.getExplicationLink());
						associatedExplicationText.setText(explication.getConceptName());
						typeAttributeViewer.setInput(selectedNewCMType.getSemanticType());
						approxAttributeViewer.setInput(selectedNewCMType.getApproximationType());
					}
				}
			}
		};
		
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
		viewExplicationBT.setText("Review the Concept");
		GridData viewExplicationBTgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		viewExplicationBTgd.horizontalSpan = 1;
		viewExplicationBT.setLayoutData(viewExplicationBTgd);
		
        typeAttributeViewer = new TableViewer(container, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL
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
		/**************************/
//	    this.getShell().setSize(1000, 1000);
	    
		dialogChanged();
		
		addDNDSupport();
		setControl(container);
		setPageComplete(true);
		loadCurrentProject();
	}
	
	private void loadCurrentProject(){
		IFile currentFile =  ActivePart.getFileOfActiveEditror();
		if( currentFile != null){
	        currentProject = currentFile.getProject();	
	        this.containerText.setText(currentProject.getName());
			TreeObject cmtypeTreeObject = CMModelUtil.readInAllCMTypesToTreeObject(this.currentProject);
			cmTypesTreeViewer.setInput(cmtypeTreeObject);
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
		
	/**
	 * Uses the standard container selection dialog to choose the new value for
	 * the container field.
	 */

	private void handleBrowse() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(
				getShell(), ResourcesPlugin.getWorkspace().getRoot(), false,
				"Select target project container");
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				containerText.setText(((Path) result[0]).toString());
				IResource container = ResourcesPlugin.getWorkspace().getRoot()
				.findMember(new Path(getContainerName()));
				this.currentProject =  container.getProject();
				TreeObject cmtypeTreeObject = CMModelUtil.readInAllCMTypesToTreeObject(this.currentProject);
				cmTypesTreeViewer.setInput(cmtypeTreeObject);
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
		if (operandOneTypeText.getText().length() == 0) {
			updateStatus("Please select operand type one");
			return;
		}
		if (operandTwoTypeText.getText().length() == 0) {
			updateStatus("Please select operand type Two");
			return;
		}
		
		if (returnTypeText.getText().length() == 0) {
			updateStatus("Please select return type");
			return;
		}
		if (operationText.getText().length() == 0) {
			updateStatus("Please select operation");
			return;
		}
		updateStatus(null);
	}

	private void addDNDSupport(){
		Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
		int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;
		
	    final DragSource source = new DragSource(cmTypesTreeViewer.getTree(), operations);
	    source.setTransfer(types);
	    source.addDragListener(new DragSourceListener() {
		public void dragStart(DragSourceEvent event) {
				ISelection selection = cmTypesTreeViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if(obj != null){
					event.doit = true;
//					dragedTreeObject = (TreeObject)obj;
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

	    DropTarget target = new DropTarget(operandOneTypeText, operations);
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
	        String text = (String) event.data;
	        CMType dragedType = CMModelUtil.getCMTypeFromTypeName(currentProject, text);
	        if(dragedType != null){
	        	operandOneTypeText.setText(dragedType.getEnabledAttributeSet());	
	        }
	      }
	     });
	    
	    target = new DropTarget(operandTwoTypeText, operations);
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
	        String text = (String) event.data;
	        CMType dragedType = CMModelUtil.getCMTypeFromTypeName(currentProject, text);
	        if(dragedType != null){
	        	operandTwoTypeText.setText(dragedType.getEnabledAttributeSet());	
	        }
	      }
	     });
	    
	    target = new DropTarget(returnTypeText, operations);
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
	        String text = (String) event.data;
	        CMType dragedType = CMModelUtil.getCMTypeFromTypeName(currentProject, text);
	        if(dragedType != null){
	        	returnTypeText.setText(dragedType.getEnabledAttributeSet());	
	        }
	      }
	     });
	}
	
	private void updateStatus(String message) {
		setErrorMessage(message);
	}
	
	public String getOperandTypeOne(){
		return this.operandOneTypeText.getText();
	}
	
	public String getOperandTypeTwo(){
		return this.operandTwoTypeText.getText();
	}
	
	public String getReturnType(){
		return this.returnTypeText.getText();
	}
	
	public String getOperation(){
		return this.operationText.getText();
	}
	
	public String getContainerName() {
		return containerText.getText();
	}
	
	public IProject getCurrentIProject(){
		return this.currentProject;
	}

	public String getRuleContentsFilePath() {
		return ruleContentsFilePath;
	}

	public void setRuleContentsFilePath(String ruleContentsFilePath) {
		this.ruleContentsFilePath = ruleContentsFilePath;
	}
}