package rwtchecker.views.provider;


import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import rwtchecker.rwt.RWType;
import rwtchecker.rwt.RWT_Approx;


public class CMApproTableContentProvider implements IStructuredContentProvider {
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	}
	
	public void dispose() {
	}
	
	public Object[] getElements(Object parent) {
		if (parent instanceof RWT_Approx){
			return ((RWT_Approx)parent).getApproximateProperties().toArray();
		}
		return new Object[0];
	}
}
