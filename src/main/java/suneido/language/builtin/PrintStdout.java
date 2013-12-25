/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.util.Map;

import suneido.language.ArgsIterator;
import suneido.language.Ops;

public class PrintStdout {

	public static synchronized Object Print(Object... args) {
		ArgsIterator iter = new ArgsIterator(args);
		while (iter.hasNext()) {
			Object x = iter.next();
			if (x instanceof Map.Entry) {
				@SuppressWarnings("unchecked")
				Map.Entry<Object, Object> e = (Map.Entry<Object, Object>) x;
				print(e.getKey());
				System.out.print(": ");
				print(e.getValue());
			} else
				print(x);
			if (iter.hasNext())
				System.out.print(' ');
		}
		System.out.flush();
		return null;
	}

	private static void print(Object x) {
		System.out.print(Ops.isString(x) ? Ops.toStr(x) : Ops.display(x));
	}

}
