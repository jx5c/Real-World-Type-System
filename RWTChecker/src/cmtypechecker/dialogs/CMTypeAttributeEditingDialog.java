package cmtypechecker.dialogs;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import cmtypechecker.CM.CMAttribute;
import cmtypechecker.CM.CMType;
import cmtypechecker.CM.CM_SemanticType;
import cmtypechecker.realworldmodel.ConceptDetail;
import cmtypechecker.util.CMModelUtil;
import cmtypechecker.views.provider.CMApproTableContentProvider;
import cmtypechecker.views.provider.CMApproTablelLabelProvider;
import cmtypechecker.views.provider.CMAttributeTableContentProvider;
import cmtypechecker.views.provider.CMAttributeTablelLabelProvider;
import cmtypechecker.views.provider.CMViewTreeContentProvider;
import cmtypechecker.views.provider.CMViewTreeViewLabelProvider;
import cmtypechecker.views.provider.TreeObject;


public class CMTypeAttributeEditingDialog extends TitleAreaDialog {

	private IProject currentProject;
	private Text selectedAttributeText;
	private Text semanticTypeText;
	private Text specificTypeText;
	private CMType selectedNewCMType;
	private Label CMtypeDetailLabel;
	private TableViewer typeAttributeViewer;
	private Text associatedExplicationText;
	private TableViewer approxAttributeViewer;
	private CM_SemanticType selectedSemanticType;
	private TableViewer orginialViewer;
	private CMAttribute modifyAtt;
	private TreeViewer cmTypesTreeViewer;
	private Action clickActionOnTreeViewer;
	protected TreeObject cmtypeTreeSelectedObject;
	
	public CMTypeAttributeEditingDialog(Shell parentShell, TableViewer viewer, CM_SemanticType selectedSemanticType, IProject currentProject) {
		super(parentShell);
		orginialViewer = viewer;
		this.currentProject = currentProject;
		this.selectedSemanticType = selectedSemanticType;
	}

	@Override
	public void create() {
		super.create();
		setTitle("This is the dialog for attribute editing");
		setMessage("use this dialog to edit the attribute", IMessageProvider.INFORMATION);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL );
		GridLayout layout = new GridLayout();
		layout.numColumns = 6;
		layout.verticalSpacing = 12;
		container.setLayout(layout);

		final Font titleFont = new Font(parent.getDisplay(),"courier new", 9 , SWT.ITALIC );
		Font boldTitleFont = new Font(parent.getDisplay(),"courier new", 10 , SWT.BOLD );
		final Font enableFont = new Font(parent.getDisplay(),"courier new", 10 , SWT.BOLD );
		
		Font titleOneFont = new Font(parent.getDisplay(),"courier new", 9 , SWT.BOLD );
		Font titleTwoFont = new Font(parent.getDisplay(),"arial", 10 , SWT.BOLD );
		
		// selected attribute row
		final Label selectedAttributeLabel = new Label(container, SWT.NULL);
		selectedAttributeLabel.setText("Attribute:");
		selectedAttributeLabel.setFont(titleTwoFont);
		GridData selectedAttributeLabelgridData = new GridData();
		selectedAttributeLabelgridData.horizontalSpan = 1;
		selectedAttributeLabelgridData.horizontalAlignment = GridData.FILL;
		selectedAttributeLabel.setLayoutData(selectedAttributeLabelgridData);
		
		selectedAttributeText = new Text(container, SWT.NULL);
		selectedAttributeText.setText("");
		selectedAttributeText.setFont(boldTitleFont);
		selectedAttributeText.setEnabled(true);
		GridData selectedAttributeTextData = new GridData();
		selectedAttributeTextData.horizontalAlignment = GridData.FILL;
		selectedAttributeTextData.horizontalSpan = 5;
		selectedAttributeText.setLayoutData(selectedAttributeTextData);
		
		//existing type line
		final Label semanticTypeLabel = new Label(container, SWT.NULL);
		semanticTypeLabel.setText("Value of the attribute:");
		semanticTypeLabel.setFont(titleTwoFont);
		GridData semanticTypeLabelgridData = new GridData();
		semanticTypeLabelgridData.horizontalSpan = 1;
		semanticTypeLabelgridData.horizontalAlignment = GridData.FILL;
		semanticTypeLabel.setLayoutData(semanticTypeLabelgridData);
		
		semanticTypeText = new Text(container, SWT.NULL|SWT.BORDER);
		semanticTypeText.setText("");
		semanticTypeText.setEditable(false);
		semanticTypeText.setFont(boldTitleFont);
//		selectedAttributeText.setEditable(false);
		semanticTypeText.setEnabled(true);
		GridData semanticTypeTextData = new GridData();
		semanticTypeTextData.horizontalAlignment = GridData.FILL;
		semanticTypeTextData.horizontalSpan = 5;
		semanticTypeText.setLayoutData(semanticTypeTextData);
		
		/***type information***/
		Label associateTypeLabel = new Label(container, SWT.NULL );
		associateTypeLabel.setText("Associate a type with the attribute:");
		associateTypeLabel.setToolTipText("a specific real world value or another correspondence type");
		associateTypeLabel.setFont(titleOneFont);
		GridData associateTypeLabelData = new GridData();
		associateTypeLabelData.horizontalAlignment = GridData.FILL;
		associateTypeLabelData.horizontalSpan = 6;
		associateTypeLabel.setLayoutData(associateTypeLabelData);
		
		//radio button for type choice
	    Label typeChoiceLabel = new Label(container, SWT.NULL);
	    typeChoiceLabel.setFont(boldTitleFont);
	    typeChoiceLabel.setText("Type Options:");
	    final Button primitiveTypeButton = new Button(container, SWT.RADIO);
	    primitiveTypeButton.setText("Simple Type");
	    final Button existingTypeButton = new Button(container, SWT.RADIO);
	    existingTypeButton.setText("Another CM Type");
		GridData typeChoiceLabelData = new GridData();
		typeChoiceLabelData.horizontalAlignment = GridData.FILL;
		typeChoiceLabelData.horizontalSpan = 1;
		typeChoiceLabel.setLayoutData(typeChoiceLabelData);
		existingTypeButton.setLayoutData(typeChoiceLabelData);
		
		Label placeHolderLabel = new Label(container, SWT.NULL);
		GridData placeHolderLabelData = new GridData();
		placeHolderLabelData.horizontalSpan = 3;
		placeHolderLabelData.grabExcessHorizontalSpace = true;
		placeHolderLabel.setLayoutData(placeHolderLabelData);
	    
		//primitive type line
		final Label primitiveTypeLabel = new Label(container, SWT.NULL);
		primitiveTypeLabel.setText("Input a simple type:  ");
		primitiveTypeLabel.setFont(titleFont);
		GridData gridData = new GridData();
		gridData.horizontalSpan = 1;
		gridData.horizontalAlignment = GridData.FILL;
		primitiveTypeLabel.setLayoutData(gridData);

		specificTypeText = new Text(container, SWT.BORDER | SWT.WRAP);
	    GridData specificTypeTextGD = new GridData();
	    specificTypeTextGD.verticalAlignment = GridData.FILL;
	    specificTypeTextGD.horizontalAlignment = GridData.FILL;
	    specificTypeTextGD.grabExcessHorizontalSpace = true;
	    specificTypeTextGD.horizontalSpan = 5;
	    specificTypeText.setLayoutData(specificTypeTextGD);
	    specificTypeText.setEnabled(false);
	    specificTypeText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				semanticTypeText.setText(specificTypeText.getText());
			}
		});
		
	    //existing correspondence type
		final Label existingCMTypeLabel = new Label(container, SWT.NULL);
		existingCMTypeLabel.setText("Another CM type");
		existingCMTypeLabel.setFont(titleFont);
		GridData existingCMTypeLabelgridData = new GridData();
		existingCMTypeLabelgridData.horizontalSpan = 1;
		existingCMTypeLabelgridData.horizontalAlignment = GridData.FILL;
		existingCMTypeLabel.setLayoutData(existingCMTypeLabelgridData);
		
		final Text existingCMTypeText = new Text(container, SWT.NULL);
		existingCMTypeText.setText("");
		existingCMTypeText.setFont(boldTitleFont);
		existingCMTypeText.setEditable(false);
		existingCMTypeText.setEnabled(false);
		GridData existingCMTypeTextData = new GridData();
		existingCMTypeTextData.horizontalAlignment = GridData.FILL;
		existingCMTypeTextData.horizontalSpan = 5;
		existingCMTypeText.setLayoutData(existingCMTypeTextData);
		
		//correspondence type information
		Label cmtypesList = new Label(container, SWT.NULL);
		cmtypesList.setText("CM Types");
		cmtypesList.setFont(titleOneFont);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		cmtypesList.setLayoutData(gd);
		
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
		cmtypeListGD.verticalSpan = 6;
		cmTypesTreeViewer.getTree().setLayoutData(cmtypeListGD);
		
		cmTypesTreeViewer.setContentProvider(new CMViewTreeContentProvider());
		cmTypesTreeViewer.setLabelProvider(new CMViewTreeViewLabelProvider());
		cmTypesTreeViewer.setInput(null);
		cmTypesTreeViewer.setAutoExpandLevel(4);
		
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
		associatedExplicationText.setEditable(false);
		
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
		viewExplicationBTgd.verticalSpan = 1;
		viewExplicationBT.setLayoutData(viewExplicationBTgd);
		
        typeAttributeViewer = new TableViewer(container, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        GridData correspondenceTypeDetailGD = new GridData();
		correspondenceTypeDetailGD.verticalAlignment = GridData.FILL;
		correspondenceTypeDetailGD.horizontalAlignment = GridData.FILL;
		correspondenceTypeDetailGD.grabExcessHorizontalSpace = true;
		correspondenceTypeDetailGD.horizontalSpan = 5;
		correspondenceTypeDetailGD.verticalSpan = 1;
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
        approxAttributeViewerGD.grabExcessHorizontalSpace = true;
        approxAttributeViewerGD.grabExcessVerticalSpace = true;
        approxAttributeViewerGD.horizontalSpan = 5;
        approxAttributeViewerGD.verticalSpan = 1;
        approxAttributeViewerGD.heightHint = 100;
        createApproxTableColumns(approxAttributeViewer);
        approxAttributeViewer.getTable().setLayoutData(approxAttributeViewerGD);
        approxAttributeViewer.setContentProvider(new CMApproTableContentProvider());
        approxAttributeViewer.setLabelProvider(new CMApproTablelLabelProvider());
        approxAttributeViewer.setInput(null);
		/**************************/
		
		Listener radioGroup = new Listener () {
		    public void handleEvent (Event event) {
		      Button button = (Button) event.widget;
		      if(button.getText().equals("Simple Type")){
		    	  specificTypeText.setEnabled(true);
		    	  primitiveTypeLabel.setFont(enableFont);
		    	  existingCMTypeLabel.setFont(titleFont);
		    	  existingCMTypeText.setEnabled(false);
		    	  primitiveTypeButton.setSelection(true);
		    	  existingTypeButton.setSelection(false);
		    	  cmTypesTreeViewer.getTree().setEnabled(false);
		      }
		      if(button.getText().equals("Another CM Type")){
		    	  specificTypeText.setEnabled(false);
		    	  primitiveTypeLabel.setFont(titleFont);
		    	  existingCMTypeLabel.setFont(enableFont);
		    	  existingCMTypeText.setEnabled(true);
		    	  primitiveTypeButton.setSelection(false);
		    	  existingTypeButton.setSelection(true);
		    	  cmTypesTreeViewer.getTree().setEnabled(true);
		    	  loadCMTypes();
		      }
		    }
		};
		primitiveTypeButton.addListener(SWT.Selection, radioGroup);
		existingTypeButton.addListener(SWT.Selection, radioGroup);
		
		TableItem selectedItem = orginialViewer.getTable().getSelection()[0];
		modifyAtt = new CMAttribute(selectedItem.getText(0), selectedItem.getText(1));
		this.selectedAttributeText.setText(selectedItem.getText(0));
		this.semanticTypeText.setText(selectedItem.getText(1));
		this.specificTypeText.setText(selectedItem.getText(1));
		return container;
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

	private void loadCMTypes(){
		TreeObject cmtypeTreeObject = CMModelUtil.readInAllCMTypesToTreeObject(this.currentProject);
		cmTypesTreeViewer.setInput(cmtypeTreeObject);
	}


	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected void okPressed() {
		String attributeName = selectedAttributeText.getText();
		String attributeType = semanticTypeText.getText();
		if(attributeName.length()>0){
			CMAttribute newAtt = new CMAttribute(attributeName, attributeType);
			if(selectedSemanticType.getSemanticTypeAttributes().contains(modifyAtt)){
				  int index = selectedSemanticType.getSemanticTypeAttributes().indexOf(modifyAtt);
				  selectedSemanticType.getSemanticTypeAttributes().set(index, newAtt);
				  orginialViewer.setInput(selectedSemanticType); 
				  
			}
		}
		super.okPressed();
	}
}
