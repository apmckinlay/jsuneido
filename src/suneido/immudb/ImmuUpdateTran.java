/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

interface ImmuUpdateTran extends ImmuReadTran {

	void abortThrow(String string);

	void removeAll(int tblnum, int[] colNums, Record key);

	void updateAll(int tblnum, int[] colNums, Record oldkey, Record newkey);

	TranIndex addIndex(int tblnum, int... colNums);

	void addRecord(int indexes, Record record);

}
