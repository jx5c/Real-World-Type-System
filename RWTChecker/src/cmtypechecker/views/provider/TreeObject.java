package cmtypechecker.views.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Stack;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;

import cmtypechecker.CM.CMType;
import cmtypechecker.util.ActivePart;
import cmtypechecker.util.CMModelUtil;



public class TreeObject implements IAdaptable, Serializable{

	private static final long serialVersionUID = -7452002560040464889L;
	private String name;
	private TreeObject parent;
	private ArrayList<TreeObject> children;
	private boolean isPrimaryType = false;
	
	private Object data;
	
	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public static String treeObjectTopName = "CMTYPE_List";
	public static String treeIndexFileName = "treeHierarchy";
	
	public TreeObject(){
		this.name = "";
		children = new ArrayList<TreeObject>();
	}
	
	public TreeObject(String name) {
		this.name = name;
		children = new ArrayList<TreeObject>();
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public void setParent(TreeObject parent) {
		this.parent = parent;
	}
	public TreeObject getParent() {
		return parent;
	}
	public String toString() {
		return getName();
	}
	public Object getAdapter(Class key) {
		return null;
	}
	public void addChild(TreeObject child) {
		children.add(child);
		child.setParent(this);
	}
	public void removeChild(TreeObject child) {
		children.remove(child);
		child.setParent(null);
	}
	public TreeObject [] getChildren() {
		return (TreeObject [])children.toArray(new TreeObject[children.size()]);
	}
	public boolean hasChildren() {
		return children.size()>0;
	}
	
	public static void writeOutTreeObject(TreeObject treeObject, File fileLocation){
		if(!fileLocation.exists()){
			try {
				fileLocation.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream
			(new FileOutputStream(fileLocation));
			out.writeObject(treeObject);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static TreeObject readInTreeObject(IProject iproject, File fileLocation){
		TreeObject treeObject = new TreeObject();
		ObjectInputStream in;
		if(fileLocation.exists()){
			try {
				in = new ObjectInputStream(new FileInputStream(fileLocation));
				treeObject = (TreeObject)in.readObject();
//				clearUp(iproject, treeObject);
				in.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return treeObject; 
	}
	
//	private static void clearUp(IProject currentProject, TreeObject to){
//		if(!to.name.equals(treeObjectTopName) && CMModelUtil.getCMTypeFromTreeObject(currentProject, to) == null){
//			to.getParent().removeChild(to);
//		}else{
//			for(TreeObject nextTo :to.children){
//				clearUp(currentProject, nextTo);
//			}
//		}
//	}
	
	private static TreeObject getTopLevelTreeObject(TreeObject treeObject){
		if((treeObject == null) 
				|| (treeObject.getName().equals(""))){
			return null ;
		}
		TreeObject parentTO = treeObject.getParent();
		TreeObject childTO = treeObject;
		while (!(childTO.getName().equals(treeObjectTopName) 
				&& 
				parentTO.getName().equals("") )){
			childTO = parentTO;
			parentTO = parentTO.getParent();
		}
		return parentTO;
	}
	
	public static void updateTreeObjectToFile(IProject currentProject, TreeObject cmtypeTreeSelectedObject){
		File treeIndexFile = CMModelUtil.getTreeIndexFile(currentProject);
		if(treeIndexFile!= null ){
			if(!treeIndexFile.exists()){
				try {
					treeIndexFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			TreeObject topLevelTO = TreeObject.getTopLevelTreeObject(cmtypeTreeSelectedObject);
			if(topLevelTO!= null){
				TreeObject.writeOutTreeObject(topLevelTO, treeIndexFile);
			}
		}
	}

	public boolean isPrimaryType() {
		return isPrimaryType;
	}

	public void setPrimaryType(boolean isPrimaryType) {
		this.isPrimaryType = isPrimaryType;
	}
	
	
	public static ArrayList<String> getAllSuperTypes(String currentType){
		TreeObject treeObject = CMModelUtil.readInAllCMTypesToTreeObject(ActivePart.getFileOfActiveEditror());
		TreeObject currentTO = depthSearchFirst(currentType, treeObject);
		ArrayList<String> superTypeList = new ArrayList<String>();
		if(currentTO!=null){
			TreeObject parentTO = currentTO.getParent();
			while(!parentTO.toString().equals(TreeObject.treeObjectTopName)){
				superTypeList.add(parentTO.toString());
				parentTO = parentTO.getParent();
			}
		}
		return superTypeList;
	}
	
	private static TreeObject depthSearchFirst(String currentType, TreeObject currentTO){
		TreeObject[] children = currentTO.getChildren();
		for(int i=0;i<children.length;i++){
			if(children[i].getName().equals(currentType)){
				return children[i];
			}else{
				return depthSearchFirst(currentType,children[i]);	
			}
		}
		return null;
	}
	
	public static boolean isSubTypeOf(IProject currentProject, String typeNameOne, String typeNameTwo){
		if(typeNameOne == null){
			return false;
		}
		if(typeNameTwo == null){
			return false;
		}
		if(typeNameOne.length() == 0){
			return false;
		}
		if(typeNameTwo.length() == 0){
			return false;
		}
		ArrayList<String> superTypeListOne = getAllSuperTypes(typeNameOne);
		if(superTypeListOne.contains(typeNameTwo)){
			return true;
		}else{
			return false;
		}
	}
	
}
