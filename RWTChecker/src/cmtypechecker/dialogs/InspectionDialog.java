package cmtypechecker.dialogs;


import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
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

import cmtypechecker.CM.CMType;
import cmtypechecker.CM.CM_SemanticType;
import cmtypechecker.realworldmodel.ConceptDetail;
import cmtypechecker.typechecker.NewTypeCheckerVisitor;
import cmtypechecker.util.CMModelUtil;
import cmtypechecker.views.provider.CMApproTableContentProvider;
import cmtypechecker.views.provider.CMApproTablelLabelProvider;
import cmtypechecker.views.provider.CMAttributeTableContentProvider;
import cmtypechecker.views.provider.CMAttributeTablelLabelProvider;
import cmtypechecker.views.provider.CMViewTreeContentProvider;
import cmtypechecker.views.provider.CMViewTreeViewLabelProvider;
import cmtypechecker.views.provider.TreeObject;


public class InspectionDialog extends TitleAreaDialog {

	private final IProject currentProject;
	private NewTypeCheckerVisitor typeCheckingVisitor;
	private Expression exp;
	
	private StyledText CMTypeDescriptionContentST;
	private StyledText CMTypeDerivationContentST;
	
	private Label CMtypeDetailLabel;
	private Text associatedExplicationText;
	private TableViewer typeAttributeViewer;
	private TableViewer approxAttributeViewer;
	
	private TreeViewer cmTypesTreeViewer;
	protected TreeObject cmtypeTreeSelectedObject;
	private TreeObject cmtypeRootTreeObject;
	
	private String inspectingCMType;
	private String derivationCMType;

	private Action clickActionOnTreeViewer;
	private CMType selectedCMType ;
	
	public InspectionDialog(Shell parentShell, IProject currentProject, NewTypeCheckerVisitor typeCheckingVisitor, Expression exp) {
		super(parentShell);
		this.currentProject = currentProject;
		this.typeCheckingVisitor = typeCheckingVisitor; 
		this.exp = exp;
	}

	@Override
	public void create() {
		super.create();
		setTitle("select an operation");
		setMessage("select an operation in this dialog", IMessageProvider.INFORMATION);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 6;
		layout.verticalSpacing = 12;
		Font titleOneFont = new Font(parent.getDisplay(),"courier new", 9 , SWT.BOLD );
		Font titleTwoFont = new Font(parent.getDisplay(),"arial", 10 , SWT.BOLD );
		
		/**starting a row here**/
		Label CMTypeDescriptionLabel = new Label(container, SWT.NULL);
		CMTypeDescriptionLabel.setText("CM Type");
		CMTypeDescriptionLabel.setFont(titleOneFont);
		
		CMTypeDescriptionContentST = new StyledText(container, SWT.NULL);
		CMTypeDescriptionContentST.setText("");
		CMTypeDescriptionContentST.setFont(titleOneFont);
		GridData CMTypeDescriptionContentLabelgd = new GridData();
		CMTypeDescriptionContentLabelgd.horizontalAlignment = GridData.FILL;
		CMTypeDescriptionContentLabelgd.grabExcessHorizontalSpace = true;
		CMTypeDescriptionContentLabelgd = new GridData(GridData.FILL_HORIZONTAL);
		CMTypeDescriptionContentLabelgd.horizontalSpan = 5;
		CMTypeDescriptionContentST.setLayoutData(CMTypeDescriptionContentLabelgd);
		/**ending a row here**/
		
		/**starting a row here**/
		Label CMTypeDerivationLabel = new Label(container, SWT.NULL);
		CMTypeDerivationLabel.setText("Derivation");
		CMTypeDerivationLabel.setFont(titleOneFont);
		
		CMTypeDerivationContentST = new StyledText(container, SWT.NULL);
		CMTypeDerivationContentST.setText("");
		CMTypeDerivationContentST.setFont(titleOneFont);
		GridData CMTypeDerivationContentLabelgd = new GridData();
		CMTypeDerivationContentLabelgd.horizontalAlignment = GridData.FILL;
		CMTypeDerivationContentLabelgd.grabExcessHorizontalSpace = true;
		CMTypeDerivationContentLabelgd = new GridData(GridData.FILL_HORIZONTAL);
		CMTypeDerivationContentLabelgd.horizontalSpan = 5;
		CMTypeDerivationContentST.setLayoutData(CMTypeDerivationContentLabelgd);
		/**ending a row here**/
		
		//correspondence type information
		/**starting a row here**/
		Label cmtypesList = new Label(container, SWT.NULL);
		cmtypesList.setText("CM Types");
		cmtypesList.setFont(titleOneFont);
		GridData cmtypesListgd = new GridData(GridData.FILL_HORIZONTAL);
		cmtypesListgd.horizontalSpan = 1;
		cmtypesList.setLayoutData(cmtypesListgd);
		
		CMtypeDetailLabel = new Label(container, SWT.NULL);
		CMtypeDetailLabel.setText("Type Detail");
		CMtypeDetailLabel.setFont(titleOneFont);
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 5;
		CMtypeDetailLabel.setLayoutData(gd);
		/**ending a row here**/
		
		/**starting a row here**/
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

		/**ending a row here**/
		
		/***************************/
		/**starting a row here**/
		//Concept explication associated 
		Label associatedExplicationLabel = new Label(container, SWT.NONE);
		associatedExplicationLabel.setText("The concept explication for this CM type: ");
		GridData associatedExplicationLabelgd = new GridData();
		associatedExplicationLabelgd.verticalSpan = 1;
		associatedExplicationLabelgd.horizontalSpan = 5;
		associatedExplicationLabelgd.widthHint = 400;
		associatedExplicationLabel.setLayoutData(associatedExplicationLabelgd);
		/**ending a row here**/
		
		/**starting a row here**/
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
		/**ending a row here**/
		
		/**starting a row here**/
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
		/**ending a row here**/
		
		/**starting a row here**/
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
        /**ending a row here**/
        
//		cmTypesTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
//			public void doubleClick(DoubleClickEvent event) {
//				clickActionOnTreeViewer.run();
//			}
//		});
		
		loadAllContents(this.currentProject);
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
	
	private void loadAllContents(final IProject currentProject) {
		
		clickActionOnTreeViewer = new Action(){
			public void run() {
				ISelection selection = cmTypesTreeViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if(obj != null){
					TreeObject thisSelectedTreeObject = (TreeObject)obj;
					selectedCMType = CMModelUtil.getCMTypeFromTreeObject(currentProject, thisSelectedTreeObject);
					showCMTypeContents(selectedCMType);
				}
			}
		};
		
		String annotatedType = this.typeCheckingVisitor.getAnnotatedTypeForExpression(exp);
		this.CMTypeDescriptionContentST.setText(annotatedType);
		cmtypeRootTreeObject = CMModelUtil.readInAllCMTypesToTreeObject(this.currentProject);
		cmTypesTreeViewer.setInput(cmtypeRootTreeObject);
		
		MouseListener myMouseListener = new MouseListener() {
			@Override
			public void mouseUp(MouseEvent arg0) {
			}
			@Override
			public void mouseDown(MouseEvent mouseEvent) {
				String selectedCMType = "";
				if(mouseEvent.getSource().equals(CMTypeDescriptionContentST)){
					selectedCMType = CMTypeDescriptionContentST.getText();
				}if(mouseEvent.getSource().equals(CMTypeDerivationContentST)){
					int selectedPos = CMTypeDerivationContentST.getSelection().x;
					if(CMTypeDerivationContentST.getText().length()>0){
						selectedCMType = getSelectedCMType(CMTypeDerivationContentST.getText(), selectedPos);	
					}
				}
				if(selectedCMType.length()>0){
					CMType cmtype = CMModelUtil.getCMTypeFromTypeName(currentProject, selectedCMType);
					showCMTypeContents(cmtype);	
				}
			}
			@Override
			public void mouseDoubleClick(MouseEvent mouseEvent) {
				String selectedCMType = "";
				if(mouseEvent.getSource().equals(CMTypeDescriptionContentST)){
					selectedCMType = CMTypeDescriptionContentST.getText();
				}if(mouseEvent.getSource().equals(CMTypeDerivationContentST)){
					int selectedPos = CMTypeDerivationContentST.getSelection().x;
					if(CMTypeDerivationContentST.getText().length()>0){
						selectedCMType = getSelectedCMType(CMTypeDerivationContentST.getText(), selectedPos);	
					}
				}
				if(selectedCMType.length()>0){
					CMType cmtype = CMModelUtil.getCMTypeFromTypeName(currentProject, selectedCMType);
					showCMTypeContents(cmtype);	
				}
			}
		};
		this.CMTypeDescriptionContentST.addMouseListener(myMouseListener);
		this.CMTypeDerivationContentST.addMouseListener(myMouseListener);
	}
	
	private void showCMTypeContents(CMType selectedCMType){
		if(selectedCMType!=null){
			CMtypeDetailLabel.setText("Type Detail: "+selectedCMType.getTypeName());
			CM_SemanticType thisType = selectedCMType.getSemanticType();
			ConceptDetail explication = ConceptDetail.readInByLink(thisType.getExplicationLink());
			associatedExplicationText.setText(explication.getConceptName());
			typeAttributeViewer.setInput(selectedCMType.getSemanticType());
			approxAttributeViewer.setInput(selectedCMType.getApproximationType());
			setHighlightedTreeObject(selectedCMType.getTypeName());	
		}
	}
	
	private String getSelectedCMType(String orignialString, int selectedPos){
		if(orignialString.indexOf("*") == -1){
			return orignialString;
		}else{
			String[] cmtypes = orignialString.split("\\*");
			int positionInString = 0;
			for(String cmtype: cmtypes){
				positionInString += cmtype.length();
				if(positionInString>selectedPos){
					if(cmtype.indexOf("^")==-1){
						return cmtype;
					}else{
						return cmtype.split("\\^")[0];
					}
				}
			}
			return cmtypes[cmtypes.length-1];
		}
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	@Override
	protected void okPressed() {
		super.okPressed();
	}

	public String getInspectingCMType() {
		return inspectingCMType;
	}

	public void setInspectingCMType(String inspectingCMType) {
		this.inspectingCMType = inspectingCMType;
	}

	public String getDerivationCMType() {
		return derivationCMType;
	}

	public void setDerivationCMType(String derivationCMType) {
		this.derivationCMType = derivationCMType;
	}
	
	private void setHighlightedTreeObject(String selectedCMTypeName){
		if(this.cmtypeRootTreeObject==null){
			if(this.cmTypesTreeViewer.getInput()!=null){
				cmtypeRootTreeObject = (TreeObject)(this.cmTypesTreeViewer.getInput());	
			}else{
				return;
			}
		}
		if(selectedCMTypeName.length()>0){
			TreeObject[] topLevelTOs = this.cmtypeRootTreeObject.getChildren();
			for(TreeObject topLevelTo: topLevelTOs){
				TreeObject toBeSelectedTO = searchTOByName(topLevelTo, selectedCMTypeName);
				if(toBeSelectedTO!=null){
					StructuredSelection toSelectedSelection = new StructuredSelection(toBeSelectedTO);
					this.cmTypesTreeViewer.setSelection(toSelectedSelection);
					break;
				}
			}
		}else{
			StructuredSelection toSelectedSelection = new StructuredSelection(cmtypeRootTreeObject);
			this.cmTypesTreeViewer.setSelection(toSelectedSelection);
		}
	}
	
	private TreeObject searchTOByName(TreeObject treeObject, String typeName){
		TreeObject[] childRenTOs = treeObject.getChildren();
		for(TreeObject childTO: childRenTOs){
			if(childTO.getName().equals(typeName)){
				return childTO;
			}else{
				TreeObject temp = searchTOByName(childTO, typeName);
				if(temp == null){
					continue;
				}
			}
		}
		return null;
	}
}
