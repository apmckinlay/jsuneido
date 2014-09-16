/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import suneido.jsdi.Dll;
import suneido.jsdi.DllFactory;
import suneido.jsdi.DllInterface;
import suneido.jsdi.type.Type;
import suneido.runtime.Args;
import suneido.runtime.FunctionSpec;

/**
 * Dll with two parameters, corresponds to {@link NativeCall64#J2_RETURN_INT64}.
 *
 * @author Victor Schappert
 * @since 20140801
 * @see FastDll0
 * @see FastDll1
 * @see FastDll3
 * @see FastDll4
 */
@DllInterface
class FastDll2 extends Dll {

	//
	// DATA
	//

	private final Type p0;
	private final Type p1;

	//
	// CONSTRUCTORS
	//

	FastDll2(long funcPtr, ParamsTypeList params, Type returnType,
			DllFactory dllFactory, String libraryName, String funcName) {
		super(funcPtr, params, returnType, dllFactory, libraryName, funcName,
				new FunctionSpec(params.getEntryNames()));
		p0 = params.get(0).getType();
		p1 = params.get(1).getType();
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public Object call(Object... args) {
		args = Args.massage(params, args);
		return call2(args[0], args[1]);
	}

	@Override
	public Object call0() {
		return call2(fillin(0), fillin(1));
	}

	@Override
	public Object call1(Object a) {
		return call2(a, fillin(1));
	}

	@Override
	public Object call2(Object a, Object b) {
		final long a_ = p0.marshallInToLong(a);
		final long b_ = p1.marshallInToLong(b);
		final long r = NativeCall64.callJ2(funcPtr, a_, b_);
		return returnType.marshallOutReturnValue(r, null);
	}
}
