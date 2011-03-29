/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.ByteBuffer;

import org.junit.Test;

import suneido.util.FileUtils;

public class TranTest {
	Storage stor = new TestStorage();
//	File tempfile = FileUtils.tempfile();
//	MmapFile stor = new MmapFile(tempfile, "rw");
//
//	@After
//	public void teardown() {
//		stor.close();
//		tempfile.delete();
//	}

	@Test
	public void empty_database() {
		check(stor, true, 0, 0);
		assertTrue(new Check(stor).fastcheck());
	}

	@Test
	public void check_empty_commit() {
		Tran tran = new Tran(stor);
		tran.startStore();
		tran.endStore();

		check(stor, true, 8 + 8, 1);
		assertTrue(new Check(stor).fastcheck());
	}

	@Test
	public void check_one_commit() {
		Tran tran = new Tran(stor);
		tran.startStore();

		byte[] data = new byte[] { 1 };
		int adr = stor.alloc(data.length);
		stor.buffer(adr).put(data);

		data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		adr = stor.alloc(data.length);
		stor.buffer(adr).put(data);

		tran.endStore();

		check(stor, true, 8 + 8 + 16 + 8, 1);
		assertTrue(new Check(stor).fastcheck());
	}

	@Test
	public void check_multiple_commits() {
		final int NCOMMITS = 20;
		Tran tran;
		byte[] data;
		for (int i = 0; i < NCOMMITS; ++i) {
			tran = new Tran(stor);
			tran.startStore();
			data = new byte[i];
			for (int j = 0; j < i; ++j)
				data[j] = (byte) j;
			stor.buffer(stor.alloc(data.length)).put(data);
			tran.endStore();
		}
		check(stor, true, 0, NCOMMITS);
		assertTrue(new Check(stor).fastcheck());
	}

	@Test
	public void check_corrupt() {
		Tran tran = new Tran(stor);
		tran.startStore();
		byte[] data = new byte[] { 1 };
		stor.buffer(stor.alloc(data.length)).put(data);
		tran.endStore();

		tran = new Tran(stor);
		tran.startStore();
		data = new byte[] { 2 };
		ByteBuffer buf = stor.buffer(stor.alloc(data.length));
		buf.put(data);
		tran.endStore();

		check(stor, true, 48, 2);
		assertTrue(new Check(stor).fastcheck());

		buf.put(0, (byte) 3); // corrupt second commit

		check(stor, false, 24, 1);
		assertFalse(new Check(stor).fastcheck());
	}

	@Test
	public void fix() {
		File tempfile = FileUtils.tempfile();
		MmapFile stor = new MmapFile(tempfile, "rw");
		try {
			Tran tran = new Tran(stor);
			tran.startStore();
			byte[] data = new byte[] { 1 };
			stor.buffer(stor.alloc(data.length)).put(data);
			tran.endStore();

			tran = new Tran(stor);
			tran.startStore();
			data = new byte[] { 2 };
			ByteBuffer buf = stor.buffer(stor.alloc(data.length));
			buf.put(data);
			tran.endStore();

			check(stor, true, 48, 2);

			buf.put(0, (byte) 3); // corrupt second commit

			long okSize = check(stor, false, 24, 1);

			stor.close();
			Fix.fix(tempfile.toString(), okSize);

			stor = new MmapFile(tempfile, "rw");
			check(stor, true, 24, 1);

		} finally {
			stor.close();
			tempfile.delete();
		}
	}

	private long check(Storage stor, boolean result, long okSize, int nCommits) {
		Check check = new Check(stor);
		assertThat(check.fullcheck(), is(result));
		if (okSize != 0)
			assertThat("okSize", check.okSize(), is(okSize));
		assertThat("nCommits", check.nCommits(), is(nCommits));
		return check.okSize();
	}

}