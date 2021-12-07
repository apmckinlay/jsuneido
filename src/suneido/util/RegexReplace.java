/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import com.google.common.base.Ascii;

import suneido.util.Regex.Result;

public class RegexReplace {

	public static void append(String s, Result res, String rep, StringBuilder sb) {
		if (get(rep, 0) == '\\' && get(rep, 1) == '=') {
			sb.append(rep.substring(2));
			return ;
		}
		char tr = 'E';
		for (int ri = 0; ri < rep.length(); ++ri) {
			char rc = rep.charAt(ri);
			if (rc == '&')
				insert(sb, res.group(s, 0), tr);
			else if ('\\' == rc && ri + 1 < rep.length()) {
				rc = get(rep, ++ri);
				if (Character.isDigit(rc))
					insert(sb, res.group(s, rc - '0'), tr);
				else if (rc == 'n')
					sb.append('\n');
				else if (rc == 't')
					sb.append('\t');
				else if (rc == '\\')
					sb.append('\\');
				else if (rc == '&')
					sb.append('&');
				else if (rc == 'u' || rc == 'l' || rc == 'U' || rc == 'L'
						|| rc == 'E')
					tr = rc;
				else
					sb.append(rc);
			} else {
				sb.append(trcase(tr, rc));
				if (tr == 'u' || tr == 'l')
					tr = 'E';
			}
		}

	}

	private static char get(String s, int i) {
		return i < s.length() ? s.charAt(i) : 0;
	}

	private static char trcase(char tr, char rc) {
		switch (tr) {
		case 'E':
			return rc;
		case 'l':
		case 'L':
			return Ascii.toLowerCase(rc);
		case 'u':
		case 'U':
			return Ascii.toUpperCase(rc);
		default:
			throw new RuntimeException("bad trcase");
		}
	}

	private static void insert(StringBuilder sb, String group, char tr) {
		if (group == null)
			return;
		switch (tr) {
		case 'E':
			sb.append(group);
			break;
		case 'l':
			sb.append(Ascii.toLowerCase(group.charAt(0)));
			sb.append(group.substring(1));
			break;
		case 'L':
			sb.append(Ascii.toLowerCase(group));
			break;
		case 'u':
			sb.append(Ascii.toUpperCase(group.charAt(0)));
			sb.append(group.substring(1));
			break;
		case 'U':
			sb.append(Ascii.toUpperCase(group));
			break;
		default:
			assert false;
		}
	}

}
