/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.SuFunction0;

public class AssertionError extends SuFunction0 {

	@Override
	public Object call0() {
		assert false : "deliberate assertion error for testing";
		return null;
	}

}