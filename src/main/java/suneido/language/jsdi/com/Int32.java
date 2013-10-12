package suneido.language.jsdi.com;

import suneido.language.jsdi.DllInterface;


/**
 * <p>
 * Contains a signed {@code int} value which should be treated either as a
 * signed 32-bit integer or as a bit-pattern-equivalent unsigned 32-bit integer.
 * </p>
 * <p>
 * This is a trivial wrapper class whose only purpose is to communicate whether
 * the {@code int} bits should be treated as signed or unsigned.
 * </p> 
 *
 * @author Victor Schappert
 * @since 20131012
 * @see Int64
 * @see Canonifier
 */
@DllInterface
final class Int32 {

	//
	// DATA
	//

	final int     value;
	final boolean isSigned;

	//
	// CONSTRUCTORS
	//
	
	public Int32(int x) {
		value = x;
		isSigned = true;
	}

	public Int32(Number x) {
		throw new RuntimeException("not implemented");
	}
}
