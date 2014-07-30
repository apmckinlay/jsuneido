/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi;

import static suneido.jsdi.Platform.WIN32_X86;
import static suneido.jsdi.marshall.VariableIndirectInstruction.NO_ACTION;
import suneido.jsdi.Platform;
import suneido.jsdi.abi.x86.MarshallTestUtilX86;
import suneido.jsdi.abi.x86.Mask;
import suneido.jsdi.marshall.MarshallPlan;
import suneido.jsdi.marshall.MarshallTestUtil;
import suneido.jsdi.marshall.Marshaller;
import suneido.jsdi.marshall.PrimitiveSize;

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
			Mask.INT32, "packedCharCharShortLongPlan"),
	SUM_PACKED_INT8x3("TestSumPackedInt8x3", Mask.INT32, "packedInt8x3Plan"),
	STRLEN("TestStrLen", Mask.INT32, "inStringPlan"),
	HELLO_WORLD_RETURN("TestHelloWorldReturn", Mask.INT32,
			"helloWorldReturnPlan"),
	HELLO_WORLD_OUT_PARAM("TestHelloWorldOutParam", Mask.VOID,
			"pointerPlan", PrimitiveSize.WORD),
	HELLO_WORLD_OUT_BUFFER("TestHelloWorldOutBuffer", Mask.VOID,
			"helloWorldOutBufferPlan"),
	NULL_PTR_OUT_PARAM("TestNullPtrOutParam", Mask.VOID,
			"pointerPlan", PrimitiveSize.WORD),
	RETURN_PTR_PTR_PTR_DOUBLE("TestReturnPtrPtrPtrDoubleAsUInt64", Mask.INT64,
			"ptrPtrPtrDoublePlan"),
	SUM_STRING("TestSumString", Mask.INT32, "sumStringPlan_TwoTier"),
	SUM_RESOURCE("TestSumResource", Mask.INT32, "sumResourcePlan"),
	SWAP("TestSwap", Mask.INT32, "swapPlan"),
	RETURN_STRING("TestReturnString", Mask.VOID, "returnStringPlan"),
	RETURN_PTR_STRING("TestReturnPtrString", Mask.VOID, "returnPtrStringPlan"),
	RETURN_STRING_OUT_BUFFER("TestReturnStringOutBuffer", Mask.VOID,
			"returnStringOutBufferPlan");

	private final QuickDll qp;
	public final long ptr;
	public final Mask returnValueMask;
	public final MarshallPlan plan;

	private TestCall(String funcName, Mask returnValueMask, int... paramSizes) {
		this(funcName, returnValueMask, makeDirectParamPlan(paramSizes));
	}

	private TestCall(String funcName, Mask returnValueMask, String planName,
			int... params) {
		this(funcName, returnValueMask, makeNamedPlan(planName, params));
	}

	private TestCall(String funcName, Mask returnValueMask,
			MarshallPlan marshallPlan) {
		qp = new QuickDll("jsdi", funcName);
		ptr = qp.ptr;
		this.returnValueMask = returnValueMask;
		this.plan = marshallPlan;
	}

	private static MarshallPlan makeDirectParamPlan(int... paramSizes) {
		final Platform platform = Platform.getPlatform();
		if (WIN32_X86 == platform) {
			return MarshallTestUtilX86.paramPlan(paramSizes);
		} else {
			throw new RuntimeException("can't handle " + platform);
		}
	}

	private static MarshallPlan makeNamedPlan(String planName, int... params) {
		final Platform platform  = Platform.getPlatform();
		if (WIN32_X86 == platform) {
			return MarshallTestUtil.makeNamedPlan(MarshallTestUtilX86.class, planName, params);
		} else {
			throw new RuntimeException("can't handle " + platform);
		}
	}

	public static Marshaller marshall(MarshallTestUtil.Recursive_StringSum rssOuter,
			MarshallTestUtil.Recursive_StringSum rssInner) {
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
			m.putPointerSizedInt(0L);
		}
		return m;
	}
}
