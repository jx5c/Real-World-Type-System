package rwtchecker.dialogs;


import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import rwtchecker.rwtrules.RWTypeRule;
import rwtchecker.rwtrules.RWTypeRulesManager;
public class TypeRuleDisplayDialog extends TitleAreaDialog {

	private Text operandOneText;
	private Text operandTwoText;
	private Text operatorText;
	private Text resultText;
	
	
	private RWTypeRule cmTypeOperation;
	private RWTypeRulesManager manager;
	
	public TypeRuleDisplayDialog(Shell parentShell, RWTypeRule cmTypeOperation, RWTypeRulesManager manager) {
		super(parentShell);
		this.cmTypeOperation = cmTypeOperation;
		this.manager = manager;
	}

	@Override
	public void create() {
		super.create();
		// Set the title
		setTitle("Display of selected CM type rule");
		// Set the message
		setMessage("Display of selected CM type rule", IMessageProvider.INFORMATION);
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
		
		//first row
		Label label1 = new Label(parent, SWT.NONE);
		label1.setFont(titleFont);
		label1.setText("Operand One:");
		
		operandOneText = new Text(parent, SWT.BORDER);
		operandOneText.setEditable(true);
		operandOneText.setLayoutData(gridData);
		
		//second row
		Label label2 = new Label(parent, SWT.NONE);
		label2.setFont(titleFont);
		label2.setText("Operand Two:");
		
		operandTwoText = new Text(parent, SWT.BORDER);
		operandTwoText.setEditable(true);
		operandTwoText.setLayoutData(gridData);
		
		//third row
		Label label3 = new Label(parent, SWT.NONE);
		label3.setFont(titleFont);
		label3.setText("Operator:");
		
		operatorText = new Text(parent, SWT.BORDER);
		operatorText.setEditable(false);
		operatorText.setLayoutData(gridData);
		
		//fouth row
		Label label4 = new Label(parent, SWT.NONE);
		label4.setFont(titleFont);
		label4.setText("Result Type:");
		
		resultText = new Text(parent, SWT.BORDER);
		resultText.setEditable(true);
		resultText.setLayoutData(gridData);
		
		
		loadContents();
		return parent;
	}
	
	private void loadContents(){
		operandOneText.setText(this.cmTypeOperation.getCMTypeOneName());
		operandTwoText.setText(this.cmTypeOperation.getCMTypeTwoName());
		operatorText.setText(this.cmTypeOperation.getOperationName());
		resultText.setText(this.cmTypeOperation.getReturnCMTypeName());
	}

	@Override
	protected boolean isResizable() {
		return false;
	}
	
	@Override
	protected void okPressed() {
		
		this.manager.delCMTypeOperation(cmTypeOperation);
		RWTypeRule newRule = new RWTypeRule();
		newRule.setCMTypeOneName(operandOneText.getText());
		newRule.setCMTypeTwoName(operandTwoText.getText());
		newRule.setOperationName(operatorText.getText());
		newRule.setReturnCMTypeName(resultText.getText());
		this.manager.addCMTypeOperation(newRule);
		manager.storeRules();
		super.okPressed();
	}
	
}
