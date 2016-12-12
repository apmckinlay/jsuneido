/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDIException;
import suneido.jsdi.marshall.Marshaller;

/**
 * Marshaller customized for x86 marshalling.
 * 
 * @author Victor Schappert
 * @since 20140717
 */
@DllInterface
final class MarshallerX86 extends Marshaller {

	//
	// CONSTRUCTORS
	//

	MarshallerX86(int sizeTotal, int variableIndirectCount,
			int[] ptrArray, int[] posArray) {
		super(sizeTotal, variableIndirectCount, ptrArray, posArray);
	} // Deliberately package-internal

	MarshallerX86(long[] data, int[] ptrArray, int[] posArray) {
		super(data, ptrArray, posArray);
	} // Deliberately package-internal

	MarshallerX86(long[] data, int[] ptrArray, int[] posArray,
			Object[] viArray, int[] viInstArray) {
		super(data, ptrArray, posArray, viArray, viInstArray);
	} // Deliberately package-internal

	//
	// ANCESTOR CLASS: Marshaller
	//

	@Override
	public void putInt64(long value) {
		final int dataIndex = nextData();
		final int wordIndex = dataIndex / 8;
		// NOTE: Doesn't HAVE to be 8-byte aligned on x86 in all circumstances.
		//       e.g. If packing an int64 into two words that will be pushed
		//       onto the stack as parameters.
		if (0 == dataIndex % 8) {
			data[wordIndex] = value;
		} else {
			assert 0 == dataIndex % 4; // must be 8- or 4-byte aligned
			data[wordIndex+0] |= value << 32;
			data[wordIndex+1] |= value >>> 32;
		}
	}

	@Override
	public void putPointerSizedInt(long value) {
		if (!((long) Integer.MIN_VALUE <= value && value <= (long) Integer.MAX_VALUE))
			throw new JSDIException(
					"can't marshall 32-bit pointer because more than 32 bits used: "
							+ value);
		putInt32((int) value);
	}

	@Override
	public long getInt64() {
		final int dataIndex = nextData();
		final int wordIndex = dataIndex / 8;
		if (0 == dataIndex % 8) {
			return data[wordIndex];
		} else {
			assert 0 == dataIndex % 4; // must be 8- or 4-byte aligned
			return data[wordIndex+0] >>> 32|
			       data[wordIndex+1] << 32;
		}
	}

	@Override
	public long getPointerSizedInt() {
		return (long)getInt32();
	}

	@Override
	public boolean isPtrNull() {
		return 0 == getInt32();
	}
}
