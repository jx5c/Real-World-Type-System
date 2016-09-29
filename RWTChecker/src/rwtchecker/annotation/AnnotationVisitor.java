package rwtchecker.annotation;

import org.eclipse.jdt.core.dom.*;

import rwtchecker.CMRules.CMTypeRulesManager;
import rwtchecker.annotation.FileAnnotations;

public class AnnotationVisitor extends ASTVisitor {
		
	private FileAnnotations fileAnnotations = new FileAnnotations ();
	
	public AnnotationVisitor(CompilationUnit compilationUnit) {
		super(true);
	}
	
	public AnnotationVisitor(){
	}
	
	public void preVisit(ASTNode node) {
		if(node instanceof TypeDeclaration){
			TypeDeclaration typeDeclaration = (TypeDeclaration)node;
			if(typeDeclaration != null){
				String classBindingKey = typeDeclaration.resolveBinding().getKey();				
				Javadoc javadoc = typeDeclaration.getJavadoc();
				if(javadoc != null){
					for(int i=0;i<javadoc.tags().size();i++){
						TagElement tagElement = (TagElement)(javadoc.tags().get(i));
						if(tagElement.getTagName()!=null){
							if(tagElement.getTagName().equals(RWTAnnotation.tagNameForAnnotation)){
								if(tagElement.fragments().get(0) instanceof TextElement){
									String tagElementContents = tagElement.fragments().get(0).toString().trim();
									String annotationType = tagElementContents.split(" ")[0];
									String annotationContents = tagElementContents.split(" ")[1];
									fileAnnotations.addAnnotationContent(classBindingKey, annotationType, annotationContents);
								}
							}
						}
					}
				}
			}
		}
				
		if(node instanceof MethodDeclaration){
			MethodDeclaration methodDecl = (MethodDeclaration)node;
			if(methodDecl != null){
				String methodKey = methodDecl.resolveBinding().getKey();		
				Javadoc javadoc = methodDecl.getJavadoc();
				if(javadoc != null){
					for(int i=0;i<javadoc.tags().size();i++){
						TagElement tagElement = (TagElement)(javadoc.tags().get(i));
						if(tagElement.getTagName()!=null){
							if(tagElement.getTagName().equals(RWTAnnotation.tagNameForAnnotation)){
								if(tagElement.fragments().get(0) instanceof TextElement){
									String tagElementContents = tagElement.fragments().get(0).toString().trim();
									String annotationType = tagElementContents.split(" ")[0];
									String annotationContents = tagElementContents.split(" ")[1];
									fileAnnotations.addAnnotationContent(methodKey, annotationType, annotationContents);
								}
							}
						}
					}
				}
			}
    	}
		
	}
	
	public FileAnnotations getFileAnnotations() {
		return fileAnnotations;
	}

	public void setFileAnnotations(FileAnnotations fileAnnotations) {
		this.fileAnnotations = fileAnnotations;
	}
}