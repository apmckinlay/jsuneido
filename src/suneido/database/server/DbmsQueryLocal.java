/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import java.util.List;

import suneido.database.immudb.Record;
import suneido.database.query.Header;
import suneido.database.query.Query;
import suneido.database.query.Query.Dir;
import suneido.database.query.Row;

public class DbmsQueryLocal implements DbmsQuery {
	private final Query q;

	public DbmsQueryLocal(Query q) {
		this.q = q;
	}

	@Override
	public Row get(Dir dir) {
		return q.get(dir);
	}

	@Override
	public Header header() {
		return q.header();
	}

	@Override
	public List<List<String>> keys() {
		return q.keys();
	}

	@Override
	public List<String> ordering() {
		return q.ordering();
	}

	@Override
	public void output(Record rec) {
		q.output(rec);
	}

	@Override
	public void rewind() {
		q.rewind();
	}

	@Override
	public void setTransaction(DbmsTran tran) {
		q.setTransaction(tran == null ? null : ((DbmsTranLocal) tran).t);
	}

	@Override
	public boolean updateable() {
		return q.updateable();
	}

	@Override
	public String strategy() {
		return q.strategy();
	}

	@Override
	public String toString() {
		return q.strategy();
	}

	@Override
	public void close() {
		q.close();
	}

}
