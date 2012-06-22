/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

/**
 * Iterate through creates and deletes of versions of data records
 * for a specific table.
 */
public interface HistoryIterator {

	void rewind();

	/**
	 * @return null at eof, else a pair of Record's
	 * the first containing the date/time and "create" or "delete"
	 * and the second the actual data record
	 */
	Record[] getNext();

	/**
	 * @return null at eof, else a pair of Record's
	 * the first containing the date/time and "create" or "delete"
	 * and the second the actual data record
	 */
	Record[] getPrev();

}