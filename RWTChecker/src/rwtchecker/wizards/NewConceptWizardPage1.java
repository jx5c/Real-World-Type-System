package rwtchecker.wizards;


import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

import rwtchecker.concept.ConceptDetail;
import rwtchecker.util.RWTSystemUtil;
import rwtchecker.views.provider.ConceptDetailContentProvider;
import rwtchecker.views.provider.ConceptDetailLabelProvider;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (att).
 */

public class NewConceptWizardPage1 extends WizardPage {

	public static final String PAGE_NAME = "NewConceptWizardPage1";
	private Text containerText;

	private Text conceptNameText;

	private TableViewer viewer;
	private Text definitonContentText;
	private Label definitonContentLabel;

	private Tree conceptListTree;
	
	private IProject currentProject;

	public NewConceptWizardPage1() {
		super(PAGE_NAME);
		setTitle("New Concept Wizard page 1");
		setDescription("This wizard creates a new concept explication");
	}

	public NewConceptWizardPage1(IProject thisProject) {
		super(PAGE_NAME);
		setTitle("New Concept Wizard page 1");
		setDescription("This wizard creates a new concept explication");
		this.currentProject = thisProject; 
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 6;
//		layout.verticalSpacing = 9;
		Font titleOneFont = new Font(parent.getDisplay(),"courier new", 9 , SWT.BOLD );
		Font titleTwoFont = new Font(parent.getDisplay(),"arial", 10 , SWT.BOLD );
		
		//Project line
		Label label = new Label(container, SWT.NULL);
		label.setText("&Project:");
		label.setFont(titleOneFont);

		containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 4;
		containerText.setLayoutData(gd);
		containerText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button button = new Button(container, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});
		
		//Concept Name line
		label = new Label(container, SWT.NULL);
		label.setText("&Concept name:");
		label.setFont(titleOneFont);
		
		conceptNameText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		conceptNameText.setLayoutData(gd);
		gd.horizontalSpan = 5;
		conceptNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		
		//place holder line
		label = new Label(container, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 6;
		label.setLayoutData(gd);
		label.setFont(titleTwoFont);
		label.setText("&Existing Concept");
		
		//Concept detail type information
		Label conceptList = new Label(container, SWT.NULL);
		conceptList.setText("Concept List");
		conceptList.setFont(titleOneFont);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		conceptList.setLayoutData(gd);
		
		Label conceptDetailLabel = new Label(container, SWT.NULL);
		conceptDetailLabel.setText("Concept Detail Information");
		conceptDetailLabel.setFont(titleOneFont);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 5;
		conceptDetailLabel.setLayoutData(gd);
		
		conceptListTree = new Tree(container, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		GridData cmtypeListGD = new GridData();
		cmtypeListGD.verticalAlignment = GridData.FILL;
		cmtypeListGD.horizontalAlignment = GridData.FILL;
		cmtypeListGD.verticalSpan = 9;
		conceptListTree.setLayoutData(cmtypeListGD);
		
		conceptListTree.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				TreeItem selectedItem = conceptListTree.getSelection()[0];
				if(selectedItem.getData() !=null){
					ConceptDetail conceptDetail = (ConceptDetail)selectedItem.getData();
					definitonContentText.setText(conceptDetail.getDefinition());
					viewer.setInput(conceptDetail.getAttributes());
					definitonContentLabel.setText("Definition: "+conceptDetail.getConceptName());
				}
			} 
		});
		createTreeItems();
		
		//Concept Detail 
		definitonContentLabel = new Label(container, SWT.NONE);
		definitonContentLabel.setText("Definition: ");
		GridData definitonContentLabelgd = new GridData();
		definitonContentLabelgd.verticalSpan = 1;
		definitonContentLabelgd.widthHint = 400;
		definitonContentLabel.setLayoutData(definitonContentLabelgd);
		
		definitonContentText = new Text(container, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		definitonContentText.setText("");
		
		GridData definitonContentTextgd = new GridData();
		definitonContentTextgd.verticalAlignment = GridData.FILL;
		definitonContentTextgd.horizontalAlignment = GridData.FILL;
		definitonContentTextgd.grabExcessHorizontalSpace = true;
		definitonContentTextgd.grabExcessVerticalSpace = true;
		definitonContentTextgd.horizontalSpan = 5;
		definitonContentTextgd.verticalSpan = 4;
		definitonContentTextgd.widthHint = 400;
		definitonContentText.setLayoutData(definitonContentTextgd);
		
        viewer = new TableViewer(container, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        GridData conceptDetailGD = new GridData();
        conceptDetailGD.verticalAlignment = GridData.FILL;
        conceptDetailGD.horizontalAlignment = GridData.FILL;
        conceptDetailGD.grabExcessHorizontalSpace = true;
        conceptDetailGD.grabExcessVerticalSpace = true;
        conceptDetailGD.horizontalSpan = 5;
        conceptDetailGD.verticalSpan = 4;
        conceptDetailGD.widthHint = 400;
        createAttributeTableColumns(viewer);
        viewer.getTable().setLayoutData(conceptDetailGD);
		viewer.setContentProvider(new ConceptDetailContentProvider());
		viewer.setLabelProvider(new ConceptDetailLabelProvider());
		viewer.setInput(null);

//		this.getShell().setSize(1000, 800);
		
		dialogChanged();
		setControl(container);
		setPageComplete(false);
	}
	
	private void createAttributeTableColumns(final TableViewer viewer) {
		Table table = viewer.getTable();
		String[] titles = { "Attribue", "Definition"};
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
	
	private void createTreeItems(){
		if(this.currentProject != null){
			containerText.setText(this.currentProject.getFullPath().toString());
			ConceptDetail[] conceptList = ConceptDetail.readInAllConceptDetails(this.currentProject);
			conceptListTree.removeAll();
			TreeItem rootItem = new TreeItem(conceptListTree, 0);
			rootItem.setText("Concept List");
			for(int i=0;i<conceptList.length;i++){
				TreeItem childItem = new TreeItem(rootItem, 0);
				childItem.setText(conceptList[i].getConceptName());
				childItem.setData(conceptList[i]);
			}	
		}
	}

	private void handleBrowse() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(
				getShell(), ResourcesPlugin.getWorkspace().getRoot(), false,
				"Select project");		
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				containerText.setText(((Path) result[0]).toString());
				IResource container = ResourcesPlugin.getWorkspace().getRoot()
				.findMember(new Path(getContainerName()));
				this.currentProject =  container.getProject();
				createTreeItems();
			}
		}
	}
	
	/**
	 * Ensures that both text fields are set.
	 */

	private void dialogChanged() {
		IResource container = ResourcesPlugin.getWorkspace().getRoot()
				.findMember(new Path(getContainerName()));
		String conceptName = getConceptName();

		if (getContainerName().length() == 0) {
			updateStatus("File container must be specified");
			return;
		}
		if (container == null
				|| (container.getType() & (IResource.PROJECT )) == 0) {
			updateStatus("Project must exist");
			return;
		}
		if (!container.isAccessible()) {
			updateStatus("Project must be writable");
			return;
		}

		if (conceptName.length() == 0) {
			updateStatus("Concept name must be specified");
			return;
		}
		if (conceptName.replace('\\', '/').indexOf('/', 1) > 0) {
			updateStatus("Concept name must be valid");
			return;
		}
		
		File file = RWTSystemUtil.getConceptDetailFile(currentProject, conceptName);		
		if(file.exists()){
			updateStatus("Concept already exists");
			return;
		}
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	
	@Override
	public IWizardPage getNextPage(){
		NewConceptWizardPage2 page = (NewConceptWizardPage2)getWizard().getPage(NewConceptWizardPage2.PAGE_NAME);
		page.updateConceptNameText(this.getConceptName());
		return page;
	}

	public String getContainerName() {
		return containerText.getText();
	}
	
	public String getConceptName() {
		return conceptNameText.getText();
	}

	public IProject getCurrentProject() {
		return currentProject;
	}

	public void setCurrentProject(IProject currentProject) {
		this.currentProject = currentProject;
		createTreeItems();
	}
}