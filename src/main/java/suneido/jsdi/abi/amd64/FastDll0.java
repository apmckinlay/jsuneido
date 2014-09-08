/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import suneido.jsdi.Dll;
import suneido.jsdi.DllFactory;
import suneido.jsdi.DllInterface;
import suneido.jsdi.type.Type;
import suneido.runtime.Args;

/**
 * Dll with no parameters, corresponds to {@link NativeCall64#J0_RETURN_INT64}.
 *
 * @author Victor Schappert
 * @since 20140801
 * @see FastDll1
 * @see FastDll2
 * @see FastDll3
 * @see FastDll4
 */
@DllInterface
final class FastDll0 extends Dll {

	//
	// CONSTRUCTORS
	//

	FastDll0(long funcPtr, ParamsTypeList params, Type returnType,
			String valueName, DllFactory dllFactory, String libraryName,
			String funcName) {
		super(funcPtr, params, returnType, valueName, dllFactory, libraryName, funcName);
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public Object call(Object... args) {
		Args.massage(params, args);
		return call0();
	}

	@Override
	public Object call0() {
		final long r = NativeCall64.callJ0(funcPtr);
		return returnType.marshallOutReturnValue(r, null);
	}
}
