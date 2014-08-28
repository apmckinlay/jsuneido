/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;

import java.nio.ByteBuffer;

import org.junit.Test;

import suneido.SuDate.SuDateBad;
import suneido.language.Pack;

public class SuDateTest {

	@Test
	public void test_constructor() {
		test(2014, 01, 15, 12, 34, 56, 789);
		test(1900, 01, 01, 0, 0, 0, 0);
		test(2499, 12, 31, 23, 59, 59, 999);
	}

	private static void test(int year, int month, int day,
			int hour, int minute, int second, int millisecond) {
		SuDate d = new SuDate(year, month, day, hour, minute, second, millisecond);
		assertThat(d.year(), equalTo(year));
		assertThat(d.month(), equalTo(month));
		assertThat(d.day(), equalTo(day));
		assertThat(d.hour(), equalTo(hour));
		assertThat(d.minute(), equalTo(minute));
		assertThat(d.second(), equalTo(second));
		assertThat(d.millisecond(), equalTo(millisecond));
	}

	@Test
	public void test_literal_toString() {
		good("20140115");
		good("19000101");
		good("24991231");
		good("20140115.1234");
		good("20140115.123456");
		good("20140115.123456789");
		bad("2014123");
		bad("20141231.1");
		bad("20140115.123");
		bad("20140115.12345");
		bad("20140115.12345678");
		bad("20140230");
		bad("20130229");
		good("20120229"); // leap year
	}

	private static void good(String s) {
		SuDate d = SuDate.fromLiteral(s);
		s = "#" + s;
		assertThat(d.toString(), equalTo(s));
		d = SuDate.fromLiteral(s);
		assertThat(d.toString(), equalTo(s));
	}

	private static void bad(String s) {
		try {
			assertThat(SuDate.fromLiteral(s), equalTo(null));
		} catch (SuDateBad e) {
		}
		s = "#" + s;
		try {
			assertThat(SuDate.fromLiteral(s), equalTo(null));
		} catch (SuDateBad e) {
		}
	}

	@Test
	public void test_pack() {
		pack("20140115");
		pack("19000101");
		pack("24991231");
		pack("20140115.1234");
		pack("20140115.123456");
		pack("20140115.123456789");
	}

	private static void pack(String s) {
		SuDate d = SuDate.fromLiteral(s);
		ByteBuffer buf = ByteBuffer.allocate(d.packSize());
		d.pack(buf);
		buf.flip();
		assertThat(buf.get(), equalTo(Pack.Tag.DATE));
		SuDate d2 = SuDate.unpack(buf);
		assertThat(d2, equalTo(d));
	}

	@Test
	public void test_compare() {
		lt("20140115", "20140116");
		lt("19000101", "20140116");
		lt("20140115", "24991231");
		lt("20140115", "20140115.0100");
		lt("20140115", "20140115.000000001");
	}

	private static void lt(String s1, String s2) {
		SuDate d1 = SuDate.fromLiteral(s1);
		assert d1.equals(SuDate.fromLiteral(s1));
		SuDate d2 = SuDate.fromLiteral(s2);
		assert d2.equals(SuDate.fromLiteral(s2));
		assert d1.compareTo(d2) < 0;
		assert d2.compareTo(d1) > 0;
		assert ! d1.equals(d2);
		assert ! d2.equals(d1);
	}

	@Test
	public void test_plus() {
		//							y	m	d	h	m	s	ms

		// no overflow
		plus("20140115.123456789",	0,	0,	0,	0,	0,	0,	0,	"20140115.123456789");
		plus("20140115.123456789",	0,	0,	0,	0,	0,	0,	1,	"20140115.123456790");
		plus("20140115.123456789",	0,	0,	0,	0,	0,	0,	-1,	"20140115.123456788");
		plus("20140115.123456789",	0,	0,	0,	0,	0,	1,	0,	"20140115.123457789");
		plus("20140115.123456789",	0,	0,	0,	0,	0,	-1,	0,	"20140115.123455789");
		plus("20140115.123456789",	0,	0,	0,	0,	1,	0,	0,	"20140115.123556789");
		plus("20140115.123456789",	0,	0,	0,	0,	-1,	0,	0,	"20140115.123356789");
		plus("20140115.123456789",	0,	0,	0,	1,	0,	0,	0,	"20140115.133456789");
		plus("20140115.123456789",	0,	0,	0,	-1,	0,	0,	0,	"20140115.113456789");
		plus("20140115.123456789",	0,	0,	1,	0,	0,	0,	0,	"20140116.123456789");
		plus("20140115.123456789",	0,	0,	-1,	0,	0,	0,	0,	"20140114.123456789");
		plus("20140115.123456789",	0,	1,	0,	0,	0,	0,	0,	"20140215.123456789");
		plus("20140215.123456789",	0,	-1,	0,	0,	0,	0,	0,	"20140115.123456789");
		plus("20140115.123456789",	1,	0,	0,	0,	0,	0,	0,	"20150115.123456789");
		plus("20140115.123456789",	-1,	0,	0,	0,	0,	0,	0,	"20130115.123456789");

		// overflow
		plus("20140115.123456789",	0,	0,	0,	0,	0,	0,	300, "20140115.123457089");
		plus("20140115.123456789",	0,	0,	0,	0,	0,	0,	2300, "20140115.123459089");
		plus("20140115.123456789",	0,	0,	0,	0,	0,	0,	-1800,	"20140115.123454989");
		plus("20140115.235959999",	0,	0,	0,	0,	0,	0,	1,	"20140116");
		plus("20120228",			0,	0,	1,	0,	0,	0,	0,	"20120229"); // leap year
		plus("20130228",			0,	0,	1,	0,	0,	0,	0,	"20130301");
		plus("20140215",			0,	20,	0,	0,	0,	0,	0,	"20151015");
		plus("20140115",			0,	-2,	0,	0,	0,	0,	0,	"20131115");
}

	private static void plus(String s, int year, int month, int day,
			int hour, int minute, int second, int millisecond,
			String expected) {
		SuDate d = SuDate.fromLiteral(s);
		SuDate e = SuDate.fromLiteral(expected);
		assertThat(d.plus(year, month, day, hour, minute, second, millisecond),
				equalTo(e));
	}

	@Test
	public void test_weekday() {
		weekday("20140112", 1);
		weekday("20140115", 4);
		weekday("20140118", 7);
	}

	private static void weekday(String s, int wd) {
		assertThat(SuDate.fromLiteral(s).weekday(), equalTo(wd));
	}

	@Test
	public void test_minusDays() {
		minusdays("20140215", "20140214", 1);
		minusdays("20140215", "20140115", 31);
		minusdays("20140215", "20130215", 365);
		minusdays("20130215", "20120215", 366);
	}

	private static void minusdays(String s1, String s2, int expected) {
		SuDate d1 = SuDate.fromLiteral(s1);
		SuDate d2 = SuDate.fromLiteral(s2);
		assertThat(d1.minusDays(d1), equalTo(0));
		assertThat(d2.minusDays(d2), equalTo(0));
		assertThat(d1.minusDays(d2), equalTo(expected));
		assertThat(d2.minusDays(d1), equalTo(-expected));
	}

	@Test
	public void test_minusMilliseconds() {
		minusms("123456008", "123456005", 3);
		minusms("123456008", "123455005", 1003);
		minusms("123456008", "123356008", 60 * 1000);
		minusms("123456008", "113456008", 60 * 60 * 1000L);

		minusms("20140115", "20140114.235959999", 1);
		minusms("20140115", "20140114.225959999", 1 + 60 * 60 * 1000L);
	}

	private static void minusms(String s1, String s2, long expected) {
		if (s1.length() == 9)
			s1 = "20140115." + s1;
		SuDate d1 = SuDate.fromLiteral(s1);
		if (s2.length() == 9)
			s2 = "20140115." + s2;
		SuDate d2 = SuDate.fromLiteral(s2);
		assertThat(d1.minusMilliseconds(d1), equalTo(0L));
		assertThat(d2.minusMilliseconds(d2), equalTo(0L));
		assertThat(d1.minusMilliseconds(d2), equalTo(expected));
		assertThat(d2.minusMilliseconds(d1), equalTo(-expected));
	}

	@Test
	public void test_format() {
		format("20140108", "yy-M-d", "14-1-8");
		format("20140116", "yy-MM-dd", "14-01-16");
		format("20140116", "yyyy-MM-dd", "2014-01-16");
		format("20140116", "ddd MMM dd, yyyy", "Thu Jan 16, 2014");
		format("20140116", "xx dddd MMMM dd, yyyy zz",
				"xx Thursday January 16, 2014 zz");

		format("20140108.103855", "HH:mm:ss", "10:38:55");
		format("20140108.103855", "hh:mm:ss a", "10:38:55 a");
		format("20140108.103855", "hh:mm:ss aa", "10:38:55 am");
		format("20140108.103855", "hh:mm:ss A", "10:38:55 A");
		format("20140108.103855", "hh:mm:ss AA", "10:38:55 AM");
		format("20140108.233855", "HH:mm:ss", "23:38:55");
		format("20140108.233855", "hh:mm:ss a", "11:38:55 p");
		format("20140108.233855", "hh:mm:ss aa", "11:38:55 pm");
		format("20140108.233855", "hh:mm:ss A", "11:38:55 P");
		format("20140108.233855", "hh:mm:ss AA", "11:38:55 PM");
		format("20140108.093855", "hh:mm:ss", "09:38:55");
		format("20140108.093855", "h:mm:ss", "9:38:55");
		format("20140108.103855", "h:mm:ss", "10:38:55");
		format("20140108.093855", "h 'h:mm:ss' s", "9 h:mm:ss 55");
	}

	private static void format(String date, String format, String expected) {
		assertThat(SuDate.fromLiteral(date).format(format), equalTo(expected));
	}

	@Test
	public void test_parse() {
		parse("090625", "yMd", "2009 Jun 25");
		parse("20090625", "yMd", "2009 Jun 25");
		parse("June 25, 2009", "yMd", "2009 Jun 25");
		parse("020304", "yMd", "2002 Mar 4");
		parse("020304", "Mdy", "2004 Feb 3");
		parse("032299", "yMd", "1999 Mar 22");
		parse("2009-06-25", "yMd", "2009 Jun 25");
		parse("Wed. 25 June '09", "yMd", "2009 Jun 25");
		parse("30000101", "yMd", "3000 Jan 1");

		noparse("19992525", "yMd");
		noparse("19991233", "yMd");
		noparse("30010303", "yMd");
	}

	private static void parse(String ds, String fmt, String expected) {
		SuDate d = SuDate.parse(ds, fmt);
		assertThat(d.format("yyyy MMM d"), equalTo(expected));
	}

	private static void noparse(String ds, String fmt) {
		assertNull(SuDate.parse(ds, fmt));
	}

}
