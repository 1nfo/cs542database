package project;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

/**
 * DBManager is to manager database, including manage storage data and the indexes
 * @author wangqian
 *
 */
public class DBManager {
	Logger logger = (Logger) LogManager.getLogger();
   
	private static DBManager dbManager = null;
	
	// database is to be contain the data
	private byte[] data;
	
	// Keep track of how much space is used in data
	private int DATA_USED;

	// Keep track of how much space is used in index metadata
	private int INDEXES_USED;

	// Keep track of how much space is used in table metadata
	private int TABMETA_USED;

	// Keep track of how much space is used in metadata
	private int METADATA_USED;

	// Size of data and metadata
	private static final int DATA_SIZE = Storage.DATA_SIZE;
	public static final int METADATA_SIZE = Storage.METADATA_SIZE;

	// Names of Data and Metadata
	private static String DB_NAME;
	
	/**
	 * indexes is to be contain the indexes in the metadata
	 * Key is the index key
	 * List is the index of that key
	 */
	private Map<Integer, Index> indexes;

	private Map<Integer,List<Pair>> tabMetadata;
	
	//Locker controls the concurrency of the database.
	private DbLocker Locker;
	private Storage DBStorage;
	private IndexHelper indexHelper;
	private DBManager(String dbname){
		DB_NAME=dbname;
	}

	public static String getDBName(){return DB_NAME;}
	public static DBManager getInstance(String dbName){
		if (dbManager == null){
			// Instantiate and Initialization of DBManager
			dbManager = new DBManager(dbName);
			dbManager.DBStorage = new StorageImpl();
			dbManager.indexHelper = new IndexHelperImpl();
			dbManager.Locker = new DbLocker();
			if(!new File(dbName).isFile()) {
				System.out.println("File "+DBManager.getDBName()+" doesn't exist\nWould you like to create a new one now?(Y/N)");
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				String is_YorN= null;
				try {
					is_YorN = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (is_YorN.toLowerCase().equals("y")){
					dbManager.readDatabase();
				}
				else dbManager=null;
			}
			else dbManager.readDatabase();
		}
		
		return dbManager;
	}
	public static DBManager getInstance(){
		return getInstance("cs542.db");
	}
	public static void close(){
		dbManager=null;
	}
	
	
	public void GetTable(int tableID) {
		
	}
	
	public void Put(int key, byte[] data) {
		/**
		 * In order to avoid during saving period rebooting, we save the data file first and then save the metadata.
		 * Saving process:
		 * 		1. There is only one thread to save data. The saving thread will occupy the data file. Other threads
		 * 			including reading threads will wait until the saving thread complete.
		 * 		2. Locate the saving index:  call the method of findFreeSpaceIndex
		 * 		3. Save the data to data part.
		 * 		4. Save the indexes information to metadata part.
		 * 		5. Update the metadata buffer in memory
		 * 
		 * Params:  key -- data key
		 * 			data -- data information
		 *
		 */ 
		try {
			Locker.writeLock();			
			logger.info("Attempting to put key: " + key + "to database");
            //if key already exists in the database, update it by removing it first.
			if (indexes.containsKey(key)){
				Remove(key);
			}
			// if database is going to be out of volume, block the put attempt.
			if (data.length + get_DATA_USED() > DATA_SIZE) {
				System.out.println("Not enough data space left. Put Attempt with key "
					+ key + " Failed.");
				return;
			}
					// Getting the index list of free space in data array;
					List<Pair<Integer,Integer>> index_pairs = indexHelper.findFreeSpaceIndex(data.length);
					indexHelper.splitDataBasedOnIndex(data, index_pairs);
					// Updating the Index map. If the metadata is out of volume, block the putting attempt.
					Index tmpindex = new Index();
					tmpindex.setKey(key);
					tmpindex.setIndexes(index_pairs);
					int indexsize = indexHelper.getIndexSize(index_pairs);
					if (indexsize + INDEXES_USED > METADATA_SIZE) {
						System.out.println("Not enough metadata space left. Put Attempt with key "
					+ key + " Failed.");
						return;
					}
					indexes.put(key, tmpindex);
					set_INDEXES_USED(get_INDEXES_USED() + indexsize);
					logger.info("Metadata buffer updated");
					
					// Writing the database onto the disk
					
					DBStorage.writeData(DB_NAME, this.data);
					set_DATA_USED(get_DATA_USED() + data.length);
					System.out.println("Data related to key is " + key + ", and size is " + data.length + " have written to " + DB_NAME);

					//concat two kinds of meta date
					byte[] tmp_meta1 = indexHelper.indexToBytes(indexes);
					byte[] tmp_meta2 = indexHelper.tabMetaToBytes(tabMetadata);
					byte[] metadata = new byte[tmp_meta1.length+tmp_meta2.length];
					System.arraycopy(tmp_meta1, 0, metadata, 0, tmp_meta1.length);
					System.arraycopy(tmp_meta2, 0, metadata, tmp_meta1.length, tmp_meta2.length);

					DBStorage.writeMetaData(DB_NAME, metadata);
					logger.info("Metadata updated on disk");
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally{
			try {
			Locker.writeUnlock();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
	}

	public byte[] Get(int key) {
		/**
		 * Returns the data that is mapped to the given key; If no
		 * such key exists in database, return null and Print a message
		 * to the console.
		 * 
		 * Querying Process:
		 * 		  1. There can be multiple threads reading the database. No threads
		 * 			 can write the database when there is at least one thread reading.
		 *  
		 *  Params:  key -- data key
		 * 
		 */
		byte[] databuffer = new byte[Storage.DATA_SIZE];
		byte[] returndata = null;
		try {
			Locker.ReadLock();
			logger.info("Attempting to get data mapped to key :" + key);
			if (indexes.containsKey(key)) {
				List<Pair<Integer, Integer>> index = indexes.get(key).getIndexes();
				//extracting the mapped data from the data in memory;
				int start = 0;
				for (Pair<Integer, Integer> p : index) {
					System.arraycopy(data, p.getLeft(), databuffer, start, p.getRight());
					start += p.getRight();
				}
				returndata = new byte[start];
				System.arraycopy(databuffer, 0, returndata, 0, start);
				logger.info("Data with key " + key + " is " + Arrays.toString(returndata));
			} else {
				System.out.println("No data with key "+ key +" exists in database.");
			}
		} catch (Exception e) {
			System.out.println("Interrupted while reading data");
			e.printStackTrace();
		} finally{
			Locker.ReadUnlock();
		}

		return returndata;	
	}

	public void Remove(int key) {
		/**
		 * Removes the mapped key - data relation in the database;
		 * If no such key exists, print an error message to the console.
		 * 
		 * Params:  key -- data key
		 */
		try {
			Locker.writeLock();
			logger.info("Attempting to remove the data with key :" + key);
			if (!indexes.containsKey(key)) {
				System.out.println("No data with key " + key + " exists in database.Failed to remove.");
			} else {
				// Removing the key in the metadata buffer and update the metadata file
				int[] tmp = getPairSize(indexes.get(key));
				indexes.remove(key);
				this.set_DATA_USED(get_DATA_USED() - tmp[0]);
				this.set_INDEXES_USED(get_INDEXES_USED() - tmp[1] );
				logger.info("Metadata buffer updated");

				//concat two kinds of meta date
				byte[] tmp_meta1 = indexHelper.indexToBytes(indexes);
				byte[] tmp_meta2 = indexHelper.tabMetaToBytes(tabMetadata);
				byte[] metadata = new byte[tmp_meta1.length+tmp_meta2.length];
				System.arraycopy(tmp_meta1, 0, metadata, 0, tmp_meta1.length);
				System.arraycopy(tmp_meta2, 0, metadata, tmp_meta1.length, tmp_meta2.length);

				DBStorage.writeMetaData(DB_NAME, metadata);
				logger.info("Metadata updated on disk");
				System.out.println("Data with key " + key + " is removed.");
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally {
			try {
			Locker.writeUnlock();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		
	}

	public void readDatabase(){
		//Read the database and upload the data into memory
		byte[] metadata;
		try{
			data = DBStorage.readData(DB_NAME);
			logger.info("Data read in memory");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to read Data into memory");
		}
		try{
			metadata = DBStorage.readMetaData(DB_NAME);
			indexes = indexHelper.bytesToIndex(metadata);
			tabMetadata=indexHelper.bytesToTabMeta(metadata);
			indexToSize();
			logger.info("Free Space left is:" + (DATA_SIZE - DATA_USED));
			logger.info("Free Meta Space left is:" + (METADATA_SIZE - INDEXES_USED));
			logger.info("Metadata read in Memory");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to read MataData into memory");
		}
	}
	
	private void indexToSize() {
		int indexSize = 0;
		int dataSize =0;
		int[] tmp;
		for (Map.Entry<Integer, Index> m : indexes.entrySet()) {
			tmp = getPairSize (m.getValue());
			dataSize += tmp[0];
			indexSize += tmp[1];
		}
		this.set_DATA_USED(dataSize);
		this.set_INDEXES_USED(indexSize);
	}
	
	private static int[] getPairSize(Index index) {
		int[] result = new int[2];
		result[1] += Index.getReservedSize() + 1 + Index.getKeySize()+
				2 * Integer.BYTES * index.getIndexes().size();
		for (Pair<Integer, Integer> p : index.getIndexes()) {
			result[0] += p.getRight();
		}
		return result;
	}
	
	public byte[] getData() {
		return data;
	}

	public void setData(byte[] database) {
		this.data = database;
	}

	public Map<Integer, Index> getIndexBuffer() {
		return indexes;
	}

	public void createTabMete(String tableName,List<Pair> attr){
		List<Pair> pairs = new ArrayList<>();
		//generate tid
		int tid=0;
		while(tabMetadata.keySet().contains(tid))
			tid++;
		pairs.add(new Pair<>(tid,tableName));
		pairs.addAll(attr);
		tabMetadata.put(tid, pairs);
	}

	public Map<Integer,List<Pair>> getTabMeta(){return tabMetadata;}

	public int get_DATA_USED() {
		return DATA_USED;
	}
	
	public void set_DATA_USED(int size) {
		DATA_USED = size;
	}
	
	public int get_INDEXES_USED() {
		return INDEXES_USED;
	}
	
	public void set_INDEXES_USED(int size) {
		INDEXES_USED = size;
	}

	public void set_TABMETA_USED(DBManager dbm) {
		int count = 0;
		for (int tid : dbm.tabMetadata.keySet())
			count+=dbm.tabMetadata.get(tid).size();
		TABMETA_USED =count*(24);
	}

	public int get_TABMETA_USED(){return TABMETA_USED;}

	public void set_METADATA_USED(){METADATA_USED=TABMETA_USED+INDEXES_USED;}

	public int get_METADATA_USED(){return METADATA_USED;}
	
	public int getFreeSpace() {
		return DATA_SIZE - DATA_USED;
	}
	
	public void clear() {
		// for clearing the database
		try {
			Locker.writeLock();
			//Setting the metadata buffer in memory to an empty Hashtable
			indexes = new Hashtable<Integer, Index>();
			logger.info("Clear : Metadata buffer updated");
			set_INDEXES_USED(0);
			byte[] metadata_buffer = new byte[0];
			DBStorage.writeMetaData(DB_NAME, metadata_buffer);
			logger.info("Metadata updated on disk");
			set_DATA_USED(0);
			//System.out.println("Database Cleared!");
			logger.info("Current Free Space is " + this.getFreeSpace());
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			Locker.writeUnlock();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
