/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static suneido.util.Tabs.detab;
import static suneido.util.Tabs.entab;

import org.junit.Test;

public class TabsTest {

	@Test
	public void test_detab() {
		assertThat(detab("", 4), equalTo(""));
		assertThat(detab("foo bar", 4), equalTo("foo bar"));
		assertThat(detab("\n", 4), equalTo("\n"));
		assertThat(detab("\tx\ty", 4), equalTo("    x   y"));
		assertThat(detab("\tx\n\ty", 4), equalTo("    x\n    y"));
		assertThat(detab("\tx\r\n\ty", 4), equalTo("    x\r\n    y"));
	}

	@Test
	public void test_entab() {
		assertThat(entab("", 4), equalTo(""));
		assertThat(entab("foo bar", 4), equalTo("foo bar"));
		assertThat(entab("\n", 4), equalTo("\n"));
		assertThat(entab("    x", 4), equalTo("\tx"));
		assertThat(entab("    x   y", 4), equalTo("\tx   y"));
		assertThat(entab("     x", 4), equalTo("\t x"));
		assertThat(entab("    x\n    y", 4), equalTo("\tx\n\ty"));
		assertThat(entab("    x\r\n    y", 4), equalTo("\tx\r\n\ty"));
		assertThat(entab("foo bar \t \n", 4), equalTo("foo bar\n"));
		assertThat(entab("foo bar \t \r\n", 4), equalTo("foo bar\r\n"));
		assertThat(entab("foo bar \t \r\n    x", 4), equalTo("foo bar\r\n\tx"));
	}

}
