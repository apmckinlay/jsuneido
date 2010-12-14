/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.util.Map;

import suneido.SuContainer;
import suneido.language.*;

public class Print extends SuFunction {

	@Override
	public Object call(Object... args) {
		SuContainer c = Args.collectArgs(new SuContainer(), args);
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (; i < c.vecSize(); ++i)
			sb.append(i > 0 ? " " : "").append(Ops.toStr(c.get(i)));
		for (Map.Entry<Object, Object> e : c.mapEntrySet())
			sb.append(i++ > 0 ? " " : "").append(e.getKey()).append(": ")
					.append(Ops.toStr(e.getValue()));
		System.out.println(sb.toString());
		System.out.flush();
		return null;
	}

}
