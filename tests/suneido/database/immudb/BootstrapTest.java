/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import suneido.database.immudb.schema.Bootstrap;
import suneido.database.immudb.schema.Tables;

public class BootstrapTest {

	@Test
	public void test() {
		TestStorage stor = new TestStorage(500, 100);
		Bootstrap bs = new Bootstrap(stor);
		bs.create();

		Database db = new Database(stor);
		db.open();

		Tables schema = db.schema();
		assertThat(schema.get("tables").schema(),
				is("(table,tablename) key(table)"));
		assertThat(schema.get("columns").schema(),
				is("(table,field,column) key(table,field)"));
		assertThat(schema.get("indexes").schema(),
				is("(table,columns,key,fktable,fkcolumns,fkmode) " +
						"key(table,columns)"));

		db.close();
	}

}
