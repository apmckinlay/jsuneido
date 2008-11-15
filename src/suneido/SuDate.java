package suneido;

import java.nio.ByteBuffer;
import java.text.*;
import java.util.Calendar;
import java.util.Date;

/**
 * Wrapper for a Java date.
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class SuDate extends SuValue {
	private Date date;
	final public static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmmssSSS");
	static { formatter.setLenient(false); }

	public SuDate() {
		date = new Date();
	}
	public SuDate(String s) {
		if (s.startsWith("#"))
			s = s.substring(1);
		try {
			date = formatter.parse(s);
		} catch (ParseException e) {
			throw new SuException("can't convert to date");
		}
	}
	public SuDate(Date date) {
		this.date = date;
	}

	public static SuDate literal(String s) {
		if (s.startsWith("#"))
			s = s.substring(1);
		if (s.length() < 8 || 18 < s.length())
			return null;
		if (s.length() == 8)
			s += ".";
		if (s.length() < 18)
			s = (s + "000000000").substring(0, 18);
		ParsePosition pos = new ParsePosition(0);
		Date date = formatter.parse(s, pos);
		if (date == null || pos.getIndex() != 18)
			return null;
		return new SuDate(date);
	}

	@Override
	public String toString() {
		String s = "#" + formatter.format(date);
		if (s.endsWith("000000000"))
			return s.substring(0, 9);
		if (s.endsWith("00000"))
			return s.substring(0, 14);
		if (s.endsWith("000"))
			return s.substring(0, 16);
		return s;
	}

	@Override
	public int hashCode() {
		return date.hashCode();
	}

	@Override
	public boolean equals(Object value) {
		return value instanceof SuDate
			? date.equals(((SuDate) value).date)
			: false;
	}

	@Override
	public int compareTo(SuValue value) {
		if (value == this)
			return 0;
		int ord = order() - value.order();
		if (ord != 0)
			return ord < 0 ? -1 : +1;
		// if order the same, value must be a date
		return date.compareTo(((SuDate) value).date);
	}

	@Override
	public int order() {
		return Order.DATE.ordinal();
	}

	@Override
	public void pack(ByteBuffer buf) {
		buf.put(Pack.DATE);
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int date = (cal.get(Calendar.YEAR) << 9)
				| (cal.get(Calendar.MONTH) << 5)
				| cal.get(Calendar.DAY_OF_MONTH);
		buf.putInt(date);
		int time = (cal.get(Calendar.HOUR) << 22)
				| (cal.get(Calendar.MINUTE) << 16)
				| (cal.get(Calendar.SECOND) << 10)
				| cal.get(Calendar.MILLISECOND);
		buf.putInt(time);
	}

	@Override
	public int packSize(int nest) {
		return 9;
	}

	public static SuDate unpack1(ByteBuffer buf) {
		int date = buf.getInt();
		int year = date >> 9;
		int month = (date >> 5) & 0xf;
		int day = date & 0x1f;

		int time = buf.getInt();
		int hour = time >> 22;
		int minute = (time >> 16) & 0x2f;
		int second = (time >> 10) & 0x2f;
		int millisecond = time & 0x3ff;

		Calendar cal = Calendar.getInstance();
		cal.set(year, month, day, hour, minute, second);
		cal.set(Calendar.MILLISECOND, millisecond);

		return new SuDate(cal.getTime());
	}
	public SuDate increment() {
		 date.setTime(date.getTime() + 1);
		return this;
	}

}
