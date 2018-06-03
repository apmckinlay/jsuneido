/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.runtime.Numbers.toDnum;
import static suneido.runtime.Numbers.toNum;

import java.math.RoundingMode;

import suneido.runtime.BuiltinMethods;
import suneido.runtime.Ops;
import suneido.runtime.Params;
import suneido.util.Dnum;

// assert self instanceof Number
// assert self instanceof Integer || self instanceof Dnum

/**
 * Methods for numbers.
 * <li>Only handles Integer and Dnum, not other numeric types.
 */
public class NumberMethods extends BuiltinMethods {
	public static final NumberMethods singleton = new NumberMethods();

	private NumberMethods() {
		super("number", NumberMethods.class, "Numbers");
	}

	public static Object ACos(Object self) {
		double d = ((Number) self).doubleValue();
		return Dnum.from(Math.acos(d));
	}

	public static Object ASin(Object self) {
		double d = ((Number) self).doubleValue();
		return Dnum.from(Math.asin(d));
	}

	public static Object ATan(Object self) {
		double d = ((Number) self).doubleValue();
		return Dnum.from(Math.atan(d));
	}

	public static Object Chr(Object self) {
		int n = ((Number) self).intValue();
		return Character.toString((char) (n & 0xff));
	}

	public static Object Cos(Object self) {
		double d = ((Number) self).doubleValue();
		return Dnum.from(Math.cos(d));
	}

	public static Object Exp(Object self) {
		double d = ((Number) self).doubleValue();
		return Dnum.from(Math.exp(d));
	}

	@Params("mask")
	public static Object Format(Object self, Object a) {
		Dnum n = toDnum(self);
		String mask = Ops.toStr(a);
		return format(mask, n);
	}

	static String format(String mask, Dnum n) {
		int masksize = mask.length();
		int before = 0;
		int after = 0;
		boolean intpart = true;
		for (int i = 0; i < masksize; ++i)
			switch (mask.charAt(i)) {
			case '.':
				intpart = false;
				break;
			case '#':
				if (intpart)
					++before;
				else
					++after;
				break;
			}
		if (n.exp() > before)
			return "#"; // too big to fit in mask

		n = n.round(after);
		char[] digits = new char[Dnum.MAX_DIGITS];
		int nd = n.digits(digits);

		int e = n.exp();
		if (nd == 0 && after == 0) {
			digits[0] = '0';
			e = nd = 1;
		}
		int di = e - before;
		assert di <= 0;
		StringBuilder dst = new StringBuilder(masksize);
		int sign = n.signum();
		boolean signok = (sign >= 0);
		boolean frac = false;
		for (int mi = 0; mi < masksize; ++mi) {
			char mc = mask.charAt(mi);
			switch (mc) {
			case '#':
				if (0 <= di && di < nd)
					dst.append(digits[di]);
				else if (frac || di >= 0)
					dst.append('0');
				++di;
				break;
			case ',':
				if (di > 0)
					dst.append(',');
				break;
			case '-':
			case '(':
				signok = true;
				if (sign < 0)
					dst.append(mc);
				break;
			case ')':
				dst.append(sign < 0 ? mc : ' ');
				break;
			case '.':
				frac = true;
			default:
				dst.append(mc);
				break;
			}
		}
		if (!signok)
			return "-"; // negative not handled by mask
		return dst.toString();
	}

	public static Object Hex(Object self) {
		if (self instanceof Integer)
			return Integer.toHexString((int) self);
		else // Dnum
			return Long.toHexString(((Number) self).longValue());
	}

	public static Object Int(Object self) {
		if (self instanceof Integer)
			return self;
		return ((Dnum) self).integer();
	}

	public static Object Frac(Object self) {
		if (self instanceof Integer)
			return 0;
		return ((Dnum) self).frac();
	}

	public static Object Log(Object self) {
		double d = ((Number) self).doubleValue();
		return Dnum.from(Math.log(d));
	}

	public static Object Log10(Object self) {
		double d = ((Number) self).doubleValue();
		return Dnum.from(Math.log10(d));
	}

	@Params("number")
	public static Object Pow(Object self, Object a) {
		Number sn = (Number) self;
		Number an = toNum(a);
		double n = Math.pow(sn.doubleValue(), an.doubleValue());
		return Dnum.from(n);
	}

	@Params("number")
	public static Object Round(Object self, Object d) {
		return round(self, d, RoundingMode.HALF_UP);
	}

	@Params("number")
	public static Object RoundDown(Object self, Object d) {
		return round(self, d, RoundingMode.DOWN);
	}

	@Params("number")
	public static Object RoundUp(Object self, Object d) {
		return round(self, d, RoundingMode.UP);
	}

	private static Object round(Object self, Object d, RoundingMode mode) {
		Dnum n = toDnum(self);
		int digits = Ops.toInt(d);
		return n.round(digits, mode);
	}

	public static Object Sin(Object self) {
		double d = ((Number) self).doubleValue();
		return Dnum.from(Math.sin(d));
	}

	public static Object Sqrt(Object self) {
		double d = ((Number) self).doubleValue();
		return Dnum.from(Math.sqrt(d));
	}

	public static Object Tan(Object self) {
		double d = ((Number) self).doubleValue();
		return Dnum.from(Math.tan(d));
	}

}

