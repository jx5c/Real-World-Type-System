package cmtypechecker.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
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
import cmtypechecker.views.provider.CMViewTreeContentProvider;
import cmtypechecker.views.provider.CMViewTreeViewLabelProvider;
import cmtypechecker.views.provider.TreeObject;


public class AttToBeCheckedSelectionDialog extends TitleAreaDialog {
	
	public static String attributeCheckingStatusNone = "none"; 
	public static String attributeCheckingStatusLocal = "local";
	public static String attributeCheckingStatusGlobal = "global"; 
	
	private Map<String, String> localCheckingMap = new HashMap<String, String>();
	private Map<String, String> globalCheckingMap = new HashMap<String, String>();
	
	private IProject currentProject;
	
	private Label CMtypeDetailLabel;
	private CMType selectedNewCMType;
	private Text associatedExplicationText;
	private TableViewer typeAttributeViewer;
	
	private TreeViewer cmTypesTreeViewer;
	private Action clickActionOnTreeViewer;
	
	protected TreeObject cmtypeTreeSelectedObject;

	private ArrayList<String> baseTypes = new ArrayList<String>();
	
	public AttToBeCheckedSelectionDialog(Shell parentShell, IProject currentProject) {
		super(parentShell);
		this.currentProject = currentProject;
		baseTypes = CMModelUtil.getBaseTypes(this.currentProject);
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
		layout.verticalSpacing = 6;
		
		Font titleOneFont = new Font(parent.getDisplay(),"courier new", 9 , SWT.BOLD );
		
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
		cmtypeListGD.verticalSpan = 5;
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
					
					saveUnitsToBeChecked();
					
					cmtypeTreeSelectedObject = (TreeObject)obj;
					selectedNewCMType = CMModelUtil.getCMTypeFromTreeObject(currentProject, cmtypeTreeSelectedObject);
					if(selectedNewCMType != null){
						CMtypeDetailLabel.setText("Type Detail: "+selectedNewCMType.getTypeName());
						ConceptDetail explication = ConceptDetail.readInByLink(selectedNewCMType.getSemanticType().getExplicationLink());
						associatedExplicationText.setText(explication.getConceptName());
						typeAttributeViewer.setInput(selectedNewCMType);
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
		typeAttributeViewer.setContentProvider(new CheckingSemanticTypeAttributeContentProvider());
		typeAttributeViewer.setLabelProvider(new CheckingSemanticTypeAttributeLabelProvider());
		typeAttributeViewer.setInput(null);
		/**ending a row here**/
		
		LoadBaseTypes(this.currentProject);
		return container;
	}
	
	private void createAttributeTableColumns(final TableViewer viewer) {
		Table table = viewer.getTable();
		String[] titles = { "Attribute", "Type", "Checking Range"};
		int[] bounds = { 100, 100, 150};
		for (int i = 0; i < titles.length; i++) {
			final TableViewerColumn viewerColumn = new TableViewerColumn(
					viewer, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL
					| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
			final TableColumn column = viewerColumn.getColumn();
			column.setText(titles[i]);
			column.setWidth(bounds[i]);
			column.setResizable(true);
			column.setMoveable(true);
			if(i==2){
				viewerColumn.setEditingSupport(new CheckingRangeEditingSupport(viewer));
			}
		}
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}
	
	private void LoadBaseTypes(IProject currentProject) {
		
		/**reserved for future usage**/
//		TreeObject baseTypesTO = CMModelUtil.readInBaseTypesToTreeObject(currentProject);
//		this.cmTypesTreeViewer.setInput(baseTypesTO);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	@Override
	protected void okPressed() {
		
		saveUnitsToBeChecked();
		super.okPressed();
	}
	
	private void saveUnitsToBeChecked(){
		if(selectedNewCMType!=null){
			TableItem[] tableItems = typeAttributeViewer.getTable().getItems();
			String localAttToBeChecked = "";
			String globalAttToBeChecked = "";
			for(TableItem tableItem : tableItems){
				CheckingSemanticTypeAttribute att = (CheckingSemanticTypeAttribute)(tableItem.getData());
				if(att.getCheckingRange().equals(attributeCheckingStatusLocal)){
					if(localAttToBeChecked.length() == 0){
						localAttToBeChecked = att.getAttName();
					}else{
						localAttToBeChecked += "|" + att.getAttName();
					}
				}
				else if(att.getCheckingRange().equals(attributeCheckingStatusGlobal)){
					if(globalAttToBeChecked.length() == 0){
						globalAttToBeChecked = att.getAttName();
					}else{
						globalAttToBeChecked += "|" + att.getAttName();
					}
				}
			}
			if(localAttToBeChecked.length()>0){
				localCheckingMap.put(selectedNewCMType.getTypeName(), localAttToBeChecked);
			}
			if(globalAttToBeChecked.length()>0){
				globalCheckingMap.put(selectedNewCMType.getTypeName(), globalAttToBeChecked);
			}
		}
	}
	
	public Map<String, String> getLocalCheckingMap() {
		return localCheckingMap;
	}

	public Map<String, String> getGlobalCheckingMap() {
		return globalCheckingMap;
	}

	public void setGlobalCheckingMap(Map<String, String> globalCheckingMap) {
		this.globalCheckingMap = globalCheckingMap;
	}

	public void setLocalCheckingMap(Map<String, String> localCheckingMap) {
		this.localCheckingMap = localCheckingMap;
	}

	private class CheckingSemanticTypeAttribute{
		private String attName = "";
		private String attValue = "";
		private String checkingRange = "none";
		
		public CheckingSemanticTypeAttribute(CMAttribute semanticTypeAtt){
			 this.attName = semanticTypeAtt.getAttributeName();
			 this.attValue = semanticTypeAtt.getAttributeValue();
		}

		public String getAttName() {
			return attName;
		}

		public String getAttValue() {
			return attValue;
		}

		public String getCheckingRange() {
			return checkingRange;
		}

		public void setCheckingRange(String checkingRange) {
			this.checkingRange = checkingRange;
		}
	}
	
	private class CheckingSemanticTypeAttributeLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			CheckingSemanticTypeAttribute checkingAttribute = (CheckingSemanticTypeAttribute)obj;
			switch (index) {
				case 0:
					return checkingAttribute.getAttName();
				case 1:
					return checkingAttribute.getAttValue();
				case 2:
					return checkingAttribute.getCheckingRange();
				default:
					throw new RuntimeException("Should not happen");
			}
		}
		
		public Image getImage(Object obj) {
			return null;
		}

		@Override
		public Image getColumnImage(Object arg0, int arg1) {
			return null;
		}
	}
	
	private class CheckingSemanticTypeAttributeContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			CMType cmtype = (CMType) parent;
			ArrayList<CheckingSemanticTypeAttribute> atts = new ArrayList<CheckingSemanticTypeAttribute>();
			for(CMAttribute att : cmtype.getSemanticType().getSemanticTypeAttributes()){
				CheckingSemanticTypeAttribute newAtt = new CheckingSemanticTypeAttribute(att);
				atts.add(newAtt);
			}
			return atts.toArray();
		}
	}
	
	private class CheckingRangeEditingSupport extends EditingSupport {
		private final String[] checkingRanges = new String[] {attributeCheckingStatusNone, attributeCheckingStatusLocal, attributeCheckingStatusGlobal};  
		private TableViewer tableViewer;
		private ComboBoxCellEditor combobox_editor;  
		
		public CheckingRangeEditingSupport(TableViewer viewer) {
	        super(viewer);
	        this.tableViewer = viewer;
	        combobox_editor = new ComboBoxCellEditor(this.tableViewer.getTable(), new String[0]);  
	    }

	    @Override
	    protected CellEditor getCellEditor(Object o) {
	    	combobox_editor.setItems(checkingRanges);  
	        return combobox_editor;
	    }

	    @Override
	    protected boolean canEdit(Object o) {
	        return true;
	    }
	    @Override
	    protected Object getValue(Object element) {
	    	String value = ((CheckingSemanticTypeAttribute)element).getCheckingRange();
	    	if(value.equals("none")){
	    		return 0;
	    	}else if(value.equals("local")){
	    		return 1;
	    	}
	    	return 2;
	    }

	    @Override
	    protected void setValue(Object element, Object value) {
	    	CheckingSemanticTypeAttribute selectedAtt = ((CheckingSemanticTypeAttribute)element);
	    	if (((Integer) value) == 0) {
	    		selectedAtt.setCheckingRange("none");
	    	}
	    	else if (((Integer) value) == 1) {
				selectedAtt.setCheckingRange("local");
			} else {
				selectedAtt.setCheckingRange("global");
			}
			tableViewer.update(selectedAtt, null);
	    } 
	}
}
