/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.Ops;
import suneido.language.Params;

public class NoMatch {

	@Params("string, pattern")
	public static Boolean NoMatch(Object a, Object b) {
		return Ops.matchnot(a, b);
	}

}
