package rwtchecker.views.provider;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import rwtchecker.CM.CorrespondenceApproTypeProperty;


public class CMApproTablelLabelProvider extends LabelProvider implements ITableLabelProvider {
	
	public String getColumnText(Object obj, int index) {
		CorrespondenceApproTypeProperty approximateProperty = (CorrespondenceApproTypeProperty) obj;
		switch (index) {
		case 0:
			return approximateProperty.getProperty_name();
		case 1:
			return approximateProperty.getPossibleValue();
		case 2:
			return approximateProperty.getDescription();
		default:
			throw new RuntimeException("Should not happen");
		}
	}
	
	public Image getColumnImage(Object obj, int index) {
		return getImage(obj);
	}
	public Image getImage(Object obj) {
		return PlatformUI.getWorkbench().
				getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
	}
}