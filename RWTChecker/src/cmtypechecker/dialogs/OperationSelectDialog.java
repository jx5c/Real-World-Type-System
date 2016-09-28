package cmtypechecker.dialogs;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import cmtypechecker.CMRules.CMTypeRuleCategory;

public class OperationSelectDialog extends TitleAreaDialog {

	private Text operationDescriptionText;
	private Tree operationListTree;
	private Text targetText;
	private Text operandTwoTypeText;
	

	public OperationSelectDialog(Shell parentShell, Text operationText,Text operandTwoTypeText) {
		super(parentShell);
		this.targetText = operationText;
		this.operandTwoTypeText = operandTwoTypeText;
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
		Font titleFont = new Font(parent.getDisplay(),"Arial", 10 , SWT.BOLD );
		
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 1;
		
		operationListTree = new Tree(container, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		GridData operationListTreeGD = new GridData();
		operationListTreeGD.verticalAlignment = GridData.FILL;
		operationListTreeGD.widthHint = 400;
		operationListTreeGD.heightHint = 250;
		operationListTree.setLayoutData(operationListTreeGD);
		operationListTree.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				TreeItem selectedItem = operationListTree.getSelection()[0];
				if(selectedItem.getData() !=null){
					CMTypeRuleCategory thisOperationType = (CMTypeRuleCategory)(selectedItem.getData());
					operationDescriptionText.setText(thisOperationType.getDescription());
				}
			} 
		});
		LoadOperationsInTree();
		
		Label label = new Label(container, SWT.NONE);
		label.setFont(titleFont);
		label.setText("Operation Explanation:");
		GridData labelGd = new GridData();
		label.setLayoutData(labelGd);
		
		operationDescriptionText = new Text(container, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		GridData operationDescriptionTextGridData = new GridData( GridData.FILL_BOTH);
		operationDescriptionTextGridData.grabExcessHorizontalSpace = true;
		operationDescriptionTextGridData.heightHint = 150;
		operationDescriptionTextGridData.grabExcessVerticalSpace = true;
		operationDescriptionText.setLayoutData(operationDescriptionTextGridData);
		
//		container.getShell().setSize(450, 600);
//		container.getShell().pack();
		return container;
	}

	private void LoadOperationsInTree() {
		ArrayList<CMTypeRuleCategory> operationList = CMTypeRuleCategory.getDefaultOperationList();
		operationListTree.removeAll();
		TreeItem rootItemForOperations = new TreeItem(operationListTree, 0);
		rootItemForOperations.setText("Operations");
		//reserve here
		for(int i=0;i<operationList.size();i++){
			TreeItem childItem = new TreeItem(rootItemForOperations, 0);
			childItem.setText(operationList.get(i).getName());
			childItem.setData(operationList.get(i));
		}
		rootItemForOperations.setExpanded(true);		
	}
	
	// We do not allow the user to resize this dialog
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void okPressed() {
		TreeItem selectedItem = operationListTree.getSelection()[0];
		if(selectedItem!=null){
			targetText.setText(selectedItem.getText());
			CMTypeRuleCategory thisOperationType = (CMTypeRuleCategory)(selectedItem.getData());
			if(thisOperationType.getParameterCourt() > 1){
				operandTwoTypeText.setEditable(true);	
				operandTwoTypeText.setEnabled(true);
			}
			if(thisOperationType.getParameterCourt() == 1){
				operandTwoTypeText.setEditable(false);	
				operandTwoTypeText.setText("");
				operandTwoTypeText.setEnabled(false);
			}
//			if(thisOperationType.getName().equals(OperationTypeForCMTypes.AllowLiteralOperations)){
//				operandTwoTypeText.setText("Number_Literal");
//			}
		}
		super.okPressed();
	}
	
}
