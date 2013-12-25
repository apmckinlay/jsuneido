/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import javax.annotation.concurrent.Immutable;

import suneido.SuContainer;

@Immutable
public abstract class Range {

	public abstract CharSequence substr(CharSequence s);

	public abstract SuContainer sublist(SuContainer c);

	public static class RangeTo extends Range {
		public final int from;
		public final int to;

		public RangeTo(int from, int to) {
			this.from = from;
			this.to = to;
		}

		@Override
		public CharSequence substr(CharSequence s) {
			int size = s.length();
			int f = prepFrom(from, size);
			int t = prepTo(f, to, size);
			return s.subSequence(f, t);
		}

		@Override
        public SuContainer sublist(SuContainer c) {
			int size = c.size();
			int f = prepFrom(from, size);
			int t = prepTo(f, to, size);
			return c.subList(f, t);
        }
	}

	public static class RangeLen extends Range {
		public final int from;
		public final int len;

		public RangeLen(int from, int len) {
			this.from = from;
			this.len = len;
		}

		@Override
		public CharSequence substr(CharSequence s) {
			int size = s.length();
			int f = prepFrom(from, size);
			int n = prepLen(len, size - f);
			return s.subSequence(f, f + n);
		}

		@Override
        public SuContainer sublist(SuContainer c) {
			int size = c.size();
			int f = prepFrom(from, size);
			int n = prepLen(len, size - f);
			return c.subList(f, f + n);
        }
	}

	private static int prepFrom(int from, int len) {
		if (from < 0) {
			from += len;
			if (from < 0)
				from = 0;
		}
		if (from > len)
			from = len;
		return from;
	}

	private static int prepTo(int from, int to, int size) {
		if (to < 0)
			to += size;
		if (to < from)
			to = from;
		if (to > size)
			to = size;
		return to;
	}

	private static int prepLen(int len, int size) {
		if (len < 0)
			len = 0;
		if (len > size)
			len = size;
		return len;
	}

}
