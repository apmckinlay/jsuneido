package suneido.language.builtin;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

import suneido.language.Ops;


public class NumberMethodsTest {

	@Test
	public void test_format() {
		format("0", "###", "0");
		format("0", "#.##", ".00");
		format(".08", "#.##", ".08");
		format(".08", "#.#", ".1");
		format("6.789", "#.##", "6.79");
		format("123", "##", "#");
		format("-1", "#.##", "-");
		format("-12", "-####", "-12");
		format("-12", "(####)", "(12)");
	}

	private void format(String num, String mask, String expected) {
		BigDecimal bd = new BigDecimal(num);
		assertEquals(expected, NumberMethods.format(bd, mask));
	}

	@Test
	public void test_frac() {
		frac("123", "0");
		frac("12.34", ".34");
		frac("10000.00002", ".00002");
		frac(".00002", ".00002");
	}

	private void frac(String num, String expected) {
		BigDecimal bd = new BigDecimal(num);
		assertEquals(expected, Ops.toStringBD(NumberMethods.frac(bd)));
	}

}
