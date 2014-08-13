/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import java.util.Map;
import java.util.TreeMap;

import suneido.jsdi.DllInterface;
import suneido.jsdi.marshall.PrimitiveSize;

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
	 * Enumerator for a C-style pointer that is opaque from the point of view
	 * of the Suneido programmer. This means Suneido represents the value as a
	 * signed integer having the same width as the operating environment's
	 * native pointer type. For example, in an x86 application, the number is
	 * 32 bits wide while in an x64 application, the number would be 64 bits
	 * wide.
	 */
	OPAQUE_POINTER("pointer", PrimitiveSize.POINTER),
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
	 * delete this type and use plain {@code pointer} instead.
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
	 * delete this type and use plain {@code pointer} instead.
	 * </p>
	 * @see #HANDLE
	 */
	GDIOBJ("gdiobj", PrimitiveSize.GDIOBJ);

	//
	// DATA/CONSTRUCTORS
	//

	private final String       name;
	private final int          size;

	private BasicType(String identifierString, int sizeIntrinsic) {
		this.name = identifierString;
		this.size             = sizeIntrinsic;
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the type size.
	 *
	 * @return Size, in bytes, of this basic type
	 * @since 20130724
	 */
	public int getSize() {
		return size;
	}

	//
	// STATICS
	//

	private static final Map<String, BasicType> map;
	static {
		map = new TreeMap<>();
		for (BasicType type : values()) {
			map.put(type.name, type);
		}
		// TEMP - accept old types during transition
		map.put("char", INT8);
		map.put("short", INT16);
		map.put("long", INT32);
	}

	/**
	 * Converts a basic type name to a basic type.
	 *
	 * @param typeName Basic type name
	 * @return Basic type corresponding to {@code typeName}, or {@code null}
	 *         if no such type
	 * @see #getName()
	 */
	public static final BasicType fromName(String typeName) {
		return map.get(typeName);
	}

	/**
	 * Returns the name of the type.
	 *
	 * @return Type name
	 * @see #fromName(String)
	 */
	public final String getName() {
		return name;
	}
}
