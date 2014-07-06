/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.type;

import java.util.Map;
import java.util.TreeMap;

import suneido.language.jsdi.DllInterface;

/**
 * Enumerates the available 'basic' (<em>ie</em> non-structure, non-callback)
 * types in the JSDI type hierarchy.
 *
 * @author Victor Schappert
 * @since 20130627
 * @see PrimitiveSize
 */
@DllInterface
public enum BasicType {

	/**
	 * Enumerator for a boolean value, which is represented in native DLL calls
	 * as a 32-bit integer.
	 */
	BOOL("bool", PrimitiveSize.BOOL),
	/**
	 * Enumerator for a signed single-byte number/character value.
	 * @see #INT16
	 * @see #INT32
	 * @see #INT64
	 */
	INT8("int8", PrimitiveSize.INT8),
	/**
	 * Enumerator for a signed short integer value, which is represented in
	 * native DLL calls as a signed 16-bit integer.
	 * @see #INT8
	 * @see #INT32
	 * @see #INT64
	 */
	INT16("int16", PrimitiveSize.INT16),
	/**
	 * Enumerator for a signed long integer value, which is represented in
	 * native DLL calls as a signed 32-bit integer.
	 * @see #INT8
	 * @see #INT16
	 * @see #INT64
	 */
	INT32("int32", PrimitiveSize.INT32),
	/**
	 * Enumerator for a 64-bit signed integer value.
	 * @see #INT8
	 * @see #INT16
	 * @see #INT32
	 */
	INT64("int64", PrimitiveSize.INT64),
	/**
	 * Enumerator for a 32-bit floating-point number (<em>ie</em> a single-
	 * precision IEEE floating-point number, known as <code>float</code> in
	 * C, C++, and Java).
	 * @see #DOUBLE
	 */
	FLOAT("float", PrimitiveSize.FLOAT),
	/**
	 * Enumerator for a 64-bit floating-point number (<em>ie</em> a double-
	 * precision IEEE floating-point number, known as <code>double</code> in C,
	 * C++, and Java).
	 * @see #SINGLE
	 */
	DOUBLE("double", PrimitiveSize.DOUBLE),
	/**
	 * Enumerator for a Windows {@code HANDLE} type (<em>ie</em> a value
	 * returned from an API function such as {@code CreateFile()}.
	 * <p>
	 * TODO: Determine whether we care about tracking calls to
	 * {@code CloseHandle()} in JSuneido. If not, it will be simpler just to
	 * delete this type and use plain {@code long} instead.
	 * </p>
	 * @see #GDIOBJ
	 */
	HANDLE("handle", PrimitiveSize.HANDLE),
	/**
	 * Enumerator for a Windows GDI object handle (<em>ie</em> a value returned
	 * from an API function such as {@code CreateSolidBrush()}.
	 * <p>
	 * TODO: Determine whether we care about tracking calls to
	 * {@code DeleteObject()} in JSuneido. If not, it will be simpler just to
	 * delete this type and use plain {@code long} instead.
	 * </p>
	 * @see #HANDLE
	 */
	GDIOBJ("gdiobj", PrimitiveSize.GDIOBJ);

	//
	// DATA/CONSTRUCTORS
	//

	private final String       identifierString;
	private final int          sizeIntrinsic;
	private final int          sizeWholeWords;

	private BasicType(String identifierString, int sizeIntrinsic) {
		this.identifierString = identifierString;
		this.sizeIntrinsic    = sizeIntrinsic;
		this.sizeWholeWords   = PrimitiveSize.minWholeWords(sizeIntrinsic) *
								PrimitiveSize.WORD;
	}

	//
	// ACCESSORS
	//

	// TODO: docs since 20130724
	public int getSizeIntrinsic() {
		return sizeIntrinsic;
	}

	// TODO: docs since 20130724
	public int getSizeWholeWords() {
		return sizeWholeWords;
	}

	//
	// STATICS
	//

	private static final Map<String, BasicType> identifierMap;
	static {
		identifierMap = new TreeMap<>();
		for (BasicType type : values()) {
			identifierMap.put(type.identifierString, type);
		}
		// TEMP - accept old types during transition
		identifierMap.put("char", INT8);
		identifierMap.put("short", INT16);
		identifierMap.put("long", INT32);
	}

	public static final BasicType fromIdentifier(String identifierString) {
		return identifierMap.get(identifierString);
	}

	public final String toIdentifier() {
		return identifierString;
	}
}
