/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static org.junit.Assert.fail;

import org.junit.Test;

public class AssertTest {

	@Test
	public void test_assert_enabled() {
		try {
			assert false;
		} catch (AssertionError e) {
			return;
		}
		fail("assert not enabled");
	}

}
