/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import suneido.SuInternalError;
import suneido.jsdi.DllInterface;
import suneido.jsdi.MarshallPlan;
import suneido.jsdi.Marshaller;
import suneido.jsdi.PrimitiveSize;

/**
 * Specialized marshall plan for making x86 marshallers.
 * 
 * @author Victor Schappert
 * @since 20140719
 */
@DllInterface
final class MarshallPlanX86 extends MarshallPlan {

	//
	// CONSTRUCTORS
	//

	MarshallPlanX86(int sizeDirect, int alignDirect, int sizeIndirect,
			int sizeTotal, int[] ptrArray, int[] posArray,
			int variableIndirectCount) {
		super(sizeDirect, alignDirect, sizeIndirect, sizeTotal, ptrArray,
				posArray, variableIndirectCount);
		if (0 != sizeTotal % 4) {
			throw new SuInternalError(
					"total size in bytes of x86 marshall plan must be divisible by 4, but is "
							+ sizeTotal);
		}
	} // Deliberately package-internal

	//
	// ANCESTOR CLASS: MarshallPlan
	//

	@Override
	public Marshaller makeMarshaller() {
		return makeMarshallerX86();
	}

	//
	// PACKAGE-INTERNAL MEMBERS
	//

	/**
	 * Creates a marshaller instance for marshalling all data described by this
	 * plan, both direct and indirect.
	 *
	 * @return Marshaller based on this plan
	 */
	MarshallerX86 makeMarshallerX86() {
		return new MarshallerX86(sizeTotal, variableIndirectCount, ptrArray,
				posArray);
	}

	/**
	 * Creates a marshaller instance which is only valid for <em>get</em>
	 * operations out of existing data.
	 * 
	 * @param data
	 *            An existing data array of the correct length
	 * @return Get-only marshaller based on this plan
	 * @since 20130806
	 * @see #makeMarshallerX86()
	 * @see #makeUnMarshaller(byte[], Object[], int[])
	 */
	MarshallerX86 makeUnMarshaller(int[] data) {
		assert 0 == variableIndirectCount;
		assert data.length == PrimitiveSize.numWholeWords(sizeTotal);
		return new MarshallerX86(data, ptrArray, posArray);
	}

	/**
	 * Creates a marshaller instance which is only valid for <em>get</em>
	 * operations out of existing data.
	 *
	 * @param data
	 *            An existing data array of the correct length
	 * @param viArray
	 *            An existing, valid, variable indirect output array
	 * @param viInstArray
	 *            An existing, valid, variable indirect instruction array
	 * @return Get-only marshaller based on this plan
	 * @since 20130806
	 * @see #makeMarshallerX86()
	 * @see #makeUnMarshaller(byte[])
	 */
	MarshallerX86 makeUnMarshaller(int[] data, Object[] viArray,
			int[] viInstArray) {
		assert data.length == sizeTotal / Integer.BYTES
				&& viArray.length == variableIndirectCount
				&& viInstArray.length == variableIndirectCount;
		return new MarshallerX86(data, ptrArray, posArray, viArray, viInstArray);
	}
}
