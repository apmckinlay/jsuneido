package suneido.language.builtin;

import static suneido.util.Util.array;

import java.math.BigDecimal;
import java.util.*;

import suneido.language.*;

public class DateMethods {

	public static Object invoke(Date d, String method, Object... args) {
		if (method == "Day")
			return Day(d, args);
		if (method == "GMTime")
			return GMTime(d, args);
		if (method == "GMTimeToLocal")
			return GMTimeToLocal(d, args);
		if (method == "Hour")
			return Hour(d, args);
		if (method == "Millisecond")
			return Millisecond(d, args);
		if (method == "MinusSeconds")
			return MinusSeconds(d, args);
		if (method == "Minute")
			return Minute(d, args);
		if (method == "Month")
			return Month(d, args);
		if (method == "Plus")
			return Plus(d, args);
		if (method == "Second")
			return Second(d, args);
		if (method == "Year")
			return Year(d, args);
		return ((DateClass) Globals.get("Date")).invoke(d, method, args);
	}

	private static Object GMTimeToLocal(Date d, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		int offset = TimeZone.getDefault().getOffset(d.getTime());
		return new Date(d.getTime() + offset);
	}

	private static Object GMTime(Date d, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		int offset = TimeZone.getDefault().getOffset(d.getTime());
		return new Date(d.getTime() - offset);
	}

	private static int Year(Date d, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.YEAR);
	}

	private static int Month(Date d, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.MONTH) + 1;
	}

	private static int Day(Date d, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.DAY_OF_MONTH);
	}

	private static int Hour(Date d, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.HOUR_OF_DAY);
	}

	private static int Minute(Date d, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.MINUTE);
	}

	private static int Second(Date d, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.SECOND);
	}

	private static int Millisecond(Date d, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.MILLISECOND);
	}

	private static final FunctionSpec dateFS = new FunctionSpec("date");
	private static Object MinusSeconds(Date d, Object[] args) {
		args = Args.massage(dateFS, args);
		Date d2 = (Date) args[0];
		long ms = d.getTime() - d2.getTime();
		return BigDecimal.valueOf(ms, 3);
	}

	private static final FunctionSpec plusFS = new FunctionSpec(
		array("years", "months", "days", "hours", "minutes", "seconds", "milliseconds"),
		0, 0, 0, 0, 0, 0, 0);

	private static Date Plus(Date d, Object[] args) {
		args = Args.massage(plusFS, args);
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		c.add(Calendar.YEAR, Ops.toInt(args[0]));
		c.add(Calendar.MONTH, Ops.toInt(args[1]));
		c.add(Calendar.DAY_OF_MONTH, Ops.toInt(args[2]));
		c.add(Calendar.HOUR_OF_DAY, Ops.toInt(args[3]));
		c.add(Calendar.MINUTE, Ops.toInt(args[4]));
		c.add(Calendar.YEAR, Ops.toInt(args[5]));
		c.add(Calendar.YEAR, Ops.toInt(args[6]));
		return c.getTime();
	}

}
