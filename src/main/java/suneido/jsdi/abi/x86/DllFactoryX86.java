/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import suneido.jsdi.Dll;
import suneido.jsdi.DllFactory;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDI;
import suneido.jsdi.marshall.ReturnTypeGroup;
import suneido.jsdi.marshall.MarshallPlan.StorageCategory;
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
		MarshallPlanX86 paramsPlan = null;
		NativeCallX86 nc = null;
		if (params.isClosed()) {
			paramsPlan = (MarshallPlanX86) params.makeParamsMarshallPlan(false,
					0 < returnType.getVariableIndirectCount());
			StorageCategory sc = paramsPlan.getStorageCategory();
			nc = NativeCallX86.get(sc, rtg);
		}
		final DllX86 result = new DllX86(funcPtr, params, returnType, rtg, nc,
				suTypeName, this, libraryName, userFuncName, paramsPlan);
		return result;
	}
}
