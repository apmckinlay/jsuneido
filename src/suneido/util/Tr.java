package suneido.util;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class Tr {

	public static String tr(String src, String from, String to) {
		boolean allbut = (from.length() > 0 && from.charAt(0) == '^');
		if (allbut)
			from = from.substring(1);
		String fromset = makset(from);
		String toset = makset(to);

		int lastto = toset.length();
		boolean collapse = allbut || lastto < fromset.length();
		--lastto;

		int srclen = src.length();
		StringBuilder buf = new StringBuilder(srclen);
		for (int si = 0; si < srclen; ++si) {
			char c = src.charAt(si);
			int i = xindex(fromset, c, allbut, lastto);
			if (collapse && i >= lastto && lastto >= 0) {
				buf.append(toset.charAt(lastto));
				do {
					if (++si >= srclen)
						break;
					c = src.charAt(si);
					i = xindex(fromset, c, allbut, lastto);
				} while (i >= lastto);
			}
			if (si >= srclen)
				break;
			if (i >= 0 && lastto >= 0)
				buf.append(toset.charAt(i));
			else if (i < 0)
				buf.append(c);
			/* else
				delete */
		}
		return buf.toString();
		}

	private static String makset(String src) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < src.length(); ++i) {
			char c = src.charAt(i);
			if (c == '-' && i > 0 && i < src.length())
				for (char r = (char) (src.charAt(i - 1) + 1); r < src.charAt(i + 1); ++r)
					sb.append(r);
			else
				sb.append(c);
		}
		return sb.toString();
	}

	private static int xindex(String from, char c, boolean allbut, int lastto) {
		int p = from.indexOf(c);
		if (allbut)
			return p == -1 ? lastto + 1 : -1;
		else
			return p;
	}

}
