package rwtchecker.views.provider;

import java.util.ArrayList;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.part.ViewPart;

import rwtchecker.CM.CMType;


public class CMViewTreeContentProvider implements IStructuredContentProvider, 
									   ITreeContentProvider {

	private TreeObject invisibleRoot;

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	}
	public void dispose() {
	}
	public Object[] getElements(Object parent) {
		 if (invisibleRoot == null){
			 initialize();
		 }
		 if(parent instanceof TreeObject){
			 this.invisibleRoot = (TreeObject)parent;
		 }
		return invisibleRoot.getChildren();
	}
	public Object getParent(Object child) {
		if (child instanceof TreeObject) {
			return ((TreeObject)child).getParent();
		}
		return null;
	}
	public Object [] getChildren(Object parent) {
		if (((TreeObject)parent).getChildren().length > 0) {
			return ((TreeObject)parent).getChildren();
		}
		return new Object[0];
	}
	public boolean hasChildren(Object parent) {
			return ((TreeObject)parent).hasChildren();
	}
	public void initialize() {
		TreeObject to1 = new TreeObject("Leaf 1");
		TreeObject to2 = new TreeObject("Leaf 2");
		TreeObject to3 = new TreeObject("Leaf 3");
		TreeObject p1 = new TreeObject("Parent 1");
		p1.addChild(to1);
		p1.addChild(to2);
		p1.addChild(to3);
		
		TreeObject to4 = new TreeObject("Leaf 4");
		TreeObject p2 = new TreeObject("Parent 2");
		p2.addChild(to4);
		
		TreeObject root = new TreeObject("Root");
		root.addChild(p1);
		root.addChild(p2);
		
		invisibleRoot = new TreeObject("");
		invisibleRoot.addChild(root);
	}

	public void setInvisibleRoot(TreeObject invisibleRoot) {
		this.invisibleRoot = invisibleRoot;
	}

	
//	public void initialize(CMType correspondenceType) {
//		invisibleRoot = new TreeObject("");
//		TreeObject typeNode = new TreeObject("CORRESPONDENCE TYPE");
//		invisibleRoot.addChild(typeNode);
//		TreeObject nameLabelNode = new TreeObject("NAME");
//		typeNode.addChild(nameLabelNode);
//		TreeObject typeNameNode = new TreeObject(correspondenceType.getTypeName());
//		nameLabelNode.addChild(typeNameNode);
//	}
}


