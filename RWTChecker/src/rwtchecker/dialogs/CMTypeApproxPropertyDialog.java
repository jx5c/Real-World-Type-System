package rwtchecker.dialogs;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import rwtchecker.CM.CM_ApproxType;
import rwtchecker.CM.CorrespondenceApproTypeProperty;

public class CMTypeApproxPropertyDialog extends TitleAreaDialog {

	private Text propertyNameText;
	private Text propertyExplanation;
	private Text propertyValueText;
	
	private CM_ApproxType correspondence_ApproxType;
	private TableViewer propertiesTableViewer;
	
	private CorrespondenceApproTypeProperty modifyAtt ;

	public CMTypeApproxPropertyDialog(Shell parentShell, CM_ApproxType approxType, TableViewer propertiesTableViewer) {
		super(parentShell);
		this.correspondence_ApproxType = approxType;
		this.propertiesTableViewer = propertiesTableViewer;
	}

	@Override
	public void create() {
		super.create();
		// Set the title
		setTitle("new Approximation Property");
		// Set the message
		setMessage("Input a new Approximation Property in this dialog", IMessageProvider.INFORMATION);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Font titleFont = new Font(parent.getDisplay(),"Arial", 10 , SWT.BOLD );
		
		GridLayout layout = new GridLayout(2, false);
		layout.numColumns = 2;
		// layout.horizontalAlignment = GridData.FILL;
		parent.setLayout(layout);

		// The text fields will grow with the size of the dialog
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
//		gridData.widthHint = 100;
		
		//first row
		Label label1 = new Label(parent, SWT.NONE);
		label1.setFont(titleFont);
		label1.setText("Property Name:");
		
		propertyNameText = new Text(parent, SWT.BORDER);
		propertyNameText.setLayoutData(gridData);
		
		//second row
		Label label2 = new Label(parent, SWT.NONE);
		label2.setFont(titleFont);
		label2.setText("Property Explanation:");
		
		propertyExplanation = new Text(parent, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		GridData propertyExplanationGridData = new GridData( GridData.FILL_BOTH);
		propertyExplanationGridData.grabExcessHorizontalSpace = true;
		propertyExplanationGridData.grabExcessVerticalSpace = true;
		propertyExplanationGridData.widthHint = 150;
		propertyExplanationGridData.heightHint = 200;
		propertyExplanation.setLayoutData(propertyExplanationGridData);
		
		//third row
		Label label3 = new Label(parent, SWT.NONE);
		label3.setFont(titleFont);
		label3.setText("Possible Value:");
		
		propertyValueText = new Text(parent, SWT.BORDER);
		propertyValueText.setLayoutData(gridData);
		
		TableItem selectedItem = propertiesTableViewer.getTable().getSelection()[0];
		modifyAtt = new CorrespondenceApproTypeProperty(selectedItem.getText(0), selectedItem.getText(2));
		modifyAtt.setPossibleValue(selectedItem.getText(1));
		
		return parent;
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
		OKbuttonGridData.horizontalAlignment = GridData.END;
		
		OKbutton.setLayoutData(OKbuttonGridData);		
		OKbutton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
				okPressed();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
			}
		});
		Shell shell = parent.getShell();
		if (shell != null) {
			shell.setDefaultButton(OKbutton);
		}
		
		GridData cancelButtonGridData = new GridData();
		cancelButtonGridData.grabExcessHorizontalSpace = true;
		cancelButtonGridData.horizontalAlignment = GridData.BEGINNING;
		Button cancelButton = createButton(parent, CANCEL, "Cancel", false);
		cancelButton.setLayoutData(cancelButtonGridData);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setReturnCode(CANCEL);
				close();
			}
		});
		
	}

	private boolean isValidInput() {
		boolean valid = true;
//		if (propertyNameText.getText().length() == 0) {
//			setErrorMessage("Please input the attribute name");
//			valid = false;
//		}else if (propertyExplanation.getText().length() == 0) {
//			setErrorMessage("Please input the description for the property");
//			valid = false;
//		}else if (propertyValueText.getText().length() == 0) {
//			setErrorMessage("Please input value related information");
//			valid = false;
//		}
		return valid;
	}

	// We allow the user to resize this dialog
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void okPressed() {
		CorrespondenceApproTypeProperty newApproximateProperty = new CorrespondenceApproTypeProperty();
		newApproximateProperty.setProperty_name(propertyNameText.getText());
		newApproximateProperty.setDescription(propertyExplanation.getText());
		newApproximateProperty.setPossibleValue(propertyValueText.getText());
		
		if(correspondence_ApproxType.getApproximateProperties().contains(modifyAtt)){
			  int index = correspondence_ApproxType.getApproximateProperties().indexOf(modifyAtt);
			  System.out.println("index is "+index);
			  correspondence_ApproxType.getApproximateProperties().set(index, newApproximateProperty);
			  propertiesTableViewer.setInput(correspondence_ApproxType); 
		}
		super.okPressed();
	}
	
}
