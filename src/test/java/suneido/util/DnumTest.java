/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static suneido.util.Dnum.*;
import static suneido.util.testing.Benchmark.benchmark;
import static suneido.util.testing.Throwing.assertThrew;

import java.math.BigDecimal;
import java.math.MathContext;

import org.junit.Test;

import suneido.PortTests;

public class DnumTest {

	@Test
	public void parse_test() {
		assertThrew(() -> Dnum.parse("."));
		assertThrew(() -> Dnum.parse("1.2.3"));
		assertThrew(() -> Dnum.parse("1111111111111111111111")); // overflow
		assertThrew(() -> Dnum.parse("-+1"));

		parseTest("0", "z0e0");
		parseTest("0.", "z0e0");
		parseTest(".0", "z0e0");
		parseTest("0.0", "z0e0");
		parseTest("-0.0e9", "z0e0");
		parseTest("9999999999999999", "+.9999999999999999e16");
		parseTest(".123", "+.123e0");
		parseTest("1", "+.1e1");
		parseTest("1234", "+.1234e4");
		parseTest("1234e0", "+.1234e4");
		parseTest(".001", "+.1e-2");
		parseTest("-12.34", "-.1234e2");
		parseTest("0012.3400", "+.1234e2");
		parseTest("0012.3400e2", "+.1234e4");
		parseTest("123000", "+.123e6");
		parseTest("100.1", "+.1001e3");
		parseTest("1e18", "+.1e19");
		parseTest(".9e-9", "+.9e-9");
		parseTest("-1e-11", "-.1e-10");
		parseTest("-12.34e56", "-.1234e58");

		assertThat(Dnum.parse("1e999"), equalTo(Inf));
		assertThat(Dnum.parse("1e-999"), equalTo(Zero));
		assertThat(Dnum.parse("0e999"), equalTo(Zero));
	}

	private static void parseTest(String s, String expected) {
		assertThat(Dnum.parse(s).show(), equalTo(expected));
	}

	@Test
	public void fromIntTest() {
		assertThat(Dnum.from(0), equalTo(Zero));
		assertThat(Dnum.from(1), equalTo(One));
		assertThat(Dnum.from(123).show(), equalTo("+.123e3"));
		assertThat(Dnum.from(-123).show(), equalTo("-.123e3"));
	}

	@Test
	public void toString_test() {
		assertThat(Zero.toString(), equalTo("0"));
		assertThat(One.toString(), equalTo("1"));
		assertThat(Inf.toString(), equalTo("inf"));
		assertThat(MinusInf.toString(), equalTo("-inf"));
		toStringTest("123");
		toStringTest("-123");
		toStringTest("1000");
		toStringTest("1e9");
		toStringTest("1e-9");
		toStringTest("123.456");
	}

	private static void toStringTest(String s) {
		assertThat(parse(s).toString(), equalTo(s));
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
		// special cases
		addsub("123", "0", "123");
		addsub("inf", "-inf", "0");
		addsub("inf", "123", "inf");
		addsub("-inf", "123", "-inf");

		assertThat(add(Inf, Inf), equalTo(Inf));
		assertThat(add(MinusInf, MinusInf), equalTo(MinusInf));
		assertThat(sub(Inf, Inf), equalTo(Zero));
		assertThat(sub(MinusInf, MinusInf), equalTo(Zero));

		addsub("0", "0", "0");
		addsub("1", "0", "1");
		addsub("0", "1", "1");
		addsub("123", "0", "123");
		addsub("inf", "-inf", "0");
		// aligned
		addsub("123", "456", "579");
		addsub("-123", "-456", "-579");
		addsub("1.23e9", "4.56e9", "5.79e9");

		// need aligning
		addsub("1e4", "2e2", "10200");
		addsub("2e4", "1e2", "20100");
		addsub("1e30", "999", "1e30"); // can't align
		addsub("1e15", "3", "1000000000000003");
		addsub("1e16", "33", "10000000000000030"); // dropped digit
		addsub("1e16", "37", "10000000000000040"); // round dropped digit
		addsub("1e12", "1e14", "1.01e14");
		addsub("123", "1e-99", "123");
		addsub("1111111111111111", "2222222222222222e-4", "1111333333333333");
		addsub("1111111111111111", "6666666666666666e-4", "1111777777777778");

		assertThat(sub(parse("1e-99"), parse("123")), equalTo(parse("-123")));
	}

	private static void addsub(String x, String y, String t) {
		Dnum xn = parse(x);
		Dnum yn = parse(y);
		Dnum tot = parse(t);
		assertThat(add(xn, yn), equalTo(tot));
		assertThat(add(yn, xn), equalTo(tot));
		assertThat(sub(tot, yn), equalTo(xn));
	}

	@Test
	public void mul_test() {
		// special cases (no actual math)
		mulTest("0", "0", "0");
		mulTest("0", "123", "0");
		mulTest("0", "inf", "0");
		mulTest("inf", "123", "inf");
		mulTest("inf", "inf", "inf");

		// fast, single multiply
		int nums[] = { 0, 1, -1, 100, 1234, 9999, -1234 };
		for (int x : nums)
			for (int y : nums)
				mulTest(x, y);

		mulTest("2e9", "333e-9", "666");
		mulTest("2e3", "3e3", "6e6");
		mulTest("1.00000001", "1.00000001", "1.00000002");
		mulTest("1.000000001", "1.000000001", "1.000000002");
		mulTest(".4294967295", ".4294967295", ".1844674406511962");
		mulTest("1.12233445566", "1.12233445566", "1.259634630361629");
		mulTest("1.111111111111111", "1.111111111111111", "1.234567901234568");
		mulTest("1.23456789", "1.23456789", "1.524157875019052");
		mulTest("1.234567899", "1.234567899", "1.524157897241274");

		mulTest("2e99", "2e99", "inf"); // exp overflow
	}

	private static void mulTest(int x, int y) {
		Dnum xn = from(x);
		Dnum yn = from(y);
		Dnum en = from(x * y);
		mulTest(xn, yn, en);
	}

	private static void mulTest(String x, String y, String e) {
		Dnum xn = parse(x);
		Dnum yn = parse(y);
		Dnum en = parse(e);
		mulTest(xn, yn, en);
	}

	private static void mulTest(Dnum xn, Dnum yn, Dnum en) {
		Dnum p = mul(xn, yn);
		assert almostSame(p, en)
			: xn + " * " + yn + " result " + p + " expected " + en;
		p = mul(yn, xn);
		assert almostSame(p, en)
			: xn + " * " + yn + " result " + p + " expected " + en;
	}

	@Test
	public void div_test() {
		// special cases (no actual math)
		divTest("0", "0", "0");
		divTest("123", "0", "inf");
		divTest("123", "1", "123");
		divTest("123", "10", "12.3");
		divTest("123", "inf", "0");
		divTest("inf", "123", "inf");
		divTest("inf", "inf", "1");

		// divides evenly
		divTest("123", "123", "1");
		divTest("123000", ".000123", "1e9");
		divTest("4444", "2222", "2");
		divTest("2222", "4444", ".5");

		// long division
		divTest("2", "3", ".6666666666666666");
		divTest("1", "3", ".3333333333333333");
		divTest("11", "17", ".6470588235294118");
		divTest("1234567890123456", "9876543210987654", ".1249999988609374");
		divTest("1", "3333333333333333", "3e-16");
		divTest("12", ".4444444444444444", "27");

		// exp overflow
		divTest("1e99", "1e-99", "inf");
		divTest("1e-99", "1e99", "0");
	}

	private static void divTest(String x, String y, String expected) {
		Dnum q = div(parse(x), parse(y));
		assert almostSame(q, parse(expected))
			: x + " / " + y + "\n" + q + " result\n" + expected + " expected ";
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

	@Test
	public void longValue_test() {
		long data[] = {0, 1, -1, 123, -123, Integer.MAX_VALUE, Integer.MIN_VALUE,
				1234_5678_9012_3456L, -9999_9999_9999_9999L };
		for (long n : data) {
			Dnum x = Dnum.from(n);
			assertThat(x.longValue(), equalTo(n));
		}
		assertThat(Dnum.parse("99e-99").longValue(), equalTo(0L));
		assertThat(Dnum.parse("99e99").longValue(), equalTo(0L));
	}

	@Test
	public void doubleValue_test() {
		String exact[] = { "0", "1", "-1", "1e9", "123456", "1234567890123456" };
		for (String s : exact) {
			Dnum x = Dnum.parse(s);
			double n = Double.parseDouble(s);
			assertThat(x.doubleValue(), equalTo(n));
		}
		String approx[] = { ".1", "-1.23e99", "4.56e-99" };
		for (String s : approx) {
			double x = Dnum.parse(s).doubleValue();
			double y = Double.parseDouble(s);
			assert Math.abs(1 - x / y) < 1e-15 : x / y;
		}
	}

	// benchmarks ---------------------------------------------------

	private final static MathContext mc16 = new MathContext(16);

	@Test
	public void benchmark_BigDecimal_div() {
		benchmark("bd div", (long nreps) -> {
			BigDecimal x = new BigDecimal("1234567890123456");
			BigDecimal y = new BigDecimal("9876543210987654");
			while (nreps-- > 0)
				bd = x.divide(y, mc16);
		});
	}
	@Test
	public void benchmark_BigDecimal_mul() {
		benchmark("bd mul", (long nreps) -> {
			BigDecimal x = new BigDecimal("1234567890123456");
			BigDecimal y = new BigDecimal("9876543210987654");
			while (nreps-- > 0)
				bd = x.multiply(y, mc16);
		});
	}
	static BigDecimal bd;

	@Test
	public void Benchmark_div() {
		benchmark("div", (long nreps) -> {
			Dnum x = Dnum.parse("1234567890123456");
			Dnum y = Dnum.parse("9876543210987654");
			while (nreps-- > 0)
				z = div(x, y);
		});
	}
	@Test
	public void Benchmark_mul() {
		benchmark("mul", (long nreps) -> {
			Dnum x = Dnum.parse("1234567890123456");
			Dnum y = Dnum.parse("9876543210987654");
			while (nreps-- > 0)
				z = mul(x, y);
		});
	}
	static Dnum z;

	// PortTests --------------------------------------------------------------

	@Test
	public void porttests() {
		PortTests.addTest("dnum_add", DnumTest::pt_dnum_add);
		PortTests.addTest("dnum_sub", DnumTest::pt_dnum_sub);
		PortTests.addTest("dnum_mul", DnumTest::pt_dnum_mul);
		PortTests.addTest("dnum_div", DnumTest::pt_dnum_div);
		PortTests.addTest("dnum_cmp", DnumTest::pt_dnum_cmp);
		assert PortTests.runFile("dnum.test");
	}

	private interface DnumCk {
		boolean ck(Dnum x, Dnum y, Dnum z);
	}

	private static boolean pt_dnum_test(String[] args, DnumCk ck) {
		//System.out.println(Arrays.toString(args));
		assertThat(args.length, equalTo(3));
		Dnum x = parse(args[0]);
		Dnum y = parse(args[1]);
		Dnum z = parse(args[2]);
		return ck.ck(x, y, z);
	}

	private interface DnumOp {
		Dnum op(Dnum x, Dnum y);
	}

	private static boolean ck(DnumOp op, Dnum x, Dnum y, Dnum z) {
		if (almostSame(op.op(x, y), z))
			return true;
		System.out.println(x + ", " + y +
				" => " + op.op(x, y) + " should be " + z);
		return false;
	}

	public static boolean pt_dnum_add(String... args) {
		return pt_dnum_test(args, (x, y, z) ->
				ck(Dnum::add, x, y, z) && ck(Dnum::add, y, x, z));
	}

	public static boolean pt_dnum_sub(String... args) {
		return pt_dnum_test(args, (x, y, z) ->
			ck(Dnum::sub, x, y, z) &&
				(z.equals(Dnum.Zero) || ck(Dnum::sub, y, x, z.neg())));
	}

	public static boolean pt_dnum_mul(String... args) {
		return pt_dnum_test(args, (x, y, z) ->
				ck(Dnum::mul, x, y, z) && ck(Dnum::mul, y, x, z));
	}

	public static boolean pt_dnum_div(String... args) {
		return pt_dnum_test(args, (x, y, z) ->
				ck(Dnum::div, x, y, z));
	}

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
