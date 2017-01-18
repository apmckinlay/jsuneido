/* Copyright 2016 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Random;

/**
 * Takes the place of a SocketChannel for connecting
 * {@link DbmsClientBinary} to {@link DbmsServer}
 */
public class TestChannel
		implements Channel, ReadableByteChannel, GatheringByteChannel {
	Runnable server;
	static final int BUFSIZE = 8 * 1024;
	ByteBuffer buf = ByteBuffer.allocate(BUFSIZE);
	boolean inServer = false;

	TestChannel(Runnable server) {
		this.server = server;
	}

	@Override
	public long write(ByteBuffer[] bufs) {
		buf.clear();
		for (ByteBuffer b : bufs)
			buf.put(b);
		buf.flip();
		if (! inServer) {
			inServer = true;
			try {
				server.run();
			} finally {
				inServer = false;
			}
		}
		return buf.limit();
	}

	private static final Random rand = new Random();

	@Override
	public int read(ByteBuffer dst) {
		int n = 1 + rand.nextInt(Math.min(buf.remaining(), dst.remaining()));
		for (int i = 0; i < n; ++i)
			dst.put(buf.get());
		return n;
	}

	@Override
	public boolean isOpen() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() {
	}

	@Override
	public int write(ByteBuffer src) {
		ByteBuffer[] bufs = new ByteBuffer[1];
		bufs[0] = src;
		return (int) write(bufs);
	}

	@Override
	public long write(ByteBuffer[] srcs, int offset, int length) {
		throw new UnsupportedOperationException();
	}

}
