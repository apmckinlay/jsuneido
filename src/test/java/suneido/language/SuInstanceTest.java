package suneido.language;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Test for {@link SuInstance}.
 *
 * @author Victor Schappert
 * @since 20130814
 */
public class SuInstanceTest {

	@Test
	public void testHashCode() {
		final SuClass a = new SuClass("a", "a", null);
		SuInstance a1 = new SuInstance(a);
		SuInstance a2 = new SuInstance(a);
		assertEquals(a1.hashCode(), a2.hashCode());
		a1.put("key", a1); // self-reference
		a2.put("key", a2); // self-reference
		assertEquals(a1.hashCode(), a1.hashCode()); 
		assertEquals(a2.hashCode(), a2.hashCode());
		assertEquals(a1, a2);
		assertEquals(a1.hashCode(), a2.hashCode());
	}
}
