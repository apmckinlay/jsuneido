/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static suneido.util.Tr.tr;

import org.junit.Test;

public class TrTest {
	@Test
	public void test() {
		assertThat(tr("", "", ""), equalTo(""));
		assertThat(tr("", "abc", "ABC"), equalTo(""));
		assertThat(tr("", "^abc", "x"), equalTo(""));

		assertThat(tr("abc", "", ""), equalTo("abc"));
		assertThat(tr("abc", "xyz", ""), equalTo("abc"));
		assertThat(tr("zon", "xyz", ""), equalTo("on"));
		assertThat(tr("oyn", "xyz", ""), equalTo("on"));
		assertThat(tr("nox", "xyz", ""), equalTo("no"));
		assertThat(tr("zyx", "xyz", ""), equalTo(""));

		assertThat(tr("zon", "xyz", "XYZ"), equalTo("Zon"));
		assertThat(tr("oyn", "xyz", "XYZ"), equalTo("oYn"));
		assertThat(tr("nox", "xyz", "XYZ"), equalTo("noX"));
		assertThat(tr("zyx", "xyz", "XYZ"), equalTo("ZYX"));
		assertThat(tr("zyx", "a-z", "A-Z"), equalTo("ZYX"));

		assertThat(tr("a b - c", "^abc", ""), equalTo("abc")); // allbut delete
		assertThat(tr("a b - c", "^a-z", ""), equalTo("abc")); // allbut delete
		assertThat(tr("a  b - c", "^abc", " "), equalTo("a b c")); // allbut collapse
		assertThat(tr("a  b - c", "^a-z", " "), equalTo("a b c")); // allbut collapse
		assertThat(tr("a-b-c", "-x", ""), equalTo("abc")); // literal dash
		assertThat(tr("a-b-c", "x-", ""), equalTo("abc")); // literal dash

		// collapse at end
		assertThat(tr("hello \t\n\n", " \t\n", "\n"), equalTo("hello\n"));
		}

	/**
	 * PortTests fixture.
	 * Takes four arguments: string, from, to, and expected result.
	 */
	public static boolean pt_tr(String... args) {
		return Tr.tr(args[0], args[1], args[2]).equals(args[3]);
	}
}
