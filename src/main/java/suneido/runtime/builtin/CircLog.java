/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.util.Arrays;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class CircLog {
	private static final int QSIZE = 500;
	private static String [] queue = new String [QSIZE];
	private static int qi = 0;

	@Params("string = false")
	public static Object CircLog(Object s) {
		if (s == Boolean.FALSE) {
			StringBuilder sb = new StringBuilder();
			int i = qi;
			do {
				if (queue[i] != null && !queue[i].isEmpty())
					sb.append(queue[i]).append("\n");
				i = (i + 1) % QSIZE;
			} while (i != qi);
			return sb.toString();
		}
		String str = Ops.toStr(s).trim();
		if (str.isEmpty())
			return null;
		queue[qi] = str;
		qi = (qi + 1) % QSIZE;
		return null;
	}

	/** for use by tests */
	static void clear() {
		Arrays.fill(queue, 0, QSIZE, null);
		qi = 0;
	}

	/** for use by tests */
	static int index() {
		return qi;
	}
}
