package suneido.database;

import static java.lang.Math.min;

import java.nio.ByteBuffer;
import java.util.zip.Adler32;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
class Checksum {

	private final Adler32 cksum = new Adler32();

	private static byte[] bytes = new byte[256];

	// used by Transaction.writeCommitRecord
	synchronized void add(ByteBuffer buf, int len) {
		buf.position(0);
		for (int i = 0; i < len; i += bytes.length) {
			int n = min(bytes.length, len - i);
			buf.get(bytes, 0, n);
			cksum.update(bytes, 0, n);
		}
	}

	synchronized void writeCommit(ByteBuffer buf) {
		// include commit in checksum, but don't include checksum itself
		add(buf, buf.position());
		buf.putInt((int) cksum.getValue());
		cksum.reset();
	}

}
