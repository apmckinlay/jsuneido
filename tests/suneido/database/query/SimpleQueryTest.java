/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import suneido.Suneido;
import suneido.database.query.Query.Dir;
import suneido.database.server.ServerData;
import suneido.intfc.database.Database;
import suneido.intfc.database.DatabasePackage;
import suneido.intfc.database.Record;
import suneido.intfc.database.Transaction;

import com.google.common.collect.ImmutableList;

@RunWith(Parameterized.class)
public class SimpleQueryTest {
	private final Database db;
	private final Transaction t;
	protected final ServerData serverData = new ServerData();
	private final DatabasePackage save_dbpkg;

	@Parameters
	public static Collection<Object[]> generateParams() {
		return ImmutableList.of(
				new Object[] { suneido.database.DatabasePackage.dbpkg },
				new Object[] { suneido.immudb.DatabasePackage.dbpkg });
	}

	public SimpleQueryTest(DatabasePackage dbpkg) {
		save_dbpkg = Suneido.dbpkg;
		Suneido.dbpkg = dbpkg;
		db = dbpkg.testdb();
		t = db.readonlyTran();
	}

	@Test
	public void table() {
		Query q = CompileQuery.query(t, serverData, "tables");
		Record r = q.get(Dir.NEXT).firstData();
		assertThat(r.getInt(0), is(1));
		assertThat(r.getString(1), is("tables"));
	}

	@Test
	public void where() {
		Query q = CompileQuery.query(t, serverData, "tables where tablename > 'd'");
		Record r = q.get(Dir.NEXT).firstData();
		assertThat(r.getInt(0), is(3));
		assertThat(r.getString(1), is("indexes"));
	}

	@Test
	public void join() {
		Query q = CompileQuery.query(t, serverData, "tables join columns");
		Record r = q.get(Dir.NEXT).firstData();
		assertThat(r.getInt(0), is(1));
		assertThat(r.getString(1), is("tables"));
	}

	@Test
	public void sort() {
		Query q = CompileQuery.query(t, serverData, "columns sort column");
		Header hdr = q.header();
		Row row = q.get(Dir.NEXT);
		assertThat((String) row.getval(hdr, "column"), is("column"));
	}

	@After
	public void cleanup() {
		t.complete();
		Suneido.dbpkg = save_dbpkg;
	}

}
