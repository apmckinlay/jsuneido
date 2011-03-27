package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.zip.Adler32;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
/** WARNING slightly different from suneido.util.checksum */
public class Checksum {
	private final Adler32 cksum = new Adler32();
	private final static byte[] bytes = new byte[1024];

	/** starts at current position, advances position to limit */
	public void update(ByteBuffer buf) {
		update(buf, buf.remaining());
	}

	/** starts at current position, advances position by len */
	public void update(ByteBuffer buf, int len) {
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

	public void update(byte[] bytes) {
		cksum.update(bytes);
	}

	public int getValue() {
		return (int) cksum.getValue();
	}

	public void reset() {
		cksum.reset();
	}

}
