/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.text.CharacterIterator.DONE;
import static suneido.language.Ops.toInt;
import static suneido.util.Tr.tr;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.regex.Pattern;

import suneido.SuContainer;
import suneido.SuException;
import suneido.SuValue;
import suneido.Suneido;
import suneido.TheDbms;
import suneido.language.BuiltinMethods;
import suneido.language.Compiler;
import suneido.language.Ops;
import suneido.language.Params;
import suneido.util.Regex;
import suneido.util.Regex.Result;
import suneido.util.RegexCache;
import suneido.util.Tabs;
import suneido.util.Util;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;

public class StringMethods extends BuiltinMethods {
	public static final StringMethods singleton = new StringMethods();

	private StringMethods() {
		super(StringMethods.class, "Strings");
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
		SuContainer warnings = (a == Boolean.FALSE) ? null : (SuContainer) a;
		return Compiler.compile("stringCompile", toStr(self), warnings);
	}

	@Params("string")
	public static Object HasQ(Object self, Object a) {
		return (toStr(self)).contains(toStr(a));
	}

	private static final int TABWIDTH = 4;

	public static Object Detab(Object self) {
		return Tabs.detab(toStr(self), TABWIDTH);
	}

	@Params("string")
	public static Object SuffixQ(Object self, Object a) {
		return toStr(self).endsWith(toStr(a));
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
		SuContainer result = new SuContainer();
		if (value != null)
			result.add(value);
		return result;
	}

	static final Pattern globalRx = Pattern.compile("[A-Z][_a-zA-Z0-9][!?]?");

	private static Object seval(String s) {
		return globalRx.matcher(s).matches()
				? Suneido.context.get(s)
				: Compiler.eval(s);
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

	@Params("s, i = 0")
	public static Object Find(Object self, Object a, Object b) {
		String s = toStr(self);
		int i = s.indexOf(toStr(a), toInt(b));
		return i == -1 ? s.length() : i;
	}

	@Params("string")
	public static Object FindLast(Object self, Object a) {
		int i = toStr(self).lastIndexOf(toStr(a));
		return i == -1 ? Boolean.FALSE : i;
	}

	@Params("string")
	public static Object Find1of(Object self, Object a) {
		String s = toStr(self);
		String set = Ops.toStr(a);
		for (int i = 0; i < s.length(); ++i) {
			int j = set.indexOf(s.charAt(i));
			if (j != -1)
				return i;
		}
		return s.length();
	}

	@Params("string")
	public static Object Findnot1of(Object self, Object a) {
		String s = toStr(self);
		String set = Ops.toStr(a);
		for (int i = 0; i < s.length(); ++i) {
			int j = set.indexOf(s.charAt(i));
			if (j == -1)
				return i;
		}
		return s.length();
	}

	@Params("string")
	public static Object FindLast1of(Object self, Object a) {
		String s = toStr(self);
		String set = Ops.toStr(a);
		for (int i = s.length() - 1; i >= 0; --i) {
			int j = set.indexOf(s.charAt(i));
			if (j != -1)
				return i;
		}
		return Boolean.FALSE;
	}

	@Params("string")
	public static Object FindLastnot1of(Object self, Object a) {
		String s = toStr(self);
		String set = Ops.toStr(a);
		for (int i = s.length() - 1; i >= 0; --i) {
			int j = set.indexOf(s.charAt(i));
			if (j == -1)
				return i;
		}
		return Boolean.FALSE;
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
		public SuValue lookup(String method) {
			return IterateMethods.singleton.lookup(method);
		}
	}

	public static final class IterateMethods extends BuiltinMethods {
		public static final SuValue singleton = new IterateMethods();

		protected IterateMethods() {
			super(IterateMethods.class, null);
		}

		public static Object Next(Object self) {
			Iterate iter = (Iterate) self;
			// NOTE: A Buffer can be modified during iteration, in the sense
			//       that the contents of the buffer can be overwritten with
			//       other data. However, the *length* of any character sequence
			//       cannot change once the object is created.
			return iter.index < iter.length ? Character.toString(
					iter.seq.charAt(iter.index++)) : self;
		}

		@Override
		public String typeName() {
			return "StringIter";
		}
	}

	public static Object Lower(Object self) {
		return toStr(self).toLowerCase();
	}

	public static Object LowerQ(Object self) {
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

	@Params("n, block")
	public static Object MapN(Object self, Object a, Object block) {
		String s = toStr(self);
		int slen = s.length();
		int n = toInt(a);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < slen; i += n) {
			String chunk = s.substring(i, Math.min(i + n, slen));
			sb.append(Ops.toStr(Ops.call(block, chunk)));
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
		SuContainer ob = new SuContainer();
		for (int i = 0; i <= result.groupCount(); ++i) {
			int start = result.pos[i];
			ob.add(SuContainer.of(start, result.end[i] - start));
		}
		return ob;
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
			if (c == '-')
				c = sget(s, ++i);
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

	// TODO: document this --VCS 20130621
	public static Object Parse(Object self) {
		return Compiler.parse(toStr(self)).toString();
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

	public static String replace(String s, String p, Object b, int n) {
		Regex.Pattern pat = RegexCache.getPattern(p);
		String rep = null;
		if (Ops.isString(b))
			rep = b.toString();
		ForEach foreach = new ForEach(s, rep, n, b);
		pat.forEachMatch(s, foreach);
		return foreach.result();
	}

	private static class ForEach implements Regex.ForEach {
		final StringBuilder sb;
		final String s;
		final String rep;
		int n;
		final Object block;
		int append = 0;

		ForEach(String s, String rep, int n, Object block) {
			this.s = s;
			this.rep = rep;
			this.n = n;
			this.block = block;
			sb = new StringBuilder(s.length());
		}
		@Override
		public int each(Result res) {
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
			sb.append(s.substring(append));
			return sb.toString();
		}
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

	@Params("s, i = 0")
	public static Object PrefixQ(Object self, Object a, Object b) {
		String s = toStr(self);
		return s.startsWith(toStr(a), toInt(b));
	}

	@Params("i, n = INTMAX")
	public static Object Substr(Object self, Object a, Object b) {
		CharSequence s = toSeq(self);
		int len = s.length();
		int i = toInt(a);
		if (i < 0)
			i += len;
		i = max(0, min(i, len));
		int n = toInt(b);
		if (n < 0)
			n += len - i;
		n = max(0, min(n, len - i));
		return s.subSequence(i, i + n);
	}

	private static final Charset Windows1252 = Charset.forName("windows-1252");

	public static Object ToUtf8(Object self) {
		CharBuffer cb = Windows1252.decode(
				ByteBuffer.wrap(Util.stringToBytes(toStr(self))));
		return Util.bytesToString(Charsets.UTF_8.encode(cb));
	}

	public static Object FromUtf8(Object self) {
		CharBuffer cb = Charsets.UTF_8.decode(
				ByteBuffer.wrap(Util.stringToBytes(toStr(self))));
		return Util.bytesToString(Windows1252.encode(cb));
	}

	@Params("from, to = ''")
	public static Object Tr(Object self, Object a, Object b) {
		return tr(toStr(self), toStr(a), toStr(b));
	}

	public static Object Unescape(Object self) {
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

	public static Object Upper(Object self) {
		return toStr(self).toUpperCase();
	}

	public static Object UpperQ(Object self) {
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
