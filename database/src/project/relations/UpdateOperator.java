package project.relations;

import project.DBManager;
import project.Pair;
import project.ExpressionParser;
import project.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class UpdateOperator  implements AlgebraNode{
    private boolean isOpen;
    //private static String operator_name = "Update";
    private AlgebraNode publisher;
    private Map<Integer,List<Pair>> SingleTBCdt;
    private List<Pair<String,ExpressionParser>> To_Update;

    public UpdateOperator(Map<Integer, Map<Integer,List<Pair>>> conditions, List<Pair<String,ExpressionParser>> updateinfo){
        publisher = null;
        SingleTBCdt = new HashMap<>();
            for (Map.Entry<Integer, Map<Integer, List<Pair>>> entry : conditions.entrySet()) {
                List<Pair> l = new ArrayList<>();
                entry.getValue().values().forEach(l::addAll);
                SingleTBCdt.put(entry.getKey(), l);
            }
        To_Update = updateinfo;
    }

    public void attach(AlgebraNode node){this.publisher = node;
    }
    public void dettach(AlgebraNode node){
        this.publisher = null;
    }

    @Override
    public void open(){
        publisher.open();
        isOpen = true;
    }

    @Override
    public List<Pair<Integer,Integer>> getNext(){
        DBManager dbm = DBManager.getInstance();
        List<Pair<Integer,Integer>> receivedData;
        while ((receivedData = publisher.getNext()) != null){
            int tID = receivedData.get(0).getLeft();
            int rID = receivedData.get(0).getRight();
            try{
                byte[] tuple = dbm.Get(tID,rID);
                boolean isvalid = true;
                for (List<Pair> subconds : SingleTBCdt.values()){
                    if (SelectOperator.handleCondition(subconds, dbm, rID, rID)){
                        isvalid = false;
                        break;
                    }
                }
                if (isvalid) {
                    for (Pair<String, ExpressionParser> p : To_Update) {
                        String attr = p.getLeft();
                        ExpressionParser parser = p.getRight();
                        parser.parse(tuple, dbm);
                        Object newval = parser.getExpr().getRight();
                        dbm.setAttribute(rID, attr, newval);
                    }
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void close() {

    }

    public static void main(String[] args){
        DBManager dbm = DBManager.getInstance("cs542.db");
        Parser p = new Parser("update", "Update Country set population = population * 1.02");
        UpdateOperator up = new UpdateOperator(p.getDispatched(),p.getUpinfo());
        p.getRelations().forEach(up::attach);
        up.open();
        up.getNext();
        p = new Parser("update", "Update city set population = population * 1.02");
        up = new UpdateOperator(p.getDispatched(),p.getUpinfo());
        p.getRelations().forEach(up::attach);
        up.open();
        up.getNext();
        dbm.Failure("redo");
    }
}
