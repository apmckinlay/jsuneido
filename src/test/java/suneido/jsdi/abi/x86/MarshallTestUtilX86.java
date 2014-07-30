/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import suneido.jsdi.abi.x86.MarshallPlanBuilderX86;
import suneido.jsdi.abi.x86.MarshallPlanX86;
import suneido.jsdi.abi.x86.MarshallerX86;
import suneido.jsdi.marshall.MarshallPlan;
import suneido.jsdi.marshall.MarshallPlanBuilder;
import suneido.jsdi.marshall.MarshallTestUtil;
import suneido.jsdi.marshall.PrimitiveSize;

/**
 * Helper functions for making {@link MarshallerX86}'s and
 * {@link MarshallPlanX86}'s.
 *
 * @author Victor Schappert
 * @since 20130725
 */
public final class MarshallTestUtilX86 extends MarshallTestUtil {

	public static MarshallPlanX86 nullPlan() {
		return new MarshallPlanX86(0, 1, 0, 0, new int[0], new int[0], 0);
	}

	public static MarshallPlanX86 directPlan(int sizeDirect, int alignDirect) {
		return new MarshallPlanX86(sizeDirect, alignDirect, 0,
				PrimitiveSize.sizeLongs(sizeDirect), new int[0],
				new int[] { 0 }, 0);
	}

	public static MarshallPlanX86 pointerPlan(int... sizeDirect) {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(0, true);
		for (int size : sizeDirect) builder.ptrBasic(size, size);
		return (MarshallPlanX86)builder.makeMarshallPlan();
	}

	public static MarshallPlanX86 arrayPlan(int sizeDirect, int numElems) {
		int[] posArray = new int[numElems];
		for (int k = 1; k < numElems; ++k) {
			posArray[k] = sizeDirect * k;
		}
		return new MarshallPlanX86(sizeDirect * numElems, sizeDirect, 0,
				PrimitiveSize.sizeLongs(sizeDirect * numElems), new int[0],
				posArray, 0);
	}

	public static MarshallPlanX86 variableIndirectPlan() {
		final int totalSize = PrimitiveSize.sizeLongs(PrimitiveSize.POINTER);
		return new MarshallPlanX86(PrimitiveSize.POINTER,
				PrimitiveSize.POINTER, 0, totalSize,
				new int[] { 0, totalSize }, new int[] { 0, totalSize }, 1);
	}

	public static MarshallPlanX86 variableIndirectPlan2() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(2, true);
		builder.ptrVariableIndirect();
		builder.ptrVariableIndirect();
		return (MarshallPlanX86)builder.makeMarshallPlan();
	}

	public static MarshallPlanX86 compoundPlan(int numArrayElems,
			int... sizeDirect) {
		// This "compound plan" is equivalent to an array of struct, where the
		// struct member sizes are given by sizeDirect
		final int numMembers = sizeDirect.length;
		int[] posArray = new int[numMembers * numArrayElems];
		int totalSizeDirect = sizeDirect[0];
		int alignDirect = totalSizeDirect;
		// Figure out positions for members of first array element
		for (int i = 1; i < numMembers; ++i) {
			posArray[i] = posArray[i - 1] + sizeDirect[i - 1];
			alignDirect = Math.max(alignDirect, sizeDirect[i]);
			totalSizeDirect += sizeDirect[i];
		}
		// Copy the first array element's positions, mutatis mutandi, through
		// the rest of the array.
		totalSizeDirect *= numArrayElems;
		for (int j = 1; j < numArrayElems; ++j) {
			posArray[numMembers * j] = posArray[numMembers * j - 1]
					+ sizeDirect[numMembers - 1];
			for (int k = 1; k < numMembers; ++k) {
				posArray[numMembers * j + k] = posArray[numMembers * j + k - 1]
						+ sizeDirect[k - 1];
			}
		}
		return new MarshallPlanX86(totalSizeDirect, alignDirect, 0,
				PrimitiveSize.sizeLongs(totalSizeDirect), new int[0], posArray,
				0);
	}

	public static MarshallPlanX86 paramPlan(int... sizeDirect) {
		int[] sizeDirect2 = new int[sizeDirect.length];
		for (int k = 0; k < sizeDirect.length; ++k) {
			sizeDirect2[k] = sizeWholeWords(sizeDirect[k]);
		}
		return compoundPlan(1, sizeDirect2);
	}

	public static MarshallPlan packedCharCharShortLongPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(0, true);
		builder.containerBegin();
		builder.basic(PrimitiveSize.INT8, PrimitiveSize.INT8);
		builder.basic(PrimitiveSize.INT8, PrimitiveSize.INT8);
		builder.basic(PrimitiveSize.INT16, PrimitiveSize.INT16);
		builder.basic(PrimitiveSize.INT32, PrimitiveSize.INT32);
		builder.containerEnd();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan packedInt8x3Plan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(0, true);
		builder.containerBegin();
		builder.basic(PrimitiveSize.INT8, PrimitiveSize.INT8);
		builder.basic(PrimitiveSize.INT8, PrimitiveSize.INT8);
		builder.basic(PrimitiveSize.INT8, PrimitiveSize.INT8);
		builder.containerEnd();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan inStringPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(1, true);
		builder.ptrVariableIndirect();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan ptrPtrPtrDoublePlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(0, true);
		builder.ptrBegin(PrimitiveSize.POINTER, PrimitiveSize.POINTER);
		builder.ptrBegin(PrimitiveSize.POINTER, PrimitiveSize.POINTER);
		builder.ptrBasic(PrimitiveSize.DOUBLE, PrimitiveSize.DOUBLE);
		builder.ptrEnd();
		builder.ptrEnd();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan helloWorldOutBufferPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(1, true);
		builder.ptrVariableIndirect();
		builder.basic(PrimitiveSize.INT32, PrimitiveSize.INT32);
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan sumStringPlan_TwoTier() {
		final int sizeOf_RecursiveStringSum = (2
				* (PrimitiveSize.INT8 + PrimitiveSize.INT8
						+ PrimitiveSize.INT16 + PrimitiveSize.INT32) + 3
				* PrimitiveSize.POINTER + PrimitiveSize.INT32);
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(2 * 2, true);
		builder.ptrBegin(sizeOf_RecursiveStringSum, PrimitiveSize.POINTER);
			builder.containerBegin();              // struct RecursiveStringSum
				builder.arrayBegin();
					builder.containerBegin();          // struct Packed_Int8Int8Int16Int32
					builder.basic(PrimitiveSize.INT8, PrimitiveSize.INT8);
					builder.basic(PrimitiveSize.INT8, PrimitiveSize.INT8);
					builder.basic(PrimitiveSize.INT16, PrimitiveSize.INT16);
					builder.basic(PrimitiveSize.INT32, PrimitiveSize.INT32);
					builder.containerEnd();
					builder.containerBegin();          // struct Packed_Int8Int8Int16Int32
					builder.basic(PrimitiveSize.INT8, PrimitiveSize.INT8);
					builder.basic(PrimitiveSize.INT8, PrimitiveSize.INT8);
					builder.basic(PrimitiveSize.INT16, PrimitiveSize.INT16);
					builder.basic(PrimitiveSize.INT32, PrimitiveSize.INT32);
					builder.containerEnd();
				builder.arrayEnd();
				builder.ptrVariableIndirect();
				builder.ptrVariableIndirect();
				builder.basic(PrimitiveSize.INT32, PrimitiveSize.INT32);
				builder.ptrBegin(sizeOf_RecursiveStringSum, PrimitiveSize.POINTER);
					builder.containerBegin();
						builder.arrayBegin();
							builder.containerBegin();          // struct Packed_Int8Int8Int16Int32
							builder.basic(PrimitiveSize.INT8, PrimitiveSize.INT8);
							builder.basic(PrimitiveSize.INT8, PrimitiveSize.INT8);
							builder.basic(PrimitiveSize.INT16, PrimitiveSize.INT16);
							builder.basic(PrimitiveSize.INT32, PrimitiveSize.INT32);
							builder.containerEnd();
							builder.containerBegin();          // struct Packed_Int8Int8Int16Int32
							builder.basic(PrimitiveSize.INT8, PrimitiveSize.INT8);
							builder.basic(PrimitiveSize.INT8, PrimitiveSize.INT8);
							builder.basic(PrimitiveSize.INT16, PrimitiveSize.INT16);
							builder.basic(PrimitiveSize.INT32, PrimitiveSize.INT32);
							builder.containerEnd();
						builder.arrayEnd();
						builder.ptrVariableIndirect();
						builder.ptrVariableIndirect();
						builder.basic(PrimitiveSize.INT32, PrimitiveSize.INT32);
						builder.basic(PrimitiveSize.POINTER, PrimitiveSize.POINTER);
					builder.containerEnd();
				builder.ptrEnd();
			builder.containerEnd();
		builder.ptrEnd();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan sumResourcePlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(2, true);
		builder.ptrVariableIndirect();
		builder.ptrBegin(PrimitiveSize.POINTER, PrimitiveSize.POINTER);
		builder.ptrVariableIndirect();
		builder.ptrEnd();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan swapPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(1, true);
		builder.ptrBegin(PrimitiveSize.POINTER + 2 * PrimitiveSize.INT32,
				PrimitiveSize.POINTER);
		builder.containerBegin();
		builder.ptrVariableIndirect();
		builder.basic(PrimitiveSize.INT32, PrimitiveSize.INT32);
		builder.basic(PrimitiveSize.INT32, PrimitiveSize.INT32);
		builder.containerEnd();
		builder.ptrEnd();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan helloWorldReturnPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(1 /*
																	 * for
																	 * return
																	 * value
																	 */, true);
		builder.basic(PrimitiveSize.BOOL, PrimitiveSize.BOOL);
		builder.ptrVariableIndirectPseudoParam();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan returnStringPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(2, true);
		builder.ptrVariableIndirect();
		builder.ptrVariableIndirectPseudoParam();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan returnPtrStringPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(2, true);
		builder.ptrBegin(PrimitiveSize.POINTER, PrimitiveSize.POINTER);
		builder.ptrVariableIndirect();
		builder.ptrEnd();
		builder.ptrVariableIndirectPseudoParam();
		return builder.makeMarshallPlan();
	}

	public static MarshallPlan returnStringOutBufferPlan() {
		MarshallPlanBuilder builder = new MarshallPlanBuilderX86(3, true);
		builder.ptrVariableIndirect();
		builder.ptrVariableIndirect();
		builder.basic(PrimitiveSize.INT32, PrimitiveSize.INT32);
		builder.ptrVariableIndirectPseudoParam();
		return builder.makeMarshallPlan();
	}
}
