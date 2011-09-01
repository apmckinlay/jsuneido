/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

public class Verify {

	public static void verify(boolean arg) {
		if (! arg)
			throw new RuntimeException("verify failed");
	}

	public static void verify(boolean arg, String msg) {
		if (! arg)
			throw new RuntimeException("verify failed: " + msg);
	}

}
