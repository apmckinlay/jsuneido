/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.runtime.FunctionSpec.NA;

import java.math.BigDecimal;

import suneido.SuDate;
import suneido.SuException;
import suneido.runtime.BuiltinMethods;
import suneido.runtime.Ops;
import suneido.runtime.Params;
import suneido.runtime.SuCallable;

/**
 * Delegates all implementation to {@link SuDate}.
 * Should NOT import Date, Calendar, TimeZone etc.
 */
public final class DateMethods {
	private static final BuiltinMethods methods =
			new BuiltinMethods(DateMethods.class, "Dates");

	/** no instances, all static */
	private DateMethods() {
	}

	public static Object Year(Object self) {
		return ((SuDate) self).year();
	}

	public static Object Month(Object self) {
		return ((SuDate) self).month();
	}

	public static Object Day(Object self) {
		return ((SuDate) self).day();
	}

	public static Object Hour(Object self) {
		return ((SuDate) self).hour();
	}

	public static Object Minute(Object self) {
		return ((SuDate) self).minute();
	}

	public static Object Second(Object self) {
		return ((SuDate) self).second();
	}

	public static Object Millisecond(Object self) {
		return ((SuDate) self).millisecond();
	}

	@Params("format")
	public static Object FormatEn(Object self, Object a) {
		String format = Ops.toStr(a);
		return ((SuDate) self).format(format);
	}

	public static Object GetLocalGMTBias(Object self) {
		return -((SuDate) self).biasUTC();
	}

	@Params("date")
	public static Object MinusDays(Object self, Object a) {
		if (a instanceof SuDate)
			return ((SuDate) self).minusDays((SuDate) a);
		else
			throw new SuException("date.MinusDays requires date, got " +
					Ops.typeName(a));
	}

	@Params("date")
	public static Object MinusSeconds(Object self, Object a) {
		if (a instanceof SuDate) {
			long ms = ((SuDate) self).minusMilliseconds((SuDate) a);
			return BigDecimal.valueOf(ms, 3);
		} else
			throw new SuException("date.MinusSeconds requires date, got " +
					Ops.typeName(a));
	}

	@Params("arg=NA, years=0, months=0, days=0, " +
			"hours=0, minutes=0, seconds=0, milliseconds=0")
	public static Object Plus(Object self, Object... args) {
		if (args[0] != NA)
			throw new SuException("usage: date.Plus(years:, months:, days:, " +
					"hours:, minutes:, seconds:, milliseconds:)");
		return ((SuDate) self).plus(
			Ops.toInt(args[1]), Ops.toInt(args[2]), Ops.toInt(args[3]),
			Ops.toInt(args[4]), Ops.toInt(args[5]),	Ops.toInt(args[6]),
			Ops.toInt(args[7]));
	}

	@Params("firstDay=sun")
	public static Object WeekDay(Object self, Object a) {
		int i = (Ops.isString(a))
				? dayNumber(Ops.toStr(a).toLowerCase())
				: Ops.toInt(a);
		return (((SuDate) self).weekday() - i + 6) % 7;
	}

	private static final String[] weekday = { "sunday", "monday", "tuesday",
			"wednesday", "thursday", "friday", "saturday" };
	private static int dayNumber(String day) {
		for (int i = 0; i < weekday.length; ++i)
			if (weekday[i].startsWith(day))
				return i;
		throw new SuException("usage: date.WeekDay(firstDay = 'Sun')" + day);
	}

	public static SuCallable lookup(String method) {
		return methods.lookup(method);
	}

}
