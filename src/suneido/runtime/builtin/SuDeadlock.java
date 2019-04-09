/* Copyright 2017 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.Deadlock;

public class SuDeadlock {

	public static Object Deadlock() {
		Deadlock.cause();
		return null;
	}
}
