/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import suneido.intfc.database.Fkmode;

import com.google.common.collect.Ordering;

public class ForeignKeyTest {
	private Database2 db = DatabasePackage2.dbpkg.testdb();

	@Test
	public void schema() {
		db.createTable("src")
			.addColumn("k")
			.addColumn("a")
			.addColumn("f1")
			.addColumn("f2")
			.addIndex("k", true, false, "", "", 0)
			.addIndex("a", false, false, "dest", "a", Fkmode.BLOCK)
			.addIndex("f2", false, false, "dest", "b", Fkmode.CASCADE)
			.finish();
		assertThat(db.getSchema("src"),
				is("(k,a,f1,f2) key(k) " +
						"index(a) in dest " +
						"index(f2) in dest(b) cascade"));
		db.createTable("dest")
			.addColumn("a")
			.addColumn("b")
			.addIndex("a", true, false, "", "", 0)
			.addIndex("b", true, false, "", "", 0)
			.finish();

		assertThat(fkdsts("dest", "a"),
				is("[ForeignKeyTarget(src, [1], block)]"));
		assertThat(fkdsts("dest", "b"),
				is("[ForeignKeyTarget(src, [3], cascade)]"));

		db = db.reopen();

		assertThat(db.getSchema("src"),
				is("(k,a,f1,f2) key(k) " +
						"index(a) in dest " +
						"index(f2) in dest(b) cascade"));

		db.createTable("src2")
			.addColumn("k")
			.addColumn("f1")
			.addIndex("k", true, false, "", "", 0)
			.addIndex("f1", false, false, "dest", "b", Fkmode.BLOCK)
			.finish();
		db.alterTable("src")
			.addIndex("f1", false, false, "dest", "a", Fkmode.BLOCK)
			.finish();

		assertThat(db.getSchema("src"),
				is("(k,a,f1,f2) key(k) " +
						"index(a) in dest " +
						"index(f1) in dest(a) " +
						"index(f2) in dest(b) cascade"));

		assertThat(fkdsts("dest", "a"),
				is("[ForeignKeyTarget(src, [1], block), ForeignKeyTarget(src, [2], block)]"));
		assertThat(fkdsts("dest", "b"),
				is("[ForeignKeyTarget(src, [3], cascade), ForeignKeyTarget(src2, [1], block)]"));

		db.alterTable("src")
			.dropIndex("f2")
			.finish();

		assertThat(db.getSchema("src"),
				is("(k,a,f1,f2) key(k) " +
						"index(a) in dest " +
						"index(f1) in dest(a)"));

		assertThat(fkdsts("dest", "a"),
				is("[ForeignKeyTarget(src, [1], block), ForeignKeyTarget(src, [2], block)]"));
		assertThat(fkdsts("dest", "b"),
				is("[ForeignKeyTarget(src2, [1], block)]"));

		db.dropTable("src2");

		assertThat(fkdsts("dest", "a"),
				is("[ForeignKeyTarget(src, [1], block), ForeignKeyTarget(src, [2], block)]"));
		assertThat(fkdsts("dest", "b"),
				is("[]"));

	}

	private String fkdsts(String tableName, String indexColumns) {
		ReadTransaction2 t = db.readTransaction();
		Set<ForeignKeyTarget> fkdsts = t.getForeignKeys(tableName, indexColumns);
		assert fkdsts != null;
		return sort(fkdsts).toString();
	}

	private static List<ForeignKeyTarget> sort(Set<ForeignKeyTarget> fks) {
		return Ordering.usingToString().sortedCopy(fks);
	}

}
