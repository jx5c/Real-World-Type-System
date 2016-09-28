package cmtypechecker.views.provider;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import cmtypechecker.realworldmodel.ConceptAttribute;


public class ConceptDetailContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		
		public void dispose() {
		}
		
		public Object[] getElements(Object parent) {
			List<ConceptAttribute> conceptAttributes = (List<ConceptAttribute>) parent;
			return conceptAttributes.toArray();
		}
}
