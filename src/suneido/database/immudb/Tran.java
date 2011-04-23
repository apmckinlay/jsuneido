/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.Iterator;

import suneido.database.immudb.DbHashTrie.Entry;
import suneido.database.immudb.DbHashTrie.IntEntry;
import suneido.database.immudb.DbHashTrie.Translator;

/**
 * Transaction "context".
 */
public class Tran implements Translator {
	private static final int SIZEOF_INT = 4;
	static final int HEAD_SIZE = 2 * SIZEOF_INT; // size and datetime
	static final int TAIL_SIZE = 2 * SIZEOF_INT; // checksum and size
	{ assert TAIL_SIZE == MmapFile.align(TAIL_SIZE); }
	public final Storage stor;
	private final Redirects redirs;
	public final IntRefs intrefs = new IntRefs();
	private int head_adr;

	public Tran(Storage stor) {
		this.stor = stor;
		redirs = new Redirects(DbHashTrie.empty(stor));
	}

	public Tran(Storage stor, int redirs) {
		this.stor = stor;
		this.redirs = new Redirects(DbHashTrie.from(stor, redirs));
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
		stor.protect(); // enable output
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
		int size = (int) stor.sizeFrom(head_adr);
		stor.buffer(head_adr).putInt(size).putInt(datetime());

		int cksum = checksum();
		stor.buffer(tail_adr).putInt(cksum).putInt(size);
		stor.protectAll(); // can't output outside tran
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
		return redirs.store(this);
	}

	@Override
	public int translate(Entry e) {
		int x = ((IntEntry) e).value;
		return IntRefs.isIntRef(x) ? getAdr(x) : x;
	}

	public Record getrec(int adr) {
		return new Record(stor.buffer(redir(adr)));
	}

}
