/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.runtime.FunctionSpec.NA;
import static suneido.util.Util.array;
import suneido.SuDate;
import suneido.SuException;
import suneido.runtime.Args;
import suneido.runtime.BuiltinClass;
import suneido.runtime.FunctionSpec;
import suneido.runtime.Ops;

public class DateClass extends BuiltinClass {
	public static final DateClass singleton = new DateClass();

	private DateClass() {
		super("Date", DateClass.class, "Dates", dateFS);
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
			if (args[0] instanceof SuDate)
				return (SuDate) args[0];
			SuDate d;
			if (args[1] == NA)
				d = SuDate.parse(Ops.toStr(args[0]));
			else
				d = SuDate.parse(Ops.toStr(args[0]), Ops.toStr(args[1]));
			return d == null ? false : d;
		} else if (hasFields(args)) {
			return named(args);
		} else
			return SuDate.now();
	}
	private static boolean hasFields(Object[] args) {
		for (int i = 2; i <= 8; ++i)
			if (args[i] != NA)
				return true;
		return false;
	}
	private static SuDate named(Object[] args) {
		SuDate now = SuDate.now();
		int year = now.year();
		int month = now.month();
		int day = now.day();
		int hour = now.hour();
		int minute = now.minute();
		int second = now.second();
		int millisecond = now.millisecond();
		if (args[2] != NA)
			year = Ops.toInt(args[2]);
		if (args[3] != NA)
			month = Ops.toInt(args[3]);
		if (args[4] != NA)
			day = Ops.toInt(args[4]);
		if (args[5] != NA)
			hour = Ops.toInt(args[5]);
		if (args[6] != NA)
			minute = Ops.toInt(args[6]);
		if (args[7] != NA)
			second = Ops.toInt(args[7]);
		if (args[8] != NA)
			millisecond = Ops.toInt(args[8]);
		return SuDate.normalized(year, month, day, hour, minute, second, millisecond);
	}

}
