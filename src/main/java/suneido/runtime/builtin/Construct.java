/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.util.Util.array;
import suneido.SuContainer;
import suneido.SuException;
import suneido.Suneido;
import suneido.runtime.Args;
import suneido.runtime.Ops;
import suneido.runtime.Params;

public class Construct {

	private static final Object[] noArgs = new Object[0];

	@Params("what, suffix = ''")
	public static Object Construct(Object what, Object s) {
		String suffix = Ops.toStr(s);
		Object[] newargs = noArgs;
		if (what instanceof SuContainer) {
			SuContainer c = Ops.toContainer(what);
			what = c.get(0);
			if (what == null)
				throw new SuException("Construct: object requires member 0");
			newargs = array(Args.Special.EACH1, c);
		}
		if (Ops.isString(what)) {
			String className = what.toString();
			if (!className.endsWith(suffix))
				className += suffix;
			what = Suneido.context.get(className);
		}
		return Ops.invoke(what, "<new>", newargs);
	}

}
