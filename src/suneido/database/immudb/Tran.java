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
	public final Storage stor;
	private final Redirects redirs;
	final IntRefs intrefs = new IntRefs();
	private int size_adr;

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
		size_adr = stor.alloc(SIZEOF_INT); // to hold the size of the commit
	}

	/**
	 * Store the size at the beginning of the commit
	 * and the checksum at the end.
	 * The checksum includes the size.
	 * The size does NOT include the checksum.
	 */
	public void endStore() {
		int size = stor.sizeFrom(size_adr);
		stor.buffer(size_adr).putInt(size);

		int cksum = checksum();
		stor.buffer(stor.alloc(SIZEOF_INT)).putInt(cksum);
	}

	public int checksum() {
		Checksum cksum = new Checksum();
		Iterator<ByteBuffer> iter = stor.iterator(size_adr);
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
