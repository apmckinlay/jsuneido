/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.Suneido;

public class Cmdline {

	public static String Cmdline() {
		return Suneido.cmdlineoptions.remainder;
	}

}