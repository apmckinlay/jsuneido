/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Random;

import org.junit.*;

public class DbHashTreeTest {

	@After
	public void teardown() {
		IntRefs.set(null);
		DbHashTree.mmf = null;
	}

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
			assert tree.get(key) == 0;
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

	@Test
	public void persist() {
		IntRefs.set(new IntRefs.Impl());
		DbHashTree tree = DbHashTree.empty();
		MmapFile mmf = new MmapFile("tmp1", "rw");
		DbHashTree.mmf = mmf;
		int at = tree.persist();
		assertEquals(0, at);
		assertEquals(8, mmf.size());
		mmf.buffer(mmf.alloc(1)).put((byte) 0xff); // ensure data isn't truncated
		mmf.close();

		mmf = new MmapFile("tmp1", "rw");
		DbHashTree.mmf = mmf;
		tree = DbHashTree.from(at);
		for (int i = 32; i < 64; ++i)
			assertEquals(0, tree.get(i));
		int at2 = tree.persist();
		assertEquals(at, at2);
		tree = tree.with(32, 123).with(64, 456);
		assertEquals(123, tree.get(32));
		assertEquals(456, tree.get(64));
		at2 = tree.persist();
		assert(at != at2);
		assertEquals(123, tree.get(32));
		assertEquals(456, tree.get(64));
		tree = DbHashTree.from(at2);
		assertEquals(123, tree.get(32));
		assertEquals(456, tree.get(64));
		mmf.close();

		mmf = new MmapFile("tmp1", "rw");
		DbHashTree.mmf = mmf;
		tree = DbHashTree.from(at2);
		assertEquals(123, tree.get(32));
		assertEquals(456, tree.get(64));
		Random r = new Random(1234);
		int key, value;
		final int N = 1000;
		for (int i = 0; i < N; ++i) {
			key = r.nextInt();
			value = r.nextInt();
			assert tree.get(key) == 0;
			if (key == 0 || value == 0)
				continue ;
			tree = tree.with(key, value);
		}
		at2 = tree.persist();
		mmf.close();

		mmf = new MmapFile("tmp1", "rw");
		DbHashTree.mmf = mmf;
		tree = DbHashTree.from(at2);
		for (int i = 0; i < N; ++i) {
			key = r.nextInt();
			value = r.nextInt();
			assert tree.get(key) == 0;
			if (key == 0 || value == 0)
				continue ;
			tree = tree.with(key, value);
		}
		r.setSeed(1234);
		for (int i = 0; i < N * 2; ++i) {
			key = r.nextInt();
			value = r.nextInt();
			if (key == 0 || value == 0)
				continue ;
			assertEquals("i=" + i + " key=" + DbHashTree.fmt(key) + " value=" + value, value, tree.get(key));
		}
		mmf.close();
	}

	@BeforeClass
	@AfterClass
	public static void cleanup() {
		new File("tmp1").delete();
	}

}
