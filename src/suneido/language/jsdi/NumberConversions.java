package suneido.language.jsdi;

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
		else if (x instanceof CharSequence)
			return toLongFromString(x.toString());
		else if (Boolean.TRUE == x)
			return 1L;
		else if (Boolean.FALSE == x)
			return 0L;
		throw new JSDIException("can't convert " + Ops.typeName(x) + " to long");
	}

	// TODO: docs
	public static float toFloat(Object x) {
		if (x instanceof Number)
			return ((Number) x).floatValue();
		else if (x instanceof CharSequence)
			return toFloatFromString(x.toString());
		else if (Boolean.TRUE == x)
			return 1f;
		else if (Boolean.FALSE == x)
			return 0f;
		throw new JSDIException("can't convert " + Ops.typeName(x) + " to float");
	}

	// TODO: docs
	public static double toDouble(Object x) {
		if (x instanceof Number)
			return ((Number) x).doubleValue();
		else if (x instanceof CharSequence)
			return toDoubleFromString(x.toString());
		else if (Boolean.TRUE == x)
			return 1d;
		else if (Boolean.FALSE == x)
			return 0d;
		throw new JSDIException("can't convert " + Ops.typeName(x) + " to double");
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
				// The NOTE in 'toFloatFromString' regarding inifinity and NaN
				// values applies equally here.
			} catch (NumberFormatException e) {
				throw new JSDIException("can't convert string to double: " + s);
			}
		}
		return result;
	}
}
