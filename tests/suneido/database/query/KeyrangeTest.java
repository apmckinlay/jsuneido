package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.database.Record;


public class KeyrangeTest {
	@Test
	public void intersect() {
		Record a = new Record().add("a");
		Record b = new Record().add("b");
		Record c = new Record().add("c");

		Keyrange x = new Keyrange(a, b);
		Keyrange all = new Keyrange();

		assertEquals(x, Keyrange.intersect(x, all));
		assertEquals(x, Keyrange.intersect(all, x));

		Keyrange none = new Keyrange().setNone();
		assertEquals(none, Keyrange.intersect(x, none));
		assertEquals(none, Keyrange.intersect(none, x));

		Keyrange y = new Keyrange(b, c);
		assertEquals(new Keyrange(b, b), Keyrange.intersect(x, y));
		assertEquals(new Keyrange(b, b), Keyrange.intersect(y, x));
	}
}
