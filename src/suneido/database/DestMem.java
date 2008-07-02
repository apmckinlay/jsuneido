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

	public long alloc(int size) {
		nodes.add(ByteBuffer.allocate(size));
		return nodes.size() << Mmfile.SHIFT; // start at one not zero
	}

	public ByteBuffer adr(long offset) {
		return nodes.get((int) (offset >> Mmfile.SHIFT) - 1);
	}

	public long size() {
		return (nodes.size() + 1) << Mmfile.SHIFT;
	}

}
