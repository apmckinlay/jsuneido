/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import suneido.runtime.Ops;

public class BtreeNodeTest {

	@Before
	public void setQuoting() {
		Ops.default_single_quotes = true;
	}

	@After
	public void restoreQuoting() {
		Ops.default_single_quotes = false;
	}

	@Test
	public void memNode_empty() {
		BtreeNode node = BtreeNode.emptyLeaf();
		assertThat(node, instanceOf(BtreeMemNode.class));
		assertThat(node.size(), equalTo(0));
	}

	@Test
	public void memNode_with() {
		BtreeKey key1 = key("one");
		BtreeKey key2 = key("three");
		BtreeKey key3 = key("two");
		BtreeKey key9 = key("z");

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

		assertEquals(key1, node.find(BtreeKey.EMPTY));
		assertEquals(key1, node.find(key1));
		assertEquals(key2, node.find(key2));
		assertEquals(key3, node.find(key3));
		assertNull(node.find(key9));
	}

	@Test
	public void memNode_without() {
		final int NKEYS = 100;
		Random rand = new Random(87665);
		List<BtreeKey> keys = randomKeys(rand, NKEYS);
		BtreeNode node = BtreeNode.emptyLeaf();
		assertThat(node.size(), equalTo(0));
		for (BtreeKey key : keys)
			node = node.with(key);
		assertThat(node.size(), equalTo(NKEYS));
		Collections.shuffle(keys, new Random(874));
		for (int i = 0; i < NKEYS / 2; ++i) {
			node = node.without(keys.get(i));
			assertNotNull(node);
		}
		assertThat(node.size(), equalTo(NKEYS / 2));
		for (int i = 0; i < NKEYS / 2; ++i)
			assertNull(node.without(keys.get(i)));
		assertThat(node.size(), equalTo(NKEYS / 2));
		for (int i = NKEYS / 2; i < NKEYS; ++i) {
			BtreeKey key = keys.get(i);
			assertThat(node.find(key), equalTo(key));
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
		node = node.with(key("new"));
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
		node = node.with(key("new"));
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
		node = node.with(key("new"));
		node = node.slice(0, 2);
		check(node, "joe", "new");
	}

	private static BtreeNode dbNode() {
		BtreeMemNode memNode = new BtreeMemNode(0);
		memNode.with(key("bob")).with(key("joe")).with(key("sue"));
		ByteBuffer buf = ByteBuffer.allocate(memNode.length());
		memNode.pack(buf);
		BtreeNode node = new BtreeDbNode(0, buf, 0);
		check(node, "bob", "joe", "sue");
		return node;
	}

	private static BtreeMemNode memNode() {
		return BtreeMemNode.from(0, key("bob"), key("joe"), key("sue"));
	}

	private static void check(BtreeNode node, String... keys) {
		if (node.size() != keys.length || ! equals(node, keys))
			fail("Expected: " + Arrays.toString(keys) + "\n   got: " + node);
	}

	private static boolean equals(BtreeNode node, String... keys) {
		for (int i = 0; i < keys.length; ++i)
			if (! node.get(i).equals(key(keys[i])))
				return false;
		return true;
	}

	@Test
	public void pack_empty_MemNode() {
		BtreeMemNode node = new BtreeMemNode(0);
		ByteBuffer buf = ByteBuffer.allocate(node.length());
		node.pack(buf);
		BtreeDbNode dbnode = new BtreeDbNode(0, buf, 0);
		assertThat("size", dbnode.size(), equalTo(0));
	}

	@Test
	public void pack_non_empty_leaf_MemNode() {
		BtreeMemNode memNode = new BtreeMemNode(0);
		BtreeKey key = key("fzvdr", 0x21726ef6);
		for (int i = 0; i < 10; ++i)
			memNode = memNode.with(key);
		ByteBuffer buf = ByteBuffer.allocate(memNode.length());
		memNode.pack(buf);
		BtreeDbNode dbnode = new BtreeDbNode(0, buf, 0);
		assertThat("size", dbnode.size(), equalTo(10));
		for (int i = 0; i < 10; ++i)
			assertThat(dbnode.get(i), equalTo((Object) key));
	}

	@Test
	public void pack_non_empty_tree_MemNode() {
		BtreeMemNode memNode = new BtreeMemNode(1);
		BtreeKey key = treekey("lsdkf", 0x21726ef6, 0x8745675);
		for (int i = 0; i < 10; ++i)
			memNode = memNode.with(key);
		ByteBuffer buf = ByteBuffer.allocate(memNode.length());
		memNode.pack(buf);
		BtreeDbNode dbnode = new BtreeDbNode(1, buf, 0);
		assertThat("size", dbnode.size(), equalTo(10));
		for (int i = 0; i < 10; ++i)
			assertThat(dbnode.get(i), equalTo((Object) key));
	}

	@Test
	public void pack_MemNode() {
		pack(memNode());
	}

	@Test
	public void pack_DbMemNode() {
		pack((BtreeMemNode) dbNode().with(key("new")));
	}

	private static void pack(BtreeMemNode node) {
		ByteBuffer buf = ByteBuffer.allocate(node.length());
		node.pack(buf);
		BtreeNode dbnode = new BtreeDbNode(0, buf, 0);
		assertThat(dbnode, equalTo((BtreeNode) node));
	}

	@Test
	public void mimimize() {
		BtreeKey k = new RecordBuilder().add("a").add("b").btreeTreeKey(123, 456);
		BtreeKey m = new RecordBuilder().btreeTreeKey(0, 456);
		assertThat(k.minimize(), equalTo(m));
		assertFalse(k.isMinimalKey());
		assertTrue(m.isMinimalKey());
	}

	@Test
	public void split_leaf_at_end() {
		BtreeNode node = leaf("a", "b", "c");
		Btree.Split split = Btree.split(node, key("d"));
		assertThat(split.left, equalTo(node));
		assertThat(split.key.toString(), equalTo("['c']*MAX^REF"));
		assertThat(split.key.child(), equalTo(leaf("d")));
	}

	@Test
	public void split_with_key_in_left() {
		BtreeNode node = leaf("a", "c", "e", "g");
		Btree.Split split = Btree.split(node, key("b"));
		assertThat(split.left, equalTo(leaf("a", "b", "c")));
		assertThat(split.key.toString(), equalTo("['c']*MAX^REF"));
		assertThat(split.key.child(), equalTo(leaf("e", "g")));
	}

	@Test
	public void split_with_key_in_right() {
		BtreeNode node = leaf("a", "c", "e", "g");
		Btree.Split split = Btree.split(node, key("f"));
		assertThat(split.left, equalTo(leaf("a", "c")));
		assertThat(split.key.toString(), equalTo("['c']*MAX^REF"));
		assertThat(split.key.child(), equalTo(leaf("e", "f", "g")));
	}

	@Test
	public void split_tree_node() {
		BtreeNode node = tree("a", "c", "e", "g");
		Btree.Split split = Btree.split(node, treekey("f", 123, 456));
		assertThat(split.left, equalTo(tree("a", "c")));
		assertThat(split.key.toString(), equalTo("['e']*123^REF"));
		assertThat(split.key.child(),
				equalTo(tree("e", "f", "g").minimizeLeftMost()));
	}

	@Test
	public void split_between_leaf_duplicates() {
		BtreeNode node = leaf(dup(1), dup(2), dup(4), dup(5));
		Btree.Split split = Btree.split(node, dup(3));
		assertThat(split.left, equalTo(leaf(dup(1), dup(2), dup(3))));
		assertThat(split.key.toString(), equalTo("['dup']*3^REF"));
		assertThat(split.key.child(), equalTo(leaf(dup(4), dup(5))));
	}

	//--------------------------------------------------------------------------

	private static BtreeNode leaf(Object... args) {
		return node(0, args);
	}
	private static BtreeNode tree(Object... args) {
		return node(1, args);
	}
	private static BtreeNode node(int level, Object... args) {
		BtreeKey keys[] = new BtreeKey[args.length];
		for (int i = 0; i < args.length; ++i)
			keys[i] = args[i] instanceof BtreeKey ? (BtreeKey) args[i]
					: level == 0 ? key((String) args[i]) : treekey((String) args[i], 123, 456);
		return BtreeMemNode.from(level, keys);
	}

	public static List<BtreeKey> randomKeys(Random rand, int n) {
		List<BtreeKey> keys = Lists.newArrayList();
		for (int i = 0; i < n; ++i)
			keys.add(randomKey(rand));
		return keys;
	}

	public static BtreeKey randomKey(Random rand) {
		int n = 4 + rand.nextInt(5);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; ++i)
			sb.append((char) ('a' + rand.nextInt(26)));
		final int UPDATE_ALLOWANCE = 10000;
		return key(sb.toString(), rand.nextInt(Integer.MAX_VALUE - UPDATE_ALLOWANCE));
	}

	private static BtreeKey key(String s, int adr) {
		return new RecordBuilder().add(s).btreeKey(adr);
	}

	static BtreeKey key(int n, String s, int adr) {
		return new RecordBuilder().add(n).add(s).btreeKey(adr);
	}

	private static BtreeKey key(String s) {
		return new RecordBuilder().add(s).btreeKey(123);
	}

	private static BtreeKey dup(int adr) {
		return new RecordBuilder().add("dup").btreeKey(adr);
	}

	private static BtreeTreeKey treekey(String s, int dataAdr, int childAdr) {
		return new RecordBuilder().add(s).btreeTreeKey(dataAdr, childAdr);
	}

}
