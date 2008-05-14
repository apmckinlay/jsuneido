package suneido;

import org.junit.Test;
import static org.junit.Assert.*;

public class SuDateTest {
	@Test
	public void test() {
		String s = "#20080514.143622123";
		SuDate d = new SuDate(s);
		assertEquals(s, d.toString());
	}
}
