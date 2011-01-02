/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

/**
 * Used to compress long offsets into int's.
 * Offsets must be aligned.
 * To convert to int they are shifted right.
 * With ALIGN = 8, max offset is 32gb.
 * NOTE: int's may be negative
 */
public class IntLongs {
	private static final int SHIFT = 3;
	public static final int ALIGN = (1 << SHIFT);
	private static final int MASK = ALIGN - 1;
	public static final long MAX = 0xffffffffL << SHIFT;

	public static int longToInt(long n) {
		assert (n & MASK) == 0;
		assert n <= MAX;
		return (int) (n >>> SHIFT);
	}

	public static long intToLong(int n) {
		return (n & 0xffffffffL) << SHIFT;
	}

}
