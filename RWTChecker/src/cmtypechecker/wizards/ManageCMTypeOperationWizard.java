package cmtypechecker.wizards;

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


import cmtypechecker.CM.CMType;
import cmtypechecker.CMRules.CMTypeRule;
import cmtypechecker.CMRules.CMTypeRuleCategory;
import cmtypechecker.CMRules.CMTypeRulesManager;
import cmtypechecker.util.ActivePart;
import cmtypechecker.util.CMModelUtil;
import rwtchecker.views.RWTRulesView;


public class ManageCMTypeOperationWizard extends Wizard implements INewWizard {
	private ManageCMTypeOperationPage page1;

	public ManageCMTypeOperationWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	public void addPages() {
		page1 = new ManageCMTypeOperationPage();
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
		File CMTypeOperationRuleFile = CMModelUtil.getRWTypeRulesFiles(page1.getCurrentIProject());
		CMTypeRulesManager typeOperationManager = new CMTypeRulesManager(CMTypeOperationRuleFile);
		
		CMTypeRule newOp = new CMTypeRule();
		newOp.setOperationName(page1.getOperation());
		newOp.setCMTypeOneName(page1.getOperandTypeOne());
		newOp.setCMTypeTwoName(page1.getOperandTypeTwo());
		newOp.setReturnCMTypeName(page1.getReturnType());
		//rules created by users are verified by default.
		newOp.setTypeRuleCategory(CMTypeRule.Verified);
		typeOperationManager.addCMTypeOperation(newOp);
		
		/**do not automatically generate relevant rules for now**/
//		typeOperationManager.addCMTypeOperations(this.getGeneratedOperation(newOp, typeOperationManager));
		typeOperationManager.storeRules();
		
		RWTRulesView cmTypeOperationView = null;
		cmTypeOperationView = (RWTRulesView)ActivePart.getSpecificView(RWTRulesView.ID);
		CMTypeRulesManager manager = CMTypeRulesManager.getManagerForCurrentProject();
		cmTypeOperationView.getTableViewer().setInput(manager);
		cmTypeOperationView.getTableViewer().refresh();
	}
	
	private ArrayList<CMTypeRule> getGeneratedOperation(CMTypeRule thisOp, CMTypeRulesManager typeOperationManager){
		ArrayList<CMTypeRule> generateOps = new ArrayList<CMTypeRule>(); 
		if(thisOp.getOperationName().equals(CMTypeRuleCategory.Plus)){
			CMTypeRule newOp = new CMTypeRule();
			newOp.setOperationName(CMTypeRuleCategory.Plus);
			newOp.setCMTypeOneName(thisOp.getCMTypeTwoName());
			newOp.setCMTypeTwoName(thisOp.getCMTypeOneName());
			newOp.setReturnCMTypeName(thisOp.getReturnCMTypeName());
			generateOps.add(newOp);
			
			newOp = new CMTypeRule();
			newOp.setOperationName(CMTypeRuleCategory.Subtraction);
			newOp.setCMTypeOneName(thisOp.getReturnCMTypeName());
			newOp.setCMTypeTwoName(thisOp.getCMTypeOneName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeTwoName());
			generateOps.add(newOp);
			
			newOp = new CMTypeRule();
			newOp.setOperationName(CMTypeRuleCategory.Subtraction);
			newOp.setCMTypeOneName(thisOp.getReturnCMTypeName());
			newOp.setCMTypeTwoName(thisOp.getCMTypeTwoName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeOneName());
			generateOps.add(newOp);
		}
		else if(thisOp.getOperationName().equals(CMTypeRuleCategory.Subtraction)){
			CMTypeRule newOp = new CMTypeRule();
			
			newOp.setOperationName(CMTypeRuleCategory.Subtraction);
			newOp.setCMTypeOneName(thisOp.getCMTypeOneName());
			newOp.setCMTypeTwoName(thisOp.getReturnCMTypeName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeTwoName());
			generateOps.add(newOp);
			
			newOp = new CMTypeRule();
			newOp.setOperationName(CMTypeRuleCategory.Plus);
			newOp.setCMTypeOneName(thisOp.getCMTypeTwoName());
			newOp.setCMTypeTwoName(thisOp.getReturnCMTypeName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeOneName());
			generateOps.add(newOp);
			
			newOp = new CMTypeRule();
			newOp.setOperationName(CMTypeRuleCategory.Plus);
			newOp.setCMTypeOneName(thisOp.getReturnCMTypeName());
			newOp.setCMTypeTwoName(thisOp.getCMTypeTwoName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeOneName());
			generateOps.add(newOp);
		}
		
		else if(thisOp.getOperationName().equals(CMTypeRuleCategory.Multiplication)){
			CMTypeRule newOp = new CMTypeRule();
			
			if(thisOp.getCMTypeOneName().equals(thisOp.getCMTypeTwoName())){
				newOp.setOperationName(CMTypeRuleCategory.Power);
				newOp.setCMTypeOneName(thisOp.getCMTypeOneName());
				newOp.setCMTypeTwoName("2");
				newOp = new CMTypeRule();
			}
			
			newOp.setOperationName(CMTypeRuleCategory.Multiplication);
			newOp.setCMTypeOneName(thisOp.getCMTypeTwoName());
			newOp.setCMTypeTwoName(thisOp.getCMTypeOneName());
			newOp.setReturnCMTypeName(thisOp.getReturnCMTypeName());
			
			generateOps.add(newOp);
			
			newOp = new CMTypeRule();
			newOp.setOperationName(CMTypeRuleCategory.Division);
			newOp.setCMTypeOneName(thisOp.getReturnCMTypeName());
			newOp.setCMTypeTwoName(thisOp.getCMTypeOneName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeTwoName());
			
			generateOps.add(newOp);
			
			newOp = new CMTypeRule();
			newOp.setOperationName(CMTypeRuleCategory.Division);
			newOp.setCMTypeOneName(thisOp.getReturnCMTypeName());
			newOp.setCMTypeTwoName(thisOp.getCMTypeTwoName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeOneName());
			
			generateOps.add(newOp);
		}
		
		else if(thisOp.getOperationName().equals(CMTypeRuleCategory.Division)){
			CMTypeRule newOp = new CMTypeRule();
			
			newOp.setOperationName(CMTypeRuleCategory.Division);
			newOp.setCMTypeOneName(thisOp.getCMTypeOneName());
			newOp.setCMTypeTwoName(thisOp.getReturnCMTypeName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeTwoName());
			generateOps.add(newOp);
			
			newOp = new CMTypeRule();
			newOp.setOperationName(CMTypeRuleCategory.Multiplication);
			newOp.setCMTypeOneName(thisOp.getReturnCMTypeName());
			newOp.setCMTypeTwoName(thisOp.getCMTypeTwoName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeOneName());
			generateOps.add(newOp);
			
			newOp = new CMTypeRule();
			newOp.setOperationName(CMTypeRuleCategory.Division);
			newOp.setCMTypeOneName(thisOp.getCMTypeTwoName());
			newOp.setCMTypeTwoName(thisOp.getReturnCMTypeName());
			newOp.setReturnCMTypeName(thisOp.getCMTypeOneName());
			generateOps.add(newOp);
		}
		
		else if(thisOp.getOperationName().equals(CMTypeRuleCategory.DegreeToRadians)){
			CMTypeRule newOp = new CMTypeRule();
			
			newOp.setOperationName(CMTypeRuleCategory.RadiansToDegree);
			newOp.setCMTypeOneName(thisOp.getReturnCMTypeName());
			newOp.setCMTypeTwoName("");
			newOp.setReturnCMTypeName(thisOp.getCMTypeOneName());
			generateOps.add(newOp);
		}

		else if(thisOp.getOperationName().equals(CMTypeRuleCategory.RadiansToDegree)){
			CMTypeRule newOp = new CMTypeRule();
			
			newOp.setOperationName(CMTypeRuleCategory.DegreeToRadians);
			newOp.setCMTypeOneName(thisOp.getReturnCMTypeName());
			newOp.setCMTypeTwoName("");
			newOp.setReturnCMTypeName(thisOp.getCMTypeOneName());
			generateOps.add(newOp);
		}
		else if(thisOp.getOperationName().equals(CMTypeRuleCategory.Sine)){
			CMTypeRule newOp = new CMTypeRule();
			
			newOp.setOperationName(CMTypeRuleCategory.Cosine);
			newOp.setCMTypeOneName(thisOp.getCMTypeOneName());
			newOp.setCMTypeTwoName("");
			newOp.setReturnCMTypeName(thisOp.getReturnCMTypeName());
			generateOps.add(newOp);
		}
		else if(thisOp.getOperationName().equals(CMTypeRuleCategory.Cosine)){
			CMTypeRule newOp = new CMTypeRule();
			
			newOp.setOperationName(CMTypeRuleCategory.Sine);
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