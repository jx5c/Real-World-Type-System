package cmtypechecker.dialogs;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;

import cmtypechecker.realworldmodel.ConceptDetail;
import cmtypechecker.views.provider.ConceptDetailContentProvider;
import cmtypechecker.views.provider.ConceptDetailLabelProvider;
import cmtypechecker.wizards.ManageConceptWizard;

public class SelectConceptExplicationDialog extends TitleAreaDialog {

	private Text correspondenceTypeText;

	private Text selectedConceptText;
	
	private ConceptDetail selectedConceptDetail;
	
	private TableViewer viewer;
	private Text definitonContentText;
	private Label definitonContentLabel;

	private Tree conceptListTree;

	private IProject currentProject;
	private String selectCMTypeName = "";
	
	private Text associatedExplicationText;
	
	public SelectConceptExplicationDialog(Shell parentShell, IProject iproject, String selectCMTypeName, Text associatedExplicationText) {
		super(parentShell);
		currentProject = iproject;
		this.selectCMTypeName = selectCMTypeName;
		this.associatedExplicationText = associatedExplicationText;
	}

	@Override
	public void create() {
		super.create();
		setTitle("This is the dialog for attribute editing");
		setMessage("use this dialog to edit the attribute", IMessageProvider.INFORMATION);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.V_SCROLL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 6;
		Font titleOneFont = new Font(parent.getDisplay(),"courier new", 9 , SWT.BOLD );
		Font titleTwoFont = new Font(parent.getDisplay(),"arial", 10 , SWT.BOLD );
				
		//Correspondence Type Name line
		Label label = new Label(container, SWT.NULL);
		label.setText("&Correspondence Type name:");
		label.setFont(titleOneFont);
		
		correspondenceTypeText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		correspondenceTypeText.setLayoutData(gd);
		gd.horizontalSpan = 5;
		
		//place holder line
		label = new Label(container, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 5;
		label.setLayoutData(gd);
		label.setFont(titleTwoFont);
		label.setText("&Existing Concept");
			
		Button createNewConceptButton = new Button(container, SWT.PUSH);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment = GridData.END;
		createNewConceptButton.setText("Create New Concept");
		createNewConceptButton.setLayoutData(gd);
		createNewConceptButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ManageConceptWizard wizard = new ManageConceptWizard();
				wizard.init(PlatformUI.getWorkbench(), null);
			    WizardDialog dialog = new WizardDialog(SelectConceptExplicationDialog.this.getShell(), wizard);
			    dialog.create();
			    dialog.open();
			}
		});
		
		//Concept detail type information
		Label conceptList = new Label(container, SWT.NULL);
		conceptList.setText("Concept List");
		conceptList.setFont(titleOneFont);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		conceptList.setLayoutData(gd);
		
		Label conceptDetailLabel = new Label(container, SWT.NULL);
		conceptDetailLabel.setText("Concept Detail Information");
		conceptDetailLabel.setFont(titleOneFont);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 5;
		conceptDetailLabel.setLayoutData(gd);
		
		conceptListTree = new Tree(container, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		GridData cmtypeListGD = new GridData();
		cmtypeListGD.verticalAlignment = GridData.FILL;
		cmtypeListGD.horizontalAlignment = GridData.FILL;
		cmtypeListGD.verticalSpan = 9;
		cmtypeListGD.heightHint = 450;
		conceptListTree.setLayoutData(cmtypeListGD);
		
		conceptListTree.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				TreeItem selectedItem = conceptListTree.getSelection()[0];
				if(selectedItem.getData() !=null){
					ConceptDetail conceptDetail = (ConceptDetail)selectedItem.getData();
					definitonContentText.setText(conceptDetail.getDefinition());
					viewer.setInput(conceptDetail.getAttributes());
					definitonContentLabel.setText("Definition: "+conceptDetail.getConceptName());
					selectedConceptText.setText(conceptDetail.getConceptName());
					selectedConceptDetail = conceptDetail;
				}
			} 
		});
		
		//Concept Detail 
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
//		definitonContentTextgd.grabExcessVerticalSpace = true;
		definitonContentTextgd.horizontalSpan = 5;
		definitonContentTextgd.verticalSpan = 4;
		definitonContentTextgd.widthHint = 400;
		definitonContentTextgd.heightHint = 200;
		definitonContentText.setLayoutData(definitonContentTextgd);
		
        viewer = new TableViewer(container, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        GridData conceptDetailGD = new GridData();
        conceptDetailGD.verticalAlignment = GridData.FILL;
        conceptDetailGD.horizontalAlignment = GridData.FILL;
        conceptDetailGD.grabExcessHorizontalSpace = true;
//        conceptDetailGD.grabExcessVerticalSpace = true;
        conceptDetailGD.horizontalSpan = 5;
        conceptDetailGD.verticalSpan = 4;
        conceptDetailGD.widthHint = 400;
        conceptDetailGD.heightHint = 200;
        createAttributeTableColumns(viewer);
        viewer.getTable().setLayoutData(conceptDetailGD);
		viewer.setContentProvider(new ConceptDetailContentProvider());
		viewer.setLabelProvider(new ConceptDetailLabelProvider());
		viewer.setInput(null);

		//display of selected real world concept
		label = new Label(container, SWT.NULL);
		label.setText("&Selected Concept:");
		label.setFont(titleOneFont);
		
		selectedConceptText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData selectedConceptTextgd = new GridData(GridData.FILL_HORIZONTAL);
		selectedConceptText.setLayoutData(selectedConceptTextgd);
		selectedConceptTextgd.horizontalSpan = 5;
		selectedConceptText.setEditable(false);
		return container;
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

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		GridLayout layout = new GridLayout(2, false);
		layout.numColumns = 2;
		parent.setLayout(layout);
		Button OKbutton = new Button(parent, SWT.PUSH);
		OKbutton.setText("OK");
		OKbutton.setFont(JFaceResources.getDialogFont());
		OKbutton.setData(new Integer(OK));
		GridData OKbuttonGridData = new GridData();
		OKbuttonGridData.grabExcessHorizontalSpace = true;
		OKbuttonGridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_CENTER;
		
		OKbutton.setLayoutData(OKbuttonGridData);
		OKbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (isValidInput()) {
					okPressed();
				}
			}
		});
		Shell shell = parent.getShell();
		if (shell != null) {
			shell.setDefaultButton(OKbutton);
		}
		
		Button cancelButton = createButton(parent, CANCEL, "Cancel", false);
		cancelButton.setLayoutData(OKbuttonGridData);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setReturnCode(CANCEL);
				close();
			}
		});
		configAtWhenInitilized();
	}
	
	public void configAtWhenInitilized(){
		this.correspondenceTypeText.setText(selectCMTypeName);
		if(this.currentProject != null){
			ConceptDetail[] conceptList = ConceptDetail.readInAllConceptDetails(this.currentProject);
			conceptListTree.removeAll();
			TreeItem rootItem = new TreeItem(conceptListTree, 0);
			rootItem.setText("Concept List");
			for(int i=0;i<conceptList.length;i++){
				TreeItem childItem = new TreeItem(rootItem, 0);
				childItem.setText(conceptList[i].getConceptName());
				childItem.setData(conceptList[i]);
			}
			rootItem.setExpanded(true);
			selectedConceptText.setText(associatedExplicationText.getText());
		}
	}

	private boolean isValidInput() {
		boolean valid = true;
		if (selectedConceptDetail == null) {
			setErrorMessage("Please select the target concept");
			valid = false;
		}
		return valid;
	}

	@Override
	protected boolean isResizable() {
		return false;
	}



	@Override
	protected void okPressed() {
		associatedExplicationText.setText(selectedConceptDetail.getConceptName());
		super.okPressed();
	}
}
