/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import javax.annotation.concurrent.Immutable;

import suneido.SuContainer;

@Immutable
public abstract class Range {

	public abstract String substr(String s);

	public abstract SuContainer sublist(SuContainer c);

	public static class RangeTo extends Range {
		public final int from;
		public final int to;

		public RangeTo(int from, int to) {
			this.from = from;
			this.to = to;
		}

		@Override
		public String substr(String s) {
			int slen = s.length();
			int f = from;
			if (f < 0)
				f += slen;
			if (f < 0)
				f = 0;
			int t = to;
			if (t < 0)
				t += slen;
			if (f >= t || f > slen)
				return "";
			if (t > slen)
				t = slen;
			return s.substring(f, t);
		}

		@Override
                public SuContainer sublist(SuContainer c) {
	                // TODO Auto-generated method stub
	                return null;
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
		public String substr(String s) {
			int slen = s.length();
			int f = prepFrom(from, slen);
			if (f > slen)
				return "";
			int n = len;
			if (n < 0)
				n = 0;
			else if (n > slen - f)
				n = slen - f;
			return s.substring(f, f + n);
		}

		@Override
                public SuContainer sublist(SuContainer c) {
	                // TODO Auto-generated method stub
	                return null;
                }
	}

	private static int prepFrom(int from, int len) {
		if (from < 0) {
			from += len;
			if (from < 0)
				from = 0;
		}
		return from;
	}

	private static int prepTo(int to, int len) {
		if (to < 0)
			to += len;
		if (to > len)
			to = len;
		return to;
	}

}
