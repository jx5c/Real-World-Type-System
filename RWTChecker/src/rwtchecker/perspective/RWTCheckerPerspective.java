package rwtchecker.perspective;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import rwtchecker.views.ConceptDetailView;
import rwtchecker.views.DiagnoseView;

public class RWTCheckerPerspective implements IPerspectiveFactory {

	public static String ID = "RWTChecker.PerspectiveID";
	
	@Override
	public void createInitialLayout(IPageLayout myLayout) {
        IFolderLayout left = myLayout.createFolder("left", IPageLayout.LEFT, (float) 0.26, myLayout.getEditorArea());
	    left.addView(IPageLayout.ID_PROJECT_EXPLORER);
	    left.addView(IPageLayout.ID_OUTLINE);
	    
	    IFolderLayout rightArea = myLayout.createFolder("right", IPageLayout.RIGHT, (float) 0.75, myLayout.getEditorArea());
	    rightArea.addView(rwtchecker.views.RWTView.ID);
	    rightArea.addView(rwtchecker.views.RWTRulesView.ID);
	    
	    IFolderLayout bottomArea = myLayout.createFolder("bottom", IPageLayout.BOTTOM, (float) 0.70, myLayout.getEditorArea());
	    bottomArea.addView(rwtchecker.views.ConceptDetailView.ID);
	    bottomArea.addView(DiagnoseView.ID);
	}

}
