package test;

import project.Index;
import project.IndexHelper;
import project.IndexHelperImpl;
import project.Pair;

import java.util.*;

/**
 * Created by vincent on 9/25/15.
 */
public class TestByteIndex extends IndexHelperImpl {
    public static void main(String args[]){
        //initial an index
        Index index = new Index();
        index.setKey(256 * 256 * 256 * 127 + 256 * 256 * 127 + 256 * 127 + 127);//not max key; but max in form of byte[]
        Pair<Integer, Integer> p1 = new Pair<Integer, Integer>(256*256*256,50);
        Pair<Integer, Integer> p2 = new Pair<Integer, Integer>(256*256,40);
        Pair<Integer, Integer> p3 = new Pair<Integer, Integer>(256,50);
        List<Pair<Integer, Integer>> l = new ArrayList<Pair<Integer, Integer>>();
        l.add(p1);
        l.add(p2);
        l.add(p3);
        index.setPhysAddrList(l);

        //put an index in map
        Map<Integer, Index> map=new Hashtable<>();
        map.put(index.getKey(), index);

        //another index
        index = new Index();
        index.setKey(20);
        index.setPhysAddrList(l);
        map.put(index.getKey(), index);
        System.out.println("input map: "+map);

        //test printing output byte[]
        IndexHelper indexHelper = new TestByteIndex();
        byte[] returnedbytes = new byte[0];
        try {
            returnedbytes = indexHelper.indexToBytes(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (byte aBytecode : returnedbytes) {
            System.out.print(aBytecode + " ");
        }
        System.out.println();
        System.out.println("length of metadata is: "+returnedbytes.length);

        //test byte to indexMap
        Map<Integer, Index> returnedMap=indexHelper.bytesToIndex(returnedbytes);
        System.out.println("returned map(pair list are sorted): "+returnedMap);

    }
}
