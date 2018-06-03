/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static suneido.util.testing.Throwing.assertThrew;

import org.junit.Test;

import suneido.util.Dnum;

public class NumbersTest {

	@Test
	public void test_toNum() {
		assertThat(Numbers.toNum(0), equalTo(0));
		assertThat(Numbers.toNum(123), equalTo(123));
		assertThat(Numbers.toNum(""), equalTo(0));
		assertThat(Numbers.toNum(false), equalTo(0));
		assertThrew(() -> Numbers.toNum("xxx"));
		assertThrew(() -> Numbers.toNum(".f"));
		assertThrew(() -> Numbers.toNum("0xffff"));
	}

	@Test
	public void test_stringToNumber() {
		assertThat(Numbers.stringToNumber("123"), equalTo(123));
		assertThat(Numbers.stringToNumber("123456789.012345e6"),
				equalTo(Dnum.from(123456789012345L)));
		assertThat(Numbers.stringToNumber("12300000000000000000000"),
				equalTo(Dnum.parse("123e20")));
		assertThat(Numbers.stringToNumber("0100"), equalTo(100)); // NOT octal
		assertThat(Numbers.stringToNumber("0x100"), equalTo(256));
		assertThat(Numbers.stringToNumber("0xffffffff"), equalTo(-1));

		assertThrew(() -> Numbers.stringToNumber("xxx"));
		assertThrew(() -> Numbers.stringToNumber(".f"));
		assertThrew(() -> Numbers.stringToNumber("0100x"));
		assertThrew(() -> Numbers.stringToNumber("0xfffffffff"));
		assertThrew(() -> Numbers.stringToNumber("0x100x"));
	}

	@Test
	public void test_longValue() {
		assertThat(Numbers.longValue(0), equalTo(0L));
		assertThat(Numbers.longValue(Integer.MIN_VALUE),
				equalTo((long) Integer.MIN_VALUE));
		assertThat(Numbers.longValue(Integer.MAX_VALUE),
				equalTo((long) Integer.MAX_VALUE));

		assertThat(Numbers.longValue(Dnum.Zero), equalTo(0L));
		assertThat(Numbers.longValue(Dnum.from(Integer.MIN_VALUE * 100L)),
				equalTo(Integer.MIN_VALUE * 100L));
		assertThat(Numbers.longValue(Dnum.from(Integer.MAX_VALUE * 100L)),
				equalTo(Integer.MAX_VALUE * 100L));

		assertThrew(() -> Numbers.longValue(Dnum.parse(".1")));
		assertThrew(() -> Numbers.longValue(Dnum.parse("1e99")));
		assertThrew(() -> Numbers.longValue("string"));
	}

}
