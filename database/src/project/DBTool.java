package project;


import test.Clear;
import test.TestConcurrency;
import test.TestFragmentation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DBTool {

    private DBTool(){}

    public static void showWrapped(DBManager dbmanager){
        if (dbmanager==null) ;
        else {
            int freeSize = Storage.DATA_SIZE - dbmanager.get_DATA_USED();
            java.util.List<Pair<Integer, Integer>> al = DBTool.freelist(dbmanager.getIndexBuffer());
            DBTool.show(DBTool.minusList(new Pair<>(0, Storage.DATA_SIZE), al), freeSize);
            System.out.println("Keys in database are:");
            for (Integer k : dbmanager.getIndexBuffer().keySet()) System.out.print(k+" ");
            System.out.println("\n");
        }
    }

    private static void show(List<Pair<Integer, Integer>> freelist, int free){
        if (free==0){
            System.out.println("Total space is " + Storage.DATA_SIZE + "byte(s).\nUsed space is " + Storage.DATA_SIZE + "byte(s).\nUnused is " + 0 + "byte(s).");
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
            System.out.println("Total space is " + total + "byte(s).\nUsed space is " + (total - free) + "byte(s).\nUnused is " + free + "byte(s).");
            System.out.println("Free space location:");
            for (int i = 0; i < freelist.size(); i++) {
                System.out.println("---[" + lset[i] + " , " + (lset[i] + rset[i]) + "]---");
            }
        }
    }

    private static List<Pair<Integer,Integer>> freelist(Map<Integer,Index> tab){
        List<Pair<Integer,Integer>> looplist=new ArrayList<>();
        for (int key:tab.keySet()) {
            Index tmplist=tab.get(key);
            looplist.addAll(tmplist.getIndexes().stream().collect(Collectors.toList()));
        }
        Index sortIndex=new Index();
        sortIndex.setIndexes(looplist);
        sortIndex.sortpairs();
        return sortIndex.getIndexes();
    }

    private static List<Pair<Integer,Integer>> minusList(Pair<Integer,Integer> F,List<Pair<Integer,Integer>> L){
        List<Pair<Integer,Integer>> rlist=new ArrayList<>();
        if(L.size()==0){
            rlist.add(F);
            return rlist;
        }
        else if((L.get(0).getLeft()<F.getLeft())
                ||(L.get(L.size()-1).getLeft()+L.get(L.size()-1).getRight()-1>F.getRight())){
            return null;
        }

        int ii=F.getLeft(),ll=0;
        for(Pair<Integer,Integer> p:L){
            ll=p.getLeft();
            if(ll==ii){
                ii=ll+p.getRight();
            } else if(ii<ll){
                rlist.add(new Pair<>(ii,ll-ii));
                ii=ll+p.getRight();
            }
            else{
                System.out.println("Bad pairs!!!");
                return null;
            }
        }
        if (ii<F.getRight()){
            rlist.add(new Pair<>(ii,F.getRight()-ii));
        }

        return rlist;
    }

    private static void shell(){
        System.out.println("Welcome! This is a group project of cs542 at WPI\nType help to see commands.");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String[] s;
        waiting_command:
        while(true){
            try {
                s=br.readLine().split(" ");
                if(s.length>3) {System.out.println("Too many argument!");continue ;}
                switch (s[0].toLowerCase()) {
                    case "quit":case "q":           System.out.println("Now Quit Shell.");break waiting_command;
                    case "show":
                        DBManager dbmanager;
                        if (s.length==1) dbmanager = DBManager.getInstance();
                        else dbmanager=DBManager.getInstance(s[1]);
                        showWrapped(dbmanager);
                        DBManager.close();break;
                    case "fragment":case "f":       TestFragmentation.main(null);break;
                    case "Concurrency":case "c":    TestConcurrency.main(null);break;
                    case "clear":case "cl":         Clear.main(null);break;
                    case "help":
                        System.out.println("Help Information:\nq|Q|quit|Quit\t\tquit the shell\n" +
                                "show [<filename>]\tshow the space of the database, default file is 'cs542.db'.\n" +
                                "fragment|f\t\t\tvalidate fragment\n" +
                                "concurrency|c\t\tvalidate concurrency control\n" +
                                "clear|cl\t\t\tclear the database");

                        break;
                    default:System.out.println("Can't find the command '"+s[0]+"'!\nyou may use 'help' command");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args){
        shell();
    }
}
