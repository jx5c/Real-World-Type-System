package rwtchecker.util;

import org.eclipse.jdt.ui.text.java.hover.IJavaEditorTextHover;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorPart;

public class RWTHover implements IJavaEditorTextHover{

	public static String rwtTypeInfo = "";
	public static String currentSelection = ""; 
	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		// TODO Auto-generated method stub
        String varName = null;
        try {
            varName = textViewer.getDocument().get(hoverRegion.getOffset(), hoverRegion.getLength());
            if(rwtTypeInfo.length() > 0 && currentSelection.equals(varName)){
            	return rwtTypeInfo;
            }
        } catch (BadLocationException e) {
           return null;
        }
        return null;
	}

	@Override
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setEditor(IEditorPart arg0) {
		// TODO Auto-generated method stub
		
	}

	
}
