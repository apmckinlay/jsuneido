/* Copyright 2016 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static suneido.util.ByteBuffers.bufsEmpty;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;

import com.google.common.collect.Lists;

/**
 * Used by client-server connection.
 * Wraps a Java Channel, provides buffering,
 * uses {@link Serializer} for serialization.
 * Usage is to call put methods followed by write.
 * <p>
 * Assumes strict request/response.
 * Reading and writing cannot overlap since a single buffer is used for both.
 * i.e. Must finish receiving request before starting to send response.
 * Does NOT handle pipelining or multiplexing.
 */
public class SuChannel extends Serializer {
	private final Channel channel;
	public static final int BUFSIZE =  8 * 1024;
	private ByteBuffer buf;
	private ArrayList<ByteBuffer> bufs = Lists.newArrayList();
	private static final ByteBuffer[] empty = new ByteBuffer[0];
	enum Mode { READ, WRITE };
	private Mode mode = Mode.WRITE;

	public SuChannel(Channel channel) {
		this.channel = channel;
		this.buf = ByteBuffer.allocate/*Direct*/(BUFSIZE);
		bufs.add(buf);
	}

	public SuChannel(Channel channel, ByteBuffer buf) {
		this.channel = channel;
		this.buf = buf;
		bufs.add(buf);
	}

	void close() {
		try {
			channel.close();
		} catch (IOException e) {
			// ignore
		}
	}

	@Override
	protected ByteBuffer allow(int nBytes) {
		if (mode == Mode.READ)
			flip();
		if (nBytes > buf.remaining()) {
			buf = ByteBuffer.allocateDirect((nBytes < BUFSIZE / 2) ? BUFSIZE : nBytes);
			bufs.add(buf);
		}
		return buf;
	}

	/**
	 * Use put(ByteBuffer) and getBuffer() for size prefix.
	 * NOTE: References to buffers may be held until write.
	 * The application must not modify them.
	 */
	@Override
	protected void putBuffer(ByteBuffer src) {
		int n = src.remaining();
		if (n < 1024 && // small
				n < buf.remaining()) { // and fits in the current buffer
			// copy
			ByteBuffer buf = allow(n);
			buf.put(src);
		} else {
			// just reference the passed in buffer
			src = src.slice(); // trim capacity
			src.position(src.limit());
			bufs.add(src);
			// remaining space in previous buffer
			buf = buf.slice();
			bufs.add(buf); // remaining space
		}
	}

	/**
	 * Write the buffered data to the channel
	 * and resets the buffers.
	 */
	public void write() {
		ByteBuffer[] data = bufs.toArray(empty);
		for (ByteBuffer b : data)
			b.flip();
		while (! bufsEmpty(data)) {
			try {
				((GatheringByteChannel) channel).write(data);
			} catch (IOException e) {
				throw new RuntimeException("DbmsRemote write", e);
			}
		}
		clear();
	}

	/** Discards all but the starting buffer which is cleared */
	public void clear() {
		buf = bufs.get(0);
		buf.clear();
		bufs.clear();
		bufs.add(buf);
		mode = Mode.WRITE;
	}

	/**
	 * Checks that all the data has been retrieved
	 * and resets the buffers ready for writing.
	 */
	private void flip() {
		assert mode == Mode.READ;
		if (buf.remaining() != 0)
			throw new RuntimeException("unread data (" + buf.remaining() + " bytes)");
		assert bufs.size() == 1;
		buf = bufs.get(0); // in case we allocated a temporary larger one
		buf.clear();
		mode = Mode.WRITE;
	}

	/**
	 * Ensure a given number of bytes are available to be fetched,
	 * reading from the channel as necessary.
	 */
	 @Override
	protected ByteBuffer need(int n) {
		// we have data between 0 and limit
		// 0 to position has already been fetched,
		// position to limit is available to fetch,
		// limit to capacity can be used to read more
		if (mode == Mode.WRITE) {
			mode = Mode.READ;
			buf.position(0);
			buf.limit(0);
		}
		if (n <= buf.remaining())
			return buf;
		int prevPos;
		if (n <= buf.capacity() - buf.limit()) {
			// sufficient space in current buffer
			prevPos = buf.position();
			buf.position(buf.limit());
			buf.limit(buf.capacity());
		} else {
			// insufficient space, allocate a bigger buffer
			newBuffer(n);
			prevPos = 0;
		}
		try {
			while (buf.position() - prevPos < n) {
				if (0 >= ((ReadableByteChannel) channel).read(buf))
					throw new RuntimeException("read failed in SuChannel");
			}
		} catch (IOException e) {
			throw new RuntimeException("read error in SuChannel", e);
		}
		buf.limit(buf.position());
		buf.position(prevPos);
		assert buf.remaining() >= n;
		return buf;
	}

	@Override
	void forceNewBuffer(int n) {
		newBuffer(n);
		buf.limit(buf.position());
		buf.position(0);
	}

	private void newBuffer(int n) {
		ByteBuffer oldbuf = buf;
		buf = ByteBuffer.allocateDirect(oldbuf.remaining() + n);
		buf.put(oldbuf); // copy unread data to new buffer
	}

}
