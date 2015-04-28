/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.zip.Adler32;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
/** WARNING slightly different from suneido.util.checksum */
class Checksum {
	private final Adler32 cksum = new Adler32();
	private final byte[] bytes = new byte[1024];

	/** starts at current position, advances position to limit */
	void update(ByteBuffer buf) {
		update(buf, buf.remaining());
	}

	/** starts at current position, advances position by len */
	void update(ByteBuffer buf, int len) {
		if (buf.hasArray()) {
			int pos = buf.position();
			cksum.update(buf.array(), buf.arrayOffset() + pos, len);
			buf.position(pos + len);
		} else {
			for (int i = 0; i < len; i += bytes.length) {
				int n = Math.min(bytes.length, len - i);
				buf.get(bytes, 0, n);
				cksum.update(bytes, 0, n);
			}
		}
	}

	void update(byte[] bytes) {
		cksum.update(bytes);
	}

	int getValue() {
		return (int) cksum.getValue();
	}

	void reset() {
		cksum.reset();
	}

}
