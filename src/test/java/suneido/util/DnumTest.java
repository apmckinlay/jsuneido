/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static suneido.util.Dnum.*;

import org.junit.Test;

public class DnumTest {

		@Test
		public void parse_test() {
			parse_test("0", Zero);
			parse_test("123", dnum(PLUS, 123, 0));
			parse_test("000123", dnum(PLUS, 123, 0));
			parse_test("123000", dnum(PLUS, 123000, 0));
			parse_test("123e0", dnum(PLUS, 123, 0));
			parse_test("+123", dnum(PLUS, 123, 0));
			parse_test("-123", dnum(MINUS, 123, 0));
			parse_test("123.456", dnum(PLUS, 123456, -3));
			parse_test(".123", dnum(PLUS, 123, -3));
			parse_test(".000123", dnum(PLUS, 123, -6));
			parse_test("123e99", dnum(PLUS, 123, 99));
			parse_test("+123e+99", dnum(PLUS, 123, 99));
			parse_test("-123e-99", dnum(MINUS, 123, -99));
		}

		private static void parse_test(String s, Dnum expected) {
			assertThat(parse(s), equalTo(expected));
		}

		private static Dnum parse(String s) {
			switch (s) {
			case "inf":
				return Inf;
			case "-inf":
				return MinusInf;
			case "-0":
				return Zero;
			default:
				return Dnum.parse(s).check();
			}
		}

		@Test
		public void toString_test() {
			toString_test(Zero, "0");
			toString_test(One, "1");
			toString_test(Inf, "inf");
			toString_test(MinusInf, "-inf");
			toString_test(dnum(PLUS, 123, 0), "123");
			toString_test(dnum(MINUS, 123, 0), "-123");
			toString_test(dnum(PLUS, 1, 3), "1000");
			toString_test(dnum(PLUS, 1, -9), "1e-9");
			toString_test(dnum(PLUS, 123000, -3), "123");
			toString_test(dnum(PLUS, 123456, -3), "123.456");
		}

		private static void toString_test(Dnum num, String expected) {
			assertThat(num.toString(), equalTo(expected));
		}

		private static Dnum dnum(byte sign, long coef, int exp) {
			return new Dnum(sign, coef, exp);
		}

		@Test
		public void neg_test() {
			neg_test(Zero, Zero);
			neg_test(Inf, MinusInf);
			neg_test(parse("123"), parse("-123"));
		}

		private static void neg_test(Dnum x, Dnum y) {
			assertThat(x.neg(), equalTo(y));
			assertThat(y.neg(), equalTo(x));
		}

		@Test
		public void abs_test() {
			abs_test(Zero, Zero);
			abs_test(One, One);
			abs_test(parse("-1"), One);
			abs_test(Inf, Inf);
			abs_test(MinusInf, Inf);
		}

		private static void abs_test(Dnum x, Dnum expected) {
			assertThat(x.abs(), equalTo(expected));
		}

		@Test
		public void addsub_test() {
			add_test("0", "0", "0");
			add_test("1", "0", "1");
			add_test("0", "1", "1");
			add_test("123", "0", "123");
			add_test("inf", "-inf", "0");
			add_test("inf", "inf", "inf");
			add_test("-inf", "-inf", "-inf");
			add_test("inf", "123", "inf");
			add_test("-inf", "123", "-inf");
			// aligned
			add_test("123", "456", "579");
			add_test("-123", "-456", "-579");
			add_test("1.23e9", "4.56e9", "5.79e9");
//			add_test("123", "-456", "-333");
//			add_test("-123", "456", "333");
			// need aligning
			add_test("1e12", "1e14", "1.01e14");
//			add_test("123", "1e-99", "123");
//			add_test("1e-99", "123", "123");
//			add_test("11111111111111111111", "2222222222222222222e-4",
//					"11111333333333333333");
//			add_test("11111111111111111111", "6666666666666666666e-4",
//					"11111777777777777778");
			// int64 overflow
//			add_test("18446744073709551615", "11", "18446744073709551630");
		}

		private static void add_test(String x, String y, String expected) {
			assertThat(add(parse(x), parse(y)), equalTo(parse(expected)));
		}

}
