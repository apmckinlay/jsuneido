/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

public interface HistoryIterator {
	void rewind();
	Record[] getNext();
	Record[] getPrev();
}