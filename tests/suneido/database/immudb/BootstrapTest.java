/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class BootstrapTest {

	@Test
	public void test() {
		TestStorage stor = new TestStorage(500, 100);
		Database db = Database.create(stor);
		check(db);

		db = Database.open(stor);
		check(db);
	}

	private void check(Database db) {
		assertThat(db.schema.get("tables").schema(),
				is("(table,tablename) key(table)"));
		assertThat(db.schema.get("columns").schema(),
				is("(table,field,column) key(table,field)"));
		assertThat(db.schema.get("indexes").schema(),
				is("(table,columns,key,fktable,fkcolumns,fkmode) " +
						"key(table,columns)"));
	}

}
