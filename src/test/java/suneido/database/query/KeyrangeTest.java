/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static org.junit.Assert.assertEquals;
import static suneido.Suneido.dbpkg;

import org.junit.Test;

import suneido.intfc.database.Record;

public class KeyrangeTest {
	@Test
	public void intersect() {
		Record a = dbpkg.recordBuilder().add("a").build();
		Record b = dbpkg.recordBuilder().add("b").build();
		Record c = dbpkg.recordBuilder().add("c").build();

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
