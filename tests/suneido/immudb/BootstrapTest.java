/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import suneido.immudb.Bootstrap.TN;

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
		assertThat(db.getSchema("tables"),
				is("(table,tablename) key(table) key(tablename)"));
		assertThat(db.getSchema("columns"),
				is("(table,field,column) key(table,column)"));
		assertThat(db.getSchema("indexes"),
				is("(table,columns,key,fktable,fkcolumns,fkmode) " +
						"key(table,columns)"));
		assertThat(db.getSchema("views"),
				is("(view_name,view_definition) key(view_name)"));

		ReadDbInfo dbinfo = new ReadDbInfo(db.getDbinfo());
		assertThat(dbinfo.get(TN.TABLES).nrows(), is(4));
		assertThat(dbinfo.get(TN.COLUMNS).nrows(), is(13));
		assertThat(dbinfo.get(TN.INDEXES).nrows(), is(5));

		assertThat(new CheckTable(db, "tables").call(), is(""));
		assertThat(new CheckTable(db, "columns").call(), is(""));
		assertThat(new CheckTable(db, "indexes").call(), is(""));
	}

}
