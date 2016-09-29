package rwtchecker.wizards;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

import rwtchecker.CM.CMType;
import rwtchecker.realworldmodel.ConceptDetail;
import rwtchecker.views.provider.ConceptDetailContentProvider;
import rwtchecker.views.provider.ConceptDetailLabelProvider;



public class NewCMTypeOperationPage extends WizardPage {

	public static final String PAGE_NAME = "NewCMTypeOperationPage";
	private Text containerText;

	//for the display of correspondence types
	private Tree cmTypesTree;

	private IProject currentProject;
	private Label definitonContentLabel;
	
	//for the display of concept detail
	private TableViewer viewer;
	private Text definitonContentText;
	
	//operands 
	private Text operandOneTypeText;
	private Text operandTwoTypeText;
	private Text returnTypeText;
	private Combo operationCombo;

	private String ruleContentsFilePath;
	

	private String selectedCMType = "";
	
	public NewCMTypeOperationPage() {
		super(PAGE_NAME);
		setTitle("New Correspondence Type page");
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
		
		//Project line
		Label label = new Label(container, SWT.NULL);
		label.setText("&Project:");
		label.setFont(titleFont);

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
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});
		
		//correspondence type information
		Label cmtypesList = new Label(container, SWT.NULL);
		cmtypesList.setText("Correspondence Types");
		cmtypesList.setFont(titleFont);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		cmtypesList.setLayoutData(gd);
		
		Label conceptDetailLabel = new Label(container, SWT.NULL);
		conceptDetailLabel.setText("Concept Detail Information");
		conceptDetailLabel.setFont(titleFont);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 5;
		conceptDetailLabel.setLayoutData(gd);
		
		cmTypesTree = new Tree(container, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		GridData cmtypeListGD = new GridData();
		cmtypeListGD.verticalAlignment = GridData.FILL;
		cmtypeListGD.horizontalAlignment = GridData.FILL;
		cmtypeListGD.verticalSpan = 9;
		cmTypesTree.setLayoutData(cmtypeListGD);
		
		cmTypesTree.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				TreeItem selectedItem = cmTypesTree.getSelection()[0];
				if(selectedItem.getData() !=null){
					CMType cmtype = (CMType)selectedItem.getData();
					ConceptDetail explication = ConceptDetail.readInByLink(cmtype.getSemanticType().getExplicationLink());
					definitonContentText.setText(explication.getDefinition());
					viewer.setInput(explication.getAttributes());
					definitonContentLabel.setText("Definition: "+cmtype.getTypeName());
					selectedCMType = cmtype.getTypeName();
				}
			} 
		});
		
		//definition 
		definitonContentLabel = new Label(container, SWT.NONE);
		definitonContentLabel.setText("Definition: ");
		GridData definitonContentLabelgd = new GridData();
		definitonContentLabelgd.verticalSpan = 1;
		definitonContentLabelgd.widthHint = 400;
		definitonContentLabel.setLayoutData(definitonContentLabelgd);
		
		definitonContentText = new Text(container, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		definitonContentText.setText("");
		
		GridData definitonContentTextgd = new GridData();
		definitonContentTextgd.verticalAlignment = GridData.FILL;
		definitonContentTextgd.horizontalAlignment = GridData.FILL;
		definitonContentTextgd.grabExcessHorizontalSpace = true;
		definitonContentTextgd.grabExcessVerticalSpace = true;
		definitonContentTextgd.horizontalSpan = 5;
		definitonContentTextgd.verticalSpan = 4;
		definitonContentTextgd.widthHint = 400;
		definitonContentText.setLayoutData(definitonContentTextgd);
		
        viewer = new TableViewer(container, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        GridData correspondenceTypeDetailGD = new GridData();
		correspondenceTypeDetailGD.verticalAlignment = GridData.FILL;
		correspondenceTypeDetailGD.horizontalAlignment = GridData.FILL;
		correspondenceTypeDetailGD.grabExcessHorizontalSpace = true;
		correspondenceTypeDetailGD.grabExcessVerticalSpace = true;
		correspondenceTypeDetailGD.horizontalSpan = 5;
		correspondenceTypeDetailGD.verticalSpan = 4;
		correspondenceTypeDetailGD.widthHint = 400;
        createAttributeTableColumns(viewer);
        viewer.getTable().setLayoutData(correspondenceTypeDetailGD);
		viewer.setContentProvider(new ConceptDetailContentProvider());
		viewer.setLabelProvider(new ConceptDetailLabelProvider());
		viewer.setInput(null);


        //operand one part
		Label operandOnelabel = new Label(container, SWT.NULL);
		operandOnelabel.setText("Operand One:");
		operandOnelabel.setFont(titleFont);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
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
	    operandOneTypeTextGD.horizontalSpan = 4;
	    operandOneTypeText.setLayoutData(operandOneTypeTextGD);

		Button operandOneButton = new Button(container, SWT.PUSH);
		operandOneButton.setText("Add Selected...");
		operandOneButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if(selectedCMType.length() != 0){
					operandOneTypeText.setText(selectedCMType);
				}
			}
		});
		
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
		operandTwoTypeTextGD.horizontalSpan = 4;
		operandTwoTypeText.setLayoutData(operandTwoTypeTextGD);
		
		Button operandTwoButton = new Button(container, SWT.PUSH);
		operandTwoButton.setText("Add Selected...");
		operandTwoButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if(selectedCMType.length() != 0){
					operandTwoTypeText.setText(selectedCMType);
				}
			}
		});
		
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
		returnTypeTextGD.horizontalSpan = 4;
		returnTypeText.setLayoutData(returnTypeTextGD);

		Button returnTypeTextButton = new Button(container, SWT.PUSH);
		returnTypeTextButton.setText("Add Selected...");
		returnTypeTextButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if(selectedCMType.length() != 0){
					returnTypeText.setText(selectedCMType);
				}
			}
		});
		
        //Operation part
		Label operationChoiceLabel = new Label(container, SWT.NULL);
		operationChoiceLabel.setText("Operation type:");
		operationChoiceLabel.setFont(titleFont);
		operationChoiceLabel.setLayoutData(gridData);

		operationCombo = new Combo(container, SWT.BORDER | SWT.SINGLE);
//		String[] operationTypes = CMTypeOperationManager.OPERATION_TYPES;
//	    for(int i=0; i<operationTypes.length; i++){
//	    	operationCombo.add(operationTypes[i]);
//	    }
	    operationCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
	    GridData operationChoiceGD = new GridData();
	    operationChoiceGD.horizontalSpan = 2;
	    operationChoiceGD.horizontalAlignment = GridData.FILL;
	    operationChoiceGD.grabExcessHorizontalSpace = true;
	    operationChoiceGD.horizontalSpan = 5;
	    operationCombo.setLayoutData(operationChoiceGD);
		
	    this.getShell().setSize(1000, 800);
	    
		dialogChanged();
		setControl(container);
		setPageComplete(true);
	}

	private void createAttributeTableColumns(final TableViewer viewer) {
		Table table = viewer.getTable();
		String[] titles = { "Attribue", "Definition"};
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
				CMType[] cmtypes = CMType.readInAllCMTypes(this.currentProject);
				cmTypesTree.removeAll();
				TreeItem rootItem = new TreeItem(cmTypesTree, 0);
				rootItem.setText("CM types List");
				for(int i=0;i<cmtypes.length;i++){
					TreeItem childItem = new TreeItem(rootItem, 0);
					childItem.setText(cmtypes[i].getTypeName());
					childItem.setData(cmtypes[i]);
				}
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
		if (operationCombo.getText().length() == 0) {
			updateStatus("Please select operation");
			return;
		}
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
//		setPageComplete(message == null);
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
		return this.operationCombo.getText();
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