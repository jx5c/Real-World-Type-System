package rwtchecker.test;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTAttribute;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTTypeId;
import org.eclipse.cdt.core.dom.ast.IBasicType.Kind;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTBinaryExpression;
import org.eclipse.cdt.internal.core.dom.parser.c.CFunction;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTBinaryExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionCallExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTIdExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTInitializerExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPArrayType;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPBasicType;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPFunction;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPMethod;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPVariable;
import org.eclipse.cdt.internal.core.model.BinaryElement;
import org.eclipse.cdt.internal.core.pdom.dom.FindBinding;
import org.eclipse.core.runtime.CoreException;

public class ParseExample {

	public static void main(String[] args) throws CoreException{
		FileContent fileContent = FileContent.createForExternalFileLocation("C:\\develop\\rcpworkspace\\testC\\Hello.c");

		Map definedSymbols = new HashMap();
		String[] includePaths = new String[0];
		IScannerInfo info = new ScannerInfo(definedSymbols, includePaths);
		IParserLogService log = new DefaultLogService();
		
//		ITranslationUnit tu = (ITranslationUnit) CoreModel.getDefault().create(myIFile);
//		IASTTranslationUnit ias = tu.getAST();
		
		IncludeFileContentProvider emptyIncludes = IncludeFileContentProvider.getEmptyFilesProvider();
		
		int opts = GPPLanguage.getDefault().OPTION_IS_SOURCE_UNIT;
		IASTTranslationUnit translationUnit = GPPLanguage.getDefault().getASTTranslationUnit(fileContent, info, emptyIncludes, null, opts, log);
//		translationUnit.
		IASTPreprocessorIncludeStatement[] includes = translationUnit.getIncludeDirectives();
		for (IASTPreprocessorIncludeStatement include : includes) {
			System.out.println("include - " + include.getName());
		}
		 
		//printTree(translationUnit, 1);
		
		System.out.println("-----------------------------------------------------");
		System.out.println("-----------------------------------------------------");
		System.out.println("-----------------------------------------------------");
	
		ASTVisitor visitor = new ASTVisitor()
		{
			private HashMap<IASTNode, String> expToRWType = new HashMap<IASTNode, String>();
			
			public int leave(IASTExpression exp){
				
				if(exp instanceof IASTCastExpression){
					IASTCastExpression caseExp = (IASTCastExpression)exp;
					System.out.println(caseExp.getOperator());
					System.out.println(caseExp.getOperand().getRawSignature());
				}
				
				IType expType = exp.getExpressionType();
				IASTNode node = exp.getOriginalNode();
				System.out.println(node);
				if(node instanceof CASTBinaryExpression){
					CASTBinaryExpression binaryExp = (CASTBinaryExpression)node;
					System.out.println(binaryExp.getOperand1());
					System.out.println(binaryExp.getOperand2());
				}
				
				if(node instanceof CPPASTBinaryExpression){
					CPPASTBinaryExpression binaryExp = (CPPASTBinaryExpression)node;
					System.out.println(binaryExp.getOperand1().getOriginalNode().getRawSignature());
//					System.out.println(rwtypeTableForExp.get());
//					if(binaryExp.getOperand1() instanceof CPPASTIdExpression){
//						IASTExpression expToRWType = ((CPPASTIdExpression)binaryExp.getOperand1()).getName();
//						CPPASTIdExpression exptemp = (CPPASTIdExpression)(binaryExp.getOperand1());
//						System.out.println(expToRWType.get(binaryExp.getOperand1()));	
//					}
					System.out.println("operand one is: "+expToRWType.get(binaryExp.getOperand1()));
					System.out.println("operand two is: "+expToRWType.get(binaryExp.getOperand2()));
				}
				
				if(node instanceof CPPASTFunctionCallExpression){
					CPPASTFunctionCallExpression functionCall = (CPPASTFunctionCallExpression)(node);
					
					System.out.println(expToRWType.get(functionCall.getFunctionNameExpression()));
				}
				
				return 3;
				
			}
			
			public int visit(IASTName name){
				System.out.println("original node is: "+name.getOriginalNode());
				IBinding fbinding = name.resolveBinding();
				
				System.out.println(name.getOriginalNode().getFileLocation());
				if(name.getParent() instanceof CPPASTIdExpression){
					expToRWType.put(name.getParent(), "rwtype:"+name.toString());
				}
				if (fbinding instanceof CPPVariable){
					IASTNode astNode = name.getOriginalNode();
					CPPVariable varName = (CPPVariable)fbinding;
					System.out.println(varName.getDefinition());
					IType aType = varName.getType();
					if(aType instanceof CPPBasicType){
						CPPBasicType basicType = (CPPBasicType)aType;
						if(basicType.getKind() == Kind.eInt || basicType.getKind() == Kind.eInt128 ){
//							IASTExpression exp = (IASTExpression )(name.getOriginalNode()); 
//							rwtypeTableForExp.put(name, "RWT_type_"+exp);
//							rwtypeTableForExp.put(name.resolveBinding(), "RWT_type_"+name);
//							expToRWType.put(astNode, "RWT_type_"+name);
//							System.out.println("Integer");
						}else if(basicType.getKind() == Kind.eFloat ){
//							System.out.println("float");
						}else if(basicType.getKind() == Kind.eDouble ){
//							System.out.println("double");
						}
					}
					if(aType instanceof CPPArrayType){
						System.out.println("array");
					}
				}else if(fbinding instanceof CFunction || fbinding instanceof CPPFunction){
					CPPFunction f = (CPPFunction)fbinding;
					System.out.println(f.getDefinition().getFileLocation());
				}
				
				
				if ((name.getParent() instanceof CPPASTFunctionDeclarator)) {
					System.out.println("IASTName: " + name.getClass().getSimpleName() + "(" + name.getRawSignature() + ") - > parent: " + name.getParent().getClass().getSimpleName());
					System.out.println("-- isVisible: " + isVisible(name));
				}
				return 3;
			}
			public int visit(IASTDeclaration declaration){
				if ((declaration instanceof IASTSimpleDeclaration)) {
					IASTSimpleDeclaration ast = (IASTSimpleDeclaration)declaration;
					IASTDeclarator[] declarators = ast.getDeclarators();
					for (IASTDeclarator iastDeclarator : declarators) {
						expToRWType.put(iastDeclarator.getName(), "rwtype:test");
					}
//				
//					IASTAttribute[] attributes = ast.getAttributes();
//					for (IASTAttribute iastAttribute : attributes) {
//						System.out.println("iastAttribute > " + iastAttribute);
//					}
				}
				return 3;
			}
			
			public int leave(IASTDeclaration declaration){
				if ((declaration instanceof IASTSimpleDeclaration)) {
					IASTSimpleDeclaration ast = (IASTSimpleDeclaration)declaration;
					IASTDeclarator[] declarators = ast.getDeclarators();
					for (IASTDeclarator iastDeclarator : declarators) {
//						System.out.println(rwtypeTableForExp.get(iastDeclarator.getName().resolveBinding()));
						System.out.println(expToRWType.get(iastDeclarator.getName()));
					}
				
					IASTAttribute[] attributes = ast.getAttributes();
					for (IASTAttribute iastAttribute : attributes) {
//						System.out.println("iastAttribute > " + iastAttribute);
					}
				}
				if ((declaration instanceof IASTFunctionDefinition)) {
					IASTFunctionDefinition ast = (IASTFunctionDefinition)declaration;
					IScope scope = ast.getScope();
					try{
						System.out.println("### function() - Parent = " + scope.getParent().getScopeName());
						System.out.println("### function() - Syntax = " + ast.getSyntax());
					}catch (DOMException e) {
						e.printStackTrace();
					} catch (ExpansionOverlapsBoundaryException e) {
						e.printStackTrace();
					}
					ICPPASTFunctionDeclarator typedef = (ICPPASTFunctionDeclarator)ast.getDeclarator();
					System.out.println("------- typedef: " + typedef.getName());
				}
				return 3;
			}
				
			
			public int visit(IASTTypeId typeId){
				System.out.println("typeId: " + typeId.getRawSignature());
				return 3;
			}
				
			public int visit(IASTStatement statement){
				
				for(IASTNode node : statement.getChildren()){
					System.out.println(node.getRawSignature());
				}
				System.out.println("statement: " + statement.getRawSignature());
				return 3;
			}
				
			public int visit(IASTAttribute attribute){
				return 3;
			}
		};
		visitor.shouldVisitNames = true;
		visitor.shouldVisitDeclarations = true;
		visitor.shouldVisitDeclarators = true;
		visitor.shouldVisitAttributes = true;
		visitor.shouldVisitStatements = true;
		visitor.shouldVisitTypeIds = true;
		visitor.shouldVisitExpressions = true;
		translationUnit.accept(visitor);
	}
	
	private static void printTree(IASTNode node, int index) {
		IASTNode[] children = node.getChildren();
		
		boolean printContents = true;
		
		if ((node instanceof CPPASTTranslationUnit)) {
			printContents = false;
		}
		
		String offset = "";
		try {
			offset = node.getSyntax() != null ? " (offset: " + node.getFileLocation().getNodeOffset() + "," + node.getFileLocation().getNodeLength() + ")" : "";
			printContents = node.getFileLocation().getNodeLength() < 30;
		} catch (ExpansionOverlapsBoundaryException e) {
			e.printStackTrace();
		} catch (UnsupportedOperationException e) {
			offset = "UnsupportedOperationException";
		}
		
		System.out.println(String.format(new StringBuilder("%1$").append(index * 2).append("s").toString(), new Object[] { "-" }) + node.getClass().getSimpleName() + offset + " -> " + (printContents ? node.getRawSignature().replaceAll("\n", " \\ ") : node.getRawSignature().subSequence(0, 5)));
		
		for (IASTNode iastNode : children)
			printTree(iastNode, index + 1);
		}
		
		public static boolean isVisible(IASTNode current)
		{
			IASTNode declator = current.getParent().getParent();
			IASTNode[] children = declator.getChildren();
			
			for (IASTNode iastNode : children) {
				if ((iastNode instanceof ICPPASTVisibilityLabel)) {
					return 1 == ((ICPPASTVisibilityLabel)iastNode).getVisibility();
				}
			}
			return false;
		}
}



