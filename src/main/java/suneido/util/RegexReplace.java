/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

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
					tr = Character.toUpperCase(rc);
				else
					sb.append(rc);
			} else
				sb.append(trcase(tr, rc));
		}

	}

	private static char get(String s, int i) {
		return i < s.length() ? s.charAt(i) : 0;
	}

	private static char trcase(char tr, char rc) {
		switch (tr) {
		case 'E':
			return rc;
		case 'L':
			return Character.toLowerCase(rc);
		case 'U':
			return Character.toUpperCase(rc);
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
		case 'L':
			sb.append(group.toLowerCase());
			break;
		case 'U':
			sb.append(group.toUpperCase());
			break;
		default:
			assert false;
		}
	}

}
