package suneido.language.builtin;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.SuContainer;

public class StringMethodsTest {

	@Test
	public void test_split() {
		split("", " ");
		split("fred", " ", "fred");
		split("fred ", " ", "fred");
		split(" fred", " ", "", "fred");
		split("now is the time", " ", "now", "is", "the", "time");
		split("one<>two<>three", "<>", "one", "two", "three");
	}

	private void split(String s, String sep, String... list) {
		SuContainer c = StringMethods.split(s, sep);
		assertEquals(list.length, c.size());
		for (int i = 0; i < list.length; ++i)
			assertEquals(list[i], c.get(i));
	}

	@Test
	public void test_extract() {
		extract("hello world", ".....$", "world");
		extract("hello world", "w(..)ld", "or");
	}

	private void extract(String s, String pat, String result) {
		assertEquals(result, StringMethods.Extract(s, pat));
	}
}
