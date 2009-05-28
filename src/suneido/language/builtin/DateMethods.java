package suneido.language.builtin;

import static suneido.language.UserDefined.userDefined;

import java.math.BigDecimal;
import java.util.Date;

import suneido.language.Args;
import suneido.language.FunctionSpec;

public class DateMethods {

	public static Object invoke(Date d, String method, Object... args) {
		if (method == "MinusSeconds")
			return minusSeconds(d, args);
		return userDefined("Dates", method).invoke(d, method, args);
	}

	private static final FunctionSpec dateFS = new FunctionSpec("date");
	private static Object minusSeconds(Date d, Object[] args) {
		args = Args.massage(dateFS, args);
		Date d2 = (Date) args[0];
		long ms = d.getTime() - d2.getTime();
		return BigDecimal.valueOf(ms, 3);
	}

}
