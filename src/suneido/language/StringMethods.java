package suneido.language;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static suneido.language.Ops.toInt;
import suneido.SuException;

public class StringMethods {
	public static Object invoke(String s, String method, Object... args) {
		if (method == "Size")
			return size(s, args);
		else if (method == "Substr")
			return substr(s, args);
		// TODO check user defined Strings
		else
			throw new SuException("unknown method: string." + method);
	}

	private static int size(String s, Object[] args) {
		return s.length();
	}

	private static String substr(String s, Object[] args) {
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
