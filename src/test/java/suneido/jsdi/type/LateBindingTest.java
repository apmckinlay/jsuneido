/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.jsdi.DllInterface;
import suneido.jsdi.SimpleContext;
import suneido.jsdi.StorageType;
import suneido.jsdi._64BitIssue;
import suneido.jsdi.marshall.PrimitiveSize;
import suneido.language.ContextLayered;
import suneido.util.testing.Assumption;

/**
 * Test for {@link LateBinding}.
 *
 * @author Victor Schappert
 * @since 20140730
 */
@DllInterface
public class LateBindingTest {

	@BeforeClass
	@_64BitIssue // This should be relaxed to jvmIsOnWindows()
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIs32BitOnWindows();
	}

	private static final String[] NAMED_TYPES = {
		"INT8", "struct { int8 x }"
	};
	private static ContextLayered CONTEXT = new SimpleContext(NAMED_TYPES);

	@Test
	public void testAlign() throws BindException {
		final LateBinding lbValue = new LateBinding(CONTEXT,
				CONTEXT.slotForName("INT8"), StorageType.VALUE, 1);
		assertEquals(true, lbValue.bind(0));
		assertEquals(PrimitiveSize.INT8, lbValue.getAlignDirect());
		final LateBinding lbArray = new LateBinding(CONTEXT,
				CONTEXT.slotForName("INT8"), StorageType.ARRAY, 5);
		assertEquals(true, lbArray.bind(0));
		assertEquals(PrimitiveSize.INT8, lbArray.getAlignDirect());
		final LateBinding lbPointer = new LateBinding(CONTEXT,
				CONTEXT.slotForName("INT8"), StorageType.POINTER, 1);
		assertEquals(true, lbPointer.bind(0));
		assertEquals(PrimitiveSize.POINTER, lbPointer.getAlignDirect());
	}

}
