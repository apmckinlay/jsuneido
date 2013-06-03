/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util; 

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RegexTest {

	@Test
	public void compile() {
		test("", "");
		test("a", "'a'");
		test("abc", "'abc'");
		test("^xyz", "^ 'xyz'");
		test("abc$", "'abc' $");
		test("^xyz$", "^ 'xyz' $");
		test("abc?de", "'ab' Branch(1) 'c' 'de'");
		test("abc+de", "'ab' 'c' Branch(-1) 'de'");
		test("abc*de", "'ab' Branch(2) 'c' Branch(-1) 'de'");
		test("(ab+c)+x", "'a' 'b' Branch(-1) 'c' Branch(-4) 'x'");
	}

	void test(String rx, String expected) {
		assertThat(Regex.compile(rx).toString().trim(), is(expected));
	}

	@Test
	public void amatch() {
		yes("", "");
		yes("abc", "a");
		no("abc", "x");
		no("ab", "abc");
		yes("abc", "abc");
		yes("abc", "^a");
		yes("abc", "^abc");
		yes("abc", "^abc$");
		no("abc", "^a$");
		no("abc", "^abcd$");

		yes("abc", "^...$");
		no("ab\n", "...");

		no("abde", "abc+de");
		yes("abcde", "abc+de");
		yes("abccde", "abc+de");
		no("abccd", "abc+de");

		yes("abde", "abc?de");
		yes("abcde", "abc?de");
		no("abccde", "abc?de");

		no("abe", "ab(cd)*ef");
		yes("abef", "ab(cd)*ef");
		yes("abcdef", "ab(cd)*ef");
		yes("abcdcdcdef", "ab(cd)*ef");
		no("abcdcdcde", "ab(cd)*ef");

		yes("abcx", "(ab*c)*x");
		yes("abbc", "(ab*c)*");
		yes("abcabc", "(ab*c)*");
		yes("acabbbc", "(ab*c)*");
		yes("abbbcac", "(ab*c)*");
		yes("acabcabbcx", "(ab*c)*x");
	}

	void yes(String s, String rx) {
		Regex.Pattern pat = Regex.compile(rx);
		assertTrue(rx + " => " + pat + " failed to match " + s,
				pat.amatch(s) >= 0);
	}

	void no(String s, String rx) {
		assertThat(Regex.compile(rx).amatch(s), lessThan(0));
	}

}
