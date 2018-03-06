/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

public interface IndexIter {

	boolean eof();

	Record curKey();

	int keyadr();

	void next();

	void prev();

	void rewind();

}