/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.com;

import suneido.language.jsdi.DllInterface;


/**
 * <p>
 * Contains a signed {@code long} value which may be treated as any of the
 * following integer types:
 * <ul>
 * <li>a signed 64-bit integer</li>
 * <li>an unsigned 64-bit integer</li>
 * <li>a signed 32-bit integer</li>
 * <li>an unsigned 32-bit integer</li>
 * </ul>
 * </p>
 * <p>
 * This is a trivial wrapper class whose purpose is to communicate the number of
 * bits of the {@code long} value to use, and whether it should be treated as a
 * signed two's complement value, or as an unsigned value.
 * </p>
 *
 * @author Victor Schappert
 * @since 20131012
 * @see Canonifier
 */
@DllInterface
final class IntValue {

	//
	// DATA
	//

	final long    value;
	final boolean isSigned;
	final boolean is64bit;

	//
	// CONSTRUCTORS
	//

	public IntValue(long x) {
		value = x;
		isSigned = true;
		is64bit  = true;
	}

	public IntValue(int x) {
		value = (long)x;
		isSigned = true;
		is64bit  = false;
	}

	public IntValue(Number x) {
		throw new RuntimeException("not implemented");
	}
}
