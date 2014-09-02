/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import suneido.jsdi.DllInterface;
import suneido.jsdi.StorageType;
import suneido.jsdi.type.BasicType;
import suneido.jsdi.type.BasicValue;
import suneido.jsdi.type.Type;
import suneido.jsdi.type.TypeId;

/**
 * Enumerators for describing Windows x64 ABI register use.
 *
 * @author Victor Schappert
 * @since 20140730
 */
@DllInterface
enum ParamRegisterType {

	/**
	 * Non-floating-point value passed in a general-purpose register
	 */
	UINT64,
	/**
	 * 64-bit <code>double</code> value passed in an SSE register
	 */
	DOUBLE,
	/**
	 * 32-bit <code>float</code> value passed in an SSE register
	 */
	FLOAT;

	//
	// CONSTANTS
	//

	static final int ALL_FOUR_REGISTERS_ARE_UINTS = 0;

	//
	// INTERNALS
	//

	private static int packForNative(ParamRegisterType param0,
			ParamRegisterType param1, ParamRegisterType param2,
			ParamRegisterType param3) {
		return
				param0.ordinal() << 030 |
				param1.ordinal() << 020 |
				param2.ordinal() << 010 |
				param3.ordinal() << 000;
	}

	//
	// STATICS
	//

	/**
	 * <p>
	 * Determines the parameter register usage needed to pass the first four
	 * parameters of a native function call and packs them in the format
	 * expected by the native side.
	 * </p>
	 *
	 * <p>
	 * If a native function call has fewer than four parameters, simply
	 * right-fill parameter types with {@link VoidType}. For example, if a
	 * function takes two parameters, pass the types of those parameters as
	 * <code>param0 .. param1</code> and pass {@link VoidType} as
	 * <code>param2 .. param3</code>. 
	 * </p>
	 *
	 * @param param0 Type of the first parameter
	 * @param param1 Type of the second parameter
	 * @param param2 Type of the third parameter
	 * @param param3 Type of the fourth parameter
	 * @return Packaged register usage instruction
	 * @since 20140730
	 */
	public static int packFromParamTypes(Type param0, Type param1, Type param2,
			Type param3) {
		return packForNative(fromParamType(param0), fromParamType(param1),
				fromParamType(param2), fromParamType(param3));
	}

	/**
	 * Determines the parameter register usage needed to pass a parameter of the
	 * given type.
	 * 
	 * @param paramType
	 *            Type of the parameter
	 * @return Register usage applicable for {@code paramType}
	 * @since 20140730
	 */
	public static ParamRegisterType fromParamType(Type paramType) {
		if (TypeId.BASIC == paramType.getTypeId()
				&& StorageType.VALUE == paramType.getStorageType()) {
			final BasicType bt = ((BasicValue) paramType).getBasicType();
			if (BasicType.DOUBLE == bt) {
				return DOUBLE;
			} else if (BasicType.FLOAT == bt) {
				return FLOAT;
			}
		}
		return UINT64;
	}
}
