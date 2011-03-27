/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.Iterator;

import suneido.util.Checksum;

/**
 * Transaction "context".
 */
public class Tran implements Translator {
	private static final int SIZEOF_INT = 4;
	static final int HEAD_SIZE = 2 * SIZEOF_INT; // size and datetime
	private static final int TAIL_SIZE = 2 * SIZEOF_INT; // checksum and size
	{ assert TAIL_SIZE == MmapFile.align(TAIL_SIZE); }
	public final Storage stor;
	private final Redirects redirs;
	final IntRefs intrefs = new IntRefs();
	private int head_adr;

	public Tran(Storage stor) {
		this(stor, new Redirects(DbHashTree.empty(stor)));
	}

	public Tran(Storage stor, Redirects redirs) {
		this.stor = stor;
		this.redirs = redirs;
	}

	public int refToInt(Object ref) {
		return intrefs.refToInt(ref);
	}

	public Object intToRef(int intref) {
		return intrefs.intToRef(intref);
	}

	public int redir(int from) {
		return redirs.get(from);
	}

	public void redir(int from, Object ref) {
		assert(! (ref instanceof Number));
		if (IntRefs.isIntRef(from))
			intrefs.update(from, ref);
		else
			redirs.put(from, refToInt(ref));
	}

	public Redirects redirs() {
		return redirs;
	}

	public void startStore() {
		intrefs.startStore();
		head_adr = stor.alloc(HEAD_SIZE); // to hold size and datetime
	}

	/**
	 * Store the size and date/time at the beginning of the commit (head)
	 * and the checksum and size at the end (tail).
	 * The checksum includes the head and a zero tail.
	 * The size includes the head and the tail
	 */
	public void endStore() {
		int tail_adr = stor.alloc(TAIL_SIZE);
		int size = stor.sizeFrom(head_adr);
		stor.buffer(head_adr).putInt(size).putInt(datetime());

		int cksum = checksum();
		stor.buffer(tail_adr).putInt(cksum).putInt(size);
	}

	/**
	 * Returns the current time in seconds since Jan. 1, 1970 UTC
	 * Only good till 2038
	 */
	private int datetime() {
		return (int) (System.currentTimeMillis() / 1000);
	}

	public int checksum() {
		Checksum cksum = new Checksum();
		Iterator<ByteBuffer> iter = stor.iterator(head_adr);
		while (iter.hasNext())
			cksum.update(iter.next());
		return cksum.getValue();
	}

	public void setAdr(int intref, int adr) {
		intrefs.setAdr(intref, adr);
	}

	public int getAdr(int intref) {
		return intrefs.getAdr(intref);
	}

	public int storeRedirs() {
		return redirs.store(stor, this);
	}

	@Override
	public int translate(int x) {
		return getAdr(x);
	}

}
