/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.abi.x86;

import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.MarshallPlan;
import suneido.language.jsdi.Marshaller;

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

	MarshallPlanX86(int sizeDirect, int sizeIndirect, int[] ptrArray,
			int[] posArray, int variableIndirectCount) {
		super(sizeDirect, sizeIndirect, ptrArray, posArray,
				variableIndirectCount);
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
		return new MarshallerX86(sizeDirect, sizeIndirect,
				variableIndirectCount, ptrArray, posArray);
	}

	/**
	 * Creates a marshaller instance which is only valid for <em>get</em>
	 * operations out of existing data.
	 * 
	 * @param data An existing data array of the correct length
	 * @return Get-only marshaller based on this plan
	 * @since 20130806
	 * @see #makeMarshallerX86()
	 * @see #makeUnMarshaller(byte[], Object[], int[])
	 */
	MarshallerX86 makeUnMarshaller(byte[] data) {
		assert 0 == variableIndirectCount;
		assert data.length == sizeDirect + sizeIndirect;
		return new MarshallerX86(data, ptrArray, posArray);
	}

	/**
	 * Creates a marshaller instance which is only valid for <em>get</em>
	 * operations out of existing data.
	 *
	 * @param data An existing data array of the correct length
	 * @param viArray An existing, valid, variable indirect output array
	 * @param viInstArray An existing, valid, variable indirect instruction
	 * array
	 * @return Get-only marshaller based on this plan
	 * @since 20130806
	 * @see #makeMarshallerX86()
	 * @see #makeUnMarshaller(byte[])
	 */
	MarshallerX86 makeUnMarshaller(byte[] data, Object[] viArray,
			int[] viInstArray) {
		assert viArray.length == variableIndirectCount
				&& viInstArray.length == variableIndirectCount;
		return new MarshallerX86(data, ptrArray, posArray, viArray, viInstArray);
	}
}
