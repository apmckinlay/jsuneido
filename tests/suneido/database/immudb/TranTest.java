/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;

import org.junit.Test;

public class TranTest {

	@Test
	public void check_empty_commit() {
		Storage stor = new TestStorage();
		Tran tran = new Tran(stor);
		tran.startStore();
		tran.endStore();

		check(stor, true, 8 + 8);
	}

	@Test
	public void check_one_commit() {
		Storage stor = new TestStorage();
		Tran tran = new Tran(stor);
		tran.startStore();

		byte[] data = new byte[] { 1 };
		int adr = stor.alloc(data.length);
		stor.buffer(adr).put(data);

		data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		adr = stor.alloc(data.length);
		stor.buffer(adr).put(data);

		tran.endStore();

		check(stor, true, 8 + 8 + 16 + 8);
	}

	@Test
	public void check_several_commits() {
		Storage stor = new TestStorage();

		Tran tran = new Tran(stor);
		tran.startStore();
		byte[] data = new byte[] { 1 };
		stor.buffer(stor.alloc(data.length)).put(data);
		tran.endStore();

		stor.alloc(16); // simulate chunk padding

		tran = new Tran(stor);
		tran.startStore();
		data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		stor.buffer(stor.alloc(data.length)).put(data);
		tran.endStore();

		check(stor, true, 24 + 16 + 32);
	}

	@Test
	public void check_corrupt() {
		Storage stor = new TestStorage();

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

		check(stor, true, 48);

		buf.put(0, (byte) 3); // corrupt it

		check(stor, false, 24);
	}

	private void check(Storage stor, boolean result, long okSize) {
		Check check = new Check(stor);
		assertThat(check.check(), is(result));
		assertThat(check.okSize(), is(okSize));
	}

}
