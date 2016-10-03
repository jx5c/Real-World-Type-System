package rwtchecker;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.statushandlers.AbstractStatusHandler;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager;
 
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {
 
	private static final String PERSPECTIVE_ID = "RWTChecker";
 
//	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
//			IWorkbenchWindowConfigurer configurer) {
//		return new ApplicationWorkbenchWindowAdvisor(configurer);
//	}
 
	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}
	
	MyStatusHandler myStatusHandler = null;
	
	@Override
	public synchronized AbstractStatusHandler getWorkbenchErrorHandler() {
	    if (myStatusHandler == null) {
	        myStatusHandler = new MyStatusHandler();
	    }
	    return myStatusHandler;
	}
 
}

class MyStatusHandler extends AbstractStatusHandler{

	@Override
	public void handle(StatusAdapter statusAdapter, int style) {
	    if(statusAdapter.getStatus().matches(IStatus.ERROR) && ((style != StatusManager.NONE)))
	    {
	            PlatformUI.getWorkbench().close();
	    }
	}
	
}