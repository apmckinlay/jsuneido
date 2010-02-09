package suneido.database;

import static suneido.SuException.verify;

import java.nio.ByteBuffer;
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
	private final ArrayList<ByteBuffer> nodes = new ArrayList<ByteBuffer>();
	private final ArrayList<Byte> types = new ArrayList<Byte>();

	@Override
	public long alloc(int size, byte type) {
		nodes.add(ByteBuffer.allocateDirect(size));
		types.add(type);
		return nodes.size() * Mmfile.ALIGN; // start at one not zero
	}

	@Override
	public ByteBuf adr(long adr) {
		verify(adr > 0);
		return ByteBuf.wrap(
				nodes.get((int) (adr / Mmfile.ALIGN) - 1),
				(int) adr % Mmfile.ALIGN);
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
	public long last() {
		return nodes.size() * Mmfile.ALIGN;
	}

	@Override
	public byte type(long adr) {
		return types.get((int) (adr / Mmfile.ALIGN) - 1);
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
