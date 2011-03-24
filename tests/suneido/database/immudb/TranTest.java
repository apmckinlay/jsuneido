/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;
import java.util.Iterator;

import org.junit.Test;

import suneido.util.Checksum;

public class TranTest {

	@Test
	public void check_empty_commit() {
		Tran tran = new Tran(new TestStorage());
		tran.startStore();
		tran.endStore();

		int size = check(tran);
		assertThat(size, is(8)); // align
	}

	@Test
	public void check_one_commit() {
		Tran tran = new Tran(new TestStorage());
		tran.startStore();

		byte[] data = new byte[] { 1 };
		int adr = tran.stor.alloc(data.length);
		tran.stor.buffer(adr).put(data);

		data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		adr = tran.stor.alloc(data.length);
		tran.stor.buffer(adr).put(data);

		tran.endStore();

		int size = check(tran);
		assertThat(size, is(32));
	}

	public int check(Tran tran) {
		Iterator<ByteBuffer> iter = tran.stor.iterator(Storage.FIRST_ADR);
		ByteBuffer buf = iter.next();
		int size = buf.getInt(0);

		Checksum cksum = new Checksum();
		int n;
		for (int i = 0; i < size; i += n) {
			if (buf.remaining() == 0)
				buf = iter.next();
			n = Math.min(size - i, buf.remaining());
			cksum.update(buf, n);
		}
		if (buf.remaining() == 0)
			buf = iter.next();
		int stor_cksum = buf.getInt();
		assertThat(stor_cksum, is(cksum.getValue()));

		return size;
	}

}
