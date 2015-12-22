package project;

import java.io.*;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;


/**
 * Implementation of the Storage Interface.
 * Data Structure: 	first is 4MB space is to save the real data, second is 1m space is to save the metadata information.
 *
 * MetadataFile:    first byte is the sign of delete (1 is deleted, 0 is not deleted);
 * 					second byte is the key;
 * 					third byte is the total number of the indexes if the data cannot save in a single consecutive space
 * 					fourth byte is the index of the data;
 * 					fifth byte is the amount of bytes
 * 					if the data have to save in several parts, then there will have other indexes and amount of bytes
 * 					information.
 * 					for example: (0,1,1,10,1024) indicates this index metadata is a valid, data key is 1, there has only
 * 					1 index pair, index in data file is 10, data size is 1024.
 * Data File:		Total size is 4MB bytes.
 */

public class StorageImpl implements Storage {

	public StorageImpl(){}

	//return the byte array data part of the given filename
	public byte[] readData(String fileName,DBManager dbm) throws IOException{
		byte[] data= readOutDataBase(fileName,dbm);
		return Arrays.copyOfRange(data,0,DATA_SIZE);
	}

	//write given byte array data into the given file with
	public void writeData(String fileName, byte[] data,DBManager dbm) throws Exception {
		// Verify the data size cannot exceed the DATA_SIZE
		if(data.length > DATA_SIZE)
			throw new Exception("The data size is exceed the requirement!");
		writeIntoDataBase(fileName,data,true,dbm);
	}

	//write given metadata into file
	public void writeMetaData(String fileName,DBManager dbm) throws Exception {
		//merge three kinds of meta first
		IndexHelper ih=new IndexHelperImpl();
		byte[] index_meta=ih.indexToBytes(dbm.getClusteredIndex());
		byte[] table_meta=ih.tabMetaToBytes(dbm.getTabMeta());
		byte[] uncl_index_meta=ih.hastabToBytes(dbm.getAttrIndex());
		byte[] metadata=new byte[index_meta.length+table_meta.length+uncl_index_meta.length];
		System.arraycopy(index_meta,0,metadata,0,index_meta.length);
		System.arraycopy(table_meta,0,metadata,index_meta.length,table_meta.length);
		System.arraycopy(uncl_index_meta,0,metadata,index_meta.length+table_meta.length,uncl_index_meta.length);

		// Verify the data size cannot exceed the METADATA_SIZE
		if (metadata.length > METADATA_SIZE)
			throw new Exception("The metadata size is exceed the requirement!");
		writeIntoDataBase(fileName,metadata,false,dbm);
	}

	//read metadata out of given file
	public byte[] readMetaData(String fileName,DBManager dbm) throws IOException{
		byte[] data=readOutDataBase(fileName,dbm);
		return Arrays.copyOfRange(data,DATA_SIZE,data.length);
	}

	//read information in database, if it does not exists, then create.
	private byte[] readOutDataBase(String fileName,DBManager dbm) throws IOException{
		InputStream inputstream;
		byte[] out;
		try {
			inputstream = new FileInputStream(fileName);
			out=IOUtils.toByteArray(inputstream);
		} catch (FileNotFoundException e) {
			System.out.println("File "+dbm.getDBName()+" created");
			FileOutputStream fos=null;
			try{
				fos = new FileOutputStream(fileName);
				byte[] empty=new byte[Storage.DATA_SIZE];
				for (int i = 0; i <empty.length;i++) {
					empty[i]=0;
				}
				fos.write(empty);
			}catch(Exception ee){
				ee.printStackTrace();
			}finally{
				if(fos != null) fos.close();
			}
			out=readOutDataBase(fileName,dbm);
		}
		return out;
	}

	//write data or metadata into a file
	private void writeIntoDataBase(String fileName,byte[] writeContent, boolean isData, DBManager dbm) throws IOException{
		byte[] out;
		if(isData){
			out = readOutDataBase(fileName,dbm);
			System.arraycopy(writeContent, 0, out, 0, writeContent.length);
		}
		else{
			out=new byte[DATA_SIZE+writeContent.length];
			byte[] data=readData(fileName,dbm);
			System.arraycopy(data,0,out,0,data.length);
			System.arraycopy(writeContent,0,out,data.length,writeContent.length);
		}
		FileOutputStream fos = null;
		try{
			fos = new FileOutputStream(fileName);
			fos.write(out);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(fos != null) fos.close();
		}
	}
}
