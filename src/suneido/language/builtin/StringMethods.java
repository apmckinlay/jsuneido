package suneido.language.builtin;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.text.CharacterIterator.DONE;
import static suneido.language.Ops.toInt;
import static suneido.language.Ops.toStr;
import static suneido.language.UserDefined.userDefined;
import static suneido.util.Util.array;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.regex.*;

import suneido.*;
import suneido.language.*;
import suneido.language.Compiler;
import suneido.util.Tr;

public class StringMethods {
	public static Object invoke(String s, String method, Object... args) {
		switch (method.charAt(0)) {
		case 'A':
			if (method == "Asc")
				return Asc(s, args);
			if (method == "Alpha?")
				return AlphaQ(s, args);
			if (method == "AlphaNum?")
				return AlphaNumQ(s, args);
			break;
		case 'C':
			if (method == "Compile")
				return Compile(s, args);
			break;
		case 'D':
			if (method == "Detab")
				return Detab(s, args);
			break;
		case 'E':
			if (method == "EndsWith")
				return EndsWith(s, args);
			if (method == "Entab")
				return Entab(s, args);
			if (method == "Eval")
				return Eval(s, args);
			if (method == "Eval2")
				return Eval2(s, args);
			if (method == "Extract")
				return Extract(s, args);
			break;
		case 'F':
			if (method == "Find")
				return Find(s, args);
			if (method == "Find1of")
				return Find1of(s, args);
			if (method == "Findnot1of")
				return Findnot1of(s, args);
			if (method == "FindLast")
				return FindLast(s, args);
			if (method == "FindLast1of")
				return FindLast1of(s, args);
			if (method == "FindLastnot1of")
				return FindLastnot1of(s, args);
			break;
		case 'L':
			if (method == "Lower")
				return Lower(s, args);
			if (method == "Lower?")
				return LowerQ(s, args);
			break;
		case 'M':
			if (method == "Match")
				return Match(s, args);
			break;
		case 'N':
			if (method == "Number?")
				return NumberQ(s, args);
			if (method == "Numeric?")
				return NumericQ(s, args);
			break;
		case 'P':
			if (method == "Prefix?")
				return StartsWith(s, args);
			break;
		case 'R':
			if (method == "Repeat")
				return Repeat(s, args);
			if (method == "Replace")
				return Replace(s, args);
			break;
		case 'S':
			if (method == "ServerEval")
				return Eval(s, args);
			if (method == "Size")
				return Size(s, args);
			if (method == "Split")
				return Split(s, args);
			if (method == "StartsWith")
				return StartsWith(s, args);
			if (method == "Substr")
				return Substr(s, args);
			if (method == "Suffix?")
				return EndsWith(s, args);
			break;
		case 'T':
			if (method == "Tr")
				return Tr(s, args);
			break;
		case 'U':
			if (method == "Unescape")
				return Unescape(s, args);
			if (method == "Upper")
				return Upper(s, args);
			if (method == "Upper?")
				return UpperQ(s, args);
			break;
		}
		return userDefined("Strings", method).invoke(s, method, args);
	}

	private static Boolean AlphaQ(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		if (s.length() == 0)
			return Boolean.FALSE;
		for (int i = 0; i < s.length(); ++i)
			if (!Character.isLetter(s.charAt(i)))
				return Boolean.FALSE;
		return Boolean.TRUE;
	}

	private static Boolean AlphaNumQ(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		if (s.length() == 0)
			return Boolean.FALSE;
		for (int i = 0; i < s.length(); ++i)
			if (!Character.isLetterOrDigit(s.charAt(i)))
				return Boolean.FALSE;
		return Boolean.TRUE;
	}

	private static int Asc(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return s.length() == 0 ? 0 : (int) s.charAt(0);
	}

	private static Object Compile(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return Compiler.compile("StringCompile", s);
	}

	private static final int TABWIDTH = 4;

	private static String Detab(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
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

	private static final FunctionSpec sFS = new FunctionSpec("s");

	private static boolean EndsWith(String s, Object[] args) {
		args = Args.massage(sFS, args);
		return s.endsWith(toStr(args[0]));
	}

	static final Pattern globalRx = Pattern.compile("[A-Z][_a-zA-Z0-9][!?]?");

	private static String Entab(String s, Object[] args) {
		StringBuilder sb = new StringBuilder(s.length());
		int si = 0;
		for (;;) { // for each line
			// convert leading spaces & tabs
			char c;
			int col = 0;
			while (0 != (c = get(s, si++))) {
				if (c == ' ')
					++col;
				else if (c == '\t')
					for (++col; !istab(col); ++col)
						;
				else
					break;
			}
			--si;
			int dstcol = 0;
			for (int j = 0; j <= col; ++j)
				if (istab(j)) {
					sb.append('\t');
					dstcol = j;
				}
			for (; dstcol < col; ++dstcol)
				sb.append(' ');

			// copy the rest of the line
			while (0 != (c = get(s, si++)) && c != '\n' && c != '\r')
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
	private static boolean istab(int col) {
		return col > 0 && (col % 4) == 0;
	}
	private static boolean isTabOrSpace(char c) {
		return c == ' ' || c == '\t';
	}

	private static Object Eval(String s, Object[] args) {
		Object result = eval(s, args);
		return result == null ? "" : result;
	}

	private static Object Eval2(String s, Object[] args) {
		Object value = eval(s, args);
		SuContainer result = new SuContainer();
		if (value != null)
			result.append(value);
		return result;
	}

	private static Object eval(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return globalRx.matcher(s).matches() ? Globals.get(s)
				: Compiler.eval(s);
	}

	private static final FunctionSpec extractFS =
		new FunctionSpec(array("pattern", "part"), false);

	static Object Extract(String s, Object... args) {
		args = Args.massage(extractFS, args);
		String pat = Ops.toStr(args[0]);
		Pattern pattern = Regex.getPat(pat, s);
		Matcher matcher = pattern.matcher(s);
		if (!matcher.find())
			return Boolean.FALSE;
		MatchResult result = matcher.toMatchResult();
		int part;
		if (args[1] == Boolean.FALSE)
			part = result.groupCount() == 0 ? 0 : 1;
		else
			part = Ops.toInt(args[1]);
		return result.group(part);
	}

	private static int Find(String s, Object[] args) {
		args = Args.massage(sFS, args);
		int i = s.indexOf(toStr(args[0]));
		return i == -1 ? s.length() : i;
	}

	private static Object FindLast(String s, Object[] args) {
		args = Args.massage(sFS, args);
		int i = s.lastIndexOf(toStr(args[0]));
		return i == -1 ? Boolean.FALSE : i;
	}

	private static int Find1of(String s, Object[] args) {
		args = Args.massage(sFS, args);
		String set = Ops.toStr(args[0]);
		for (int i = 0; i < s.length(); ++i) {
			int j = set.indexOf(s.charAt(i));
			if (j != -1)
				return i;
		}
		return s.length();
	}

	private static int Findnot1of(String s, Object[] args) {
		args = Args.massage(sFS, args);
		String set = Ops.toStr(args[0]);
		for (int i = 0; i < s.length(); ++i) {
			int j = set.indexOf(s.charAt(i));
			if (j == -1)
				return i;
		}
		return s.length();
	}

	private static Object FindLast1of(String s, Object[] args) {
		args = Args.massage(sFS, args);
		String set = Ops.toStr(args[0]);
		for (int i = s.length() - 1; i >= 0; --i) {
			int j = set.indexOf(s.charAt(i));
			if (j != -1)
				return i;
		}
		return Boolean.FALSE;
	}

	private static Object FindLastnot1of(String s, Object[] args) {
		args = Args.massage(sFS, args);
		String set = Ops.toStr(args[0]);
		for (int i = s.length() - 1; i >= 0; --i) {
			int j = set.indexOf(s.charAt(i));
			if (j == -1)
				return i;
		}
		return Boolean.FALSE;
	}

	private static String Lower(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return s.toLowerCase();
	}

	private static Boolean LowerQ(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Boolean result = Boolean.FALSE;
		for (int i = 0; i < s.length(); ++i) {
			char c = s.charAt(i);
			if (Character.isUpperCase(c))
				return Boolean.FALSE;
			else if (Character.isLowerCase(c))
				result = true;
		}
		return result;
	}

	private static final FunctionSpec matchFS = new FunctionSpec("pattern");

	private static Object Match(String s, Object[] args) {
		args = Args.massage(matchFS, args);
		Pattern pat = Regex.getPat(Ops.toStr(args[0]), s);
		Matcher m = pat.matcher(s);
		if (!m.find())
			return Boolean.FALSE;
		MatchResult mr = m.toMatchResult();
		SuContainer c = new SuContainer();
		for (int i = 0; i <= mr.groupCount(); ++i) {
			SuContainer c2 = new SuContainer();
			int start = mr.start(i);
			c2.append(start);
			c2.append(start == -1 ? -1 : mr.end() - start);
			c.append(c2);
		}
		return c;
	}

	private static Boolean NumberQ(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		int i = 0;
		char c = get(s, i);
		if (c == '+' || c == '-')
			c = get(s, ++i);
		boolean intdigits = Character.isDigit(c);
		while (Character.isDigit(c))
			c = get(s, ++i);
		if (c == '.')
			c = get(s, ++i);
		boolean fracdigits = Character.isDigit(c);
		while (Character.isDigit(c))
			c = get(s, ++i);
		if (!intdigits && !fracdigits)
			return Boolean.FALSE;
		if (c == 'e' || c == 'E') {
			c = get(s, ++i);
			if (c == '-')
				c = get(s, ++i);
			while (Character.isDigit(c))
				c = get(s, ++i);
		}
		return i == s.length();
	}

	private static Boolean NumericQ(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		if (s.length() == 0)
			return Boolean.FALSE;
		for (int i = 0; i < s.length(); ++i)
			if (!Character.isDigit(s.charAt(i)))
				return Boolean.FALSE;
		return Boolean.TRUE;
	}

	private static char get(String s, int i) {
		return i < s.length() ? s.charAt(i) : 0;
	}

	private static final FunctionSpec repeatFS = new FunctionSpec("n");

	private static String Repeat(String s, Object[] args) {
		args = Args.massage(repeatFS, args);
		int n = Math.max(0, Ops.toInt(args[0]));
		StringBuilder sb = new StringBuilder(n * s.length());
		for (int i = 0; i < n; ++i)
			sb.append(s);
		return sb.toString();
	}

	private static final FunctionSpec replaceFS =
			new FunctionSpec(array("pattern", "block", "count"), 99999);

	static String Replace(String s, Object... args) {
		args = Args.massage(replaceFS, args);
		Pattern pat = Regex.getPat(Ops.toStr(args[0]), s);
		String rep = null;
		if (args[1] instanceof String)
			rep = Ops.toStr(args[1]);
		int n = Ops.toInt(args[2]);

		Matcher m = pat.matcher(s);
		StringBuilder sb = new StringBuilder();
		int append = 0;
		for (int i = 0; i < n && m.find(); ++i) {
			sb.append(s.substring(append, m.start()));
			if (rep == null) {
				Object t = Ops.call(args[1], m.group());
				sb.append(t == null ? m.group() : Ops.toStr(t));
			} else
				suneido.Regex.appendReplacement(m, sb, rep);
			append = m.end();
		}
		sb.append(s.substring(append));
		return sb.toString();
	}

	private static int Size(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return s.length();
	}

	static SuContainer Split(String s, Object... args) {
		args = Args.massage(sFS, args);
		String sep = Ops.toStr(args[0]);
		if (sep.equals(""))
			throw new SuException(
					"string.Split: separator must not be empty string");
		SuContainer ob = new SuContainer();
		int i = 0;
		for (int j; -1 != (j = s.indexOf(sep, i)); i = j + sep.length())
			ob.append(s.substring(i, j));
		if (i < s.length())
			ob.append(s.substring(i));
		return ob;
	}

	private static boolean StartsWith(String s, Object[] args) {
		args = Args.massage(sFS, args);
		return s.startsWith(toStr(args[0]));
	}

	private static final FunctionSpec substrFS =
			new FunctionSpec(array("i", "n"), Integer.MAX_VALUE);

	private static String Substr(String s, Object[] args) {
		args = Args.massage(substrFS, args);
		int len = s.length();
		int i = toInt(args[0]);
		if (i < 0)
			i += len;
		i = max(0, min(i, len));
		int n = toInt(args[1]);
		if (n < 0)
			n += len - i;
		n = max(0, min(n, len - i));
		return s.substring(i, i + n);
	}

	private static final FunctionSpec trFS =
			new FunctionSpec(array("from", "to"), "");

	private static String Tr(String s, Object[] args) {
		args = Args.massage(trFS, args);
		return Tr.tr(s, toStr(args[0]), toStr(args[1]));
	}

	private static String Unescape(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		CharacterIterator ci = new StringCharacterIterator(s);
		StringBuilder sb = new StringBuilder(s.length());
		for (char c = ci.first(); c != DONE; c = ci.next())
			if (c == '\\')
				sb.append(doesc(ci));
			else
				sb.append(c);
		return sb.toString();
	}

	public static char doesc(CharacterIterator ci) {
		assert ci.current() == '\\';
		int dig1, dig2, dig3;
		char c = ci.next();
		switch (c) {
		case 'n':
			return '\n';
		case 't':
			return '\t';
		case 'r':
			return '\r';
		case 'x':
			// hex
			dig1 = Character.digit(ci.next(), 16);
			dig2 = Character.digit(ci.next(), 16);
			if (dig1 != -1 && dig2 != -1)
				return (char) (16 * dig1 + dig2);
			else {
				ci.setIndex(ci.getIndex() - 3); // back to backslash
				return '\\';
			}
		case '\\':
		case '"':
		case '\'':
			return c;
		default:
			// octal
			dig1 = Character.digit(ci.next(), 8);
			dig2 = Character.digit(ci.next(), 8);
			dig3 = Character.digit(ci.next(), 8);
			if (dig1 != -1 && dig2 != -1 && dig3 != -1)
				return (char) (64 * dig1 + 8 * dig2 + dig3);
			else {
				ci.setIndex(ci.getIndex() - 4); // back to backslash
				return '\\';
			}
		}
	}

	private static String Upper(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return s.toUpperCase();
	}

	private static Boolean UpperQ(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Boolean result = Boolean.FALSE;
		for (int i = 0; i < s.length(); ++i) {
			char c = s.charAt(i);
			if (Character.isLowerCase(c))
				return Boolean.FALSE;
			else if (Character.isUpperCase(c))
				result = Boolean.TRUE;
		}
		return result;
	}

}
