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
 * NOTE: IntRefs is intended to be "context"
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
			list.add(ref);
			assert (list.size() & MASK) == 0;
			return MASK | list.size();
		}

		public Object intToRef(int index) {
			return list.get((index ^ MASK) - 1);
		}

		public int size() {
			return list.size();
		}

	}

}
