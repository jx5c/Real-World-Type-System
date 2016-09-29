package rwtchecker.dialogs;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
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

import rwtchecker.CM.CMType;
import rwtchecker.CM.CM_SemanticType;
import rwtchecker.realworldmodel.ConceptDetail;
import rwtchecker.util.CMModelUtil;
import rwtchecker.views.provider.CMApproTableContentProvider;
import rwtchecker.views.provider.CMApproTablelLabelProvider;
import rwtchecker.views.provider.CMAttributeTableContentProvider;
import rwtchecker.views.provider.CMAttributeTablelLabelProvider;
import rwtchecker.views.provider.CMViewTreeContentProvider;
import rwtchecker.views.provider.CMViewTreeViewLabelProvider;
import rwtchecker.views.provider.TreeObject;


public class TypeDerivationDialog extends TitleAreaDialog {

	private Text derivationSpecificationText;
	private IProject currentProject;
	
	private String derivationSpecification;
	
	private Label CMtypeDetailLabel;
	private CMType selectedNewCMType;
	private Text associatedExplicationText;
	private TableViewer typeAttributeViewer;
	private TableViewer approxAttributeViewer;
	
	private TreeViewer cmTypesTreeViewer;
	private Action clickActionOnTreeViewer;
	protected TreeObject cmtypeTreeSelectedObject;

	private ArrayList<String> baseTypes = new ArrayList<String>();
	
	public TypeDerivationDialog(Shell parentShell, IProject currentProject) {
		super(parentShell);
		this.currentProject = currentProject;
		baseTypes = CMModelUtil.getBaseTypes(this.currentProject);
	}

	@Override
	public void create() {
		super.create();
		// Set the title
		setTitle("select an operation");
		// Set the message
		setMessage("select an operation in this dialog", IMessageProvider.INFORMATION);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		
		
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 6;
		layout.verticalSpacing = 11;
		Font titleOneFont = new Font(parent.getDisplay(),"courier new", 9 , SWT.BOLD );
		Font titleTwoFont = new Font(parent.getDisplay(),"arial", 10 , SWT.BOLD );
		
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
        /**starting a row here**/
		derivationSpecificationText = new Text(container, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		GridData derivationSpecificationTextgd = new GridData( GridData.FILL_BOTH);
		derivationSpecificationTextgd.grabExcessHorizontalSpace = true;
		derivationSpecificationTextgd.heightHint = 150;
		derivationSpecificationTextgd.grabExcessVerticalSpace = true;
		derivationSpecificationTextgd.horizontalSpan = 6;
		derivationSpecificationText.setLayoutData(derivationSpecificationTextgd);
		derivationSpecificationText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				String derivation = derivationSpecificationText.getText();
				if(derivationSpecificationText.getText().length() > 0){
					if(!derivation.contains(CMModelUtil.complexTypeSeparator)){
						String cmtypePart = "";
						if(derivation.indexOf("^")!=-1){
							cmtypePart = derivation.split("\\^")[0];
						}else{
							cmtypePart = derivation;
						}
						
						if(baseTypes.contains(cmtypePart)){
							setReturnCode(OK);
						}else{
							setReturnCode(CANCEL);
							setErrorMessage("please use the correct format of derivation");
						}
					}else{
						String[] specificationTypes = derivation.split("\\"+CMModelUtil.complexTypeSeparator);
						for(String specificationType: specificationTypes){
							String cmtypePart = "";
							if(specificationType.indexOf("^")!=-1){
								cmtypePart = specificationType.split("\\^")[0];
							}else{
								cmtypePart = specificationType;
							}
							
							if(baseTypes.contains(cmtypePart)){
								setReturnCode(OK);
							}else{
								setReturnCode(CANCEL);
								setErrorMessage("please use the correct format of derivation");
							}
							
							if(!baseTypes.contains(cmtypePart)){
								setReturnCode(CANCEL);
								setErrorMessage("please use the correct format of derivation");
							}
						}
					}
					
				}else{
					setReturnCode(CANCEL);
					setErrorMessage("please use the correct format of derivation");
				}
			}
		});
        /**ending a row here**/

		addDNDSupport();
		LoadCMTypes(this.currentProject);
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
	
	private void LoadCMTypes(IProject currentProject) {
		TreeObject baseTypesTO = CMModelUtil.readInAllCMTypesToTreeObject(currentProject);
		this.cmTypesTreeViewer.setInput(baseTypesTO);
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

	    DropTarget target = new DropTarget(this.derivationSpecificationText, operations);
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
	        derivationSpecificationText.setText(derivationSpecificationText.getText() + " "+text);
	      }
	     });
	}
	@Override
	protected boolean isResizable() {
		return true;
	}
	@Override
	protected void okPressed() {
		String derivation = derivationSpecificationText.getText();
		if(derivationSpecificationText.getText().length() > 0){
			if(!derivation.contains(CMModelUtil.complexTypeSeparator)){
				if(derivation.indexOf("^")!=-1){
					String cmtypePart = derivation.split("\\^")[0];
					if(baseTypes.contains(cmtypePart)){
						setReturnCode(OK); 
					}
				}
			}else{
				String[] specificationTypes = derivation.split("\\"+CMModelUtil.complexTypeSeparator);
				for(String specificationType: specificationTypes){
					if(!baseTypes.contains(specificationType)){
						setReturnCode(CANCEL);
					}
				}
			}
			derivationSpecification = derivation.trim();
		}
		super.okPressed();
	}
	
	public String getDerivationSpecification(){
		return derivationSpecification;
	}
}
