/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi;

import static suneido.SuInternalError.unreachable;

import java.util.EnumMap;

import suneido.SuInternalError;
import suneido.jsdi.type.BasicArray;
import suneido.jsdi.type.BasicType;
import suneido.jsdi.type.BasicValue;
import suneido.jsdi.type.BufferType;
import suneido.jsdi.type.Callback;
import suneido.jsdi.type.InOutString;
import suneido.jsdi.type.InString;
import suneido.jsdi.type.ResourceType;
import suneido.jsdi.type.StringDirect;
import suneido.jsdi.type.Structure;
import suneido.jsdi.type.Type;
import suneido.jsdi.type.TypeList;
import suneido.jsdi.type.TypeList.Args;

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
			throw new SuInternalError("storageType cannot be null");
		if (numElements < 1)
			throw new SuInternalError("numElements must be positive");
		if (StorageType.ARRAY != storageType && 1 != numElements)
			throw new SuInternalError(
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

	/**
	 * <p>
	 * Returns an instance of {@link Type} representing the requested basic
	 * type.
	 * </p>
	 *
	 * <p>
	 * As of 20140728, pointers to basic types are no longer permitted in
	 * Suneido. Thus passing {@link StorageType#POINTER POINTER} as the
	 * <code>storageType</code> parameter will result in an exception. The
	 * reason for this is that a pointer to a basic type doesn't have an
	 * intuitive marshalling syntax within the Suneido language. For example,
	 * suppose you have a function
	 * <code>f&nbsp;=&nbsp;<b>dll</b>&nbsp;lib:func(<b>int32</b>&nbsp;*&nbsp;a)</code>
	 * and you wish to call it with code such as: <code>f(x)</code>. What
	 * Suneido type(s) might <code>x</code> be? All possibilities are
	 * unsatisfactory:
	 * <ul>
	 * <li>
	 * If <code>x</code> can be a mere number&mdash;indicating the actual
	 * <em>value</em> pointed-to&mdash;then it is impossible to pass a NULL
	 * pointer; moreover, the value pointed to cannot be marshalled out since
	 * Suneido numbers are passed by value, not by reference.
	 * </li>
	 * <li>
	 * If <code>x</code> can be a Suneido <code>Object</code>, then it is not
	 * obvious which member of <code>x</code> should be used to represent the
	 * value pointed-to.
	 * </li>
	 * </ul>
	 * For these reasons, pointers to basic types are not permitted and the
	 * appropriate solution in Suneido is to define a <code>struct</code>
	 * containing one member of the appropriate basic type and to use a pointer
	 * to the <code>struct</code> type in the function signature. Using the
	 * above example, and supposing <code>INT32</code> is defined to be
	 * <code>struct&nbsp;{&nbsp;<b>int32</b>&nbsp;value&nbsp;}</code>, the
	 * function should be declared as
	 * <code>f&nbsp;=&nbsp;<b>dll</b>&nbsp;lib:func(INT32&nbsp;*&nbsp;a)</code>.
	 * </p>
	 * 
	 * @param basicType The basic type underlying the type
	 * @param storageType The storage type (value, pointer, or array)
	 * @param numElements Number of elements in the array (ignored unless
	 *        <code>storageType</code> is {@link StorageType#ARRAY ARRAY}
	 * @return Type instance
	 * @since 20140718
	 * @throws JSDIException If <code>storageType</code> is
	 *         {@link StorageType#POINTER POINTER}
	 */
	public final Type makeBasicType(BasicType basicType,
			StorageType storageType, int numElements) {
		if (basicType == null)
			throw new SuInternalError("basicType cannot be null");
		check(storageType, numElements);
		switch (storageType) {
		case VALUE:
			return basicValues.get(basicType);
		case POINTER:
			throw new JSDIException("pointer to basic type not allowed: "
					+ basicType.getName() + '*');
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
	@SuppressWarnings("static-method")
	public final Structure makeStruct(String valueName, TypeList members) {
		return new Structure(valueName, members);
	}

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
	public final Callback makeCallback(String valueName, TypeList params) {
		return new Callback(valueName, params, jsdi.getThunkManager());
	}

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
