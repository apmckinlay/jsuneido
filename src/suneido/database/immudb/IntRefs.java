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
 * <p>
 * NOTE: IntRefs is intended to be static "context"
 * it has to be set to an Impl by the using code,
 * i.e. at the start of a Transaction operation.
 */
public class IntRefs {
	private static final ThreadLocal<Impl> ir = new ThreadLocal<Impl>();
	private static final int MASK = 0xfff00000;

	public static void set(Impl impl) {
		ir.set(impl);
	}

	public static int refToInt(Object ref) {
		return ir.get().refToInt(ref);
	}

	public static Object intToRef(int index) {
		return ir.get().intToRef(index);
	}

	public static boolean isIntRef(int n) {
		return (n & MASK) == MASK;
	}

	public static int size() {
		return ir.get().size();
	}

	public static class Impl {
		private final List<Object> list = new ArrayList<Object>();

		public int refToInt(Object ref) {
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

	}

}
