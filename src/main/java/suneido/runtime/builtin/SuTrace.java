/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.Trace.Type.CONSOLE;
import static suneido.Trace.Type.LOGFILE;
import suneido.Trace;
import suneido.runtime.Ops;
import suneido.runtime.Params;

public class SuTrace {

	@Params("flags, block = false")
	public static Object Trace(Object a, Object b) {
		if (Ops.isString(a))
			Trace.println(Ops.toStr(a));
		else {
			int flags = Ops.toInt(a);
			if (0 == (flags & (CONSOLE.bit | LOGFILE.bit)))
				flags |= CONSOLE.bit | LOGFILE.bit;
			int original_flags = Trace.flags;
			Trace.flags = flags;
			if (b != Boolean.FALSE)
				try {
					return Ops.call(b);
				} finally {
					Trace.flags = original_flags;
				}
		}
		return null;
	}

}
