/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

public class Verify {

	public static void verify(boolean arg, String msg) {
		if (! arg) {
			RuntimeException e = new RuntimeException("verify failed " + msg);
e.printStackTrace();
			throw e;
		}
	}

	public static void verify(boolean arg) {
		verify(arg, "");
	}

	public static void verifyEquals(long expected, long actual) {
		verify(expected == actual,
				"expected " + expected + " got: " + actual);
	}

	public static void verifyEquals(Object expected, Object actual) {
		verify(expected.equals(actual),
				"expected " + expected + " got: " + actual);
	}

}
