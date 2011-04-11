/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import javax.annotation.concurrent.Immutable;

@Immutable
public abstract class Range {

	public abstract String substr(String s);

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
			int f = from;
			if (f > slen)
				return "";
			if (f < 0) {
				f += slen;
				if (f < 0)
					f = 0;
			}
			int n = len;
			if (n < 0)
				n = 0;
			else if (n > slen - f)
				n = slen - f;
			return s.substring(f, f + n);
		}
	}

}
