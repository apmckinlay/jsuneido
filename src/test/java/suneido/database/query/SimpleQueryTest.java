/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Test;

import suneido.database.query.Query.Dir;
import suneido.database.server.ServerData;
import suneido.intfc.database.Record;
import suneido.intfc.database.Transaction;

public class SimpleQueryTest extends TestBase {
	private final Transaction t;
	protected final ServerData serverData = new ServerData();

	public SimpleQueryTest() {
		t = db.readTransaction();
	}

	@Test
	public void table() {
		Query q = CompileQuery.query(t, serverData, "tables");
		Record r = q.get(Dir.NEXT).firstData();
		assertThat(r.getInt(0), equalTo(1));
		assertThat(r.getString(1), equalTo("tables"));
	}

	@Test
	public void where() {
		Query q = CompileQuery.query(t, serverData, "tables where tablename > 'd'");
		Record r = q.get(Dir.NEXT).firstData();
		assertThat(r.getInt(0), equalTo(3));
		assertThat(r.getString(1), equalTo("indexes"));
	}

	@Test
	public void join() {
		Query q = CompileQuery.query(t, serverData, "tables join columns");
		Record r = q.get(Dir.NEXT).firstData();
		assertThat(r.getInt(0), equalTo(1));
		assertThat(r.getString(1), equalTo("tables"));
	}

	@Test
	public void sort() {
		Query q = CompileQuery.query(t, serverData, "columns sort column");
		Header hdr = q.header();
		Row row = q.get(Dir.NEXT);
		assertThat((String) row.getval(hdr, "column"), equalTo("column"));
	}

	@After
	public void cleanup() {
		t.complete();
	}

}
