package rwtchecker.handler;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.AbstractStatusHandler;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager;

public class NoRWTSystemErrorHandler extends AbstractStatusHandler{

	@Override
	public void handle(StatusAdapter statusAdapter, int style) {
	    if(statusAdapter.getStatus().matches(IStatus.ERROR) && ((style != StatusManager.NONE)))
	    {
	            
	            PlatformUI.getWorkbench().close();
	    }
	}

}
