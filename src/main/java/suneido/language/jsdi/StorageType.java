/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi;

import suneido.language.Token;

/**
 * Enumerates the storage types supported by the DLL interface.
 * @author Victor Schappert
 * @since 20130626
 */
@DllInterface
public enum StorageType {
	/**
	 * The underlying type is passed to DLL functions by value, and stored in
	 * structures by value.
	 */
	VALUE,
	/**
	 * The underlying type is passed to DLL functions by pointer, and stored in
	 * structures by pointer.
	 */
	POINTER,
	/**
	 * A contiguous array of values of the underlying type is passed to DLL
	 * functions and stored in structures.
	 */
	ARRAY;

	public static StorageType fromToken(Token token)
	{
		switch (token)
		{
		case VALUETYPE:
			return VALUE;
		case ARRAYTYPE:
			return ARRAY;
		case POINTERTYPE:
			return POINTER;
		default:
			assert false : "No storage type corresponds to token " + token;
			return null;
		}
	}
}
