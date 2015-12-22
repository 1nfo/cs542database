package project.relations;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import project.Index;
import project.DBManager;
import project.DBTool;
import project.Pair;
import java.util.Map;

public class Relation implements AlgebraNode{
	private boolean isOpen;
	private int relation_id;
	private String relation_name;
	private List<String> Attrnames;
	private List<Integer> rIDs;
	private int current;
	private DBManager dbm = DBManager.getInstance();

	public String toString(){
		return "Relation: "+relation_name+"-"+relation_id;
	}

	public Relation(String rname){
		this.relation_name = rname;
		try{
			int tid = DBTool.tabNameToID(dbm,rname);
			if (tid >= 0) relation_id = tid;
		}catch (Exception e){
			e.printStackTrace();
		}
		this.rIDs = new ArrayList<>();
		this.Attrnames = new ArrayList<>();
		isOpen = false;
	}

	private class AttrComparator implements Comparator<Integer>{

		@Override
		public int compare(Integer r1, Integer r2) {
			int cmp = -1;
			int i = 0;
			for (String attr : Attrnames){
				byte[] tuple1 = dbm.Get(relation_id, r1);
				byte[] tuple2 = dbm.Get(relation_id, r2);
				Object val1 = dbm.getAttribute(relation_id, tuple1, attr);
				Object val2 = dbm.getAttribute(relation_id, tuple2, attr);
				cmp = val1.toString().compareTo(val2.toString());
				if (cmp != 0) break;
			}
			return cmp;
		}
	}

	@Override
	public void open() {
		//table-scan;pre-fetch everything
		if (Attrnames.size() > 0 && dbm.isAttrIndex(relation_id, Attrnames)){
				rIDs = dbm.Indexsort(relation_id, Attrnames);
		}else {
			Map<Integer, Index> cIndex = dbm.getClusteredIndex();
			for (int i =1; i < cIndex.size(); i ++){
				int tID = cIndex.get(i).getTID();
				if (relation_id == tID) rIDs.add(i);
			}
			if (Attrnames.size() > 0){
				rIDs.sort(new AttrComparator());}

		}
		current = 1;
		isOpen = true;
	}

	@Override
	public List<Pair<Integer,Integer>> getNext() {
		//May need to change later. Depends on how we send the data;
		if (hasNext() && isOpen) {
			int rID = rIDs.get(current - 1);
			current ++;
			List<Pair<Integer,Integer>> l = new ArrayList<Pair<Integer,Integer>>();
			l.add(new Pair(relation_id,rID));
			return l;
		}
		else return null; // null represents notFound
	}

	@Override
	public void close() {
		isOpen = false;
		rIDs = null;
	}

	public int getRelation_id() {
		return relation_id;
	}

	public String getRelation_name() {
		return relation_name;
	}

	public void addSortattrs(String attr){
		Attrnames.add(attr);
	}

	public static void main(String[] args) {
		Relation r1 = new Relation("city");
		r1.addSortattrs("countrycode");
		r1.open();
		List<Pair<Integer,Integer>> l;
		while((l = r1.getNext()) != null){
			System.out.println(l.get(0).getRight());
		}
	}

	private boolean hasNext() {
		return current <= rIDs.size();
	}

}
