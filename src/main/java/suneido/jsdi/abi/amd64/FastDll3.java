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
 * Dll with three parameters, corresponds to
 * {@link NativeCall64#J3_RETURN_INT64}.
 *
 * @author Victor Schappert
 * @since 20140801
 * @see FastDll0
 * @see FastDll1
 * @see FastDll2
 * @see FastDll4
 */
@DllInterface
class FastDll3 extends Dll {

	//
	// DATA
	//

	private final Type p0;
	private final Type p1;
	private final Type p2;

	//
	// CONSTRUCTORS
	//

	FastDll3(long funcPtr, ParamsTypeList params, Type returnType,
			String valueName, DllFactory dllFactory, String libraryName,
			String funcName) {
		super(funcPtr, params, returnType, valueName, dllFactory, libraryName, funcName);
		p0 = params.get(0).getType();
		p1 = params.get(1).getType();
		p2 = params.get(2).getType();
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public Object call(Object... args) {
		Args.massage(params, args);
		return call3(args[0], args[1], args[2]);
	}

	@Override
	public Object call0() {
		return call3(fillin(0), fillin(1), fillin(2));
	}

	@Override
	public Object call1(Object a) {
		return call3(a, fillin(1), fillin(2));
	}

	@Override
	public Object call2(Object a, Object b) {
		return call3(a, b, fillin(2));
	}

	@Override
	public Object call3(Object a, Object b, Object c) {
		final long a_ = p0.marshallInToLong(a);
		final long b_ = p1.marshallInToLong(b);
		final long c_ = p2.marshallInToLong(c);
		final long r = NativeCall64.callJ3(funcPtr, a_, b_, c_);
		return returnType.marshallOutReturnValue(r, null);
	}
}
