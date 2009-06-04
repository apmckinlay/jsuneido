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
		if (method == "ACos")
			return acos(n, args);
		if (method == "ASin")
			return asin(n, args);
		if (method == "ATan")
			return atan(n, args);
		if (method == "Chr")
			return chr(n.intValue(), args);
		if (method == "Cos")
			return cos(n, args);
		if (method == "Exp")
			return exp(n, args);
		if (method == "Format")
			return format(n, args);
		if (method == "Frac")
			return frac(n, args);
		if (method == "Int")
			return Int(n, args);
		if (method == "Log")
			return log(n, args);
		if (method == "Log10")
			return log10(n, args);
		if (method == "Pow")
			return pow(n, args);
		if (method == "Sin")
			return sin(n, args);
		if (method == "Sqrt")
			return sqrt(n, args);
		if (method == "Tan")
			return tan(n, args);
		return userDefined("Numbers", method).invoke(n, method, args);
	}

	static BigDecimal frac(BigDecimal n, Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		BigDecimal i = new BigDecimal(n.toBigInteger());
		return n.subtract(i);
	}

	private static BigDecimal Int(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return new BigDecimal(n.toBigInteger());
	}

	private static String chr(Integer n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return Character.toString((char) (int) n);
	}

	private static final FunctionSpec maskFS = new FunctionSpec("mask");

	static BigDecimal half = new BigDecimal(".5");

	static String format(BigDecimal n, Object... args) {
		args = Args.massage(maskFS, args);
		String mask = Ops.toStr(args[0]);

		BigDecimal x = n.abs();

		int masksize = mask.length();
		String num = "";
		if (n.equals(BigDecimal.ZERO)) {
			int i = mask.indexOf('.');
			if (i != -1)
				for (++i; i < masksize && mask.charAt(i) == '#'; ++i)
					num += '0';
		} else {
			int i = mask.indexOf('.');
			if (i != -1)
				for (++i; i < masksize && mask.charAt(i) == '#'; ++i)
					x = x.movePointRight(1);
			BigDecimal tmp = new BigDecimal(x.toBigInteger());
			if (x != tmp) { // need to round
				x = x.add(half);
				x = new BigDecimal(x.toBigInteger());
			}
			num = Ops.toStringBD(x);
		}
		StringBuilder dst = new StringBuilder();
		int sign = n.signum();
		boolean signok = (sign >= 0);
		int i, j;
		for (j = num.length() - 1, i = masksize - 1; i >= 0; --i) {
			char c = mask.charAt(i);
			switch (c) {
			case '#':
				dst.append(j >= 0 ? num.charAt(j--) : '0');
				break;
			case ',':
				if (j >= 0)
					dst.append(',');
				break;
			case '-':
			case '(':
				signok = true;
				if (sign < 0)
					dst.append(c);
				break;
			case ')':
				dst.append(sign < 0 ? c : ' ');
				break;
			case '.':
			default:
				dst.append(c);
				break;
			}
		}
		dst.reverse();

		// strip leading zeros
		int start = 0;
		while (dst.charAt(start) == '-' || dst.charAt(start) == '(')
			++start;
		int end = start;
		while (dst.charAt(end) == '0' && end + 1 < dst.length())
			++end;
		dst.delete(start, end);

		if (j >= 0)
			return "#"; // too many digits for mask
		if (!signok)
			return "-"; // negative not handled by mask
		return dst.toString();
	}

	private static final FunctionSpec intFS = new FunctionSpec("integer");
	private static Object pow(BigDecimal n, Object[] args) {
		args = Args.massage(intFS, args);
		return n.pow(Ops.toInt(args[0]), Ops.mc);
	}

	private static BigDecimal cos(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return new BigDecimal(Math.cos(d));
	}

	private static BigDecimal sin(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return new BigDecimal(Math.sin(d));
	}

	private static BigDecimal tan(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return new BigDecimal(Math.tan(d));
	}

	private static BigDecimal acos(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return new BigDecimal(Math.acos(d));
	}

	private static BigDecimal asin(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return new BigDecimal(Math.asin(d));
	}

	private static BigDecimal atan(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return new BigDecimal(Math.atan(d));
	}

	private static BigDecimal exp(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return new BigDecimal(Math.exp(d));
	}

	private static BigDecimal log(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return new BigDecimal(Math.log(d));
	}

	private static BigDecimal log10(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return new BigDecimal(Math.log10(d));
	}

	private static BigDecimal sqrt(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return new BigDecimal(Math.sqrt(d));
	}

}
