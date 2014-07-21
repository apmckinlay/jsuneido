/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi;

import static suneido.SuInternalError.unreachable;

import java.util.EnumMap;

import suneido.language.jsdi.type.BasicArray;
import suneido.language.jsdi.type.BasicType;
import suneido.language.jsdi.type.BasicValue;
import suneido.language.jsdi.type.BufferType;
import suneido.language.jsdi.type.Callback;
import suneido.language.jsdi.type.InOutString;
import suneido.language.jsdi.type.InString;
import suneido.language.jsdi.type.ResourceType;
import suneido.language.jsdi.type.StringDirect;
import suneido.language.jsdi.type.Structure;
import suneido.language.jsdi.type.Type;
import suneido.language.jsdi.type.TypeList;
import suneido.language.jsdi.type.TypeList.Args;

/**
 * Factory for construction JSDI objects in an implementation-neutral manner.
 *
 * @author Victor Schappert
 * @since 20140718
 */
@DllInterface
public abstract class Factory {

	//
	// DATA
	//

	protected final JSDI jsdi;
	private final EnumMap<BasicType, BasicValue> basicValues;

	//
	// CONSTRUCTORS
	//

	protected Factory(JSDI jsdi) {
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
	// TYPE CREATION
	//

	/**
	 * Constructs a new type list.
	 *
	 * @param args Type list arguments
	 * @return Type list based on {@code args}
	 */
	public abstract TypeList makeTypeList(Args args);

	// TODO: Docs since 20140718
	public final Type makeBasicType(BasicType basicType, StorageType storageType,
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
		throw unreachable();
	}

	// TODO: docs since 20140718
	@SuppressWarnings("static-method")
	public final Type makeStringType(StorageType storageType, int numElements,
			boolean isZeroTerminated, boolean hasInModifier) {
		check(storageType, numElements);
		// ASSERT: If [in] modifier incorrectly added, an exception will be
		// thrown by the compiler, so no need to handle it here.
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
		throw unreachable();
	}

	// TODO: docs since 20140718
	@SuppressWarnings("static-method")
	public final Type makeResourceType(StorageType storageType, int numElements,
			boolean isZeroTerminated, boolean hasInModifier) {
		check(storageType, numElements);
		switch (storageType) {
		case VALUE:
			return ResourceType.INSTANCE;
		case ARRAY:
			throw new JSDIException("jSuneido does not support "
					+ ResourceType.IDENTIFIER + "[]");
		case POINTER:
			throw new JSDIException("jSuneido does not support "
					+ ResourceType.IDENTIFIER + "*");
		}
		throw unreachable();
	}

	/**
	 * Constructs a {@link Structure} with the given member list.
	 *
	 * @param valueName
	 *            The Suneido (<em>ie</em> user-assigned) name of the
	 *            {@code callback} object
	 * @param members
	 *            Structure member list
	 * @return Constructed {@link Structure}
	 * @since 20140718
	 */
	public abstract Structure makeStruct(String valueName, TypeList members);

	/**
	 * Constructs a {@link Callback} with the given parameter signature.
	 * 
	 * @param valueName
	 *            The Suneido (<em>ie</em> user-assigned) name of the
	 *            {@code callback} object
	 * @param params
	 *            Callback parameter signature
	 * @return Constructed {@link Callback}
	 * @since 20140718
	 */
	public abstract Callback makeCallback(String valueName, TypeList params);

	//
	// DLL CREATION
	//

	/**
	 * Constructs a {@link Dll} capable of invoking the given library function.
	 * 
	 * @param suTypeName
	 *            The Suneido (<em>ie</em> user-assigned) type name
	 * @param libraryName
	 *            Name of the DLL library module
	 * @param userFuncName
	 *            Function name to load within the library. If no function with
	 *            this name is found, the name {@code userFuncName + 'A'} is
	 *            also tried.
	 * @param params
	 *            {@link TypeList} describing the names, types, and positions of
	 *            the {@link Dll}'s parameters.
	 * @param returnType
	 *            {@link Type} describing the return type of the {@link Dll}.
	 * @return Constructed {@link Dll}
	 * @since 20140718
	 * @throws JSDIException
	 *             If library cannot be loaded, address of function cannot be
	 *             located, or some other error occurs
	 */
	public abstract Dll makeDll(String suTypeName, String libraryName,
			String userFuncName, TypeList params, Type returnType);
}
