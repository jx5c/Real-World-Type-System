package rwtchecker.util;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.ui.text.java.hover.IJavaEditorTextHover;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorPart;

import rwtchecker.annotation.FileAnnotations;
import rwtchecker.rwt.RWT_Attribute;
import rwtchecker.rwt.RWType;

public class RWTHover implements IJavaEditorTextHover{

	public static RWType rwtTypeInfo;
	public static String currentSelection = ""; 
	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		// TODO Auto-generated method stub
        String varName = null;
        try {
            varName = textViewer.getDocument().get(hoverRegion.getOffset(), hoverRegion.getLength());           
            if(rwtTypeInfo != null && currentSelection.equals(varName)){
            	String result = "<p><b>Real-World Type Information: </b></p>"
            			+"<p></p>"
            			+"<p></p>"
            			+"<p><b><li indent=\"40px\">" + rwtTypeInfo.getTypeName().toUpperCase() + "</li></b></p>"
            			+"<p></p>"
            			+ "<p>real-world semantics</p>";
            	
            	ArrayList<RWT_Attribute> semantics = rwtTypeInfo.getSemanticType().getSemanticTypeAttributes();
            	for(int i =1;i<=semantics.size();i++){
            		result += 
                			"<p>"
            				+ i + ". " + semantics.get(i-1).getAttributeName()
            				+ "</p>";
            	}
            	return result;
            			
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
