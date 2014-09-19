/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;

import suneido.immudb.Bootstrap.TN;

public class BootstrapTest {

	@Test
	public void test() {
		HeapStorage dstor = new HeapStorage();
		HeapStorage istor = new HeapStorage();
		Database db = Database.create(dstor, istor);
		check(db);

		db = Database.open(dstor, istor);
		check(db);
	}

	private static void check(Database db) {
		assertThat(db.getSchema("tables"),
				equalTo("(table,tablename) key(table) key(tablename)"));
		assertThat(db.getSchema("columns"),
				equalTo("(table,field,column) key(table,column)"));
		assertThat(db.getSchema("indexes"),
				equalTo("(table,fields,key,fktable,fkcolumns,fkmode) " +
						"key(table,fields)"));
		assertThat(db.getSchema("views"),
				equalTo("(view_name,view_definition) key(view_name)"));

		DbHashTrie dbinfo = db.state.dbinfo;
		assertThat(((TableInfo) dbinfo.get(TN.TABLES)).nrows(), equalTo(4));
		assertThat(((TableInfo) dbinfo.get(TN.COLUMNS)).nrows(), equalTo(13));
		assertThat(((TableInfo) dbinfo.get(TN.INDEXES)).nrows(), equalTo(5));

		assertThat(new CheckTable(db, "tables").call(), equalTo(""));
		assertThat(new CheckTable(db, "columns").call(), equalTo(""));
		assertThat(new CheckTable(db, "indexes").call(), equalTo(""));
	}

}
