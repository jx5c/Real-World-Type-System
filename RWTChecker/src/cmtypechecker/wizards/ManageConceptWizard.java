package cmtypechecker.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import java.io.*;

import org.eclipse.ui.*;

import cmtypechecker.realworldmodel.ConceptAttribute;
import cmtypechecker.realworldmodel.ConceptDetail;
import cmtypechecker.util.CMModelUtil;
import cmtypechecker.views.provider.TreeObject;

/**
 * This is a sample new wizard. Its role is to create a new file 
 * resource in the provided container. If the container resource
 * (a folder or a project) is selected in the workspace 
 * when the wizard is opened, it will accept it as the target
 * container. The wizard creates one file with the extension
 * "att". If a sample multi-page editor (also available
 * as a template) is registered for the same extension, it will
 * be able to open it.
 */

public class ManageConceptWizard extends Wizard implements INewWizard {
	private ManageConceptWizardPage page1;
	/**
	 * Constructor for NewConceptWizard.
	 */
	public ManageConceptWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page1 = new ManageConceptWizardPage();
		
		addPage(page1);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doMyFinish(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
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
	
	private void doMyFinish(final IProgressMonitor monitor)
			throws CoreException {
			// create a sample file
			monitor.beginTask("Creating Concept File", 2);
			monitor.worked(1);
			monitor.setTaskName("Opening file for editing...");
			getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
//					ConceptDetail newConceptDetail = new ConceptDetail();
//					newConceptDetail.setConceptName(page2.getConceptName());
//					newConceptDetail.setDefinition(page2.getDefinition());
//					Table conceptAttributesTable = page2.getAttributeTable();
//					int itemCount = conceptAttributesTable.getItemCount();
//					for(int i=0;i<itemCount;i++){
//						ConceptAttribute attribute = new ConceptAttribute();
//						attribute.setAttributeName(conceptAttributesTable.getItem(i).getText(0));
//						attribute.setAttributeExplanation(conceptAttributesTable.getItem(i).getText(1));
//						newConceptDetail.addAttribute(attribute);
//					}
//					IProject currentProject = page1.getCurrentProject();
//					File newConceptFile = ProjectProperty.getConceptDetailFile(currentProject, page2.getConceptName());
//					ConceptDetail.writeOutConceptDetails(newConceptDetail, newConceptFile);
					
				}
			});
			monitor.done();
		}

	@Override
	public void init(IWorkbench arg0, IStructuredSelection arg1) {
	}
}