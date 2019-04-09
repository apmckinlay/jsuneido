/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

public class Tabs {

	/**
	 * Convert all tabs to spaces
	 */
	public static String detab(String s, int TABWIDTH) {
		int sn = s.length();
		StringBuilder buf = new StringBuilder(sn);
		int col = 0;
		for (int si = 0; si < sn; ++si) {
			char c = s.charAt(si);
			switch (c) {
			case '\t':
				do
					buf.append(' ');
				while (++col % TABWIDTH != 0);
				break;
			case '\n':
			case '\r':
				buf.append(c);
				col = 0;
				break;
			default:
				buf.append(c);
				++col;
				break;
			}
		}
		return buf.toString();
	}

	/**
	 * Convert <u>leading</u> spaces to tabs.
	 * Also strips trailing spaces and tabs.
	 */
	public static String entab(String s, int TABWIDTH) {
		StringBuilder sb = new StringBuilder(s.length());
		int si = 0;
		for (;;) { // for each line
			// convert leading spaces & tabs
			char c;
			int col = 0;
			while (0 != (c = sget(s, si++))) {
				if (c == ' ')
					++col;
				else if (c == '\t')
					for (++col; ! istab(col, TABWIDTH); ++col)
						;
				else
					break;
			}
			--si;
			int dstcol = 0;
			for (int j = 0; j <= col; ++j)
				if (istab(j, TABWIDTH)) {
					sb.append('\t');
					dstcol = j;
				}
			for (; dstcol < col; ++dstcol)
				sb.append(' ');

			// copy the rest of the line
			while (0 != (c = sget(s, si++)) && c != '\n' && c != '\r')
				sb.append(c);

			// strip trailing spaces & tabs

			for (int j = sb.length() - 1; j >= 0 && isTabOrSpace(sb.charAt(j)); --j)
				sb.deleteCharAt(j);
			if (c == 0)
				break;
			sb.append(c); // \n or \r
		}
		return sb.toString();
	}

	private static boolean istab(int col, int TABWIDTH) {
		return col > 0 && (col % TABWIDTH) == 0;
	}

	private static boolean isTabOrSpace(char c) {
		return c == ' ' || c == '\t';
	}

	private static char sget(String s, int i) {
		return i < s.length() ? s.charAt(i) : 0;
	}


}
