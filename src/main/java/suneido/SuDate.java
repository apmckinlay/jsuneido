/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static suneido.SuDate.Field.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;

import javax.annotation.concurrent.Immutable;

import suneido.language.Pack;
import suneido.language.builtin.DateMethods;
import suneido.util.FAQCalendar;

import com.google.common.primitives.Ints;

/**
 * A date class that matches the cSuneido implementation.
 * <p>
 * Represents a readable "local" date and time.
 * Does not take into account time zones or daylight savings.
 */
@Immutable
public class SuDate extends SuValue implements Comparable<SuDate> {
	private final int date;
	private final int time;

	public SuDate(int date, int time) {
		this.date = date;
		this.time = time;
		if (! valid(year(), month(), day(),
				hour(), minute(), second(), millisecond()))
			throw new SuDateBad();
	}

	public SuDate(int year, int month, int day,
			int hour, int minute, int second, int millisecond) {
		if (! valid(year, month, day, hour, minute, second, millisecond))
			throw new SuDateBad();
		date = (year << 9) | (month << 5) | day;
		time = (hour << 22) | (minute << 16) | (second << 10) | millisecond;
	}

	public static SuDate normalized(int year, int month, int day,
			int hour, int minute, int second, int millisecond) {
		return normalize(year, month, day, hour, minute, second, millisecond);
	}

	/** @return An SuDate for the current local date & time */
	public static SuDate now() {
		Calendar cal = Calendar.getInstance();
		return fromCalendar(cal);
	}

	public static SuDate fromLiteral(String s) {
		if (s.startsWith("#"))
			s = s.substring(1);
		int datelen = s.indexOf('.');
		int timelen = 0;
		if (datelen == -1)
			datelen = s.length();
		else
			timelen = s.length() - datelen - 1;
		if (datelen != 8 ||
				(timelen != 0 && timelen != 4 && timelen != 6 && timelen != 9))
			return null;

		int year = nsub(s, 0, 4);
		int month = nsub(s, 4, 6);
		int day = nsub(s, 6, 8);

		int hour = nsub(s, 9, 11);
		int minute = nsub(s, 11, 13);
		int second = nsub(s, 13, 15);
		int millisecond = nsub(s, 15, 18);

		return new SuDate(year, month, day, hour, minute, second, millisecond);
	}

	private static int nsub(String s, int from, int to) {
		if (to > s.length())
			return 0;
		return Integer.parseInt(s.substring(from, to));
	}

	public static SuDate fromTime(long d) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(d);
		return fromCalendar(cal);
	}

	private static SuDate fromCalendar(Calendar cal) {
		return new SuDate(
				cal.get(Calendar.YEAR),
				cal.get(Calendar.MONTH) + 1,
				cal.get(Calendar.DAY_OF_MONTH),
				cal.get(Calendar.HOUR_OF_DAY),
				cal.get(Calendar.MINUTE),
				cal.get(Calendar.SECOND),
				cal.get(Calendar.MILLISECOND));
	}

	@Override
	public String toString() {
		if (time == 0)
			return String.format("#%04d%02d%02d", year(), month(), day());
		String s = String.format("#%04d%02d%02d.%02d%02d%02d%03d",
				year(), month(), day(),
				hour(), minute(), second(), millisecond());
		if (s.endsWith("00000"))
			return s.substring(0, 14);
		if (s.endsWith("000"))
			return s.substring(0, 16);
		return s;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if (! (other instanceof SuDate))
			return false;
		SuDate that = (SuDate) other;
		return this.date == that.date && this.time == that.time;
	}

	@Override
	public int hashCode() {
		int h = 17;
		h = 31 * h + date;
		h = 31 * h + time;
		return h;
	}

	@Override
	public int compareTo(SuDate that) {
		int cmp = Ints.compare(this.date, that.date);
		return cmp != 0 ? cmp : Ints.compare(this.time, that.time);
	}

	@Override
	public String typeName() {
		return "Date";
	}

	@Override
	public SuValue lookup(String method) {
		return DateMethods.lookup(method);
	}

	// validation

	private static boolean valid(int year, int month, int day,
			int hour, int minute, int second, int millisecond) {
		if (year == YEAR.max && (month != 1 || day != 1 || 
				hour != 0 || minute != 0 || second != 0 || millisecond != 0))
			return false;
		if (! YEAR.valid(year) || ! MONTH.valid(month) || ! DAY.valid(day) ||
				! HOUR.valid(hour) || ! MINUTE.valid(minute) ||
				! SECOND.valid(second) || ! MILLISECOND.valid(millisecond))
			return false;

		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year, month - 1, day);
		return cal.get(Calendar.YEAR) == year &&
				cal.get(Calendar.MONTH) == month - 1 &&
				cal.get(Calendar.DAY_OF_MONTH) == day;
	}

	public static class SuDateBad extends SuException {
		private static final long serialVersionUID = 1L;

		SuDateBad() {
			super("bad date");
		}
	}

	// Packable

	@Override
	public int packSize(int nest) {
		return 9;
	}

	@Override
	public void pack(ByteBuffer buf) {
		buf.put(Pack.Tag.DATE);
		buf.putInt(date);
		buf.putInt(time);
	}

	public static SuDate unpack(ByteBuffer buf) {
		int date = buf.getInt();
		int time = buf.getInt();
		return new SuDate(date, time);
	}

	private static final int MILLISECONDS_PER_MINUTE = 60 * 1000;

	/** return offset from local to UTC in minutes */
	public int biasUTC() {
		Calendar cal = toCalendar();
		return (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET))
				/ MILLISECONDS_PER_MINUTE;
	}

	// getters

	public int year() {
		return date >> 9;
	}

	public int month() {
		return (date >> 5) & 0xf;
	}

	public int day() {
		return date & 0x1f;
	}

	public int hour() {
		return time >> 22;
	}

	public int minute() {
		return (time >> 16) & 0x3f;
	}

	public int second() {
		return (time >> 10) & 0x3f;
	}

	public int millisecond() {
		return time & 0x3ff;
	}

	public SuDate plus(int year, int month, int day,
			int hour, int minute, int second, int millisecond) {
		year += year();
		month += month();
		day += day();
		hour += hour();
		minute += minute();
		second += second();
		millisecond += millisecond();
		return normalize(year, month, day, hour, minute, second, millisecond);
	}

	private static SuDate normalize(int year, int month, int day, int hour,
			int minute, int second, int millisecond) {
		// adjust to bring back into range
		while (millisecond < 0) {
			--second;
			millisecond += 1000;
		}
		while (millisecond >= 1000) {
			++second;
			millisecond -= 1000;
		}

		while (second < 0) {
			--minute;
			second += 60;
		}
		while (second >= 60) {
			++minute;
			second -= 60;
		}

		while (minute < 0) {
			--hour;
			minute += 60;
		}
		while (minute >= 60) {
			++hour;
			minute -= 60;
		}

		while (hour < 0) {
			--day;
			hour += 24;
		}
		while (hour >= 24) {
			++day;
			hour -= 24;
		}

		// use Calendar for days to handle leap years etc.
		if (day < 1 || 28 < day) {
			Calendar cal = Calendar.getInstance();
			cal.clear();
			cal.set(year, month - 1, day);
			year = cal.get(Calendar.YEAR);
			month =	cal.get(Calendar.MONTH) + 1;
			day = cal.get(Calendar.DAY_OF_MONTH);
		}

		while (month < 1) {
			--year;
			month += 12;
		}
		while (month > 12) {
			++year;
			month -= 12;
		}

		return new SuDate(year, month, day, hour, minute, second, millisecond);
	}

	public int weekday() {
		return calendar().get(Calendar.DAY_OF_WEEK); // Sun is 1, Sat is 7
	}

	private Calendar calendar() {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year(), month() - 1, day());
		return cal;
	}

	public int minusDays(SuDate other) {
		return (int) (unixday() - other.unixday());
	}

	private long unixday() {
		FAQCalendar c = new FAQCalendar(year(), month() - 1, day());
		return c.getUnixDay();
	}

	// WARNING: doing this around daylight savings changes may be problematic
	public long minusMilliseconds(SuDate other) {
		if (date == other.date) // avoid calendar if possible
			return timeAsMs() - other.timeAsMs();
		else
			return time() - other.time();
	}

	private long timeAsMs() {
		return millisecond() + 1000 * (second() + 60 * (minute() + 60L * hour()));
	}

	private long time() {
		return toCalendar().getTimeInMillis();
	}

	private Calendar toCalendar() {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year(), month() - 1, day(), hour(), minute(), second());
		cal.set(Calendar.MILLISECOND, millisecond());
		return cal;
	}

	// format

	private static final String months[] =
		{ "January", "February", "March", "April", "May", "June", "July",
		"August", "September", "October", "November", "December" };
	private static final String days[] =
		{ "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
		"Saturday" };

	public String format(String fmt) {
		int fmtlen = fmt.length();
		StringBuilder dst = new StringBuilder(fmtlen);
		for (int i = 0; i < fmtlen; ++i) {
			int n = 1;
			if (Character.isLetter(fmt.charAt(i)))
				for (char c = fmt.charAt(i);
						i + 1 < fmtlen && fmt.charAt(i + 1) == c; ++i)
					++n;
			switch (fmt.charAt(i)) {
			case 'y' :
				int yr = year();
				if (n >= 4)
					append(dst, yr / 1000);
				if (n >= 3)
					append(dst, (yr % 1000) / 100);
				if (n >= 2 || (yr % 100) > 9)
					append(dst, (yr % 100) / 10);
				append(dst, yr % 10);
				break ;
			case 'M' :
				if (n > 3)
					dst.append(months[month() - 1]);
				else if (n == 3)
					dst.append(months[month() - 1].substring(0, 3));
				else {
					if (n >= 2 || month() > 9)
						append(dst, month() / 10);
					append(dst, month() % 10);
				}
				break ;
			case 'd' :
				if (n > 3)
					dst.append(days[weekday() - 1]);
				else if (n == 3)
					dst.append(days[weekday() - 1].substring(0, 3));
				else {
					if (n >= 2 || day() > 9)
						append(dst, day() / 10);
					append(dst, day() % 10);
				}
				break ;
			case 'h' : // 12 hour
				int hr = hour() % 12;
				if (hr == 0)
					hr = 12;
				if (n >= 2 || hr > 9)
					append(dst, hr / 10);
				append(dst, hr % 10);
				break ;
			case 'H' : // 24 hour
				if (n >= 2 || hour() > 9)
					append(dst, hour() / 10);
				append(dst, hour() % 10);
				break ;
			case 'm' :
				if (n >= 2 || minute() > 9)
					append(dst, minute() / 10);
				append(dst, minute() % 10);
				break ;
			case 's' :
				if (n >= 2 || second() > 9)
					append(dst, second() / 10);
				append(dst, second() % 10);
				break ;
			case 'a' :
				dst.append(hour() < 12 ? 'a' : 'p');
				if (n > 1)
					dst.append('m');
				break ;
			case 'A' :
			case 't' :
				dst.append(hour() < 12 ? 'A' : 'P');
				if (n > 1)
					dst.append('M');
				break ;
			case '\'':
				++i;
				while (i < fmtlen && (fmt.charAt(i) != '\''))
					dst.append(fmt.charAt(i++));
				break;
			case '\\' :
				dst.append(fmt.charAt(++i));
				break ;
			default :
				while (--n >= 0)
					dst.append(fmt.charAt(i));
				}
			}
		return dst.toString();
	}

	private static void append(StringBuilder dst, int i) {
		dst.append((char) ('0' + i));
	}

	// parse

	public static SuDate parse(String s) {
		return parse(s, "yMd");
	}

	public static SuDate parse(String s, String order) {
		final int NOTSET = 9999;
		int year = NOTSET;
		int month = 0;
		int day = 0;
		int hour = NOTSET;
		int minute = NOTSET;
		int second = NOTSET;
		int millisecond = 0;

		final String[] date_patterns =
			{
			"", // set to supplied order
			"md",
			"dm",
			"dmy",
			"mdy",
			"ymd"
			};

		char[] syspat = getSyspat(order, date_patterns);

		// scan
		final int MAXTOKENS = 20;
		Field[] type = new Field[MAXTOKENS];
		int[] tokens = new int[MAXTOKENS];
		int ntokens = 0;
		boolean got_time = false;
		Arrays.fill(type, Field.UNK);
		char prev = 0;
		for (int si = 0; si < s.length(); ) {
			char c = s.charAt(si);
			assert(ntokens < MAXTOKENS);
			String next;
			if (!"".equals(next = nextWord(s, si))) {
				si += next.length();
				int i;
				for (i = 0; i < 12; ++i)
					if (months[i].startsWith(next))
						break ;
				if (i < 12) {
					type[ntokens] = Field.MONTH;
					tokens[ntokens] = i + 1;
					++ntokens;
				} else if (next.equals("Am") || next.equals("Pm")) {
					if (next.charAt(0) == 'P') {
						if (hour < 12)
							hour += 12;
					} else { // (word[0] == 'A')
						if (hour == 12)
							hour = 0;
						if (hour > 12)
							return null;
					}
				} else {
					// ignore days of week
					for (i = 0; i < 7; ++i)
						if (days[i].startsWith(next))
							break ;
					if (i >= 7)
						return null;
				}
			} else if (!"".equals(next = nextNumber(s, si))) {
				int n = Integer.parseInt(next);
				int len = next.length();
				si += len;
				c = get(s, si);
				if (len == 6 || len == 8) {
					Digits digits = new Digits(next);
					if (len == 6) {
						// date with no separators with yy
						tokens[ntokens++] = digits.get(2);
						tokens[ntokens++] = digits.get(2);
						tokens[ntokens++] = digits.get(2);
					} else if (len == 8) {
						// date with no separators with yyyy
						for (int i = 0; i < 3; ++i)
							tokens[ntokens++] =
								syspat[i] == 'y' ? digits.get(4) : digits.get(2);
					}
					if (c == '.') { // time
						++si;
						String time = nextNumber(s, si);
						int tlen = time.length();
						si += tlen;
						if (tlen == 4 || tlen == 6 || tlen == 9) {
							digits = new Digits(time);
							hour = digits.get(2);
							minute = digits.get(2);
							if (tlen >= 6) {
								second = digits.get(2);
								if (tlen == 9)
									millisecond = digits.get(3);
							}
						}
					}
				} else if (prev == ':' || c == ':' || ampm_ahead(s, si)) {
					// time
					got_time = true;
					if (hour == NOTSET)
						hour = n;
					else if (minute == NOTSET)
						minute = n;
					else if (second == NOTSET)
						second = n;
					else
						return null;
				} else {
					// date
					tokens[ntokens] = n;
					if (prev == '\'')
						type[ntokens] = Field.YEAR;
					++ntokens;
				}
			} else {
				prev = c; // ignore
				++si;
			}
		}
		if (hour == NOTSET)
			hour = 0;
		if (minute == NOTSET)
			minute = 0;
		if (second == NOTSET)
			second = 0;

		// search for date match
		int pat = 0;
		String p = null;
		for (; pat < date_patterns.length; ++pat) {
			p = date_patterns[pat];
			// try one pattern
			int t;
			for (t = 0; t < p.length() && t < ntokens; ++t) {
				Field part;
				if (p.charAt(t) == 'y')
					part = Field.YEAR;
				else if (p.charAt(t) == 'm')
					part = Field.MONTH;
				else if (p.charAt(t) == 'd')
					part = Field.DAY;
				else
					throw SuInternalError.unreachable();
				if ((type[t] != Field.UNK && type[t] != part) ||
					tokens[t] < part.min || tokens[t] > part.max)
					break ;
				}
			// stop at first match
			assert p != null;
			if (t == p.length() && t == ntokens)
				break ;
		}
		assert p != null;

		SuDate now = SuDate.now();

		if (pat < date_patterns.length) {
			// use match
			for (int t = 0; t < p.length(); ++t) {
				if (p.charAt(t) == 'y')
					year = tokens[t];
				else if (p.charAt(t) == 'm')
					month = tokens[t];
				else if (p.charAt(t) == 'd')
					day = tokens[t];
				else
					throw SuInternalError.unreachable();
			}
		} else if (got_time && ntokens == 0) {
			year = now.year();
			month = now.month();
			day = now.day();
		} else
			return null; // no match

		if (year == NOTSET) {
			if (month >= Math.max(now.month() - 6, 1) &&
				month <= Math.min(now.month() + 5, 12))
				year = now.year();
			else if (now.month() < 6)
				year = now.year() - 1;
			else
				year = now.year() + 1;
		} else if (year < 100) {
			int thisyr = now.year();
			year += thisyr - thisyr % 100;
			if (year - thisyr > 20)
				year -= 100;
		}
		if (! valid(year, month, day, hour, minute, second, millisecond))
			return null;
		return new SuDate(year, month, day, hour, minute, second, millisecond);
	}

	private static String nextWord(String s, int si) {
		StringBuilder dst = new StringBuilder();
		for (; si < s.length() && Character.isLetter(s.charAt(si)); ++si)
			dst.append(Character.toLowerCase(s.charAt(si)));
		if (dst.length() == 0)
			return "";
		dst.setCharAt(0, Character.toUpperCase(dst.charAt(0)));
		return dst.toString();
	}

	private static String nextNumber(String s, int si) {
		StringBuilder dst = new StringBuilder();
		for (; si < s.length() && Character.isDigit(s.charAt(si)); ++si)
			dst.append(s.charAt(si));
		return dst.toString();
	}

	private static char[] getSyspat(String order, String[] date_patterns) {
		char[] syspat = new char[3];
		int i = 0;
		char oc = 0;
		char prev = 0;
		for (int oi = 0; oi < order.length() && i < 3; prev = oc, ++oi) {
			oc = order.charAt(oi);
			if (oc != prev && (oc == 'y' || oc == 'M' || oc == 'd'))
				syspat[i++] = Character.toLowerCase(oc);
		}
		if (i != 3)
			throw new SuException("invalid date format: '" + order + "'");
		date_patterns[0] = new String(syspat);

		// swap month-day patterns if system setting is day first
		for (i = 0; i < 3; ++i)
			if (syspat[i] == 'm')
				break ;
			else if (syspat[i] == 'd')
				swap(date_patterns, 1, 2);
		return syspat;
	}

	private static void swap(String[] a, int i, int j) {
		String tmp = a[1];
		a[1] = a[2];
		a[2] = tmp;
	}

	static boolean ampm_ahead(String s, int i) {
		char s0 = get(s, i);
		if (s0 == ' ')
			s0 = get(s, ++i);
		s0 = Character.toLowerCase(s0);
		return (s0 == 'a' || s0 == 'p') &&
				Character.toLowerCase(get(s, i + 1)) == 'm';
	}

	private static char get(String s, int i) {
		return i < s.length() ? s.charAt(i) : 0;
	}

	private static class Digits {
		private final String s;
		private int i = 0;

		Digits(String s) {
			this.s = s;
		}

		int get(int n) {
			i += n;
			return Integer.parseInt(s.substring(i - n, i));
		}
	}

	static enum Field {
		YEAR(0, 3000), MONTH(1, 12), DAY(1, 31),
		HOUR(0, 23), MINUTE(0, 59), SECOND(0, 59), MILLISECOND(0, 999),
		UNK(0, 0);

		final int min;
		final int max;

		Field(int min, int max) {
			this.min = min;
			this.max = max;
		}

		boolean valid(int n) {
			return min <= n && n <= max;
		}
	}

}
