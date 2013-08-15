package suneido.language.jsdi.dll;

import static suneido.language.jsdi.MarshallTestUtil.pointerPlan;
import static suneido.language.jsdi.VariableIndirectInstruction.NO_ACTION;
import suneido.language.jsdi.*;
import suneido.language.jsdi.type.PrimitiveSize;

/**
 * Should be kept in sync with {@code test_exports.h/cpp}.
 * 
 * @author Victor Schappert
 * @since 20130723
 * @see QuickDll
 */
public enum TestCall {

	VOID("TestVoid", Mask.VOID, 0),
	CHAR("TestChar", Mask.CHAR, PrimitiveSize.CHAR),
	SHORT("TestShort", Mask.SHORT, PrimitiveSize.SHORT),
	LONG("TestLong", Mask.LONG, PrimitiveSize.LONG),
	INT64("TestInt64", Mask.INT64, PrimitiveSize.INT64),
	RETURN1_0FLOAT("TestReturn1_0Float", Mask.DOUBLE, 0),
	RETURN1_0DOUBLE("TestReturn1_0Double", Mask.DOUBLE, 0),
	FLOAT("TestFloat", Mask.DOUBLE, PrimitiveSize.FLOAT),
	DOUBLE("TestDouble", Mask.DOUBLE, PrimitiveSize.DOUBLE),
	REMOVE_SIGN_FROM_LONG("TestRemoveSignFromLong", Mask.INT64,
			PrimitiveSize.LONG),
	SUM_TWO_CHARS("TestSumTwoChars", Mask.CHAR,
			PrimitiveSize.CHAR, PrimitiveSize.CHAR),
	SUM_TWO_SHORTS("TestSumTwoShorts", Mask.SHORT,
			PrimitiveSize.SHORT, PrimitiveSize.SHORT),
	SUM_TWO_LONGS("TestSumTwoLongs", Mask.LONG,
			PrimitiveSize.LONG, PrimitiveSize.LONG),
	SUM_TWO_FLOATS("TestSumTwoFloats", Mask.DOUBLE,
			PrimitiveSize.FLOAT, PrimitiveSize.FLOAT),
	SUM_TWO_DOUBLES("TestSumTwoDoubles", Mask.DOUBLE,
			PrimitiveSize.DOUBLE, PrimitiveSize.DOUBLE),
	SUM_THREE_LONGS("TestSumThreeLongs", Mask.LONG,
			PrimitiveSize.LONG, PrimitiveSize.LONG, PrimitiveSize.LONG),
	SUM_FOUR_LONGS("TestSumFourLongs", Mask.LONG,
			PrimitiveSize.LONG, PrimitiveSize.LONG, PrimitiveSize.LONG,
			PrimitiveSize.LONG),
	SUM_CHAR_PLUS_INT64("TestSumCharPlusInt64", Mask.INT64,
			PrimitiveSize.CHAR, PrimitiveSize.INT64),
	SUM_PACKED_CHAR_CHAR_SHORT_LONG("TestSumPackedCharCharShortLong", Mask.LONG,
			makePackedCharCharShortLongPlan()),
	STRLEN("TestStrLen", Mask.LONG, makeInStringPlan()),
	HELLO_WORLD_RETURN("TestHelloWorldReturn", Mask.LONG,
			makeHelloWorldReturnPlan(), PrimitiveSize.BOOL),
	HELLO_WORLD_OUT_PARAM("TestHelloWorldOutParam", Mask.VOID,
			pointerPlan(PrimitiveSize.WORD)),
	HELLO_WORLD_OUT_BUFFER("TestHelloWorldOutBuffer", Mask.VOID,
			makeHelloWorldOutBufferPlan()),
	NULL_PTR_OUT_PARAM("TestNullPtrOutParam", Mask.VOID,
			pointerPlan(PrimitiveSize.WORD)),
	RETURN_PTR_PTR_PTR_DOUBLE("TestReturnPtrPtrPtrDoubleAsUInt64", Mask.INT64,
			makePtrPtrPtrDoublePlan()),
	SUM_STRING("TestSumString", Mask.LONG, makeSumStringPlan_TwoTier()),
	SUM_RESOURCE("TestSumResource", Mask.LONG, makeSumResourcePlan()),
	SWAP("TestSwap", Mask.LONG, makeSwapPlan()),
	RETURN_STRING("TestReturnString", Mask.VOID, makeReturnStringPlan(),
			PrimitiveSize.POINTER),
	RETURN_PTR_STRING("TestReturnPtrString", Mask.VOID,
			makeReturnPtrStringPlan(), PrimitiveSize.POINTER),
	RETURN_STRING_OUT_BUFFER(
			"TestReturnStringOutBuffer", Mask.VOID,
			makeReturnStringOutBufferPlan(), 2 * PrimitiveSize.POINTER
					+ PrimitiveSize.LONG);
	
	private final QuickDll qp;
	public final long ptr;
	public final Mask returnValueMask;
	public final MarshallPlan plan;

	private TestCall(String funcName, Mask returnValueMask, int... paramSizes) {
		this(funcName, returnValueMask, MarshallTestUtil.paramPlan(paramSizes));
	}

	private TestCall(String funcName, Mask returnValueMask,
			MarshallPlan marshallPlan) {
		this(funcName, returnValueMask, marshallPlan, marshallPlan
				.getSizeDirect());
	}

	private TestCall(String funcName, Mask returnValueMask,
			MarshallPlan marshallPlan, int paramSize) {
		qp = new QuickDll("jsdi", '_' + funcName + "@"
				+ PrimitiveSize.sizeWholeWords(paramSize));
		ptr = qp.ptr;
		this.returnValueMask = returnValueMask;
		this.plan = marshallPlan;
	}

	private static MarshallPlan makePackedCharCharShortLongPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilder(
				PrimitiveSize.sizeWholeWords(PrimitiveSize.CHAR
						+ PrimitiveSize.CHAR + PrimitiveSize.SHORT
						+ PrimitiveSize.LONG), 0, 0, true);
		builder.containerBegin();
		builder.pos(PrimitiveSize.CHAR);
		builder.pos(PrimitiveSize.CHAR);
		builder.pos(PrimitiveSize.SHORT);
		builder.pos(PrimitiveSize.LONG);
		builder.containerEnd();
		return builder.makeMarshallPlan();
	}

	private static MarshallPlan makeInStringPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilder(
			PrimitiveSize.pointerWholeWordBytes(),0, 1, true);
		builder.variableIndirectPtr();
		return builder.makeMarshallPlan();
	}

	private static MarshallPlan makePtrPtrPtrDoublePlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilder(
				PrimitiveSize.pointerWholeWordBytes(),
				2 * PrimitiveSize.POINTER + PrimitiveSize.DOUBLE, 0, true
		);
		builder.ptrBegin(PrimitiveSize.POINTER);
		builder.ptrBegin(PrimitiveSize.POINTER);
		builder.ptrBasic(PrimitiveSize.DOUBLE);
		builder.ptrEnd();
		builder.ptrEnd();
		return builder.makeMarshallPlan();
	}

	private static MarshallPlan makeHelloWorldOutBufferPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilder(
				PrimitiveSize.pointerWholeWordBytes()
						+ PrimitiveSize.sizeWholeWords(PrimitiveSize.LONG), 0,
				1, true);
		builder.variableIndirectPtr();
		builder.pos(PrimitiveSize.LONG);
		return builder.makeMarshallPlan();
	}

	private static MarshallPlan makeSumStringPlan_TwoTier() {
		final int sizeOf_RecursiveStringSum = (2
				* (PrimitiveSize.CHAR + PrimitiveSize.CHAR
						+ PrimitiveSize.SHORT + PrimitiveSize.LONG) + 3
				* PrimitiveSize.POINTER + PrimitiveSize.LONG);
		MarshallPlanBuilder builder = new MarshallPlanBuilder(
				PrimitiveSize.pointerWholeWordBytes(),
				2 * sizeOf_RecursiveStringSum,
				2 * (2), true);
		builder.ptrBegin(sizeOf_RecursiveStringSum);
			builder.containerBegin();              // struct RecursiveStringSum
				builder.arrayBegin();
					builder.containerBegin();          // struct Packed_CharCharShortLong
					builder.pos(PrimitiveSize.CHAR);
					builder.pos(PrimitiveSize.CHAR);
					builder.pos(PrimitiveSize.SHORT);
					builder.pos(PrimitiveSize.LONG);
					builder.containerEnd();
					builder.containerBegin();          // struct Packed_CharCharShortLong
					builder.pos(PrimitiveSize.CHAR);
					builder.pos(PrimitiveSize.CHAR);
					builder.pos(PrimitiveSize.SHORT);
					builder.pos(PrimitiveSize.LONG);
					builder.containerEnd();
				builder.arrayEnd();
				builder.variableIndirectPtr();
				builder.variableIndirectPtr();
				builder.pos(PrimitiveSize.LONG);
				builder.ptrBegin(sizeOf_RecursiveStringSum);
					builder.containerBegin();
						builder.arrayBegin();
							builder.containerBegin();          // struct Packed_CharCharShortLong
							builder.pos(PrimitiveSize.CHAR);
							builder.pos(PrimitiveSize.CHAR);
							builder.pos(PrimitiveSize.SHORT);
							builder.pos(PrimitiveSize.LONG);
							builder.containerEnd();
							builder.containerBegin();          // struct Packed_CharCharShortLong
							builder.pos(PrimitiveSize.CHAR);
							builder.pos(PrimitiveSize.CHAR);
							builder.pos(PrimitiveSize.SHORT);
							builder.pos(PrimitiveSize.LONG);
							builder.containerEnd();
						builder.arrayEnd();
						builder.variableIndirectPtr();
						builder.variableIndirectPtr();
						builder.pos(PrimitiveSize.LONG);
						builder.pos(PrimitiveSize.POINTER);
					builder.containerEnd();
				builder.ptrEnd();
			builder.containerEnd();
		builder.ptrEnd();
		return builder.makeMarshallPlan();
	}

	public static class Recursive_StringSum
	{
		public byte   a1;
		public byte   b1;
		public short  c1;
		public int    d1;
		public byte   a2;
		public byte   b2;
		public short  c2;
		public int    d2;
		public String str;
		public Buffer buffer;
		public Recursive_StringSum(String str, Buffer buffer, int... x)
		{
			this.str = str;
			this.buffer = buffer;
			switch (x.length)
			{
			default: this.d2 = (int)  x[7];
			case 7:  this.c2 = (short)x[6];
			case 6:  this.b2 = (byte) x[5];
			case 5:  this.a2 = (byte) x[4];
			case 4:  this.d1 = (int)  x[3];
			case 3:  this.c1 = (short)x[2];
			case 2:  this.b1 = (byte) x[1];
			case 1:  this.a1 = (byte) x[0];
			case 0:  break;
			}
		}
	}

	public static Marshaller marshall(Recursive_StringSum rssOuter,
			Recursive_StringSum rssInner) {
		Marshaller m = TestCall.SUM_STRING.plan.makeMarshaller();
		m.putPtr();
		m.putChar(rssOuter.a1);
		m.putChar(rssOuter.b1);
		m.putShort(rssOuter.c1);
		m.putLong(rssOuter.d1);
		m.putChar(rssOuter.a2);
		m.putChar(rssOuter.b2);
		m.putShort(rssOuter.c2);
		m.putLong(rssOuter.d2);
		if (null != rssOuter.str) {
			m.putStringPtr(rssOuter.str, NO_ACTION);
		} else {
			m.putNullStringPtr(NO_ACTION);
		}
		if (null != rssOuter.buffer) {
			m.putStringPtr(rssOuter.buffer, NO_ACTION);
			m.putLong(rssOuter.buffer.length());
		} else {
			m.putNullStringPtr(NO_ACTION);
			m.putLong(0);
		}
		if (null == rssInner) {
			m.putNullPtr();
			m.skipBasicArrayElements(8);
			m.putNullStringPtr(NO_ACTION);
			m.putNullStringPtr(NO_ACTION);
			m.skipBasicArrayElements(2);
		} else {
			m.putPtr();
			m.putChar(rssInner.a1);
			m.putChar(rssInner.b1);
			m.putShort(rssInner.c1);
			m.putLong(rssInner.d1);
			m.putChar(rssInner.a2);
			m.putChar(rssInner.b2);
			m.putShort(rssInner.c2);
			m.putLong(rssInner.d2);
			if (null != rssInner.str) {
				m.putStringPtr(rssInner.str, NO_ACTION);
			} else {
				m.putNullStringPtr(NO_ACTION);
			}
			if (null != rssInner.buffer) {
				m.putStringPtr(rssInner.buffer, NO_ACTION);
				m.putLong(rssInner.buffer.length());
			} else {
				m.putNullStringPtr(NO_ACTION);
				m.putLong(0);
			}
			m.putLong(0);
		}
		return m;
	}

	public static MarshallPlan makeSumResourcePlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilder(
				2 * PrimitiveSize.POINTER, PrimitiveSize.POINTER, 2, true);
		builder.variableIndirectPtr();
		builder.ptrBegin(PrimitiveSize.POINTER);
		builder.variableIndirectPtr();
		builder.ptrEnd();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan makeSwapPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilder(
				PrimitiveSize.POINTER, PrimitiveSize.POINTER + 2
						* PrimitiveSize.LONG, 1, true);
		builder.ptrBegin(PrimitiveSize.POINTER + 2 * PrimitiveSize.LONG);
		builder.containerBegin();
		builder.variableIndirectPtr();
		builder.pos(PrimitiveSize.LONG);
		builder.pos(PrimitiveSize.LONG);
		builder.containerEnd();
		builder.ptrEnd();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan makeHelloWorldReturnPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilder(
				PrimitiveSize.BOOL,
				PrimitiveSize.POINTER /* for return value */,
				1 /* for return value */, true);
		builder.pos(PrimitiveSize.BOOL);
		builder.variableIndirectPseudoArg();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan makeReturnStringPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilder(
				PrimitiveSize.POINTER,
				PrimitiveSize.POINTER /* for return value */,
				2 /* * one is for return value */, true);
		builder.variableIndirectPtr();
		builder.variableIndirectPseudoArg();
		return builder.makeMarshallPlan();
	}
	
	public static MarshallPlan makeReturnPtrStringPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilder(
				PrimitiveSize.POINTER,
				2 * PrimitiveSize.POINTER  /* one is for return value */,
				2 /* one is for return value */, true);
		builder.ptrBegin(PrimitiveSize.POINTER);
		builder.variableIndirectPtr();
		builder.ptrEnd();
		builder.variableIndirectPseudoArg();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan makeReturnStringOutBufferPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilder(2
				* PrimitiveSize.POINTER + PrimitiveSize.LONG,
				PrimitiveSize.POINTER /* for return value */,
				3 /* one is for return value */, true
		);
		builder.variableIndirectPtr();
		builder.variableIndirectPtr();
		builder.pos(PrimitiveSize.LONG);
		builder.variableIndirectPseudoArg();
		return builder.makeMarshallPlan();
	}
}
