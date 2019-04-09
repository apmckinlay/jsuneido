/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;

/**
 * Built-in implementation of the {@code Name(value)} function.
 *
 * @author Victor Schappert
 * @since 20130815
 * @see Type
 */
public final class Name {

	//
	// Built-in functions
	//

	@Params("value")
	public static String Name(Object a) {
		return Ops.valueName(a);
	}

}
