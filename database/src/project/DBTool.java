package project;

import project.relations.UpdateOperator;
import test.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DBTool {

    private DBTool(){}

    private static void showTab(DBManager dbm){
        if(dbm==null){System.out.println("No db file");return;}
        for(int tid:dbm.getTabMeta().keySet()){
            System.out.print(dbm.getTabMeta().get(tid).get(0).getRight()+" ");
        }
        System.out.print('\n');
    }
    private static void showSchema(DBManager dbm,String tName){
        if(dbm==null){System.out.println("No db file");return;}
        int tid=tabNameToID(dbm,tName);
        for(int i=1;i<dbm.getTabMeta().get(tid).size();i++){
            System.out.print(dbm.getTabMeta().get(tid).get(i).getLeft()+" ");
        }
        System.out.print('\n');
    }

    public static void showWrapped(DBManager dbmanager){
        if (dbmanager!=null) {
            int freeSize = Storage.DATA_SIZE - dbmanager.get_DATA_USED();
            java.util.List<Pair<Integer, Integer>> al = DBTool.freelist(dbmanager.getClusteredIndex());
            DBTool.show(DBTool.minusList(new Pair<>(0, Storage.DATA_SIZE), al), freeSize);
            System.out.println("Keys in database are:");
            for (Integer k : dbmanager.getClusteredIndex().keySet()) System.out.print(k+" ");
            System.out.println("\n");
        }
    }

    private static void show(List<Pair<Integer, Integer>> freelist, int free){
        if (free==0){
            System.out.println("Total space is " + Storage.DATA_SIZE + "byte(s).\nUsed space is "
                    + Storage.DATA_SIZE + "byte(s).\nUnused is " + 0 + "byte(s).");
            System.out.println("Free space location:");
            System.out.println("---[]--- Database is full!");}
        else if(freelist==null) {System.out.println("metadata disorder");}
        else {
            int total = Storage.DATA_SIZE;
            int[] lset = new int[freelist.size()];
            int[] rset = new int[freelist.size()];
            for (int i = 0; i < freelist.size(); i++) {
                lset[i] = freelist.get(i).getLeft();
                rset[i] = freelist.get(i).getRight() - 1;
            }
            System.out.println("Total space is " + total + "byte(s).\nUsed space is "
                    + (total - free) + "byte(s).\nUnused is " + free + " byte(s).");
            System.out.println("Free space location:");
            for (int i = 0; i < freelist.size(); i++) {
                System.out.println("---[" + lset[i] + " , " + (lset[i] + rset[i]) + "]---");
            }
        }
    }

    private static List<Pair<Integer,Integer>> freelist(Map<Integer, Index> tab){
        List<Pair<Integer,Integer>> loopList=new ArrayList<>();
        for (int key:tab.keySet()) {
            Index tmpList=tab.get(key);
            loopList.addAll(tmpList.getIndexList().stream().collect(Collectors.toList()));
        }
        Index sortIndex =new Index();
        sortIndex.setPhysAddrList(loopList);
        sortIndex.sortPairs();
        return sortIndex.getIndexList();
    }

    private static List<Pair<Integer,Integer>> minusList(Pair<Integer,Integer> F,List<Pair<Integer,Integer>> L){
        List<Pair<Integer,Integer>> rList=new ArrayList<>();
        if(L.size()==0){
            rList.add(F);
            return rList;
        }
        else if((L.get(0).getLeft()<F.getLeft())
                ||(L.get(L.size()-1).getLeft()+L.get(L.size()-1).getRight()-1>F.getRight())){
            return null;
        }

        int ii=F.getLeft(),ll;
        for(Pair<Integer,Integer> p:L){
            ll=p.getLeft();
            if(ll==ii){
                ii=ll+p.getRight();
            } else if(ii<ll){
                rList.add(new Pair<>(ii, ll - ii));
                ii=ll+p.getRight();
            }
            else{
                System.out.println("Bad pairs!!!");
                return null;
            }
        }
        if (ii<F.getRight())
            rList.add(new Pair<>(ii,F.getRight()-ii));
        return rList;
    }

    //find tid through table names
    public static int tabNameToID(DBManager dbm, String tables) {
        int tid=-1;
        for (int id : dbm.getTabMeta().keySet()) {
            if (dbm.getTabMeta().get(id).get(0).getRight().equals(tables.toLowerCase())) {
                tid = id;
                break;
            }
        }
        return tid;
    }

    public static void shell(String dbfilename){
        System.out.println("Welcome! This is a group project of cs542 at WPI\nType help to see commands.");
        DBManager dbmanager=DBManager.getInstance(dbfilename);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String[] s;
        waiting_command:
        while(true){
            try {
                String input=Condition.removeExtraSpace(br.readLine()).toLowerCase();
                s=input.split(" ");
                if(s.length>10) {System.out.println("Too many argument!");continue ;}
                switch (s[0]) {
                    case "quit":case "q":           System.out.println("Now Quit Shell.");break waiting_command;
                    case "show":
                        if (s.length>1) dbmanager=DBManager.getInstance(s[1]);
                        showWrapped(dbmanager);
                        DBManager.close();break;
                    case "fragment":case "f":       TestFragmentation.main(null);break;
                    case "Concurrency":case "c":    TestConcurrency.main(null);break;
                    case "clear":case "cl":         Clear.main(null);break;
                    case "readcsv":case"r":         TestReadCSV.main(null);break;
                    case "mtable":case"m":          TestMultab.main(null);break;
                    case "pipeline":case"p":        Pipeline.main(null);break;
                    case ".table":case".t":         showTab(dbmanager);break;
                    case ".schema":case".s":        if(s.length>1)showSchema(dbmanager,s[1]);
                                                    else System.out.println(
                                                    ".schema|.s <tablename>\t\tshow table attribute names");
                                                    break;
                    case "select":
                        if(s.length>3&&s[2].equals("from")){
                            if(s[3].matches("[\\s\\S]+,[\\s\\S]+")){
                                Pipeline p=new Pipeline(new Parser("Query",input));
                                p.exec();
                            }else {
                                dbmanager = DBManager.getInstance();
                                if (s.length > 5 && s[4].equals("where")) {
                                    dbmanager.printQuery(tabNameToID(dbmanager, s[3]),
                                            dbmanager.tabProject(s[3], s[1]), new Condition(input.split("where")[1]));
                                } else if (dbmanager != null) {
                                    dbmanager.printQuery(tabNameToID(dbmanager, s[3]),
                                            dbmanager.tabProject(s[3], s[1]), new Condition());
                                }
                            }
                        }break;
                    case "update":
                        Parser p = new Parser("update", input);
                        UpdateOperator up = new UpdateOperator(p.getDispatched(),p.getUpinfo());
                        up.attach(p.getRelations().get(0));
                        up.open();
                        up.getNext();
                        dbmanager.Commit();
                        break;
                    case "create":
                        dbmanager=DBManager.getInstance();
                        if(s.length>2)
                            switch (s[1]){
                                case "index":
                                    if(s.length==3){
                                        String[] schema=s[2].split("\\(|\\)");
                                        if(schema.length==2)
                                            dbmanager.createIndex(schema[0],schema[1]);
                                        else System.out.println("Can't resolve SQL");
                                    }
                                    else System.out.println("Can't resolve SQL");
                                    break;
                                case "table":
                                    if(s.length>=3) {
                                        String[] schema = input.split("table\\s|\\(|\\)");
                                        if(schema.length==3)
                                            dbmanager.createTab(schema[1],schema[2]);
                                        else System.out.println("Can't resolve SQL");
                                    }
                                    break;
                                default:System.out.println("Can't resolve SQL");
                            }
                        else System.out.println("Not Enough parameters!");break;
                    case "help":case "h":
                        System.out.println("Help Information:\nq|Q|quit|Quit\t\tquit the shell\n" +
                                "show [<file name>]\tshow the space of the database, default file is 'cs542.db'.\n" +
                                "fragment|f\t\t\tvalidate fragment\n" +
                                "concurrency|c\t\tvalidate concurrency control\n" +
                                "clear|cl\t\t\tclear the database\n"+
                                "readcsv|r\t\t\tread movies file and create table\n" +
                                "mtable|m\t\t\tread city and country table\n" +
                                "pipepline|p\t\t\tshow an pipeline example" +
                                "\n------SQL-----\n"+
                                "select <attribute(s)> from <table> [where <condition(s)>]\n" +
                                "update <table> set <attribute(s)> where [<condition(s)>]\n" +
                                "create index <table(attribute name[, ...])>\n" +
                                ".table|.t\t\t\t\t\tshow table name in database\n" +
                                ".schema|.s <table name>\t\tshow table attribute names");break;
                    default:System.out.println("Can't find the command '"+s[0]+"'\nyou may use 'help' command");
                }
            } catch (Exception e) {
                e.printStackTrace();
                //System.out.print(e.getMessage()+'\n');
            }
        }
    }

    public static void main(String[] args){shell("cs542.db");}
}
