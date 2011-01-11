/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

/**
 * The same encoding as
 * <a href="http://code.google.com/apis/protocolbuffers/docs/encoding.html#varints">
 * Google Protocol Buffer varint</a>
 */
public class Varint {

	/** @return The number of bytes required to encode the int. */
	public static int length(int n) {
	    if ((n & (0xffffffff <<  7)) == 0) return 1;
	    if ((n & (0xffffffff << 14)) == 0) return 2;
	    if ((n & (0xffffffff << 21)) == 0) return 3;
	    if ((n & (0xffffffff << 28)) == 0) return 4;
	    return 5;
	}

	public static void encode(byte[] buf, int n) {
		int i = 0;
	    while (true)
	        if ((n & ~0x7F) == 0) {
	          buf[i++] = (byte) n;
	          return;
	        } else {
	          buf[i++] = (byte) ((n & 0x7F) | 0x80);
	          n >>>= 7;
	        }
	}

	public static void encode(ByteBuffer buf, int n) {
	    while (true)
	        if ((n & ~0x7F) == 0) {
	          buf.put((byte) n);
	          return;
	        } else {
	          buf.put((byte) ((n & 0x7F) | 0x80));
	          n >>>= 7;
	        }
	}

	public static int decode(byte[] buf) {
		int i = 0;
		int result = 0;
		for (int offset = 0; offset < 32; offset += 7) {
			final int b = buf[i++];
			result |= (b & 0x7f) << offset;
			if ((b & 0x80) == 0)
				return result;
		}
		throw new RuntimeException("Varint decode invalid data");

	}

}
