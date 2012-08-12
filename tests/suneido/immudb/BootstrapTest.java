/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import suneido.immudb.Bootstrap.TN;

public class BootstrapTest {

	@Test
	public void test() {
		MemStorage dstor = new MemStorage(500, 100);
		MemStorage istor = new MemStorage(500, 100);
		Database db = Database.create(dstor, istor);
		check(db);

		db = Database.open(dstor, istor);
		check(db);
	}

	private static void check(Database db) {
		assertThat(db.getSchema("tables"),
				is("(table,tablename) key(table) key(tablename)"));
		assertThat(db.getSchema("columns"),
				is("(table,field,column) key(table,column)"));
		assertThat(db.getSchema("indexes"),
				is("(table,fields,key,fktable,fkcolumns,fkmode) " +
						"key(table,fields)"));
		assertThat(db.getSchema("views"),
				is("(view_name,view_definition) key(view_name)"));

		DbHashTrie dbinfo = db.state.dbinfo;
		assertThat(((TableInfo) dbinfo.get(TN.TABLES)).nrows(), is(4));
		assertThat(((TableInfo) dbinfo.get(TN.COLUMNS)).nrows(), is(13));
		assertThat(((TableInfo) dbinfo.get(TN.INDEXES)).nrows(), is(5));

		assertThat(new CheckTable(db, "tables").call(), is(""));
		assertThat(new CheckTable(db, "columns").call(), is(""));
		assertThat(new CheckTable(db, "indexes").call(), is(""));
	}

}
