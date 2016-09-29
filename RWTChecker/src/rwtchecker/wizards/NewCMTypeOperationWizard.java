package rwtchecker.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import java.io.*;

import org.eclipse.ui.*;

import rwtchecker.views.RWTRulesView;
import rwtchecker.views.RWTView;
import rwtchecker.views.provider.TreeObject;
import rwtchecker.realworldmodel.ConceptAttribute;
import rwtchecker.realworldmodel.ConceptDetail;
import rwtchecker.util.ActivePart;
import rwtchecker.util.CMModelUtil;
import rwtchecker.views.ConceptDetailView;


public class NewCMTypeOperationWizard extends Wizard implements INewWizard {
	private NewCMTypeOperationPage page1;
	private IFile editingFile ;

	public NewCMTypeOperationWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	public void addPages() {
		page1 = new NewCMTypeOperationPage();
		addPage(page1);
	}

	public boolean performFinish(){
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				monitor.worked(1);
				monitor.setTaskName("Opening file for editing...");
				getShell().getDisplay().asyncExec(new Runnable() {
					public void run() {
						createCMTypeOperationRule();
					}
				});
				monitor.done();
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}
	
	private void createCMTypeOperationRule(){
//		File CMTypeOperationRuleFile = ProjectProperty.getCMTypeOperationRuleFile(page1.getCurrentIProject());
//		CMTypeOperationManager typeOperationManager = new CMTypeOperationManager(CMTypeOperationRuleFile);
//		CMTypeOperation newOp = typeOperationManager.new CMTypeOperation();
//		newOp.setCMTypeOneName(page1.getOperandTypeOne());
//		newOp.setCMTypeTwoName(page1.getOperandTypeTwo());
//		newOp.setReturnCMTypeName(page1.getReturnType());
//		newOp.setOperationName(page1.getOperation());
//		typeOperationManager.addCMTypeOperation(newOp);
//		typeOperationManager.storeOperationRules();
//		
//		CMTypeOperationView cmTypeOperationView = null;
//		cmTypeOperationView = (CMTypeOperationView)ActivePart.getSpecificView(CMTypeOperationView.ID);
//		cmTypeOperationView.getTableViewer().setInput(typeOperationManager);
	}

	@Override
	public void init(IWorkbench arg0, IStructuredSelection arg1) {
	}
	
	
	
}