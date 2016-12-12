/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.marshall;

import suneido.jsdi.DllInterface;

/**
 * Trivial partial implementation of marshaller for general testing.
 *
 * @author Victor Schappert
 * @since 20140801
 */
@DllInterface
final class TestMarshaller extends Marshaller {

	TestMarshaller(int sizeTotal, int variableIndirectCount,
			int[] ptrArray, int[] posArray) {
		super(sizeTotal, variableIndirectCount, ptrArray, posArray);
	}

	@Override
	public void putInt64(long value) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void putPointerSizedInt(long value) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public long getInt64() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public long getPointerSizedInt() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public boolean isPtrNull() {
		throw new RuntimeException("not implemented");
	}
}
