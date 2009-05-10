package suneido.language.builtin;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static suneido.language.Ops.toInt;
import static suneido.language.Ops.toStr;
import static suneido.util.Util.array;

import java.util.regex.Pattern;

import suneido.SuException;
import suneido.language.*;
import suneido.language.Compiler;
import suneido.util.Tr;

public class StringMethods {
	public static Object invoke(String s, String method, Object... args) {
		if (method == "Find")
			return find(s, args);
		if (method == "FindLast")
			return findLast(s, args);
		if (method == "Size")
			return size(s, args);
		if (method == "Substr")
			return substr(s, args);
		if (method == "Asc")
			return asc(s, args);
		if (method == "Tr")
			return tr(s, args);
		if (method == "Eval")
			return eval(s, args);
		// TODO check user defined Strings
		throw new SuException("unknown method: string." + method);
	}

	private static final FunctionSpec findFS = new FunctionSpec("s");
	private static Object find(String s, Object[] args) {
		args = Args.massage(findFS, args);
		int i = s.indexOf(toStr(args[0]));
		return i == -1 ? s.length() : i;
	}

	private static Object findLast(String s, Object[] args) {
		args = Args.massage(findFS, args);
		int i = s.lastIndexOf(toStr(args[0]));
		return i == -1 ? false : i;
	}

	private static int size(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return s.length();
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

	private static int asc(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return s.length() == 0 ? 0 : (int) s.charAt(0);
	}

	private static final FunctionSpec trFS =
			new FunctionSpec(array("from", "to"), "");
	private static String tr(String s, Object[] args) {
		args = Args.massage(trFS, args);
		return Tr.tr(s, toStr(args[0]), toStr(args[1]));
	}

	static final Pattern globalRx = Pattern.compile("[A-Z][_a-zA-Z0-9][!?]?");
	private static Object eval(String s, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		if (globalRx.matcher(s).matches())
			return Globals.get(s);
		Object result = Compiler.eval(s);
		return result == null ? "" : result;
	}

}
