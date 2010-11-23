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

import com.google.common.collect.ImmutableMap;

public class DateMethods extends SuClass implements Ops.Invoker {
	public static final DateMethods instance = new DateMethods();

	public DateMethods() {
		super("Date", null, members());
	}

	private static Object members() {
		ImmutableMap.Builder<String, SuMethod> b = ImmutableMap.builder();
		b.put("Year", new GetField(Calendar.YEAR));
		b.put("Month", new GetField(Calendar.MONTH, 1));
		b.put("Day", new GetField(Calendar.DAY_OF_MONTH));
		b.put("Hour", new GetField(Calendar.HOUR));
		b.put("Minute", new GetField(Calendar.MINUTE));
		b.put("Second", new GetField(Calendar.SECOND));
		b.put("Millisecond", new GetField(Calendar.MILLISECOND));

		b.put("FormatEn", new FormatEn());
		b.put("GMTime", new GMTime());
		b.put("GMTimeToLocal", new GMTimeToLocal());
		b.put("MinusDays", new MinusDays());
		b.put("MinusSeconds", new MinusSeconds());
		b.put("Plus", new Plus());
		b.put("WeekDay", new WeekDay());
		return b.build();
	}

	@Override
	protected void linkMethods() {
	}

	@Override
	protected Object notFound(Object self, String method, Object... args) {
		return ((DateClass) Globals.get("Date")).invoke(self, method, args);
	}

	private static class GetField extends BuiltinMethod0 {
		private final int field;
		private final int offset;
		GetField(int field) {
			this(field, 0);
		}
		GetField(int field, int offset) {
			this.field = field;
			this.offset = offset;
		}
		@Override
		public Object eval0(Object self) {
			Calendar c = Calendar.getInstance();
			c.setTime((Date) self);
			return c.get(field) + offset;
		}
	}

	private static class FormatEn extends BuiltinMethod1 {
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

	private static class GMTime extends BuiltinMethod0 {
		@Override
		public Object eval0(Object self) {
			Date d = (Date) self;
			int offset = TimeZone.getDefault().getOffset(d.getTime());
			return new Date(d.getTime() - offset);
		}
	}

	private static class GMTimeToLocal extends BuiltinMethod0 {
		@Override
		public Object eval0(Object self) {
			Date d = (Date) self;
			int offset = TimeZone.getDefault().getOffset(d.getTime());
			return new Date(d.getTime() + offset);
		}
	}

	private static class MinusDays extends BuiltinMethod1 {
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

	private static class MinusSeconds extends BuiltinMethod1 {
		{ params = new FunctionSpec("date"); }
		@Override
		public Object eval1(Object self, Object a) {
			Date d2 = (Date) a;
			long ms = ((Date) self).getTime() - d2.getTime();
			return BigDecimal.valueOf(ms, 3);
		}
	}

	private static class Plus extends SuMethod {
		static final Object nil = new Object();
		{ params = new FunctionSpec(
				array("arg", "years", "months", "days",
						"hours", "minutes", "seconds", "milliseconds"),
				nil, 0, 0, 0, 0, 0, 0, 0); }
		@Override
		public Object eval(Object self, Object... args) {
			args = Args.massage(params, args);
			if (args[0] != nil)
				throw new SuException("usage: date.Plus(years:, months:, days:, hours:, minutes:, seconds:, milliseconds:)");
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

	private static class WeekDay extends SuMethod {
		{ params = new FunctionSpec(array("firstDay"), "sun"); }
		@Override
		public Object eval(Object self, Object... args) {
			args = Args.massage(params, args);
			int i = (Ops.isString(args[0]))
					? dayNumber(Ops.toStr(args[0]).toLowerCase())
					: Ops.toInt(args[0]);
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
