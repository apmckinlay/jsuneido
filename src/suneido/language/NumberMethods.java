package suneido.language;

import java.math.BigDecimal;

import suneido.SuException;

public class NumberMethods {
	public static Object invoke(Integer n, String method, Object... args) {
		if (method == "Chr")
			return chr(n, args);
		return invoke(BigDecimal.valueOf(n), method, args);
	}

	public static Object invoke(BigDecimal n, String method, Object... args) {
		if (method == "Chr")
			return chr(n.intValue(), args);
		// TODO check user defined Numbers
		throw new SuException("unknown method: number." + method);
	}

	private static String chr(Integer n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return Character.toString((char) (int) n);
	}

}
