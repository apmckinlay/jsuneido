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
		test(".", "CharMatcher.anyOf(\"\\u000D\\u000A\").negate()");
		test("a", "'a'");
		test("abc", "'abc'");
		test("^xyz", "^ 'xyz'");
		test("abc$", "'abc' $");
		test("^xyz$", "^ 'xyz' $");
		test("abc?de", "'ab' Branch(1, 2) 'c' 'de'");
		test("abc+de", "'ab' 'c' Branch(-1, 1) 'de'");
		test("abc*de", "'ab' Branch(1, 3) 'c' Branch(-1, 1) 'de'");
		test("(ab+c)+x", "'a' 'b' Branch(-1, 1) 'c' Branch(-4, 1) 'x'");
		test("ab|cd",
				"Branch(1, 3) 'ab' Jump(2) 'cd'");
		test("ab|cd|ef",
				"Branch(1, 3) 'ab' Jump(3) Branch(1, 3) 'cd' Jump(2) 'ef'");
		test("abc\\Z", "'abc' \\Z");
		test("[a]", "'a'");
	}

	void test(String rx, String expected) {
		assertThat(Regex.compile(rx).toString().trim(), is(expected));
	}

	@Test
	public void amatch() {
		amatch("", "");
		amatch("abc", "a", 1);
		noamatch("abc", "x");
		noamatch("ab", "abc");
		amatch("abc", "abc");
		amatch("abc", "^a", 1);
		amatch("abc", "^abc");
		amatch("abc", "^abc$");
		noamatch("abc", "^a$");
		noamatch("abc", "^abcd$");

		amatch("abc", "^...$");
		noamatch("ab\n", "...");

		noamatch("abde", "abc+de");
		amatch("abcde", "abc+de");
		amatch("abccde", "abc+de");
		noamatch("abccd", "abc+de");

		amatch("abde", "abc?de");
		amatch("abcde", "abc?de");
		noamatch("abccde", "abc?de");

		noamatch("abe", "ab(cd)*ef");
		amatch("abef", "ab(cd)*ef");
		amatch("abcdef", "ab(cd)*ef");
		amatch("abcdcdcdef", "ab(cd)*ef");
		noamatch("abcdcdcde", "ab(cd)*ef");

		amatch("abcx", "(ab*c)*x");
		amatch("abbc", "(ab*c)*");
		amatch("abcabc", "(ab*c)*");
		amatch("acabbbc", "(ab*c)*");
		amatch("abbbcac", "(ab*c)*");
		amatch("acabcabbcx", "(ab*c)*x");

		amatch("a", "a|b");
		amatch("b", "a|b");
		noamatch("x", "a|b");
		noamatch("", "a|b");

		amatch("a", "a|b|c");
		amatch("b", "a|b|c");
		amatch("c", "a|b|c");
		noamatch("x", "a|b|c");
		noamatch("", "a|b|c");

		amatch("a", "a?", 1);
		amatch("a", "a??", 0);
		amatch("ab", "a?b");
		amatch("ab", "a??b");

		amatch("aaab", "a*", 3);
		amatch("aaab", "a*?", 0);
		amatch("aaab", "a+?", 1);
		amatch("aaab", "a*b");
		amatch("aaab", "a*?b");
		amatch("aaab", "a+?b");
	}

	@Test
	public void charClass() {
		amatch("a", "[abc]");
		amatch("b", "[abc]");
		amatch("c", "[abc]");
		noamatch("b", "[^abc]");
		amatch("x", "[^abc]");

		amatch("c", "\\w");
		amatch(" ", "\\W");
		noamatch(" ", "\\w");
		amatch(" ", "\\s");
		amatch("c", "\\S");
		noamatch("c", "\\s");

		amatch("c", "[\\w]");
		amatch(" ", "[\\W]");
		noamatch(" ", "[\\w]");
		amatch(" ", "[\\s]");
		amatch("c", "[\\S]");
		noamatch("c", "[\\s]");

		amatch("b", "[[:alpha:]]");
		amatch("b", "[[:alnum:]]");
		amatch("b", "[[:print:]]");
		amatch("b", "[[:graph:]]");
		amatch("b", "[[:lower:]]");
		noamatch("b", "[[:upper:]]");
		amatch("B", "[[:upper:]]");
		amatch("5", "[[:digit:]]");
		amatch("5", "[[:alnum:]]");
		noamatch("5", "[[:alpha:]]");
		noamatch("5", "[[:lower:]]");
		noamatch("5", "[[:upper:]]");
	}

	@Test
	public void match() {
		match("hello\nworld", "^he");
		match("hello\nworld", "^wo");
		match("hello\nworld", "\\Ahe");
		nomatch("hello\nworld", "\\Awo");

		match("hello\nworld", "ld$");
		match("hello\nworld", "lo$");
		match("hello\nworld", "ld\\Z");
		nomatch("hello\nworld", "lo\\Z");
	}

	void match(String s, String rx) {
		Regex.Pattern pat = Regex.compile(rx);
		assertTrue(rx + " => " + pat + " failed to match " + s,
				pat.match(s));
	}
	void nomatch(String s, String rx) {
		Regex.Pattern pat = Regex.compile(rx);
		assertTrue(rx + " => " + pat + " shouldn't match " + s,
				! pat.match(s));
	}

	void amatch(String s, String rx) {
		amatch(s, rx, s.length());
	}

	void amatch(String s, String rx, int len) {
		Regex.Pattern pat = Regex.compile(rx);
		assertTrue(rx + " => " + pat + " failed to match " + s,
				pat.amatch(s) == len);
	}

	void noamatch(String s, String rx) {
		assertThat(Regex.compile(rx).amatch(s), lessThan(0));
	}

}
