package project;

import java.io.IOException;
/**
 * @title: Storage data management interface
 * @author wangqian
 *
 */

public interface Storage {

	// The size of data file
	int DATA_SIZE = 4096000;
	// The size of metadata file
	int METADATA_SIZE = 1024000;

	/**
	 * write the data into file
	 * @param fileName: data file name
	 * @param data: data bytes
	 * @throws Exception
	 */
	void writeData(String fileName, byte[] data,DBManager dbm) throws Exception;

	/**
	 * read the data file into memory
	 * @param fileName: data file name
	 * @return data list
	 * @throws IOException
	 */
	byte[] readData(String fileName, DBManager dbm) throws IOException;

	/**
	 * write the index into metadata
	 * @param fileName: data file name
	 * @throws Exception
	 */
	void writeMetaData(String fileName, DBManager dbm) throws Exception;

	/**
	 * read indexes from metadata
	 * @param fileName: data file name
	 * @return index list
	 * @throws IOException
	 */
	byte[] readMetaData(String fileName, DBManager dbm) throws IOException;
}
