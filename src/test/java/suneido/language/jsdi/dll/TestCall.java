/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.dll;

import static suneido.language.jsdi.MarshallTestUtil.pointerPlan;
import static suneido.language.jsdi.VariableIndirectInstruction.NO_ACTION;
import suneido.language.jsdi.*;

/**
 * Should be kept in sync with {@code test_exports.h/cpp}.
 *
 * @author Victor Schappert
 * @since 20130723
 * @see QuickDll
 */
public enum TestCall {

	VOID("TestVoid", Mask.VOID, 0),
	INT8("TestInt8", Mask.INT8, PrimitiveSize.INT8),
	INT16("TestInt16", Mask.INT16, PrimitiveSize.INT16),
	INT32("TestInt32", Mask.INT32, PrimitiveSize.INT32),
	INT64("TestInt64", Mask.INT64, PrimitiveSize.INT64),
	RETURN1_0FLOAT("TestReturn1_0Float", Mask.DOUBLE, 0),
	RETURN1_0DOUBLE("TestReturn1_0Double", Mask.DOUBLE, 0),
	FLOAT("TestFloat", Mask.DOUBLE, PrimitiveSize.FLOAT),
	DOUBLE("TestDouble", Mask.DOUBLE, PrimitiveSize.DOUBLE),
	REMOVE_SIGN_FROM_INT32("TestRemoveSignFromInt32", Mask.INT64,
			PrimitiveSize.INT32),
	COPY_INT32_VALUE("TestCopyInt32Value", Mask.VOID, PrimitiveSize.POINTER,
			PrimitiveSize.POINTER),
	SUM_TWO_INT8("TestSumTwoInt8s", Mask.INT8,
			PrimitiveSize.INT8, PrimitiveSize.INT8),
	SUM_TWO_INT16("TestSumTwoInt16s", Mask.INT16,
			PrimitiveSize.INT16, PrimitiveSize.INT16),
	SUM_TWO_INT32("TestSumTwoInt32s", Mask.INT32,
			PrimitiveSize.INT32, PrimitiveSize.INT32),
	SUM_TWO_FLOATS("TestSumTwoFloats", Mask.DOUBLE,
			PrimitiveSize.FLOAT, PrimitiveSize.FLOAT),
	SUM_TWO_DOUBLES("TestSumTwoDoubles", Mask.DOUBLE,
			PrimitiveSize.DOUBLE, PrimitiveSize.DOUBLE),
	SUM_THREE_INT32("TestSumThreeInt32s", Mask.INT32,
			PrimitiveSize.INT32, PrimitiveSize.INT32, PrimitiveSize.INT32),
	SUM_FOUR_INT32("TestSumFourInt32s", Mask.INT32,
			PrimitiveSize.INT32, PrimitiveSize.INT32, PrimitiveSize.INT32,
			PrimitiveSize.INT32),
	SUM_FIVE_INT32("TestSumFiveInt32s", Mask.INT32,
			PrimitiveSize.INT32, PrimitiveSize.INT32, PrimitiveSize.INT32,
			PrimitiveSize.INT32, PrimitiveSize.INT32),
	SUM_SIX_INT32("TestSumSixInt32s", Mask.INT32,
			PrimitiveSize.INT32, PrimitiveSize.INT32, PrimitiveSize.INT32,
			PrimitiveSize.INT32, PrimitiveSize.INT32, PrimitiveSize.INT32),
	SUM_SEVEN_INT32("TestSumSevenInt32s", Mask.INT32,
			PrimitiveSize.INT32, PrimitiveSize.INT32, PrimitiveSize.INT32,
			PrimitiveSize.INT32, PrimitiveSize.INT32, PrimitiveSize.INT32,
			PrimitiveSize.INT32),
	SUM_EIGHT_INT32("TestSumEightInt32s", Mask.INT32,
			PrimitiveSize.INT32, PrimitiveSize.INT32, PrimitiveSize.INT32,
			PrimitiveSize.INT32, PrimitiveSize.INT32, PrimitiveSize.INT32,
			PrimitiveSize.INT32, PrimitiveSize.INT32),
	SUM_NINE_INT32("TestSumNineInt32s", Mask.INT32,
			PrimitiveSize.INT32, PrimitiveSize.INT32, PrimitiveSize.INT32,
			PrimitiveSize.INT32, PrimitiveSize.INT32, PrimitiveSize.INT32,
			PrimitiveSize.INT32, PrimitiveSize.INT32, PrimitiveSize.INT32),
	SUM_INT8_PLUS_INT64("TestSumInt8PlusInt64", Mask.INT64,
			PrimitiveSize.INT8, PrimitiveSize.INT64),
	SUM_PACKED_INT8_INT8_INT16_INT32("TestSumPackedInt8Int8Int16Int32",
			Mask.INT32, makePackedCharCharShortLongPlan()),
	SUM_PACKED_INT8x3("TestSumPackedInt8x3", Mask.INT32, makePackedInt8x3Plan()),
	STRLEN("TestStrLen", Mask.INT32, makeInStringPlan()),
	HELLO_WORLD_RETURN("TestHelloWorldReturn", Mask.INT32,
			makeHelloWorldReturnPlan()),
	HELLO_WORLD_OUT_PARAM("TestHelloWorldOutParam", Mask.VOID,
			pointerPlan(PrimitiveSize.WORD)),
	HELLO_WORLD_OUT_BUFFER("TestHelloWorldOutBuffer", Mask.VOID,
			makeHelloWorldOutBufferPlan()),
	NULL_PTR_OUT_PARAM("TestNullPtrOutParam", Mask.VOID,
			pointerPlan(PrimitiveSize.WORD)),
	RETURN_PTR_PTR_PTR_DOUBLE("TestReturnPtrPtrPtrDoubleAsUInt64", Mask.INT64,
			makePtrPtrPtrDoublePlan()),
	SUM_STRING("TestSumString", Mask.INT32, makeSumStringPlan_TwoTier()),
	SUM_RESOURCE("TestSumResource", Mask.INT32, makeSumResourcePlan()),
	SWAP("TestSwap", Mask.INT32, makeSwapPlan()),
	RETURN_STRING("TestReturnString", Mask.VOID, makeReturnStringPlan()),
	RETURN_PTR_STRING("TestReturnPtrString", Mask.VOID, makeReturnPtrStringPlan()),
	RETURN_STRING_OUT_BUFFER("TestReturnStringOutBuffer", Mask.VOID,
			makeReturnStringOutBufferPlan());

	private final QuickDll qp;
	public final long ptr;
	public final Mask returnValueMask;
	public final MarshallPlan plan;

	private TestCall(String funcName, Mask returnValueMask, int... paramSizes) {
		this(funcName, returnValueMask, MarshallTestUtil.paramPlan(paramSizes));
	}

	private TestCall(String funcName, Mask returnValueMask,
			MarshallPlan marshallPlan) {
		qp = new QuickDll("jsdi", funcName);
		ptr = qp.ptr;
		this.returnValueMask = returnValueMask;
		this.plan = marshallPlan;
	}

	private static MarshallPlan makePackedCharCharShortLongPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilder(
				PrimitiveSize.sizeWholeWords(PrimitiveSize.INT8
						+ PrimitiveSize.INT8 + PrimitiveSize.INT16
						+ PrimitiveSize.INT32), 0, 0, true);
		builder.containerBegin();
		builder.pos(PrimitiveSize.INT8);
		builder.pos(PrimitiveSize.INT8);
		builder.pos(PrimitiveSize.INT16);
		builder.pos(PrimitiveSize.INT32);
		builder.containerEnd();
		return builder.makeMarshallPlan();
	}

	private static MarshallPlan makePackedInt8x3Plan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilder(
				PrimitiveSize.sizeWholeWords(PrimitiveSize.INT8 * 3), 0, 0,
				true);
		builder.containerBegin();
		builder.pos(PrimitiveSize.INT8);
		builder.pos(PrimitiveSize.INT8);
		builder.pos(PrimitiveSize.INT8);
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
						+ PrimitiveSize.sizeWholeWords(PrimitiveSize.INT32), 0,
				1, true);
		builder.variableIndirectPtr();
		builder.pos(PrimitiveSize.INT32);
		return builder.makeMarshallPlan();
	}

	private static MarshallPlan makeSumStringPlan_TwoTier() {
		final int sizeOf_RecursiveStringSum = (2
				* (PrimitiveSize.INT8 + PrimitiveSize.INT8
						+ PrimitiveSize.INT16 + PrimitiveSize.INT32) + 3
				* PrimitiveSize.POINTER + PrimitiveSize.INT32);
		MarshallPlanBuilder builder = new MarshallPlanBuilder(
				PrimitiveSize.pointerWholeWordBytes(),
				2 * sizeOf_RecursiveStringSum,
				2 * (2), true);
		builder.ptrBegin(sizeOf_RecursiveStringSum);
			builder.containerBegin();              // struct RecursiveStringSum
				builder.arrayBegin();
					builder.containerBegin();          // struct Packed_Int8Int8Int16Int32
					builder.pos(PrimitiveSize.INT8);
					builder.pos(PrimitiveSize.INT8);
					builder.pos(PrimitiveSize.INT16);
					builder.pos(PrimitiveSize.INT32);
					builder.containerEnd();
					builder.containerBegin();          // struct Packed_Int8Int8Int16Int32
					builder.pos(PrimitiveSize.INT8);
					builder.pos(PrimitiveSize.INT8);
					builder.pos(PrimitiveSize.INT16);
					builder.pos(PrimitiveSize.INT32);
					builder.containerEnd();
				builder.arrayEnd();
				builder.variableIndirectPtr();
				builder.variableIndirectPtr();
				builder.pos(PrimitiveSize.INT32);
				builder.ptrBegin(sizeOf_RecursiveStringSum);
					builder.containerBegin();
						builder.arrayBegin();
							builder.containerBegin();          // struct Packed_Int8Int8Int16Int32
							builder.pos(PrimitiveSize.INT8);
							builder.pos(PrimitiveSize.INT8);
							builder.pos(PrimitiveSize.INT16);
							builder.pos(PrimitiveSize.INT32);
							builder.containerEnd();
							builder.containerBegin();          // struct Packed_Int8Int8Int16Int32
							builder.pos(PrimitiveSize.INT8);
							builder.pos(PrimitiveSize.INT8);
							builder.pos(PrimitiveSize.INT16);
							builder.pos(PrimitiveSize.INT32);
							builder.containerEnd();
						builder.arrayEnd();
						builder.variableIndirectPtr();
						builder.variableIndirectPtr();
						builder.pos(PrimitiveSize.INT32);
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
		m.putInt8(rssOuter.a1);
		m.putInt8(rssOuter.b1);
		m.putInt16(rssOuter.c1);
		m.putInt32(rssOuter.d1);
		m.putInt8(rssOuter.a2);
		m.putInt8(rssOuter.b2);
		m.putInt16(rssOuter.c2);
		m.putInt32(rssOuter.d2);
		if (null != rssOuter.str) {
			m.putStringPtr(rssOuter.str, NO_ACTION);
		} else {
			m.putNullStringPtr(NO_ACTION);
		}
		if (null != rssOuter.buffer) {
			m.putStringPtr(rssOuter.buffer, NO_ACTION);
			m.putInt32(rssOuter.buffer.length());
		} else {
			m.putNullStringPtr(NO_ACTION);
			m.putInt32(0);
		}
		if (null == rssInner) {
			m.putNullPtr();
			m.skipBasicArrayElements(8);
			m.putNullStringPtr(NO_ACTION);
			m.putNullStringPtr(NO_ACTION);
			m.skipBasicArrayElements(2);
		} else {
			m.putPtr();
			m.putInt8(rssInner.a1);
			m.putInt8(rssInner.b1);
			m.putInt16(rssInner.c1);
			m.putInt32(rssInner.d1);
			m.putInt8(rssInner.a2);
			m.putInt8(rssInner.b2);
			m.putInt16(rssInner.c2);
			m.putInt32(rssInner.d2);
			if (null != rssInner.str) {
				m.putStringPtr(rssInner.str, NO_ACTION);
			} else {
				m.putNullStringPtr(NO_ACTION);
			}
			if (null != rssInner.buffer) {
				m.putStringPtr(rssInner.buffer, NO_ACTION);
				m.putInt32(rssInner.buffer.length());
			} else {
				m.putNullStringPtr(NO_ACTION);
				m.putInt32(0);
			}
			m.putInt32(0);
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
						* PrimitiveSize.INT32, 1, true);
		builder.ptrBegin(PrimitiveSize.POINTER + 2 * PrimitiveSize.INT32);
		builder.containerBegin();
		builder.variableIndirectPtr();
		builder.pos(PrimitiveSize.INT32);
		builder.pos(PrimitiveSize.INT32);
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
				* PrimitiveSize.POINTER + PrimitiveSize.INT32,
				PrimitiveSize.POINTER /* for return value */,
				3 /* one is for return value */, true
		);
		builder.variableIndirectPtr();
		builder.variableIndirectPtr();
		builder.pos(PrimitiveSize.INT32);
		builder.variableIndirectPseudoArg();
		return builder.makeMarshallPlan();
	}
}
