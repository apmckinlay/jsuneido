package suneido.language;

import static suneido.language.UserDefined.userDefined;

import java.math.BigDecimal;


public class NumberMethods {

	public static Object invoke(Integer n, String method, Object... args) {
		if (method == "Chr")
			return chr(n, args);
		return invoke(BigDecimal.valueOf(n), method, args);
	}

	public static Object invoke(BigDecimal n, String method, Object... args) {
		if (method == "Chr")
			return chr(n.intValue(), args);
		return userDefined("Numbers", method).invoke(n, method, args);
	}

	private static String chr(Integer n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return Character.toString((char) (int) n);
	}

}
