package rwtchecker.util;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.jdt.core.dom.ASTNode;

public class DiagnosticMessage {
	public static String WARNING = "Warning";
	public static String ERROR = "Error";
	
	public static int javaChecking = 1;
	public static int cChecking = 2;
	//maybe others
	
	//specify if this is for Java or C
	private int errorCategory = 0;
	
	//for java error checking
	private ASTNode javaErrorNode;
	//for c error checking
	private IASTNode cErrorNode;
	
	private String messageType = "";
	private String messageDetail = "";
	private String contextInfo = "";
	private boolean permitted = false;
	//could have additional fields
	
	public IASTNode getcErrorNode() {
		return cErrorNode;
	}
	public void setcErrorNode(IASTNode cErrorNode) {
		this.cErrorNode = cErrorNode;
	}
	public String getMessageType() {
		return messageType;
	}
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}
	public String getMessageDetail() {
		return messageDetail;
	}
	public void setMessageDetail(String messageDetail) {
		this.messageDetail = messageDetail;
	}
	public String getContextInfo() {
		return contextInfo;
	}
	public void setContextInfo(String contextInfo) {
		this.contextInfo = contextInfo;
	}
	public ASTNode getJavaErrorNode() {
		return javaErrorNode;
	}
	public void setJavaErrorNode(ASTNode errorNode) {
		this.javaErrorNode = errorNode;
	}
	public boolean isPermitted() {
		return permitted;
	}
	public void setPermitted(boolean permitted) {
		this.permitted = permitted;
	}
	public int getErrorCategory() {
		return errorCategory;
	}
	public void setErrorCategory(int errorCategory) {
		this.errorCategory = errorCategory;
	}
	
}
