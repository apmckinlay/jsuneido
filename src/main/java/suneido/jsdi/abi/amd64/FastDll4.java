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
 * Dll with four parameters, corresponds to {@link NativeCall64#J4_RETURN_INT64}
 * .
 *
 * @author Victor Schappert
 * @since 20140801
 * @see FastDll0
 * @see FastDll1
 * @see FastDll2
 * @see FastDll3
 */
@DllInterface
final class FastDll4 extends Dll {

	//
	// DATA
	//

	private final Type p0;
	private final Type p1;
	private final Type p2;
	private final Type p3;

	//
	// CONSTRUCTORS
	//

	FastDll4(long funcPtr, ParamsTypeList params, Type returnType,
			DllFactory dllFactory, String libraryName, String funcName) {
		super(funcPtr, params, returnType, dllFactory, libraryName, funcName);
		p0 = params.get(0).getType();
		p1 = params.get(1).getType();
		p2 = params.get(2).getType();
		p3 = params.get(3).getType();
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public Object call(Object... args) {
		args = Args.massage(params, args);
		return call4(args[0], args[1], args[2], args[3]);
	}

	@Override
	public Object call0() {
		return call4(fillin(0), fillin(1), fillin(2), fillin(3));
	}

	@Override
	public Object call1(Object a) {
		return call4(a, fillin(1), fillin(2), fillin(3));
	}

	@Override
	public Object call2(Object a, Object b) {
		return call4(a, b, fillin(2), fillin(3));
	}

	@Override
	public Object call3(Object a, Object b, Object c) {
		return call4(a, b, c, fillin(3));
	}

	@Override
	public Object call4(Object a, Object b, Object c, Object d) {
		final long a_ = p0.marshallInToLong(a);
		final long b_ = p1.marshallInToLong(b);
		final long c_ = p2.marshallInToLong(c);
		final long d_ = p3.marshallInToLong(d);
		final long r = NativeCall64.callJ4(funcPtr, a_, b_, c_, d_);
		return returnType.marshallOutReturnValue(r, null);
	}
}
