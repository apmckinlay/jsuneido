/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;
import java.util.zip.Adler32;

import org.junit.Test;

public class TranTest {

	@Test
	public void checksum_nothing() {
		Tran tran = new Tran(new TestStorage());
		tran.startStore();
		Adler32 adler32 = new Adler32();
		assertThat(tran.checksum(), is((int) adler32.getValue()));
	}

	@Test
	public void checksum() {
		Adler32 adler32 = new Adler32();
		Tran tran = new Tran(new TestStorage());
		tran.startStore();

		byte[] data = new byte[] { 1, 2, 3, -1, 0, 0, 0, 0 };
		int adr = tran.stor.alloc(data.length);
		ByteBuffer buf = tran.stor.buffer(adr);
		buf.put(data);
		adler32.update(data);

		data = new byte[] { 12, 34, 56, 78, 0, 0, 0, 0 };
		adr = tran.stor.alloc(data.length);
		buf = tran.stor.buffer(adr);
		buf.put(data);
		adler32.update(data);

		assertThat(tran.checksum(), is((int) adler32.getValue()));
	}

}
