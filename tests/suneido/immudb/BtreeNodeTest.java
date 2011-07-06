/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static suneido.immudb.BtreeTest.randomKeys;
import static suneido.immudb.BtreeTest.record;

import java.nio.ByteBuffer;
import java.util.*;

import org.junit.Test;

import suneido.immudb.*;
import suneido.language.Pack;

public class BtreeNodeTest {

	@Test
	public void memNode_empty() {
		BtreeNode node = BtreeNode.emptyLeaf();
		assertThat(node, is(BtreeMemNode.class));
		assertThat(node.size(), is(0));
	}

	@Test
	public void memNode_with() {
		Record key1 = record("one");
		Record key2 = record("three");
		Record key3 = record("two");
		Record key9 = record("z");

		BtreeNode node = BtreeNode.emptyLeaf();
		node = node.with(key2);
		assertEquals(key2, node.get(0));

		node = node.with(key1);
		assertEquals(key1, node.get(0));
		assertEquals(key2, node.get(1));

		node = node.with(key3);
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
	public void memNode_without() {
		final int NKEYS = 100;
		Random rand = new Random(87665);
		List<Record> keys = randomKeys(rand, NKEYS);
		BtreeNode node = BtreeNode.emptyLeaf();
		assertThat(node.size(), is(0));
		for (Record key : keys)
			node = node.with(key);
		assertThat(node.size(), is(NKEYS));
		Collections.shuffle(keys, new Random(874));
		for (int i = 0; i < NKEYS / 2; ++i) {
			node = node.without(keys.get(i));
			assertNotNull(node);
		}
		assertThat(node.size(), is(NKEYS / 2));
		for (int i = 0; i < NKEYS / 2; ++i)
			assertNull(node.without(keys.get(i)));
		assertThat(node.size(), is(NKEYS / 2));
		for (int i = NKEYS / 2; i < NKEYS; ++i) {
			Record key = keys.get(i);
			assertThat(node.find(key), is(key));
		}
	}

	@Test
	public void withDbNode() {
		with(dbNode());
	}

	@Test
	public void withMemNode() {
		with(memNode());
	}

	public void with(BtreeNode node) {
		node = node.with(record("new"));
		check(node, "bob", "joe", "new", "sue");
		node = node.without(1);
		check(node, "bob", "new", "sue");
		node = node.without(1);
		check(node, "bob", "sue");
		node = node.without(0).without(0);
		check(node);
	}

	@Test
	public void withoutDbNode() {
		without(dbNode());
	}

	@Test
	public void withoutMemNode() {
		without(memNode());
	}

	private void without(BtreeNode node) {
		node = node.without(1);
		check(node, "bob", "sue");
		node = node.with(record("new"));
		check(node, "bob", "new", "sue");
	}

	@Test
	public void withoutRange() {
		BtreeNode node;

		node = dbNode().without(0, 2);
		check(node, "sue");

		node = dbNode().without(1, 3);
		check(node, "bob");

		node = memNode().without(0, 2);
		check(node, "sue");

		node = memNode().without(1, 3);
		check(node, "bob");
	}

	@Test
	public void sliceDbNode() {
		slice(dbNode());
	}

	@Test
	public void sliceMemNode() {
		slice(memNode());
	}

	private void slice(BtreeNode node) {
		node = node.slice(1, 3);
		check(node, "joe", "sue");
		node = node.with(record("new"));
		node = node.slice(0, 2);
		check(node, "joe", "new");
	}

	private BtreeNode dbNode() {
		Record rec = new RecordBuilder()
				.add(record("bob")).add(record("joe")).add(record("sue")).build();
		BtreeNode node = new BtreeDbNode(0, Pack.pack(rec));
		check(node, "bob", "joe", "sue");
		return node;
	}

	private BtreeMemNode memNode() {
		return new BtreeMemNode(0, record("bob"), record("joe"), record("sue"));
	}

	private void check(BtreeNode node, String... keys) {
		if (node.size() != keys.length || ! equals(node, keys))
			fail("Expected: " + Arrays.toString(keys) + "\n   got: " + node);
	}

	private boolean equals(BtreeNode node, String... keys) {
		for (int i = 0; i < keys.length; ++i)
			if (!node.get(i).equals(record(keys[i])))
				return false;
		return true;
	}

	@Test
	public void pack_empty_MemNode() {
		BtreeMemNode node = new BtreeMemNode(0);
		ByteBuffer buf = ByteBuffer.allocate(node.length());
		node.pack(buf);
		BtreeDbNode dbnode = new BtreeDbNode(0, buf);
		assertThat("size", dbnode.size(), is(0));
	}

	@Test
	public void pack_non_empty_MemNode() {
		BtreeMemNode node = new BtreeMemNode(0);
		Record key = new RecordBuilder().add("fzvdr").add(0x21726ef6).build();
		for (int i = 0; i < 10; ++i)
			node = node.with(key);
		ByteBuffer buf = ByteBuffer.allocate(node.length());
		node.pack(buf);
		BtreeDbNode dbnode = new BtreeDbNode(0, buf);
		assertThat("size", dbnode.size(), is(10));
		for (int i = 0; i < 10; ++i)
			assertThat(dbnode.get(i), is((Object) key));
	}

	@Test
	public void pack_MemNode() {
		pack(memNode());
	}

	@Test
	public void pack_DbMemNode() {
		pack(dbNode().with(record("new")));
	}

	private void pack(BtreeStoreNode node) {
		ByteBuffer buf = ByteBuffer.allocate(node.length());
		node.pack(buf);
		BtreeNode dbnode = new BtreeDbNode(0, buf);
		assertThat(dbnode, is((BtreeNode) node));
	}

}
