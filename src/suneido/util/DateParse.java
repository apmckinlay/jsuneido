package suneido.util;

import java.util.*;

import suneido.SuException;

public class DateParse {
	private final static String[] month =
		{ "January", "February", "March", "April", "May", "June", "July",
			"August", "September", "October", "November", "December" };
	private final static String[] weekday =
		{ "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
			"Saturday" };

	static enum Field {
		YEAR(0, 2500), MONTH(1, 12), DAY(1, 31),
		HOUR(0, 59), MINUTE(0, 59), SECOND(0, 59), UNK(0, 0);

		int min;
		int max;

		Field(int min, int max) {
			this.min = min;
			this.max = max;
		}
	}

	static class DateTime {
		int year;
		int month;
		int day;
		int hour;
		int minute;
		int second;
		int millisecond;

		DateTime(int y, int mon, int d, int h, int min, int s, int milli) {
			year = y;
			month = mon;
			day = d;
			hour = h;
			minute = min;
			second = s;
			millisecond = milli;
		}
		DateTime() {
			Calendar c = Calendar.getInstance();
			year = c.get(Calendar.YEAR);
			month = c.get(Calendar.MONTH) + 1;
			day = c.get(Calendar.DAY_OF_MONTH);
			hour = c.get(Calendar.HOUR_OF_DAY);
			minute = c.get(Calendar.MINUTE);
			second = c.get(Calendar.SECOND);
			millisecond = c.get(Calendar.MILLISECOND);
		}
		boolean valid() {
			Calendar c = getCalendar();
			return year == c.get(Calendar.YEAR)
					&& month == c.get(Calendar.MONTH) + 1
					&& day == c.get(Calendar.DAY_OF_MONTH)
					&& hour == c.get(Calendar.HOUR_OF_DAY)
					&& minute == c.get(Calendar.MINUTE)
					&& second == c.get(Calendar.SECOND)
					&& millisecond == c.get(Calendar.MILLISECOND);
		}
		Date getDate() {
			return getCalendar().getTime();
		}
		private Calendar getCalendar() {
			Calendar c = Calendar.getInstance();
			c.clear();
			c.set(year, month - 1, day, hour, minute, second);
			c.set(Calendar.MILLISECOND, millisecond);
			return c;
		}
		@Override
		public String toString() {
			return year + "-" + month + "-" + day + " "
					+ hour + ":" + minute + ":" + second + ":" + millisecond;
		}
	}

	public static Date parse(String s) {
		return parse(s, "yMd");
	}

	public static Date parse(String s, String order) {
		final int NOTSET = 9999;
		DateTime dt = new DateTime(9999, 0, 0, NOTSET, NOTSET, NOTSET, 0);

		String[] date_patterns =
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
					if (month[i].startsWith(next))
						break ;
				if (i < 12) {
					type[ntokens] = Field.MONTH;
					tokens[ntokens] = i + 1;
					++ntokens;
				} else if (next.equals("Am") || next.equals("Pm")) {
					if (next.charAt(0) == 'P') {
						if (dt.hour < 12)
							dt.hour += 12;
					} else { // (word[0] == 'A')
						if (dt.hour == 12)
							dt.hour = 0;
						if (dt.hour > 12)
							return null;
					}
				} else {
					// ignore days of week
					for (i = 0; i < 7; ++i)
						if (weekday[i].startsWith(next))
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
							dt.hour = digits.get(2);
							dt.minute = digits.get(2);
							if (tlen >= 6) {
								dt.second = digits.get(2);
								if (tlen == 9)
									dt.millisecond = digits.get(3);
							}
						}
					}
				} else if (prev == ':' || c == ':' || ampm_ahead(s, si)) {
					// time
					got_time = true;
					if (dt.hour == NOTSET)
						dt.hour = n;
					else if (dt.minute == NOTSET)
						dt.minute = n;
					else if (dt.second == NOTSET)
						dt.second = n;
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
		if (dt.hour == NOTSET)
			dt.hour = 0;
		if (dt.minute == NOTSET)
			dt.minute = 0;
		if (dt.second == NOTSET)
			dt.second = 0;

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
					throw SuException.unreachable();
				if ((type[t] != Field.UNK && type[t] != part) ||
					tokens[t] < part.min || tokens[t] > part.max)
					break ;
				}
			// stop at first match
			if (t == p.length() && t == ntokens)
				break ;
		}

		DateTime now = new DateTime();

		if (pat < date_patterns.length) {
			// use match
			for (int t = 0; t < p.length(); ++t) {
				if (p.charAt(t) == 'y')
					dt.year = tokens[t];
				else if (p.charAt(t) == 'm')
					dt.month = tokens[t];
				else if (p.charAt(t) == 'd')
					dt.day = tokens[t];
				else
					throw SuException.unreachable();
			}
		} else if (got_time && ntokens == 0) {
			dt.year = now.year;
			dt.month = now.month;
			dt.day = now.day;
			}
		else
			return null; // no match

		if (dt.year == 9999)
			{
			if (dt.month >= Math.max(now.month - 6, 1) &&
				dt.month <= Math.min(now.month + 5, 12))
				dt.year = now.year;
			else if (now.month < 6)
				dt.year = now.year - 1;
			else
				dt.year = now.year + 1;
			}
		else if (dt.year < 100)
			{
			int thisyr = now.year;
			dt.year += thisyr - thisyr % 100;
			if (dt.year - thisyr > 20)
				dt.year -= 100;
			}
		if (!dt.valid())
			return null;
		return dt.getDate();
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

}
