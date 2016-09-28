package cmtypechecker.dialogs;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
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
import org.eclipse.swt.widgets.Text;

import cmtypechecker.util.CMModelUtil;
import cmtypechecker.views.provider.TreeObject;

public class CMTypeRenameDialog extends TitleAreaDialog {

	private Text oldNameText;
	private Text newNameText;
	
	
	private TreeObject renameTreeObject;
	private IProject currentProject;
	
	public CMTypeRenameDialog(Shell parentShell, IProject currentProject, TreeObject renameTreeObject) {
		super(parentShell);
		this.renameTreeObject = renameTreeObject;
		this.currentProject = currentProject;
	}

	@Override
	public void create() {
		super.create();
		// Set the title
		setTitle("change the name of the correspondence type");
		// Set the message
		setMessage("change the name of the correspondence type", IMessageProvider.INFORMATION);
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
		label1.setText("Old Name:");
		
		oldNameText = new Text(parent, SWT.BORDER);
		oldNameText.setEditable(false);
		oldNameText.setLayoutData(gridData);
		
		//second row
		Label label2 = new Label(parent, SWT.NONE);
		label2.setFont(titleFont);
		label2.setText("New Name:");
		
		newNameText = new Text(parent, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		newNameText.setLayoutData(gridData);
		
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
				if(isValidInput()){
					okPressed();	
				}
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
		
		oldNameText.setText(renameTreeObject.getName());
		newNameText.setText("");
		newNameText.setFocus();
	}
	
	private boolean isValidInput() {
		boolean valid = true;

		if(newNameText.getText().length() == 0){
			setErrorMessage("Please input the new name");
			valid = false;
		}
		else if ((newNameText.getText().replace('\\', '/').indexOf('/', 1) > 0)
				|| (newNameText.getText().indexOf(':') >= 0)
				|| (newNameText.getText().indexOf('?') >= 0)
				|| (newNameText.getText().indexOf('*') >= 0)
				|| (newNameText.getText().indexOf('"') >= 0)
				|| (newNameText.getText().indexOf('<') >= 0)
				|| (newNameText.getText().indexOf('>') >= 0)
				|| (newNameText.getText().indexOf('|') >= 0)
				) {
			setErrorMessage("do not use invalid characters in the new name");
			valid = false;
		}else{
			renameTreeObject.setName(newNameText.getText().trim());
			File newCMTypeFile = CMModelUtil.getCMTypeFile(currentProject, renameTreeObject);
			if(newCMTypeFile.exists()){
				setErrorMessage("the file for that name is existed, please use another name");
				renameTreeObject.setName(oldNameText.getText().trim());
				valid = false;
			}
		}
		return valid;
	}

	@Override
	protected boolean isResizable() {
		return false;
	}
	
	@Override
	protected void okPressed() {
		super.okPressed();
	}
	
}
