/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.Test;

import suneido.language.builtin.StringMethods;

public class RegexTest {

	@Test
	public void convertRegex() {
		convertTest("[[:alnum:]]", "[\\p{Alnum}]");
		convertTest("foo[", "foo\\[");
		convertTest("foo(", "foo\\(");
		convertTest("[$]", "[\\$]");
	}
	
	private static void convertTest(String from, String to) {
		String converted = Regex.convertRegex(from);
		assertThat(converted, is(to));
		Pattern.compile(converted); // throws if bad syntax	
	}

	@Test
	public void getPat() {
		Pattern p1 = Regex.getPat("x", "");
		Pattern p2 = Regex.getPat("a.*b", "");
		assertSame(p1, Regex.getPat("x", ""));
		assertSame(p2, Regex.getPat("a.*b", ""));
	}

	@Test
	public void contains() {
		String truecases[] = new String[] {
				"abc", "b",
				"abc", "a.c",
				"a", "[[:alnum:]]",
				"hello", "\\<hello\\>",
				"-", "[-x]",
				"-", "[x-]",
				"[", "[[]",
				"]", "]",
				"[", "[",
				")", ")",
				"))", "))",
				};
		for (int i = 0; i < truecases.length; i += 2)
			assertTrue(truecases[i] + " =~ " + truecases[i + 1],
					Regex.contains(truecases[i], truecases[i + 1]));
		String falsecases[] = new String[] {
				"abc", "d",
				"abc", "(?q).",
				"hello", "\\<hell\\>",
				};
		for (int i = 0; i < falsecases.length; i += 2)
			assertFalse(falsecases[i] + " !~ " + falsecases[i + 1],
					Regex.contains(falsecases[i], falsecases[i + 1]));
	}

	@Test
	public void start() {
		assertTrue(Pattern.compile("^").matcher("").find());
		assertFalse(Pattern.compile("^", Pattern.MULTILINE).matcher("").find());
		assertFalse(Pattern.compile("(?m)^").matcher("").find());

		assertTrue(Regex.contains("", "^"));
	}

	@Test
	public void escaping() {
		assertTrue(Regex.contains("_._", "_\\._"));
		assertTrue(Regex.contains("_(_", "_\\(_"));
		assertTrue(Regex.contains("_)_", "_\\)_"));
		assertTrue(Regex.contains("_[_", "_\\[_"));
		assertTrue(Regex.contains("_]_", "_\\]_"));
		assertTrue(Regex.contains("_*_", "_\\*_"));
		assertTrue(Regex.contains("_+_", "_\\+_"));
		assertTrue(Regex.contains("_?_", "_\\?_"));
		assertTrue(Regex.contains("_|_", "_\\|_"));
		assertTrue(Regex.contains("_^_", "_\\^_"));
		assertTrue(Regex.contains("_$_", "_\\$_"));

		assertTrue(Regex.contains("_&_", "_\\&_"));
	}

	@Test
	public void replace() {
		assertThat(StringMethods.replace("a&b", "\\&", "X", 1), is("aXb"));
	}

}
