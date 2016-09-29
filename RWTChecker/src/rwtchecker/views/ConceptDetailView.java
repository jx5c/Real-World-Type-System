package rwtchecker.views;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;



import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;

import rwtchecker.realworldmodel.ConceptDetail;
import rwtchecker.util.CMModelUtil;
import rwtchecker.views.provider.ConceptDetailContentProvider;
import rwtchecker.views.provider.ConceptDetailLabelProvider;
import rwtchecker.wizards.ManageConceptWizard;
import rwtchecker.wizards.NewConceptWizard;

public class ConceptDetailView extends ViewPart {

	public static final String ID = "cmtypechecker.views.ConceptDetailView";

	private TableViewer viewer;
	private Text definitonContentText;
	
//	private Action addNewConceptDetailAction;
	private Action manageConceptDetailAction;
	private Action doubleClickAction;
	
	SashForm sashFormMain;
    SashForm sashFormSub;
    
	class NameSorter extends ViewerSorter {
	}
	public TableViewer getViewer() {
		return viewer;
	}	
	
	public ConceptDetailView() {
	}

	public void createPartControl(Composite parent) {	
        sashFormMain = new SashForm(parent, SWT.VERTICAL | SWT.NULL);

        final Composite composite = new Composite(sashFormMain, SWT.None);
		GridLayout layout = new GridLayout(1, false);
		composite.setLayout(layout);

		Label definitonContentLabel = new Label(composite, SWT.NONE);
		definitonContentLabel.setText("Definition: ");
		definitonContentText = new Text(composite, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		GridData definitionGridData = new GridData( GridData.FILL_BOTH);
		definitionGridData.grabExcessHorizontalSpace = true;
		definitonContentText.setLayoutData(definitionGridData);
		definitonContentText.setText("");
		sashFormSub = new SashForm(sashFormMain, SWT.HORIZONTAL | SWT.NULL);
                
        viewer = new TableViewer(sashFormSub, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

        createAttributeTableColumns(viewer);
		viewer.setContentProvider(new ConceptDetailContentProvider());
		viewer.setLabelProvider(new ConceptDetailLabelProvider());
		viewer.setInput(null);

        // Set the size of the split pane
        sashFormMain.setWeights(new int [] {40, 60});
        
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}
	
	private void createAttributeTableColumns(final TableViewer viewer) {
		Table table = viewer.getTable();
		String[] titles = { "Attribue", "Definition"};
		int[] bounds = { 100, 400};
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
	
	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				ConceptDetailView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}
	private void fillContextMenu(IMenuManager manager) {
//		manager.add(addNewConceptDetailAction);
		manager.add(manageConceptDetailAction);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}
	private void fillLocalPullDown(IMenuManager manager) {
//		manager.add(addNewConceptDetailAction);
		manager.add(manageConceptDetailAction);
		manager.add(new Separator());
	}
	private void fillLocalToolBar(IToolBarManager manager) {
//		manager.add(addNewConceptDetailAction);
		manager.add(manageConceptDetailAction);
	}

	private void makeActions() {
		
//		addNewConceptDetailAction = new Action(){
//			public void run() {
//			    // Instantiates and initializes the wizard
//				NewConceptWizard wizard = new NewConceptWizard();
//				wizard.init(ConceptDetailView.this.getSite().getWorkbenchWindow().getWorkbench(),
//			            null);
//			    // Instantiates the wizard container with the wizard and opens it
//			    WizardDialog dialog = new WizardDialog(ConceptDetailView.this.getSite().getShell(), wizard);
//			    dialog.create();
//			    dialog.open();
//			}
//		};
//		addNewConceptDetailAction.setText("Create a new Real World Concept");
//		addNewConceptDetailAction.setToolTipText("Create a new Real World Concept");
//		addNewConceptDetailAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
//			getImageDescriptor(ISharedImages.IMG_OBJ_ADD));
		
		manageConceptDetailAction = new Action(){
			public void run() {
			    // Instantiates and initializes the wizard
				ManageConceptWizard wizard = new ManageConceptWizard();
				wizard.init(ConceptDetailView.this.getSite().getWorkbenchWindow().getWorkbench(),
			            null);
			    // Instantiates the wizard container with the wizard and opens it
			    WizardDialog dialog = new WizardDialog(ConceptDetailView.this.getSite().getShell(), wizard);
			    dialog.create();
			    dialog.open();
			}
		};
		manageConceptDetailAction.setText("Manipulate existing Real World Concepts");
		manageConceptDetailAction.setToolTipText("Get the list of real world concepts, and change them if needed");
		manageConceptDetailAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJ_ADD));
				
		doubleClickAction = new Action() {
			public void run() {
				int selectedRow = viewer.getTable().getSelectionIndex();
				String selectedAttribute = viewer.getTable().getItem(selectedRow).getText(0);
				IWorkbenchPage page = getViewSite().getPage();
			}
		};
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}
	
	private void showMessage(String message) {
		MessageDialog.openInformation(
			viewer.getControl().getShell(),
			"Concept Detail View",
			message);
	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	public void showConceptDetail(String currentConcept, IFile fileInput){
		Object location = CMModelUtil.readPropertyFromConfigFile(fileInput.getProject().getName());
		System.out.println("ProjectProperty.CMReferenceLocation is here: "+location);
		if(location!=null){
			String filePath = location.toString() + CMModelUtil.PathSeparator + currentConcept+"."+CMModelUtil.RealWorldConcept_FileExtension;
			if(filePath!=null && new File(filePath).exists()){
				ConceptDetail newConceptDetail = ConceptDetail.readInConceptDetails(filePath);
				this.showConceptDetail(newConceptDetail);
			}
		}else{
			showMessage("Please setup the location for CM reference document first");
		}
		/*
		IFile conceptDetailFile = container.getFile(filePath);
		ObjectInputStream ois = new ObjectInputStream(conceptDetailFile.getContents());
		ConceptDetail newConceptDetail = (ConceptDetail)ois.readObject();
		conceptDetailView.getDefinitonContentText().setText(newConceptDetail.getDefinition());
		conceptDetailView.setCurrentConcept(newConceptDetail.getConceptName());
		conceptDetailView.currentProject = container;
		conceptDetailView.getViewer().setInput(newConceptDetail.getAttributes());
		*/
	}
	
	public void showConceptDetail(ConceptDetail conceptDetail){
		this.definitonContentText.setText(conceptDetail.getDefinition());
		this.getViewer().setInput(conceptDetail.getAttributes());
	}
	
	public void clearAllContents(){
		this.definitonContentText.setText("");
		this.getViewer().setInput(null);
	}
	
	public void setSelectionToAttribute(String attributeName){
		TableItem[] items = viewer.getTable().getItems();
		for(int i=0;i<items.length;i++){
			System.out.println("attributeName here "+items[i].getText());
			if(items[i].getText(0).equals(attributeName)){
				
				viewer.getTable().setSelection(items[i]);
				items[i].setBackground(viewer.getTable().getDisplay().getSystemColor(SWT.COLOR_GREEN));
				items[i].setForeground(viewer.getTable().getDisplay().getSystemColor(SWT.COLOR_GREEN));
//				items[i].setFont(this.selectedFont);
			}
		}
		
	}
}