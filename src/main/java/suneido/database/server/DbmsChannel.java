/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static suneido.Trace.trace;
import static suneido.Trace.Type.CLIENTSERVER;
import static suneido.util.ByteBuffers.putStringToByteBuffer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.SuException;
import suneido.util.Tr;

/**
 * Channel input/output for {@link DbmsRemote}
 */
@NotThreadSafe
public class DbmsChannel {
	private SocketChannel channel;
	private ByteBuffer rbuf = ByteBuffer.allocate(10000);
	private ByteBuffer wbuf = ByteBuffer.allocate(10000);
	private final ByteBuffer wbufs[] = new ByteBuffer[2];

	public DbmsChannel(String ip, int port) {
		try {
			channel = SocketChannel.open(new InetSocketAddress(ip, port));
		} catch (Exception e) {
			throw new SuException("can't connect to " + ip + ":" + port, e);
		}
	}

	/**
	 * @return The internal buffer which should only be used temporarily
	 */
	public ByteBuffer read(int n) {
		if (rbuf.limit() < n)
			rbuf = realloc(rbuf, n);
		while (rbuf.position() < n) {
			try {
				channel.read(rbuf);
			} catch (IOException e) {
				throw new SuException("error", e);
			}
		}
		assert rbuf.position() == n;
		ByteBuffer result = rbuf.duplicate();
		rbuf.clear();
		result.flip();
		return result;
	}

	/**
	 * @return A new buffer that is owned by caller
	 */
	public ByteBuffer readNew(int n) {
		ByteBuffer buf = ByteBuffer.allocate(n);
		if (rbuf.remaining() > 0) {
			rbuf.flip();
			int limit = rbuf.limit();
			if (limit > n)
				rbuf.limit(n);
			buf.put(rbuf);
			rbuf.limit(limit);
			rbuf.compact();
		}
		while (buf.position() < n) {
			try {
				channel.read(buf);
			} catch (IOException e) {
				throw new SuException("error", e);
			}
		}
		buf.flip();
		return buf;
	}

	// may leave unread data in rbuf
	public String readLine() {
		int nl;
		while (-1 == (nl = indexOf(rbuf, (byte) '\n'))) {
			if (rbuf.remaining() == 0)
				rbuf = realloc(rbuf, 2 * rbuf.capacity());
			try {
				channel.read(rbuf);
			} catch (IOException e) {
				throw new SuException("error", e);
			}
		}
		rbuf.flip();
		String s = getString(rbuf, nl + 1);
		rbuf.compact();
		trace(CLIENTSERVER, "    => " + s);
		if (s.startsWith("ERR"))
			throw new SuException(s.substring(4) + " (from server)");
		return s;
	}

	private static ByteBuffer realloc(ByteBuffer oldbuf, int n) {
		ByteBuffer buf = ByteBuffer.allocateDirect(n);
		oldbuf.flip();
		buf.put(oldbuf);
		return buf;
	}

	private static int indexOf(ByteBuffer buf, byte b) {
		for (int i = 0; i < buf.position(); ++i)
			if (buf.get(i) == b)
				return i;
		return -1;
	}

	public static String getString(ByteBuffer buf, int n) {
		StringBuilder sb = new StringBuilder(n);
		for (int i = 0; i < n; ++i)
			sb.append((char) (buf.get() & 0xff));
		String s = sb.toString();
		assert s.endsWith("\r\n");
		return s.substring(0, s.length() - 2);
	}

	public void writeLine(String cmd) {
		trace(CLIENTSERVER, cmd);
		write(cmd + "\n");
	}

	public void writeLine(String cmd, String s) {
		trace(CLIENTSERVER, cmd + " " + s);
		assert rbuf.position() == 0;
		write(cmd + " " + Tr.tr(s, " \r\n", " ").trim() + "\r\n");
	}

	public void write(String s) {
		writeBuf(s);
		wbuf.flip();
		try {
			channel.write(wbuf);
		} catch (IOException e) {
			throw new SuException("error", e);
		} finally {
			wbuf.clear();
		}
	}

	public void writeLineBuf(String cmd, String s) {
		trace(CLIENTSERVER, cmd + " " + s);
		assert rbuf.position() == 0;
		writeBuf(cmd + " " + Tr.tr(s, " \r\n", " ").trim() + "\r\n");
	}

	public void writeBuf(String s) {
		if (wbuf.remaining() < s.length())
			wbuf = realloc(wbuf, wbuf.capacity() + s.length());
		putStringToByteBuffer(s, wbuf);
	}

	public void write(ByteBuffer buf) {
		wbuf.flip();
		wbufs[0] = wbuf;
		wbufs[1] = buf;
		try {
			channel.write(wbufs);
		} catch (IOException e) {
			throw new SuException("error", e);
		} finally {
			wbuf.clear();
		}
	}

	public void close() {
		try {
			channel.close();
		} catch (IOException e) {
			throw new SuException("error", e);
		}
	}

	public InetAddress getInetAddress() {
		return channel.socket().getInetAddress();
	}

}
