package project;


import project.relations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by vincent on 11/14/15.
 */
public class Pipline {
	//Single Table Selection.The Integer in the outer Map is the groupno.
	//Pair<Operator,Pair<Expr,Expr>> Operator:" >, < >=, <=, !=, ="
    private List<Relation> Relations;
    private Map<Pair<Integer,Integer>,Pair<String,Pair>> crosstab;
    private Map<Integer,List<Pair>> Dispatched;// Here Dispatched only supports and .
    private Map<Integer,List<String>> attrnames; //Projection Info. Integer for tID, List<String> for corresponding attrs
    private List<Pair<Integer,String>> On_Conditions; // Join info. Integer for tID, String for Attrname
    private AlgebraNode root;
    public Pipline(Parser p) throws Exception {
    	Dispatched = p.getDispatched().get(0);
    	attrnames = p.getAttrnames();
    	On_Conditions = p.getJInfo();
        Relations=p.Relations;
        crosstab=p.getCrossTable();
        construct();
    }
    private void construct() throws Exception {
    	AlgebraNode top = null;
        if(On_Conditions.size()>2 || On_Conditions.size()%2 !=0){
            throw new Exception("Not two tables join or currently not supported!");
        }
        //Here we assumes at most only one join happens.
        ProjectOperator ProjNode=new ProjectOperator(attrnames);
        if (Relations.size() > 1){
            JoinOperator joinNode=new JoinOperator(On_Conditions, crosstab);
            List<SelectOperator> Selections = new ArrayList<SelectOperator>();
            for (Relation r : Relations){
            	if(Dispatched != null){
            	if (Dispatched.containsKey(r.getRelation_id())){
            		SelectOperator SlctNode=new SelectOperator(Dispatched.get(r.getRelation_id()),null);
            		SlctNode.attach(r);
            		top = SlctNode;
            	}else{
            		top = r;
            	}
            	}
          		joinNode.attach(top);
            }
            top = joinNode;
        }else{//Only one table
        	Relation r= Relations.get(0);
        	if (Dispatched.containsKey(r.getRelation_id())){
            	SelectOperator SlctNode=new SelectOperator(Dispatched.get(r.getRelation_id()),null);
            	SlctNode.attach(r);
            	top = SlctNode;
        	}
        }
        ProjNode.attach(top);
        root=ProjNode;
    }
    public void exec(){
    	DBManager dbm = DBManager.getInstance();
        root.open();
        List<Pair<Integer,Integer>> l;
    	while( (l = root.getNext()) != null){

    	}
    }

    public static void main(String[] args){
        Parser par = new Parser("select Country.code,Country.name,city.name from Country, City on Country.code = city.CountryCode"
    			+ " where 0.4 * Country.population <= city.population and city.population < 1000000");

        try {
            Pipline p= new Pipline(par);
            System.out.println((ProjectOperator)p.root);
           p.exec();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
