package suneido.language.builtin;

import static suneido.language.Ops.inf;
import static suneido.language.Ops.minus_inf;
import static suneido.language.UserDefined.userDefined;

import java.math.BigDecimal;

import suneido.SuException;
import suneido.language.*;

public class NumberMethods {

	public static Object invoke(Integer n, String method, Object... args) {
		if (method == "Chr")
			return Chr(n, args);
		if (method == "Int")
			return n;
		if (method == "Hex")
			return Hex(n, args);
		return invoke(BigDecimal.valueOf(n), method, args);
	}

	public static Object invoke(BigDecimal n, String method, Object... args) {
		if (method == "ACos")
			return ACos(n, args);
		if (method == "ASin")
			return ASin(n, args);
		if (method == "ATan")
			return ATan(n, args);
		if (method == "Chr")
			return Chr(n.intValue(), args);
		if (method == "Cos")
			return Cos(n, args);
		if (method == "Exp")
			return Exp(n, args);
		if (method == "Format")
			return Format(n, args);
		if (method == "Frac")
			return Frac(n, args);
		if (method == "Int")
			return Int(n, args);
		if (method == "Hex")
			return Hex(n, args);
		if (method == "Log")
			return Log(n, args);
		if (method == "Log10")
			return Log10(n, args);
		if (method == "Pow")
			return Pow(n, args);
		if (method == "Sin")
			return Sin(n, args);
		if (method == "Sqrt")
			return Sqrt(n, args);
		if (method == "Tan")
			return Tan(n, args);
		return userDefined("Numbers", method).invoke(n, method, args);
	}

	static BigDecimal Frac(BigDecimal n, Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		BigDecimal i = new BigDecimal(n.toBigInteger());
		return n.subtract(i);
	}

	private static BigDecimal Int(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return n == inf || n == minus_inf ? n
				: new BigDecimal(n.toBigInteger());
	}

	private static String Hex(Integer n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return Integer.toHexString(n);
	}

	private static String Hex(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return Long.toHexString(n.longValue());
	}

	private static String Chr(Integer n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return Character.toString((char) (n & 0xff));
	}

	private static final FunctionSpec maskFS = new FunctionSpec("mask");

	static BigDecimal half = new BigDecimal(".5");

	static String Format(BigDecimal n, Object... args) {
		args = Args.massage(maskFS, args);
		String mask = Ops.toStr(args[0]);

		BigDecimal x = n.abs();

		int masksize = mask.length();
		String num = "";
		if (n.equals(BigDecimal.ZERO)) {
			int i = mask.indexOf('.');
			if (i != -1) {
				StringBuilder sb = new StringBuilder(8);
				for (++i; i < masksize && mask.charAt(i) == '#'; ++i)
					sb.append('0');
				num = sb.toString();
			}
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
	private static Object Pow(BigDecimal n, Object[] args) {
		args = Args.massage(intFS, args);
		return n.pow(Ops.toInt(args[0]), Ops.mc);
	}

	private static BigDecimal Cos(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return convert(Math.cos(d), "Cos");
	}

	private static BigDecimal Sin(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return convert(Math.sin(d), "Sin");
	}

	private static BigDecimal Tan(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return convert(Math.tan(d), "Tan");
	}

	private static BigDecimal ACos(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return convert(Math.acos(d), "ACos");
	}

	private static BigDecimal ASin(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return convert(Math.asin(d), "ASin");
	}

	private static BigDecimal ATan(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return convert(Math.atan(d), "ATan");
	}

	private static BigDecimal Exp(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return convert(Math.exp(d), "Exp");
	}

	private static BigDecimal Log(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return convert(Math.log(d), "Log");
	}

	private static BigDecimal Log10(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return convert(Math.log10(d), "Log10");
	}

	private static BigDecimal Sqrt(BigDecimal n, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		double d = n.doubleValue();
		return convert(Math.sqrt(d), "Sqrt");
	}

	private static BigDecimal convert(double d, String op) {
		if (Double.isInfinite(d))
			throw new SuException("infinite result");
		if (Double.isNaN(d))
			throw new SuException(op + ": not-a-number result");
		return new BigDecimal(d);
	}

}
