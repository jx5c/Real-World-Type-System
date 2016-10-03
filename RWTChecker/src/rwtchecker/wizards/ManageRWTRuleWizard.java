package rwtchecker.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.MessageDialog;
import java.io.*;

import rwtchecker.rwt.RWType;
import rwtchecker.rwtrules.RWTypeRule;
import rwtchecker.rwtrules.RWTypeRuleCategory;
import rwtchecker.rwtrules.RWTypeRulesManager;
import rwtchecker.util.ActivePart;
import rwtchecker.util.RWTSystemUtil;
import rwtchecker.views.RWTRulesView;


public class ManageRWTRuleWizard extends Wizard implements INewWizard {
	private ManageRWTOperationPage page1;

	public ManageRWTRuleWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	public void addPages() {
		page1 = new ManageRWTOperationPage();
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
		File CMTypeOperationRuleFile = RWTSystemUtil.getRWTypeRulesFiles(page1.getCurrentIProject());
		RWTypeRulesManager typeOperationManager = new RWTypeRulesManager(CMTypeOperationRuleFile);
		
		RWTypeRule newOp = new RWTypeRule();
		newOp.setOperationName(page1.getOperation());
		newOp.setCMTypeOneName(page1.getOperandTypeOne());
		newOp.setCMTypeTwoName(page1.getOperandTypeTwo());
		newOp.setReturnCMTypeName(page1.getReturnType());
		//rules created by users are verified by default.
		newOp.setTypeRuleCategory(RWTypeRule.Verified);
		typeOperationManager.addCMTypeOperation(newOp);
		
		/**do not automatically generate relevant rules for now**/
//		typeOperationManager.addCMTypeOperations(this.getGeneratedOperation(newOp, typeOperationManager));
		typeOperationManager.storeRules();
		
		RWTRulesView cmTypeOperationView = null;
		cmTypeOperationView = (RWTRulesView)ActivePart.getSpecificView(RWTRulesView.ID);
		RWTypeRulesManager manager = RWTypeRulesManager.getManagerForCurrentProject();
		cmTypeOperationView.getTableViewer().setInput(manager);
		cmTypeOperationView.getTableViewer().refresh();
	}
	
	private ArrayList<RWTypeRule> getGeneratedOperation(RWTypeRule thisOp, RWTypeRulesManager typeOperationManager){
		ArrayList<RWTypeRule> generateOps = new ArrayList<RWTypeRule>(); 
		if(thisOp.getOperationName().equals(RWTypeRuleCategory.Plus)){
			RWTypeRule newOp = new RWTypeRule();
			newOp.setOperationName(RWTypeRuleCategory.Plus);
			newOp.setCMTypeOneName(thisOp.getCMTypeTwoName());
			newOp.setCMTypeTwoName(thisOp.getCMTypeOneName());
			newOp.setReturnCMTypeName(thisOp.getReturnCMTypeName());
			generateOps.add(newOp);
			
			newOp = new RWTypeRule();
			newOp.setOperationName(RWTypeRuleCategory.Subtraction);
			newOp.setCMTypeOneName(thisOp.getReturnCMTypeName());
			newOp.setCMTypeTwoName(thisOp.getCMTypeOneName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeTwoName());
			generateOps.add(newOp);
			
			newOp = new RWTypeRule();
			newOp.setOperationName(RWTypeRuleCategory.Subtraction);
			newOp.setCMTypeOneName(thisOp.getReturnCMTypeName());
			newOp.setCMTypeTwoName(thisOp.getCMTypeTwoName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeOneName());
			generateOps.add(newOp);
		}
		else if(thisOp.getOperationName().equals(RWTypeRuleCategory.Subtraction)){
			RWTypeRule newOp = new RWTypeRule();
			
			newOp.setOperationName(RWTypeRuleCategory.Subtraction);
			newOp.setCMTypeOneName(thisOp.getCMTypeOneName());
			newOp.setCMTypeTwoName(thisOp.getReturnCMTypeName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeTwoName());
			generateOps.add(newOp);
			
			newOp = new RWTypeRule();
			newOp.setOperationName(RWTypeRuleCategory.Plus);
			newOp.setCMTypeOneName(thisOp.getCMTypeTwoName());
			newOp.setCMTypeTwoName(thisOp.getReturnCMTypeName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeOneName());
			generateOps.add(newOp);
			
			newOp = new RWTypeRule();
			newOp.setOperationName(RWTypeRuleCategory.Plus);
			newOp.setCMTypeOneName(thisOp.getReturnCMTypeName());
			newOp.setCMTypeTwoName(thisOp.getCMTypeTwoName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeOneName());
			generateOps.add(newOp);
		}
		
		else if(thisOp.getOperationName().equals(RWTypeRuleCategory.Multiplication)){
			RWTypeRule newOp = new RWTypeRule();
			
			if(thisOp.getCMTypeOneName().equals(thisOp.getCMTypeTwoName())){
				newOp.setOperationName(RWTypeRuleCategory.Power);
				newOp.setCMTypeOneName(thisOp.getCMTypeOneName());
				newOp.setCMTypeTwoName("2");
				newOp = new RWTypeRule();
			}
			
			newOp.setOperationName(RWTypeRuleCategory.Multiplication);
			newOp.setCMTypeOneName(thisOp.getCMTypeTwoName());
			newOp.setCMTypeTwoName(thisOp.getCMTypeOneName());
			newOp.setReturnCMTypeName(thisOp.getReturnCMTypeName());
			
			generateOps.add(newOp);
			
			newOp = new RWTypeRule();
			newOp.setOperationName(RWTypeRuleCategory.Division);
			newOp.setCMTypeOneName(thisOp.getReturnCMTypeName());
			newOp.setCMTypeTwoName(thisOp.getCMTypeOneName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeTwoName());
			
			generateOps.add(newOp);
			
			newOp = new RWTypeRule();
			newOp.setOperationName(RWTypeRuleCategory.Division);
			newOp.setCMTypeOneName(thisOp.getReturnCMTypeName());
			newOp.setCMTypeTwoName(thisOp.getCMTypeTwoName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeOneName());
			
			generateOps.add(newOp);
		}
		
		else if(thisOp.getOperationName().equals(RWTypeRuleCategory.Division)){
			RWTypeRule newOp = new RWTypeRule();
			
			newOp.setOperationName(RWTypeRuleCategory.Division);
			newOp.setCMTypeOneName(thisOp.getCMTypeOneName());
			newOp.setCMTypeTwoName(thisOp.getReturnCMTypeName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeTwoName());
			generateOps.add(newOp);
			
			newOp = new RWTypeRule();
			newOp.setOperationName(RWTypeRuleCategory.Multiplication);
			newOp.setCMTypeOneName(thisOp.getReturnCMTypeName());
			newOp.setCMTypeTwoName(thisOp.getCMTypeTwoName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeOneName());
			generateOps.add(newOp);
			
			newOp = new RWTypeRule();
			newOp.setOperationName(RWTypeRuleCategory.Division);
			newOp.setCMTypeOneName(thisOp.getCMTypeTwoName());
			newOp.setCMTypeTwoName(thisOp.getReturnCMTypeName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeOneName());
			generateOps.add(newOp);
		}
		
		else if(thisOp.getOperationName().equals(RWTypeRuleCategory.DegreeToRadians)){
			RWTypeRule newOp = new RWTypeRule();
			
			newOp.setOperationName(RWTypeRuleCategory.RadiansToDegree);
			newOp.setCMTypeOneName(thisOp.getReturnCMTypeName());
			newOp.setCMTypeTwoName("");
			newOp.setReturnCMTypeName(thisOp.getCMTypeOneName());
			generateOps.add(newOp);
		}

		else if(thisOp.getOperationName().equals(RWTypeRuleCategory.RadiansToDegree)){
			RWTypeRule newOp = new RWTypeRule();
			
			newOp.setOperationName(RWTypeRuleCategory.DegreeToRadians);
			newOp.setCMTypeOneName(thisOp.getReturnCMTypeName());
			newOp.setCMTypeTwoName("");
			newOp.setReturnCMTypeName(thisOp.getCMTypeOneName());
			generateOps.add(newOp);
		}
		else if(thisOp.getOperationName().equals(RWTypeRuleCategory.Sine)){
			RWTypeRule newOp = new RWTypeRule();
			
			newOp.setOperationName(RWTypeRuleCategory.Cosine);
			newOp.setCMTypeOneName(thisOp.getCMTypeOneName());
			newOp.setCMTypeTwoName("");
			newOp.setReturnCMTypeName(thisOp.getReturnCMTypeName());
			generateOps.add(newOp);
		}
		else if(thisOp.getOperationName().equals(RWTypeRuleCategory.Cosine)){
			RWTypeRule newOp = new RWTypeRule();
			
			newOp.setOperationName(RWTypeRuleCategory.Sine);
			newOp.setCMTypeOneName(thisOp.getCMTypeOneName());
			newOp.setCMTypeTwoName("");
			newOp.setReturnCMTypeName(thisOp.getReturnCMTypeName());
			generateOps.add(newOp);
		}
		return generateOps;
	}

	@Override
	public void init(IWorkbench arg0, IStructuredSelection arg1) {
	}
	
	
	
}