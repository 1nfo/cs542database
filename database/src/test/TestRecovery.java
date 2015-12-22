package test;

import project.DBManager;
import project.DBTool;
import project.relations.UpdateOperator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Created by vincent on 11/28/15.
 */
public class TestRecovery {
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if(!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        }
        finally {
            if(source != null) {
                source.close();
            }
            if(destination != null) {
                destination.close();
            }
        }
    }

    public static void main(String[] args){
        String org="cs542.db",cpy="cs542A.db";
        System.out.println("Recovery validation:");
        System.out.println("copy "+org+" "+cpy+"");
        try {
            copyFile(new File(org),new File(cpy));
            System.out.println("update by 2%, please wait ...");
            UpdateOperator.main(null);
            System.out.println("Done!");
            DBManager.close();
            System.out.println("Now open "+cpy+" file  in shell\ncheck value with SQL:\nselect population from city\nselect population from country");
            DBTool.shell(cpy);
            System.out.println("Now open the backup"+org+" file in shell\ncheck value with SQL:\nselect population from city\nselect population from country");
            DBTool.shell(org);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
