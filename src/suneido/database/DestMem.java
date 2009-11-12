package suneido.database;

import java.util.ArrayList;

import suneido.util.ByteBuf;

/**
 * Used by tests for in-memory databases.
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class DestMem extends Destination {
	private final ArrayList<ByteBuf> nodes = new ArrayList<ByteBuf>();

	@Override
	public long alloc(int size, byte type) {
		nodes.add(ByteBuf.allocateDirect(size));
		return nodes.size() * Mmfile.ALIGN; // start at one not zero
	}

	@Override
	public ByteBuf adr(long adr) {
		ByteBuf buf = nodes.get((int) (adr / Mmfile.ALIGN) - 1);
		int offset = (int) adr % Mmfile.ALIGN;
		return buf.slice(offset);
	}

	@Override
	public long size() {
		return (nodes.size() + 1) * Mmfile.ALIGN;
	}

	@Override
	public long first() {
		return 1 * Mmfile.ALIGN;
	}

	@Override
	public int length(long adr) {
		return adr(adr).size();
	}

	@Override
	public void sync() {
	}

	@Override
	public void close() {
	}

	@Override
	public Destination unwrap() {
		return this;
	}

}
