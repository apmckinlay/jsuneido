package suneido.language.jsdi.type;

import static org.junit.Assert.*;
import static suneido.language.jsdi.type.BasicType.*;

import java.util.EnumSet;

import org.junit.Test;

import suneido.language.jsdi.MarshallPlan;
import suneido.language.jsdi.MarshallPlanBuilder;
import suneido.language.jsdi.Marshaller;

/**
 * Test for {@link BasicValue}.
 *
 * @author Victor Schappert
 * @since 20130718
 */
public class BasicValueTest {

	static BasicValue bv(BasicType basicType) {
		return new BasicValue(basicType);
	}

	//
	// TESTS for Marshalling basic values IN/OUT of native arguments
	//

	private static final class BasicTypeSet {
		public final BasicType type;
		public final Object[] values;
		public BasicTypeSet(BasicType type, Object... values) {
			this.type = type;
			this.values = values;
		}
	}
	private static final BasicTypeSet[] SETS = new BasicTypeSet[] {
		new BasicTypeSet(BOOL, Boolean.TRUE, Boolean.FALSE),
		new BasicTypeSet(CHAR, (int)Byte.MIN_VALUE, -1, 0, 1, (int)Byte.MAX_VALUE),
		new BasicTypeSet(SHORT, (int)Short.MIN_VALUE, -1, 0, 1, 0x01ff, (int)Short.MAX_VALUE),
		new BasicTypeSet(LONG, Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE),
		new BasicTypeSet(INT64, Long.MIN_VALUE, -1L, 0L, 1L, 0x01ffffffffL, Long.MAX_VALUE),
		new BasicTypeSet(FLOAT, Float.MIN_VALUE, Float.MIN_NORMAL,
				Float.NaN, Float.NEGATIVE_INFINITY,
				Float.POSITIVE_INFINITY, -1.0f, 0.0f, 1.0f),
		new BasicTypeSet(DOUBLE, Double.MIN_VALUE, Double.MIN_NORMAL,
				Double.NaN, Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY, -1.0, 0.0, 1.0),
		new BasicTypeSet(HANDLE, Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE),
		new BasicTypeSet(GDIOBJ, Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE)
	};

	@Test
	public void testMarshallInOutAll() {
		EnumSet<BasicType> typesSeen = EnumSet.noneOf(BasicType.class);
		for (BasicTypeSet bts : SETS) {
			assertFalse(typesSeen.contains(bts.type));
			BasicValue type = bv(bts.type);
			MarshallPlanBuilder builder = new MarshallPlanBuilder(
					type.getSizeDirectWholeWords(), 0, 0);
			type.addToPlan(builder, false);
			MarshallPlan mp = builder.makeMarshallPlan();
			for (Object value : bts.values) {
				Marshaller m = mp.makeMarshaller();
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

	//
	// TESTS for Marshalling basic values OUT from native return values
	//

	@Test
	public void testMarshallOutReturnValue() {
		assertEquals(Boolean.FALSE, bv(BOOL).marshallOutReturnValue(0));
		assertEquals(Boolean.FALSE, bv(BOOL).marshallOutReturnValue(0L));
		assertEquals(Boolean.TRUE, bv(BOOL).marshallOutReturnValue(1));
		assertEquals(Boolean.TRUE, bv(BOOL).marshallOutReturnValue(1L));
		assertEquals(0, bv(CHAR).marshallOutReturnValue(0));
		assertEquals(0, bv(CHAR).marshallOutReturnValue(0xf00));  // truncated
		assertEquals(0, bv(CHAR).marshallOutReturnValue(0L));
		assertEquals(0, bv(CHAR).marshallOutReturnValue(0xf00L)); // truncated
		assertEquals(-1, bv(CHAR).marshallOutReturnValue(0xff));
		assertEquals(-1, bv(CHAR).marshallOutReturnValue(-1));
		assertEquals(-1, bv(CHAR).marshallOutReturnValue(0xffL));
		assertEquals(-1, bv(CHAR).marshallOutReturnValue(-1L));
		assertEquals(0, bv(SHORT).marshallOutReturnValue(0));
		assertEquals(0, bv(SHORT).marshallOutReturnValue(0xf0000));  // truncated
		assertEquals(0, bv(SHORT).marshallOutReturnValue(0L));
		assertEquals(0, bv(SHORT).marshallOutReturnValue(0xf0000L)); // truncated
		assertEquals(-1, bv(SHORT).marshallOutReturnValue(0xffff));
		assertEquals(-1, bv(SHORT).marshallOutReturnValue(-1));
		assertEquals(-1, bv(SHORT).marshallOutReturnValue(0xffffL));
		assertEquals(-1, bv(SHORT).marshallOutReturnValue(-1L));
		for (BasicType type : new BasicType[] { LONG, HANDLE, GDIOBJ }) {
			assertEquals(0, bv(type).marshallOutReturnValue(0));
			assertEquals(0, bv(type).marshallOutReturnValue(0L));
			assertEquals(0, bv(type).marshallOutReturnValue(0xf00000000L)); // truncated
			assertEquals(-1, bv(type).marshallOutReturnValue(-1));
			assertEquals(-1, bv(type).marshallOutReturnValue(0x0ffffffffL));
			assertEquals(-1, bv(type).marshallOutReturnValue(-1L));
		}
		assertEquals(0L, bv(INT64).marshallOutReturnValue(0L));
		assertEquals(-1L, bv(INT64).marshallOutReturnValue(-1L));
		assertEquals(Long.MAX_VALUE, bv(INT64).marshallOutReturnValue(Long.MAX_VALUE));
		assertEquals(0.0f, bv(FLOAT).marshallOutReturnValue(Float.floatToRawIntBits(0.0f)));
		assertEquals(Float.MIN_VALUE, bv(FLOAT).marshallOutReturnValue(Float.floatToRawIntBits(Float.MIN_VALUE)));
		assertEquals(0.0f, bv(FLOAT).marshallOutReturnValue((long)Float.floatToRawIntBits(0.0f)));
		assertEquals(Float.MIN_VALUE, bv(FLOAT).marshallOutReturnValue((long)Float.floatToRawIntBits(Float.MIN_VALUE)));
		assertEquals(0.0, bv(DOUBLE).marshallOutReturnValue(Double.doubleToRawLongBits(0.0)));
		assertEquals(Double.MIN_VALUE, bv(DOUBLE).marshallOutReturnValue(Double.doubleToRawLongBits(Double.MIN_VALUE)));
	}

	@Test(expected=IllegalStateException.class)
	public void testMarshallOutReturnValueErrorInt64() {
		bv(INT64).marshallOutReturnValue(0);
	}

	@Test(expected=IllegalStateException.class)
	public void testMarshallOutReturnValueErrorDouble() {
		bv(DOUBLE).marshallOutReturnValue(0);
	}
}
