package suneido.language.builtin;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.SuContainer;

public class StringMethodsTest {

	@Test
	public void test_split() {
		test("", " ");
		test("fred", " ", "fred");
		test("fred ", " ", "fred");
		test(" fred", " ", "", "fred");
		test("now is the time", " ", "now", "is", "the", "time");
		test("one<>two<>three", "<>", "one", "two", "three");
	}

	private void test(String s, String sep, String... list) {
		SuContainer c = StringMethods.split(s, sep);
		assertEquals(list.length, c.size());
		for (int i = 0; i < list.length; ++i)
			assertEquals(list[i], c.get(i));
	}
}
