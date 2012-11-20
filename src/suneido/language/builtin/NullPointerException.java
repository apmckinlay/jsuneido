/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

public class NullPointerException {
	public static Object x;

	public static Object NullPointerException() {
		return x.toString();
	}

}
