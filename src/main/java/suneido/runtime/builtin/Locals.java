/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.SuContainer;
import suneido.runtime.Params;

public class Locals {
	
	@Params("offset")
	public static SuContainer Locals(Object a) {
		return new SuContainer(0); // TODO implement
	}

}
