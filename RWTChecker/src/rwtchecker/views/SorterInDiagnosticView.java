package rwtchecker.views;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import rwtchecker.util.DiagnosticMessage;

public class SorterInDiagnosticView extends ViewerSorter {
    private static final int ErrorNode = 1;
    private static final int ErrorMessage = 2;
    
    public static final SorterInDiagnosticView ErrorNode_ASC = new SorterInDiagnosticView(ErrorNode);
    public static final SorterInDiagnosticView ErrorNode_DESC = new SorterInDiagnosticView(-ErrorNode);
    public static final SorterInDiagnosticView ErrorMessage_ASC = new SorterInDiagnosticView(ErrorMessage);
    public static final SorterInDiagnosticView ErrorMessage_DESC = new SorterInDiagnosticView(-ErrorMessage);
    
    private int sortType ;
    private SorterInDiagnosticView(int sortType){
        this.sortType = sortType;
    }
    public int compare(Viewer viewer, Object e1, Object e2) {
        DiagnosticMessage dm1 = (DiagnosticMessage)e1;
        DiagnosticMessage dm2 = (DiagnosticMessage)e2;
        switch(sortType){
            case ErrorNode:{
                String l1 = dm1.getJavaErrorNode().toString();
                String l2 = dm2.getJavaErrorNode().toString();
                return l1.compareTo(l2);
            }
            case -ErrorNode:{
                String l1 = dm1.getJavaErrorNode().toString();
                String l2 = dm2.getJavaErrorNode().toString();
                return l2.compareTo(l1);
            }
            case ErrorMessage:{
                String s1 = dm1.getMessageDetail();
                String s2 = dm2.getMessageDetail();
                return s1.compareTo(s2);
            }
            case -ErrorMessage:{
                String s1 = dm1.getMessageDetail();
                String s2 = dm2.getMessageDetail();
                return s2.compareTo(s1);
            }
        }
        return 0;
    }
}
