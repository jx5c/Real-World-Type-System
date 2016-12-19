package rwtchecker.dialogs;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import rwtchecker.concept.ConceptDetail;
import rwtchecker.rwt.RWT_Attribute;
import rwtchecker.rwt.RWType;
import rwtchecker.util.ActivePart;
import rwtchecker.util.RWTSystemUtil;
import rwtchecker.views.provider.CMAttributeTableContentProvider;
import rwtchecker.views.provider.CMAttributeTablelLabelProvider;
import rwtchecker.views.provider.CMViewTreeContentProvider;
import rwtchecker.views.provider.CMViewTreeViewLabelProvider;
import rwtchecker.views.provider.TreeObject;

public class ArrayTypeBindingDialog extends TitleAreaDialog {

	private String rwtLocation ; 
	private String varName ;
	
	private TreeViewer existingTypesTreeViewer;
	private TableViewer typeAttributeViewer;
	private Action clickActionOnTreeViewer;
	private IProject currentProject;
	
	
	private Table currentBindingTable;
	private Combo dimensionCombo;
	private Text bindingIdxText;
	private Text rwtypeSelectedText;
	
	private CMViewTreeContentProvider treeContentProvider = new CMViewTreeContentProvider();
	
	private Set<String> currentBindings = new HashSet<String>();
	
	Font titleFont;
	Font tableItemFont;
	
	public ArrayTypeBindingDialog(Shell parentShell, String variableName) {
		super(parentShell);
		varName = variableName;
	}

	@Override
	public void create() {
		super.create();
		setTitle("For arrays, real-world types are bound to indices of one choosen dimension.");
		setMessage("Choose the dimension and index for ", IMessageProvider.INFORMATION);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		titleFont = new Font(parent.getDisplay(),"Arial", 10 , SWT.BOLD );
		
		tableItemFont = new Font(parent.getDisplay(),"Arial", 9 , SWT.ITALIC );
		
		int leftWidth = 350;
	    Group leftGroup = new Group(parent, SWT.NULL);
	    leftGroup.setLocation(30, 10);
	    leftGroup.setSize(leftWidth*2+100, 700);
	    
	    //show the variable
	    Label varLabel = new Label(leftGroup, SWT.NULL);
	    varLabel.setBounds(20, 20, leftWidth, 20);
	    Font varNameFont = new Font(parent.getDisplay(),"Arial", 10 , SWT.BOLD );
	    varLabel.setText("Name of the array is: "+this.varName);
	    varLabel.setFont(varNameFont);
	    
		//for adding bindings
		Label chooseDimensionLabel = new Label(leftGroup, SWT.NULL);
		chooseDimensionLabel.setBounds(20, 50, leftWidth, 20);
		chooseDimensionLabel.setText("1. Choose the dimension of this array: ");
		
	    final String[] ITEMS = { "1st Dimension", "2nd Dimension", "3rd Dimension" };
	    dimensionCombo = new Combo(leftGroup, SWT.DROP_DOWN);
	    dimensionCombo.setBounds(20, 75, 200, 20);
	    dimensionCombo.setItems(ITEMS);
	    
        //for adding bindings
	  	Label chooseIndexLabel = new Label(leftGroup, SWT.NULL);
	  	chooseIndexLabel.setText("2. Specify the index for a binding; starting with 0");
	  	chooseIndexLabel.setBounds(20, 110, leftWidth, 20);
	  	
	  	bindingIdxText = new Text(leftGroup, SWT.SINGLE | SWT.BORDER | SWT.WRAP);
	  	bindingIdxText.setBounds(20, 135, 200, 25);
	  	
	  	//current selected rwt
	  	Label rwtypeSelected = new Label(leftGroup, SWT.NULL);
	  	rwtypeSelected.setText("3. Select the real-world type from the tree");
	  	rwtypeSelected.setBounds(20, 165, leftWidth, 20);
	  	
	  	rwtypeSelectedText = new Text(leftGroup, SWT.SINGLE | SWT.BORDER | SWT.WRAP);
	  	rwtypeSelectedText.setBounds(20, 190, leftWidth, 25);
	  	
	  	
	  	//confirm button
	  	Button confirmBT = new Button(leftGroup, SWT.PUSH );
	  	confirmBT.setText("Create Binding");
	  	confirmBT.setBounds(180, 220, 120, 25);
	  	confirmBT.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				createBindingListItem();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				createBindingListItem();
			}
		});
	  	//reset button
	  	Button resetBT = new Button(leftGroup, SWT.PUSH );
	  	resetBT.setText("reset");
	  	resetBT.setBounds(310, 220, 60, 25);
	  	Font btFont = new Font(parent.getDisplay(),"Arial", 10 , SWT.ITALIC );
	  	confirmBT.setFont(btFont);
	  	resetBT.setFont(btFont);
	  	resetBT.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				resetEntries();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				resetEntries();
			}
		});
	  	
	  	
		//display the current binding
	  	Label existingBindingLabel = new Label(leftGroup, SWT.NULL);
	  	existingBindingLabel.setText("Existing bindings for this array are listed below: ");
	  	existingBindingLabel.setBounds(20, 250, leftWidth, 20);
	  	
	  	currentBindingTable = new Table(leftGroup, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
	    currentBindingTable.setBounds(20, 280, leftWidth, 150);
	    currentBindingTable.setHeaderVisible(true);
	    currentBindingTable.setLinesVisible(true);

	    TableColumn tc1 = new TableColumn(currentBindingTable, SWT.LEFT);
	    TableColumn tc2 = new TableColumn(currentBindingTable, SWT.CENTER);
	    TableColumn tc3 = new TableColumn(currentBindingTable, SWT.CENTER);
	    tc1.setText("Dimension");
	    tc2.setText("Index");
	    tc3.setText("Real-world type");
	    tc1.setWidth(90);
	    tc2.setWidth(70);
	    tc3.setWidth(180);		
		
	    Group rightGroup = new Group(leftGroup, SWT.NULL);
	    rightGroup.setLocation(400, 10);
	    rightGroup.setSize(420, 420);	
	    //right panel
	    SashForm sashFormRight = new SashForm(rightGroup, SWT.VERTICAL | SWT.NULL);
	    sashFormRight.setBounds(0, 0, 400, 420);
		//for real-world type display
		SashForm sashFormRightTop = new SashForm(sashFormRight, SWT.HORIZONTAL | SWT.NULL);
		//for display of details of real-world types
		SashForm sashFormRightBottom = new SashForm(sashFormRight, SWT.HORIZONTAL | SWT.NULL);
		
		existingTypesTreeViewer = new TreeViewer(sashFormRightTop, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		existingTypesTreeViewer.getTree().setBounds(10, 10, 300, 350);
		existingTypesTreeViewer.setContentProvider(treeContentProvider);
		existingTypesTreeViewer.setLabelProvider(new CMViewTreeViewLabelProvider());
		//show the contents of current rwtypes
		IFile currentFile =  ActivePart.getFileOfActiveEditror();
		if( currentFile != null){
			this.currentProject = currentFile.getProject();
			TreeObject cmtypeTreeRootObject = RWTSystemUtil.readInAllCMTypesToTreeObject(currentFile);
			if(cmtypeTreeRootObject!= null){
				existingTypesTreeViewer.setInput(cmtypeTreeRootObject);
			}
		}
		existingTypesTreeViewer.expandToLevel(4);
		
		//real-world type display here;
		typeAttributeViewer = new TableViewer(sashFormRightBottom, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        createAttributeTableColumns(typeAttributeViewer);
        hookActionsToTypeAttributeTable();
		typeAttributeViewer.setContentProvider(new CMAttributeTableContentProvider());
		typeAttributeViewer.setLabelProvider(new CMAttributeTablelLabelProvider());
		typeAttributeViewer.setInput(null);
		
        // Set the size of the split pane
		sashFormRight.setWeights(new int [] {40, 60});
		hookClickAction();
		parent.layout();
	    return parent;
	}
	
	private void resetEntries(){
		dimensionCombo.setText("");
		bindingIdxText.setText("");
		rwtypeSelectedText.setText("");
	}
	
	private void createBindingListItem(){
		String dimension = dimensionCombo.getText();
		String bindingIdx = bindingIdxText.getText();
		String rwtype = rwtypeSelectedText.getText();
		if(dimension.length()==0 || bindingIdx.length()==0 || rwtype.length() == 0){
			return;
		}
		dimension = dimension.substring(0,1);
		String bindingInStr = makeItemStr(dimension, bindingIdx, rwtype);
		if(!currentBindings.contains(bindingInStr)){
			currentBindings.add(bindingInStr);
			TableItem newItem = new TableItem(currentBindingTable,SWT.NONE);
			newItem.setText(new String[] {dimension, bindingIdx, rwtype});
			newItem.setFont(tableItemFont);	
		}
		resetEntries();
	}
	
	private void hookClickAction() {
		clickActionOnTreeViewer = new Action(){
			public void run() {
				ISelection selection = existingTypesTreeViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if(obj != null){
					TreeObject thisSelectedTreeObject = (TreeObject)obj;
					RWType selectedType = RWTSystemUtil.getCMTypeFromTreeObject(currentProject, thisSelectedTreeObject);
					if(selectedType!=null){
						typeAttributeViewer.setInput(selectedType.getSemanticType());
						rwtypeSelectedText.setText(selectedType.getTypeName());
					}
				}
			}
		};
		
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
	
	private void createAttributeTableColumns(final TableViewer viewer) {
		Table table = viewer.getTable();
		String[] titles = { "Attribute", "Type"};
		int[] bounds = { 100, 150};
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
	
	private void hookActionsToTypeAttributeTable() {	    
	    Menu menu = new Menu (currentBindingTable.getShell(), SWT.POP_UP);
	    currentBindingTable.setMenu (menu);
		MenuItem removeAttItem = new MenuItem (menu, SWT.PUSH);
		removeAttItem.setText ("Delete Selection");
		removeAttItem.addListener (SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				for(int i : currentBindingTable.getSelectionIndices()){
					TableItem selectedItem = currentBindingTable.getItem(i);
					String itemToDelete = makeItemStr(selectedItem.getText(0), selectedItem.getText(1),selectedItem.getText(2));
					currentBindings.remove(itemToDelete);
				}
				currentBindingTable.remove(currentBindingTable.getSelectionIndices());
			}
		});
	}

	private String makeItemStr(String dimension, String index, String rwtype){
		return dimension + "#" + index + "#" + rwtype;
	}

	@Override
	protected boolean isResizable() {
		return false;
	}
	
	@Override
	protected void okPressed() {
	    this.setReturnCode(OK);
	    super.okPressed();		
	}

	public Set<String> getCurrentBindings() {
		return currentBindings;
	}
}
