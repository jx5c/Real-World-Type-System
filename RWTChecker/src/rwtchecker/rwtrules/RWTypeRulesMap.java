package rwtchecker.rwtrules;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;

import rwtchecker.util.ActivePart;
import rwtchecker.util.RWTSystemUtil;

public class RWTypeRulesMap implements Serializable{
	
	private static final long serialVersionUID = 5880862074200358476L;
	private String cmtypefilePath = "";
	private Map<String, String> typeRuleMap = new HashMap<String, String>(); 
	
	public RWTypeRulesMap(File file){
		this.cmtypefilePath = file.getAbsolutePath();
		if(!file.exists()){
			this.storeRules();
		}else{
			this.loadRules();
		}
	}
	
	public static RWTypeRulesMap getCMTypeRulesMapForCurrentProject(){
		if(ActivePart.getFileOfActiveEditror() != null){
			File file = RWTSystemUtil.getRWTypeRulesFiles(ActivePart.getFileOfActiveEditror().getProject());
			return new RWTypeRulesMap(file);
		}
		return null;
	}
	
	public void storeRules(){
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream
			(new FileOutputStream(new File(this.cmtypefilePath)));
			out.writeObject(this.typeRuleMap);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void loadRules(){
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(new FileInputStream(this.cmtypefilePath));
			typeRuleMap = (Map<String, String>)in.readObject();
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void addCMTypeOperation(CMTypeRule cmTypeRule){
		String typeRuleKey = constructKey(cmTypeRule);
		String cmTypeReturnType = cmTypeRule.getReturnCMTypeName();
		this.typeRuleMap.put(typeRuleKey, cmTypeReturnType);
	}
	
	public void delCMTypeOperation(CMTypeRule cmTypeRule){
		String typeRuleKey = constructKey(cmTypeRule);
		if(this.typeRuleMap.containsKey(typeRuleKey)){
			this.typeRuleMap.remove(typeRuleKey);
		}
	}
	
	public static String constructKey(String cmTypeOneName, String cmTypeTwoName, String operationName){
		String typeRuleKey = "";
		if((operationName.equals(RWTypeRuleCategory.Plus))
			&&(operationName.equals(RWTypeRuleCategory.Multiplication))
		){
			if(cmTypeOneName.compareToIgnoreCase(cmTypeTwoName)<0){
				typeRuleKey = cmTypeOneName + operationName + cmTypeTwoName;
			}else{
				typeRuleKey = cmTypeTwoName + operationName + cmTypeOneName ;
			}
		}else{
			typeRuleKey = cmTypeOneName + operationName + cmTypeTwoName;
		}
		return typeRuleKey;
	}
	
	public static String constructKey(CMTypeRule cmTypeRule){
		String operationName = cmTypeRule.getOperationName();
		String cmTypeOneName = cmTypeRule.getCMTypeOneName();
		String cmTypeTwoName = cmTypeRule.getCMTypeTwoName();
		return constructKey(cmTypeOneName, cmTypeTwoName, operationName);
	}
	
	public String getReturnType(IProject currentProject, String argumentOneCMType, String argumentTwoCMType, String operationName){
		String typeRuleKey = constructKey(argumentOneCMType,argumentTwoCMType,operationName);
		String returnValue = this.typeRuleMap.get(typeRuleKey);
		return returnValue;
	}
	
	public String getReturnType(IProject currentProject, String argumentCMType, String operationName){
		String typeRuleKey = constructKey(argumentCMType,"",operationName);
		return this.typeRuleMap.get(typeRuleKey);
	}
	
	public class CMTypeRule implements Serializable{
		private static final long serialVersionUID = -2237305791742897925L;
		private String CMTypeOneName = "";
		private String CMTypeTwoName = "";
		private String returnCMTypeName = "";
		private String operationName = "";
		
		public String getCMTypeOneName() {
			return CMTypeOneName;
		}
		public void setCMTypeOneName(String cMTypeOneName) {
			CMTypeOneName = cMTypeOneName;
		}
		public String getCMTypeTwoName() {
			return CMTypeTwoName;
		}
		public void setCMTypeTwoName(String cMTypeTwoName) {
			CMTypeTwoName = cMTypeTwoName;
		}
		public String getReturnCMTypeName() {
			return returnCMTypeName;
		}
		public void setReturnCMTypeName(String returnCMTypeName) {
			this.returnCMTypeName = returnCMTypeName;
		}
		public String getOperationName() {
			return operationName;
		}
		public void setOperationName(String operationName) {
			this.operationName = operationName;
		}
		public String toString(){
			return this.returnCMTypeName+"=" + this.CMTypeOneName + this.operationName + this.CMTypeTwoName;
		}
		
		public boolean equals(Object obj){
			if(obj instanceof CMTypeRule){
				if(((CMTypeRule)obj).toString().equals(this.toString())){
					return true;
				}
			}
			return false;
		}
	}
}
