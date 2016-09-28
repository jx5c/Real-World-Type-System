package cmtypechecker.views.provider;


import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import cmtypechecker.CM.CM_ApproxType;
import cmtypechecker.CM.CMType;


public class CMApproTableContentProvider implements IStructuredContentProvider {
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	}
	
	public void dispose() {
	}
	
	public Object[] getElements(Object parent) {
		if (parent instanceof CM_ApproxType){
			return ((CM_ApproxType)parent).getApproximateProperties().toArray();
		}
		return new Object[0];
	}
}
