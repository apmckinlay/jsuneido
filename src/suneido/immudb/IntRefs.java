/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Used to assign integer values to in-memory references
 * so they can be stored in data structures that normally
 * contain integer offsets in the database file.
 * <p>
 * Returned int's have the upper 12 bits set to 1
 * to distinguish them from database offsets from {@link IntLongs}
 */
@NotThreadSafe
class IntRefs implements Iterable<Object> {
	static final int MASK = 0xfff00000;
	public static final int MAXADR = 0xffefffff;
	private final List<Object> list = new ArrayList<Object>();
	private int adrs[] = null;

	/** adds the ref and returns its intref */
	int refToInt(Object ref) {
		int i = list.size();
		assert (i & MASK) == 0 : "too many IntRefs";
		list.add(ref);
		return MASK | i;
	}

	/** returns the ref for an intref (that was returned by refToInt) */
	Object intToRef(int intref) {
		// xor the mask bits instead of &
		// so if they weren't correct it'll be an invalid index
		return list.get(intref ^ MASK);
	}

	int size() {
		return list.size();
	}

	static boolean isIntRef(int n) {
		return (n & MASK) == MASK;
	}

	void update(int intref, Object ref) {
		list.set(intref ^ MASK, ref);
	}

	void startStore() {
		assert adrs == null;
		adrs = new int[list.size()];
	}

	/** record the adr that a ref has been persisted at */
	void setAdr(int intref, int adr) {
		assert ! isIntRef(adr);
		adrs[intref ^ MASK] = adr;
	}

	int getAdr(int intref) {
		return adrs == null ? intref : adrs[intref ^ MASK];
	}

	@Override
	public Iterator<Object> iterator() {
		return list.iterator();
	}

	void clear() {
		list.clear();
		adrs = null;
	}

	int next() {
		return MASK | list.size();
	}

}

