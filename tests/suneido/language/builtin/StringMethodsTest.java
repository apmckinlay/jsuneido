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

	@Test
	public void test_replace() {
		replace("now is the time", "is", "may be", "now may be the time");
		replace("now is the time", "t", "X", "now is Xhe Xime");
		replace("now is the time", "t", "X", 1, "now is Xhe time");
		replace("now is the time", "[a-z]+", "(&)", 2, "(now) (is) the time");
	}

	private void replace(String s, String pat, String rep, String result) {
		assertEquals(result, StringMethods.Replace(s, pat, rep));
	}
	private void replace(String s, String pat, String rep, int n, String result) {
		assertEquals(result, StringMethods.Replace(s, pat, rep, n));
	}

}
