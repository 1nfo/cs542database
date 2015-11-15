package project.relations;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import project.DBManager;
import project.Pair;
import project.Parser;

import java.util.Comparator;
import java.util.LinkedList;
/**
 * Created by wangqian on 11/9/15.
 */
public class JoinOperator implements AlgebraNode {
    private static String operator_name = "Join";
    private List<AlgebraNode> publishers; // Here this list's size is said to be max 2.
    private List<List<Pair<Integer,Integer>>> TuplesofLeft; //Now let's assume only one join happens
	private Map<Integer,String> JoinInfo;
	private Map<Pair<Integer,Integer>, Pair<String,Pair>> CrossTbCdt;
	private static DBManager dbm = DBManager.getInstance();
	private LinkedList<List<Pair<Integer,Integer>>> Results;


    public JoinOperator(List<Pair<Integer,String>> Info
    		,Map<Pair<Integer,Integer>, Pair<String,Pair>> Cross){
    	publishers = new ArrayList<AlgebraNode>();
    	TuplesofLeft = new ArrayList<List<Pair<Integer,Integer>>>();
    	Results = new LinkedList<List<Pair<Integer,Integer>>>();
    	CrossTbCdt = Cross;
    	JoinInfo = new HashMap<Integer, String>();
    	for (Pair<Integer,String> p : Info){
        	int corTID = p.getLeft();
        	String attr = p.getRight();
        	JoinInfo.put(corTID, attr);
    	}
    }

    public void attach(AlgebraNode node){
        this.publishers.add(node);
    }
    public void dettach(AlgebraNode node){
        this.publishers.remove(node);
    }
    
    private class AttrComparator implements Comparator<List<Pair<Integer,Integer>>>{

		@Override
		public int compare(List<Pair<Integer,Integer>> l1, List<Pair<Integer,Integer>> l2) {
			ArrayList<Integer> tIDs = new ArrayList<Integer>();
			for (Pair<Integer,Integer> p : l1){
				int tmp = p.getLeft();
				if (JoinInfo.containsKey(tmp)) tIDs.add(p.getLeft());
			}
			int cmp = -1;
			int i = 0;
			for (int t : tIDs){
				String attr = JoinInfo.get(t);
				byte[] tuple1 = dbm.Get(t, l1.get(i).getRight());
				byte[] tuple2 = dbm.Get(t, l2.get(i).getRight());
				Object val1 = dbm.getAttribute(t, tuple1, attr);
				Object val2 = dbm.getAttribute(t, tuple2, attr);
				cmp = val1.toString().compareTo(val2.toString());
				if (cmp != 0) break;
				i++;
			}
			return cmp;
		}
    	
    }

    @Override
    public void open() {
    	List<Pair<Integer,Integer>> l;
    	AlgebraNode left = publishers.get(0);
    	AlgebraNode right = publishers.get(1);
    	//Get everything from the left algebraNode
    	left.open();
    	while ((l=left.getNext())!=null){
    		TuplesofLeft.add(l);
    	}
    	//Pre-sorting based on to-join attributes
    	TuplesofLeft.sort(new AttrComparator());
    	//Testing Sort
    	for (List<Pair<Integer,Integer>> a : TuplesofLeft){
    		int key = a.get(0).getRight();
    		byte[] tuple = dbm.Get(a.get(0).getLeft(),key);
    	}
    	right.open();
    }
    
    private List<String> union(List<String> l1, List<Pair> l2){
    	for (Pair<String,Pair> p : l2){
    		String tmp = p.getLeft();
    		if (!l1.contains(tmp)){
    			l1.add(tmp);
    		}
    	}
    	return l1;
    }
    

    @Override
    public List<Pair<Integer,Integer>> getNext() {
    	List<Pair<Integer,Integer>> r = null;
    	List<Pair<Integer,Integer>> receivedData = publishers.get(1).getNext();
    	if (receivedData != null){
    		int tID = receivedData.get(0).getLeft();
    		int rID = receivedData.get(0).getRight();
    		byte[] tupleR = dbm.Get(tID, rID);
    		Object val1 = dbm.getAttribute(tID, tupleR, JoinInfo.get(tID));
    		for (List<Pair<Integer,Integer>> left : TuplesofLeft){
    	    	List<Pair<Integer,Integer>> l = new ArrayList<Pair<Integer,Integer>>();
    			int lrID = left.get(0).getRight();
    			int ltID = left.get(0).getLeft();
        		byte[] tupleL = dbm.Get(ltID,lrID);
        		Object val = dbm.getAttribute(ltID, tupleL, JoinInfo.get(ltID));
        		List<Pair> listed = new ArrayList<Pair>();
        		Pair p = CrossTbCdt.get(new Pair(ltID,tID));
        		if (p == null) p = CrossTbCdt.get(new Pair(tID,ltID));
        		listed.add(CrossTbCdt.get(new Pair(ltID,tID)));
        		try{
        		if (val.equals(val1) 
        				&& SelectOperator.handleCondition(listed, dbm, lrID, rID)){
        			l.add(left.get(0));
        			l.add(receivedData.get(0));
        			Results.offer(l);
        		}
        		}catch (Exception e){
        			e.printStackTrace();
        		}
        		
    		}
    		if (!Results.isEmpty()) return Results.poll();
    		else return this.getNext();
    	}
    	return receivedData;
    }

    @Override
    public void close() {

    }


    public static String getOperator_name() {
        return operator_name;
    }


    public static void main(String[] args) {
    	DBManager dbm = DBManager.getInstance();
    	AlgebraNode r1 = new Relation("country");
    	AlgebraNode r2 = new Relation("city");
    	Parser p = new Parser("select Country.code from Country, City on Country.code = city.CountryCode"
    			+ " where 0.4 * Country.population <= city.population");
    	JoinOperator j1 = new JoinOperator(p.getJInfo(),p.getCrossTable());
    	j1.attach(r1);
    	j1.attach(r2);
    	j1.open();
    	List<Pair<Integer,Integer>> l;
    	while( (l = j1.getNext()) != null){
    		byte[] t1 = dbm.Get(0,l.get(0).getRight());
    		byte[] t2 = dbm.Get(1,l.get(1).getRight());
    		int tid1 = l.get(0).getLeft();
    		int tid2 = l.get(1).getLeft();
    		System.out.println("Same Code:"+ dbm.getAttribute(tid1, t1, "code") + " " +
    				dbm.getAttribute(tid1, t1, "Name")+" "+dbm.getAttribute(tid1, t1, "Population")+ " " +" "+
    				dbm.getAttribute(tid2, t2, "Name") + " "+ dbm.getAttribute(tid2, t2, "Population"));
    	}
    }

}
