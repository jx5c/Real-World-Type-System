package rwtchecker.fpclose;
 
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
 
public class FPTree {
 
    private int minSuport;
 
    public int getMinSuport() {
        return minSuport;
    }
 
    public void setMinSuport(int minSuport) {
        this.minSuport = minSuport;
    }
 
    // Read data
    public List<List<String>> readDataRocords(String... filenames) {
        List<List<String>> inputData = null;
        if (filenames.length > 0) {
            inputData = new LinkedList<List<String>>();
            for (String filename : filenames) {
                try {
                    FileReader fr = new FileReader(filename);
                    BufferedReader br = new BufferedReader(fr);
                    try {
                        String line;
                        List<String> record;
                        while ((line = br.readLine()) != null) {
                            if(line.trim().length()>0){
                                String str[] = line.split(",");
                                record = new LinkedList<String>();
                                for (String w : str)
                                    record.add(w);
                                inputData.add(record);
                            }
                        }
                    } finally {
                        br.close();
                    }
                } catch (IOException ex) {
                    System.out.println("Read input data failed."
                            + ex.getMessage());
                    System.exit(1);
                }
            }
        }
        return inputData;
    }
 
    // FP-Growth algorithm
    public void FPGrowth(List<List<String>> dataRecords,
            List<String> postPattern) {
        // head table
        ArrayList<TreeNode> HeaderTable = buildHeaderTable(dataRecords);
        // create FP-Tree
        TreeNode treeRoot = buildFPTree(dataRecords, HeaderTable);
        // return if FP-Tree null
        if (treeRoot.getChildren()==null || treeRoot.getChildren().size() == 0)
            return;
        //postPattern
        if(postPattern!=null){
            for (TreeNode header : HeaderTable) {
                System.out.print(header.getCount() + "\t" + header.getName());
                for (String ele : postPattern)
                    System.out.print("\t" + ele);
                System.out.println();
            }
        }
        //for each header, conduct recursive call 
        for (TreeNode header : HeaderTable) {
            // 后缀模式增加一项
            List<String> newPostPattern = new LinkedList<String>();
            newPostPattern.add(header.getName());
            if (postPattern != null)
                newPostPattern.addAll(postPattern);
            // 寻找header的条件模式基CPB，放入newTransRecords中
            List<List<String>> newTransRecords = new LinkedList<List<String>>();
            TreeNode backnode = header.getNextHomonym();
            while (backnode != null) {
                int counter = backnode.getCount();
                List<String> prenodes = new ArrayList<String>();
                TreeNode parent = backnode;
                // 遍历backnode的祖先节点，放到prenodes中
                while ((parent = parent.getParent()).getName() != null) {
                    prenodes.add(parent.getName());
                }
                while (counter-- > 0) {
                    newTransRecords.add(prenodes);
                }
                backnode = backnode.getNextHomonym();
            }
            // recursive call
            FPGrowth(newTransRecords, newPostPattern);
        }
    }
 
    // create header table
    public ArrayList<TreeNode> buildHeaderTable(List<List<String>> dataRecords) {
        ArrayList<TreeNode> F1 = null;
        if (dataRecords.size() > 0) {
            F1 = new ArrayList<TreeNode>();
            Map<String, TreeNode> map = new HashMap<String, TreeNode>();
            // compute the support of each 
            for (List<String> record : dataRecords) {
                for (String item : record) {
                    if (!map.keySet().contains(item)) {
                        TreeNode node = new TreeNode(item);
                        node.setCount(1);
                        map.put(item, node);
                    } else {
                        map.get(item).countIncrement(1);
                    }
                }
            }
            // add items with support bigger than minSup
            Set<String> names = map.keySet();
            for (String name : names) {
                TreeNode tnode = map.get(name);
                if (tnode.getCount() >= minSuport) {
                    F1.add(tnode);
                }
            }
            //sort the table
            Collections.sort(F1);
            return F1;
        } else {
            return null;
        }
    }
 
    // create FP-Tree
    public TreeNode buildFPTree(List<List<String>> dataRecords,
            ArrayList<TreeNode> F1) {
        TreeNode root = new TreeNode(); // root tree_node
        for (List<String> dataRecord : dataRecords) {
            LinkedList<String> record = sortByF1(dataRecord, F1);
            TreeNode subTreeRoot = root;
            TreeNode tmpRoot = null;
            if (root.getChildren() != null) {
                while (!record.isEmpty()
                        && (tmpRoot = subTreeRoot.findChild(record.peek())) != null) {
                    tmpRoot.countIncrement(1);
                    subTreeRoot = tmpRoot;
                    record.poll();
                }
            }
            addNodes(subTreeRoot, record, F1);
        }
        return root;
    }
 
    // reorganize the data to the order of F1 
    private LinkedList<String> sortByF1(List<String> dataRecord,
            ArrayList<TreeNode> F1) {
        Map<String, Integer> map = new HashMap<String, Integer>();
        for (String item : dataRecord) {
            for (int i = 0; i < F1.size(); i++) {
                TreeNode tnode = F1.get(i);
                if (tnode.getName().equals(item)) {
                    map.put(item, i);
                }
            }
        }
        ArrayList<Entry<String, Integer>> al = new ArrayList<Entry<String, Integer>>(
                map.entrySet());
        Collections.sort(al, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Entry<String, Integer> arg0,
                    Entry<String, Integer> arg1) {
                // 
                return arg0.getValue() - arg1.getValue();
            }
        });
        LinkedList<String> rest = new LinkedList<String>();
        for (Entry<String, Integer> entry : al) {
            rest.add(entry.getKey());
        }
        return rest;
    }
  
    private void addNodes(TreeNode ancestor, LinkedList<String> record,
            ArrayList<TreeNode> F1) {
        if (record.size() > 0) {
            while (record.size() > 0) {
                String item = record.poll();
                TreeNode leafnode = new TreeNode(item);
                leafnode.setCount(1);
                leafnode.setParent(ancestor);
                ancestor.addChild(leafnode);
 
                for (TreeNode f1 : F1) {
                    if (f1.getName().equals(item)) {
                        while (f1.getNextHomonym() != null) {
                            f1 = f1.getNextHomonym();
                        }
                        f1.setNextHomonym(leafnode);
                        break;
                    }
                }
 
                addNodes(leafnode, record, F1);
            }
        }
    }
 
    public static void main(String[] args) {
        FPTree fptree = new FPTree();
        fptree.setMinSuport(3);
        List<List<String>> transRecords = fptree
                .readDataRocords(new String[]{"e:\\develop\\testing_data\\1.txt"});
        fptree.FPGrowth(transRecords, null);
    }
}