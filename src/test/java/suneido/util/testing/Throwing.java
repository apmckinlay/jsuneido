/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util.testing;

import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods to make testing for exceptions within JUnit easier in some
 * circumstances.
 *
 * @author Victor Schappert
 * @since 20130721
 */
public final class Throwing {

	//
	// STATICS
	//

	/**
	 * Shortcut equivalent to "<code>assertTrue(threw(r))</code>".
	 * @param r Code to run
	 * @see #threw(Runnable)
	 * @see #assertThrew(Runnable, Class)
	 * @see #assertThrew(Runnable, Class, String)
	 */
	public static void assertThrew(Runnable r) {
		assertTrue(threw(r));
	}

	/**
	 * Shortcut equivalent to "<code>assertTrue(threw(r, clazz))</code>".
	 * @param r Code to run
	 * @param clazz Expected class of the exception. See also the JUnit
	 * {@code @Test(expected=...)} annotation.
	 * @see #threw(Runnable, Class)
	 * @see #assertThrew(Runnable)
	 * @see #assertThrew(Runnable, Class, String)
	 */
	public static void assertThrew(Runnable r, Class<? extends Throwable> clazz) {
		assertTrue(threw(r, clazz));
	}

	/**
	 * Shortcut equivalent to "<code>assertTrue(threw(r, clazz, pattern))</code>".
	 * @param r Code to run
	 * @param clazz Expected class of the exception. See also the JUnit
	 * {@code @Test(expected=...)} annotation.
	 * @param pattern Regular expression which the caught exception must match
	 * in order for the assertion to succeed
	 * @see #threw(Runnable, Class)
	 * @see #assertThrew(Runnable)
	 * @see #assertThrew(Runnable, Class, String)
	 */
	public static void assertThrew(Runnable r, Class<? extends Throwable> clazz, String pattern) {
		assertTrue(threw(r, clazz, pattern));
	}

	/**
	 * <p>
	 * Returns true iff running a {@link Runnable} threw an exception.
	 * </p>
	 * <p>
	 * USAGE:
	 * <pre>static import ...Throwing.*;
	 *assertTrue(threw(new Runnable() { &lt;code...&gt; }));</pre>
	 * </p>
	 * @param r Code to run
	 * @return Whether {@code r} threw
	 * @see #threw(Runnable, Class)
	 * @see #threw(Runnable, Class, String)
	 */
	public static boolean threw(Runnable r) {
		try {
			r.run();
			return false;
		} catch (Throwable t) {
			return true;
		}
	}

	/**
	 * <p>
	 * Returns true iff running a {@link Runnable} threw an exception of a
	 * particular class.
	 * </p>
	 * <p>
	 * USAGE:
	 * <pre>static import ...Throwing.*;
	 *assertTrue(threw(new Runnable() { &lt;code...&gt; }, RuntimeException.class));</pre>
	 * </p>
	 * @param r Code to run
	 * @param clazz Expected class of the exception. See also the JUnit
	 * {@code @Test(expected=...)} annotation.
	 * @return Whether {@code r} threw an exception of class {@code clazz}
	 * @see #assertThrew(Runnable, Class)
	 * @see #threw(Runnable)
	 * @see #threw(Runnable, Class, String)
	 */
	public static boolean threw(Runnable r, Class<? extends Throwable> clazz) {
		try {
			r.run();
		} catch (Throwable t) {
			if (clazz == t.getClass()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * <p>
	 * Returns true iff running a {@link Runnable} threw an exception of a
	 * particular class whose message matches a given regular expression.
	 * </p>
	 * <p>
	 * USAGE:
	 * <pre>static import ...Throwing.*;
	 *assertTrue(threw(new Runnable() { &lt;code...&gt; }, RuntimeException.class, "horrible problem: .*"));</pre>
	 * </p>
	 * @param r Code to run
	 * @param clazz Expected class of the exception. See also the JUnit
	 * {@code @Test(expected=...)} annotation.
	 * @param pattern Regular expression which the caught exception must match
	 * in order for the assertion to succeed
	 * @return Whether {@code r} threw an exception of class {@code clazz} with
	 * a message matching the regular expression {@code pattern}
	 */
	public static boolean threw(Runnable r, Class<? extends Throwable> clazz,
			String pattern) {
		try {
			r.run();
		} catch (Exception e) {
			Matcher matcher = Pattern.compile(pattern).matcher(e.getMessage());
			if (clazz == e.getClass() && matcher.find()) {
				return true;
			}
		}
		return false;
	}
}
