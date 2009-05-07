package suneido.util;

import static org.junit.Assert.assertEquals;
import static suneido.util.Tr.tr;

import org.junit.Test;

public class TrTest {
	@Test
	public void test() {
		assertEquals("", tr("", "", ""));
		assertEquals("", tr("", "abc", "ABC"));
		assertEquals("", tr("", "^abc", ""));
		assertEquals("CAB", tr("cab", "abc", "ABC"));
		assertEquals("CAB", tr("cab", "a-z", "A-Z"));
		assertEquals("abc", tr("a b - c", "^abc", ""));
		assertEquals("a b c", tr("a  b - c", "^abc", " "));
	}
}
