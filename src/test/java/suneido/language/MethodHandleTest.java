/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static org.junit.Assert.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class MethodHandleTest {

	static Object func(Object... args) {
		return args[0];
	}

	public static void main(String[] args)
			throws Throwable {
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		MethodType mt = MethodType.methodType(Object.class, Object[].class);
		MethodHandle mh = lookup.findStatic(MethodHandleTest.class, "func", mt);
		assertTrue(mh.isVarargsCollector());
		assertEquals("hello", mh.invoke("hello", "world"));
	}

}
