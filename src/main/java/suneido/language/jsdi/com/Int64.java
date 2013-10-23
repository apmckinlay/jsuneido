/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.com;

import suneido.language.jsdi.DllInterface;


/**
 * <p>
 * Contains a signed {@code long} value which should be treated either as a
 * signed 64-bit integer or as a bit-pattern-equivalent unsigned 64-bit integer.
 * </p>
 * <p>
 * This is a trivial wrapper class whose only purpose is to communicate whether
 * the {@code long} bits should be treated as signed or unsigned.

 *
 * @author Victor Schappert
 * @since 20131012
 * @see Int32
 * @see Canonifier
 */
@DllInterface
final class Int64 {

	//
	// DATA
	//

	final long    value;
	final boolean isSigned;

	//
	// CONSTRUCTORS
	//

	public Int64(long x) {
		value = x;
		isSigned = true;
	}

	public Int64(Number x) {
		throw new RuntimeException("not implemented");
	}
}
