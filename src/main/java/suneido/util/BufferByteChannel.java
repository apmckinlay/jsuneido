/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/** In memory ByteChannel for testing */
public class BufferByteChannel implements ByteChannel {
	private final ByteBuffer buf;

	public BufferByteChannel(int capacity) {
		buf = ByteBuffer.allocate(capacity);
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		int n = src.remaining();
		buf.put(src);
		return n;
	}

	public void flip() {
		buf.flip();
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		int n = 0;
		while (buf.hasRemaining() && dst.hasRemaining()) {
			dst.put(buf.get());
			++n;
		}
		return n;
	}

	public ByteBuffer getBuffer() {
		return (ByteBuffer) buf.duplicate().flip();
	}

	@Override
	public void close() throws IOException {
	}

}
