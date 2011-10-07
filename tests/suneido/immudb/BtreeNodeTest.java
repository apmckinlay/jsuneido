/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static suneido.immudb.BtreeTest.randomKeys;
import static suneido.immudb.BtreeTest.record;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

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

	private static void without(BtreeNode node) {
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

	private static void slice(BtreeNode node) {
		node = node.slice(1, 3);
		check(node, "joe", "sue");
		node = node.with(record("new"));
		node = node.slice(0, 2);
		check(node, "joe", "new");
	}

	private static BtreeNode dbNode() {
		Record rec = new RecordBuilder()
				.add(record("bob")).add(record("joe")).add(record("sue")).build();
		BtreeNode node = new BtreeDbNode(0, Pack.pack(rec));
		check(node, "bob", "joe", "sue");
		return node;
	}

	private static BtreeMemNode memNode() {
		return new BtreeMemNode(0, record("bob"), record("joe"), record("sue"));
	}

	private static void check(BtreeNode node, String... keys) {
		if (node.size() != keys.length || ! equals(node, keys))
			fail("Expected: " + Arrays.toString(keys) + "\n   got: " + node);
	}

	private static boolean equals(BtreeNode node, String... keys) {
		for (int i = 0; i < keys.length; ++i)
			if (! node.get(i).equals(record(keys[i])))
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

	private static void pack(BtreeStorableNode node) {
		ByteBuffer buf = ByteBuffer.allocate(node.length());
		node.pack(buf);
		BtreeNode dbnode = new BtreeDbNode(0, buf);
		assertThat(dbnode, is((BtreeNode) node));
	}

	@Test
	public void mimimize() {
		Record k = new RecordBuilder().add("a").add("b").adduint(7685).build();
		Record m = new RecordBuilder().addMin().addMin().adduint(7685).build();
		assertThat(BtreeNode.minimize(k), is(m));
		assertFalse(BtreeNode.isMinimalKey(k));
		assertTrue(BtreeNode.isMinimalKey(m));
	}

	@Test
	public void split_leaf_at_end() {
		BtreeNode node = node("a", "b", "c");
		Tran tran = mock(Tran.class);
		when(tran.refToInt(anyObject())).thenReturn(456);
		Btree.Split split = node.split(tran, key("d"), 999);
		verify(tran).refToInt(node("d"));
		assertThat(split.key, is(key("d", 456)));

		BtreeNode root = BtreeMemNode.newRoot(tran, split);
		assertThat(root, is(node(1, key("c", 999), key("d", 456))));
	}

	@Test
	public void split_with_key_in_left() {
		BtreeNode node = node("a", "c", "e", "g");
		Tran tran = mock(Tran.class);
		when(tran.refToInt(anyObject())).thenReturn(456);
		Btree.Split split = node.split(tran, key("b"), 999);
		verify(tran).redir(999, node("a", "b", "c"));
		verify(tran).refToInt(node("e", "g"));
		assertThat(split.key, is(key("e", 456)));

		BtreeNode root = BtreeMemNode.newRoot(tran, split);
		assertThat(root, is(node(1, min(999), key("e", 456))));
	}

	@Test
	public void split_with_key_in_right() {
		BtreeNode node = node("a", "c", "e", "g");
		Tran tran = mock(Tran.class);
		when(tran.refToInt(anyObject())).thenReturn(456);
		Btree.Split split = node.split(tran, key("f"), 999);
		verify(tran).redir(999, node("a", "c"));
		verify(tran).refToInt(node("e", "f", "g"));
		assertThat(split.key, is(key("e", 456)));
	}

	private static BtreeNode node(String... args) {
		return node(0, args);
	}
	private static BtreeNode node(int level, String... args) {
		Record keys[] = new Record[args.length];
		for (int i = 0; i < args.length; ++i)
			keys[i] = level == 0 ? key(args[i]) : key(args[i], 456);
		return new BtreeMemNode(level, keys);
	}
	private static BtreeNode node(int level, Record... keys) {
		return new BtreeMemNode(level, keys);
	}

	private static Record key(String s) {
		return new RecordBuilder().add(s).adduint(123).build();
	}

	private static Record key(String s, int treeAdr) {
		return new RecordBuilder().add(s).addMin().adduint(treeAdr).build();
	}

	private static Record min(int adr) {
		return new RecordBuilder().addMin().addMin().adduint(adr).build();
	}

}
