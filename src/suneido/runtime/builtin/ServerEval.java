/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.SuObject;
import suneido.Suneido;
import suneido.TheDbms;
import suneido.runtime.Args;
import suneido.runtime.Ops;

public class ServerEval {

	public static Object ServerEval(Object... args) {
		SuObject c = Args.collectArgs(new SuObject(), args);
		return TheDbms.dbms().exec(c);
	}

	public static Object exec(SuObject c) {
		Object[] args = new Object[] { Args.Special.EACH1, c };
		return SuThread.runWithMainSuneido(() -> {
			String fname = Ops.toStr(c.get(0));
			int i = fname.indexOf('.');
			if (i == -1)
				return Ops.call(Suneido.context.get(fname), args);
			else {
				String f = fname.substring(0, i);
				String m = fname.substring(i + 1);
				return Ops.invoke(Suneido.context.get(f), m, args);
			}
		});
	}

}
