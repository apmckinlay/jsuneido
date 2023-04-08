/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static suneido.runtime.Ops.toInt;
import static suneido.util.Tr.tr;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import com.google.common.base.Ascii;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;

import suneido.*;
import suneido.compiler.Compiler;
import suneido.compiler.Doesc;
import suneido.runtime.BuiltinMethods;
import suneido.runtime.Ops;
import suneido.runtime.Params;
import suneido.util.Regex;
import suneido.util.Regex.Result;
import suneido.util.RegexCache;
import suneido.util.Tabs;
import suneido.util.Util;

public class StringMethods extends BuiltinMethods {
	public static final StringMethods singleton = new StringMethods();

	private StringMethods() {
		super("string", StringMethods.class, "Strings");
	}

	private static String toStr(Object self) {
		return self.toString();
	}

	private static CharSequence toSeq(Object self) {
		return self instanceof CharSequence ? (CharSequence) self : toStr(self);
	}

	public static Object AlphaQ(Object self) {
		String s = toStr(self);
		if (s.length() == 0)
			return Boolean.FALSE;
		for (int i = 0; i < s.length(); ++i)
			if (!Character.isLetter(s.charAt(i)))
				return Boolean.FALSE;
		return Boolean.TRUE;
	}

	public static Object AlphaNumQ(Object self) {
		String s = toStr(self);
		if (s.length() == 0)
			return Boolean.FALSE;
		for (int i = 0; i < s.length(); ++i)
			if (!Character.isLetterOrDigit(s.charAt(i)))
				return Boolean.FALSE;
		return Boolean.TRUE;
	}

	/** overridden in Except.As */
	@Params("string")
	public static Object As(Object self, Object a) {
		return a;
	}

	public static Object Asc(Object self) {
		String s = toStr(self);
		return s.length() == 0 ? 0 : (int) s.charAt(0);
	}

	@Params("object=false")
	public static Object Compile(Object self, Object a) {
		SuObject warnings = (a == Boolean.FALSE) ? null : (SuObject) a;
		return Compiler.compile("stringCompile", toStr(self), warnings);
	}

	@Params("c")
	public static Object Count(Object self, Object a) {
		String s = toStr(self);
		String substr = toStr(a);

		if (substr.length() == 0)
			return 0;
		int n = 0;
		for (int i = 0; (i = s.indexOf(substr, i)) != -1; i += substr.length())
			++n;
		return n;
	}

	private static final int TABWIDTH = 4;

	public static Object Detab(Object self) {
		return Tabs.detab(toStr(self), TABWIDTH);
	}

	public static Object Entab(Object self) {
		return Tabs.entab(toStr(self), TABWIDTH);
	}

	public static Object Eval(Object self) {
		Object result = seval(toStr(self));
		return result == null ? "" : result;
	}

	public static Object Eval2(Object self) {
		Object value = seval(toStr(self));
		SuObject result = new SuObject();
		if (value != null)
			result.add(value);
		return result;
	}

	private static Object seval(String s) {
		return isGlobal(s)
				? Suneido.context.get(s)
				: Compiler.eval(s);
	}

	static final Pattern globalRx = Pattern.compile("[A-Z][_a-zA-Z0-9]*[!?]?");

	static boolean isGlobal(String s) {
		return globalRx.matcher(s).matches();
	}

	@Params("pattern, part = false")
	public static Object Extract(Object self, Object a, Object b) {
		String s = toStr(self);
		String pat = Ops.toStr(a);
		return extract(s, pat, b);
	}
	static Object extract(String s, String pat, Object part) {
		Regex.Result result = RegexCache.getPattern(pat).firstMatch(s, 0);
		if (result == null)
			return Boolean.FALSE;
		int part_i = (part == Boolean.FALSE)
				? (result.groupCount() == 0) ? 0 : 1
				: Ops.toInt(part);
		return result.group(s, part_i);
	}

	@Params("s, pos = 0")
	public static Object Find(Object self, Object a, Object b) {
		String s = toStr(self);
		int pos = toInt(b);
		int i = s.indexOf(toStr(a), pos);
		return i == -1 ? s.length() : i;
	}

	@Params("s, pos = false")
	public static Object FindLast(Object self, Object a, Object b) {
		String s = toStr(self);
		String sub = toStr(a);
		int pos = (b == Boolean.FALSE) ? s.length() : toInt(b);
		int i = s.lastIndexOf(sub, pos);
		return i == -1 ? Boolean.FALSE : i;
	}

	@Params("string, pos = 0")
	public static Object Find1of(Object self, Object a, Object b) {
		String s = toStr(self);
		String set = Ops.toStr(a);
		int pos = toInt(b);
		for (int i = Math.max(0, pos); i < s.length(); ++i) {
			if (-1 != set.indexOf(s.charAt(i)))
				return i;
		}
		return s.length();
	}

	@Params("string, pos = 0")
	public static Object Findnot1of(Object self, Object a, Object b) {
		String s = toStr(self);
		String set = Ops.toStr(a);
		int pos = toInt(b);
		for (int i = Math.max(0, pos); i < s.length(); ++i) {
			if (-1 == set.indexOf(s.charAt(i)))
				return i;
		}
		return s.length();
	}

	@Params("string, pos = false")
	public static Object FindLast1of(Object self, Object a, Object b) {
		String s = toStr(self);
		String set = Ops.toStr(a);
		int pos = (b == Boolean.FALSE)
				? s.length() - 1
				: Math.min(toInt(b), s.length() - 1);
		for (int i = pos; i >= 0; --i) {
			if (-1 != set.indexOf(s.charAt(i)))
				return i;
		}
		return Boolean.FALSE;
	}

	@Params("string, pos = false")
	public static Object FindLastnot1of(Object self, Object a, Object b) {
		String s = toStr(self);
		String set = Ops.toStr(a);
		int pos = (b == Boolean.FALSE)
				? s.length() - 1
				: Math.min(toInt(b), s.length() - 1);
		for (int i = pos; i >= 0; --i) {
			if (-1 == set.indexOf(s.charAt(i)))
				return i;
		}
		return Boolean.FALSE;
	}

	static final Charset Windows1252 = Charset.forName("windows-1252");

	public static Object FromUtf8(Object self) {
		CharBuffer cb = Charsets.UTF_8.decode(
				ByteBuffer.wrap(Util.stringToBytes(toStr(self))));
		return Util.bytesToString(Windows1252.encode(cb));
	}

	@Params("string")
	public static Object HasQ(Object self, Object a) {
		return (toStr(self)).contains(toStr(a));
	}

	public static Object Iter(Object self) {
		return new Iterate((CharSequence) self);
	}

	private static final class Iterate extends SuValue {
		final CharSequence seq;
		final int length;
		int index;

		Iterate(CharSequence seq) {
			this.seq = seq;
			this.length = seq.length();
			this.index = 0;
		}

		@Override
		public String typeName() {
			return "StringIter";
		}

		@Override
		public SuValue lookup(String method) {
			return IterateMethods.singleton.lookup(method);
		}
	}

	public static final class IterateMethods extends BuiltinMethods {
		public static final SuValue singleton = new IterateMethods();

		protected IterateMethods() {
			super("stringiter", IterateMethods.class, null);
		}

		public static Object Next(Object self) {
			Iterate iter = (Iterate) self;
			// NOTE: A Buffer can be modified during iteration, in the sense
			//       that the contents of the buffer can be overwritten with
			//       other data. However, the *length* of any character sequence
			//       can't change once the object is created.
			return iter.index < iter.length
					? Character.toString(iter.seq.charAt(iter.index++)) : self;
		}

		public static Object Iter(Object self) {
			return self;
		}
	}

	public static Object Lower(Object self) {
		return Ascii.toLowerCase(toStr(self));
	}

	public static Object LowerQ(Object self) {
		String s = toStr(self);
		Boolean result = false;
		for (int i = 0; i < s.length(); ++i) {
			char c = s.charAt(i);
			if (Ascii.isUpperCase(c))
				return false;
			else if (Ascii.isLowerCase(c))
				result = true;
		}
		return result;
	}

	@Params("n, block")
	public static Object MapN(Object self, Object a, Object block) {
		String s = toStr(self);
		int slen = s.length();
		int n = toInt(a);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < slen; i += n) {
			String chunk = s.substring(i, Math.min(i + n, slen));
			Object result = Ops.call(block, chunk);
			if (result != null)
				sb.append(Ops.toStr(result));
		}
		return sb.toString();
	}

	@Params("pattern, pos = false, prev = false")
	public static Object Match(Object self, Object a, Object b, Object c) {
		String s = toStr(self);
		boolean prev = Ops.toBoolean_(c);
		int pos = b == Boolean.FALSE ? (prev ? s.length() : 0) : toInt(b);
		Regex.Pattern pat = RegexCache.getPattern(toStr(a));
		Regex.Result result = prev ? pat.lastMatch(s, pos) : pat.firstMatch(s, pos);
		if (result == null)
			return Boolean.FALSE;
		SuObject ob = new SuObject();
		for (int i = 0; i <= result.groupCount(); ++i) {
			if (result.end[i] != -1) {
				int start = result.pos[i];
				ob.put(i, SuObject.of(start, result.end[i] - start));
			}
		}
		return ob;
	}

	@Params("n")
	public static Object NthLine(Object self, Object a) {
		String s = toStr(self);
		int sn = s.length();
		int n = toInt(a);
		int i = 0;
		for (; i < sn && n > 0; ++i)
			if (s.charAt(i) == '\n')
				--n;
		int end = i;
		while (end < sn && s.charAt(end) != '\n')
			++end;
		while (end > i && s.charAt(end - 1) == '\r')
			--end;
		return s.substring(i, end);
	}

	public static Object NumberQ(Object self) {
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
			if (c == '-' || c == '+')
				c = sget(s, ++i);
			if (! Character.isDigit(c))
				return false;
			while (Character.isDigit(c))
				c = sget(s, ++i);
		}
		return i == s.length();
	}

	public static Object NumericQ(Object self) {
		String s = toStr(self);
		if (s.length() == 0)
			return Boolean.FALSE;
		for (int i = 0; i < s.length(); ++i)
			if (!Character.isDigit(s.charAt(i)))
				return Boolean.FALSE;
		return Boolean.TRUE;
	}

	private static char sget(String s, int i) {
		return i < s.length() ? s.charAt(i) : 0;
	}

	// for debugging
	// see also AstParse
	public static Object Parse(Object self) {
		return Compiler.parse("", toStr(self)).toString();
	}

	@Params("s, i = 0")
	public static Object PrefixQ(Object self, Object a, Object b) {
		String s = toStr(self);
		int len = s.length();
		int i = toInt(b);
		if (i < 0)
			i += len;
		i = max(0, min(i, len));
		return s.startsWith(toStr(a), i);
	}

	@Params("n")
	public static Object Repeat(Object self, Object a) {
		return Strings.repeat(toStr(self), Math.max(0, toInt(a)));
	}

	@Params("pattern, block = '', count = INTMAX")
	public static Object Replace(Object self, Object a, Object b, Object c) {
		String s = toStr(self);
		String pat = Ops.toStr(a);
		int n = toInt(c);
		return replace(s, pat, b, n);
	}

	static String replace(String s, String p, Object r, int n) {
		if (n <= 0)
			return s;
		Regex.Pattern pat = RegexCache.getPattern(p);
		ForEach foreach = new ForEach(s, r, n);
		pat.forEachMatch(s, foreach);
		return foreach.result();
	}

	private static class ForEach implements Regex.ForEach {
		final String s;
		final String rep;
		int n;
		final Object block;
		int append = 0;
		StringBuilder sb = null; // construct only if needed

		ForEach(String s, Object r, int n) {
			this.s = s;
			this.n = n;
			block = r;
			rep = Ops.isString(r) ? r.toString() : null;
		}
		@Override
		public int each(Result res) {
			if (sb == null)
				sb = new StringBuilder(s.length());
			sb.append(s.substring(append, res.pos[0]));
			if (rep == null) {
				String matched = res.group(s, 0);
				Object t = Ops.call(block, matched);
				sb.append(t == null ? matched : Ops.toStr(t));
			} else
				suneido.util.RegexReplace.append(s, res, rep, sb);
			append = res.end[0];
			return --n > 0 ? Math.max(res.end[0], res.pos[0] + 1) : s.length() + 1;
		}
		String result() {
			if (sb == null)
				return s;
			sb.append(s.substring(append));
			return sb.toString();
		}
	}

	public static Object Reverse(Object self) {
		return new StringBuilder(toStr(self)).reverse().toString();
	}

	public static Object ServerEval(Object self) {
		return TheDbms.dbms().run(toStr(self));
	}

	public static Object Size(Object self) {
		return toSeq(self).length();
	}

	@Params("string")
	public static Object Split(Object self, Object a) {
		String s = toStr(self);
		String sep = Ops.toStr(a);
		return split(s, sep);
	}

	static SuObject split(String s, String sep) {
		if (sep.equals(""))
			throw new SuException(
					"string.Split: separator must not be empty string");
		SuObject ob = new SuObject();
		int i = 0;
		for (int j; -1 != (j = s.indexOf(sep, i)); i = j + sep.length())
			ob.add(s.substring(i, j));
		if (i < s.length())
			ob.add(s.substring(i));
		return ob;
	}

	@Params("i, n = INTMAX")
	public static Object Substr(Object self, Object a, Object b) {
		CharSequence s = toSeq(self);
		int len = s.length();
		int i = Ops.index(a);
		if (i < 0)
			i += len;
		i = max(0, min(i, len));
		int n = toInt(b);
		if (n < 0)
			n += len - i;
		n = max(0, min(n, len - i));
		return s.subSequence(i, i + n);
	}

	@Params("string")
	public static Object SuffixQ(Object self, Object a) {
		return toStr(self).endsWith(toStr(a));
	}

	public static Object ToUtf8(Object self) {
		CharBuffer cb = Windows1252.decode(
				ByteBuffer.wrap(Util.stringToBytes(toStr(self))));
		return Util.bytesToString(Charsets.UTF_8.encode(cb));
	}

	@Params("from, to = ''")
	public static Object Tr(Object self, Object a, Object b) {
		return tr(toStr(self), toStr(a), toStr(b));
	}

	public static Object Unescape(Object self) {
		String s = toStr(self);
		return Doesc.doesc(s);
	}

	public static Object Upper(Object self) {
		return Ascii.toUpperCase(toStr(self));
	}

	public static Object UpperQ(Object self) {
		String s = toStr(self);
		Boolean result = false;
		for (int i = 0; i < s.length(); ++i) {
			char c = s.charAt(i);
			if (Ascii.isLowerCase(c))
				return false;
			else if (Ascii.isUpperCase(c))
				result = true;
		}
		return result;
	}
}
