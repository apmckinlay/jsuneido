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

	public static Dnum parse(String s) {
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
		toString_test(dnum(PLUS, 1000000, -3), "1000");
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
	public void add_test() {
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
		// need aligning
		add_test("1e12", "1e14", "1.01e14");
		add_test("123", "1e-99", "123");
		add_test("1e-99", "123", "123");
		add_test("11111111111111111111", "2222222222222222222e-4",
				"11111333333333333333");
		add_test("11111111111111111111", "6666666666666666666e-4",
				"11111777777777777778");
		// int64 overflow
		add_test("18446744073709551615", "11", "18446744073709551630");
		add_test("18446744073709551615e126", "18446744073709551615e126", "inf");
	}

	private static void add_test(String x, String y, String expected) {
		Dnum xn = parse(x);
		Dnum yn = parse(y);
		assertThat(add(xn, yn).check().toString(), equalTo(expected));
		assertThat(add(yn, xn).check().toString(), equalTo(expected));
	}

	@Test
	public void sub_test() {
		sub_test("123", "0", "123");
		sub_test("inf", "-inf", "inf");
		sub_test("inf", "inf", "0");
		sub_test("-inf", "-inf", "0");
		sub_test("inf", "123", "inf");
		// aligned
		sub_test("456", "123", "333");
		sub_test("-123", "-456", "333");
		sub_test("4.56e9", "1.23e9", "3.33e9");
		// need aligning
		sub_test("123", "1e-99", "123");
		sub_test("1e99", "123", "1e99");
		sub_test("1e14", "1e12", "9.9e13");
		sub_test("12222222222222222222", "11111111111111111111e-4",
				"12221111111111111111");
	}

	private static void sub_test(String x, String y, String expected) {
		assertThat(sub(parse(x), parse(y)).check().toString(), equalTo(expected));
	}

	@Test
	public void mul_test() {
		// special cases (no actual math)
		mul_test("0", "0", "0");
		mul_test("123", "0", "0");
		mul_test("123", "inf", "inf");
		mul_test("inf", "inf", "inf");
		// result fits in uint64
		mul_test("2", "333", "666");
		mul_test("2e9", "333e-9", "666");
		mul_test("2e3", "3e3", "6e6");
		mul_test("123456789000000000", "123456789000000000", "1.5241578750190521e34");
		// result too big for uint64
		mul_test("1234567890123456", "1234567890123456", "1.524157875323881728e30");
		// exp overflow
		mul_test("2e99", "2e99", "inf");
	}

	private static void mul_test(String x, String y, String expected) {
		Dnum xn = parse(x);
		Dnum yn = parse(y);
		assertThat(mul(xn, yn).check().toString(), equalTo(expected));
		assertThat(mul(yn, xn).check().toString(), equalTo(expected));
	}

	@Test
	public void div_test() {
		// special cases (no actual math)
		div_test("0", "0", "0");
		div_test("123", "0", "inf");
		div_test("123", "inf", "0");
		div_test("inf", "123", "inf");
		div_test("inf", "inf", "1");
		div_test("123", "123", "1");
		div_test("123000", ".000123", "1e9");
		// exp overflow
		div_test("1e99", "1e-99", "inf");
		div_test("1e-99", "1e99", "0");
		// divides evenly
		div_test("4444", "2222", "2");
		div_test("2222", "4444", ".5");
		// long division
		div_test("2", "3", ".6666666666666666666");
		div_test("1", "3", ".3333333333333333333");
		div_test("11", "17", ".6470588235294117647");
		div_test("1234567890123456", "9876543210123456", ".12499999887187493");
	}

	private static void div_test(String x, String y, String expected) {
		assertThat(div(parse(x), parse(y)).check().toString(), equalTo(expected));
	}

	@Test
	public void cmp_test() {
		String data[] = {"-inf", "-1e9", "-1e-9", "0", "1e-9", "1e9", "inf"};
		int n = data.length;
		for (int i = 0; i < n; ++i) {
			Dnum x = parse(data[i]);
			assertThat(cmp(x, x), equalTo(0));
			for (int j = i + 1; j < n; ++j) {
				Dnum y = parse(data[j]);
				assertThat(cmp(x, y), equalTo(-1));
				assertThat(cmp(y, x), equalTo(+1));
			}
		}
	}

	/** PortTests */
	public static boolean pt_dnum_add(String... args) {
		Dnum x = parse(args[0]);
		Dnum y = parse(args[1]);
		return add(x, y).toString().equals(args[2]) &&
				add(y, x).toString().equals(args[2]);
	}

	/** PortTests */
	public static boolean pt_dnum_sub(String... args) {
		Dnum x = parse(args[0]);
		Dnum y = parse(args[1]);
		return sub(x, y).toString().equals(args[2]) &&
				(args[2].equals("0") || sub(y, x).toString().equals("-" + args[2]));
	}

	/** PortTests */
	public static boolean pt_dnum_mul(String... args) {
		Dnum x = parse(args[0]);
		Dnum y = parse(args[1]);
		return mul(x, y).toString().equals(args[2]) &&
				mul(y, x).toString().equals(args[2]);
	}

	/** PortTests */
	public static boolean pt_dnum_div(String... args) {
		Dnum x = parse(args[0]);
		Dnum y = parse(args[1]);
		return div(x, y).toString().equals(args[2]);
	}

	/** PortTests */
	public static boolean pt_dnum_cmp(String... data) {
		int n = data.length;
		for (int i = 0; i < n; ++i) {
			Dnum x = parse(data[i]);
			if (cmp(x, x) != 0)
				return false;
			for (int j = i + 1; j < n; ++j) {
				Dnum y = parse(data[j]);
				if (cmp(x, y) != -1 || cmp(y, x) != +1)
					return false;
			}
		}
		return true;
	}

}
