/* Copyright 2018 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static suneido.util.Div128.divide;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Random;

import suneido.util.testing.Benchmark;

public class TestDiv128 {

	private final static long E15 = 1_000_000_000_000_000L;

	public static void main(String[] args) {
		//long q = divide(4444, 2222);
		//System.out.println(q);

		Random rand = new Random(12345678);
		final int N = 1_000_000;
		for (int i = 0; i < N; ++i) {
			long dividend = next(rand);
			long divisor = next(rand);
			long q = divide(dividend, divisor);
			//System.out.printf("%18d %18d %18d\n", dividend, divisor, q);

			BigDecimal x = BigDecimal.valueOf(dividend).movePointRight(16);
			BigDecimal y = BigDecimal.valueOf(divisor);
			try {
				BigDecimal qb = x.divideToIntegralValue(y, mc16);
				//System.out.printf("%18d %18d %18d\n", dividend, divisor, q);
				assert q == qb.longValueExact();
			} catch (ArithmeticException e) {
				BigDecimal qb = x.divide(y, mc16);
				//System.out.printf("%56s\n\n", qb.toPlainString());
				almostSame(BigDecimal.valueOf(q), qb);
			}
		}

		Benchmark.benchmark("div128", (long nreps) -> {
			long x = 1234567890123456L;
			long y = 9876543210987654L;
			while (nreps-- > 0)
				z += divide(x++, y--);
		});
	}
	private static long z = 0;

	private static long next(Random rand) {
		int ndigits = 1 + rand.nextInt(16);
		long n = 0;
		for (int i = 0; i < ndigits - 1; ++i)
			n = n / 10 + rand.nextInt(10) * E15;
		n = n / 10 + (1 + rand.nextInt(9)) * E15; // high digit != 0
		return n;
	}

	private final static MathContext mc16 = new MathContext(16);
	private final static MathContext mc15 = new MathContext(15);
	private final static MathContext mc15d = new MathContext(15, RoundingMode.DOWN);

	static void almostSame(BigDecimal x, BigDecimal y) {
		BigDecimal d = x.subtract(y).abs();
		if (d.intValueExact() <= 1)
			return;
		if (x.round(mc15).equals(y.round(mc15)))
			return;
		if (x.round(mc15d).equals(y.round(mc15d)))
			return;
		throw new AssertionError("\n" + y.toPlainString() + " expected\n" +
			x.toPlainString() + " actual");
	}

}
