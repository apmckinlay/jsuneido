/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;

public class BufferByteChannelTest {

	@Test
	public void test() throws IOException {
		BufferByteChannel b = new BufferByteChannel(100);
		try {
			assertThat(b.getBuffer().position(), is(0));
			assertThat(b.getBuffer().remaining(), is(0));
			b.write(ByteBuffers.stringToBuffer("hello"));
			b.write(ByteBuffers.stringToBuffer(" world"));
			assertThat(b.getBuffer().position(), is(0));
			assertThat(b.getBuffer().remaining(), is(11));
			b.flip();
			ByteBuffer buf = ByteBuffer.allocate(11);
			assertThat(b.read(buf), is(11));
			buf.rewind();
			assertThat(ByteBuffers.bufferToString(buf), is("hello world"));
		} finally {
			b.close();
		}
	}

}
