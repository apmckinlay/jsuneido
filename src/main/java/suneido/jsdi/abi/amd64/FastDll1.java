/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import suneido.jsdi.Dll;
import suneido.jsdi.DllFactory;
import suneido.jsdi.DllInterface;
import suneido.jsdi.type.Type;
import suneido.language.Args;

/**
 * Dll with one parameters, corresponds to {@link NativeCall64#J1_RETURN_INT64}.
 *
 * @author Victor Schappert
 * @since 20140801
 * @see FastDll0
 * @see FastDll2
 * @see FastDll3
 * @see FastDll4
 */
@DllInterface
final class FastDll1 extends Dll {

	//
	// DATA
	//

	private final Type p0;

	//
	// CONSTRUCTORS
	//

	FastDll1(long funcPtr, ParamsTypeList params, Type returnType,
			String valueName, DllFactory dllFactory, String libraryName,
			String funcName) {
		super(funcPtr, params, returnType, valueName, dllFactory, libraryName, funcName);
		p0 = params.get(0).getType();
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public Object call(Object... args) {
		args = Args.massage(params, args);
		return call1(args[0]);
	}

	@Override
	public Object call0() {
		return call1(fillin(0));
	}

	@Override
	public Object call1(Object a) {
		final long a_ = p0.marshallInToLong(a);
		final long r = NativeCall64.callJ1(funcPtr, a_);
		return returnType.marshallOutReturnValue(r, null);
	}
}
