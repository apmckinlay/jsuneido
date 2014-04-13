/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static suneido.Suneido.dbpkg;

import org.junit.Test;

import suneido.intfc.database.DatabasePackage.Status;
import suneido.util.BufferByteChannel;

public class DumpLoadTest extends TestBase {

	@Test
	public void dump_load_database() {
		makeTable(7);
		check();

		BufferByteChannel b = new BufferByteChannel(1000);
		dbpkg.dumpDatabase(db, b);
		db.close();

		b.flip();
		db = dbpkg.testdb();
		dbpkg.loadDatabase(db, b);
		check();
	}

	@Test
	public void dump_load_table() {
		makeTable(7);
		check();

		BufferByteChannel b = new BufferByteChannel(1000);
		dbpkg.dumpTable(db, "test", b);
		db.close();

		b.flip();
		db = dbpkg.testdb();
		dbpkg.loadTable(db, "test", b);
		check();
	}

	private void check() {
		assertEquals(Status.OK, db.check());
		assertThat(db.getSchema("test"), equalTo("(a,b) key(a) index(b,a)"));
		assertThat(getNrecords("test"), equalTo(7));
	}

}
