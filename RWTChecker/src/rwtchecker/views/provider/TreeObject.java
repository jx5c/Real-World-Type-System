package rwtchecker.views.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;

import rwtchecker.rwt.RWT_Attribute;
import rwtchecker.rwt.RWT_Semantic;
import rwtchecker.rwt.RWType;
import rwtchecker.util.ActivePart;
import rwtchecker.util.RWTSystemUtil;
import rwtchecker.util.XMLGeneratorForTypes;



public class TreeObject implements IAdaptable{

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

	public static String treeObjectTopName = "RWT_List";
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
	
//	private static String XMLTag_rootLevel = "invisible";
	private static String XMLTag_topLevel = "rwts";
	private static String XMLTag_childrenLevel = "rwt";
	
	
	public static void writeOutTreeObject(TreeObject treeObject, File fileLocation){
		if(fileLocation==null || !fileLocation.isFile()){
			return;
		}
		Document document = DocumentHelper.createDocument();
        Element root = document.addElement( XMLTag_topLevel );
		for(TreeObject child : treeObject.children.get(0).children){
			Element typeElement = root.addElement( XMLTag_childrenLevel );
	        typeElement.addText(child.name);	
		}

        XMLWriter writer;
		try {
//			file.delete();
//			file.createNewFile();
			writer = new XMLWriter(
			        new FileWriter(fileLocation));
            writer.write( document );
            writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static TreeObject readInTreeObject(IProject iproject, File fileLocation){
		TreeObject invisibleRoot = new TreeObject("invisible");
		TreeObject treeObject = new TreeObject(treeObjectTopName);
		if(fileLocation.exists()){
	        SAXReader reader = new SAXReader();
	        try {
				Document document = reader.read(fileLocation);
				Element root = document.getRootElement();
				for ( Iterator i = root.elementIterator(XMLTag_childrenLevel); i.hasNext(); ) {
					Element element = (Element) i.next();
					TreeObject child = new TreeObject(element.getText());
					treeObject.addChild(child);
				}				
			} catch (DocumentException e) {
				e.printStackTrace();
			}
	        invisibleRoot.addChild(treeObject);
		}else{
			//rebuild the tree object for rwt type list
			for(File dir : fileLocation.getParentFile().listFiles()){
				if(dir.isDirectory()){
					TreeObject child = new TreeObject(dir.getName());
					treeObject.addChild(child);
				}
			}
			try {
				fileLocation.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			invisibleRoot.addChild(treeObject);
			TreeObject.writeOutTreeObject(invisibleRoot, fileLocation);
		}
		
		return invisibleRoot; 
	}
	
	public static TreeObject getTopLevelTreeObject(TreeObject treeObject){
		TreeObject node = treeObject;
		while(node.getParent()!=null){
			node = node.getParent();
		}
		return node;
	}
	
	public static void updateTreeObjectToFile(IProject currentProject, TreeObject cmtypeTreeSelectedObject){
		File treeIndexFile = RWTSystemUtil.getTreeIndexFile(currentProject);
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
//
//	public boolean isPrimaryType() {
//		return isPrimaryType;
//	}
//
//	public void setPrimaryType(boolean isPrimaryType) {
//		this.isPrimaryType = isPrimaryType;
//	}
//	
//	
//	public static ArrayList<String> getAllSuperTypes(String currentType){
//		TreeObject treeObject = RWTSystemUtil.readInAllCMTypesToTreeObject(ActivePart.getFileOfActiveEditror());
//		TreeObject currentTO = depthSearchFirst(currentType, treeObject);
//		ArrayList<String> superTypeList = new ArrayList<String>();
//		if(currentTO!=null){
//			TreeObject parentTO = currentTO.getParent();
//			while(!parentTO.toString().equals(TreeObject.treeObjectTopName)){
//				superTypeList.add(parentTO.toString());
//				parentTO = parentTO.getParent();
//			}
//		}
//		return superTypeList;
//	}
//	
//	private static TreeObject depthSearchFirst(String currentType, TreeObject currentTO){
//		TreeObject[] children = currentTO.getChildren();
//		for(int i=0;i<children.length;i++){
//			if(children[i].getName().equals(currentType)){
//				return children[i];
//			}else{
//				return depthSearchFirst(currentType,children[i]);	
//			}
//		}
//		return null;
//	}
//	
//	public static boolean isSubTypeOf(IProject currentProject, String typeNameOne, String typeNameTwo){
//		if(typeNameOne == null){
//			return false;
//		}
//		if(typeNameTwo == null){
//			return false;
//		}
//		if(typeNameOne.length() == 0){
//			return false;
//		}
//		if(typeNameTwo.length() == 0){
//			return false;
//		}
//		ArrayList<String> superTypeListOne = getAllSuperTypes(typeNameOne);
//		if(superTypeListOne.contains(typeNameTwo)){
//			return true;
//		}else{
//			return false;
//		}
//	}
	
}
