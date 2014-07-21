/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import suneido.jsdi.DllInterface;
import suneido.jsdi.StorageType;

/**
 * Abstract base class for {@link StringDirect} and {@link StringIndirect}. This
 * shared base class is mainly a container for useful constants.
 *
 * @author Victor Schappert
 * @since 20130710
 */
@DllInterface
public abstract class StringType extends Type {

	//
	// STATIC DATA
	//

	/**
	 * String value identifying a string or string[#] object.
	 * @see #IDENTIFIER_BUFFER
	 * @see VoidType#IDENTIFIER
	 * @see BasicType#fromIdentifier(String)
	 */
	public static final String IDENTIFIER_STRING = "string";
	/**
	 * String value identifying a buffer or buffer[#] object.
	 * @see #IDENTIFIER_STRING
	 * @see VoidType#IDENTIFIER
	 * @see BasicType#fromIdentifier(String)
	 */
	public static final String IDENTIFIER_BUFFER = "buffer";

	//
	// CONSTRUCTORS
	//

	protected StringType(TypeId typeId, StorageType storageType) {
		super(typeId, storageType);
	}
}
