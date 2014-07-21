/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.abi.x86;

import suneido.language.jsdi.MarshallPlan;
import suneido.language.jsdi.MarshallPlanBuilder;
import suneido.language.jsdi.MarshallTestUtil;
import suneido.language.jsdi.PrimitiveSize;

/**
 * Helper functions for making {@link MarshallerX86}'s and
 * {@link MarshallPlanX86}'s.
 *
 * @author Victor Schappert
 * @since 20130725
 */
public final class MarshallTestUtilX86 extends MarshallTestUtil {

	public static MarshallPlanX86 nullPlan() {
		return new MarshallPlanX86(0, 0, new int[0], new int[0], 0);
	}

	public static MarshallPlanX86 directPlan(int sizeDirect) {
		return new MarshallPlanX86(sizeDirect, 0, new int[0], new int[] { 0 }, 0);
	}

	public static MarshallPlanX86 pointerPlan(int sizeDirect) {
		return new MarshallPlanX86(PrimitiveSize.pointerWholeWordBytes(),
				sizeDirect, new int[] { 0, PrimitiveSize.POINTER }, new int[] {
						0, PrimitiveSize.POINTER }, 0);
	}

	public static MarshallPlanX86 pointerPlan(int... sizeDirect) {
		int sizeDirect_ = 0;
		int sizeIndirect = 0;
		for (int size : sizeDirect)
		{
			sizeDirect_ += PrimitiveSize.pointerWholeWordBytes();
			sizeIndirect += size;
		}
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(sizeDirect_,
				sizeIndirect, 0, true);
		for (int size : sizeDirect) builder.ptrBasic(size);
		return (MarshallPlanX86)builder.makeMarshallPlan();
	}

	public static MarshallPlanX86 arrayPlan(int sizeDirect, int numElems) {
		int[] posArray = new int[numElems];
		for (int k = 1; k < numElems; ++k) {
			posArray[k] = sizeDirect * k;
		}
		return new MarshallPlanX86(sizeDirect * numElems, 0, new int[0], posArray,
				0);
	}

	public static MarshallPlanX86 variableIndirectPlan() {
		return new MarshallPlanX86(PrimitiveSize.pointerWholeWordBytes(), 0,
				new int[] { 0, PrimitiveSize.POINTER }, new int[] { 0,
						PrimitiveSize.POINTER }, 1);
	}

	public static MarshallPlanX86 variableIndirectPlan2() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(
			2 * PrimitiveSize.pointerWholeWordBytes(),
			0, 2, true);
		builder.variableIndirectPtr();
		builder.variableIndirectPtr();
		return (MarshallPlanX86)builder.makeMarshallPlan();
	}

	public static MarshallPlanX86 compoundPlan(int numArrayElems,
			int... sizeDirect) {
		final int numMembers = sizeDirect.length;
		int[] posArray = new int[numMembers * numArrayElems];
		int totalSizeDirect = sizeDirect[0];
		for (int i = 1; i < numMembers; ++i) {
			posArray[i] = posArray[i - 1] + sizeDirect[i - 1];
			totalSizeDirect += sizeDirect[i];
		}
		totalSizeDirect *= numArrayElems;
		for (int j = 1; j < numArrayElems; ++j) {
			posArray[numMembers * j] = posArray[numMembers * j - 1]
					+ sizeDirect[numMembers - 1];
			for (int k = 1; k < numMembers; ++k) {
				posArray[numMembers * j + k] = posArray[numMembers * j + k - 1]
						+ sizeDirect[k - 1];
			}
		}
		return new MarshallPlanX86(totalSizeDirect, 0, new int[0], posArray, 0);
	}

	public static MarshallPlanX86 paramPlan(int... sizeDirect) {
		int[] sizeDirect2 = new int[sizeDirect.length];
		for (int k = 0; k < sizeDirect.length; ++k) {
			sizeDirect2[k] = PrimitiveSize.sizeWholeWords(sizeDirect[k]);
		}
		return compoundPlan(1, sizeDirect2);
	}

	public static MarshallPlan packedCharCharShortLongPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(
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

	public static MarshallPlan packedInt8x3Plan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(
				PrimitiveSize.sizeWholeWords(PrimitiveSize.INT8 * 3), 0, 0,
				true);
		builder.containerBegin();
		builder.pos(PrimitiveSize.INT8);
		builder.pos(PrimitiveSize.INT8);
		builder.pos(PrimitiveSize.INT8);
		builder.containerEnd();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan inStringPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(
			PrimitiveSize.pointerWholeWordBytes(),0, 1, true);
		builder.variableIndirectPtr();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan ptrPtrPtrDoublePlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(
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

	public static MarshallPlan helloWorldOutBufferPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(
				PrimitiveSize.pointerWholeWordBytes()
						+ PrimitiveSize.sizeWholeWords(PrimitiveSize.INT32), 0,
				1, true);
		builder.variableIndirectPtr();
		builder.pos(PrimitiveSize.INT32);
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan sumStringPlan_TwoTier() {
		final int sizeOf_RecursiveStringSum = (2
				* (PrimitiveSize.INT8 + PrimitiveSize.INT8
						+ PrimitiveSize.INT16 + PrimitiveSize.INT32) + 3
				* PrimitiveSize.POINTER + PrimitiveSize.INT32);
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(
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

	public static MarshallPlan sumResourcePlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(
				2 * PrimitiveSize.POINTER, PrimitiveSize.POINTER, 2, true);
		builder.variableIndirectPtr();
		builder.ptrBegin(PrimitiveSize.POINTER);
		builder.variableIndirectPtr();
		builder.ptrEnd();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan swapPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(
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

	public static MarshallPlan helloWorldReturnPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(
				PrimitiveSize.BOOL,
				PrimitiveSize.POINTER /* for return value */,
				1 /* for return value */, true);
		builder.pos(PrimitiveSize.BOOL);
		builder.variableIndirectPseudoArg();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan returnStringPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(
				PrimitiveSize.POINTER,
				PrimitiveSize.POINTER /* for return value */,
				2 /* * one is for return value */, true);
		builder.variableIndirectPtr();
		builder.variableIndirectPseudoArg();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan returnPtrStringPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(
				PrimitiveSize.POINTER,
				2 * PrimitiveSize.POINTER  /* one is for return value */,
				2 /* one is for return value */, true);
		builder.ptrBegin(PrimitiveSize.POINTER);
		builder.variableIndirectPtr();
		builder.ptrEnd();
		builder.variableIndirectPseudoArg();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan returnStringOutBufferPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(2
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
