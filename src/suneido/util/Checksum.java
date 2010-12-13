package suneido.util;

import java.nio.ByteBuffer;
import java.util.zip.Adler32;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class Checksum {
	private final Adler32 cksum = new Adler32();
	private final static byte[] bytes = new byte[1024];

	public synchronized void add(ByteBuffer buf, int len) {
		buf.position(0);
		if (buf.hasArray())
			cksum.update(buf.array(), buf.arrayOffset(), len);
		else {
			for (int i = 0; i < len; i += bytes.length) {
				int n = Math.min(bytes.length, len - i);
				buf.get(bytes, 0, n);
				cksum.update(bytes, 0, n);
			}
		}
	}

	public synchronized long getValue() {
		return cksum.getValue();
	}

	public synchronized void reset() {
		cksum.reset();
	}

}
