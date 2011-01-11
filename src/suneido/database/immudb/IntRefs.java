/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to assign integer values to in-memory references
 * so they can be stored in data structures that normally
 * contain integer offsets in the database file.
 * <p>
 * Returned int's have the upper 12 bits set to 1
 * to distinguish them from database offsets from {@link IntLongs}
 */
public class IntRefs {
	private static final int MASK = 0xfff00000;
	private final List<Object> list = new ArrayList<Object>();
	private int adrs[] = null;

	public int refToInt(Object ref) {
		assert adrs == null;
		int i = list.size();
		assert (i & MASK) == 0 : "too many IntRefs";
		list.add(ref);
		return MASK | i;
	}

	public Object intToRef(int index) {
		// xor the mask bits instead of &
		// so if they weren't correct it'll be an invalid index
		return list.get(index ^ MASK);
	}

	public int size() {
		return list.size();
	}

	public static boolean isIntRef(int n) {
		return (n & MASK) == MASK;
	}

	public void update(int index, Object ref) {
		list.set(index ^ MASK, ref);
	}

	public void startPersist() {
		assert adrs == null;
		adrs = new int[list.size()];
	}

	/** record the adr that a ref has been persisted at */
	public void setAdr(int index, int adr) {
		adrs[index ^ MASK] = adr;
	}

	public int getAdr(int index) {
		return adrs[index ^ MASK];
	}

}

