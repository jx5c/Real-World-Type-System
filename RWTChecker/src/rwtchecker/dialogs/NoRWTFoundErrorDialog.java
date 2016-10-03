package rwtchecker.dialogs;

import java.awt.Checkbox;
import java.awt.Container;
import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import rwtchecker.util.ActivePart;
import rwtchecker.util.RWTSystemUtil;

public class NoRWTFoundErrorDialog extends TitleAreaDialog {

	private String currentProject;
	private String rwtLocation ; 
	
	Font titleFont;
	
	public NoRWTFoundErrorDialog(Shell parentShell, String currentProject) {
		super(parentShell);
		this.currentProject = currentProject;
	}

	@Override
	public void create() {
		super.create();
		setTitle("Please choose a folder to store the real-world type system");
		setMessage("The location of the RWT system is not set yet", IMessageProvider.ERROR);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		titleFont = new Font(parent.getDisplay(),"Arial", 10 , SWT.BOLD );
		final Composite shlScrambledata = parent;
//		shlScrambledata.setSize(450, 405);
//	    shlScrambledata.setText("ScrambleData");
	    
	    
		// level one
	    Group docTypeGroup = new Group(shlScrambledata, SWT.NULL);
	    docTypeGroup.setLocation(10, 10);
	    docTypeGroup.setSize(400, 49);
//	    docTypeGroup.setText("Project Name");

	    Label projectLabel = new Label(docTypeGroup, SWT.NULL);
	    projectLabel.setBounds(10, 20, 80, 20);
	    projectLabel.setText("Project Name: ");
	    
	    Label projectNameLabel = new Label(docTypeGroup, SWT.BORDER);
	    projectNameLabel.setBounds(100, 20, 430, 20);
	    projectNameLabel.setText(this.currentProject);

	    // level two
	    Group grpSelectTheFake = new Group(shlScrambledata, SWT.NONE);
	    Button defaultBT = new Button(grpSelectTheFake, SWT.CHECK);
	    defaultBT.setText("Use default location");
	    defaultBT.setBounds(10, 18, 520, 20);   


	    // level three
	    Group grpSelectScrambleFile = new Group(shlScrambledata, SWT.NONE);
	    Label projLocLabel = new Label(grpSelectScrambleFile, SWT.NULL);
	    projLocLabel.setBounds(10, 18, 120, 20);
	    projLocLabel.setText("RWT System Location: ");
	    
	    final Text rwtsystemLocText = new Text(grpSelectScrambleFile, SWT.BORDER);
	    rwtsystemLocText.setBounds(140, 18, 300, 20);

	    Button rwtSystemBT = new Button(grpSelectScrambleFile, SWT.NONE);
	    rwtSystemBT.setBounds(450, 18, 80, 20);
	    rwtSystemBT.setText("Browse...");
	    rwtSystemBT.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dlg = new DirectoryDialog(parent.getShell());
		        dlg.setText("Real-world type system Location");
		        // Customizable message displayed in the dialog
		        dlg.setMessage("Select the Directory for the real-world type system");
		        String dir = dlg.open();
		        if(dir != null && isValidInput(dir)){
		        	rwtsystemLocText.setText(dir);
		        	rwtLocation = dir;
		        }
			}
		});
	    
	    defaultBT.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button btn = (Button) e.getSource();
	            if(btn.getSelection()){
	            	String defaultLoc = loadDefaultLocation();
	            	rwtsystemLocText.setText(defaultLoc);
	            	rwtLocation = defaultLoc;
	            	rwtsystemLocText.setEditable(false);
	            }else{
	            	rwtsystemLocText.setEditable(true);
	            }
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	    shlScrambledata.layout();
	    return parent;
	}
	
	private String loadDefaultLocation(){
		IFile currentFile =  ActivePart.getFileOfActiveEditror();
		if( currentFile != null){
	        IProject project = currentFile.getProject();
	        String defaultLoc = project.getLocation().toFile().getAbsolutePath()+RWTSystemUtil.PathSeparator 
	        		+ RWTSystemUtil.defaultRWTSystemFolder;
	        return defaultLoc;
		}
		return "";
	}
	
	private boolean isValidInput(String dir) {
		File rwtLoc = new File(dir);
		if(!rwtLoc.exists() || rwtLoc.isDirectory()){
			return true;
		}else{
			return false;
		}
	}

	@Override
	protected boolean isResizable() {
		return false;
	}
	
	@Override
	protected void okPressed() {
		if(isValidInput(this.rwtLocation)){
			RWTSystemUtil.storePropertyToConfigFile(currentProject, this.rwtLocation);
			File top = new File(this.rwtLocation);
			if(!top.exists()){
				top.mkdir();
			}
	    	String conceptDir = this.rwtLocation + RWTSystemUtil.PathSeparator + RWTSystemUtil.ConceptDefinitionFolder;
	    	if(!new File(conceptDir).exists()){
	    		new File(conceptDir).mkdir();	
	    	}
	    	String RWTypeDir = this.rwtLocation + RWTSystemUtil.PathSeparator + RWTSystemUtil.CMTypesFolder;
	    	if(!new File(RWTypeDir).exists()){
	    		new File(RWTypeDir).mkdir();
	    	}
	    	String annotationDir = this.rwtLocation + RWTSystemUtil.PathSeparator + RWTSystemUtil.annotationFolder;
	    	if(!new File(annotationDir).exists()){
	    		new File(annotationDir).mkdir();
	    	}	
	    	this.setReturnCode(OK);
		}else{
			this.setReturnCode(CANCEL);	
		}
		super.okPressed();
	}
}
