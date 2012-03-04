/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class LocksTest {

	@Test
	public void test() {
		Database db = DatabasePackage.dbpkg.testdb();
		UpdateTransaction t1 = db.updateTransaction();
		UpdateTransaction t2 = db.updateTransaction();
		UpdateTransaction t3 = db.updateTransaction();

		Locks locks = new Locks();

		locks.checkEmpty();
		assertTrue(locks.isEmpty());
		assertEquals(locks.toString(), "Locks{ }");

		assertNull(locks.addRead(t1, 123));
		assertFalse(locks.isEmpty());
		assertNull(locks.addRead(t1, 123));
		assertNull(locks.addRead(t1, 456));

		locks.remove(t1);
		assertTrue(locks.isEmpty());

		assertTrue(locks.addWrite(t1, 123).isEmpty());
		assertTrue(locks.addWrite(t1, 123).isEmpty());

		assertTrue(locks.addWrite(t2, 456).isEmpty());

		assertSame(t1, locks.addRead(t2, 123));
		assertSame(t2, locks.addRead(t1, 456));

		assertNull(locks.addRead(t1, 789));
		assertNull(locks.addRead(t2, 789));
		assertEquals(ImmutableSet.of(t1, t2), locks.addWrite(t3, 789));

		try {
			locks.addWrite(t2, 123);
			fail();
		} catch (RuntimeException e) {
			assertThat(e.toString(), containsString("conflict"));
		}

		locks.remove(t1);
		locks.remove(t3);
		locks.remove(t2);
		assertTrue(locks.isEmpty());
	}

}
