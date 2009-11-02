package suneido;

import java.util.regex.*;

import suneido.util.LruCache;

public class Regex {
	private static LruCache<String, Pattern> cache =
		new LruCache<String, Pattern>(32);

	public static boolean contains(String s, String rx) {
		return getPat(rx, s).matcher(s).find();
	}

	// cache get/put not synchronized, but idempotent
	public static Pattern getPat(String rx, String s) {
		if (s.indexOf('\n') != -1)
			rx = "(?m)" + rx;
		Pattern p = cache.get(rx);
		if (p == null)
			try {
				p = Pattern.compile(convertRegex(rx));
				cache.put(rx, p);
			} catch (PatternSyntaxException e) {
				throw new SuException("bad regular expression: " + rx + " => "
						+ convertRegex(rx));
			}
		return p;
	}

	/**
	 * Convert from Suneido's regular expression syntax to Java's.
	 */
	static String convertRegex(String rx) {
		if (rx.length() == 0)
			return rx;
		StringBuilder sb = new StringBuilder();
		char c = rx.charAt(0);
		if (c == '+' || c == '*' || c == '?')
			sb.append('\\');
		boolean inCharClass = false;
		for (int i = 0; i < rx.length();) {
			c = rx.charAt(i);
			switch (c) {
			case '[':
				if (inCharClass) {
					if (rx.startsWith("[:", i))
						i += posixClass(rx, i, sb);
					else {
						sb.append("\\[");
						++i;
					}
				} else {
					inCharClass = true;
					sb.append(c);
					c = rx.charAt(++i);
					if (c == '^') {
						sb.append(c);
						c = rx.charAt(++i);
					}
					if (c == ']') {
						sb.append("\\]");
						++i;
					}
				}
				break;
			case ']':
				if (inCharClass)
					inCharClass = false;
				sb.append(c);
				++i;
				break;
			case '(':
				if (rx.startsWith("(?q)", i)) {
					sb.append("\\Q");
					i += 4;
				} else if (rx.startsWith("(?-q)", i)) {
					sb.append("\\E");
					i += 5;
				} else {
					sb.append(c);
					++i;
				}
				break;
			case '\\':
				i += backslash(rx, i, sb);
				break;
			case '{':
			case '}':
				sb.append('\\');
				sb.append(c);
				++i;
				break;
			case '$':
				if (inCharClass)
					sb.append('\\');
				sb.append(c);
				++i;
				break;
			default:
				sb.append(c);
				++i;
				break;
			}
		}
		//System.out.println("convertRegex '" + rx + "' => '" + sb.toString() + "'");
		return sb.toString();
	}

	private static final String[][] posix = {
		{ "[:alnum:]", "\\p{Alnum}" },
		{ "[:alpha:]", "\\p{Alpha}" },
		{ "[:blank:]", "\\p{Blank}" },
		{ "[:cntrl:]", "\\p{Cntrl}" },
		{ "[:digit:]", "\\p{Digit}" },
		{ "[:graph:]", "\\p{Graph}" },
		{ "[:lower:]", "\\p{Lower}" },
		{ "[:print:]", "\\p{Print}" },
		{ "[:punct:]", "\\p{Punct}" },
		{ "[:space:]", "\\p{Space}" },
		{ "[:upper:]", "\\p{Upper}" },
		{ "[:xdigit:]", "\\p{XDigit}" }
		};
	private static int posixClass(String rx, int i, StringBuilder sb) {
		for (String[] p : posix) {
			if (rx.startsWith(p[0], i)) {
				sb.append(p[1]);
				return p[0].length();
			}
		}
		throw new SuException("Regex: bad posix character class");
	}

	private static int backslash(String rx, int i, StringBuilder sb) {
		if (i + 1 >= rx.length()) {
			sb.append("\\\\");
			return 1;
		}
		switch (rx.charAt(i + 1)) {
		case '<': case '>':
			sb.append("\\b");
			return 2;
		case '\\':
		case '.':
		case 'd': case 'D':
		case 's': case 'S':
		case 'w': case 'W':
		case 't': case 'n': case 'r':
		case 'A':
		case '(':
		case ')':
		case '[':
		case ']':
		case '*':
		case '+':
		case '?':
		case '|':
		case '1': case '2': case '3': case '4':
		case '5': case '6': case '7': case '8': case '9':
			sb.append(rx.substring(i, i + 2));
			return 2;
		case 'Z':
			sb.append("\\z");
			return 2;
		default:
			sb.append("\\\\");
			return 1;
		}
	}

	public static void appendReplacement(Matcher m, StringBuilder sb, String rep) {
		if (get(rep, 0) == '\\' && get(rep, 1) == '=') {
			sb.append(rep.substring(2));
			return ;
		}
		char tr = 'E';
		for (int ri = 0; ri < rep.length(); ++ri) {
			char rc = rep.charAt(ri);
			if (rc == '&')
				insert(sb, m.group(), tr);
			else if ('\\' == rc && ri + 1 < rep.length()) {
				rc = get(rep, ++ri);
				if (Character.isDigit(rc))
					insert(sb, m.group(rc - '0'), tr);
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
			throw SuException.unreachable();
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
			throw SuException.unreachable();
		}
	}

}
