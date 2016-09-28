package cmtypechecker.typechecker;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.LineComment;

public class CommentVisitor extends ASTVisitor {

    CompilationUnit compilationUnit;

    public static final String def = "def";
    public static final String func = "func";
    
    private String[] source;
    //end line of comments
    private int lineCourt = 0;
    private ArrayList<String> commentConents = new ArrayList<String>();
    private boolean defComment = false;
    private boolean funcComment = false;

	public CommentVisitor(CompilationUnit compilationUnit, String[] source) {
        super();
        this.compilationUnit = compilationUnit;
        this.source = source;
    }

    public boolean visit(LineComment node) {
        return true;
    }

    public boolean visit(BlockComment node) {
        int startLineNumber = compilationUnit.getLineNumber(node.getStartPosition()) - 1;
        int endLineNumber = compilationUnit.getLineNumber(node.getStartPosition() + node.getLength()) - 1;
             
        String firstLine = source[startLineNumber].trim();
        if(firstLine.startsWith("/*cm")){
        	String comments = "";
            if(startLineNumber==endLineNumber){
            	comments = firstLine.replace("/*cm ", "").replace(" cm*/", "").trim();
				lineCourt = startLineNumber;
            }
            else{
            	StringBuffer blockComment = new StringBuffer();
            	blockComment.append(source[startLineNumber].trim());
                for (int lineCount = startLineNumber+1 ; lineCount<= endLineNumber; lineCount++) {
                    String blockCommentLine = source[lineCount].replaceAll("\\* ", "").trim();
                    blockComment.append(blockCommentLine);
                    if (lineCount == endLineNumber) {
                        break;
                    }
                }
                comments = blockComment.toString().replace("/*cm ", "").replace(" cm*/", "").trim();
				lineCourt = endLineNumber;
            }
            
			for (int j=0;j<comments.split(";").length;j++){
				if(comments.startsWith(CommentVisitor.def)){
					defComment = true;
					funcComment = false;
					comments = comments.replace(CommentVisitor.def, "").trim();
				}
				else if(comments.startsWith(CommentVisitor.func)){
					defComment = false;
					funcComment = true;
					comments = comments.replace(CommentVisitor.func, "").trim();
				}
				commentConents.add(comments.split(";")[j]);
			}
        }
        return true;
    }

    public void preVisit(ASTNode node) {

    }

	public int getLineCourt() {
		return lineCourt;
	}

	public ArrayList<String> getCommentConents() {
		return commentConents;
	}

	public boolean isDefComment() {
		return defComment;
	}

	public boolean isFuncComment() {
		return funcComment;
	}
}