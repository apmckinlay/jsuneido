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
		public void toString_test() {
			test(Zero, "0");
			test(One, "1");
			test(Inf, "inf");
			test(MinusInf, "-inf");
			test(new Dnum(PLUS, 123, 0), "123");
			test(new Dnum(MINUS, 123, 0), "-123");
			test(new Dnum(PLUS, 1, 3), "1000");
			test(new Dnum(PLUS, 1, -9), "1e-9");
			test(new Dnum(PLUS, 123000, -3), "123");
		}

		private static void test(Dnum num, String expected) {
			assertThat(num.toString(), equalTo(expected));
		}

}
