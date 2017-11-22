/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

public class Range {

	public static int prepFrom(int from, int len) {
		if (from < 0) {
			from += len;
			if (from < 0)
				from = 0;
		}
		if (from > len)
			from = len;
		return from;
	}

	public static int prepTo(int from, int to, int size) {
		if (to < 0)
			to += size;
		if (to < from)
			to = from;
		if (to > size)
			to = size;
		return to;
	}

	public static int prepLen(int len, int size) {
		if (len < 0)
			len = 0;
		if (len > size)
			len = size;
		return len;
	}

}
