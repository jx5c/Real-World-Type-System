package rwtchecker.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.core.runtime.CoreException;

import rwtchecker.rwt.RWType;
import rwtchecker.util.ActivePart;
import rwtchecker.util.RWTSystemUtil;
import rwtchecker.views.RWTView;
import rwtchecker.wizards.pages.NewRWTJavaProjectWizardPage1;
import rwtchecker.wizards.pages.NewRWTJavaProjectWizardPage2;


public class NewRWTJavaProjectWizard extends Wizard implements INewWizard {
	protected NewRWTJavaProjectWizardPage1 one;
    protected NewRWTJavaProjectWizardPage2 two;

    public NewRWTJavaProjectWizard() {
            super();
            setNeedsProgressMonitor(true);
    }

    @Override
    public String getWindowTitle() {
            return "Export My Data";
    }

    @Override
    public void addPages() {
            one = new NewRWTJavaProjectWizardPage1();
            two = new NewRWTJavaProjectWizardPage2();
            addPage(one);
            addPage(two);
    }

    @Override
    public boolean performFinish() {
            // Print the result to the console
            System.out.println(one.getText1());
            System.out.println(two.getText1());

            return true;
    }

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// TODO Auto-generated method stub
		
	}

}