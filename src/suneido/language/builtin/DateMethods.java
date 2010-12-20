/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static suneido.util.Util.array;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import suneido.SuException;
import suneido.language.*;
import suneido.util.FAQCalendar;

/** used by {@link Ops} target */
public class DateMethods extends BuiltinMethods {
	public static final DateMethods singleton = new DateMethods();

	private DateMethods() {
		super(DateMethods.class, "Dates");
	}

	public static class Year extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return getField(self, Calendar.YEAR);
		}
	}

	public static class Month extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return getField(self, Calendar.MONTH) + 1;
		}
	}

	public static class Day extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return getField(self, Calendar.DAY_OF_MONTH);
		}
	}

	public static class Hour extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return getField(self, Calendar.HOUR_OF_DAY);
		}
	}

	public static class Minute extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return getField(self, Calendar.MINUTE);
		}
	}

	public static class Second extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return getField(self, Calendar.SECOND);
		}
	}

	public static class Millisecond extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return getField(self, Calendar.MILLISECOND);
		}
	}

	private static int getField(Object self, int field) {
		Calendar c = Calendar.getInstance();
		c.setTime((Date) self);
		return c.get(field);
	}

	public static class FormatEn extends SuMethod1 {
		{ params = new FunctionSpec("format"); }
		@Override
		public Object eval1(Object self, Object a) {
			String format = convertFormat(Ops.toStr(a));
			DateFormat df = new SimpleDateFormat(format);
			return df.format((Date) self);
		}
	}

	private static String convertFormat(String fmt) {
		return fmt.replace("A", "a")
				.replace('t', 'a')
				.replace("dddd", "EEEE")
				.replace("ddd", "EEE")
				.replaceAll("[^adhHmMsyE]+", "'$0'");
	}

	public static class GMTime extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			Date d = (Date) self;
			int offset = TimeZone.getDefault().getOffset(d.getTime());
			return new Date(d.getTime() - offset);
		}
	}

	public static class GMTimeToLocal extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			Date d = (Date) self;
			int offset = TimeZone.getDefault().getOffset(d.getTime());
			return new Date(d.getTime() + offset);
		}
	}

	public static class MinusDays extends SuMethod1 {
		{ params = new FunctionSpec("date"); }
		@Override
		public Object eval1(Object self, Object a) {
			return (int) (day((Date) self) - day((Date) a));
		}
	}

	private static long day(Date d) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		FAQCalendar c2 = new FAQCalendar(c.get(Calendar.YEAR),
				c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
		return c2.getUnixDay();
	}

	protected static final long MILLISECS_PER_DAY = 24 * 60 * 60 * 1000;

	public static class MinusSeconds extends SuMethod1 {
		{ params = new FunctionSpec("date"); }
		@Override
		public Object eval1(Object self, Object a) {
			Date d2 = (Date) a;
			long ms = ((Date) self).getTime() - d2.getTime();
			return BigDecimal.valueOf(ms, 3);
		}
	}

	public static class Plus extends SuMethod {
		static final Object nil = new Object();
		{ params = new FunctionSpec(
				array("arg", "years", "months", "days",
						"hours", "minutes", "seconds", "milliseconds"),
				nil, 0, 0, 0, 0, 0, 0, 0); }
		@Override
		public Object eval(Object self, Object... args) {
			args = Args.massage(params, args);
			if (args[0] != nil)
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
	}

	public static class WeekDay extends SuMethod1 {
		{ params = new FunctionSpec(array("firstDay"), "sun"); }
		@Override
		public Object eval1(Object self, Object a) {
			int i = (Ops.isString(a))
					? dayNumber(Ops.toStr(a).toLowerCase())
					: Ops.toInt(a);
			Calendar c = Calendar.getInstance();
			c.setTime((Date) self);
			return (c.get(Calendar.DAY_OF_WEEK) - i + 6) % 7;
		}
	}

	private final static String[] weekday = { "sunday", "monday", "tuesday",
			"wednesday", "thursday", "friday", "saturday" };
	private static int dayNumber(String day) {
		for (int i = 0; i < weekday.length; ++i)
			if (weekday[i].startsWith(day))
				return i;
		throw new SuException("usage: date.WeekDay(firstDay = 'Sun')" + day);
	}

}
