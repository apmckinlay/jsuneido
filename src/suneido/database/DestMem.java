package suneido.database;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * @author Andrew McKinlay
 *         <p>
 *         <small>Copyright 2008 Suneido Software Corp. All rights reserved.
 *         Licensed under GPLv2.</small>
 *         </p>
 */
public class DestMem implements Destination {
	private final ArrayList<ByteBuffer> nodes = new ArrayList<ByteBuffer>();

	public long alloc(int size, byte type) {
		nodes.add(ByteBuffer.allocate(size));
		return nodes.size() * Mmfile.ALIGN; // start at one not zero
	}

	public ByteBuffer adr(long adr) {
		ByteBuffer buf = nodes.get((int) (adr / Mmfile.ALIGN) - 1);
		int offset = (int) adr % Mmfile.ALIGN;
		if (offset != 0) {
			buf.position(offset);
			buf = buf.slice();
		}
		buf.position(0);
		return buf;
	}

	public long size() {
		return (nodes.size() + 1) * Mmfile.ALIGN;
	}

	public long first() {
		return 1 * Mmfile.ALIGN;
	}

	public int length(long adr) {
		return adr(adr).capacity();
	}

	public void sync() {
	}

	public void close() {
	}

}
