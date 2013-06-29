package suneido.language.jsdi.type;

import java.util.EnumMap;

import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.JSDI;
import suneido.language.jsdi.StorageType;

/**
 * Factory for constructing types in the JSDI type hierarchy. JSDI types must
 * only be created, outside this package, through this factory class.
 * 
 * @author Victor Schappert
 * @since 20130627
 */
@DllInterface
public final class TypeFactory {

	//
	// DATA
	//

	private final JSDI jsdi;
	private final EnumMap<BasicType, BasicValue> basicValues;

	//
	// CONSTRUCTORS
	//

	public TypeFactory(JSDI jsdi) {
		this.jsdi = jsdi;
		this.basicValues = new EnumMap<BasicType, BasicValue>(BasicType.class);
		loadBasicValues();
	}

	//
	// INTERNALS
	//

	private void loadBasicValues() {
		for (BasicType basicType : BasicType.values()) {
			// Get *permanent* handles to the value type and pointer type based
			// on the given underlying type. These handles persist for the
			// lifetime of the loaded JSDI library and do not need to be freed.
			long basicValueHandle = getBasicValueHandle(basicType);
			long basicPointerHandle = getBasicPointerHandle(basicType);
			BasicValue T = new BasicValue(basicType, basicValueHandle,
					basicPointerHandle);
			basicValues.put(basicType, T);
		}
	}

	private Type makeBasicArray(BasicType underlying, int numElements) {
		// TODO: implement this
		throw new RuntimeException("not implemented");
	}

	static native void releaseHandle(long jsdiHandle);

	private static native long getBasicValueHandle(BasicType underlying);

	private static native long getBasicPointerHandle(BasicType underlying);

	private static native long makeBasicArrayHandle(BasicType underlying,
			int numElements);

	//
	// ACCESSORS
	//

	public Type makeBasicType(BasicType basicType, StorageType storageType,
			int numElements) {
		if (basicType == null)
			throw new IllegalArgumentException("basicType cannot be null");
		if (storageType == null)
			throw new IllegalArgumentException("storageType cannot be null");
		if (numElements < 0)
			throw new IllegalArgumentException("numElements cannot be negative");
		if (StorageType.ARRAY != storageType && 1 != numElements)
			throw new IllegalArgumentException(
					"numElements must be 1 for VALUE/POINTER");
		switch (storageType) {
		case VALUE:
			return basicValues.get(basicType);
		case POINTER:
			return basicValues.get(basicType).getPointerType();
		case ARRAY:
			return makeBasicArray(basicType, numElements);
		}
		assert false : "Control should never pass here";
		return null;
	}
}
