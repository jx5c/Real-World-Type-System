package rwtchecker.views;

import java.text.Collator;
import java.util.Locale;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import rwtchecker.rwtrules.RWTypeRule;

public class SorterInOperationView extends ViewerSorter {
    private static final int Operation_Name = 1;
    private static final int Argument_One = 2;
    private static final int Argument_Two = 3;
    private static final int Return_Type = 4;
    
    public static final SorterInOperationView Operation_Name_ASC = new SorterInOperationView(Operation_Name);
    public static final SorterInOperationView Operation_Name_DESC = new SorterInOperationView(-Operation_Name);
    public static final SorterInOperationView Argument_One_ASC = new SorterInOperationView(Argument_One);
    public static final SorterInOperationView Argument_One_DESC = new SorterInOperationView(-Argument_One);
    public static final SorterInOperationView Argument_Two_ASC = new SorterInOperationView(Argument_Two);
    public static final SorterInOperationView Argument_Two_DESC = new SorterInOperationView(-Argument_Two);
    public static final SorterInOperationView Return_Type_ASC = new SorterInOperationView(Return_Type);
    public static final SorterInOperationView Return_Type_DESC = new SorterInOperationView(-Return_Type);
    
    private int sortType ;
    private SorterInOperationView(int sortType){
        this.sortType = sortType;
    }
    public int compare(Viewer viewer, Object obj1, Object obj2) {
    	Collator collator = Collator.getInstance(Locale.getDefault());
    	RWTypeRule operation1 = (RWTypeRule)obj1;
    	RWTypeRule operation2 = (RWTypeRule)obj2;
        switch(sortType){
            case Operation_Name:{
                String l1 = operation1.getOperationName();
                String l2 = operation2.getOperationName();
                return collator.compare(l1,l2);
            }
            case -Operation_Name:{
                String l1 = operation1.getOperationName();
                String l2 = operation2.getOperationName();
                return collator.compare(l2, l1);
            }
            case Argument_One:{
                String s1 = operation1.getCMTypeOneName();
                String s2 = operation2.getCMTypeOneName();
                return collator.compare(s1, s2);
            }
            case -Argument_One:{
                String s1 = operation1.getCMTypeOneName();
                String s2 = operation2.getCMTypeOneName();
                return collator.compare(s2, s1);
            }
            case Argument_Two:{
                String l1 = operation1.getCMTypeTwoName();
                String l2 = operation2.getCMTypeTwoName();
                return collator.compare(l1, l2);
            }
            case -Argument_Two:{
            	String l1 = operation1.getCMTypeTwoName();
                String l2 = operation2.getCMTypeTwoName();
                return collator.compare(l2, l1);
            }
            case Return_Type:{
                String s1 = operation1.getReturnCMTypeName();
                String s2 = operation2.getReturnCMTypeName();
                return collator.compare(s1, s2);
            }
            case -Return_Type:{
                String s1 = operation1.getReturnCMTypeName();
                String s2 = operation2.getReturnCMTypeName();
                return collator.compare(s2, s1);
            }
        }
        return 0;
    }
}
