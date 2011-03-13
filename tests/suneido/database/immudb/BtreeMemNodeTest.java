/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static suneido.database.immudb.RecordTest.record;

import java.nio.ByteBuffer;

import org.junit.Test;

import suneido.language.Pack;

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

	@Test
	public void pack_empty() {
		BtreeMemNode node = BtreeMemNode.emptyLeaf();
		ByteBuffer buf = ByteBuffer.allocate(node.length());
		node.pack(buf);
		BtreeDbNode dbnode = new BtreeDbNode(0, buf);
		assertThat("size", dbnode.size(), is(0));
	}

	@Test
	public void pack_non_empty() {
		BtreeMemNode node = BtreeMemNode.emptyLeaf();
		Record key = new MemRecord().add("fzvdr").add(0x21726ef6);
		for (int i = 0; i < 5; ++i)
			node.add(key);
		ByteBuffer keybuf = Pack.pack(key);
		Record key2 = new DbRecord(keybuf, 0);
		for (int i = 0; i < 5; ++i)
			node.add(key2);
		ByteBuffer buf = ByteBuffer.allocate(node.length());
		node.pack(buf);
		BtreeDbNode dbnode = new BtreeDbNode(0, buf);
		assertThat("size", dbnode.size(), is(10));
		for (int i = 0; i < 10; ++i)
			assertThat(dbnode.get(i), is((Object) key));
	}

}
