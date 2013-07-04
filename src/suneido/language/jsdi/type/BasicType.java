package suneido.language.jsdi.type;

import suneido.language.jsdi.DllInterface;

/**
 * Enumerates the available 'basic' (<em>ie</em> non-structure, non-callback)
 * types in the JSDI type hierarchy.
 * 
 * @author Victor Schapppert
 * @since 20130627
 */
@DllInterface
public enum BasicType {

	/**
	 * Enumerator for a boolean value, which is represented in native DLL calls
	 * as a 32-bit integer.
	 */
	BOOL("bool"),
	/**
	 * Enumerator for a character value, which is represented in native DLL
	 * calls as a single byte.
	 */
	CHAR("char"),
	/**
	 * Enumerator for a signed short integer value, which is represented in
	 * native DLL calls as a signed 16-bit integer.
	 */
	SHORT("short"),
	/**
	 * Enumerator for a signed long integer value, which is represented in
	 * native DLL calls as a signed 32-bit integer.
	 */
	LONG("long"),
	/**
	 * Enumerator for a 64-bit signed integer value.
	 */
	INT64("int64");

	//
	// DATA/CONSTRUCTORS
	//

	private final String identifierString;

	private BasicType(String identifierString) {
		this.identifierString = identifierString;
	}

	//
	// STATICS
	//

	public static final BasicType fromIdentifier(String identifierString) {
		// TODO: make this more efficient later
		// TODO: calling values() is pretty expensive and you should statically
		// cache some better data structure
		for (BasicType type : values()) {
			if (type.identifierString.equals(identifierString))
				return type;
		}
		return null;
	}

	public final String toIdentifier() {
		return identifierString;
	}
}
