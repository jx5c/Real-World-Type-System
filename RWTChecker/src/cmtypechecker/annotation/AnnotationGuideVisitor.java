package cmtypechecker.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.core.dom.*;

public class AnnotationGuideVisitor extends ASTVisitor {
		
	private Map<IMethodBinding, ArrayList<IVariableBinding>> methodAndItsIndependentVars = new HashMap<IMethodBinding, ArrayList<IVariableBinding>>();
	
	public AnnotationGuideVisitor(){
	}
	
	public boolean visit(MethodDeclaration methodDeclaration) {
		if(methodDeclaration != null){
			IMethodBinding methodBinding = methodDeclaration.resolveBinding();
			AssignmentVisitor assignmentVisitor = new AssignmentVisitor();
			methodDeclaration.accept(assignmentVisitor);
			ArrayList<IVariableBinding> inDependentVariableList  = assignmentVisitor.getIndependentVariableList();
			if(inDependentVariableList.size()>0){
				methodAndItsIndependentVars.put(methodBinding, inDependentVariableList);	
			}
		}
		return true;
	}
	
	public Map<IMethodBinding, ArrayList<IVariableBinding>> getMethodAndItsIndependentVars() {
		return methodAndItsIndependentVars;
	}
	
	class AssignmentVisitor extends ASTVisitor{
		private ArrayList<IVariableBinding> independentVariableList = new ArrayList<IVariableBinding>();
		private ArrayList<IVariableBinding> dependentVariableList = new ArrayList<IVariableBinding>();
		
		public ArrayList<IVariableBinding> getIndependentVariableList() {
			return independentVariableList;
		}

		public boolean visit(VariableDeclarationStatement variableDeclarationStatement) {
			for (Iterator iter = variableDeclarationStatement.fragments().iterator(); iter.hasNext();) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) iter.next();
				IBinding fbinding = fragment.getName().resolveBinding();
				if(fbinding instanceof IVariableBinding){
					IVariableBinding variableBinding = (IVariableBinding) fbinding;
					Expression initizlizer = fragment.getInitializer();
					if(initizlizer != null){
						SimpleNameVisitor simpleNameVisitor = new SimpleNameVisitor(); 
						initizlizer.accept(simpleNameVisitor);
						ArrayList<IVariableBinding> rightHandVariables = simpleNameVisitor.getVariables();
						if(rightHandVariables.size() > 0){
							if(rightHandVariables.contains(variableBinding)){
								for(IVariableBinding thisVariableBinding: rightHandVariables){
									if(!independentVariableList.contains(thisVariableBinding) && 
											!dependentVariableList.contains(thisVariableBinding)){
										independentVariableList.add(thisVariableBinding);
									}
								}
							}else{
								for(IVariableBinding thisVariableBinding: rightHandVariables){
									if(!independentVariableList.contains(thisVariableBinding) &&
											!dependentVariableList.contains(thisVariableBinding)){
										independentVariableList.add(thisVariableBinding);
									}
								}
								if(!dependentVariableList.contains(variableBinding)){
									dependentVariableList.add(variableBinding);	
								}
							}
						}
					}

				}
			}
			return true;
		}
		public boolean visit(Assignment assignment) {
			Expression lefthand = assignment.getLeftHandSide();
			SimpleNameVisitor leftSimpleNameVisitor = new SimpleNameVisitor();
			lefthand.accept(leftSimpleNameVisitor);
			ArrayList<IVariableBinding> leftVariables = leftSimpleNameVisitor.getVariables();
			
			Expression righthand = assignment.getRightHandSide();
			SimpleNameVisitor rightSimpleNameVisitor = new SimpleNameVisitor();
			righthand.accept(rightSimpleNameVisitor);
			ArrayList<IVariableBinding> rightVariables = rightSimpleNameVisitor.getVariables();
			
			for(IVariableBinding leftVariable :leftVariables){
				if(rightVariables.size() > 0){
					if(rightVariables.contains(leftVariable)){
						for(IVariableBinding thisVariableBinding: rightVariables){
							if(!independentVariableList.contains(thisVariableBinding) && 
									!dependentVariableList.contains(thisVariableBinding)){
								independentVariableList.add(thisVariableBinding);
							}
						}
					}else{
						for(IVariableBinding thisVariableBinding: rightVariables){
							if(!independentVariableList.contains(thisVariableBinding) &&
									!dependentVariableList.contains(thisVariableBinding)){
								independentVariableList.add(thisVariableBinding);
							}
						}
						if(!dependentVariableList.contains(leftVariable)){
							dependentVariableList.add(leftVariable);	
						}
					}
				}
			}
			return true;
		}
	}
	
	class SimpleNameVisitor extends ASTVisitor{
		private ArrayList<IVariableBinding> variables = new ArrayList<IVariableBinding>();
		
		public ArrayList<IVariableBinding> getVariables() {
			return variables;
		}

		public boolean visit(SimpleName simpleName) {
			IBinding fbinding = simpleName.resolveBinding();
			if(fbinding instanceof IVariableBinding){
				IVariableBinding variableBinding = (IVariableBinding) fbinding;
				variables.add(variableBinding);
			}
			return false;
		}
	}
	
}