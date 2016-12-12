/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import suneido.jsdi.DllInterface;
import suneido.jsdi.marshall.Marshaller;

/**
 * Marshaller customized for amd64 marshalling.
 * 
 * @author Victor Schappert
 * @since 20140730
 */
@DllInterface
final class Marshaller64 extends Marshaller {

	//
	// CONSTRUCTORS
	//

	Marshaller64(int sizeTotal, int variableIndirectCount, int[] ptrArray,
			int[] posArray) {
		super(sizeTotal, variableIndirectCount, ptrArray, posArray);
	} // Deliberately package-internal

	Marshaller64(long[] data, int[] ptrArray, int[] posArray) {
		super(data, ptrArray, posArray);
	} // Deliberately package-internal

	Marshaller64(long[] data, int[] ptrArray, int[] posArray, Object[] viArray,
			int[] viInstArray) {
		super(data, ptrArray, posArray, viArray, viInstArray);
	} // Deliberately package-internal

	//
	// ANCESTOR CLASS: Marshaller
	//

	@Override
	public void putInt64(long value) {
		final int dataIndex = nextData();
		final int wordIndex = dataIndex / 8;
		assert 0 == dataIndex % 8 : "64-bit value must be 8-byte aligned";
		data[wordIndex] = value;
	}

	@Override
	public void putPointerSizedInt(long value) {
		putInt64(value);
	}

	@Override
	public long getInt64() {
		final int dataIndex = nextData();
		final int wordIndex = dataIndex / 8;
		assert 0 == dataIndex % 8 : "64-bit value must be 8-byte aligned";
		return data[wordIndex];
	}

	@Override
	public long getPointerSizedInt() {
		return getInt64();
	}

	@Override
	public boolean isPtrNull() {
		return 0L == getInt64();
	}
}
