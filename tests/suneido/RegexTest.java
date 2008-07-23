package suneido;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.Test;

public class RegexTest {

	@Test
	public void getPat() {
		Pattern p1 = Regex.getPat("x");
		Pattern p2 = Regex.getPat("a.*b");
		assertSame(p1, Regex.getPat("x"));
		assertSame(p2, Regex.getPat("a.*b"));
	}

	@Test
	public void contains() {
		String truecases[] = new String[] {
				"abc", "b",
				"abc", "a.c",
				"a", "[[:alnum:]]",
				"hello", "\\<hello\\>",
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
}
