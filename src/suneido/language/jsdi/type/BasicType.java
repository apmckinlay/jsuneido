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
 */
@DllInterface
public enum BasicType {

	/**
	 * Enumerator for a boolean value, which is represented in native DLL calls
	 * as a 32-bit integer.
	 */
	BOOL("bool", SizeDirect.BOOL),
	/**
	 * Enumerator for a character value, which is represented in native DLL
	 * calls as a single byte.
	 */
	CHAR("char", SizeDirect.CHAR),
	/**
	 * Enumerator for a signed short integer value, which is represented in
	 * native DLL calls as a signed 16-bit integer.
	 */
	SHORT("short", SizeDirect.SHORT),
	/**
	 * Enumerator for a signed long integer value, which is represented in
	 * native DLL calls as a signed 32-bit integer.
	 */
	LONG("long", SizeDirect.LONG),
	/**
	 * Enumerator for a 64-bit signed integer value.
	 */
	INT64("int64", SizeDirect.INT64);

	//
	// DATA/CONSTRUCTORS
	//

	private final String       identifierString;
	private final MarshallPlan marshallPlan;

	private BasicType(String identifierString, int sizeDirect) {
		this.identifierString = identifierString;
		this.marshallPlan = new MarshallPlan(sizeDirect);
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
