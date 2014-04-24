/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import java.util.List;

import suneido.database.query.Header;
import suneido.database.query.Query.Dir;
import suneido.database.query.Row;
import suneido.intfc.database.Record;

public interface DbmsQuery {

	Header header();

	List<String> ordering();

	List<List<String>> keys();

	Row get(Dir dir);

	void rewind();

	@Override
	String toString();

	void output(Record rec);

	void setTransaction(DbmsTran tran);

	boolean updateable();

	String explain();

	void close();

}
