/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.abi.x86;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static suneido.language.jsdi.type.BasicValueTest.SETS;
import static suneido.language.jsdi.type.BasicValueTest.bv;

import java.util.EnumSet;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.language.jsdi.MarshallPlanBuilder;
import suneido.language.jsdi.type.BasicType;
import suneido.language.jsdi.type.BasicValue;
import suneido.language.jsdi.type.BasicValueTest.BasicTypeSet;
import suneido.util.testing.Assumption;

/**
 * x86-specific tests for {@link BasicValue}.
 *
 * @author Victor Schappert
 * @since 20140719
 * @see suneido.language.jsdi.type.BasicValueTest
 */
public class BasicValueTestX86 {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIs32BitOnWindows();
	}

	//
	// TESTS for Marshalling basic values IN/OUT of native arguments
	//

	@Test
	public void testMarshallInOutAll() {
		EnumSet<BasicType> typesSeen = EnumSet.noneOf(BasicType.class);
		for (BasicTypeSet bts : SETS) {
			assertFalse(typesSeen.contains(bts.type));
			BasicValue type = bv(bts.type);
			MarshallPlanBuilder builder = new MarshallPlanBuilderX86(
					type.getSizeDirectWholeWords(), 0, 0, true);
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
