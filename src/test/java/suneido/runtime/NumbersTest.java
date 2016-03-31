package suneido.runtime;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import suneido.SuException;

public class NumbersTest {

	@Test
	public void test_toNum() {
		assertThat(Numbers.toNum(0), equalTo(0));
		assertThat(Numbers.toNum(123), equalTo(123));
		assertThat(Numbers.toNum(""), equalTo(0));
		assertThat(Numbers.toNum("123"), equalTo(123));
		assertThat(Numbers.toNum("0100"), equalTo(100));
		assertThat(Numbers.toNum(true), equalTo(1));
		assertThat(Numbers.toNum(false), equalTo(0));
		assertThrows(() -> Numbers.toNum("xxx"));
		assertThrows(() -> Numbers.toNum(".f"));
		assertThrows(() -> Numbers.toNum("0xffff"));
	}

	@Test
	public void test_stringToNumber() {
		assertThat(Numbers.stringToNumber(""), equalTo(0));
		assertThat(Numbers.stringToNumber("123"), equalTo(123));
		assertThat(Numbers.stringToNumber("0100"), equalTo(64));
		assertThat(Numbers.stringToNumber("0x100"), equalTo(256));
		assertThrows(() -> Numbers.stringToNumber("xxx"));
		assertThrows(() -> Numbers.stringToNumber(".f"));
		assertThrows(() -> Numbers.stringToNumber("0100x"));
		assertThrows(() -> Numbers.stringToNumber("0x100x"));
	}

	private static void assertThrows(Runnable f) {
		try {
			f.run();
			fail();
		} catch (Throwable e) {
			assert e instanceof SuException;
		}
	}

}
