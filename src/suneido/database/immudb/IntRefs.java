/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.util.*;

/**
 * Used to assign integer values to in-memory references
 * so they can be stored in data structures that normally
 * contain integer offsets in the database file.
 * <p>
 * Returned int's have the upper 12 bits set to 1
 * to distinguish them from database offsets from {@link IntLongs}
 */
public class IntRefs implements Iterable<Object> {
	public static final int MASK = 0xfff00000;
	private final List<Object> list = new ArrayList<Object>();
	private int adrs[] = null;

	/** adds the ref and returns its intref */
	public int refToInt(Object ref) {
		int i = list.size();
		assert (i & MASK) == 0 : "too many IntRefs";
		list.add(ref);
		return MASK | i;
	}

	/** returns the ref for an intref (that was returned by refToInt) */
	public Object intToRef(int intref) {
		// xor the mask bits instead of &
		// so if they weren't correct it'll be an invalid index
		return list.get(intref ^ MASK);
	}

	public int size() {
		return list.size();
	}

	public static boolean isIntRef(int n) {
		return (n & MASK) == MASK;
	}

	public void update(int intref, Object ref) {
		list.set(intref ^ MASK, ref);
	}

	public void startStore() {
		assert adrs == null;
		adrs = new int[list.size()];
	}

	/** record the adr that a ref has been persisted at */
	public void setAdr(int intref, int adr) {
		assert ! isIntRef(adr);
		adrs[intref ^ MASK] = adr;
	}

	public int getAdr(int intref) {
		return adrs == null ? intref : adrs[intref ^ MASK];
	}

	@Override
	public Iterator<Object> iterator() {
		return list.iterator();
	}

}

