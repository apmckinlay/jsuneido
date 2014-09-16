/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

public class AssertionError {

	public static Object AssertionError() {
		assert false : "deliberate assertion error for testing";
		return null;
	}

}