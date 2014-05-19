/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi;

import java.math.BigDecimal;

import suneido.language.Numbers;
import suneido.language.Ops;

/**
 * <p>
 * Supplementary number conversions not provided by {@link Ops}
 * and {@link suneido.language.Numbers}.
 * </p>
 * <p>
 * <strong>NOTE</strong>: The methods in this class make no attempt to respect
 * the precision limitations of Suneido numbers. This is partly out of
 * expediency (to get JSDI working as rapidly as possible) and partly out of
 * necessity (the jSuneido number system is a bit confused as of 20130716).
 * </p>
 * @author Victor Schappert
 * @since 20130716
 */
@DllInterface
public final class NumberConversions {

	//
	// PUBLIC INTERFACE
	//

	// TODO: docs
	public static long toLong(Object x) {
		if (x instanceof Number)
			return ((Number) x).longValue();
		else if (Boolean.FALSE == x)
			return 0L;
		else if (x instanceof CharSequence)
			return toLongFromString(x.toString());
		else if (Boolean.TRUE == x)
			return 1L;
		throw new JSDIException("can't convert " + Ops.typeName(x) + " to long");
	}

	// TODO: docs
	public static float toFloat(Object x) {
		if (x instanceof Number)
			return ((Number) x).floatValue();
		else if (Boolean.FALSE == x)
			return 0f;
		else if (x instanceof CharSequence)
			return toFloatFromString(x.toString());
		else if (Boolean.TRUE == x)
			return 1f;
		throw new JSDIException("can't convert " + Ops.typeName(x) + " to float");
	}

	// TODO: docs
	public static double toDouble(Object x) {
		if (x instanceof Number)
			return ((Number) x).doubleValue();
		else if (Boolean.FALSE == x)
			return 0d;
		else if (x instanceof CharSequence)
			return toDoubleFromString(x.toString());
		else if (Boolean.TRUE == x)
			return 1d;
		throw new JSDIException("can't convert " + Ops.typeName(x) + " to double");
	}

	/**
	 * <p>
	 * Converts an integer value which representing a 32-bit pointer into a Java
	 * {@code int} capable of being passed to native code.
	 * </p>
	 * <p>
	 * In particular, this function converts the following values:
	 * <ul>
	 * <li>{@link Boolean#FALSE} {@code =>} 0</li>
	 * <li>Any {@link Number} {@code x} that is an integer and whose value falls
	 * within the range [{@link Integer#MIN_VALUE}, {@link Integer#MAX_VALUE}]
	 * {@code => x}</li>
	 * <li>Any string-type value {@code y} which can be parsed into a number
	 * {@code x} meeting the above criteria {@code => x}
	 * <li>Anything else {@code =>} {@link JSDIException}</li> 
	 * </ul>
	 * The result of the conversion is an {@code int} in the range
	 * 0x00000000-0xffffffff. Although Java treats this number as a signed
	 * integer, native code will treat it as the bitwise-equivalent unsigned
	 * 32-bit pointer.
	 * </p>
	 * <p>
	 * TODO: This function is deprecated and needs to be removed. The current
	 * barriers to removal (20131013) are that it is still being used for
	 * callback thunk pointers, and it is still being used for the resource
	 * type. Once these are cleaned up, it can be removed.
	 * </p>
	 * @param a Value to convert
	 * @return Value that is bitwise equivalent to the pointer represented by
	 * {@code a}
	 * @throws JSDIException If {@code a} cannot be converted to a 32-bit
	 * pointer
	 * @since 20130912
	 */
	@Deprecated
	public static int toPointer32(Object a) {
		// TODO: This should be completely removed in favour of toPointer64.
		//       The various places in which we assume 32-bit pointers
		//       (callback thunks, resources) should be changed to use 64-bit
		//       integers. Otherwise converting to 64-bit will be even more
		//       difficult.
		if (a instanceof Number) {
			if (a instanceof Integer) {
				return (int)a;
			} else if (a instanceof Long) {
				return toPointer32FromLong((Long)a);
			} else {
				return toPointer32FromNumber((Number)a);
			}
		} else if (Boolean.FALSE == a) {
			return 0;
		} else if (a instanceof CharSequence) {
			return toPointer32FromLong(toLongFromString(a.toString()));
		} else {
			throw new JSDIException("can't convert " + Ops.typeName(a)
					+ " into number suitable for pointer");
		}
	}

	/**
	 * <p>
	 * Converts an integer value representing a 64-bit pointer into a Java
	 * {@code long} capable of being passed to native code.
	 * </p>
	 * <p>
	 * In particular, this function converts the following values:
	 * <ul>
	 * <li>{@link Boolean#FALSE} {@code =>} 0</li>
	 * <li>Any {@link Number} {@code x} that is an integer and whose value falls
	 * within the range [{@link Long#MIN_VALUE}, {@link Long#MAX_VALUE}]
	 * {@code => x}</li>
	 * <li>Any string-type value {@code y} which can be parsed into a number
	 * {@code x} meeting the above criteria {@code => x}
	 * <li>Anything else {@code =>} {@link JSDIException}</li> 
	 * </ul>
	 * The result of the conversion is an {@code long} in the range
	 * 0x0000000000000000L-0xffffffffffffffffL. Although Java treats this number
	 * as a signed integer, native code will treat it as the bitwise-equivalent
	 * unsigned 64-bit pointer.
	 * </p>
	 * @param a Value to convert
	 * @return Value that is bitwise equivalent to the pointer represented by
	 * {@code a}
	 * @throws JSDIException If {@code a} cannot be converted to a 64-bit
	 * pointer
	 * @see #nullPointer64()
	 * @since 20131013
	 */
	public static long toPointer64(Object a) {
		if (a instanceof Number) {
			if (a instanceof Long) {
				return (long)a;
			} else if (a instanceof Integer) {
				return (long)(int)a;
			} else {
				return toPointer64FromNumber((Number)a);
			}
		} else if (Boolean.FALSE == a) {
			return 0L;
		} else if (a instanceof CharSequence) {
			return toLongFromString(a.toString());
		} else {
			throw cantConvertToPointer(a);
		}
	}

	/**
	 * Returns an integer which, when passed to native code, represents a NULL
	 * 64-bit pointer.
	 * @return Null pointer value
	 * @since 20131104
	 * @see #toPointer64(Object)
	 */
	public static long nullPointer64() {
		return 0L;
	}

	//
	// INTERNALS
	//

	private static long toLongFromString(String s) {
		if (s.equals(""))
			return 0L;
		String t = s;
		int radix = 10;
		if (s.startsWith("0x") || s.startsWith("0X")) {
			radix = 16;
			t = s.substring(2);
		} else if (s.startsWith("0"))
			radix = 8;
		try {
			return Long.parseLong(t, radix);
		} catch (NumberFormatException e) {
			throw new JSDIException("can't convert string to long: " + s);
		}
	}

	private static float toFloatFromString(String s) {
		float result = 0f;
		if (! s.equals("")) {
			try {
				result = Float.parseFloat(s);
				// NOTE: This can return Float.POSITIVE_INFINITY or
				//       Float.NEGATIVE_INFINITY if the absolute value is too
				//       large to represent in a float; or Float.NaN if the
				//       string is one of the strings which Float.parseFloat
				//       converts to NaN (i.e. "NaN"). In the current
				//       implementation, that is the user's problem.
			} catch (NumberFormatException e) {
				throw new JSDIException("can't convert string to float: " + s);
			}
		}
		return result;
	}

	private static double toDoubleFromString(String s) {
		double result = 0d;
		if (! s.equals("")) {
			try {
				result = Double.parseDouble(s);
				// The NOTE in 'toFloatFromString' regarding infinity and NaN
				// values applies equally here.
			} catch (NumberFormatException e) {
				throw new JSDIException("can't convert string to double: " + s);
			}
		}
		return result;
	}

	@Deprecated
	private static int toPointer32FromLong(long x) {
		if ((long)Integer.MIN_VALUE <= x && x <= (long)Integer.MAX_VALUE) {
			return (int)x;
		} else {
			throw cantConvertToPointer(x);
		}
	}

	@Deprecated
	private static int toPointer32FromNumber(Number x) {
		BigDecimal d = Numbers.toBigDecimal(x);
		if (Numbers.integral(x)
				&& Numbers.isInRange(d, Numbers.BD_INT_MIN, Numbers.BD_INT_MAX)) {
			return d.intValue();
		} else {
			throw cantConvertToPointer(x);
		}
	}

	private static long toPointer64FromNumber(Number x) {
		BigDecimal d = Numbers.toBigDecimal(x);
		if (Numbers.integral(x)
				&& Numbers.isInRange(d,  Numbers.BD_LONG_MIN, Numbers.BD_LONG_MAX)) {
			return d.longValue();
		} else {
			throw cantConvertToPointer(x);
		}
	}

	private static JSDIException cantConvertToPointer(Object x) {
		return new JSDIException(String.format(
				"can't convert %s %s into number suitable for pointer",
				Ops.typeName(x), x.toString()));
	}
}
