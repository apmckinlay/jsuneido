/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Params;

public class Locals {

	@Params("offset")
	public static Object Locals(Object a) {
		return suneido.SuContainer.EMPTY;
	}
}
