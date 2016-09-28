package cmtypechecker.util;

import org.eclipse.jdt.core.dom.ASTNode;

public class DiagnosticMessage {
	public static String WARNING = "Warning";
	public static String ERROR = "Error";
	
	
	private ASTNode errorNode;
	private String messageType = "";
	private String messageDetail = "";
	private String contextInfo = "";
	private boolean permitted = false;
	//could have additional fields
	
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
	public ASTNode getErrorNode() {
		return errorNode;
	}
	public void setErrorNode(ASTNode errorNode) {
		this.errorNode = errorNode;
	}
	public boolean isPermitted() {
		return permitted;
	}
	public void setPermitted(boolean permitted) {
		this.permitted = permitted;
	}

	
}
