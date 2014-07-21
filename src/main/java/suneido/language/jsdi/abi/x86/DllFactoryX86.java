/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.abi.x86;

import suneido.language.jsdi.CallGroup;
import suneido.language.jsdi.Dll;
import suneido.language.jsdi.DllFactory;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.JSDI;
import suneido.language.jsdi.ReturnTypeGroup;
import suneido.language.jsdi.type.Type;
import suneido.language.jsdi.type.TypeList;

/**
 * Dll factory specialized to create {@code x86} {@code dll} values.
 *
 * @author Victor Schappert
 * @since 20140718
 */
@DllInterface
final class DllFactoryX86 extends DllFactory {

	//
	// CONSTRUCTORS
	//

	DllFactoryX86(JSDI jsdi) {
		super(jsdi);
	}

	//
	// ANCESTOR CLASS DllFactory
	//

	@Override
	protected Dll makeRealDll(String suTypeName, String libraryName,
			String userFuncName, TypeList params, Type returnType) {
		final ReturnTypeGroup rtg = ReturnTypeGroup.fromType(returnType);
		long funcPtr = getFuncPtr(libraryName, userFuncName);
		CallGroup cg = CallGroup.fromTypeList(params);
		NativeCallX86 nc = null;
		if (null != cg) {
			nc = NativeCallX86.get(cg, rtg);
		}
		final DllX86 result = new DllX86(funcPtr, params, returnType, rtg, nc,
				suTypeName, this, libraryName, userFuncName);
		return result;
	}
}
