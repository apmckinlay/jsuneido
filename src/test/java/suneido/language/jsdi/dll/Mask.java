/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.dll;

import suneido.language.jsdi.type.PrimitiveSize;

/**
 * Simple type for masking native call return values in tests.
 *
 * @author Victor Schappert
 * @since 20130727
 */
public enum Mask {

	VOID(0),
	CHAR(PrimitiveSize.CHAR),
	SHORT(PrimitiveSize.SHORT),
	LONG(PrimitiveSize.LONG),
	INT64(PrimitiveSize.INT64),
	DOUBLE(PrimitiveSize.DOUBLE);

	public final long value;

	private Mask(int size) {
		// For some reason in Java and JavaScript, a left shift by the size of
		// the primitive is a no-op (unlike in C where it zeroes the variable).
		value = size < 8 ? ~(~0L << (8 * size)) : 0xffffffffffffffffL;
	}

	@Override
	public String toString() {
		return String.format("Mask[%s=0x%016x]", name(), value);
	}
}
