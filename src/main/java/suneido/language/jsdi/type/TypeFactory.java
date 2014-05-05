/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.type;

import java.util.EnumMap;

import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.JSDI;
import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.StorageType;

/**
 * Factory for constructing types in the JSDI type hierarchy. JSDI types must
 * only be created, outside of this package, through this factory class.
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
		if (null == jsdi) {
			throw new IllegalArgumentException("jsdi cannot be null");
		}
		this.jsdi = jsdi;
		this.basicValues = new EnumMap<>(BasicType.class);
		loadBasicValues();
	}

	//
	// INTERNALS
	//

	private void loadBasicValues() {
		for (BasicType basicType : BasicType.values()) {
			BasicValue T = new BasicValue(basicType);
			basicValues.put(basicType, T);
		}
	}

	private static Type makeBasicArray(BasicValue underlying, int numElems) {
		assert null != underlying : "Underlying basic type cannot be null";
		return new BasicArray(underlying, numElems);
	}

	private static void check(StorageType storageType, int numElements) {
		if (storageType == null)
			throw new IllegalArgumentException("storageType cannot be null");
		if (numElements < 1)
			throw new IllegalArgumentException("numElements must be positive");
		if (StorageType.ARRAY != storageType && 1 != numElements)
			throw new IllegalArgumentException(
					"numElements must be 1 for VALUE/POINTER");
	}

	//
	// ACCESSORS
	//

	public Type makeBasicType(BasicType basicType, StorageType storageType,
			int numElements) {
		if (basicType == null)
			throw new IllegalArgumentException("basicType cannot be null");
		check(storageType, numElements);
		switch (storageType) {
		case VALUE:
			return basicValues.get(basicType);
		case POINTER:
			return basicValues.get(basicType).getPointerType();
		case ARRAY:
			return makeBasicArray(basicValues.get(basicType), numElements);
		}
		assert false : "control should never pass here";
		return null;
	}

	@SuppressWarnings("static-method")
	public Type makeStringType(StorageType storageType, int numElements,
			boolean isZeroTerminated, boolean hasInModifier) {
		check(storageType, numElements);
		// ASSERT: If [in] modifier incorrectly added, an exception will be
		//         thrown by the compiler, so no need to handle it here.
		switch (storageType) {
		case VALUE:
			if (isZeroTerminated) {
				return hasInModifier ? InString.INSTANCE : InOutString.INSTANCE;
			} else {
				return BufferType.INSTANCE;
			}
		case ARRAY:
			return new StringDirect(numElements, isZeroTerminated);
		case POINTER:
			throw new JSDIException(
					"jSuneido does not support string* or buffer*");
		}
		assert false : "control should never pass here";
		return null;
	}

	@SuppressWarnings("static-method")
	public Type makeResourceType(StorageType storageType, int numElements,
			boolean isZeroTerminated, boolean hasInModifier) {
		check(storageType, numElements);
		switch (storageType) {
		case VALUE:
			return ResourceType.INSTANCE;
		case ARRAY:
			throw new JSDIException("jSuneido does not support " +
				ResourceType.IDENTIFIER + "[]");
		case POINTER:
			throw new JSDIException("jSuneido does not support " +
					ResourceType.IDENTIFIER + "*");
		}
		assert false : "control should never pass here";
		return null;
	}

	@SuppressWarnings("static-method")
	public Structure makeStruct(String valueName, TypeList members) {
		return new Structure(valueName, members);
	}

	public Callback makeCallback(String valueName, TypeList members) {
		return new Callback(valueName, members, jsdi.getThunkManager());
	}
}
