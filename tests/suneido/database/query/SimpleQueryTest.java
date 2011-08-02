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
	protected final ServerData serverData = new ServerData();

	@Parameters
	public static Collection<Object[]> generateParams() {
		return ImmutableList.of(
				new Object[] { new suneido.database.DatabasePackage() },
				new Object[] { new suneido.immudb.DatabasePackage() }
				);
	}

	public SimpleQueryTest(DatabasePackage dbpkg) {
		Suneido.dbpkg = dbpkg;
		db = dbpkg.testdb();
	}

	@Test
	public void test() {
		Transaction t = db.readonlyTran();
		try {
			Query q = CompileQuery.query(t, serverData, "tables where tablename > 'd'");
			Record r = q.get(Dir.NEXT).firstData();
			assertThat(r.getInt(0), is(3));
			assertThat(r.getString(1), is("indexes"));
		} finally {
			t.complete();
		}
	}

	@After
	public void resetDbpkg() {
		Suneido.dbpkg = new suneido.database.DatabasePackage();
	}

}
