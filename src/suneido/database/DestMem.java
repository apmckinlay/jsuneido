package suneido.database;

import java.util.ArrayList;

import suneido.util.ByteBuf;

/**
 * @author Andrew McKinlay
 *         <p>
 *         <small>Copyright 2008 Suneido Software Corp. All rights reserved.
 *         Licensed under GPLv2.</small>
 *         </p>
 */
public class DestMem implements Destination {
	private final ArrayList<ByteBuf> nodes = new ArrayList<ByteBuf>();

	public long alloc(int size, byte type) {
		nodes.add(ByteBuf.allocate(size));
		return nodes.size() * Mmfile.ALIGN; // start at one not zero
	}

	public ByteBuf adr(long adr) {
		ByteBuf buf = nodes.get((int) (adr / Mmfile.ALIGN) - 1);
		int offset = (int) adr % Mmfile.ALIGN;
		return buf.slice(offset);
	}

	public ByteBuf adrForWrite(long offset) {
		return adr(offset);
	}

	public long size() {
		return (nodes.size() + 1) * Mmfile.ALIGN;
	}

	public long first() {
		return 1 * Mmfile.ALIGN;
	}

	public int length(long adr) {
		return adr(adr).size();
	}

	public void sync() {
	}

	public void close() {
	}

	public Destination unwrap() {
		return this;
	}

}
