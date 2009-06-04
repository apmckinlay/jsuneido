package suneido.language.builtin;

import static suneido.language.UserDefined.userDefined;

import java.math.BigDecimal;

import suneido.language.*;


public class NumberMethods {

	public static Object invoke(Integer n, String method, Object... args) {
		if (method == "Chr")
			return chr(n, args);
		if (method == "Int")
			return n;
		return invoke(BigDecimal.valueOf(n), method, args);
	}

	public static Object invoke(BigDecimal n, String method, Object... args) {
		if (method == "Chr")
			return chr(n.intValue(), args);
		if (method == "Int")
			return n.intValue();
		if (method == "Pow")
			return pow(n, args);
		return userDefined("Numbers", method).invoke(n, method, args);
	}

	private static String chr(Integer n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return Character.toString((char) (int) n);
	}

	private static final FunctionSpec intFS = new FunctionSpec("integer");
	private static Object pow(BigDecimal n, Object[] args) {
		args = Args.massage(intFS, args);
		return n.pow(Ops.toInt(args[0]), Ops.mc);
	}

}
