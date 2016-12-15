package rwtchecker.dialogs;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import rwtchecker.util.ActivePart;
import rwtchecker.util.RWTSystemUtil;
import rwtchecker.views.provider.CMAttributeTableContentProvider;
import rwtchecker.views.provider.CMAttributeTablelLabelProvider;
import rwtchecker.views.provider.CMViewTreeContentProvider;
import rwtchecker.views.provider.CMViewTreeViewLabelProvider;

public class ArrayTypeBindingDialog extends TitleAreaDialog {

	private String currentProject;
	private String rwtLocation ; 
	private String varName ;
	
	private TreeViewer existingTypesTreeViewer;
	private TableViewer typeAttributeViewer;
	
	private CMViewTreeContentProvider treeContentProvider = new CMViewTreeContentProvider();
	
	Font titleFont;
	
	public ArrayTypeBindingDialog(Shell parentShell, String currentProject, String variableName) {
		super(parentShell);
		this.currentProject = currentProject;
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
		final Composite shlScrambledata = parent;
//		shlScrambledata.setSize(450, 405);
//	    shlScrambledata.setText("ScrambleData");
		// first row used for displaying the variable information
	    Group docTypeGroup = new Group(shlScrambledata, SWT.NULL);
	    docTypeGroup.setLocation(10, 10);
	    docTypeGroup.setSize(400, 49);

	    Label projectLabel = new Label(docTypeGroup, SWT.NULL);
	    projectLabel.setBounds(10, 20, 80, 20);
	    projectLabel.setText("Variable: ");
	    
	    Label projectNameLabel = new Label(docTypeGroup, SWT.BORDER);
	    projectNameLabel.setBounds(100, 20, 430, 20);
	    projectNameLabel.setText(this.currentProject);
		
		
		//left panel for array binding information
		SashForm sashFormLeft = new SashForm(parent, SWT.VERTICAL | SWT.NULL);
		SashForm sashFormLeftTop = new SashForm(sashFormLeft, SWT.HORIZONTAL | SWT.NULL);
		SashForm sashFormLeftBottom = new SashForm(sashFormLeft, SWT.HORIZONTAL | SWT.NULL);
        
		//for adding bindings
		Label chooseDimensionLabel = new Label(sashFormLeftTop, SWT.NULL);
		chooseDimensionLabel.setText("Choose the dimension of this array: ");
		
		Text dimensionText = new Text(sashFormLeftTop, SWT.SINGLE);
		Button dimensionBT = new Button(sashFormLeftTop, SWT.NULL);
		dimensionBT.setText("OK");
		
		//display the current binding
		Text currentBindings = new Text(sashFormLeftBottom, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		currentBindings.setLayoutData(new GridData(GridData.FILL_BOTH));
		
        // Set the size of the split pane
		sashFormLeft.setWeights(new int [] {40, 60});
		
	        
	    //right panel
	    SashForm sashFormRight = new SashForm(parent, SWT.VERTICAL | SWT.NULL);
		//for real-world type display
		SashForm sashFormRightTop = new SashForm(sashFormRight, SWT.HORIZONTAL | SWT.NULL);

		existingTypesTreeViewer = new TreeViewer(sashFormRightTop, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		existingTypesTreeViewer.setContentProvider(treeContentProvider);
		existingTypesTreeViewer.setLabelProvider(new CMViewTreeViewLabelProvider());
		existingTypesTreeViewer.setInput(null);
		existingTypesTreeViewer.setAutoExpandLevel(4);
		
		//for display of details of real-world types
		SashForm sashFormRightBottom = new SashForm(sashFormRight, SWT.HORIZONTAL | SWT.NULL);
		//real-world type display here;
		typeAttributeViewer = new TableViewer(sashFormRightBottom, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        createAttributeTableColumns(typeAttributeViewer);
		typeAttributeViewer.setContentProvider(new CMAttributeTableContentProvider());
		typeAttributeViewer.setLabelProvider(new CMAttributeTablelLabelProvider());
		typeAttributeViewer.setInput(null);
        // Set the size of the split pane
		sashFormRight.setWeights(new int [] {40, 60});
		
	    shlScrambledata.layout();
	    return parent;
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
