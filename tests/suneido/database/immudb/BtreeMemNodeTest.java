/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static suneido.database.immudb.RecordTest.record;

import org.junit.Test;

public class BtreeMemNodeTest {

	@Test
	public void empty() {
		BtreeNode node = BtreeMemNode.emptyLeaf(null);
		assertThat(node.size(), is(0));
		assertThat(node.get(0), is(Record.EMPTY));
	}

	@Test
	public void main() {
		Record key1 = record("one");
		Record key2 = record("three");
		Record key3 = record("two");
		Record key9 = record("z");

		Tran tran = new Tran(null);
		BtreeNode node = BtreeMemNode.emptyLeaf(tran);
		node.with(tran, key2);
		assertEquals(key2, node.get(0));

		node.with(tran, key1);
		assertEquals(key1, node.get(0));
		assertEquals(key2, node.get(1));

		node.with(tran, key3);
		assertEquals(3, node.size());
		assertEquals(key1, node.get(0));
		assertEquals(key2, node.get(1));
		assertEquals(key3, node.get(2));

		assertEquals(key1, node.find(Record.EMPTY));
		assertEquals(key1, node.find(key1));
		assertEquals(key2, node.find(key2));
		assertEquals(key3, node.find(key3));
		assertNull(node.find(key9));
	}

}
