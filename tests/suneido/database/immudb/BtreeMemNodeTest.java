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
		BtreeNode node = BtreeMemNode.emptyLeaf();
		assertThat(node.size(), is(0));
		assertThat(node.get(0), is(Record.EMPTY));
	}

	@Test
	public void main() {
		Record key1 = record("one");
		Record key2 = record("three");
		Record key3 = record("two");
		Record key9 = record("z");

		BtreeNode node = BtreeMemNode.emptyLeaf();
		node.with(key2);
		assertEquals(key2, node.get(0));

		node.with(key1);
		assertEquals(key1, node.get(0));
		assertEquals(key2, node.get(1));

		node.with(key3);
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

//	@Test
	public void persist() {
		Record key1 = record("one", 123);
		Record key2 = record("two", 456);
		BtreeMemNode node = BtreeMemNode.emptyLeaf();
		node.with(key1);
		node.with(key2);
		node.updatePointer(0, 123);
		node.updatePointer(1, 456);
		Record r = node.builder().build();
		assertThat(r.toString(), is("<<one,7b>,<two,1c8>>"));
	}

}
