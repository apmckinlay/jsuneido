/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import static org.junit.Assert.*;

import org.junit.Test;

import suneido.jsdi.marshall.PrimitiveSize;

/**
 * Test for {@code ResourceType}.
 *
 * @author Victor Schappert
 * @since 20140806
 */
public class ResourceTest {

	@Test
	public void testSizeDirect() {
		assertEquals(PrimitiveSize.POINTER, ResourceType.INSTANCE.getSizeDirect());
	}

	@Test
	public void testAlignDirect() {
		assertEquals(PrimitiveSize.POINTER, ResourceType.INSTANCE.getAlignDirect());
	}
}
