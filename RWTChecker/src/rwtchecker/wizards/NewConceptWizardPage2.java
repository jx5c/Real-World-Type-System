package rwtchecker.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import rwtchecker.dialogs.NewAttributeDetailDialog;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (att).
 */

public class NewConceptWizardPage2 extends WizardPage {
	
	public static final String PAGE_NAME = "NewConceptWizardPage2"; 
	
	private Table attributeTable;
	
	private Text conceptName;

	private Text definitionText;
	

	public NewConceptWizardPage2() {
		super(PAGE_NAME);
		setTitle("Concept Details");
		setDescription("This wizard creates a new file with *.att extension.");
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);
		Font titleFont = new Font(parent.getDisplay(),"courier new", 9 , SWT.ITALIC );
		//first row
		Label conceptLabel = new Label(composite, SWT.NONE);
		conceptLabel.setText("Concept:");
		GridData conceptLabelGridData = new GridData();
		conceptLabel.setLayoutData(conceptLabelGridData);
		
		conceptName = new Text(composite, SWT.BORDER );
		GridData conceptNameGridData = new GridData();
		conceptNameGridData.horizontalAlignment = GridData.FILL;
		conceptNameGridData.grabExcessHorizontalSpace = true;
		conceptName.setLayoutData(conceptNameGridData);
		conceptName.setEditable(false);
		
		//second row
		Label definitionLabel = new Label(composite, SWT.NONE);
		definitionLabel.setText("Definition:");
		GridData definitionLabelGridData = new GridData(GridData.FILL_VERTICAL);
		definitionLabel.setLayoutData(definitionLabelGridData);
		
		definitionText = new Text(composite, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		GridData definitionTextGridData = new GridData();
		definitionTextGridData.verticalAlignment = GridData.FILL;
		definitionTextGridData.horizontalAlignment = GridData.FILL;
		definitionTextGridData.grabExcessHorizontalSpace = true;
		definitionTextGridData.grabExcessVerticalSpace = true;
		definitionTextGridData.heightHint = 150;
		definitionText.setLayoutData(definitionTextGridData);
		
		//Third Row
		final Label attributeLabel = new Label(composite, SWT.NONE);
		attributeLabel.setText("Accessible Attribute:");
		Button addAttributeButton = new Button(composite, SWT.PUSH);
		addAttributeButton.setLayoutData(new GridData(SWT.RIGHT));
		addAttributeButton.setText("add");
				
		addAttributeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				IWorkbench workbench = PlatformUI.getWorkbench();
        		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
				NewAttributeDetailDialog dialog = new NewAttributeDetailDialog(window.getShell());
				dialog.setAttributeTable(attributeTable);
				dialog.setActionType(NewAttributeDetailDialog.CreateFromWizard);
				dialog.create();
				dialog.open();
			}
		});
		
		attributeTable = new Table(composite, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL
		        | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
		attributeTable.setLinesVisible(true);
		attributeTable.setHeaderVisible(true);
	    GridData gridData = new GridData(GridData.FILL_BOTH);
	    gridData.horizontalSpan = 2;
	    gridData.heightHint = 150;
	    attributeTable.setLayoutData(gridData);
//		    table.addTraverseListener(traverseListener);
	    String[] columnHeaders = new String[]{"Accessible Attribute", "Definition"};
	    for (int i = 0; i < columnHeaders.length; i++) {
	      TableColumn column = new TableColumn(attributeTable, SWT.NONE);
	      column.setText(columnHeaders[i]);
	      if (i == 0)
	        column.setWidth(100);
	      else if (i == 1)
	        column.setWidth(600);
	      else
	        column.pack();
	    }		   
//	    hookMenuToAttributeTable();
		setControl(composite);
	}
	
	private void hookMenuToAttributeTable() {
	    Menu menu = new Menu(attributeTable.getShell(), SWT.POP_UP);
	    attributeTable.setMenu(menu);
	    MenuItem item = new MenuItem(menu, SWT.PUSH);
	    item.setText("Delete This attribute");
	    item.addListener(SWT.Selection, new Listener() {
	      public void handleEvent(Event event) {
	    	  attributeTable.remove(attributeTable.getSelectionIndices());
	      }
	    });
	}
	
	public void updateConceptNameText (String newConceptName){
		this.conceptName.setText(newConceptName);
	}

	public String getConceptName(){
		return conceptName.getText();
	}
	
	public String getDefinition(){
		return definitionText.getText();
	}
	
	public Table getAttributeTable() {
		return attributeTable;
	}

}