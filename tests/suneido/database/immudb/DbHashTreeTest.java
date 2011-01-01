/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

public class DbHashTreeTest {

	@Test
	public void empty() {
		DbHashTree tree = DbHashTree.empty();
		for (int i = 32; i < 64; ++i)
			assertEquals(0, tree.get(i));
	}

	@Test
	public void one_node() {
		IntRefs.set(null); // shouldn't need to create more nodes
		DbHashTree tree = DbHashTree.empty();
		for (int i = 32; i < 64; ++i)
			tree.with(i, i * 7);
		for (int i = 32; i < 64; ++i)
			assertEquals(i * 7, tree.get(i));
	}

	@Test
	public void collisions() {
		IntRefs.set(new IntRefs.Impl());
		DbHashTree tree = DbHashTree.empty();
		tree.with(0x10000, 123);
		tree.with(0x20000, 456);
		assertEquals(123, tree.get(0x10000));
		assertEquals(456, tree.get(0x20000));
		assertEquals(3, IntRefs.size());
	}

	@Test
	public void random() {
		IntRefs.set(new IntRefs.Impl());
		DbHashTree tree = DbHashTree.empty();
		Random r = new Random(123);
		int key, value;
		final int N = 10000;
		for (int i = 0; i < N; ++i) {
			key = r.nextInt();
			value = r.nextInt();
			if (key == 0 || value == 0)
				continue ;
			tree.with(key, value);
		}
		r.setSeed(123);
		for (int i = 0; i < N; ++i) {
			key = r.nextInt();
			value = r.nextInt();
			if (key == 0 || value == 0)
				continue ;
			assertEquals(value, tree.get(key));
		}
	}

}
