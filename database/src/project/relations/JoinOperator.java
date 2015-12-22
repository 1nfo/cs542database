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

public class JoinOperator implements AlgebraNode {
	private boolean isOpen;
	private static String operator_name = "Join";
	private List<AlgebraNode> publishers; // Here this list's size is said to be max 2.
	private List<List<Pair<Integer,Integer>>> TupleOfLeft; //Now let's assume only one join happens
	private Map<Integer,String> JoinInfo;
	private Map<Pair<Integer,Integer>, Pair<String,Pair>> CrossTbCdt;
	private static DBManager dbm = DBManager.getInstance();
	private LinkedList<List<Pair<Integer,Integer>>> Results;
	private int next,start;
	private Object oldVal;


	public String toString(){
		return operator_name+" : publisher-"+publishers.toString();
	}
	public JoinOperator(List<Pair<Integer,String>> Info
			,Map<Pair<Integer,Integer>, Pair<String,Pair>> Cross){
		publishers = new ArrayList<>();
		TupleOfLeft = new ArrayList<>();
		Results = new LinkedList<>();
		CrossTbCdt = Cross;
		JoinInfo = new HashMap<>();
		for (Pair<Integer,String> p : Info){
			int corTID = p.getLeft();
			String attr = p.getRight();
			JoinInfo.put(corTID, attr);
		}
		isOpen = false;
	}

	public void attach(AlgebraNode node){
		this.publishers.add(node);
	}
	public void dettach(AlgebraNode node){
		this.publishers.remove(node);
	}


	//currently useless. Maybe used by selection operator through index.
	private class AttrComparator implements Comparator<List<Pair<Integer,Integer>>>{

		@Override
		public int compare(List<Pair<Integer,Integer>> l1, List<Pair<Integer,Integer>> l2) {
			ArrayList<Integer> tIDs = new ArrayList<>();
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
			TupleOfLeft.add(l);
		}
		//Pre-sorting based on to-join attributes
		//TupleOfLeft.sort(new AttrComparator());
		right.open();
		isOpen = true;
		start = next = 0;
		oldVal = null;
	}


	@Override
	public List<Pair<Integer,Integer>> getNext() {
		List<Pair<Integer,Integer>> receivedData = publishers.get(1).getNext();
		if (receivedData != null && isOpen){
			int tID = receivedData.get(0).getLeft();
			int rID = receivedData.get(0).getRight();
			byte[] tupleR = dbm.Get(tID, rID);
			Object val1 = dbm.getAttribute(tID, tupleR, JoinInfo.get(tID));
			if (oldVal !=null){
				if (val1.toString().compareTo(oldVal.toString()) > 0) start = next;
				else next = start;
			}
			for (int i = start; i < TupleOfLeft.size(); i++){
				List<Pair<Integer,Integer>> left = TupleOfLeft.get(i);
				List<Pair<Integer,Integer>> l = new ArrayList<Pair<Integer,Integer>>();
				int lrID = left.get(0).getRight();
				int ltID = left.get(0).getLeft();
				byte[] tupleL = dbm.Get(ltID,lrID);
				Object val = dbm.getAttribute(ltID, tupleL, JoinInfo.get(ltID));
				if (oldVal == null) oldVal = val;
				//Pre-break if it has passed the sorted value. Test needed
				if (val.toString().compareTo(val1.toString()) > 0) {
					break;
				}
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
				next ++;
			}
			oldVal = val1;
			if (!Results.isEmpty()) return Results.poll();
			else return this.getNext();
		}
		return receivedData;
	}

	@Override
	public void close() {
		for (AlgebraNode Node : publishers){
			Node.close();
		}
		isOpen = false;
	}

	public static String getOperator_name() {
		return operator_name;
	}

	public static void main(String[] args) {
		DBManager dbm = DBManager.getInstance();
		Parser p = new Parser("Query","select Country.code from Country, City on Country.code = city.CountryCode"
				+ " where 0.4 * Country.population <= city.population");
		JoinOperator j1 = new JoinOperator(p.getJInfo(),p.getCrossTable());
		for (Relation r : p.getRelations()){
			j1.attach(r);
		}
		System.out.println(j1);
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
