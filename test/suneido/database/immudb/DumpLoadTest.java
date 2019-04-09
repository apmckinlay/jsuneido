/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import suneido.util.BufferByteChannel;

public class DumpLoadTest extends TestBase {

	@Test
	public void dump_load_database() {
		makeTable(7);
		check();

		BufferByteChannel b = new BufferByteChannel(1000);
		Dbpkg.dumpDatabase(db, b);
		db.close();

		b.flip();
		db = Dbpkg.testdb();
		Dbpkg.loadDatabase(db, b);
		check();
	}

	@Test
	public void dump_load_table() {
		makeTable(7);
		check();

		BufferByteChannel b = new BufferByteChannel(1000);
		Dbpkg.dumpTable(db, "test", b);
		db.close();

		b.flip();
		db = Dbpkg.testdb();
		Dbpkg.loadTable(db, "test", b);
		check();
	}

	private void check() {
		assertEquals("", db.check());
		assertThat(db.getSchema("test"), equalTo("(a,b) key(a) index(b,a)"));
		assertThat(getNrecords("test"), equalTo(7));
	}

}
