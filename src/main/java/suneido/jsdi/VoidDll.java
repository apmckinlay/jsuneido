/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi;

import suneido.jsdi.type.Type;
import suneido.jsdi.type.TypeList;
import suneido.jsdi.type.VoidType;
import suneido.runtime.Args;

/**
 * <p>
 * Simple class implementing a "void" {@code dll}. A "void" {@code dll} is a
 * syntactically valid {@code dll} which doesn't actually have a library and
 * thus isn't linked to a DLL function. The purpose of the "void" {@code dll} is
 * to enable test code to obtain a reference to a {@code dll} without the need
 * for an underlying library function.
 * </p>
 * <p>
 * Examples:
 * <pre>dll void library:function(long param)
 *     => dll which calls 'function' declared in 'library' when invoked
 * dll void void:function(long param)
 *     => "void" dll which simply returns false when invoked</pre>
 * </p>
 * @author Victor Schappert
 * @since 20130815
 */
@DllInterface
public final class VoidDll extends Dll {

	//
	// CONSTRUCTORS
	//

	/**
	 * Constructs a new "void" {@code dll}.
	 */
	public VoidDll(TypeList params, Type returnType, String valueName,
			DllFactory dllFactory, String userFuncName) {
		super(0, params, returnType, dllFactory, VoidType.IDENTIFIER,
				userFuncName);
	}

	//
	// ANCESTOR TYPE: SuCallable
	//

	@Override
	public Object call(Object... args) {
		Args.massage(super.params, args);
		return Boolean.FALSE;
	}
}
