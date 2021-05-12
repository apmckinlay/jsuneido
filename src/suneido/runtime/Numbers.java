/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import suneido.SuException;
import suneido.runtime.builtin.NumberMethods;
import suneido.util.Dnum;

/**
 * static helper methods for working with numbers.<p>
 * Used by {@link Ops} and {@link NumberMethods}
  */
public class Numbers {

	private Numbers() {
	} // all static, no instances

	/** Assumes argument is Integer or Dnum i.e. from toNum */
	public static Dnum toDnum(Object n) {
		if (n instanceof Dnum)
			return (Dnum) n;
		if (n instanceof Integer)
			return Dnum.from((int) n);
		Ops.likeZero(n, "number");
		return Dnum.Zero;
	}

	/**
	 * @return The value converted to a Number.
	 * "" and false are converted to 0.
	 * The original value is returned if instanceof Number.
	 */
	public static Number toNum(Object x) {
		if (x instanceof Number)
			return (Number) x;
		Ops.likeZero(x, "number");
		return 0;
	}

	/**
	 * Handles hex (0x...) in addition to integers and Dnum
	 * Used by AstCompile to convert numbers.
	 * @return The string converted to a Number (long or Dnum)
	 */
	public static Number stringToNumber(String s) {
		if (s.startsWith("0x"))
			try {
				return Integer.parseUnsignedInt(s.substring(2), 16);
			} catch (NumberFormatException e) {
				throw new SuException("can't convert to number: " + s);
			}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return Dnum.parse(s);
		}
	}

	public static long longValue(Object x) {
		if (x instanceof Integer)
			return (int) x;
		else if (x instanceof Dnum)
			return ((Dnum) x).longValue();
		throw new SuException("can't convert to integer");
	}

	/** @return The integer value of x if convertible, else Integer.MIN_VALUE */
	public static int intOrMin(Object x) {
		if (x instanceof Integer)
			return (int) x;
		else if (x instanceof Dnum)
			return ((Dnum) x).intOrMin();
		return Integer.MIN_VALUE;
	}

}
