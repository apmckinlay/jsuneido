/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.SuContainer;
import suneido.TheDbms;
import suneido.language.Ops;
import suneido.language.Params;

public class DoWithoutTriggers {

	@Params("tables, block")
	public static Object DoWithoutTriggers(Object tables, Object block) {
		SuContainer c = Ops.toContainer(tables);
		try {
			for (Object x : c.vec)
				TheDbms.dbms().disableTrigger(Ops.toStr(x));
			return Ops.call(block);
		} finally {
			for (Object x : c.vec)
				TheDbms.dbms().enableTrigger(Ops.toStr(x));
		}
	}

}
