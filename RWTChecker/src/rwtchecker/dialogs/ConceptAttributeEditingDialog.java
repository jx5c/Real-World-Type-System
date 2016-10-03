package rwtchecker.dialogs;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.TableViewer;
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
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import rwtchecker.concept.ConceptAttribute;
import rwtchecker.concept.ConceptDetail;

public class ConceptAttributeEditingDialog extends TitleAreaDialog {

	private Text attributeNameText;
	private Text attributeDetailText;
	private ConceptAttribute modifyAtt;
	private TableViewer viewer; 
	private ConceptDetail selectedConceptDetail;
	
	public ConceptAttributeEditingDialog(Shell parentShell, TableViewer viewer, ConceptDetail selectedConceptDetail) {
		super(parentShell);
		this.viewer = viewer;
		this.selectedConceptDetail = selectedConceptDetail;
	}

	@Override
	public void create() {
		super.create();
		setTitle("This is the dialog for attribute editing");
		setMessage("use this dialog to edit the attribute", IMessageProvider.INFORMATION);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		GridLayout layout = new GridLayout(2, false);
		layout.numColumns = 2;
		// layout.horizontalAlignment = GridData.FILL;
		parent.setLayout(layout);

		// The text fields will grow with the size of the dialog
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.widthHint = 100;
		Label label1 = new Label(parent, SWT.NONE);
		Font titleFont = new Font(parent.getDisplay(),"courier new", 9 , SWT.ITALIC );
		label1.setFont(titleFont);
		label1.setText("Attribute Name:");
		
		attributeNameText = new Text(parent, SWT.BORDER);
		attributeNameText.setLayoutData(gridData);
		Label label2 = new Label(parent, SWT.NONE);
		label2.setFont(titleFont);
		label2.setText("Attribute Definition:");
		attributeDetailText = new Text(parent, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		GridData attributeDetailTextGridData = new GridData( GridData.FILL_BOTH);
		attributeDetailTextGridData.grabExcessHorizontalSpace = true;
		attributeDetailTextGridData.grabExcessVerticalSpace = true;
		attributeDetailTextGridData.widthHint = 150;
		attributeDetailTextGridData.heightHint = 200;
		attributeDetailText.setLayoutData(attributeDetailTextGridData);
		
		
		TableItem selectedItem = viewer.getTable().getSelection()[0];
		modifyAtt = new ConceptAttribute(selectedItem.getText(0), selectedItem.getText(1));
		
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
		this.attributeNameText.setText(modifyAtt.getAttributeName());
		this.attributeDetailText.setText(modifyAtt.getAttributeExplanation());

	}

	private boolean isValidInput() {
		boolean valid = true;
		if (attributeNameText.getText().length() == 0) {
			setErrorMessage("Please input the attribute name");
			valid = false;
		}
		if (attributeDetailText.getText().length() == 0) {
			setErrorMessage("Please input the attribute detail");
			valid = false;
		}
		return valid;
	}

	@Override
	protected boolean isResizable() {
		return false;
	}

	private void editEntryInTable() {
		String attributeName = attributeNameText.getText();
		String attributeDefinition = attributeDetailText.getText();
		
//		selectedConceptDetail.getAttributes().remove(modifyAtt);
		ConceptAttribute newAtt = new ConceptAttribute(attributeName, attributeDefinition);
		if(selectedConceptDetail.getAttributes().contains(modifyAtt)){
			  int index = selectedConceptDetail.getAttributes().indexOf(modifyAtt);
			  selectedConceptDetail.getAttributes().set(index, newAtt);
			  viewer.setInput(selectedConceptDetail.getAttributes()); 
		}
	}

	@Override
	protected void okPressed() {
		editEntryInTable();
		super.okPressed();
	}
}
