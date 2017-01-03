/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import suneido.database.query.Query.Dir;
import suneido.database.server.Dbms.HeaderAndRow;
import suneido.intfc.database.Record;

public interface DbmsTran {
	/**
	 * Commit a transaction.
	 * @return null on success, otherwise an error string
	 */
	String complete();

	/** Abort a transaction. */
	void abort();

	int request(String s);

	DbmsQuery query(String s);

	void erase(int recadr);

	int update(int recadr, Record rec);

	/** @return null on eof */
	HeaderAndRow get(Dir dir, String query, boolean one);

	boolean isReadonly();

	boolean isEnded();

	int readCount();
	int writeCount();
}
