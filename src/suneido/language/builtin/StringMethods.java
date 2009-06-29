package suneido.language.builtin;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static suneido.language.Ops.toInt;
import static suneido.language.Ops.toStr;
import static suneido.language.UserDefined.userDefined;
import static suneido.util.Util.array;

import java.util.regex.*;

import suneido.*;
import suneido.language.*;
import suneido.language.Compiler;
import suneido.util.Tr;

public class StringMethods {
	public static Object invoke(String s, String method, Object... args) {
		char c = method.charAt(0);
		if (c < 'P') {
			if (method == "Asc")
				return asc(s, args);
			if (method == "Compile")
				return compile(s, args);
			if (method == "EndsWith")
				return endsWith(s, args);
			if (method == "Eval")
				return eval(s, args);
			if (method == "Extract")
				return Extract(s, args);
			if (method == "Find")
				return find(s, args);
			if (method == "Find1of")
				return Find1of(s, args);
			if (method == "Findnot1of")
				return Findnot1of(s, args);
			if (method == "FindLast")
				return findLast(s, args);
			if (method == "FindLast1of")
				return FindLast1of(s, args);
			if (method == "FindLastnot1of")
				return FindLastnot1of(s, args);
			if (method == "Lower")
				return Lower(s, args);
			if (method == "Lower?")
				return LowerQ(s, args);
			if (method == "Number?")
				return NumberQ(s, args);
			if (method == "Numeric?")
				return NumericQ(s, args);
		} else {
			if (method == "Prefix?")
				return startsWith(s, args);
			if (method == "Repeat")
				return repeat(s, args);
			if (method == "Replace")
				return Replace(s, args);
			if (method == "ServerEval")
				return eval(s, args);
			if (method == "Size")
				return size(s, args);
			if (method == "Split")
				return split(s, args);
			if (method == "StartsWith")
				return startsWith(s, args);
			if (method == "Substr")
				return substr(s, args);
			if (method == "Suffix?")
				return endsWith(s, args);
			if (method == "Tr")
				return tr(s, args);
			if (method == "Upper")
				return Upper(s, args);
			if (method == "Upper?")
				return UpperQ(s, args);
		}
		return userDefined("Strings", method).invoke(s, method, args);
	}

	private static Object NumberQ(String s, Object[] args) {
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
		if (! intdigits && ! fracdigits)
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

	private static char get(String s, int i) {
		return i < s.length() ? s.charAt(i) : 0;
	}

	private static final FunctionSpec replaceFS =
			new FunctionSpec(array("pattern", "block", "count"), 99999);

	// TODO convert case
	static String Replace(String s, Object... args) {
		args = Args.massage(replaceFS, args);
		Pattern pat = Regex.getPat(Ops.toStr(args[0]));
		String rep = null;
		if (args[1] instanceof String)
			rep = Regex.convertReplacement(Ops.toStr(args[1]));
		int n = Ops.toInt(args[2]);

		Matcher m = pat.matcher(s);
		StringBuffer sb = new StringBuffer();
		if (rep != null) {
			for (int i = 0; i < n && m.find(); ++i)
				m.appendReplacement(sb, rep);
			m.appendTail(sb);
		} else {
			int append = 0;
			for (int i = 0; i < n && m.find(); ++i) {
				sb.append(s.substring(append, m.start()));
				Object t = Ops.call(args[1], m.group());
				sb.append(t == null ? m.group() : Ops.toStr(t));
				append = m.end();
			}
			sb.append(s.substring(append));
		}
		return sb.toString();
	}

	private static Object Lower(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return s.toLowerCase();
	}

	private static Object LowerQ(String s, Object[] args) {
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

	private static Object Upper(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return s.toUpperCase();
	}

	private static Object UpperQ(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Boolean result = Boolean.FALSE;
		for (int i = 0; i < s.length(); ++i) {
			char c = s.charAt(i);
			if (Character.isLowerCase(c))
				return Boolean.FALSE;
			else if (Character.isUpperCase(c))
				result = true;
		}
		return result;
	}

	private static final FunctionSpec extractFS =
			new FunctionSpec(array("pattern", "part"), false);

	static Object Extract(String s, Object... args) {
		args = Args.massage(extractFS, args);
		String pat = Ops.toStr(args[0]);
		Pattern pattern = Regex.getPat(pat);
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

	private static Object NumericQ(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		if (s.length() == 0)
			return Boolean.FALSE;
		for (int i = 0; i < s.length(); ++i)
			if (! Character.isDigit(s.charAt(i)))
				return Boolean.FALSE;
		return Boolean.TRUE;
	}

	private static Object Find1of(String s, Object[] args) {
		args = Args.massage(sFS, args);
		String set = Ops.toStr(args[0]);
		for (int i = 0; i < s.length(); ++i) {
			int j = set.indexOf(s.charAt(i));
			if (j != -1)
				return i;
		}
		return s.length();
	}

	private static Object Findnot1of(String s, Object[] args) {
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

	private static int asc(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return s.length() == 0 ? 0 : (int) s.charAt(0);
	}

	private static final FunctionSpec sFS = new FunctionSpec("s");

	private static boolean endsWith(String s, Object[] args) {
		args = Args.massage(sFS, args);
		return s.endsWith(toStr(args[0]));
	}

	private static Object compile(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return Compiler.compile("StringCompile", s);
	}

	static final Pattern globalRx = Pattern.compile("[A-Z][_a-zA-Z0-9][!?]?");

	private static Object eval(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		if (globalRx.matcher(s).matches())
			return Globals.get(s);
		Object result = Compiler.eval(s);
		return result == null ? "" : result;
	}

	private static Object find(String s, Object[] args) {
		args = Args.massage(sFS, args);
		int i = s.indexOf(toStr(args[0]));
		return i == -1 ? s.length() : i;
	}

	private static Object findLast(String s, Object[] args) {
		args = Args.massage(sFS, args);
		int i = s.lastIndexOf(toStr(args[0]));
		return i == -1 ? false : i;
	}

	private static final FunctionSpec repeatFS = new FunctionSpec("n");

	private static String repeat(String s, Object[] args) {
		args = Args.massage(repeatFS, args);
		int n = Math.max(0, Ops.toInt(args[0]));
		StringBuilder sb = new StringBuilder(n * s.length());
		for (int i = 0; i < n; ++i)
			sb.append(s);
		return sb.toString();
	}

	private static int size(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return s.length();
	}

	static SuContainer split(String s, Object... args) {
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

	private static boolean startsWith(String s, Object[] args) {
		args = Args.massage(sFS, args);
		return s.startsWith(toStr(args[0]));
	}

	private static final FunctionSpec substrFS =
			new FunctionSpec(array("i", "n"), Integer.MAX_VALUE);
	private static String substr(String s, Object[] args) {
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
	private static String tr(String s, Object[] args) {
		args = Args.massage(trFS, args);
		return Tr.tr(s, toStr(args[0]), toStr(args[1]));
	}

}
