package rwtchecker.views.provider;


import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import rwtchecker.CM.CMType;
import rwtchecker.CM.CM_ApproxType;


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
