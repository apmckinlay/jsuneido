/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.zip.Adler32;

/**
 * Transaction "context".
 */
public class Tran implements Translator {
	public final Storage stor;
	private final Redirects redirs;
	final IntRefs intrefs = new IntRefs();
	private Iterator<ByteBuffer> stor_iter;

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
		stor_iter = stor.iterator();
		intrefs.startStore();
	}

	public int checksum() {
		Adler32 cksum = new Adler32();
		while (stor_iter.hasNext()) {
			ByteBuffer buf = stor_iter.next();
			for (int i = 0; i < buf.limit(); ++i)
				cksum.update(buf.get(i));
		}
		return (int) cksum.getValue();
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
