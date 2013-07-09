package suneido.language.jsdi.type;

import suneido.language.jsdi.DllInterface;

/**
 * Contains the "direct" size of various native types supported by JSDI. The
 * sizes are given in bytes.
 * @author Victor Schapppert
 * @since 20130703
 */
@DllInterface
public final class SizeDirect {
	/**
	 * Size of a native pointer type, in bytes. This should give the same
	 * result as the value reported by a C compiler for {@code sizeof(void *)}
	 * on the native platform. 
	 */
	public static final int POINTER = 4;
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
	 * Size of a native {@code char} in bytes.
	 */
	public static final int CHAR = 1;
	/**
	 * Size of a native {@code short} in bytes.
	 */
	public static final int SHORT = 2;
	/**
	 * Size of a native {@code long} in bytes.
	 */
	public static final int LONG = 4;
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

	// Don't instantiate!
	private SizeDirect() {
	}
}
