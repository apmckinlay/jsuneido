/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.nio.ByteBuffer;

public class TestByteBufferCopy {

	public static void main(String[] args) {
		ByteBuffer src = ByteBuffer.allocateDirect(4096);
		long t = System.currentTimeMillis();
		for (int i = 0; i < 5000000; ++i) {
//			ByteBuffer dst = ByteBuffer.allocate(4096);
//			src.position(0);
//			src.limit(4096);
//			dst.put(src);
			ByteBuf.wrap(src).copy(4096);
		}
		t = System.currentTimeMillis() - t;
		System.out.println(t + " ms");
	}

}
