package cmtypechecker.views.provider;


import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import cmtypechecker.CM.CMType;
import cmtypechecker.CM.CM_SemanticType;
import cmtypechecker.CMRules.CMTypeRule;

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
