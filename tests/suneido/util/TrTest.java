/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static suneido.util.Tr.tr;

import org.junit.Test;

public class TrTest {
	@Test
	public void test() {
		assertThat(tr("", "", ""), is(""));
		assertThat(tr("", "abc", "ABC"), is(""));
		assertThat(tr("", "^abc", "x"), is(""));

		assertThat(tr("abc", "", ""), is("abc"));
		assertThat(tr("abc", "xyz", ""), is("abc"));
		assertThat(tr("zon", "xyz", ""), is("on"));
		assertThat(tr("oyn", "xyz", ""), is("on"));
		assertThat(tr("nox", "xyz", ""), is("no"));
		assertThat(tr("zyx", "xyz", ""), is(""));

		assertThat(tr("zon", "xyz", "XYZ"), is("Zon"));
		assertThat(tr("oyn", "xyz", "XYZ"), is("oYn"));
		assertThat(tr("nox", "xyz", "XYZ"), is("noX"));
		assertThat(tr("zyx", "xyz", "XYZ"), is("ZYX"));
		assertThat(tr("zyx", "a-z", "A-Z"), is("ZYX"));

		assertThat(tr("a b - c", "^abc", ""), is("abc")); // allbut delete
		assertThat(tr("a b - c", "^a-z", ""), is("abc")); // allbut delete
		assertThat(tr("a  b - c", "^abc", " "), is("a b c")); // allbut collapse
		assertThat(tr("a  b - c", "^a-z", " "), is("a b c")); // allbut collapse
		assertThat(tr("a-b-c", "-x", ""), is("abc")); // literal dash
		assertThat(tr("a-b-c", "x-", ""), is("abc")); // literal dash

		// collapse at end
		assertThat(tr("hello \t\n\n", " \t\n", "\n"), is("hello\n"));
		}
}
