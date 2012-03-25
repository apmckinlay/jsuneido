/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class DbInfoTest {

	@Test
	public void test() {
		Storage stor = new MemStorage(64, 32);
		UpdateDbInfo dbinfo = new UpdateDbInfo();
		assertNull(dbinfo.get(123));
		ImmutableList<IndexInfo> indexes = ImmutableList.of(
				new IndexInfo(new int[0], 123, 0, 1, 0));
		TableInfo ti = new TableInfo(6, 5, 4, 99, indexes);
		dbinfo.add(ti);
		assertThat(dbinfo.get(6), is(ti));
		int adr = dbinfo.store(stor);

		ReadDbInfo rdbinfo = new ReadDbInfo(
				DbHashTrie.load(stor, adr, new Database.DbinfoTranslator(stor)));
		TableInfo ti2 = rdbinfo.get(6);
		assertThat(ti2.nextfield, is(ti.nextfield));
	}

}
