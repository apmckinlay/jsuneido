/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.SuContainer;
import suneido.language.Params;

public class Locals {
	
	@Params("offset")
	public static SuContainer Locals(Object a) {
		return new SuContainer(0); // TODO implement
	}

}
