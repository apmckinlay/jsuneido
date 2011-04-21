/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * Transaction "context".
 */
public class Tran implements Translator {
	private static final int SIZEOF_INT = 4;
	static final int HEAD_SIZE = 2 * SIZEOF_INT; // size and datetime
	static final int TAIL_SIZE = 2 * SIZEOF_INT; // checksum and size
	{ assert TAIL_SIZE == MmapFile.align(TAIL_SIZE); }
	public final Context context;
	private final Redirects redirs;
	private int head_adr;

	public Tran(Storage stor) {
		context = new Context(stor);
		this.redirs = new Redirects(DbHashTree.empty(context));
	}

	public Tran(Storage stor, int redirs) {
		context = new Context(stor);
		this.redirs = new Redirects(DbHashTree.from(context, redirs));
	}

	public int refToInt(Object ref) {
		return context.intrefs.refToInt(ref);
	}

	public Object intToRef(int intref) {
		return context.intrefs.intToRef(intref);
	}

	public int redir(int from) {
		return redirs.get(from);
	}

	public void redir(int from, Object ref) {
		assert(! (ref instanceof Number));
		if (IntRefs.isIntRef(from))
			context.intrefs.update(from, ref);
		else
			redirs.put(from, refToInt(ref));
	}

	public Redirects redirs() {
		return redirs;
	}

	public void startStore() {
		context.intrefs.startStore();
		head_adr = context.stor.alloc(HEAD_SIZE); // to hold size and datetime
	}

	/**
	 * Store the size and date/time at the beginning of the commit (head)
	 * and the checksum and size at the end (tail).
	 * The checksum includes the head and a zero tail.
	 * The size includes the head and the tail
	 */
	public void endStore() {
		int tail_adr = context.stor.alloc(TAIL_SIZE);
		int size = (int) context.stor.sizeFrom(head_adr);
		context.stor.buffer(head_adr).putInt(size).putInt(datetime());

		int cksum = checksum();
		context.stor.buffer(tail_adr).putInt(cksum).putInt(size);
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
		Iterator<ByteBuffer> iter = context.stor.iterator(head_adr);
		while (iter.hasNext())
			cksum.update(iter.next());
		return cksum.getValue();
	}

	public void setAdr(int intref, int adr) {
		context.intrefs.setAdr(intref, adr);
	}

	public int getAdr(int intref) {
		return context.intrefs.getAdr(intref);
	}

	public int storeRedirs() {
		return redirs.store(this);
	}

	@Override
	public int translate(int x) {
		return IntRefs.isIntRef(x) ? getAdr(x) : x;
	}

	public Record getrec(int adr) {
		return new Record(context.stor.buffer(redir(adr)));
	}

}
