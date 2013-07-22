/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static suneido.language.FunctionSpec.NA;
import static suneido.util.Util.array;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import suneido.SuException;
import suneido.language.Args;
import suneido.language.BuiltinClass;
import suneido.language.FunctionSpec;
import suneido.language.Ops;
import suneido.util.DateParse;

public class DateClass extends BuiltinClass {
	public static final DateClass singleton = new DateClass();

	private DateClass() {
		super(DateClass.class, "Dates");
	}

	private static final FunctionSpec dateFS =
			new FunctionSpec(array("string", "pattern",
					"year", "month", "day",
					"hour",	"minute", "second", "millisecond"),
					NA, NA, NA, NA, NA, NA, NA, NA, NA);

	@Override
	public Object newInstance(Object... args) {
		args = Args.massage(dateFS, args);
		if (args[0] != NA && hasFields(args))
			throw new SuException(
					"usage: Date() or Date(string [, pattern]) or "
							+ "Date(year:, month:, day:, hour:, minute:, second:)");
		if (args[0] != NA) {
			if (args[0] instanceof Date)
				return args[0];
			Date d;
			if (args[1] == NA)
				d = DateParse.parse(Ops.toStr(args[0]));
			else
				d = DateParse.parse(Ops.toStr(args[0]), Ops.toStr(args[1]));
			return d == null ? false : d;
		} else if (hasFields(args)) {
			return named(args);
		} else
			return new Date();
	}
	private static boolean hasFields(Object[] args) {
		for (int i = 2; i <= 8; ++i)
			if (args[i] != NA)
				return true;
		return false;
	}
	private static Date named(Object[] args) {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		if (args[2] != NA)
			c.set(Calendar.YEAR, Ops.toInt(args[2]));
		if (args[3] != NA)
			c.set(Calendar.MONTH, Ops.toInt(args[3]) - 1);
		if (args[4] != NA)
			c.set(Calendar.DAY_OF_MONTH, Ops.toInt(args[4]));
		if (args[5] != NA)
			c.set(Calendar.HOUR_OF_DAY, Ops.toInt(args[5]));
		if (args[6] != NA)
			c.set(Calendar.MINUTE, Ops.toInt(args[6]));
		if (args[7] != NA)
			c.set(Calendar.SECOND, Ops.toInt(args[7]));
		if (args[8] != NA)
			c.set(Calendar.MILLISECOND, Ops.toInt(args[8]));
		return c.getTime();
	}

	public static Object GetLocalGMTBias(Object self) {
		int offset = TimeZone.getDefault().getOffset(new Date().getTime());
		return -offset / 60000; // convert from ms to minutes
	}

}
