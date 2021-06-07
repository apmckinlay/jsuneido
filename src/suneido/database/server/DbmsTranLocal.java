/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static suneido.Trace.trace;
import static suneido.Trace.tracing;
import static suneido.Trace.Type.QUERY;

import suneido.SuException;
import suneido.database.immudb.Record;
import suneido.database.immudb.Transaction;
import suneido.database.query.CompileQuery;
import suneido.database.query.Query;
import suneido.database.query.Query.Dir;
import suneido.database.query.QueryAction;
import suneido.database.query.Row;
import suneido.database.server.Dbms.HeaderAndRow;

public class DbmsTranLocal implements DbmsTran {
	final Transaction t;

	public DbmsTranLocal(Transaction t) {
		this.t = t;
	}

	@Override
	public String complete() {
		return t.complete();
	}

	@Override
	public void abort() {
		t.abort();
	}

	@Override
	public int request(String s) {
		if (tracing(QUERY))
			trace(QUERY, t + " " + s);
		Query q = CompileQuery.parse(t, ServerData.forThread(), s);
		return ((QueryAction) q).execute();
	}

	@Override
	public DbmsQuery query(String s) {
		if (tracing(QUERY))
			trace(QUERY, t + " " + s);
		return new DbmsQueryLocal(CompileQuery.query(t, ServerData.forThread(), s));
	}

	@Override
	public HeaderAndRow get(Dir dir, String query, boolean one) {
		if (tracing(QUERY))
			trace(QUERY, t + " " + query);
		Query q = CompileQuery.query(t, ServerData.forThread(), query);
		try {
			Row row = q.get(dir);
			if (row == null)
				return null;
			if (one && q.get(dir) != null)
				throw new SuException("Query1 not unique: " + query);
			return new HeaderAndRow(q.header(), row);
		} finally {
			q.close();
		}
	}

	@Override
	public void erase(int recadr) {
		t.removeRecord(recadr);
	}

	@Override
	public int update(int recadr, Record rec) {
		return t.updateRecord(recadr, rec);
	}

	@Override
	public boolean isReadonly() {
		return t.isReadonly();
	}

	@Override
	public boolean isEnded() {
		return t.isEnded();
	}

	int num() {
		return t.num();
	}

	@Override
	public String toString() {
		return t.toString();
	}

	@Override
	public int readCount() {
		return t.readCount();
	}

	@Override
	public int writeCount() {
		return t.writeCount();
	}

}
