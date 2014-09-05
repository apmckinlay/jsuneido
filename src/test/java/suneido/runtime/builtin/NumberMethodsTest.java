/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

import suneido.runtime.Ops;
import suneido.runtime.builtin.NumberMethods;


public class NumberMethodsTest {

	@Test
	public void test_format() {
		format("0", "###", "0");
		format("0", "###.", "0.");
		format("0", "#.##", ".00");
		format(".08", "#.##", ".08");
		format(".08", "#.#", ".1");
		format("6.789", "#.##", "6.79");
		format("123", "##", "#");
		format("-1", "#.##", "-");
		format("-12", "-####", "-12");
		format("-12", "(####)", "(12)");
	}

	private static void format(String num, String mask, String expected) {
		BigDecimal bd = new BigDecimal(num);
		assertEquals(expected, NumberMethods.format(mask, bd));
	}

	@Test
	public void test_frac() {
		frac("123", "0");
		frac("12.34", ".34");
		frac("10000.00002", ".00002");
		frac(".00002", ".00002");
	}

	private static void frac(String num, String expected) {
		BigDecimal bd = new BigDecimal(num);
		Object result = Ops.invoke0(bd, "Frac");
		assertEquals(expected, Ops.toStr(result));
	}

	@Test
	public void test_hex() {
		hex((short) 0x1234, "1234");
		hex((short) 0xabcd, "abcd"); // make sure no sign extension
		hex(0x12345678, "12345678");
		hex(0xaabbccdd, "aabbccdd"); // make sure no sign extension
	}

	private static void hex(Object x, String expected) {
		assertEquals(expected, NumberMethods.Hex(x).toString());
	}

	@Test
	public void test_round() {
		Object bd = new BigDecimal("0");
		bd = NumberMethods.Round(bd, 2);
		assertEquals("0", Ops.display(bd));
	}

}
