/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static suneido.jsdi.type.BasicValueTest.SETS;
import static suneido.jsdi.type.BasicValueTest.bv;

import java.util.EnumSet;

import org.junit.Test;

import suneido.jsdi.MarshallPlanBuilder;
import suneido.jsdi.type.BasicType;
import suneido.jsdi.type.BasicValue;
import suneido.jsdi.type.BasicValueTest.BasicTypeSet;

/**
 * x86-specific tests for {@link BasicValue}.
 *
 * @author Victor Schappert
 * @since 20140719
 * @see suneido.jsdi.type.BasicValueTest
 */
public class BasicValueTestX86 {

	//
	// TESTS for Marshalling basic values IN/OUT of native arguments
	//

	@Test
	public void testMarshallInOutAll() {
		EnumSet<BasicType> typesSeen = EnumSet.noneOf(BasicType.class);
		for (BasicTypeSet bts : SETS) {
			assertFalse(typesSeen.contains(bts.type));
			BasicValue type = bv(bts.type);
			MarshallPlanBuilder builder = new MarshallPlanBuilderX86(0, true);
			type.addToPlan(builder, false);
			MarshallPlanX86 mp = (MarshallPlanX86)builder.makeMarshallPlan();
			for (Object value : bts.values) {
				MarshallerX86 m = mp.makeMarshallerX86();
				type.marshallIn(m, value);
				m.rewind();
				assertEquals(value, type.marshallOut(m, null));
				m.rewind();
				assertEquals(value, type.marshallOut(m, value));
			}
			typesSeen.add(bts.type);
		}
		assertEquals(BasicType.values().length, typesSeen.size());
	}
}
