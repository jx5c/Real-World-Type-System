package rwtchecker.dialogs;

import java.io.File;

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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import rwtchecker.concept.ConceptDetail;
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
	
	private CMViewTreeContentProvider treeContentProvider = new CMViewTreeContentProvider();
	
	Font titleFont;
	
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
//		shlScrambledata.setSize(450, 405);
//	    shlScrambledata.setText("ScrambleData");
		// first row used for displaying the variable information
		
		int leftWidth = 350;
	    Group leftGroup = new Group(parent, SWT.NULL);
	    leftGroup.setLocation(30, 10);
	    leftGroup.setSize(leftWidth, 49);
	    
	    //show the variable
	    Label varLabel = new Label(leftGroup, SWT.BORDER);
	    varLabel.setBounds(20, 20, 60, 20);
	    varLabel.setText("1. Variable: ");
	    
	    Label varNameLabel = new Label(leftGroup, SWT.BORDER);
	    varNameLabel.setBounds(90, 20, 210, 20);
	    varNameLabel.setText(this.varName);

		//for adding bindings
		Label chooseDimensionLabel = new Label(leftGroup, SWT.NULL);
		chooseDimensionLabel.setBounds(20, 50, leftWidth, 20);
		chooseDimensionLabel.setText("2. Choose the dimension of this array: ");
		
	    final String[] ITEMS = { "Dimension One", "Dimension Two", "Dimension Three" };
	    Combo combo = new Combo(leftGroup, SWT.DROP_DOWN);
	    combo.setBounds(20, 75, 200, 20);
	    combo.setItems(ITEMS);
	    
        //for adding bindings
	  	Label chooseIndexLabel = new Label(leftGroup, SWT.NULL);
	  	chooseIndexLabel.setText("3. Specify the index for a binding");
	  	chooseIndexLabel.setBounds(20, 100, leftWidth, 20);
	  	
	  	Text bindingIdxText = new Text(leftGroup, SWT.SINGLE | SWT.BORDER | SWT.WRAP);
	  	bindingIdxText.setBounds(20, 125, 200, 20);
	  	
	  	//current selected rwt
	  	Label rwtypeSelected = new Label(leftGroup, SWT.NULL);
	  	rwtypeSelected.setText("4. Select the real-world type from the tree for this binding");
	  	rwtypeSelected.setBounds(20, 150, leftWidth, 20);
	  	
	  	Text rwtypeSelectedText = new Text(leftGroup, SWT.SINGLE | SWT.BORDER | SWT.WRAP);
	  	rwtypeSelectedText.setBounds(20, 175, leftWidth, 20);
	  	
		//display the current binding
	  	Label existingBindingLabel = new Label(leftGroup, SWT.NULL);
	  	existingBindingLabel.setText("5. Current bindings for this array are listed below: ");
	  	existingBindingLabel.setBounds(20, 200, leftWidth, 20);
	  	
		Text currentBindings = new Text(leftGroup, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		currentBindings.setBounds(20, 225, leftWidth, 150);
		
	    Group rightGroup = new Group(leftGroup, SWT.NULL);
	    rightGroup.setLocation(400, 10);
	    rightGroup.setSize(leftWidth, 350);	
	    //right panel
	    SashForm sashFormRight = new SashForm(rightGroup, SWT.VERTICAL | SWT.NULL);
	    sashFormRight.setBounds(0, 0, 400, 500);
		//for real-world type display
		SashForm sashFormRightTop = new SashForm(sashFormRight, SWT.HORIZONTAL | SWT.NULL);
		//for display of details of real-world types
		SashForm sashFormRightBottom = new SashForm(sashFormRight, SWT.HORIZONTAL | SWT.NULL);
		
		existingTypesTreeViewer = new TreeViewer(sashFormRightTop, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		existingTypesTreeViewer.getTree().setBounds(10, 10, 350, 350);
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
		existingTypesTreeViewer.setAutoExpandLevel(4);
		
		//real-world type display here;
		typeAttributeViewer = new TableViewer(sashFormRightBottom, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        createAttributeTableColumns(typeAttributeViewer);
		typeAttributeViewer.setContentProvider(new CMAttributeTableContentProvider());
		typeAttributeViewer.setLabelProvider(new CMAttributeTablelLabelProvider());
		typeAttributeViewer.setInput(null);
		
		
        // Set the size of the split pane
		sashFormRight.setWeights(new int [] {40, 60});
		hookClickAction();
		parent.layout();
	    return parent;
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

	@Override
	protected boolean isResizable() {
		return false;
	}
	
	@Override
	protected void okPressed() {
		if(true){	
	    	this.setReturnCode(OK);
	    	super.okPressed();
		}else{
			this.setReturnCode(CANCEL);	
			super.cancelPressed();
		}
		
	}

	public String getRwtLocation() {
		return rwtLocation;
	}
}
