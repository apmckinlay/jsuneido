/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static suneido.language.FunctionSpec.NA;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import suneido.SuException;
import suneido.language.BuiltinMethods;
import suneido.language.Ops;
import suneido.language.Params;
import suneido.util.FAQCalendar;

/** used by {@link Ops} target */
public final class DateMethods extends BuiltinMethods {
	public static final DateMethods singleton = new DateMethods();

	private DateMethods() {
		super(DateMethods.class, "Dates");
	}

	public static Object Year(Object self) {
		return getField(self, Calendar.YEAR);
	}

	public static Object Month(Object self) {
		return getField(self, Calendar.MONTH) + 1;
	}

	public static Object Day(Object self) {
		return getField(self, Calendar.DAY_OF_MONTH);
	}

	public static Object Hour(Object self) {
		return getField(self, Calendar.HOUR_OF_DAY);
	}

	public static Object Minute(Object self) {
		return getField(self, Calendar.MINUTE);
	}

	public static Object Second(Object self) {
		return getField(self, Calendar.SECOND);
	}

	public static Object Millisecond(Object self) {
		return getField(self, Calendar.MILLISECOND);
	}

	private static int getField(Object self, int field) {
		Calendar c = Calendar.getInstance();
		c.setTime((Date) self);
		return c.get(field);
	}

	@Params("format")
	public static Object FormatEn(Object self, Object a) {
		String format = convertFormat(Ops.toStr(a));
		DateFormat df = new SimpleDateFormat(format);
		return df.format((Date) self);
	}

	private static String convertFormat(String fmt) {
		return fmt.replace("A", "a")
				.replace('t', 'a')
				.replace("dddd", "EEEE")
				.replace("ddd", "EEE")
				.replaceAll("[^adhHmMsyE]+", "'$0'");
	}

	public static Object GMTime(Object self) {
		Date d = (Date) self;
		int offset = TimeZone.getDefault().getOffset(d.getTime());
		return new Date(d.getTime() - offset);
	}

	public static Object GMTimeToLocal(Object self) {
		Date d = (Date) self;
		int offset = TimeZone.getDefault().getOffset(d.getTime());
		return new Date(d.getTime() + offset);
	}

	private static final int MILLISECONDS_PER_MINUTE = 60 * 1000;
	
	public static Object GetLocalGMTBias(Object self) {
		Date d = (Date) self;
		return -TimeZone.getDefault().getOffset(d.getTime())
				/ MILLISECONDS_PER_MINUTE;
	}

	@Params("date")
	public static Object MinusDays(Object self, Object a) {
		if (a instanceof Date)
			return (int) (day((Date) self) - day((Date) a));
		else
			throw new SuException("date.MinusDays requires date, got " +
					Ops.typeName(a));
	}

	private static long day(Date d) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		FAQCalendar c2 = new FAQCalendar(c.get(Calendar.YEAR),
				c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
		return c2.getUnixDay();
	}

	protected static final long MILLISECS_PER_DAY = 24 * 60 * 60 * 1000;

	@Params("date")
	public static Object MinusSeconds(Object self, Object a) {
		if (a instanceof Date) {
			Date d2 = (Date) a;
			long ms = ((Date) self).getTime() - d2.getTime();
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
		Calendar c = Calendar.getInstance();
		c.setTime((Date) self);
		c.add(Calendar.YEAR, Ops.toInt(args[1]));
		c.add(Calendar.MONTH, Ops.toInt(args[2]));
		c.add(Calendar.DAY_OF_MONTH, Ops.toInt(args[3]));
		c.add(Calendar.HOUR_OF_DAY, Ops.toInt(args[4]));
		c.add(Calendar.MINUTE, Ops.toInt(args[5]));
		c.add(Calendar.SECOND, Ops.toInt(args[6]));
		c.add(Calendar.MILLISECOND, Ops.toInt(args[7]));
		return c.getTime();
	}

	@Params("firstDay=sun")
	public static Object WeekDay(Object self, Object a) {
		int i = (Ops.isString(a))
				? dayNumber(Ops.toStr(a).toLowerCase())
				: Ops.toInt(a);
		Calendar c = Calendar.getInstance();
		c.setTime((Date) self);
		return (c.get(Calendar.DAY_OF_WEEK) - i + 6) % 7;
	}

	private static final String[] weekday = { "sunday", "monday", "tuesday",
			"wednesday", "thursday", "friday", "saturday" };
	private static int dayNumber(String day) {
		for (int i = 0; i < weekday.length; ++i)
			if (weekday[i].startsWith(day))
				return i;
		throw new SuException("usage: date.WeekDay(firstDay = 'Sun')" + day);
	}

}
