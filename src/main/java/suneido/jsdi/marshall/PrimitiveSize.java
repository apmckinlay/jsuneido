/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.marshall;

import suneido.SuInternalError;
import suneido.boot.Platform;
import suneido.jsdi.DllInterface;
import suneido.jsdi.type.BasicType;

/**
 * Contains the "direct" size of various native types supported by JSDI. The
 * sizes are given in bytes.
 * @author Victor Schappert
 * @since 20130703
 */
@DllInterface
public final class PrimitiveSize {
	/**
	 * Size of a native pointer type, in bytes. This should give the same
	 * result as the value reported by a C compiler for {@code sizeof(void *)}
	 * on the native platform. 
	 */
	public static final int POINTER = getPlatformPointerSize();
	/**
	 * Size of the native word size, in bytes.
	 */
	public static final int WORD = POINTER;
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
	public static final int HANDLE = POINTER;
	/**
	 * Size of a handle to a Windows GDI object in bytes.
	 * @see #HANDLE
	 * @see BasicType#GDIOBJ
	 */
	public static final int GDIOBJ = POINTER;

	/**
	 * Returns the minimum number of contiguous Java <code><b>long</b></code>
	 * values that completely contain the given number of bytes.
	 * 
	 * @param bytes
	 *            Number of bytes
	 * @return Minimum number of <code><b>long</b></code> values that completely
	 *         contain {@code bytes}
	 * @since 20140729
	 * @see #sizeLongs(int)
	 */
	public static int minLongs(int bytes) {
		return ((bytes + Long.BYTES - 1) / Long.BYTES);
	}

	/**
	 * Returns the number of bytes taken by the minimum number of contiguous
	 * Java <code><b>long</b></code> values which contain the given number of
	 * bytes.
	 * 
	 * @param bytes
	 *            Number of bytes
	 * @return Number of bytes occupied by the minimum number of
	 *        <code><b>long</b></code> values which completely contain
	 *        {@code bytes}
	 * @since 20140729
	 * @see #minLongs(int)
	 */
	public static int sizeLongs(int bytes) {
		return minLongs(bytes) * Long.BYTES;
	}

	// Don't instantiate!
	private PrimitiveSize() {
	}

	private static final int getPlatformPointerSize() {
		switch (Platform.getPlatform()) {
		case WIN32_AMD64:
			return 8;
		case WIN32_X86:
			return 4;
		case LINUX_AMD64:
			// For known platforms which simply don't support JSDI, fall through
			// to "unknown platform".
		case UNKNOWN_PLATFORM:
			return 0;
		default:
			throw SuInternalError.unhandledEnum(Platform.getPlatform());
		}
	}
}
