package rwtchecker.views.provider;


import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import rwtchecker.CM.CMType;
import rwtchecker.CM.CM_SemanticType;
import rwtchecker.CMRules.CMTypeRule;

public class CMAttributeTableContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		
		public void dispose() {
		}
		
		public Object[] getElements(Object parent) {
			CM_SemanticType cmsmtype = (CM_SemanticType) parent;
			return cmsmtype.getSemanticTypeAttributes().toArray();
		}
}
