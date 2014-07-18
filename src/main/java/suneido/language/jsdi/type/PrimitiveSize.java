/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.type;

import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi._64BitIssue;

/**
 * Contains the "direct" size of various native types supported by JSDI. The
 * sizes are given in bytes.
 * @author Victor Schappert
 * @since 20130703
 */
@DllInterface
public final class PrimitiveSize {
	/**
	 * Size of the native word size, in bytes.
	 */
	@_64BitIssue
	public static final int WORD = 4;
	/**
	 * Size of a native pointer type, in bytes. This should give the same
	 * result as the value reported by a C compiler for {@code sizeof(void *)}
	 * on the native platform. 
	 */
	@_64BitIssue
	public static final int POINTER = WORD;
	/**
	 * <p>
	 * Size of a native integer type, in bytes. This should give the same
	 * result as the value reported by a C compiler for {@code sizeof(int)}
	 * on the native platform.
	 * </p>
	 * <p>
	 * NOTE do not confuse this with the size of the C++ {@code bool} type
	 * which, although implementation defined, is typically only one byte.
	 * </p>
	 */
	public static final int BOOL = 4;
	/**
	 * Size of a native {@code int8} in bytes.
	 */
	public static final int INT8 = 1;
	/**
	 * Size of a native {@code int16} in bytes.
	 */
	public static final int INT16 = 2;
	/**
	 * Size of a native {@code int32} in bytes.
	 */
	public static final int INT32 = 4;
	/**
	 * Size of a 64-bit integer in bytes. This is, somewhat coincidentally,
	 * equal to the number of bytes taken up by 64 bits.
	 */
	public static final int INT64 = 8;
	/**
	 * Size of a native {@code float} in bytes.
	 * @see #DOUBLE
	 * @see BasicType#FLOAT
	 */
	public static final int FLOAT = 4;
	/**
	 * Size of a native {@code double} in bytes.
	 * @see #FLOAT
	 * @see BasicType#DOUBLE
	 */
	public static final int DOUBLE = 8;
	/**
	 * Size of the Windows {@code HANDLE} type in bytes.
	 * @see #GDIOBJ
	 * @see BasicType#HANDLE
	 */
	public static final int HANDLE = 4;
	/**
	 * Size of a handle to a Windows GDI object in bytes.
	 * @see #HANDLE
	 * @see BasicType#GDIOBJ
	 */
	public static final int GDIOBJ = 4;

	/**
	 * Returns the minimum number of whole words that completely contains the
	 * given number of bytes.
	 * @param bytes Number of bytes
	 * @return Number of whole words that completely contains {@bytes}
	 * @see #sizeWholeWords(int)
	 */
	public static int minWholeWords(int bytes) {
		return (bytes + WORD - 1) / WORD;
	}

	/**
	 * Returns the number of bytes taken by the minimum number of whole words
	 * which contain the given number of bytes.
	 * @param bytes Number of bytes
	 * @return Number of bytes occupied by the minimum number of whole words
	 * which completely contain {@bytes}
	 * @see #pointerWholeWordBytes()
	 */
	public static int sizeWholeWords(int bytes) {
		return minWholeWords(bytes) * WORD;
	}

	/**
	 * Returns the number of bytes taken by the minimum number of whole words
	 * which contains a pointer.
	 * @return Number of bytes taken by minimum whole words that contain a
	 * pointer
	 * @see #sizeWholeWords(int)
	 */
	public static int pointerWholeWordBytes() {
		return ((POINTER + WORD - 1) / WORD) * POINTER;
	}

	// Don't instantiate!
	private PrimitiveSize() {
	}
}
