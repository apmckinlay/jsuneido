package suneido.language.jsdi.type;

import java.util.Map;
import java.util.TreeMap;

import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.MarshallPlan;

/**
 * Enumerates the available 'basic' (<em>ie</em> non-structure, non-callback)
 * types in the JSDI type hierarchy.
 * 
 * @author Victor Schapppert
 * @since 20130627
 * @see SizeDirect
 */
@DllInterface
public enum BasicType {

	/**
	 * Enumerator for a boolean value, which is represented in native DLL calls
	 * as a 32-bit integer.
	 */
	BOOL("bool", SizeDirect.BOOL),
	/**
	 * Enumerator for a signed single-byte number/character value.
	 * @see #SHORT
	 * @see #LONG
	 * @see #INT64
	 */
	CHAR("char", SizeDirect.CHAR),
	/**
	 * Enumerator for a signed short integer value, which is represented in
	 * native DLL calls as a signed 16-bit integer.
	 * @see #CHAR
	 * @see #LONG
	 * @see #INT64
	 */
	SHORT("short", SizeDirect.SHORT),
	/**
	 * Enumerator for a signed long integer value, which is represented in
	 * native DLL calls as a signed 32-bit integer.
	 * @see #CHAR
	 * @see #SHORT
	 * @see #INT64
	 */
	LONG("long", SizeDirect.LONG),
	/**
	 * Enumerator for a 64-bit signed integer value.
	 * @see #CHAR
	 * @see #SHORT
	 * @see #LONG
	 */
	INT64("int64", SizeDirect.INT64),
	/**
	 * Enumerator for a 32-bit floating-point number (<em>ie</em> a single-
	 * precision IEEE floating-point number, known as <code>float</code> in
	 * C, C++, and Java).
	 * @see #DOUBLE
	 */
	FLOAT("float", SizeDirect.FLOAT),
	/**
	 * Enumerator for a 64-bit floating-point number (<em>ie</em> a double-
	 * precision IEEE floating-point number, nkown as <code>double</code> in C,
	 * C++, and Java).
	 * @see #SINGLE
	 */
	DOUBLE("double", SizeDirect.DOUBLE),
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
	HANDLE("handle", SizeDirect.HANDLE),
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
	GDIOBJ("gdiobj", SizeDirect.GDIOBJ);

	//
	// DATA/CONSTRUCTORS
	//

	private final String       identifierString;
	private final MarshallPlan marshallPlan;

	private BasicType(String identifierString, int sizeDirect) {
		this.identifierString = identifierString;
		this.marshallPlan = MarshallPlan.makeDirectPlan(sizeDirect);
	}

	//
	// ACCESSORS
	//

	public MarshallPlan getMarshallPlan() {
		return marshallPlan;
	}

	//
	// STATICS
	//

	private static final Map<String, BasicType> identifierMap;
	static {
		identifierMap = new TreeMap<String, BasicType>();
		for (BasicType type : values()) {
			identifierMap.put(type.identifierString, type);
		}
	}

	public static final BasicType fromIdentifier(String identifierString) {
		return identifierMap.get(identifierString);
	}

	public final String toIdentifier() {
		return identifierString;
	}
}
