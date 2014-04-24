/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.nio.ByteBuffer;
import java.util.zip.Adler32;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class Checksum {
	private final Adler32 cksum = new Adler32();
	private static final byte[] bytes = new byte[1024];

	public synchronized void update(ByteBuffer buf) {
		update(buf, buf.limit());
	}

	/** starts at position 0, leaves position at len */
	public synchronized void update(ByteBuffer buf, int len) {
		buf.position(0);
		if (buf.hasArray()) {
			cksum.update(buf.array(), buf.arrayOffset(), len);
			buf.position(len);
		}
		else {
			for (int i = 0; i < len; i += bytes.length) {
				int n = Math.min(bytes.length, len - i);
				buf.get(bytes, 0, n);
				cksum.update(bytes, 0, n);
			}
		}
	}

	public synchronized void update(byte[] bytes) {
		cksum.update(bytes);
	}

	public synchronized int getValue() {
		return (int) cksum.getValue();
	}

	public synchronized void reset() {
		cksum.reset();
	}

}
