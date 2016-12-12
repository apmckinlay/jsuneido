/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import suneido.SuInternalError;
import suneido.jsdi.DllInterface;
import suneido.jsdi.marshall.MarshallPlan;
import suneido.jsdi.marshall.Marshaller;

/**
 * Specialized marshall plan for making amd64 marshallers.
 * 
 * @author Victor Schappert
 * @since 20140730
 */
@DllInterface
final class MarshallPlan64 extends MarshallPlan {

	//
	// CONSTRUCTORS
	//

	MarshallPlan64(int sizeDirect, int alignDirect, int sizeIndirect,
			int sizeTotal, int[] ptrArray, int[] posArray,
			int variableIndirectCount) {
		super(sizeDirect, alignDirect, sizeIndirect, sizeTotal, ptrArray,
				posArray, variableIndirectCount);
		if (0 != sizeTotal % 8) {
			throw new SuInternalError(
					"total size in bytes of x86 marshall plan must be divisible by 8, but is "
							+ sizeTotal);
		}
	} // Deliberately package-internal

	//
	// ANCESTOR CLASS: MarshallPlan
	//

	@Override
	public Marshaller makeMarshallerInternal(int sizeTotal,
			int variableIndirectCount, int[] ptrArray, int[] posArray) {
		return new Marshaller64(sizeTotal, variableIndirectCount, ptrArray,
				posArray);
	}

	@Override
	public Marshaller makeUnMarshallerInternal(long[] data, int[] ptrArray,
			int[] posArray) {
		return new Marshaller64(data, ptrArray, posArray);
	}

	@Override
	public Marshaller makeUnMarshallerInternal(long[] data, int[] ptrArray,
			int[] posArray, Object[] viArray, int[] viInstArray) {
		return new Marshaller64(data, ptrArray, posArray, viArray, viInstArray);
	}
}
