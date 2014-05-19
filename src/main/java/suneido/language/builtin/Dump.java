/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static suneido.language.Ops.toStr;
import suneido.TheDbms;
import suneido.language.Params;

public class Dump {

	@Params("table = false")
	public static Object Dump(Object table) {
		if (table == Boolean.FALSE)
			TheDbms.dbms().dump("");
		else
			TheDbms.dbms().dump(toStr(table));
		return null;
	}

}
