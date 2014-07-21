/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import suneido.jsdi.CallGroup;
import suneido.jsdi.Dll;
import suneido.jsdi.DllFactory;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDI;
import suneido.jsdi.ReturnTypeGroup;
import suneido.jsdi.type.Type;
import suneido.jsdi.type.TypeList;

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
