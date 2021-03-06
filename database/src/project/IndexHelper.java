package project;

import java.util.List;
import java.util.Map;

public interface IndexHelper {
	/**
	 * The start sign in the metadata file
	 */
	byte START_SIGN = Index.getSign();
	byte TAB_START_SIGN = -2;
	byte ATTRINDEX_START_SIGN=-3;
	byte TAB_META_RESERVED=3;

	/**
	 * find the indexes list of free spaces based on the data size Get all free space based on the delete sign and
		 * amount blocks to find some enough space to save the data.
	 * @param size : the size of data that to be saved
	 * @return The indexes list of data file
	 */
	List<Pair<Integer, Integer>> findFreeSpaceIndex(int size) ;

	/**
	 * Get the index buffer in Map type
	 * key is the start index of local part on data file
	 * value is the end index of local part on data file
	 * @return sorted list
	 */
	List<Integer> getSortedIndexList();
	/**
	 * This method is to split the original data into pieces based on the findFreeSpaceIndex method in order to save a big data
	 * @param data_to_save: the data to be saved
	 * @param indexes: the indexes indicate the free space can be used to save the data
	 */
	void splitDataBasedOnIndex(byte[] data_to_save, List<Pair<Integer,Integer>> indexes);

	/**
	 * convert the index Hashtable to byte array
	 * @param indexes storage index
	 * @return the meta data byte array
	 */
	byte[] indexToBytes(Map<Integer, Index> indexes) throws Exception;

	/**
	 * convert the meta data to the index Hashtable
	 * @param metadata byte array
	 * @return The Hashtable of indexes, key is the index key, value is the index object
	 */
	Map<Integer, Index> bytesToIndex(byte[] metadata);

	int getIndexSize(List<Pair<Integer,Integer>> pairs_list);
	//transform table metadata into byte array
	byte[] tabMetaToBytes(Map<Integer, List<Pair>> tabMetadata);
	//transform back
	Map<Integer, List<Pair>>  bytesToTabMeta(byte[] metadata);
	//transform hash table into byte array
	byte[] hastabToBytes(Map hashTab);
	//transform back
	Map<Integer, Map<String, AttrIndex>> bytesToHashtab(byte[] metadata);
}