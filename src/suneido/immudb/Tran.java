/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.immudb.DbHashTrie.Entry;
import suneido.immudb.DbHashTrie.IntEntry;
import suneido.immudb.DbHashTrie.StoredIntEntry;
import suneido.immudb.DbHashTrie.Translator;

import com.google.common.primitives.Ints;

/**
 * Transaction "context". Manages IntRefs and Storage.
 */
@NotThreadSafe
class Tran implements Translator {
	static final int HEAD_SIZE = 2 * Ints.BYTES; // size and datetime
	static final int TAIL_SIZE = 2 * Ints.BYTES; // checksum and size
	{ assert TAIL_SIZE == MmapFile.align(TAIL_SIZE); }
	final Storage dstor;
	final Storage istor;
	final IntRefs intrefs = new IntRefs();
	private int head_adr = 0;

	Tran(Storage stor, Storage istor) {
		this.dstor = stor;
		this.istor = istor;
	}

	int refToInt(Object ref) {
		return intrefs.refToInt(ref);
	}

	Object intToRef(int intref) {
		return intrefs.intToRef(intref);
	}

	void update(int intref, Object ref) {
		intrefs.update(intref, ref);
	}

	void startStore() {
		intrefs.startStore();
		if (head_adr == 0)
			allowStore();
	}

	void allowStore() {
		dstor.protect(); // enable output
		head_adr = dstor.alloc(HEAD_SIZE); // to hold size and datetime
	}

	/**
	 * Store the size and date/time at the beginning of the commit (head)
	 * and the checksum and size at the end (tail).
	 * The checksum includes the head and a zero tail.
	 * The size includes the head and the tail
	 * @return The checksum of the commit
	 */
	StoreInfo endStore() {
		assert head_adr != 0;
		try {
			int tail_adr = dstor.alloc(TAIL_SIZE);
			int size = (int) dstor.sizeFrom(head_adr);
			dstor.buffer(head_adr).putInt(size).putInt(datetime());

			int cksum = dstor.checksum(head_adr);
			dstor.buffer(tail_adr).putInt(cksum).putInt(size);
			dstor.protectAll(); // can't output outside tran

			return new StoreInfo(cksum, head_adr);
		} finally {
			head_adr = 0;
		}
	}

	/**
	 * Abort a store by writing a zero date in the header.
	 * Don't bother calculating checksum, just store zero.
	 */
	void abortIncompleteStore() {
		if (head_adr == 0)
			return;
		int tail_adr = dstor.alloc(TAIL_SIZE);
		int size = (int) dstor.sizeFrom(head_adr);
		dstor.buffer(head_adr).putInt(size).putInt(0); // zero date
		dstor.buffer(tail_adr).putInt(0).putInt(size); // zero checksum
		dstor.protectAll(); // can't output outside tran
		head_adr = 0;
	}

	static class StoreInfo {
		final int cksum;
		final int adr;

		public StoreInfo(int cksum, int adr) {
			this.cksum = cksum;
			this.adr = adr;
		}

		@Override
		public String toString() {
			return "adr " + adr + " cksum " + Integer.toHexString(cksum);
		}
	}

	/**
	 * Returns the current time in seconds since Jan. 1, 1970 UTC
	 * Only good till 2038.
	 * NOTE: Could be a performance bottleneck - in which case
	 * a background thread could update a "current time" at larger intervals
	 * e.g. once per second.
	 */
	static int datetime() {
		return (int) (System.currentTimeMillis() / 1000);
	}

	void setAdr(int intref, int adr) {
		intrefs.setAdr(intref, adr);
	}

	int getAdr(int intref) {
		return intrefs.getAdr(intref);
	}

	@Override
	public Entry translate(Entry e) {
		IntEntry ie = (IntEntry) e;
		int key = ie.key;
		int val = ie.value;
		if (IntRefs.isIntRef(val))
			val = getAdr(val);
		return new StoredIntEntry(key, val);
	}

	DataRecord getrec(int adr) {
		if (IntRefs.isIntRef(adr)) {
			DataRecord r = (DataRecord) intToRef(adr);
			r.address(adr);
			return r;
		} else
			return new DataRecord(dstor, adr);
	}

	/** for ExclusiveTransaction */
	void reset() {
		head_adr = 0;
		intrefs.clear();
	}

}
