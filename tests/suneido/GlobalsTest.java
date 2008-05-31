package suneido;

import org.junit.Test;
import static org.junit.Assert.*;

public class GlobalsTest {
	@Test
	public void num() {
		String s1 = "akldfjaklsdfjsklfjsdkj";
		String s2 = "dlkjfdklsjfkljkldsjdsk";
		int n = Globals.size();
		int i1 = Globals.num(s1);
		assertEquals(n + 1, Globals.size());
		assertEquals(i1, Globals.num(s1));
		assertEquals(n + 1, Globals.size());
		int i2 = Globals.num(s2);
		assertEquals(n + 2, Globals.size());
		assertEquals(i1, Globals.num(s1));
		assertEquals(i2, Globals.num(s2));
		assertEquals(n + 2, Globals.size());
	}
	
	@Test
	public void getset() {
		int i = Globals.num("dkjfkdjfkdjdjfjfur");
		assertEquals(null, Globals.get(i));
		Globals.set(i, SuInteger.ONE);
		assertEquals(SuInteger.ONE, Globals.get(i));
	}
}
