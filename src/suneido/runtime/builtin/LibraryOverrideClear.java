/* Copyright 2018 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.Suneido;
import suneido.runtime.Params;

public class LibraryOverrideClear {

	@Params("")
	public static Object LibraryOverrideClear() {
		Suneido.context.overrideClear();
		return null;
	}

}
