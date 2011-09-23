/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import static org.junit.Assert.*;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class LocksTest {

	@Test
	public void test() {
		Transaction t1 = new Transaction();
		Transaction t2 = new Transaction();
		Transaction t3 = new Transaction();

		Locks locks = new Locks();

		assertTrue(locks.isEmpty());

		assertNull(locks.addRead(t1, 123));
		assertFalse(locks.isEmpty());
		assertNull(locks.addRead(t1, 123));
		assertNull(locks.addRead(t1, 456));

		locks.remove(t1);
		assertTrue(locks.isEmpty());

		assertTrue(locks.addWrite(t1, 123).isEmpty());
		assertTrue(locks.addWrite(t1, 123).isEmpty());

		assertNull(locks.addWrite(t2, 123));
		assertTrue(locks.addWrite(t2, 456).isEmpty());

		assertSame(t1, locks.addRead(t2, 123));
		assertSame(t2, locks.addRead(t1, 456));

		assertNull(locks.addRead(t1, 789));
		assertNull(locks.addRead(t2, 789));
		assertEquals(ImmutableSet.of(t1, t2), locks.addWrite(t3, 789));

		locks.remove(t1);
		locks.remove(t3);
		locks.remove(t2);
		assertTrue(locks.isEmpty());
	}

}
