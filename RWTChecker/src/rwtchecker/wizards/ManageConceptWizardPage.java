package rwtchecker.wizards;


import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

import rwtchecker.concept.ConceptAttribute;
import rwtchecker.concept.ConceptDetail;
import rwtchecker.dialogs.ConceptAttributeEditingDialog;
import rwtchecker.util.RWTSystemUtil;
import rwtchecker.views.ConceptDetailView;
import rwtchecker.views.provider.ConceptDetailContentProvider;
import rwtchecker.views.provider.ConceptDetailLabelProvider;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (att).
 */

public class ManageConceptWizardPage extends WizardPage {

	public static final String PAGE_NAME = "ManageConceptWizardPage";
	private Text containerText;

	private TableViewer viewer;
	private Text definitonContentText;
	private Label definitonContentLabel;

	private Tree conceptListTree;
	
	private TreeItem rootItemOfConceptList;
	
	private TreeItem selectedConceptItem;
	
	private IProject currentProject;
	
	private TreeEditor renameEditor;
	
	private TableEditor editingEditor;
	
	private ConceptDetail selectedConceptDetail; 

	public ManageConceptWizardPage() {
		super(PAGE_NAME);
		setTitle("Manipulate explications for real world concepts");
		setDescription("This wizard allows modification of explications of real world concepts");
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

		Button browseBT = new Button(container, SWT.PUSH);
		browseBT.setText("Browse...");
		GridData browseBTgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		browseBTgd.horizontalSpan = 1;
		browseBT.setLayoutData(browseBTgd);
		browseBT.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
				rootItemOfConceptList.setExpanded(true);
			}
		});
			
		//place holder line
		label = new Label(container, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 6;
		label.setLayoutData(gd);
		label.setFont(titleTwoFont);
		label.setText("Select the concept requires modification");
		
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
		GridData cmtypeListGD = new GridData(GridData.FILL_BOTH);
		cmtypeListGD.grabExcessVerticalSpace = true;
		cmtypeListGD.verticalSpan = 10;
		cmtypeListGD.horizontalSpan = 1;
		conceptListTree.setLayoutData(cmtypeListGD);
		
		conceptListTree.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				clearDisplayContent();
				selectedConceptItem = conceptListTree.getSelection()[0];
				if(selectedConceptItem.getData() !=null){
					selectedConceptDetail = (ConceptDetail)selectedConceptItem.getData();
					definitonContentText.setText(selectedConceptDetail.getDefinition());
					viewer.setInput(selectedConceptDetail.getAttributes());
					definitonContentLabel.setText("Definition: "+selectedConceptDetail.getConceptName());
				}
				dialogChanged();
			} 
		});
		
		renameEditor = new TreeEditor(conceptListTree);
		renameEditor.horizontalAlignment = SWT.LEFT;
		renameEditor.grabHorizontal = true;
		
		Menu menu = new Menu (this.getShell(), SWT.POP_UP);

		MenuItem renameItem = new MenuItem (menu, SWT.PUSH);
		renameItem.setText ("Rename the concept");
		renameItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				  final TreeItem selectedTreeItem = conceptListTree.getSelection()[0];
				  editingConceptNameInTree(selectedTreeItem);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

//		MenuItem addSubConceptitem = new MenuItem (menu, SWT.PUSH);
//		addSubConceptitem.setText("Add a sub concept");
//		addSubConceptitem.addSelectionListener(new SelectionListener() {
//			@Override
//			public void widgetSelected(SelectionEvent arg0) {
//				  final TreeItem selectedTreeItem = conceptListTree.getSelection()[0];
//				  final TreeItem newConceptTreeItem = new TreeItem(selectedTreeItem, 0);
//					newConceptTreeItem.setText("new Concept");
//				  editingConceptNameInTree(newConceptTreeItem);
//			}
//			@Override
//			public void widgetDefaultSelected(SelectionEvent arg0) {
//			}
//		});
		
		conceptListTree.setMenu (menu);
		
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
		
		Button addNewAttBT = new Button(container, SWT.NONE);
		addNewAttBT.setText("add Attribute");
		GridData addNewAttGD = new GridData(GridData.HORIZONTAL_ALIGN_END);
		addNewAttGD.horizontalSpan = 3;
		addNewAttGD.grabExcessHorizontalSpace = true;
        addNewAttBT.setLayoutData(addNewAttGD);
        addNewAttBT.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				addAttributeEvent();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
        
		Button editAttBT = new Button(container, SWT.NONE);
		editAttBT.setText("edit Attribute");
		GridData editAttGD = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		editAttGD.horizontalSpan = 1;
		editAttBT.setLayoutData(editAttGD);
		editAttBT.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				editingAttributeEvent();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
        
		Button delAttBT = new Button(container, SWT.NONE);
		delAttBT.setText("delete Attribute");
		GridData delAttGD = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		delAttGD.horizontalSpan = 1;
		delAttBT.setLayoutData(delAttGD);
		delAttBT.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				deleteAttributeEvent();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		
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

		editingEditor = new TableEditor(viewer.getTable());
		editingEditor.horizontalAlignment = SWT.LEFT;
		editingEditor.grabHorizontal = true; 
		
		Button applyChangeBT = new Button(container, SWT.NULL);
		applyChangeBT.setText("Apply Changes to concept");
		GridData applyChangeBTgd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		applyChangeBTgd.grabExcessHorizontalSpace = true;
		applyChangeBTgd.horizontalSpan = 4;
		applyChangeBT.setLayoutData(applyChangeBTgd);
		applyChangeBT.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if(currentProject != null){
					selectedConceptDetail.setDefinition(definitonContentText.getText());
					File newConceptFile = RWTSystemUtil.getConceptDetailFile(currentProject, selectedConceptDetail.getConceptName());
					ConceptDetail.writeOutConceptDetails(selectedConceptDetail, newConceptFile);
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		
		Button addNewConceptBT = new Button(container, SWT.NULL);
		addNewConceptBT.setText("Add new Concept");
		GridData addNewConceptBTgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		addNewConceptBTgd.horizontalSpan = 1;
		addNewConceptBT.setLayoutData(addNewConceptBTgd);
		addNewConceptBT.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
//				NewConceptWizard wizard = new NewConceptWizard(currentProject);
//				wizard.init(PlatformUI.getWorkbench(), null);
//			    WizardDialog dialog = new WizardDialog(ManageConceptWizardPage.this.getShell(), wizard);
//			    dialog.create();
//			    dialog.open();
				final TreeItem newConceptTreeItem = new TreeItem(rootItemOfConceptList, 0);
				ConceptDetail conceptDetail = new ConceptDetail();
				conceptDetail.setConceptName("new Concept");
				newConceptTreeItem.setText(conceptDetail.getConceptName());
				newConceptTreeItem.setData(conceptDetail);
				File conceptFile = RWTSystemUtil.getConceptDetailFile(currentProject, conceptDetail.getConceptName());
				ConceptDetail.writeOutConceptDetails(conceptDetail, conceptFile);
				selectedConceptDetail = conceptDetail;
				editingConceptNameInTree(newConceptTreeItem);  
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		
		Button deleteConceptBT = new Button(container, SWT.NULL);
		deleteConceptBT.setText("Delete Concept");
		GridData deleteConceptBTgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		deleteConceptBTgd.horizontalSpan = 1;
		deleteConceptBT.setLayoutData(deleteConceptBTgd);
		deleteConceptBT.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		hookActionsToAttributeTable();
		this.getShell().setSize(1000, 800);
		dialogChanged();
		setControl(container);
		setPageComplete(false);
	}
	private void addAttributeEvent(){
		 ConceptAttribute newAtt = new ConceptAttribute("new Attribute", "new Attribute Definition");
		  if(!selectedConceptDetail.getAttributes().contains(newAtt)){
			  selectedConceptDetail.getAttributes().add(newAtt);
			  viewer.setInput(selectedConceptDetail.getAttributes()); 
		 }else{
			 updateStatus("Existing concept attribute, try another name for the attribute");
		 }
	}
	
	private void editingAttributeEvent(){
		if(viewer.getTable().getSelection()[0] != null){
			IWorkbench workbench = PlatformUI.getWorkbench();
    		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    		ConceptAttributeEditingDialog dialog = new ConceptAttributeEditingDialog(window.getShell(), viewer, selectedConceptDetail );
			dialog.create();
			dialog.open();		
		}
	}
	
	private void deleteAttributeEvent(){
  	  TableItem selectedItem = viewer.getTable().getSelection()[0];
	  if(selectedItem != null){
		  ConceptAttribute deleteAtt = new ConceptAttribute(selectedItem.getText(0), selectedItem.getText(1));
		  if(selectedConceptDetail.getAttributes().contains(deleteAtt)){
			  selectedConceptDetail.getAttributes().remove(deleteAtt);
    		  viewer.setInput(selectedConceptDetail.getAttributes()); 
		  }
	  }
	}
	private void hookActionsToAttributeTable() {
	    Menu menu = new Menu(viewer.getTable().getShell(), SWT.POP_UP);
	    viewer.getTable().setMenu(menu);
	    MenuItem addAttItem = new MenuItem(menu, SWT.PUSH);
	    addAttItem.setText("Add new attribute");
	    addAttItem.addListener(SWT.Selection, new Listener() {
	      public void handleEvent(Event event) {
//	    	  viewer.getTable().remove(viewer.getTable().getSelectionIndices());
	    	  addAttributeEvent();
	      }
	    });
	    MenuItem removeAttItem = new MenuItem(menu, SWT.PUSH);
	    removeAttItem.setText("Delete This attribute");
	    removeAttItem.addListener(SWT.Selection, new Listener() {
	      public void handleEvent(Event event) {
//	    	  viewer.getTable().remove(viewer.getTable().getSelectionIndices());
	    	  deleteAttributeEvent();
	      }
	    });
	    
	    viewer.getTable().addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent arg0) {
			}
			@Override
			public void mouseDown(MouseEvent arg0) {
			}
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {
				editingAttributeEvent();
				dialogChanged();
			}
		});

	}
	
	private void editingConceptNameInTree(final TreeItem treeItem){
	    rootItemOfConceptList.setExpanded(true);
        // Create a text field to do the editing
        final Text text = new Text(conceptListTree, SWT.NONE);
        text.setText(treeItem.getText());
        text.selectAll();
        text.setFocus();

        text.addFocusListener(new FocusAdapter() {
          public void focusLost(FocusEvent event) {
        	  treeItem.setText(text.getText());
        	  renameConceptEvent(treeItem.getText());
        	  text.dispose();
          }
        });

        text.addKeyListener(new KeyAdapter() {
          public void keyPressed(KeyEvent event) {
            switch (event.keyCode) {
            case SWT.CR:
            	treeItem.setText(text.getText());
            	renameConceptEvent(treeItem.getText());
            case SWT.ESC:
              text.dispose();
              break;
            }
          }
        });
        renameEditor.setEditor(text, treeItem);
	}
	
	private void renameConceptEvent(String newConceptName){
		File oldConceptFile = RWTSystemUtil.getConceptDetailFile(currentProject, selectedConceptDetail.getConceptName());
		File newConceptFile = RWTSystemUtil.getConceptDetailFile(currentProject, newConceptName);
		selectedConceptDetail.setConceptName(newConceptName);
		oldConceptFile.renameTo(newConceptFile);
		ConceptDetail.writeOutConceptDetails(selectedConceptDetail, newConceptFile);
	}
	
//	 private void attachCellEditors(final TableViewer viewer, Composite parent) {
//		 final String NAME_PROPERTY = "attribute_name";
//		 String DEFINITION_PROPERTY = "attribute_name";
//		    
//		    viewer.setCellModifier(new ICellModifier() {
//		      public boolean canModify(Object element, String property) {
//		        return true;
//		      }
//
//		      public Object getValue(Object element, String property) {
//		        if (NAME_PROPERTY.equals(property))
//		          return ((ConceptAttribute) element).getAttributeName();
//		        else
//		          return ((ConceptAttribute) element).getAttributeExplanation();
//		      }
//
//		      public void modify(Object element, String property, Object value) {
//		        TableItem tableItem = (TableItem) element;
//		        ConceptAttribute data = (ConceptAttribute) tableItem
//		            .getData();
//		        if (NAME_PROPERTY.equals(property))
//		        	data.setAttributeName(value.toString());
//		        else
//		        	data.setAttributeExplanation(value.toString());
//
//		        viewer.refresh(data);
//		      }
//		    });
//
//		    viewer.setCellEditors(new CellEditor[] { new TextCellEditor(parent),
//		        new TextCellEditor(parent) });
//		    viewer.setColumnProperties(new String[] { NAME_PROPERTY,
//		            DEFINITION_PROPERTY });
//	 }
	
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
	
	/**
	 * Uses the standard container selection dialog to choose the new value for
	 * the container field.
	 */

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
				ConceptDetail[] conceptList = ConceptDetail.readInAllConceptDetails(this.currentProject);
				conceptListTree.removeAll();
				rootItemOfConceptList = new TreeItem(conceptListTree, 0);
				rootItemOfConceptList.setText("Concept List");
				for(int i=0;i<conceptList.length;i++){
					TreeItem childItem = new TreeItem(rootItemOfConceptList, 0);
					childItem.setText(conceptList[i].getConceptName());
					childItem.setData(conceptList[i]);
				}
			}
		}
	}
	
	/**
	 * Ensures that both text fields are set.
	 */

	private void dialogChanged() {
		IResource container = ResourcesPlugin.getWorkspace().getRoot()
				.findMember(new Path(getContainerName()));
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

		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	

	public String getContainerName() {
		return containerText.getText();
	}
	
	public IProject getCurrentProject() {
		return currentProject;
	}

	public void setCurrentProject(IProject currentProject) {
		this.currentProject = currentProject;
	}
	
	private void clearDisplayContent(){
		this.definitonContentText.setText("");
		viewer.setInput(null);
	}
}