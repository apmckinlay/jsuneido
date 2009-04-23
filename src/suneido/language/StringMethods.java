package suneido.language;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static suneido.language.Ops.toInt;
import static suneido.language.Ops.toStr;
import static suneido.util.Util.array;
import suneido.SuException;

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
		args = Args.massage(FunctionSpec.noParams, args);
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
		i = max(0, min(i, len - 1));
		int n = args[1] == null ? len : toInt(args[1]);
		if (n < 0)
			n += len - i;
		n = max(0, min(n, len - i));
		return s.substring(i, i + n);
	}
}
