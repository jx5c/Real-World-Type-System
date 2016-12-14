package rwtchecker.views;


import java.util.ArrayList;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.*;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.parser.IToken;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.SWT;

import rwtchecker.Activator;
import rwtchecker.util.DiagnosticMessage;


public class DiagnoseView extends ViewPart {
	
	public static String[] tableTitles = { "Error AST Node", "Error Type", "Error Message", "Context", "Permission"};
	
	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "rwtchecker.views.DiagnoseView";
	
	public static final String[] permissionStatus =  new String[]{"Operation Allowed", "Operation Denied"};

	private TableViewer errorTableViewer;
	private StyledText textControl;
//	private ExampleEditingSupport exampleEditingSupport;
	
	
	static ImageDescriptor imageD = Activator.getImageDescriptor("icons/checked.gif");
	private static final Image CHECKED = imageD.createImage();
	private static final Image UNCHECKED = Activator.getImageDescriptor("icons/unchecked.gif").createImage();
	
	class ErrorViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if(parent instanceof ArrayList){
				ArrayList<DiagnosticMessage> typeCheckingResults = (ArrayList<DiagnosticMessage>)parent;
				return typeCheckingResults.toArray();
			}
			return new Object[0];
		}
	}
	class ErrorViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			DiagnosticMessage errorMessage = (DiagnosticMessage)(obj); 
			switch (index) {
			case 0:
				return errorMessage.getJavaErrorNode().toString();
			case 1:
				return errorMessage.getMessageType();
			case 2:
				return errorMessage.getMessageDetail();
			case 3:
				return errorMessage.getContextInfo();
			case 4:
				if(errorMessage.isPermitted()){
					return permissionStatus[0];
				}else{
					return permissionStatus[1];
				}
			default:
				throw new RuntimeException("Should not happen");
			}
		}
		public Image getColumnImage(Object obj, int index) {
			switch (index) {
			case 0:
				return null;
			case 1:
				return null;
			case 2:
				return null;
			case 3:
				return null;
			case 4:
				if (((DiagnosticMessage) obj).isPermitted()) {
					return CHECKED;
				} else {
					return UNCHECKED;
				}
			default:
				throw new RuntimeException("Should not happen");
			}
		}
		public Image getImage(Object obj) {
			return null;
		}
	}
	public DiagnoseView() {
	}

	public void createPartControl(Composite parent) {
		int style =  SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL |SWT.FULL_SELECTION;
	    
		errorTableViewer = new TableViewer(parent, style);
		createErrorTableColumns(errorTableViewer);
		errorTableViewer.setContentProvider(new ErrorViewContentProvider());
		
		final Color redColor = this.getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_RED);
		
		errorTableViewer.setLabelProvider(new ErrorViewLabelProvider());
		errorTableViewer.setColumnProperties(tableTitles);
	
		errorTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = errorTableViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				DiagnosticMessage errorMessage = (DiagnosticMessage)(obj);
				if(errorMessage != null){
					//if this is a error message for Java error checking
					ASTNode thisNode = errorMessage.getJavaErrorNode();
					if (thisNode != null) {
						if(textControl != null){
							textControl.setSelectionRange(thisNode.getStartPosition(),thisNode.getLength());
							textControl.setTopIndex(textControl.getLineAtOffset(thisNode.getStartPosition()));
							StyleRange redRange = createRange(thisNode.getStartPosition(), thisNode.getLength(), redColor);
							textControl.setStyleRange(redRange);
						}
					}//if this is a error message for C error checking
					else if(errorMessage.getcErrorNode()!=null){
						IASTNode cASTNode = errorMessage.getcErrorNode();
						if(textControl != null){
							IToken token;
							try {
								token = cASTNode.getLeadingSyntax();
								textControl.setSelectionRange(token.getOffset(),token.getLength());
								textControl.setTopIndex(textControl.getLineAtOffset(token.getOffset()));
								StyleRange redRange = createRange(token.getOffset(), token.getLength(), redColor);
								textControl.setStyleRange(redRange);
							} catch (UnsupportedOperationException e) {
								e.printStackTrace();
							} catch (ExpansionOverlapsBoundaryException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		});
		
		errorTableViewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent arg0) {
				ISelection selection = errorTableViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				DiagnosticMessage errorMessage = (DiagnosticMessage)(obj);
				if(errorMessage != null){
					MessageBox m = new MessageBox(errorTableViewer.getTable().getShell());
					m.setMessage(errorMessage.getMessageDetail());
					m.open();
				}
			}
		});

	}
	
	private void createErrorTableColumns(final TableViewer viewer) {
		Table table = viewer.getTable();
		String[] titles = tableTitles;
		int[] bounds = { 150, 100, 300, 100, 200};
		for (int i = 0; i < titles.length; i++) {
			final TableViewerColumn viewerColumn = new TableViewerColumn(
					viewer, SWT.NONE);
			final TableColumn column = viewerColumn.getColumn();
			column.setText(titles[i]);
			column.setWidth(bounds[i]);
			column.setResizable(true);
			column.setMoveable(true);

			
			if(i == 4){
				viewerColumn.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						return null;
					}

					@Override
					public Image getImage(Object element) {
						if (((DiagnosticMessage) element).isPermitted()) {
							return CHECKED;
						} else {
							return UNCHECKED;
						}
					}
				});
//				exampleEditingSupport = new ExampleEditingSupport(viewer);
//				viewerColumn.setEditingSupport(exampleEditingSupport);
			}
			if(i == 0){
				column.addSelectionListener(new SelectionAdapter(){
		            boolean asc = true;
		            public void widgetSelected(SelectionEvent e){
		            	viewer.setSorter(asc?SorterInDiagnosticView.ErrorNode_ASC:SorterInDiagnosticView.ErrorNode_DESC);
		                asc = !asc;
		            }
		        });
			}
			if(i == 2){
				column.addSelectionListener(new SelectionAdapter(){
		            boolean asc = true;
		            public void widgetSelected(SelectionEvent e){
		            	viewer.setSorter(asc?SorterInDiagnosticView.ErrorMessage_ASC:SorterInDiagnosticView.ErrorMessage_DESC);
		                asc = !asc;
		            }
		        });
			}
		}
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}

//	static private StyleRange createRange(int start,int length,Color color) {
//		StyleRange styleRange = new StyleRange();
//		styleRange.start = start;
//		styleRange.length = length; 
//		styleRange.fontStyle = SWT.BOLD;
//		styleRange.foreground = color;
//		return styleRange;
//	}
	
	@Override
	public void setFocus() {
		errorTableViewer.getControl().setFocus();
	}

	public TableViewer getErrorTableViewer() {
		return errorTableViewer;
	}

	public StyledText getTextControl() {
		return textControl;
	}

	public void setTextControl(StyledText textControl) {
		this.textControl = textControl;
	}
	
	static private StyleRange createRange(int start,int length,Color color) {
		StyleRange styleRange = new StyleRange();
		styleRange.start = start;
		styleRange.length = length; 
		styleRange.fontStyle = SWT.BOLD;
		styleRange.foreground = color;
		return styleRange;
	}
	
}