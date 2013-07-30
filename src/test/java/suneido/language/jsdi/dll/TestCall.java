package suneido.language.jsdi.dll;

import static suneido.language.jsdi.MarshallTestUtil.pointerPlan;
import suneido.language.jsdi.MarshallPlan;
import suneido.language.jsdi.MarshallPlanBuilder;
import suneido.language.jsdi.MarshallTestUtil;
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
	SUM_TWO_CHARS("TestSumTwoChars", Mask.CHAR,
			PrimitiveSize.CHAR, PrimitiveSize.CHAR),
	SUM_TWO_SHORTS("TestSumTwoShorts", Mask.SHORT,
			PrimitiveSize.SHORT, PrimitiveSize.SHORT),
	SUM_TWO_LONGS("TestSumTwoLongs", Mask.LONG,
			PrimitiveSize.LONG, PrimitiveSize.LONG),
	SUM_THREE_LONGS("TestSumThreeLongs", Mask.LONG,
			PrimitiveSize.LONG, PrimitiveSize.LONG, PrimitiveSize.LONG),
	SUM_FOUR_LONGS("TestSumFourLongs", Mask.LONG,
			PrimitiveSize.LONG, PrimitiveSize.LONG, PrimitiveSize.LONG,
			PrimitiveSize.LONG),
	SUM_CHAR_PLUS_INT64("TestSumCharPlusInt64", Mask.INT64,
			PrimitiveSize.CHAR, PrimitiveSize.INT64),
	SUM_PACKED_CHAR_CHAR_SHORT_LONG("TestSumPackedCharCharShortLong", Mask.LONG,
			makePackedCharCharShortLongPlan()),
	STRLEN("TestStrLen", Mask.LONG, PrimitiveSize.POINTER),
	HELLO_WORLD_RETURN("TestHelloWorldReturn", Mask.LONG, PrimitiveSize.BOOL),
	HELLO_WORLD_OUT_PARAM("TestHelloWorldOutParam", Mask.VOID,
			pointerPlan(PrimitiveSize.WORD)),
	NULL_PTR_OUT_PARAM("TestNullPtrOutParam", Mask.VOID,
			pointerPlan(PrimitiveSize.WORD)),
	RETURN_PTR_PTR_PTR_DOUBLE("TestReturnPtrPtrPtrDoubleAsUInt64", Mask.INT64,
			makePtrPtrPtrDoublePlan());

	private final QuickDll qp;
	public final long ptr;
	public final Mask returnValueMask;
	public final MarshallPlan plan;

	private TestCall(String funcName, Mask returnValueMask, int... paramSizes) {
		this(funcName, returnValueMask, MarshallTestUtil.paramPlan(paramSizes));
	}

	private TestCall(String funcName, Mask returnValueMask,
			MarshallPlan marshallPlan) {
		qp = new QuickDll("jsdi", '_' + funcName + "@"
				+ marshallPlan.getSizeDirect());
		ptr = qp.ptr;
		this.returnValueMask = returnValueMask;
		this.plan = marshallPlan;
	}

	private static MarshallPlan makePackedCharCharShortLongPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilder(
				PrimitiveSize.sizeWholeWords(PrimitiveSize.CHAR
						+ PrimitiveSize.CHAR + PrimitiveSize.SHORT
						+ PrimitiveSize.LONG), 0, 0);
		builder.containerBegin();
		builder.pos(PrimitiveSize.CHAR);
		builder.pos(PrimitiveSize.CHAR);
		builder.pos(PrimitiveSize.SHORT);
		builder.pos(PrimitiveSize.LONG);
		builder.containerEnd();
		return builder.makeMarshallPlan();
	}

	private static MarshallPlan makePtrPtrPtrDoublePlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilder(
				PrimitiveSize.pointerWholeWordBytes(),
				2 * PrimitiveSize.POINTER + PrimitiveSize.DOUBLE, 0
		);
		builder.ptrBegin(PrimitiveSize.POINTER);
		builder.ptrBegin(PrimitiveSize.POINTER);
		builder.ptrBasic(PrimitiveSize.DOUBLE);
		builder.ptrEnd();
		builder.ptrEnd();
		return builder.makeMarshallPlan();
	}
}
