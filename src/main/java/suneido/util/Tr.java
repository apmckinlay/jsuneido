/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@ThreadSafe
public class Tr {
	private static LoadingCache<String,String> setCache =
			CacheBuilder.newBuilder()
				.maximumSize(100)
				.expireAfterAccess(1, TimeUnit.SECONDS)
				.build(new CacheLoader<String,String>() {
					@Override
					public String load(String set) {
						return expandRanges(set);
					}});

	public static String tr(String src, String from, String to) {
		int srclen = src.length();
		if (srclen == 0 | from.length() == 0)
			return src;

		boolean allbut = from.startsWith("^");
		if (allbut)
			from = from.substring(1);
		String fromset = makset(from);

		int si = 0;
		for (; si < srclen; ++si) {
			char c = src.charAt(si);
			int p = fromset.indexOf(c);
			if (allbut == (p == -1))
				break;
		}
		if (si == srclen)
			return src; // no changes
		StringBuilder buf = new StringBuilder(srclen);
		buf.append(src.substring(0, si));

		String toset = makset(to);
		int lastto = toset.length();
		boolean collapse = lastto > 0 && (allbut || lastto < fromset.length());
		--lastto;

		scan: for (; si < srclen; ++si) {
			char c = src.charAt(si);
			int i = xindex(fromset, c, allbut, lastto);
			if (collapse && i >= lastto) {
				buf.append(toset.charAt(lastto));
				do {
					if (++si >= srclen)
						break scan;
					c = src.charAt(si);
					i = xindex(fromset, c, allbut, lastto);
				} while (i >= lastto);
			}
			if (i < 0)
				buf.append(c);
			else if (lastto >= 0)
				buf.append(toset.charAt(i));
			/* else
				delete */
		}
		return buf.toString();
	}

	private static String makset(String s) {
		int dash = s.indexOf('-', 1);
		if (dash == -1 || dash == s.length() - 1)
			return s; // no ranges to expand
		return setCache.getUnchecked(s);
	}

	private static String expandRanges(String s) {
		int slen = s.length();
		StringBuilder sb = new StringBuilder(slen);
		for (int i = 0; i < slen; ++i) {
			char c = s.charAt(i);
			if (c == '-' && i > 0 && i + 1 < slen)
				for (char r = (char) (s.charAt(i - 1) + 1); r < s.charAt(i + 1); ++r)
					sb.append(r);
			else
				sb.append(c);
		}
		return sb.toString();
	}

	private static int xindex(String fromset, char c, boolean allbut, int lastto) {
		int i = fromset.indexOf(c);
		if (allbut)
			return i == -1 ? lastto + 1 : -1;
		else
			return i;
	}

}
