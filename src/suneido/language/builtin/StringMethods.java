package suneido.language.builtin;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static suneido.language.Ops.toInt;
import static suneido.language.Ops.toStr;
import static suneido.language.UserDefined.userDefined;
import static suneido.util.Util.array;

import java.util.regex.Pattern;

import suneido.SuContainer;
import suneido.SuException;
import suneido.language.*;
import suneido.language.Compiler;
import suneido.util.Tr;

public class StringMethods {
	public static Object invoke(String s, String method, Object... args) {
		char c = method.charAt(0);
		if (c < 'R') {
			if (method == "Asc")
				return asc(s, args);
			if (method == "EndsWith")
				return endsWith(s, args);
			if (method == "Eval")
				return eval(s, args);
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
			if (method == "Numeric?")
				return NumericQ(s, args);
			if (method == "Prefix?")
				return startsWith(s, args);
		} else {
			if (method == "Repeat")
				return repeat(s, args);
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
		}
		return userDefined("Strings", method).invoke(s, method, args);
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
		int n = Ops.toInt(args[0]);
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
