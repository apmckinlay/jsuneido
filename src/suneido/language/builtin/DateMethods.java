package suneido.language.builtin;

import static suneido.util.Util.array;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import suneido.SuException;
import suneido.language.*;
import suneido.util.FAQCalendar;

public class DateMethods {

	public static Object invoke(Date d, String method, Object... args) {
		if (method == "Day")
			return Day(d, args);
		if (method == "FormatEn")
			return FormatEn(d, args);
		if (method == "GMTime")
			return GMTime(d, args);
		if (method == "GMTimeToLocal")
			return GMTimeToLocal(d, args);
		if (method == "Hour")
			return Hour(d, args);
		if (method == "Millisecond")
			return Millisecond(d, args);
		if (method == "MinusDays")
			return MinusDays(d, args);
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
		if (method == "WeekDay")
			return WeekDay(d, args);
		if (method == "Year")
			return Year(d, args);
		return ((DateClass) Globals.get("Date")).invoke(d, method, args);
	}

	private static int Day(Date d, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.DAY_OF_MONTH);
	}

	private static final FunctionSpec formatFS = new FunctionSpec("format");

	private static Object FormatEn(Date d, Object[] args) {
		args = Args.massage(formatFS, args);
		String format = convertFormat(Ops.toStr(args[0]));
		DateFormat df = new SimpleDateFormat(format);
		return df.format(d);
	}

	private static String convertFormat(String fmt) {
		return fmt.replace("A", "a")
				.replace('t', 'a')
				.replace("dddd", "EEEE")
				.replace("ddd", "EEE")
				.replaceAll("[^adhHmMsyE]+", "'$0'");
	}

	private static Object GMTime(Date d, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		int offset = TimeZone.getDefault().getOffset(d.getTime());
		return new Date(d.getTime() - offset);
	}

	private static Object GMTimeToLocal(Date d, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		int offset = TimeZone.getDefault().getOffset(d.getTime());
		return new Date(d.getTime() + offset);
	}

	private static int Hour(Date d, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.HOUR_OF_DAY);
	}

	private static int Millisecond(Date d, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.MILLISECOND);
	}

	private static final FunctionSpec dateFS = new FunctionSpec("date");

	private static int MinusDays(Date d, Object[] args) {
		args = Args.massage(dateFS, args);
		Date d2 = (Date) args[0];
		return (int) (day(d) - day(d2));
	}

	private static long day(Date d) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		FAQCalendar c2 = new FAQCalendar(c.get(Calendar.YEAR),
				c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
		return c2.getUnixDay();
	}

	protected static final long MILLISECS_PER_DAY = 24 * 60 * 60 * 1000;

	private static Object MinusSeconds(Date d, Object[] args) {
		args = Args.massage(dateFS, args);
		Date d2 = (Date) args[0];
		long ms = d.getTime() - d2.getTime();
		return BigDecimal.valueOf(ms, 3);
	}

	private static int Minute(Date d, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.MINUTE);
	}

	private static int Month(Date d, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.MONTH) + 1;
	}

	private static final Object nil = new Object();
	private static final FunctionSpec plusFS = new FunctionSpec(
		array("arg", "years", "months", "days", "hours", "minutes", "seconds", "milliseconds"),
		nil, 0, 0, 0, 0, 0, 0, 0);

	private static Date Plus(Date d, Object[] args) {
		args = Args.massage(plusFS, args);
		if (args[0] != nil)
			throw new SuException("usage: date.Plus(years:, months:, days:, hours:, minutes:, seconds:, milliseconds:)");
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		c.add(Calendar.YEAR, Ops.toInt(args[1]));
		c.add(Calendar.MONTH, Ops.toInt(args[2]));
		c.add(Calendar.DAY_OF_MONTH, Ops.toInt(args[3]));
		c.add(Calendar.HOUR_OF_DAY, Ops.toInt(args[4]));
		c.add(Calendar.MINUTE, Ops.toInt(args[5]));
		c.add(Calendar.SECOND, Ops.toInt(args[6]));
		c.add(Calendar.MILLISECOND, Ops.toInt(args[7]));
		return c.getTime();
	}

	private static int Second(Date d, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.SECOND);
	}

	private static final FunctionSpec weekdayFS =
			new FunctionSpec(array("firstDay"), "sun");

	private static Object WeekDay(Date d, Object[] args) {
		args = Args.massage(weekdayFS, args);
		int i = (Ops.isString(args[0]))
				? dayNumber(Ops.toStr(args[0]).toLowerCase())
				: Ops.toInt(args[0]);
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return (c.get(Calendar.DAY_OF_WEEK) - i + 6) % 7;
	}

	private final static String[] weekday = { "sunday", "monday", "tuesday",
			"wednesday", "thursday", "friday", "saturday" };
	private static int dayNumber(String day) {
		for (int i = 0; i < weekday.length; ++i)
			if (weekday[i].startsWith(day))
				return i;
		throw new SuException("usage: date.WeekDay(firstDay = 'Sun')" + day);
	}

	private static int Year(Date d, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.YEAR);
	}

}
