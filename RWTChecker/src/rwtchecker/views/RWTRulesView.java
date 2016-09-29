package rwtchecker.views;



import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;

import rwtchecker.CMRules.CMTypeRule;
import rwtchecker.CMRules.CMTypeRulesManager;
import rwtchecker.dialogs.TypeRuleDisplayDialog;
import rwtchecker.util.ActivePart;
import rwtchecker.util.CMModelUtil;
import rwtchecker.wizards.ManageCMTypeOperationWizard;
import rwtchecker.wizards.NewCMTypeOperationWizard;

public class RWTRulesView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "cmtypechecker.views.CMTypeRulesView";

	private TableViewer tableViewer;
	private Action loadCMTypeOperationsAction;
	private Action createNewCMTypeOperationAction;
	private Action delCMTypeOperationAction;
	
	private CMTypeRulesManager manager;
	
	private Action doubleClickAction;
	 
	class CMOperationViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if(parent instanceof CMTypeRulesManager){
				CMTypeRulesManager operationRules = (CMTypeRulesManager)(parent);
				return operationRules.getDefinedOperations().toArray();	
			}
			return new Object[0];
		}
	}
	class CMOperationViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			CMTypeRule operation = (CMTypeRule)obj;
			switch (index) {
			case 0:
				return operation.getOperationName();
			case 1:
				return operation.getCMTypeOneName();
			case 2:
				return operation.getCMTypeTwoName();
			case 3:
				return operation.getReturnCMTypeName();
			default:
				throw new RuntimeException("Should not happen");
			}
		}
		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}
		public Image getImage(Object obj) {
			return PlatformUI.getWorkbench().
					getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}
	class NameSorter extends ViewerSorter {
	}

	public RWTRulesView() {
	}

	public void createPartControl(Composite parent) {
		tableViewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		createAttributeTableColumns(tableViewer);
		tableViewer.setContentProvider(new CMOperationViewContentProvider());
		tableViewer.setLabelProvider(new CMOperationViewLabelProvider());
		tableViewer.setSorter(new NameSorter());
		tableViewer.setInput(null);
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}
	
	private void createAttributeTableColumns(final TableViewer viewer) {
		Table table = viewer.getTable();
		String[] titles = { "Operation", "Operand 1", "Operand 2", "Result"};
		int[] bounds = { 120, 120, 120, 120};
		for (int i = 0; i < titles.length; i++) {
			final TableViewerColumn viewerColumn = new TableViewerColumn(
					viewer, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL
					| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
			final TableColumn column = viewerColumn.getColumn();
			column.setText(titles[i]);
			column.setWidth(bounds[i]);
			column.setResizable(true);
			column.setMoveable(true);
			switch(i){
				case 0:{
					column.addSelectionListener(new SelectionAdapter(){
			            boolean asc = true;
			            public void widgetSelected(SelectionEvent e){
			            	viewer.setSorter(asc?SorterInOperationView.Operation_Name_ASC:SorterInOperationView.Operation_Name_DESC);
			                asc = !asc;
			            }
			        });
				}
				case 1:{
					column.addSelectionListener(new SelectionAdapter(){
			            boolean asc = true;
			            public void widgetSelected(SelectionEvent e){
			            	viewer.setSorter(asc?SorterInOperationView.Argument_One_ASC:SorterInOperationView.Argument_One_DESC);
			                asc = !asc;
			            }
			        });
				}
				case 2:{
					column.addSelectionListener(new SelectionAdapter(){
			            boolean asc = true;
			            public void widgetSelected(SelectionEvent e){
			            	viewer.setSorter(asc?SorterInOperationView.Argument_Two_ASC:SorterInOperationView.Argument_Two_DESC);
			                asc = !asc;
			            }
			        });
				}
				case 3:{
					column.addSelectionListener(new SelectionAdapter(){
			            boolean asc = true;
			            public void widgetSelected(SelectionEvent e){
			            	viewer.setSorter(asc?SorterInOperationView.Return_Type_ASC:SorterInOperationView.Return_Type_DESC);
			                asc = !asc;
			            }
			        });
				}
			};
		}
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				RWTRulesView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(tableViewer.getControl());
		tableViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, tableViewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(loadCMTypeOperationsAction);
		manager.add(createNewCMTypeOperationAction);
		manager.add(delCMTypeOperationAction);
		manager.add(new Separator());
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(loadCMTypeOperationsAction);
		manager.add(createNewCMTypeOperationAction);
		manager.add(delCMTypeOperationAction);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(loadCMTypeOperationsAction);
		manager.add(createNewCMTypeOperationAction);
		manager.add(delCMTypeOperationAction);
	}

	private void makeActions() {
		createNewCMTypeOperationAction = new Action() {
			public void run() {
//				NewCMTypeOperationWizard wizard = new NewCMTypeOperationWizard();
				ManageCMTypeOperationWizard wizard = new ManageCMTypeOperationWizard();
				wizard.init(RWTRulesView.this.getSite().getWorkbenchWindow().getWorkbench(), null);
			    WizardDialog dialog = new WizardDialog(RWTRulesView.this.getSite().getShell(), wizard);
			    dialog.create();
			    dialog.open();
			}
		};
		createNewCMTypeOperationAction.setText("Create a new operation/rule for correspondence types");
		createNewCMTypeOperationAction.setToolTipText("Create a new operation/rule for correspondence types");
		createNewCMTypeOperationAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJ_ADD));
		
		loadCMTypeOperationsAction = new Action() {
			public void run() {
				CMTypeRulesManager newManager = CMTypeRulesManager.getManagerForCurrentProject();
				manager = newManager; 
				tableViewer.setInput(manager);
				tableViewer.refresh();
			}
		};
		loadCMTypeOperationsAction.setText("Load CM type Operations for current active Project");
		loadCMTypeOperationsAction.setToolTipText("Load CM type Operations for current Project");
		loadCMTypeOperationsAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER));
		
		delCMTypeOperationAction = new Action() {
			public void run() {
				int selectedRow = tableViewer.getTable().getSelectionIndex();
				if(tableViewer.getTable().getItem(selectedRow) != null){
					CMTypeRule cmTypeOperation = (CMTypeRule)(tableViewer.getTable().getItem(selectedRow).getData());
					if(manager!=null){
						manager.delCMTypeOperation(cmTypeOperation);
						tableViewer.setInput(manager);
						manager.storeRules();	
					}
				}
			}
		};
		delCMTypeOperationAction.setText("delete CM types Operations for current Project");
		delCMTypeOperationAction.setToolTipText("delete CM types Operations for current Project");
		delCMTypeOperationAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		
		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = tableViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				CMTypeRule cmTypeOperation = (CMTypeRule)obj;
				
				IWorkbench workbench = PlatformUI.getWorkbench();
	    		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
	    		TypeRuleDisplayDialog dialog = new TypeRuleDisplayDialog(window.getShell(), cmTypeOperation, manager);
				dialog.create();
				dialog.open();
			}
		};
	}

	private void hookDoubleClickAction() {
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}
	private void showMessage(String message) {
		MessageDialog.openInformation(
			tableViewer.getControl().getShell(),
			"CM Type Operation View",
			message);
	}

	public void setFocus() {
		tableViewer.getControl().setFocus();
	}

	public TableViewer getTableViewer() {
		return tableViewer;
	}

	public CMTypeRulesManager getManager() {
		return manager;
	}

	public void setManager(CMTypeRulesManager manager) {
		this.manager = manager;
	}
	

	
	
}