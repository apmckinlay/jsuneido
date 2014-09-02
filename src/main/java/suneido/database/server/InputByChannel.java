/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class InputByChannel {
	private static final int INITIAL_SIZE = 16 * 1024;
	private static final int MAX_SIZE = 64 * 1024;
	private final SocketChannel channel;
	private ByteBuffer buf = ByteBuffer.allocate(INITIAL_SIZE);
	private int nlPos;

	public InputByChannel(SocketChannel channel) {
		this.channel = channel;
	}

	public ByteBuffer readLine() {
		do {
			if (read() == -1)
				return null;
			nlPos = indexOf(buf, (byte) '\n');
		} while (nlPos == -1);
		ByteBuffer line = buf.duplicate();
		line.position(0);
		line.limit(++nlPos);
		return line;
	}

	private int read() {
		if (buf.remaining() == 0) {
			ByteBuffer oldbuf = buf;
			buf = ByteBuffer.allocate(2 * oldbuf.capacity());
			oldbuf.flip();
			buf.put(oldbuf);
		}
		try {
			return channel.read(buf);
		} catch (IOException e) {
			// we get this if the client aborts the connection
			return -1;
		}
	}

	private static int indexOf(ByteBuffer buf, byte b) {
		// NOTE: use buf.position because buf not flipped
		for (int i = 0; i < buf.position(); ++i)
			if (buf.get(i) == b)
				return i;
		return -1;
	}

	public ByteBuffer readExtra(int n) {
		while (buf.position() < nlPos + n)
			if (read() == -1)
				return null;
		buf.flip();
		buf.position(nlPos);
		ByteBuffer result = buf.slice();
		if (buf.capacity() <= MAX_SIZE)
			buf.clear();
		else // don't keep buffer bigger than max
			buf = ByteBuffer.allocateDirect(MAX_SIZE);
		return result;
	}

}

