/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.Random;

import org.junit.*;

public class DbHashTreeTest {

	@Test
	public void empty() {
		DbHashTree tree = DbHashTree.empty(mock(Tran.class));
		for (int i = 32; i < 64; ++i)
			assertEquals(0, tree.get(i));
	}

	@Test
	public void one_node() {
		DbHashTree tree = DbHashTree.empty(mock(Tran.class));
		for (int i = 32; i < 64; ++i)
			tree.with(i, i * 7);
		for (int i = 32; i < 64; ++i)
			assertEquals(i * 7, tree.get(i));

		// update
		tree.with(50, 555);
		assertEquals(555, tree.get(50));
	}

	@Test
	public void collisions() {
		DbHashTree tree = DbHashTree.empty(new Tran());
		tree.with(0x10000, 123);
		tree.with(0x20000, 456);
		assertEquals(123, tree.get(0x10000));
		assertEquals(456, tree.get(0x20000));
	}

	@Test
	public void random() {
		DbHashTree tree = DbHashTree.empty(new Tran());
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
		Tran tran = new Tran();
		DbHashTree tree = DbHashTree.empty(tran);
		MmapFile mmf = new MmapFile("tmp1", "rw");
		tran.mmf(mmf);
		int at = tree.persist(mmf);
		assertEquals(1, at);
		mmf.buffer(mmf.alloc(1)).put((byte) 0xff); // ensure data isn't truncated
		mmf.close();

		mmf = new MmapFile("tmp1", "rw");
		tran.mmf(mmf);
		tree = DbHashTree.from(tran, at);
		for (int i = 32; i < 64; ++i)
			assertEquals(0, tree.get(i));
		tree = tree.with(32, 123).with(64, 456);
		assertEquals(123, tree.get(32));
		assertEquals(456, tree.get(64));
		int at2 = tree.persist(mmf);
		assert(at != at2);
		assertEquals(123, tree.get(32));
		assertEquals(456, tree.get(64));
		tree = DbHashTree.from(tran, at2);
		assertEquals(123, tree.get(32));
		assertEquals(456, tree.get(64));
		mmf.close();

		mmf = new MmapFile("tmp1", "rw");
		tran.mmf(mmf);
		tree = DbHashTree.from(tran, at2);
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
		at2 = tree.persist(mmf);
		mmf.close();

		mmf = new MmapFile("tmp1", "rw");
		tran.mmf(mmf);
		tree = DbHashTree.from(tran, at2);
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
