/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.nio.ByteBuffer;
import java.util.Calendar;

import javax.annotation.concurrent.Immutable;

import suneido.util.FAQCalendar;

import com.google.common.primitives.Ints;

/**
 */
@Immutable
public class SuDate extends SuValue implements Comparable<SuDate> {
	private final int date;
	private final int time;

	public SuDate(int date, int time) {
		this.date = date;
		this.time = time;
		validate(year(), month(), day(), hour(), minute(), second(), millisecond());
	}
	
	public SuDate(int year, int month, int day,
			int hour, int minute, int second, int millisecond) {
		validate(year, month, day, hour, minute, second, millisecond);
		date = (year << 9) | (month << 5) | day;
		time = (hour << 22) | (minute << 16) | (second << 10) | millisecond;
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
	
	// validation
	
	private static void validate(int year, int month, int day, 
			int hour, int minute, int second, int millisecond) {
		validate(year, 1700, 3000);
		validate(month, 1, 12);
		validate(day, 1, 31);
		validate(hour, 0, 23);
		validate(minute, 0, 59);
		validate(second, 0, 59);
		validate(millisecond, 0, 999);
		
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year, month - 1, day);
		if (cal.get(Calendar.YEAR) != year ||
				cal.get(Calendar.MONTH) != month - 1 ||
				cal.get(Calendar.DAY_OF_MONTH) != day)
			throw new SuDateBad();
	}

	private static void validate(int val, int from, int to) {
		if (val < from || to < val)
			throw new SuDateBad();
	}
	
	public static class SuDateBad extends SuException {
		private static final long serialVersionUID = 1L;

		SuDateBad() {
			super("bad date");
		}
	}

	// Packable
	
	@Override
	public int packSize() {
		return 9;
	}
	
	@Override
	public void pack(ByteBuffer buf) {
		buf.putInt(date);
		buf.putInt(time);
	}
	
	public static SuDate unpack(ByteBuffer buf) {
		int date = buf.getInt();
		int time = buf.getInt();
		return new SuDate(date, time);
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
		if (day < 0 || 28 < day) {
			Calendar cal = Calendar.getInstance();
			cal.clear();
			cal.set(year, month - 1, day);
			year = cal.get(Calendar.YEAR);
			month =	cal.get(Calendar.MONTH) + 1;
			day = cal.get(Calendar.DAY_OF_MONTH);
		}
		
		while (month < 0) {
			--year;
			month += 12;
		}
		while (month >= 12) {
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
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year(), month() - 1, day(), hour(), minute(), second());
		cal.set(Calendar.MILLISECOND, millisecond());
		return cal.getTime().getTime();
	}

}
