package rwtchecker.views.provider;


import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import rwtchecker.rwt.RWType;
import rwtchecker.rwtrules.RWTypeRule;
import rwtchecker.rwt.RWT_Semantic;

public class CMAttributeTableContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		
		public void dispose() {
		}
		
		public Object[] getElements(Object parent) {
			RWT_Semantic cmsmtype = (RWT_Semantic) parent;
			return cmsmtype.getSemanticTypeAttributes().toArray();
		}
}
