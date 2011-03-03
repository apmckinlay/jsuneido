package suneido.language.builtin;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.text.CharacterIterator.DONE;
import static suneido.language.Ops.toInt;
import static suneido.util.Tr.tr;
import static suneido.util.Util.array;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.regex.*;

import suneido.*;
import suneido.language.*;
import suneido.language.Compiler;
import suneido.util.Util;

import com.google.common.base.Charsets;

public class StringMethods extends BuiltinMethods {
	public static final StringMethods singleton = new StringMethods();

	private StringMethods() {
		super(StringMethods.class, "Strings");
	}

	private static String toStr(Object self) {
		return self instanceof String
				? (String) self
				: ((Concat) self).toString();
	}

	public static class AlphaQ extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			String s = toStr(self);
			if (s.length() == 0)
				return Boolean.FALSE;
			for (int i = 0; i < s.length(); ++i)
				if (!Character.isLetter(s.charAt(i)))
					return Boolean.FALSE;
			return Boolean.TRUE;
		}
	}

	public static class AlphaNumQ extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			String s = toStr(self);
			if (s.length() == 0)
				return Boolean.FALSE;
			for (int i = 0; i < s.length(); ++i)
				if (!Character.isLetterOrDigit(s.charAt(i)))
					return Boolean.FALSE;
			return Boolean.TRUE;
		}
	}

	// TODO implement exception.As
	public static class As extends SuMethod1 {
		@Override
		public Object eval1(Object self, Object a) {
			return a;
		}
	}

	public static class Asc extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			String s = toStr(self);
			return s.length() == 0 ? 0 : (int) s.charAt(0);
		}
	}

	public static class Compile extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return Compiler.compile("stringCompile", toStr(self));
		}
	}

	public static class Contains extends SuMethod1 {
		{ params = FunctionSpec.string; }
		@Override
		public Object eval1(Object self, Object a) {
			return (toStr(self)).contains(toStr(a));
		}
	}
	public static class HasQ extends Contains {
		{ params = FunctionSpec.string; }
	}

	private static final int TABWIDTH = 4;

	// TODO factor out to util
	public static class Detab extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			String s = toStr(self);
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
	}

	public static class EndsWith extends SuMethod1 {
		{ params = FunctionSpec.string; }
		@Override
		public Object eval1(Object self, Object a) {
			return toStr(self).endsWith(toStr(a));
		}
	}
	public static class SuffixQ extends EndsWith {
		{ params = FunctionSpec.string; }
	}

	// TODO factor out to util
	public static class Entab extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			String s = toStr(self);
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
		private static boolean istab(int col) {
			return col > 0 && (col % 4) == 0;
		}
		private static boolean isTabOrSpace(char c) {
			return c == ' ' || c == '\t';
		}
	}

	public static class Eval extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			Object result = seval(toStr(self));
			return result == null ? "" : result;
		}
	}

	public static class Eval2 extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			Object value = seval(toStr(self));
			SuContainer result = new SuContainer();
			if (value != null)
				result.add(value);
			return result;
		}
	}

	static final Pattern globalRx = Pattern.compile("[A-Z][_a-zA-Z0-9][!?]?");

	private static Object seval(String s) {
		return globalRx.matcher(s).matches()
				? Globals.get(s)
				: Compiler.eval(s);
	}

	public static class Extract extends SuMethod2 {
		{ params = new FunctionSpec(array("pattern", "part"), false); }
		@Override
		public Object eval2(Object self, Object a, Object b) {
			String s = toStr(self);
			String pat = Ops.toStr(a);
			return extract(s, pat, b);
		}
	}
	static Object extract(String s, String pat, Object part) {
		Pattern pattern = Regex.getPat(pat, s);
		Matcher matcher = pattern.matcher(s);
		if (!matcher.find())
			return Boolean.FALSE;
		MatchResult result = matcher.toMatchResult();
		int part_i = (part == Boolean.FALSE)
				? (result.groupCount() == 0) ? 0 : 1
				: Ops.toInt(part);
		return result.group(part_i);
	}

	public static class Find extends SuMethod2 {
		{ params = new FunctionSpec(array("s", "i"), 0); }
		@Override
		public Object eval2(Object self, Object a, Object b) {
			String s = toStr(self);
			int i = s.indexOf(toStr(a), toInt(b));
			return i == -1 ? s.length() : i;
		}
	}

	public static class FindLast extends SuMethod1 {
		{ params = FunctionSpec.string; }
		@Override
		public Object eval1(Object self, Object a) {
			int i = toStr(self).lastIndexOf(toStr(a));
			return i == -1 ? Boolean.FALSE : i;
		}
	}

	public static class Find1of extends SuMethod1 {
		{ params = FunctionSpec.string; }
		@Override
		public Object eval1(Object self, Object a) {
			String s = toStr(self);
			String set = Ops.toStr(a);
			for (int i = 0; i < s.length(); ++i) {
				int j = set.indexOf(s.charAt(i));
				if (j != -1)
					return i;
			}
			return s.length();
		}
	}

	public static class Findnot1of extends SuMethod1 {
		{ params = FunctionSpec.string; }
		@Override
		public Object eval1(Object self, Object a) {
			String s = toStr(self);
			String set = Ops.toStr(a);
			for (int i = 0; i < s.length(); ++i) {
				int j = set.indexOf(s.charAt(i));
				if (j == -1)
					return i;
			}
			return s.length();
		}
	}

	public static class FindLast1of extends SuMethod1 {
		{ params = FunctionSpec.string; }
		@Override
		public Object eval1(Object self, Object a) {
			String s = toStr(self);
			String set = Ops.toStr(a);
			for (int i = s.length() - 1; i >= 0; --i) {
				int j = set.indexOf(s.charAt(i));
				if (j != -1)
					return i;
			}
			return Boolean.FALSE;
		}
	}

	public static class FindLastnot1of extends SuMethod1 {
		{ params = FunctionSpec.string; }
		@Override
		public Object eval1(Object self, Object a) {
			String s = toStr(self);
			String set = Ops.toStr(a);
			for (int i = s.length() - 1; i >= 0; --i) {
				int j = set.indexOf(s.charAt(i));
				if (j == -1)
					return i;
			}
			return Boolean.FALSE;
		}
	}

	public static class Lower extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return toStr(self).toLowerCase();
		}
	}

	public static class LowerQ extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			String s = toStr(self);
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
	}

	public static class Match extends SuMethod1 {
		{ params = new FunctionSpec("pattern"); }
		@Override
		public Object eval1(Object self, Object a) {
			String s = toStr(self);
			Pattern pat = Regex.getPat(Ops.toStr(a), s);
			Matcher m = pat.matcher(s);
			if (!m.find())
				return Boolean.FALSE;
			MatchResult mr = m.toMatchResult();
			SuContainer c = new SuContainer();
			for (int i = 0; i <= mr.groupCount(); ++i) {
				SuContainer c2 = new SuContainer();
				int start = mr.start(i);
				c2.add(start);
				c2.add(start == -1 ? -1 : mr.end() - start);
				c.add(c2);
			}
			return c;
		}
	}

	public static class NumberQ extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			String s = toStr(self);
			int i = 0;
			char c = sget(s, i);
			if (c == '+' || c == '-')
				c = sget(s, ++i);
			boolean intdigits = Character.isDigit(c);
			while (Character.isDigit(c))
				c = sget(s, ++i);
			if (c == '.')
				c = sget(s, ++i);
			boolean fracdigits = Character.isDigit(c);
			while (Character.isDigit(c))
				c = sget(s, ++i);
			if (!intdigits && !fracdigits)
				return Boolean.FALSE;
			if (c == 'e' || c == 'E') {
				c = sget(s, ++i);
				if (c == '-')
					c = sget(s, ++i);
				while (Character.isDigit(c))
					c = sget(s, ++i);
			}
			return i == s.length();
		}
	}

	public static class NumericQ extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			String s = toStr(self);
			if (s.length() == 0)
				return Boolean.FALSE;
			for (int i = 0; i < s.length(); ++i)
				if (!Character.isDigit(s.charAt(i)))
					return Boolean.FALSE;
			return Boolean.TRUE;
		}
	}

	private static char sget(String s, int i) {
		return i < s.length() ? s.charAt(i) : 0;
	}

	public static class Repeat extends SuMethod1 {
		{ params = new FunctionSpec("n"); }
		@Override
		public Object eval1(Object self, Object a) {
			String s = toStr(self);
			int n = Math.max(0, toInt(a));
			StringBuilder sb = new StringBuilder(n * s.length());
			for (int i = 0; i < n; ++i)
				sb.append(s);
			return sb.toString();
		}
	}

	public static class Replace extends SuMethod3 {
		{ params = new FunctionSpec(array("pattern", "block", "count"), 99999); }
		@Override
		public Object eval3(Object self, Object a, Object b, Object c) {
			String s = toStr(self);
			String pat = Ops.toStr(a);
			int n = toInt(c);
			return replace(s, pat, b, n);
		}
	}
	static Object replace(String s, String p, Object b, int n) {
		Pattern pat = Regex.getPat(p, s);
		String rep = null;
		if (Ops.isString(b))
			rep = b.toString();

		Matcher m = pat.matcher(s);
		StringBuilder sb = new StringBuilder();
		int append = 0;
		for (int i = 0; i < n && m.find(); ++i) {
			sb.append(s.substring(append, m.start()));
			if (rep == null) {
				Object t = Ops.call(b, m.group());
				sb.append(t == null ? m.group() : Ops.toStr(t));
			} else
				suneido.Regex.appendReplacement(m, sb, rep);
			append = m.end();
		}
		sb.append(s.substring(append));
		return sb.toString();
	}

	public static class ServerEval extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return TheDbms.dbms().run(toStr(self));
		}
	}

	public static class Size extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return self instanceof String
				? ((String) self).length()
				: ((Concat) self).length();
		}
	}

	public static class Split extends SuMethod1 {
		{ params = FunctionSpec.string; }
		@Override
		public Object eval1(Object self, Object a) {
			String s = toStr(self);
			String sep = Ops.toStr(a);
			return split(s, sep);
		}
	}

	static SuContainer split(String s, String sep) {
		if (sep.equals(""))
			throw new SuException(
					"string.Split: separator must not be empty string");
		SuContainer ob = new SuContainer();
		int i = 0;
		for (int j; -1 != (j = s.indexOf(sep, i)); i = j + sep.length())
			ob.add(s.substring(i, j));
		if (i < s.length())
			ob.add(s.substring(i));
		return ob;
	}

	public static class StartsWith extends SuMethod2 {
		{ params = new FunctionSpec(array("s", "i"), 0); }
		@Override
		public Object eval2(Object self, Object a, Object b) {
			String s = toStr(self);
			return s.startsWith(toStr(a), toInt(b));
		}
	}
	public static class PrefixQ extends SuMethod2 {
		{ params = new FunctionSpec(array("s", "i"), 0); }
		@Override
		public Object eval2(Object self, Object a, Object b) {
			String s = toStr(self);
			return s.startsWith(toStr(a), toInt(b));
		}
	}

	public static class Substr extends SuMethod2 {
		{ params = new FunctionSpec(array("i", "n"), Integer.MAX_VALUE); }
		@Override
		public Object eval2(Object self, Object a, Object b) {
			String s = toStr(self);
			int len = s.length();
			int i = toInt(a);
			if (i < 0)
				i += len;
			i = max(0, min(i, len));
			int n = toInt(b);
			if (n < 0)
				n += len - i;
			n = max(0, min(n, len - i));
			return s.substring(i, i + n);
		}
	}

	private static final Charset Windows1252 =
			Charset.forName("windows-1252");

	public static class ToUtf8 extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			CharBuffer cb = Windows1252.decode(
					ByteBuffer.wrap(Util.stringToBytes(toStr(self))));
			return Util.bytesToString(Charsets.UTF_8.encode(cb));
		}
	}

	public static class FromUtf8 extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			CharBuffer cb = Charsets.UTF_8.decode(
					ByteBuffer.wrap(Util.stringToBytes(toStr(self))));
			return Util.bytesToString(Windows1252.encode(cb));
	        }
	}

	public static class Tr extends SuMethod2 {
		{ params = new FunctionSpec(array("from", "to"), ""); }
		@Override
		public Object eval2(Object self, Object a, Object b) {
			return tr(toStr(self), toStr(a), toStr(b));
		}
	}

	public static class Unescape extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			String s = toStr(self);
			CharacterIterator ci = new StringCharacterIterator(s);
			StringBuilder sb = new StringBuilder(s.length());
			for (char c = ci.first(); c != DONE; c = ci.next())
				if (c == '\\')
					sb.append(doesc(ci));
				else
					sb.append(c);
			return sb.toString();
		}
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

	public static class Upper extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return toStr(self).toUpperCase();
		}
	}

	public static class UpperQ extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			String s = toStr(self);
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

}
